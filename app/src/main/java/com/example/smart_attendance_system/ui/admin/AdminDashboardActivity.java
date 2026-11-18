package com.example.smart_attendance_system.ui.admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.ui.auth.LoginActivity;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardManageStudents, cardManageFaculty, cardMasterAttendance, cardSystemSettings;
    private ImageButton btnLogout;
    private TextView tvAdminEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initializeViews();
        setupClickListeners();
        displayAdminInfo();
    }

    private void initializeViews() {
        cardManageStudents = findViewById(R.id.card_manage_students);
        cardManageFaculty = findViewById(R.id.card_manage_faculty);
        cardMasterAttendance = findViewById(R.id.card_master_attendance);
        cardSystemSettings = findViewById(R.id.card_system_settings);
        btnLogout = findViewById(R.id.btn_logout);
        tvAdminEmail = findViewById(R.id.tv_admin_email);
    }

    private void displayAdminInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvAdminEmail.setText(user.getEmail());
        }
    }

    private void setupClickListeners() {
        cardManageStudents.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageStudentsActivity.class);
            startActivity(intent);
        });

        cardManageFaculty.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageFacultyActivity.class);
            startActivity(intent);
        });

        cardMasterAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAttendanceActivity.class);
            startActivity(intent);
        });

        cardSystemSettings.setOnClickListener(v -> {
            // Placeholder for System Settings
             Toast.makeText(this, "System Settings (Coming Soon)", Toast.LENGTH_SHORT).show();
             // Intent intent = new Intent(AdminDashboardActivity.this, SystemSettingsActivity.class);
             // startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        // Clear prefs if needed, mostly user type
        PreferenceManager.clearStudentInfo(this); // Clears generic
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
