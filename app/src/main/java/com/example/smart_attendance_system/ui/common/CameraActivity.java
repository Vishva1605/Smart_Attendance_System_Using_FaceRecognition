package com.example.smart_attendance_system.ui.common;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.utils.FaceRecognitionUtils;
import com.example.smart_attendance_system.utils.Constants;

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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
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
    private boolean isCapturing = false;

    // --- Liveness Detection Variables ---
    private enum LivenessState {
        NONE,           // Not active (e.g. registration)
        START,          // Waiting for steady face
        CHALLENGE_1,    // First random action
        CHALLENGE_2,    // Second random action
        PASSED,         // Success, taking photo
        FAILED
    }

    private enum LivenessAction {
        TURN_HEAD_LEFT,
        TURN_HEAD_RIGHT,
        SMILE,
        BLINK
    }

    private LivenessState livenessState = LivenessState.NONE;
    private LivenessAction currentAction = null;
    private long actionStartTime = 0;
    private int challengesCompleted = 0;
    private static final long CHALLENGE_TIMEOUT = 5000; // 5 seconds per challenge
    private static final int REQUIRED_CHALLENGES = 2;

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
            livenessState = LivenessState.NONE;
        } else if (isAttendance) {
            instructionText.setText("🔐 Face Verification\n\nStarting Liveness Check...");
            livenessState = LivenessState.START;
            challengesCompleted = 0;
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
                .setTargetResolution(new Size(1280, 720)) // Increased resolution to HD
                .build();

        // Image analysis use case for face detection
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720)) // Increased resolution to HD
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
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            
            FaceRecognitionUtils.detectFaces(image, new FaceRecognitionUtils.FaceDetectionCallback() {
                @Override
                public void onFaceDetected(List<Face> faces, Bitmap processedBitmap) {
                    runOnUiThread(() -> {
                        // CRITICAL FIX: If already passed liveness or capturing, IGNORE updates
                        // This prevents "No Face Detected" from overwriting "Liveness Verified"
                        if (livenessState == LivenessState.PASSED || isCapturing) {
                            return; 
                        }

                        if (faces.size() == 1) {
                            Face face = faces.get(0);
                            
                            // Registration Mode: Simple Quality Check
                            if (isRegistration) {
                                if (FaceRecognitionUtils.isFaceQualityGood(face)) {
                                    faceDetected = true;
                                    statusText.setText("✅ Face detected - Ready to capture!");
                                    statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                    if (!isCapturing) captureButton.setEnabled(true);
                                } else {
                                    faceDetected = false;
                                    statusText.setText("⚠️ Improve lighting / Look straight");
                                    statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                                    captureButton.setEnabled(false);
                                }
                            } 
                            // Attendance Mode: Active Liveness Check
                            else if (isAttendance) {
                                // Relaxed quality check for liveness (allow more movement)
                                // We check specific angles in processLivenessLogic anyway
                                if (face.getBoundingBox().width() * face.getBoundingBox().height() > 10000) {
                                     processLivenessLogic(face);
                                } else {
                                     statusText.setText("Move closer");
                                }
                            }
                            // Other modes
                            else {
                                faceDetected = true;
                                captureButton.setEnabled(true);
                            }
                            
                        } else if (faces.size() > 1) {
                            statusText.setText("❌ Multiple faces detected!");
                            captureButton.setEnabled(false);
                        } else {
                            onNoFaceDetected();
                        }
                    });
                    imageProxy.close();
                }

                @Override
                public void onNoFaceDetected() {
                    runOnUiThread(() -> {
                         // CRITICAL FIX: Ignore "No Face" if we are already successfully capturing
                        if (livenessState == LivenessState.PASSED || isCapturing) {
                            return; 
                        }

                        faceDetected = false;
                        captureButton.setEnabled(false);
                        statusText.setText("👤 Position face in frame");
                        statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        
                        if (isAttendance && livenessState != LivenessState.PASSED) {
                           // livenessState = LivenessState.START; // Optional: Reset on loss
                        }
                    });
                    imageProxy.close();
                }

                @Override
                public void onError(String error) {
                    imageProxy.close();
                }
            });
        } else {
            imageProxy.close();
        }
    }

    private void processLivenessLogic(Face face) {
        if (livenessState == LivenessState.PASSED || isCapturing) return;

        long currentTime = System.currentTimeMillis();

        switch (livenessState) {
            case START:
                // Ensure face is steady and frontal before starting challenges
                if (Math.abs(face.getHeadEulerAngleY()) < 10 && Math.abs(face.getHeadEulerAngleZ()) < 10) {
                     startNextChallenge();
                } else {
                    instructionText.setText("Please look straight at the camera");
                    statusText.setText("Align face to start");
                }
                break;

            case CHALLENGE_1:
            case CHALLENGE_2:
                checkChallengeCompletion(face, currentTime);
                break;
        }
    }

    private void startNextChallenge() {
        if (challengesCompleted >= REQUIRED_CHALLENGES) {
            livenessState = LivenessState.PASSED;
            instructionText.setText("✅ Liveness Verified!");
            statusText.setText("Hold still for capture...");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            captureButton.setEnabled(false); 
            
            // Force capture on main thread with a 1.5s delay to allow face centering
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::capturePhoto, 1500);
            return;
        }

        // Select Random Challenge
        double rand = Math.random();
        if (rand < 0.33) {
            currentAction = LivenessAction.TURN_HEAD_LEFT;
            instructionText.setText("⬅️ Slowly turn head LEFT");
            livenessState = (challengesCompleted == 0) ? LivenessState.CHALLENGE_1 : LivenessState.CHALLENGE_2;
        } else if (rand < 0.66) {
            currentAction = LivenessAction.TURN_HEAD_RIGHT;
            instructionText.setText("➡️ Slowly turn head RIGHT");
            livenessState = (challengesCompleted == 0) ? LivenessState.CHALLENGE_1 : LivenessState.CHALLENGE_2;
        } else {
            currentAction = LivenessAction.SMILE;
            instructionText.setText("😄 Smile widely!");
            livenessState = (challengesCompleted == 0) ? LivenessState.CHALLENGE_1 : LivenessState.CHALLENGE_2;
        }
        
        actionStartTime = System.currentTimeMillis();
        statusText.setText("Perform action...");
    }

    private void checkChallengeCompletion(Face face, long currentTime) {
        // Timeout Logic
        if (currentTime - actionStartTime > CHALLENGE_TIMEOUT) {
            instructionText.setText("❌ Too slow! Restarting...");
            livenessState = LivenessState.START;
            challengesCompleted = 0;
            return;
        }

        boolean passed = false;
        float rotY = face.getHeadEulerAngleY(); // negative is Right ?? (Check API) -> Left is Positive usually for CameraX/MLKit
        // ML Kit: Y is left/right. Positive is Left (from user perspective looking at screen? Need verification. usually user's left is positive)
        
        Float smileProb = face.getSmilingProbability();
        
        switch (currentAction) {
            case TURN_HEAD_LEFT: // Expect Positive Y (Standard coordinate system for front camera mirroring can vary)
                // Relaxed threshold and check absolute value direction
                if (rotY > 15) passed = true; 
                break;
            case TURN_HEAD_RIGHT: // Expect Negative Y
                if (rotY < -15) passed = true;
                break;
            case SMILE:
                // Lower threshold for easier smiling
                if (smileProb != null && smileProb > 0.6) passed = true; 
                break;
        }

        if (passed) {
            challengesCompleted++;
            instructionText.setText("✅ Good!");
            // Short delay or immediate next
            startNextChallenge();
        }
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
        // Allow capture if Face is detected OR Liveness is Passed (bypass face check for auto-capture)
        boolean canCapture = faceDetected || (livenessState == LivenessState.PASSED);
        
        if (imageCapture == null || !canCapture || isCapturing) {
            // Only show toast if it's a manual press. For auto-capture, we just log/ignore
            if (!isAttendance) {
                Toast.makeText(this, "Camera not ready or no face detected", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        isCapturing = true;
        captureButton.setEnabled(false);
        statusText.setText("📸 Capturing...");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                new java.io.File(getCacheDir(), "temp_face_" + System.currentTimeMillis() + ".jpg")
        ).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        statusText.setText("🔄 Processing...");
                        
                        // Move heavy processing to background thread to prevent ANR
                        cameraExecutor.execute(() -> {
                            if (output.getSavedUri() != null) {
                                try {
                                    // Load the captured image
                                    Bitmap bitmap = BitmapFactory.decodeFile(output.getSavedUri().getPath());
                                    
                                    if (bitmap != null) {
                                        // Rotate bitmap if needed (important for front camera)
                                        // We use the URI to get EXIF data for correct rotation
                                        Bitmap rotatedBitmap = FaceRecognitionUtils.rotateBitmap(bitmap, output.getSavedUri(), CameraActivity.this);
                                        
                                        // Process the rotated bitmap
                                        processCapturedImage(rotatedBitmap);
                                    } else {
                                        runOnUiThread(() -> {
                                            onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                                    "Failed to decode captured image", null));
                                        });
                                    }
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                                "Error processing captured image", e));
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    onError(new ImageCaptureException(ImageCapture.ERROR_FILE_IO, 
                                            "No output URI", null));
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        runOnUiThread(() -> {
                            statusText.setText("❌ Capture failed - Try again");
                            captureButton.setEnabled(true);
                            isCapturing = false;
                            Toast.makeText(CameraActivity.this, 
                                    "Photo capture failed: " + exception.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void processCapturedImage(Bitmap bitmap) {
        // This runs on background thread now
        
        File photoFile = new File(getCacheDir(), "captured_face_" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(photoFile);
            // Compress to JPEG (quality 80)
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.FACE_JPEG_QUALITY, fos);
            fos.flush();
            fos.close();
            
            final String filePath = photoFile.getAbsolutePath();
            
            // Also convert to Base64 (optional, but might be needed by some activities if small enough)
            // But we prefer path to avoid TransactionTooLargeException
            // String base64Image = FaceRecognitionUtils.bitmapToBase64(bitmap);
            
            runOnUiThread(() -> {
                // Return result with path
                Intent resultIntent = new Intent();
                resultIntent.putExtra(Constants.EXTRA_FACE_CAPTURE_SUCCESS, true);
                resultIntent.putExtra(Constants.EXTRA_FACE_IMAGE_PATH, filePath);
                // resultIntent.putExtra(Constants.EXTRA_FACE_IMAGE_BASE64, base64Image); // Commented out to prevent crash
                setResult(RESULT_OK, resultIntent);
                finish();
            });
            
        } catch (IOException e) {
             Log.e(TAG, "Error saving captured face: " + e.getMessage(), e);
             runOnUiThread(() -> {
                statusText.setText("❌ Error saving image - Try again");
                captureButton.setEnabled(true);
                isCapturing = false; // Reset flag
                Toast.makeText(CameraActivity.this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
