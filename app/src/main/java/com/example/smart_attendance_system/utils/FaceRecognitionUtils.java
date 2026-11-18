package com.example.smart_attendance_system.utils;

import com.example.smart_attendance_system.utils.FaceEmbeddingUtils;
import com.example.smart_attendance_system.utils.Constants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for face recognition operations including detection, comparison, and image processing
 */
public class FaceRecognitionUtils {

    private static final String TAG = "FaceRecognitionUtils";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static FaceEmbeddingUtils faceEmbeddingUtils;
    private static Context appContext; // Store context for lazy init

    public static void initialize(Context context) {
        appContext = context.getApplicationContext(); // Prevent memory leaks
        try {
            if (faceEmbeddingUtils == null) {
                faceEmbeddingUtils = new FaceEmbeddingUtils(appContext);
                Log.d(TAG, "FaceEmbeddingUtils initialized successfully");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error initializing FaceEmbeddingUtils", e);
        }
    }

    // Face detection callback interface
    public interface FaceDetectionCallback {
        void onFaceDetected(List<Face> faces, Bitmap processedBitmap);
        void onNoFaceDetected();
        void onError(String error);
    }

    // Face comparison callback interface
    public interface FaceComparisonCallback {
        void onComparisonComplete(boolean isMatch, float confidence);
        void onError(String error);
    }

    /**
     * Detect faces in a bitmap using ML Kit Face Detection
     */
    public static void detectFaces(Bitmap bitmap, FaceDetectionCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                callback.onError("Input bitmap is null");
            }
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        detectFaces(image, callback);
    }

    /**
     * Detect faces in an InputImage using ML Kit Face Detection
     */
    public static void detectFaces(InputImage image, FaceDetectionCallback callback) {
        if (image == null) {
            if (callback != null) {
                callback.onError("Input image is null");
            }
            return;
        }

        try {
            // Configure face detector options
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Optimized for speed
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.15f)
                    //.enableTracking() // Tracking can be good but sometimes causes issues with single image detection
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> faces) {
                            if (faces != null && faces.size() > 0) {
                                Log.d(TAG, "Detected " + faces.size() + " face(s)");
                                if (callback != null) {
                                    // Pass null for bitmap since we don't always have one with InputImage
                                    // Use callback.onFaceDetected(faces, null) or refactor callback to not require bitmap
                                    // For now, we'll pass null and handle it in activity
                                    callback.onFaceDetected(faces, null);
                                }
                            } else {
                                Log.d(TAG, "No faces detected");
                                if (callback != null) {
                                    callback.onNoFaceDetected();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                            if (callback != null) {
                                callback.onError("Face detection failed: " + e.getMessage());
                            }
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in detectFaces: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Error initializing face detection: " + e.getMessage());
            }
        }
    }

