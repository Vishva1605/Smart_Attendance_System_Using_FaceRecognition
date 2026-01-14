package com.example.smart_attendance_system;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    
    private PreviewView previewView;
    private ImageView overlayView;
    private Button captureButton, switchCameraButton, backButton;
    private TextView instructionText, statusText;
    
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    
    private boolean isBackCamera = true;
    private boolean faceDetected = false;
    private boolean isRegistration = false;
    private boolean isAttendance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initializeViews();
        getIntentData();
        setupClickListeners();
        
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        captureButton = findViewById(R.id.captureButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        backButton = findViewById(R.id.backButton);
        instructionText = findViewById(R.id.instructionText);
        statusText = findViewById(R.id.statusText);
        
        // Initially disable capture button
        captureButton.setEnabled(false);
        statusText.setText("Initializing camera...");
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            isRegistration = intent.getBooleanExtra(Constants.EXTRA_IS_REGISTRATION, false);
            isAttendance = intent.getBooleanExtra(Constants.EXTRA_IS_ATTENDANCE, false);
        }
        
        if (isRegistration) {
            instructionText.setText("📸 Face Registration\n\nPosition your face in the center and look directly at the camera");
        } else if (isAttendance) {
            instructionText.setText("🔐 Face Verification\n\nPosition your face in the center for attendance verification");
        } else {
            instructionText.setText("📷 Face Capture\n\nPosition your face in the center of the frame");
        }
    }

    private void setupClickListeners() {
        captureButton.setOnClickListener(v -> capturePhoto());
        
        switchCameraButton.setOnClickListener(v -> {
            isBackCamera = !isBackCamera;
            startCamera();
        });
        
        backButton.setOnClickListener(v -> finish());
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        // Camera selector
        CameraSelector cameraSelector = isBackCamera ? 
                CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(640, 480))
                .build();

        // Image analysis use case for face detection
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
            statusText.setText("Camera ready - Position your face in the frame");
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
            Toast.makeText(this, "Error binding camera: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeImage(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        
        if (mediaImage != null) {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            
            if (bitmap != null) {
                // Detect faces
                FaceRecognitionUtils.detectFaces(bitmap, new FaceRecognitionUtils.FaceDetectionCallback() {
                    @Override
                    public void onFaceDetected(List<Face> faces, Bitmap processedBitmap) {
                        runOnUiThread(() -> {
                            if (faces.size() == 1) {
                                Face face = faces.get(0);
                                if (FaceRecognitionUtils.isFaceQualityGood(face)) {
                                    faceDetected = true;
                                    captureButton.setEnabled(true);
                                    statusText.setText("✅ Face detected - Ready to capture!");
                                    statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                } else {
                                    faceDetected = false;
                                    captureButton.setEnabled(false);
                                    statusText.setText("⚠️ Face quality low - Improve lighting");
                                    statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                                }
                            } else if (faces.size() > 1) {
                                faceDetected = false;
                                captureButton.setEnabled(false);
                                statusText.setText("❌ Multiple faces detected - Only one person allowed");
                                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            } else {
                                onNoFaceDetected();
                            }
                        });
                    }

                    @Override
                    public void onNoFaceDetected() {
                        runOnUiThread(() -> {
                            faceDetected = false;
                            captureButton.setEnabled(false);
                            statusText.setText("👤 No face detected - Position your face in the frame");
                            statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Face detection error: " + error);
                            statusText.setText("❌ Face detection error");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        });
                    }
                });
            }
        }
        
        imageProxy.close();
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            Image.Plane[] planes = imageProxy.getImage().getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 
                    imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 
                    100, out);
            
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            
            if (!isBackCamera) {
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                matrix.postScale(-1, 1); // Mirror for front camera
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
                        bitmap.getHeight(), matrix, true);
            }
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    private void capturePhoto() {
        if (imageCapture == null || !faceDetected) {
            Toast.makeText(this, "Camera not ready or no face detected", Toast.LENGTH_SHORT).show();
            return;
        }

        captureButton.setEnabled(false);
        statusText.setText("📸 Capturing...");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                new java.io.File(getCacheDir(), "temp_face_" + System.currentTimeMillis() + ".jpg")
        ).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        // Load the captured image
                        if (output.getSavedUri() != null) {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeFile(output.getSavedUri().getPath());
                                if (bitmap != null) {
                                    processCapturedImage(bitmap);
                                } else {
                                    onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                            "Failed to decode captured image", null));
                                }
                            } catch (Exception e) {
                                onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                        "Error processing captured image", e));
                            }
                        } else {
                            onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                    "No output URI", null));
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        runOnUiThread(() -> {
                            statusText.setText("❌ Capture failed - Try again");
                            captureButton.setEnabled(true);
                            Toast.makeText(CameraActivity.this, 
                                    "Photo capture failed: " + exception.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void processCapturedImage(Bitmap bitmap) {
        // Convert bitmap to base64
        String base64Image = FaceRecognitionUtils.bitmapToBase64(bitmap);
        
        if (base64Image != null) {
            // Return result
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.EXTRA_FACE_CAPTURE_SUCCESS, true);
            resultIntent.putExtra(Constants.EXTRA_FACE_IMAGE_BASE64, base64Image);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            runOnUiThread(() -> {
                statusText.setText("❌ Error processing image - Try again");
                captureButton.setEnabled(true);
                Toast.makeText(this, "Error processing captured image", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}