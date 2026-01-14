package com.example.smart_attendance_system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    private Button btn_logout, btn_check_sessions, btn_view_my_attendance;
    private Button btn_register_face, btn_reset_password;
    private TextView tv_network_status, tv_welcome_message, tv_student_info;
    private boolean isNetworkValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        initializeViews();
        setupClickListeners();
        displayStudentInfo();
        checkLocationPermissionAndWifi();
    }

    private void initializeViews() {
        btn_logout = findViewById(R.id.btn_logout);
        btn_check_sessions = findViewById(R.id.btn_check_sessions);
        btn_view_my_attendance = findViewById(R.id.btn_view_my_attendance);
        btn_register_face = findViewById(R.id.bt_register_face);
        btn_reset_password = findViewById(R.id.btn_reset_password);
        tv_network_status = findViewById(R.id.tv_network_status);
        tv_welcome_message = findViewById(R.id.tv_welcome_message);
        tv_student_info = findViewById(R.id.tv_student_info);

        // Ensure all buttons are properly initialized
        if (btn_register_face == null) {
            Log.e(TAG, "Register face button not found in layout!");
        }

        updateFaceRegistrationButton();
        checkFirstTimeLogin();
    }

    private void displayStudentInfo() {
        String studentName = PreferenceManager.getStudentName(this);
        String enrollmentNo = PreferenceManager.getEnrollmentNo(this);
        String branch = PreferenceManager.getStudentBranch(this);
        String year = PreferenceManager.getStudentYear(this);
        String section = PreferenceManager.getStudentSection(this);

        tv_welcome_message.setText("Welcome, " + (studentName != null ? studentName : "Student"));

        String studentInfo = "👤 Student Information\n" +
                "Name: " + (studentName != null ? studentName : "Unknown") + "\n" +
                "Enrollment: " + (enrollmentNo != null ? enrollmentNo : "Unknown") + "\n" +
                "Class: " + branch + " - Year " + year + " - Section " + section;

        tv_student_info.setText(studentInfo);
    }

    private void setupClickListeners() {
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        btn_check_sessions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkValid) {
                    Intent intent = new Intent(StudentDashboardActivity.this, SelectAttendanceActivity.class);
                    startActivity(intent);
                } else {
                    checkLocationPermissionAndWifi();
                    Toast.makeText(StudentDashboardActivity.this,
                            Constants.ERROR_WIFI_NOT_CONNECTED, Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_view_my_attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentDashboardActivity.this, ActivityAttendanceReportStudent.class);
                startActivity(intent);
            }
        });

        btn_register_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Register face button clicked");
                boolean isFaceRegistered = PreferenceManager.isFaceRegistered(StudentDashboardActivity.this);

                if (isFaceRegistered) {
                    // Show message that face is already registered
                    new androidx.appcompat.app.AlertDialog.Builder(StudentDashboardActivity.this)
                            .setTitle("Face Already Registered")
                            .setMessage("Your face has already been registered for this account.\n\nIf you need to update your face data, please contact the administrator who can reset your face registration.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Intent intent = new Intent(StudentDashboardActivity.this, FaceRegistrationActivity.class);
                    startActivity(intent);
                }
            }
        });

        btn_reset_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentDashboardActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkFirstTimeLogin() {
        boolean isFirstLogin = !PreferenceManager.isFirstLoginCompleted(this);
        boolean isFaceRegistered = PreferenceManager.isFaceRegistered(this);

        if (isFirstLogin && !isFaceRegistered) {
            // Show mandatory face registration dialog
            showMandatoryFaceRegistrationDialog();
        }
    }

    private void showMandatoryFaceRegistrationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Face Registration Required")
                .setMessage("For security purposes, you must register your face during your first login. This will be used for attendance verification.")
                .setPositiveButton("Register Face", (dialog, which) -> {
                    Intent intent = new Intent(StudentDashboardActivity.this, FaceRegistrationActivity.class);
                    intent.putExtra(Constants.EXTRA_IS_FIRST_TIME_REGISTRATION, true);
                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }

    private void checkLocationPermissionAndWifi() {
        if (hasLocationPermissions()) {
            checkWifiConnection();
        } else {
            requestLocationPermissions();
        }
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void checkWifiConnection() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiManager == null) {
                updateNetworkStatus("❌ WiFi not available", false);
                return;
            }

            if (!wifiManager.isWifiEnabled()) {
                updateNetworkStatus("❌ WiFi is disabled\nPlease enable WiFi and connect to university network", false);
                return;
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                updateNetworkStatus("❌ No WiFi connection\nPlease connect to university WiFi", false);
                return;
            }

            String currentBSSID = wifiInfo.getBSSID();
            String currentSSID = wifiInfo.getSSID();

            // Remove quotes from SSID if present
            if (currentSSID != null && currentSSID.startsWith("\"") && currentSSID.endsWith("\"")) {
                currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
            }

            Log.d(TAG, "Current BSSID: " + currentBSSID);
            Log.d(TAG, "Current SSID: " + currentSSID);
            Log.d(TAG, "Expected BSSID: " + Constants.UNIVERSITY_WIFI_BSSID);

            // Check for placeholder BSSID (indicates location services issue)
            if (Constants.PLACEHOLDER_BSSID.equals(currentBSSID)) {
                updateNetworkStatus("⚠️ Location services required\nPlease enable location services to detect WiFi network properly", false);
                return;
            }

            // Check if connected to university network
            if (currentBSSID != null && ("98:25:4a:25:7a:b7".equalsIgnoreCase(currentBSSID)||"98:25:4a:25:7a:b6".equalsIgnoreCase(currentBSSID)||"04:ab:08:cc:92:e2".equalsIgnoreCase(currentBSSID)||"92:ca:e7:dc:8e:53".equalsIgnoreCase(currentBSSID)||"9a:ca:e7:dc:8e:54".equalsIgnoreCase(currentBSSID))){
                updateNetworkStatus("✅ Connected to University Network\n" +
                        "Network: " + (currentSSID != null ? currentSSID : "University WiFi") + "\n" +
                        "You can now check for active sessions!", true);
            } else {
                updateNetworkStatus("❌ Not connected to University Network\n" +
                        "Current Network: " + (currentSSID != null ? currentSSID : "Unknown") + "\n" +
                        "BSSID: " + (currentBSSID != null ? currentBSSID : "Unknown") + "\n" +
                        "Please connect to the university WiFi to mark attendance", false);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while checking WiFi: " + e.getMessage());
            updateNetworkStatus("❌ Permission denied\nPlease grant location permissions to check WiFi network", false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi connection: " + e.getMessage());
            updateNetworkStatus("❌ Error checking network connection\nPlease try again", false);
        }
    }
   /*private void checkWifiConnection() {
       // For testing: always allow login regardless of network
       updateNetworkStatus(
               "✅ Connected (Test Mode)\nYou can now check for active sessions!",
               true
       );
       Log.d(TAG, "Bypassed WiFi check: test mode enabled");
   }*/


    private void updateNetworkStatus(String message, boolean isValid) {
        tv_network_status.setText(message);
        isNetworkValid = isValid;

        // Update button states
        btn_check_sessions.setEnabled(isValid);

        if (isValid) {
            btn_check_sessions.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            btn_check_sessions.setText("Check Active Sessions");
        } else {
            btn_check_sessions.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btn_check_sessions.setText("Connect to WiFi First");
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        PreferenceManager.clearStudentInfo(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkWifiConnection();
            } else {
                updateNetworkStatus(Constants.ERROR_LOCATION_PERMISSION_REQUIRED, false);
                Toast.makeText(this, Constants.ERROR_LOCATION_PERMISSION_REQUIRED, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateFaceRegistrationButton() {
        boolean isFaceRegistered = PreferenceManager.isFaceRegistered(this);
        boolean isFirstLogin = !PreferenceManager.isFirstLoginCompleted(this);

        // Ensure button is always visible
        btn_register_face.setVisibility(View.VISIBLE);

        if (isFaceRegistered) {
            btn_register_face.setText("✅ Face Registered");
            btn_register_face.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            btn_register_face.setEnabled(true); // Keep enabled to show the message
        } else if (isFirstLogin) {
            btn_register_face.setText("📸 Register Face (Required)");
            btn_register_face.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            btn_register_face.setEnabled(true);
        } else {
            btn_register_face.setText("📸 Register Face");
            btn_register_face.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            btn_register_face.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFaceRegistrationButton();
        // Recheck WiFi connection when activity resumes
        if (hasLocationPermissions()) {
            checkWifiConnection();
        }
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog or directly logout
        logout();
    }
}