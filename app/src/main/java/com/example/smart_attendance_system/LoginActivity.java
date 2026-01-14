package com.example.smart_attendance_system;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

public class LoginActivity extends AppCompatActivity {

    private Button btn_login_to_faculty, btn_login_to_student;
    private TextView tv_app_title, tv_app_subtitle, tv_app_description;
    private ImageView iv_app_logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btn_login_to_faculty = findViewById(R.id.btn_login_to_faculty);
        btn_login_to_student = findViewById(R.id.btn_login_to_student);
        tv_app_title = findViewById(R.id.tv_app_title);
        tv_app_subtitle = findViewById(R.id.tv_app_subtitle);
        tv_app_description = findViewById(R.id.tv_app_description);
        iv_app_logo = findViewById(R.id.iv_app_logo);

        // Set app information
        tv_app_title.setText("Smart Attendance System");
        tv_app_subtitle.setText("SAEC University");
        tv_app_description.setText("Secure and efficient attendance management system.\nChoose your login type to continue.");
    }

    private void setupClickListeners() {
        btn_login_to_student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, LoginStudentActivity.class);
                startActivity(intent);
            }
        });

        btn_login_to_faculty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, LoginFacultyActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Exit app when back is pressed on login screen
        finishAffinity();
    }
}