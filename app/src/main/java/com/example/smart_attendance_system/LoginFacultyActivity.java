package com.example.smart_attendance_system;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFacultyActivity extends AppCompatActivity implements TextWatcher {

    private static final String TAG = "LoginFaculty";

    private Button btn_login, btn_back;
    private ProgressDialog progressDialog;
    private TextInputLayout til_email, til_password;
    private String loginPassword, loginEmail;
    private FirebaseAuth mAuth;
    private DatabaseReference facultyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_faculty);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btn_login = findViewById(R.id.btn_login);
        btn_back = findViewById(R.id.btn_back);
        til_email = findViewById(R.id.til_email);
        til_password = findViewById(R.id.til_password);

        // Add text watchers to clear errors
        if (til_email.getEditText() != null) {
            til_email.getEditText().addTextChangedListener(this);
        }
        if (til_password.getEditText() != null) {
            til_password.getEditText().addTextChangedListener(this);
        }

        mAuth = FirebaseAuth.getInstance();
        facultyRef = FirebaseDatabase.getInstance().getReference(Constants.FACULTY_REF);
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

        showProgressDialog("Authenticating faculty...");

        mAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Faculty authentication successful");
                            verifyFacultyInDatabase();
                        } else {
                            closeProgressDialog();
                            handleAuthenticationError(task.getException());
                        }
                    }
                });
    }

    private void verifyFacultyInDatabase() {
        facultyRef.orderByChild(Constants.FACULTY_EMAIL)
                .equalTo(loginEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        closeProgressDialog();
                        if (dataSnapshot.exists()) {
                            // Faculty exists in database
                            Toast.makeText(LoginFacultyActivity.this,
                                    Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();

                            // Save faculty info to preferences
                            for (DataSnapshot facultySnapshot : dataSnapshot.getChildren()) {
                                String facultyId = facultySnapshot.getKey();
                                PreferenceManager.saveFacultyInfo(LoginFacultyActivity.this,
                                        facultyId, loginEmail);
                                break;
                            }

                            navigateToFacultyDashboard();
                        } else {
                            // Faculty not found in database
                            mAuth.signOut();
                            Toast.makeText(LoginFacultyActivity.this,
                                    "Faculty record not found. Please contact administrator.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        closeProgressDialog();
                        Log.e(TAG, "Database error: " + error.getMessage());
                        Toast.makeText(LoginFacultyActivity.this,
                                "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToFacultyDashboard() {
        Intent intent = new Intent(LoginFacultyActivity.this, TakeAttendanceActivity.class);
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