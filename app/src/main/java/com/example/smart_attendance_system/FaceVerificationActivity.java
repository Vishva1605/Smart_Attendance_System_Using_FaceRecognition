package com.example.smart_attendance_system;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FaceVerificationActivity extends AppCompatActivity {

    private static final String TAG = "FaceVerification";
    private static final int REQUEST_FACE_CAPTURE = 300;

    private TextView tv_instructions, tv_student_info, tv_verification_status;
    private ImageView iv_reference_face, iv_current_face;
    private Button btn_capture_face, btn_verify_again, btn_back;
    private ProgressBar progressBar;

    private String enrollmentNo;
    private String studentName;
    private String referenceFaceBase64;
    private String currentFaceBase64;
    private String sessionId;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);

        initializeViews();
        getIntentData();
        getStudentInfo();
        setupClickListeners();
        loadReferenceFace();
    }

    private void initializeViews() {
        tv_instructions = findViewById(R.id.tv_instructions);
        tv_student_info = findViewById(R.id.tv_student_info);
        tv_verification_status = findViewById(R.id.tv_verification_status);
        iv_reference_face = findViewById(R.id.iv_reference_face);
        iv_current_face = findViewById(R.id.iv_current_face);
        btn_capture_face = findViewById(R.id.btn_capture_face);
        btn_verify_again = findViewById(R.id.btn_verify_again);
        btn_back = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progressBar);

        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        // Initially hide some elements
        btn_verify_again.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        tv_instructions.setText("🔐 Face Verification for Attendance\n\n" +
                "Verify your identity to mark attendance.\n\n" +
                "Instructions:\n" +
                "• Ensure good lighting\n" +
                "• Look directly at camera\n" +
                "• Match your registered pose\n" +
                "• Keep face centered");
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            sessionId = intent.getStringExtra(Constants.EXTRA_SESSION_ID);
        }
    }

    private void getStudentInfo() {
        enrollmentNo = PreferenceManager.getEnrollmentNo(this);
        studentName = PreferenceManager.getStudentName(this);

        if (enrollmentNo == null || studentName == null) {
            Toast.makeText(this, "Student information not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String studentInfo = "👤 Student: " + studentName + "\n" +
                "Enrollment: " + enrollmentNo;

        tv_student_info.setText(studentInfo);
    }

    private void setupClickListeners() {
        btn_capture_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFace();
            }
        });

        btn_verify_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetVerification();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadReferenceFace() {
        progressBar.setVisibility(View.VISIBLE);
        tv_verification_status.setText("🔄 Loading registered face...");

        studentsRef.child(enrollmentNo).child(Constants.STUDENT_FACE_DATA)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        progressBar.setVisibility(View.GONE);

                        if (dataSnapshot.exists()) {
                            referenceFaceBase64 = dataSnapshot.getValue(String.class);

                            if (referenceFaceBase64 != null) {
                                Bitmap referenceBitmap = FaceRecognitionUtils.base64ToBitmap(referenceFaceBase64);
                                if (referenceBitmap != null) {
                                    iv_reference_face.setImageBitmap(referenceBitmap);
                                    tv_verification_status.setText("✅ Registered face loaded. Capture your current photo to verify.");
                                    btn_capture_face.setEnabled(true);
                                } else {
                                    tv_verification_status.setText("❌ Error loading registered face image.");
                                    btn_capture_face.setEnabled(false);
                                }
                            } else {
                                tv_verification_status.setText("❌ No registered face data found.");
                                btn_capture_face.setEnabled(false);
                            }
                        } else {
                            tv_verification_status.setText("❌ No face registered. Please register your face first.");
                            btn_capture_face.setEnabled(false);

                            // Offer to go to registration
                            Toast.makeText(FaceVerificationActivity.this,
                                    "Please register your face first", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        tv_verification_status.setText("❌ Error loading registered face: " + error.getMessage());
                        Log.e(TAG, "Error loading reference face: " + error.getMessage());
                    }
                });
    }

    private void captureFace() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(Constants.EXTRA_IS_ATTENDANCE, true);
        startActivityForResult(intent, REQUEST_FACE_CAPTURE);
    }

    private void resetVerification() {
        iv_current_face.setImageResource(android.R.color.transparent);
        btn_verify_again.setVisibility(View.GONE);
        btn_capture_face.setVisibility(View.VISIBLE);
        tv_verification_status.setText("✅ Registered face loaded. Capture your current photo to verify.");
        currentFaceBase64 = null;
    }

    private void performFaceVerification() {
        if (referenceFaceBase64 == null || currentFaceBase64 == null) {
            Toast.makeText(this, "Missing face data for verification", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tv_verification_status.setText("🔄 Verifying face...");

        // Convert base64 to bitmaps
        Bitmap referenceBitmap = FaceRecognitionUtils.base64ToBitmap(referenceFaceBase64);
        Bitmap currentBitmap = FaceRecognitionUtils.base64ToBitmap(currentFaceBase64);

        if (referenceBitmap == null || currentBitmap == null) {
            progressBar.setVisibility(View.GONE);
            tv_verification_status.setText("❌ Error processing face images.");
            return;
        }

        // Perform face comparison
        FaceRecognitionUtils.compareFaces(referenceBitmap, currentBitmap,
                new FaceRecognitionUtils.FaceComparisonCallback() {
                    @Override
                    public void onComparisonComplete(boolean isMatch, float confidence) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            if (isMatch) {
                                tv_verification_status.setText(String.format(
                                        "✅ Face Verification Successful!\n\nConfidence: %.1f%%\n\nProceeding to mark attendance...",
                                        confidence * 100));

                                // Proceed to mark attendance
                                proceedToAttendance();
                            } else {
                                tv_verification_status.setText(String.format(
                                        "❌ Face Verification Failed\n\nConfidence: %.1f%%\n\nPlease try again with better lighting and positioning.",
                                        confidence * 100));

                                btn_verify_again.setVisibility(View.VISIBLE);
                                btn_capture_face.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tv_verification_status.setText("❌ Verification error: " + error);
                            btn_verify_again.setVisibility(View.VISIBLE);
                            btn_capture_face.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private void proceedToAttendance() {
        // Navigate to attendance marking with verification success
        Intent intent = new Intent(this, SelectAttendanceActivity.class);
        intent.putExtra(Constants.EXTRA_FACE_VERIFIED, true);
        intent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FACE_CAPTURE && resultCode == RESULT_OK && data != null) {
            boolean success = data.getBooleanExtra(Constants.EXTRA_FACE_CAPTURE_SUCCESS, false);

            if (success) {
                currentFaceBase64 = data.getStringExtra(Constants.EXTRA_FACE_IMAGE_BASE64);

                if (currentFaceBase64 != null) {
                    // Display captured face
                    Bitmap currentBitmap = FaceRecognitionUtils.base64ToBitmap(currentFaceBase64);
                    if (currentBitmap != null) {
                        iv_current_face.setImageBitmap(currentBitmap);

                        // Start verification
                        performFaceVerification();
                    }
                } else {
                    Toast.makeText(this, "Error processing captured face", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Face capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}