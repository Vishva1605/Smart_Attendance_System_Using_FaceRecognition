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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FaceRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "FaceRegistration";
    private static final int REQUEST_FACE_CAPTURE = 200;

    private TextView tv_instructions, tv_student_info, tv_registration_status;
    private ImageView iv_reference_face;
    private Button btn_capture_face, btn_register_face, btn_back;
    private ProgressBar progressBar;

    private String enrollmentNo;
    private String studentName;
    private String studentEmail;
    private String referenceFaceBase64;
    private boolean isFirstTimeRegistration = false;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        initializeViews();
        getStudentInfo();
        setupClickListeners();
        checkFirstTimeRegistration();
    }

    private void initializeViews() {
        tv_instructions = findViewById(R.id.tv_instructions);
        tv_student_info = findViewById(R.id.tv_student_info);
        tv_registration_status = findViewById(R.id.tv_registration_status);
        iv_reference_face = findViewById(R.id.iv_reference_face);
        btn_capture_face = findViewById(R.id.btn_capture_face);
        btn_register_face = findViewById(R.id.btn_register_face);
        btn_back = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progressBar);

        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        // Initially hide register button
        btn_register_face.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        tv_instructions.setText("📸 Face Registration\n\nRegister your face for secure attendance marking.\n\n" +
                "Instructions:\n" +
                "• Ensure good lighting\n" +
                "• Look directly at camera\n" +
                "• Keep face centered\n" +
                "• Avoid shadows on face");
    }
    
    private void checkFirstTimeRegistration() {
        Intent intent = getIntent();
        if (intent != null) {
            isFirstTimeRegistration = intent.getBooleanExtra(Constants.EXTRA_IS_FIRST_TIME_REGISTRATION, false);
        }
        
        if (isFirstTimeRegistration) {
            tv_instructions.setText("📸 Mandatory Face Registration\n\n" +
                    "This is your first login. Face registration is required for security.\n\n" +
                    "Instructions:\n" +
                    "• Ensure good lighting\n" +
                    "• Look directly at camera\n" +
                    "• Keep face centered\n" +
                    "• This can only be done once");
            
            // Disable back button for first time registration
            btn_back.setEnabled(false);
            btn_back.setText("Registration Required");
        }
    }

    private void getStudentInfo() {
        enrollmentNo = PreferenceManager.getEnrollmentNo(this);
        studentName = PreferenceManager.getStudentName(this);
        studentEmail = PreferenceManager.getStudentEmail(this);

        if (enrollmentNo == null || studentName == null) {
            Toast.makeText(this, "Student information not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String studentInfo = "👤 Student Information\n" +
                "Name: " + studentName + "\n" +
                "Enrollment: " + enrollmentNo + "\n" +
                "Email: " + studentEmail;

        tv_student_info.setText(studentInfo);
    }

    private void setupClickListeners() {
        btn_capture_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFace();
            }
        });

        btn_register_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerFace();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void captureFace() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(Constants.EXTRA_IS_REGISTRATION, true);
        startActivityForResult(intent, REQUEST_FACE_CAPTURE);
    }

    private void registerFace() {
        if (referenceFaceBase64 == null) {
            Toast.makeText(this, "Please capture your face first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btn_register_face.setEnabled(false);
        tv_registration_status.setText("🔄 Registering face...");

        // Save face data to Firebase
        Map<String, Object> faceData = new HashMap<>();
        faceData.put(Constants.STUDENT_FACE_DATA, referenceFaceBase64);
        faceData.put("face_registered_at", System.currentTimeMillis());
        faceData.put("face_registration_device", DeviceUtils.getDeviceId(this));

        studentsRef.child(enrollmentNo).updateChildren(faceData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        btn_register_face.setEnabled(true);

                        if (task.isSuccessful()) {
                            tv_registration_status.setText("✅ Face registered successfully!\n\nYou can now use face verification for attendance.");
                            Toast.makeText(FaceRegistrationActivity.this,
                                    isFirstTimeRegistration ? Constants.SUCCESS_FACE_REGISTRATION_FIRST_TIME : "Face registered successfully!", 
                                    Toast.LENGTH_SHORT).show();

                            // Update preferences to indicate face is registered
                            PreferenceManager.setFaceRegistered(FaceRegistrationActivity.this, true);
                            
                            if (isFirstTimeRegistration) {
                                PreferenceManager.setFirstLoginCompleted(FaceRegistrationActivity.this, true);
                            }

                            // Navigate back or to dashboard
                            finish();
                        } else {
                            tv_registration_status.setText("❌ Face registration failed. Please try again.");
                            Toast.makeText(FaceRegistrationActivity.this,
                                    "Face registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FACE_CAPTURE && resultCode == RESULT_OK && data != null) {
            boolean success = data.getBooleanExtra(Constants.EXTRA_FACE_CAPTURE_SUCCESS, false);

            if (success) {
                referenceFaceBase64 = data.getStringExtra(Constants.EXTRA_FACE_IMAGE_BASE64);

                if (referenceFaceBase64 != null) {
                    // Display captured face
                    Bitmap faceBitmap = FaceRecognitionUtils.base64ToBitmap(referenceFaceBase64);
                    if (faceBitmap != null) {
                        iv_reference_face.setImageBitmap(faceBitmap);
                        btn_register_face.setVisibility(View.VISIBLE);
                        tv_registration_status.setText("✅ Face captured successfully!\nClick 'Register Face' to complete registration.");
                    }
                } else {
                    Toast.makeText(this, "Error processing captured face", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Face capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isFirstTimeRegistration) {
            // Don't allow back press during mandatory registration
            Toast.makeText(this, "Face registration is required for first-time login", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}