package com.example.smart_attendance_system.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FaceEmbeddingUtils {
    private static final String TAG = "FaceEmbeddingUtils";
    private static final String MODEL_FILE = "mobilefacenet.tflite";
    
    // MobileFaceNet Input: 112x112, 3 channels (RGB)
    // MobileFaceNet Output: 192 float embedding
    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_SIZE = 192;
    
    private Interpreter tflite;

    public FaceEmbeddingUtils(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        tflite = new Interpreter(loadModelFile(context), options);
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] getFaceEmbedding(Bitmap faceBitmap) {
        if (tflite == null) {
            Log.e(TAG, "TFLite Interpreter is null");
            return null;
        }

        // 1. Preprocess Bitmap (Resize to 112x112 + Normalize)
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(faceBitmap);
        
        // 2. Run Inference
        float[][] outputEmbedding = new float[1][EMBEDDING_SIZE];
        tflite.run(inputBuffer, outputEmbedding);
        
        // 3. Return Embedding (Flattened)
        float[] embedding = outputEmbedding[0];

        // LOGGING FOR DEBUGGING
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, embedding.length); i++) {
            sb.append(String.format("%.4f, ", embedding[i]));
        }
        Log.d(TAG, "Embedding Sample: [" + sb.toString() + "...]");

        return embedding;
    }
    
    // Preprocess: Resize 112x112, Normalize (pixel - 128) / 128.0
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 1 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
        
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                
                // MobileFaceNet normalization: (x - 128) / 128.0
                // Correct logic: ((val - 128.0f) / 128.0f)
                // This produces range [-1.0, 1.0]

                float r = ((val >> 16) & 0xFF);
                float g = ((val >> 8) & 0xFF);
                float b = (val & 0xFF);

                byteBuffer.putFloat((r - 128.0f) / 128.0f);
                byteBuffer.putFloat((g - 128.0f) / 128.0f);
                byteBuffer.putFloat((b - 128.0f) / 128.0f);
            }
        }
        return byteBuffer;
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
    
    // Helper: Calculate Cosine Similarity
    public static float calculateCosineSimilarity(float[] emp1, float[] emp2) {
        if (emp1 == null || emp2 == null || emp1.length != emp2.length) return 0.0f;
        
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < emp1.length; i++) {
            dotProduct += emp1[i] * emp2[i];
            normA += emp1[i] * emp1[i];
            normB += emp2[i] * emp2[i];
        }
        
        if (normA == 0 || normB == 0) return 0.0f;
        
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    // Helper: Calculate Euclidean Distance (L2)
    public static float calculateL2Distance(float[] emp1, float[] emp2) {
        if (emp1 == null || emp2 == null || emp1.length != emp2.length) return Float.MAX_VALUE;
        
        float sum = 0.0f;
        for (int i = 0; i < emp1.length; i++) {
            float diff = emp1[i] - emp2[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }
}
