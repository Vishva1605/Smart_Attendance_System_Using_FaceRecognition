package com.example.smart_attendance_system;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity implements TextWatcher {

    private static final String TAG = "ResetPassword";

    private TextInputLayout til_current_password, til_new_password, til_confirm_password;
    private Button btn_reset_password, btn_back;
    private TextView tv_user_info, tv_instructions;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private String userEmail;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initializeViews();
        getUserInfo();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initializeViews() {
        til_current_password = findViewById(R.id.til_current_password);
        til_new_password = findViewById(R.id.til_new_password);
        til_confirm_password = findViewById(R.id.til_confirm_password);
        btn_reset_password = findViewById(R.id.btn_reset_password);
        btn_back = findViewById(R.id.btn_back);
        tv_user_info = findViewById(R.id.tv_user_info);
        tv_instructions = findViewById(R.id.tv_instructions);

        mAuth = FirebaseAuth.getInstance();

        tv_instructions.setText("🔐 Reset Your Password\n\n" +
                "Enter your current password and choose a new secure password.\n\n" +
                "Password Requirements:\n" +
                "• Minimum 6 characters\n" +
                "• Mix of letters and numbers recommended\n" +
                "• Avoid common passwords");
    }

    private void getUserInfo() {
        userType = PreferenceManager.getUserType(this);
        
        if (Constants.USER_TYPE_STUDENT.equals(userType)) {
            userEmail = PreferenceManager.getStudentEmail(this);
            String studentName = PreferenceManager.getStudentName(this);
            String enrollmentNo = PreferenceManager.getEnrollmentNo(this);
            
            tv_user_info.setText("👤 Student Account\n" +
                    "Name: " + (studentName != null ? studentName : "Unknown") + "\n" +
                    "Enrollment: " + (enrollmentNo != null ? enrollmentNo : "Unknown") + "\n" +
                    "Email: " + (userEmail != null ? userEmail : "Unknown"));
        } else if (Constants.USER_TYPE_FACULTY.equals(userType)) {
            userEmail = PreferenceManager.getFacultyEmail(this);
            
            tv_user_info.setText("👨‍🏫 Faculty Account\n" +
                    "Email: " + (userEmail != null ? userEmail : "Unknown"));
        }

        if (userEmail == null) {
            Toast.makeText(this, "User information not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        btn_reset_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptPasswordReset();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupTextWatchers() {
        if (til_current_password.getEditText() != null) {
            til_current_password.getEditText().addTextChangedListener(this);
        }
        if (til_new_password.getEditText() != null) {
            til_new_password.getEditText().addTextChangedListener(this);
        }
        if (til_confirm_password.getEditText() != null) {
            til_confirm_password.getEditText().addTextChangedListener(this);
        }
    }

    private void attemptPasswordReset() {
        if (!validateInputs()) {
            return;
        }

        String currentPassword = til_current_password.getEditText().getText().toString().trim();
        String newPassword = til_new_password.getEditText().getText().toString().trim();

        showProgressDialog("Resetting password...");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            closeProgressDialog();
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Re-authenticate user with current password
        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, currentPassword);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated successfully");
                            updatePassword(newPassword);
                        } else {
                            closeProgressDialog();
                            Log.e(TAG, "Re-authentication failed: " + task.getException());
                            til_current_password.setError("Current password is incorrect");
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Current password is incorrect. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            closeProgressDialog();
            return;
        }

        user.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        closeProgressDialog();
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password updated successfully");
                            Toast.makeText(ResetPasswordActivity.this,
                                    "✅ Password reset successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Log.e(TAG, "Password update failed: " + task.getException());
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Failed to reset password. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean validateInputs() {
        String currentPassword = til_current_password.getEditText().getText().toString().trim();
        String newPassword = til_new_password.getEditText().getText().toString().trim();
        String confirmPassword = til_confirm_password.getEditText().getText().toString().trim();

        // Clear previous errors
        clearErrors();

        if (currentPassword.isEmpty()) {
            til_current_password.setError("Current password cannot be empty");
            til_current_password.requestFocus();
            return false;
        }

        if (newPassword.isEmpty()) {
            til_new_password.setError("New password cannot be empty");
            til_new_password.requestFocus();
            return false;
        }

        if (newPassword.length() < Constants.MIN_PASSWORD_LENGTH) {
            til_new_password.setError("Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters");
            til_new_password.requestFocus();
            return false;
        }

        if (confirmPassword.isEmpty()) {
            til_confirm_password.setError("Please confirm your new password");
            til_confirm_password.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            til_confirm_password.setError("Passwords do not match");
            til_confirm_password.requestFocus();
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            til_new_password.setError("New password must be different from current password");
            til_new_password.requestFocus();
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

    private void clearErrors() {
        if (til_current_password != null) {
            til_current_password.setError(null);
        }
        if (til_new_password != null) {
            til_new_password.setError(null);
        }
        if (til_confirm_password != null) {
            til_confirm_password.setError(null);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        clearErrors();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        clearErrors();
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Not needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
    }
}