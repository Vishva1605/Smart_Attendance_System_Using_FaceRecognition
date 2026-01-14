package com.example.smart_attendance_system;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tv_app_name, tv_loading_text;
    private static final int SPLASH_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        initializeViews();
        startSplashTimer();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        tv_app_name = findViewById(R.id.tv_app_name);
        tv_loading_text = findViewById(R.id.tv_loading_text);

        tv_app_name.setText("Smart Attendance System");
        tv_loading_text.setText("Loading...");
    }

    private void startSplashTimer() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DELAY);
    }
}