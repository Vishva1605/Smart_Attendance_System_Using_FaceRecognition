package com.example.smart_attendance_system;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;

public class PresenceRecordedActivity extends AppCompatActivity {

    private TextView tv_success_message, tv_session_subject, tv_session_date, tv_session_time;
    private Button btn_back_to_dashboard, btn_view_report;
    private ImageView iv_success_icon;

    private String sessionSubject;
    private String sessionDate;
    private String sessionTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_recorded);

        initializeViews();
        getIntentData();
        setupClickListeners();
        animateSuccessElements();
    }

    private void initializeViews() {
        tv_success_message = findViewById(R.id.tv_success_message);
        tv_session_subject = findViewById(R.id.tv_session_subject);
        tv_session_date = findViewById(R.id.tv_session_date);
        tv_session_time = findViewById(R.id.tv_session_time);
        btn_back_to_dashboard = findViewById(R.id.btn_back_to_dashboard);
        btn_view_report = findViewById(R.id.btn_view_report);
        iv_success_icon = findViewById(R.id.iv_success_icon);

        tv_success_message.setText("✅ Attendance Marked Successfully!\n\nYour presence has been recorded for this session.");
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            sessionSubject = intent.getStringExtra(Constants.EXTRA_SESSION_SUBJECT);
            sessionDate = intent.getStringExtra(Constants.EXTRA_SESSION_DATE);
            sessionTime = intent.getStringExtra(Constants.EXTRA_SESSION_TIME);

            updateSessionInfo();
        }
    }

    private void updateSessionInfo() {
        if (sessionSubject != null) {
            tv_session_subject.setText("Subject: " + sessionSubject);
        } else {
            tv_session_subject.setText("Subject: Not available");
        }

        if (sessionDate != null) {
            tv_session_date.setText("Date: " + formatDate(sessionDate));
        } else {
            tv_session_date.setText("Date: Not available");
        }

        if (sessionTime != null) {
            tv_session_time.setText("Time: " + sessionTime);
        } else {
            tv_session_time.setText("Time: Not available");
        }
    }

    private String formatDate(String dateString) {
        try {
            if (dateString.contains(",")) {
                return dateString;
            }

            String[] parts = dateString.split("/");
            if (parts.length == 3) {
                String day = parts[0];
                String month = getMonthName(Integer.parseInt(parts[1]));
                return day + " " + month;
            }

            return dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private String getMonthName(int month) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        if (month >= 1 && month <= 12) {
            return months[month];
        }
        return "Unknown";
    }

    private void setupClickListeners() {
        btn_back_to_dashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToDashboard();
            }
        });

        btn_view_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAttendanceReport();
            }
        });
    }

    private void animateSuccessElements() {
        // Animate success icon
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv_success_icon, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv_success_icon, "scaleY", 0f, 1.2f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(iv_success_icon, "alpha", 0f, 1f);

        AnimatorSet iconAnimation = new AnimatorSet();
        iconAnimation.playTogether(scaleX, scaleY, alpha);
        iconAnimation.setDuration(Constants.ANIMATION_DURATION_LONG);

        // Animate success message
        ObjectAnimator messageAlpha = ObjectAnimator.ofFloat(tv_success_message, "alpha", 0f, 1f);
        ObjectAnimator messageTranslation = ObjectAnimator.ofFloat(tv_success_message, "translationY", 50f, 0f);

        AnimatorSet messageAnimation = new AnimatorSet();
        messageAnimation.playTogether(messageAlpha, messageTranslation);
        messageAnimation.setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        messageAnimation.setStartDelay(400);

        iconAnimation.start();
        messageAnimation.start();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToAttendanceReport() {
        Intent intent = new Intent(this, ActivityAttendanceReportStudent.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        navigateToDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSessionInfo();
    }
}