    /**
     * Check if the detected face has good quality for recognition
     */
    public static boolean isFaceQualityGood(Face face) {
        if (face == null) {
            return false;
        }

        try {
            // Check face size (should be large enough for texture analysis)
            Rect boundingBox = face.getBoundingBox();
            int faceArea = boundingBox.width() * boundingBox.height();
            // Increased threshold for better resolution
            if (faceArea < 15000) { 
                Log.d(TAG, "Face too small: area = " + faceArea);
                return false;
            }

            // Check if face is purely frontal (Strict Mode)
            Float rotY = face.getHeadEulerAngleY(); // Left/Right
            Float rotZ = face.getHeadEulerAngleZ(); // Tilt

            // Strict 15 degree limit
            if (rotY != null && Math.abs(rotY) > 12) {
                Log.d(TAG, "Face rotation Y too high: " + rotY);
                return false;
            }

            if (rotZ != null && Math.abs(rotZ) > 12) {
                Log.d(TAG, "Face rotation Z too high: " + rotZ);
                return false;
            }

            // Check eye open probability - High threshold to ensure alertness
            Float leftEyeOpenProb = face.getLeftEyeOpenProbability();
            Float rightEyeOpenProb = face.getRightEyeOpenProbability();

            if (leftEyeOpenProb != null && rightEyeOpenProb != null) {
                if (leftEyeOpenProb < 0.2f || rightEyeOpenProb < 0.2f) {
                     Log.d(TAG, "Eyes potentially closed");
                     // return false; // Still optional, as glasses can interfere
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error checking face quality: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get face quality score (0.0 to 1.0)
     */
    public static float getFaceQualityScore(Face face) {
        if (face == null) {
            return 0.0f;
        }

        try {
            float score = 1.0f;

            // Factor in face size
            Rect boundingBox = face.getBoundingBox();
            int faceArea = boundingBox.width() * boundingBox.height();
            if (faceArea < 20000) {
                score *= 0.7f;
            }

            // Factor in head rotation
            Float rotY = face.getHeadEulerAngleY();
            Float rotZ = face.getHeadEulerAngleZ();

            if (rotY != null) {
                score *= Math.max(0.3f, 1.0f - Math.abs(rotY) / 45.0f);
            }

            if (rotZ != null) {
                score *= Math.max(0.3f, 1.0f - Math.abs(rotZ) / 30.0f);
            }

            // Factor in eye open probability
            Float leftEyeOpenProb = face.getLeftEyeOpenProbability();
            Float rightEyeOpenProb = face.getRightEyeOpenProbability();

            if (leftEyeOpenProb != null) {
                score *= leftEyeOpenProb;
            }

            if (rightEyeOpenProb != null) {
                score *= rightEyeOpenProb;
            }

            return Math.max(0.0f, Math.min(1.0f, score));

        } catch (Exception e) {
            Log.e(TAG, "Error calculating face quality score: " + e.getMessage(), e);
            return 0.5f;
        }
    }

    /**
     * Extract face region from bitmap
     */
    public static Bitmap extractFace(Bitmap bitmap, Face face) {
        if (bitmap == null || face == null) {
            return null;
        }

        try {
            Rect boundingBox = face.getBoundingBox();

            // Add some padding around the face
            int padding = Math.min(boundingBox.width(), boundingBox.height()) / 4;
            int left = Math.max(0, boundingBox.left - padding);
            int top = Math.max(0, boundingBox.top - padding);
            int right = Math.min(bitmap.getWidth(), boundingBox.right + padding);
            int bottom = Math.min(bitmap.getHeight(), boundingBox.bottom + padding);

            int width = right - left;
            int height = bottom - top;

            if (width > 0 && height > 0) {
                return Bitmap.createBitmap(bitmap, left, top, width, height);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting face: " + e.getMessage(), e);
        }

        return bitmap; // Return original bitmap if extraction fails
    }



    /**
     * Generate Spatial LBP Histogram Embedding
     * Workflow: Resize -> Grayscale -> Grid Division -> LBP -> Histogram -> Concatenate
     */
    private static float[] generateLBPEmbedding(Bitmap bitmap) {
        try {
            // 1. Resize to standard size (128x128)
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 128, 128, true);
            int width = scaled.getWidth();
            int height = scaled.getHeight();

            // 2. Convert to Grayscale Array
            int[] pixels = new int[width * height];
            scaled.getPixels(pixels, 0, width, 0, 0, width, height);
            int[] grayPixels = new int[width * height];

            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                // Luminance formula
                grayPixels[i] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }

            // 3. Compute LBP Image
            // We can compute histograms directly, but let's be explicit
            // 4x4 Grid = 16 regions.
            // Each region is 32x32 pixels.
            int gridRows = 4;
            int gridCols = 4;
            int blockW = width / gridCols;
            int blockH = height / gridRows;

            // Total Histogram size = 16 regions * 256 bins = 4096 floats
            float[] embedding = new float[gridRows * gridCols * 256];
            int embedIdx = 0;

            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    
                    // Process this block
                    int startX = col * blockW;
                    int startY = row * blockH;
                    
                    // 256-bin histogram for this block
                    float[] hist = new float[256];

                    // Iterate pixels in block (avoiding boundary)
                    for (int y = startY + 1; y < startY + blockH - 1; y++) {
                        for (int x = startX + 1; x < startX + blockW - 1; x++) {
                            
                            int center = grayPixels[y * width + x];
                            int code = 0;

                            // Standard 3x3 LBP Neighborhood
                            // 7 0 1
                            // 6 C 2
                            // 5 4 3
                            
                            code |= (grayPixels[(y-1) * width + (x)]   >= center ? 1 : 0) << 0; // Top
                            code |= (grayPixels[(y-1) * width + (x+1)] >= center ? 1 : 0) << 1; // Top-Right
                            code |= (grayPixels[(y)   * width + (x+1)] >= center ? 1 : 0) << 2; // Right
                            code |= (grayPixels[(y+1) * width + (x+1)] >= center ? 1 : 0) << 3; // Bottom-Right
                            code |= (grayPixels[(y+1) * width + (x)]   >= center ? 1 : 0) << 4; // Bottom
                            code |= (grayPixels[(y+1) * width + (x-1)] >= center ? 1 : 0) << 5; // Bottom-Left
                            code |= (grayPixels[(y)   * width + (x-1)] >= center ? 1 : 0) << 6; // Left
                            code |= (grayPixels[(y-1) * width + (x-1)] >= center ? 1 : 0) << 7; // Top-Left

                            hist[code]++;
                        }
                    }

                    // Normalize block histogram to handle lighting intensity variations
                    float sum = 0;
                    for (float v : hist) sum += v;
                    if (sum > 0) {
                        for (int k = 0; k < 256; k++) {
                            embedding[embedIdx++] = hist[k] / sum;
                        }
                    } else {
                        embedIdx += 256; // Skip empty block (shouldn't happen)
                    }
                }
            }
            return embedding;

        } catch (Exception e) {
            Log.e(TAG, "LBP Generation Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Chi-Squared Distance Statistic for Histogram Matching
     * D(x, y) = sum( (xi - yi)^2 / (xi + yi) )
     */
    private static float calculateChiSquareDistance(float[] hist1, float[] hist2) {
        if (hist1.length != hist2.length) return Float.MAX_VALUE;

        float distance = 0.0f;
        for (int i = 0; i < hist1.length; i++) {
            float h1 = hist1[i];
            float h2 = hist2[i];
            float sum = h1 + h2;
            
            if (sum > 1e-6) { // Avoid divide by zero
                distance += ((h1 - h2) * (h1 - h2)) / sum;
            }
        }
        return distance;
    }

    /**
     * Convert bitmap to base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.FACE_JPEG_QUALITY, baos);
            byte[] byteArray = baos.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to base64: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert base64 string to bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting base64 to bitmap: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Rotate bitmap based on EXIF orientation
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, Uri imageUri, Context context) {
        if (bitmap == null || imageUri == null || context == null) {
            return bitmap;
        }

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return bitmap;
            }

            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (IOException e) {
            Log.e(TAG, "Error reading EXIF data: " + e.getMessage(), e);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error rotating bitmap: " + e.getMessage(), e);
            return bitmap;
        }
    }

    /**
     * Resize bitmap while maintaining aspect ratio
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) {
            return null;
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width <= maxWidth && height <= maxHeight) {
                return bitmap;
            }

            float scaleWidth = (float) maxWidth / width;
            float scaleHeight = (float) maxHeight / height;
            float scale = Math.min(scaleWidth, scaleHeight);

            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        } catch (Exception e) {
            Log.e(TAG, "Error resizing bitmap: " + e.getMessage(), e);
            return bitmap;
        }
    }

    /**
     * Clean up resources
     */
    public static void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Simulate face verification for testing purposes
     * This method provides realistic confidence scores for development
     */
    /**
     * Compare two face bitmaps using Hybrid Approach: LBP (Texture) + Geometry (Biometrics)
     */
    /**
     * Compare two face bitmaps using Deep Learning (MobileFaceNet) Embeddings
     */
    public static void compareFaces(Bitmap referenceFace, Bitmap capturedFace, FaceComparisonCallback callback) {
        if (referenceFace == null || capturedFace == null) {
            if (callback != null) callback.onError("One or both face images are null");
            return;
        }

        // Lazy Initialization fallback
        if (faceEmbeddingUtils == null && appContext != null) {
            initialize(appContext);
        }

        if (faceEmbeddingUtils == null) {
             Log.e(TAG, "Face matcher not initialized and lazy init failed");
             if (callback != null) callback.onError("Face matcher not initialized");
             return;
        }

        executorService.execute(() -> {
            try {
                // 1. Get Embeddings (192-float vector)
                float[] refEmbedding = faceEmbeddingUtils.getFaceEmbedding(referenceFace);
                float[] capEmbedding = faceEmbeddingUtils.getFaceEmbedding(capturedFace);

                if (refEmbedding == null || capEmbedding == null) {
                    if (callback != null) callback.onError("Failed to extract face features");
                    return;
                }

                // 2. Calculate Similarity (Cosine Similarity)
                // Range: -1.0 to 1.0 (1.0 is identical)
                float similarity = FaceEmbeddingUtils.calculateCosineSimilarity(refEmbedding, capEmbedding);
                
                // 3. Thresholding for MobileFaceNet
                // Typical Thresholds:
                // > 0.8 : Strong Match
                // > 0.6 : Possible Match
                // < 0.4 : Different Person
                
                // We set a moderate bar for attendance
                float threshold = 0.75f; 
                boolean isMatch = similarity > threshold;

                Log.d(TAG, String.format("Deep Learning Match: Similarity=%.4f, Threshold=%.2f, Match=%b", 
                        similarity, threshold, isMatch));

                if (callback != null) {
                    callback.onComparisonComplete(isMatch, similarity);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in Deep Learning comparison: " + e.getMessage(), e);
                if (callback != null) callback.onError("Comparison failed: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronous face detection for internal use in comparison
     */
    private static Face detectFaceSynchronously(Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .build();
            FaceDetector detector = FaceDetection.getClient(options);
            
            // Blocking call for background thread
            List<Face> faces = com.google.android.gms.tasks.Tasks.await(detector.process(image));
            if (faces != null && !faces.isEmpty()) {
                return faces.get(0); // Assume largest/primary face
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync detection failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calculate Biometric Geometric Ratios
     * Returns array of ratios [ref1, ref2, ref3...]
     */
    private static float[] calculateGeometricRatios(Face face, int imgWidth, int imgHeight) {
        try {
            // We need specific landmarks
            // Note: ML Kit defines landmarks, but we need to check if they exist
            // Landmarks: Left Eye, Right Eye, Nose Base, Mouth Left, Mouth Right
            
            // Getting coordinates is complex because ML Kit Face Landmark API returns raw points.
            // We use relative positions to be scale-invariant.

            // Key Biometric Features:
            // 1. Inter-ocular distance (dist between pupil centers)
            // 2. Eye-to-Nose distance
            // 3. Eye-to-Mouth distance
            // 4. Nose-to-Mouth distance
            // 5. Mouth Width
            
            // We normalize all distances by "Face Width" (bounding box width) or "Inter-ocular distance" 
            // to make it scale-invariant.
            
            // For robustness, we'll use BoundingBox width as the primary normalizer.
            float scale = (float) face.getBoundingBox().width();
            if (scale == 0) return null;

            // Approximate landmark positions from the detailed face mesh or available landmarks
            // ML Kit usually gives: LEFT_EYE, RIGHT_EYE, NOSE_BASE, MOUTH_LEFT, MOUTH_RIGHT, etc.
            
            if (face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE) == null ||
                face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE) == null ||
                face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE) == null ||
                face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM) == null) {
                    return null; // Missing critical landmarks
            }

            android.graphics.PointF leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE).getPosition();
            android.graphics.PointF rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE).getPosition();
            android.graphics.PointF nose = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE).getPosition();
            android.graphics.PointF mouth = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM).getPosition();

            // Distances
            float distEyes = calculateDistance(leftEye, rightEye);
            float distLeftEyeNose = calculateDistance(leftEye, nose);
            float distRightEyeNose = calculateDistance(rightEye, nose);
            float distNoseMouth = calculateDistance(nose, mouth);
            
            // Ratios (features)
            float[] features = new float[4];
            features[0] = distEyes / scale;                 // Normalized Eye Distance
            features[1] = distLeftEyeNose / scale;          // Left Eye-Nose Ratio
            features[2] = distRightEyeNose / scale;         // Right Eye-Nose Ratio
            features[3] = distNoseMouth / scale;            // Nose-Mouth Ratio
            
            return features;

        } catch (Exception e) {
            Log.e(TAG, "Error calc geometry: " + e.getMessage());
            return null;
        }
    }

    private static float calculateDistance(android.graphics.PointF p1, android.graphics.PointF p2) {
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Compare two geometric feature vectors
     * Returns similarity score 0.0 to 1.0
     */
    private static float calculateGeometricSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) return 0;
        
        float totalDiff = 0;
        for (int i=0; i<vec1.length; i++) {
            float diff = Math.abs(vec1[i] - vec2[i]);
            // Weigh the difference. 
            // If difference is 0.05 (5% of face width), that's significant.
            // We want to penalize heavily.
            totalDiff += diff;
        }
        
        // Average difference
        float avgDiff = totalDiff / vec1.length;
        
        // Mapping: 
        // 0.00 diff -> 1.0 score
        // 0.05 diff -> 0.5 score
        // 0.10 diff -> 0.0 score
        
        float score = 1.0f - (avgDiff * 10.0f); // Steep penalty
        return Math.max(0.0f, score);
    }

    /**
     * Release TFLite resources to prevent memory leaks
     */
    public static void cleanup() {
        if (faceEmbeddingUtils != null) {
            faceEmbeddingUtils.close();
            faceEmbeddingUtils = null;
            Log.d(TAG, "FaceEmbeddingUtils resources cleaned up");
        }
    }
}



















