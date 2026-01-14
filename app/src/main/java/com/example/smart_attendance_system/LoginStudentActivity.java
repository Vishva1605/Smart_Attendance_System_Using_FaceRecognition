package com.example.smart_attendance_system;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginStudentActivity extends AppCompatActivity implements TextWatcher {

    private static final String TAG = "LoginStudent";

    private TextInputLayout til_email, til_password;
    private Button btn_login, btn_back;
    private String loginEmail, loginPassword;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private DatabaseReference studentsRef;
    private String enrollmentNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_student);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btn_login = findViewById(R.id.btn_login);
        btn_back = findViewById(R.id.btn_back);
        til_email = findViewById(R.id.til_email);
        til_password = findViewById(R.id.til_password);

        // Add text watchers
        if (til_email.getEditText() != null) {
            til_email.getEditText().addTextChangedListener(this);
        }
        if (til_password.getEditText() != null) {
            til_password.getEditText().addTextChangedListener(this);
        }

        mAuth = FirebaseAuth.getInstance();
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);
    }

    private void setupClickListeners() {
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void attemptLogin() {
        if (!validateInputs()) {
            return;
        }

        loginEmail = til_email.getEditText().getText().toString().trim();
        loginPassword = til_password.getEditText().getText().toString().trim();

        showProgressDialog("Authenticating student...");

        mAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Student authentication successful");
                            searchStudentRecord();
                        } else {
                            closeProgressDialog();
                            handleAuthenticationError(task.getException());
                        }
                    }
                });
    }

    private void searchStudentRecord() {
        studentsRef.orderByChild(Constants.STUDENT_EMAIL)
                .equalTo(loginEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                                enrollmentNo = studentSnapshot.getKey();
                                Log.d(TAG, "Student found with enrollment: " + enrollmentNo);

                                String currentHardwareId = DeviceUtils.getDeviceId(LoginStudentActivity.this);
                                validateDeviceAndProceed(enrollmentNo, currentHardwareId, studentSnapshot);
                                break;
                            }
                        } else {
                            closeProgressDialog();
                            mAuth.signOut();
                            Toast.makeText(LoginStudentActivity.this,
                                    Constants.ERROR_STUDENT_NOT_FOUND, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        closeProgressDialog();
                        Log.e(TAG, "Database error: " + error.getMessage());
                        Toast.makeText(LoginStudentActivity.this,
                                "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void validateDeviceAndProceed(String enrollmentNo, String currentHardwareId, DataSnapshot studentSnapshot) {
        String storedHardwareId = studentSnapshot.child(Constants.STUDENT_HARDWARE_ID).getValue(String.class);

        if (storedHardwareId == null || storedHardwareId.trim().isEmpty()) {
            // First time login - store hardware ID
            studentsRef.child(enrollmentNo).child(Constants.STUDENT_HARDWARE_ID).setValue(currentHardwareId)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            closeProgressDialog();
                            if (task.isSuccessful()) {
                                saveStudentInfoAndProceed(enrollmentNo, studentSnapshot);
                            } else {
                                Toast.makeText(LoginStudentActivity.this,
                                        "Failed to register device", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else if (currentHardwareId.equals(storedHardwareId)) {
            // Same device - allow login
            closeProgressDialog();
            saveStudentInfoAndProceed(enrollmentNo, studentSnapshot);
        } else {
            // Different device - block login
            closeProgressDialog();
            mAuth.signOut();
            Toast.makeText(LoginStudentActivity.this,
                    Constants.ERROR_DEVICE_NOT_AUTHORIZED, Toast.LENGTH_LONG).show();
        }
    }

    private void saveStudentInfoAndProceed(String enrollmentNo, DataSnapshot studentSnapshot) {
        // Save student info to preferences
        String studentName = studentSnapshot.child(Constants.STUDENT_NAME).getValue(String.class);
        String branch = studentSnapshot.child(Constants.STUDENT_BRANCH).getValue(String.class);
        String year = studentSnapshot.child(Constants.STUDENT_YEAR).getValue(String.class);
        String section = studentSnapshot.child(Constants.STUDENT_SECTION).getValue(String.class);

        PreferenceManager.saveStudentInfo(this, enrollmentNo, loginEmail, studentName, branch, year, section);

        Toast.makeText(this, Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();
        navigateToStudentDashboard();
    }

    private void navigateToStudentDashboard() {
        Intent intent = new Intent(LoginStudentActivity.this, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleAuthenticationError(Exception exception) {
        String errorMessage = Constants.ERROR_AUTHENTICATION_FAILED;
        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("password")) {
                    errorMessage = "Invalid password. Please try again.";
                } else if (exceptionMessage.contains("email")) {
                    errorMessage = "Invalid email address.";
                } else if (exceptionMessage.contains("network")) {
                    errorMessage = Constants.ERROR_NETWORK_UNAVAILABLE;
                }
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private boolean validateInputs() {
        loginEmail = til_email.getEditText().getText().toString().trim();
        loginPassword = til_password.getEditText().getText().toString().trim();

        // Clear previous errors
        til_email.setError(null);
        til_password.setError(null);

        if (loginEmail.isEmpty()) {
            til_email.setError("Email cannot be empty");
            til_email.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(loginEmail).matches()) {
            til_email.setError("Please enter a valid email address");
            til_email.requestFocus();
            return false;
        }
        if (!ValidationUtils.isValidEmailDomain(loginEmail)) {
            til_email.setError(Constants.ERROR_INVALID_EMAIL_DOMAIN);
            til_email.requestFocus();
            return false;
        }
        if (loginPassword.isEmpty()) {
            til_password.setError("Password cannot be empty");
            til_password.requestFocus();
            return false;
        }
        if (loginPassword.length() < Constants.MIN_PASSWORD_LENGTH) {
            til_password.setError("Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters");
            til_password.requestFocus();
            return false;
        }
        return true;
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        clearErrors();
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        clearErrors();
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // Not needed
    }

    private void clearErrors() {
        if (til_email != null) {
            til_email.setError(null);
        }
        if (til_password != null) {
            til_password.setError(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
    }
}