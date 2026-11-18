package com.example.smart_attendance_system.ui.faculty;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.data.Student;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.services.SessionAutoCloseService;
import com.example.smart_attendance_system.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;

public class SessionActivity extends AppCompatActivity {

    private static final String TAG = "SessionActivity";

    private String branch, year, section, subject;
    private long autoCloseDuration = 30 * 60 * 1000; // Default 30 mins
    private String sessionId;
    private TextView tv_session_details, tv_session_status, tv_student_count;
    private Button btn_end_session, btn_view_live_attendance, btn_refresh_count;
    private ImageView iv_session_indicator;

    private DatabaseReference attendanceReportRef, studentsRef;
    private boolean isSessionActive = false;
    private BroadcastReceiver autoCloseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        initializeViews();
        getIntentData();
        setupClickListeners();
        setupAutoCloseReceiver();
        createAttendanceSession();
    }

    private void initializeViews() {
        tv_session_details = findViewById(R.id.tv_session_details);
        tv_session_status = findViewById(R.id.tv_session_status);
        tv_student_count = findViewById(R.id.tv_student_count);
        btn_end_session = findViewById(R.id.btn_end_session);
        btn_view_live_attendance = findViewById(R.id.btn_view_live_attendance);
        btn_refresh_count = findViewById(R.id.btn_refresh_count);
        iv_session_indicator = findViewById(R.id.iv_session_indicator);

        attendanceReportRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        // Initially disable buttons until session is created
        btn_end_session.setEnabled(false);
        btn_view_live_attendance.setEnabled(false);
        btn_refresh_count.setEnabled(false);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            branch = intent.getStringExtra(Constants.EXTRA_BRANCH);
            year = intent.getStringExtra(Constants.EXTRA_YEAR);
            section = intent.getStringExtra(Constants.EXTRA_SECTION);
            section = intent.getStringExtra(Constants.EXTRA_SECTION);
            subject = intent.getStringExtra(Constants.EXTRA_SUBJECT);
            autoCloseDuration = intent.getLongExtra(Constants.EXTRA_AUTO_CLOSE_DURATION, 30 * 60 * 1000);

            Log.d(TAG, "Session data - Branch: " + branch + ", Duration: " + autoCloseDuration);
        }
    }

    private void setupClickListeners() {
        btn_end_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEndSessionConfirmation();
            }
        });

        btn_view_live_attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewLiveAttendance();
            }
        });

        btn_refresh_count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshStudentCount();
            }
        });

        // Back button to return to previous screen
        View btn_back = findViewById(R.id.btn_back);
        if (btn_back != null) {
            btn_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }
    }

    private void setupAutoCloseReceiver() {
        autoCloseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.ACTION_SESSION_AUTO_CLOSED.equals(intent.getAction())) {
                    String closedSessionId = intent.getStringExtra(Constants.EXTRA_SESSION_ID);
                    if (sessionId != null && sessionId.equals(closedSessionId)) {
                        handleAutoClose();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(Constants.ACTION_SESSION_AUTO_CLOSED);

        // Use numeric API 31 instead of Build.VERSION_CODES.S
        if (Build.VERSION.SDK_INT >= 31) { // Android 12+
            registerReceiver(autoCloseReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(autoCloseReceiver, filter);
        }
    }


    private void handleAutoClose() {
        runOnUiThread(() -> {
            isSessionActive = false;
            tv_session_status.setText("⏰ Session Auto-Closed\nSession was automatically closed after 30 minutes");
            
            // Disable buttons
            btn_end_session.setEnabled(false);
            btn_view_live_attendance.setEnabled(false);
            btn_refresh_count.setEnabled(false);
            
            Toast.makeText(SessionActivity.this, 
                    "Session was automatically closed after 30 minutes", Toast.LENGTH_LONG).show();
            
            // Navigate to report after a delay
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(SessionActivity.this, AttendanceReportActivity.class);
                intent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
                startActivity(intent);
               finish();
            }, 3000);
        });
    }

    private void createAttendanceSession() {
        tv_session_status.setText("Creating attendance session...");

        // Generate unique session ID with timestamp
        long timestamp = System.currentTimeMillis();
        sessionId = "session_" + branch + "_" + year + "_" + section + "_" + timestamp;

        Log.d(TAG, "Creating session with ID: " + sessionId);

        // Get current date and time
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String formattedDate = dateFormat.format(currentDate);
        String startTime = timeFormat.format(currentDate);

        // Calculate end time (2 hours later)
        Date endDate = new Date(currentDate.getTime() + (2 * 60 * 60 * 1000));
        String endTime = timeFormat.format(endDate);

        // Create session data
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put(Constants.SESSION_BRANCH, branch);
        sessionData.put(Constants.SESSION_YEAR, year);
        sessionData.put(Constants.SESSION_SECTION, section);
        sessionData.put(Constants.SESSION_SUBJECT, subject);
        sessionData.put(Constants.SESSION_DATE, formattedDate);
        sessionData.put(Constants.SESSION_START_TIME, startTime);
        sessionData.put(Constants.SESSION_END_TIME, endTime);
        sessionData.put(Constants.SESSION_AUTO_CLOSE_DURATION, autoCloseDuration);
        sessionData.put(Constants.SESSION_STATUS, Constants.SESSION_ACTIVE);
        sessionData.put(Constants.SESSION_CREATED_AT, timestamp);
        sessionData.put(Constants.SESSION_FACULTY_EMAIL, PreferenceManager.getFacultyEmail(this));
        sessionData.put(Constants.SESSION_FACULTY_ID, PreferenceManager.getFacultyId(this));

        attendanceReportRef.child(sessionId).setValue(sessionData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Session created successfully");
                            isSessionActive = true;
                            updateUIAfterSessionCreation(formattedDate, startTime, endTime);
                            refreshStudentCount();
                            
                            // Start auto-close service
                            startAutoCloseService(timestamp);
                        } else {
                            Log.e(TAG, "Failed to create session: " + task.getException());
                            Toast.makeText(SessionActivity.this,
                                    "Failed to create session. Please try again.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }
    
    private void startAutoCloseService(long sessionStartTime) {
        Intent serviceIntent = new Intent(this, SessionAutoCloseService.class);
        serviceIntent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
        serviceIntent.putExtra(Constants.EXTRA_AUTO_CLOSE_DURATION, autoCloseDuration);
        serviceIntent.putExtra(Constants.SESSION_CREATED_AT, sessionStartTime);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d(TAG, "Auto-close service started for session: " + sessionId + " with duration: " + autoCloseDuration);
    }

    private void updateUIAfterSessionCreation(String date, String startTime, String endTime) {
        String sessionDetails = "📚 Active Attendance Session\n\n" +
                "Subject: " + subject + "\n" +
                "Branch: " + branch + "\n" +
                "Year: " + year + "\n" +
                "Section: " + section + "\n" +
                "Date: " + date + "\n" +
                "Time: " + startTime + " - " + endTime + "\n" +
                "Session ID: " + sessionId;

        tv_session_details.setText(sessionDetails);
        tv_session_status.setText("✅ Session is ACTIVE\nStudents can now mark their attendance");

        // Enable buttons
        btn_end_session.setEnabled(true);
        btn_view_live_attendance.setEnabled(true);
        btn_refresh_count.setEnabled(true);

        // Update button colors
        btn_end_session.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        btn_view_live_attendance.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        btn_refresh_count.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));

        Toast.makeText(this, Constants.SUCCESS_SESSION_CREATED, Toast.LENGTH_SHORT).show();
    }

    private void refreshStudentCount() {
        if (!isSessionActive) return;

        studentsRef.orderByChild(Constants.STUDENT_BRANCH).equalTo(branch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int totalStudents = 0;
                        int presentStudents = 0;

                        for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                            String studentYear = studentSnapshot.child(Constants.STUDENT_YEAR).getValue(String.class);
                            String studentSection = studentSnapshot.child(Constants.STUDENT_SECTION).getValue(String.class);

                            if (year.equals(studentYear) && section.equals(studentSection)) {
                                totalStudents++;

                                // Check if student has marked attendance
                                DataSnapshot attendanceSnapshot = studentSnapshot
                                        .child(Constants.STUDENT_ATTENDANCE)
                                        .child(sessionId);

                                if (attendanceSnapshot.exists()) {
                                    String status = attendanceSnapshot.getValue(String.class);
                                    if (Constants.ATTENDANCE_PRESENT.equals(status)) {
                                        presentStudents++;
                                    }
                                }
                            }
                        }

                        updateStudentCountDisplay(totalStudents, presentStudents);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error refreshing student count: " + error.getMessage());
                        Toast.makeText(SessionActivity.this,
                                "Error refreshing count: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStudentCountDisplay(int total, int present) {
        String countText = "👥 Class Strength: " + total + "\n" +
                "✅ Present: " + present + "\n" +
                "❌ Absent: " + (total - present) + "\n" +
                "📊 Attendance: " + (total > 0 ? String.format("%.1f%%", (present * 100.0 / total)) : "0%");

        tv_student_count.setText(countText);
    }

    private void showEndSessionConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this attendance session?\n\n" +
                        "Students will no longer be able to mark attendance after this.")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        if (sessionId != null && isSessionActive) {
            // Update session status to ended
            attendanceReportRef.child(sessionId).child(Constants.SESSION_STATUS)
                    .setValue(Constants.SESSION_ENDED)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                isSessionActive = false;
                                Toast.makeText(SessionActivity.this,
                                        Constants.SUCCESS_SESSION_ENDED, Toast.LENGTH_SHORT).show();
                                
                                // Stop auto-close service
                                stopAutoCloseService();

                                Intent intent = new Intent(SessionActivity.this, AttendanceReportActivity.class);
                                intent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(SessionActivity.this,
                                        "Failed to end session. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
    
    private void stopAutoCloseService() {
        Intent serviceIntent = new Intent(this, SessionAutoCloseService.class);
        stopService(serviceIntent);
        Log.d(TAG, "Auto-close service stopped");
    }

    private void viewLiveAttendance() {
        if (sessionId != null && isSessionActive) {
            Intent intent = new Intent(this, AttendanceReportActivity.class);
            intent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
            intent.putExtra(Constants.EXTRA_IS_LIVE_VIEW, true);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (isSessionActive) {
            new AlertDialog.Builder(this)
                    .setTitle("Session Active")
                    .setMessage("An attendance session is currently active. Please end the session properly before going back.")
                    .setPositiveButton("End Session", (dialog, which) -> endSession())
                    .setNegativeButton("Continue Session", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister broadcast receiver safely
        if (autoCloseReceiver != null) {
            try {
                unregisterReceiver(autoCloseReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver already unregistered, ignore
                Log.w(TAG, "Receiver already unregistered");
            }
            autoCloseReceiver = null;
        }

        // Stop auto-close service and mark session ended if still active
        if (sessionId != null && isSessionActive) {
            stopAutoCloseService();
            attendanceReportRef.child(sessionId).child(Constants.SESSION_STATUS)
                    .setValue(Constants.SESSION_ENDED);
        }
    }
}



















