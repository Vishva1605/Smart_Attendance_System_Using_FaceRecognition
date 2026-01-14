package com.example.smart_attendance_system;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceReportActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceReport";

    private TextView tv_session_info, tv_attendance_summary;
    private ListView lv_attendance_list;
    private Button btn_back, btn_refresh, btn_export;

    private DatabaseReference attendanceSessionsRef, studentsRef;
    private String sessionId;
    private boolean isLiveView = false;
    
    private List<AttendanceRecord> attendanceRecords;
    private AttendanceListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        initializeViews();
        getIntentData();
        setupClickListeners();
        loadAttendanceReport();
    }

    private void initializeViews() {
        tv_session_info = findViewById(R.id.tv_session_info);
        tv_attendance_summary = findViewById(R.id.tv_attendance_summary);
        lv_attendance_list = findViewById(R.id.lv_attendance_list);
        btn_back = findViewById(R.id.btn_back);
        btn_refresh = findViewById(R.id.btn_refresh);
        btn_export = findViewById(R.id.btn_export);

        attendanceSessionsRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        attendanceRecords = new ArrayList<>();
        adapter = new AttendanceListAdapter(this, attendanceRecords);
        lv_attendance_list.setAdapter(adapter);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            sessionId = intent.getStringExtra(Constants.EXTRA_SESSION_ID);
            isLiveView = intent.getBooleanExtra(Constants.EXTRA_IS_LIVE_VIEW, false);
        }

        if (sessionId == null) {
            Toast.makeText(this, "Session ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isLiveView) {
            setTitle("Live Attendance View");
            btn_export.setVisibility(View.GONE);
        } else {
            setTitle("Attendance Report");
        }
    }

    private void setupClickListeners() {
        btn_back.setOnClickListener(v -> finish());
        
        btn_refresh.setOnClickListener(v -> {
            attendanceRecords.clear();
            adapter.notifyDataSetChanged();
            loadAttendanceReport();
        });
        
        btn_export.setOnClickListener(v -> exportReport());
    }

    private void loadAttendanceReport() {
        tv_session_info.setText("Loading session information...");
        tv_attendance_summary.setText("Loading attendance data...");

        // Load session information
        attendanceSessionsRef.child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    displaySessionInfo(dataSnapshot);
                    loadAttendanceData(dataSnapshot);
                } else {
                    tv_session_info.setText("❌ Session not found");
                    Toast.makeText(AttendanceReportActivity.this, 
                            "Session not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading session: " + error.getMessage());
                tv_session_info.setText("❌ Error loading session information");
                Toast.makeText(AttendanceReportActivity.this, 
                        "Error loading session: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displaySessionInfo(DataSnapshot sessionSnapshot) {
        String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
        String branch = sessionSnapshot.child(Constants.SESSION_BRANCH).getValue(String.class);
        String year = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
        String section = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);
        String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
        String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
        String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);
        String status = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);

        String sessionInfo = "📚 Session Information\n\n" +
                "Subject: " + (subject != null ? subject : "N/A") + "\n" +
                "Class: " + branch + " - Year " + year + " - Section " + section + "\n" +
                "Date: " + (date != null ? date : "N/A") + "\n" +
                "Time: " + startTime + " - " + endTime + "\n" +
                "Status: " + (Constants.SESSION_ACTIVE.equals(status) ? "🟢 Active" : "🔴 Ended");

        tv_session_info.setText(sessionInfo);
    }

    private void loadAttendanceData(DataSnapshot sessionSnapshot) {
        String branch = sessionSnapshot.child(Constants.SESSION_BRANCH).getValue(String.class);
        String year = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
        String section = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);

        // Load all students for this class
        studentsRef.orderByChild(Constants.STUDENT_BRANCH).equalTo(branch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        attendanceRecords.clear();
                        
                        int totalStudents = 0;
                        int presentStudents = 0;

                        for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                            String studentYear = studentSnapshot.child(Constants.STUDENT_YEAR).getValue(String.class);
                            String studentSection = studentSnapshot.child(Constants.STUDENT_SECTION).getValue(String.class);

                            // Check if student belongs to this class
                            if (year.equals(studentYear) && section.equals(studentSection)) {
                                totalStudents++;

                                String enrollmentNo = studentSnapshot.getKey();
                                String studentName = studentSnapshot.child(Constants.STUDENT_NAME).getValue(String.class);
                                String studentEmail = studentSnapshot.child(Constants.STUDENT_EMAIL).getValue(String.class);

                                // Check attendance status
                                DataSnapshot attendanceSnapshot = studentSnapshot
                                        .child(Constants.STUDENT_ATTENDANCE)
                                        .child(sessionId);

                                String attendanceStatus = Constants.ATTENDANCE_ABSENT;
                                String markedTime = "Not marked";

                                if (attendanceSnapshot.exists()) {
                                    String status = attendanceSnapshot.getValue(String.class);
                                    if (Constants.ATTENDANCE_PRESENT.equals(status)) {
                                        attendanceStatus = Constants.ATTENDANCE_PRESENT;
                                        presentStudents++;
                                        
                                        // Get marked time (you might want to store this)
                                        markedTime = getCurrentTime();
                                    }
                                }

                                AttendanceRecord record = new AttendanceRecord(
                                        enrollmentNo,
                                        studentName != null ? studentName : "Unknown",
                                        studentEmail != null ? studentEmail : "Unknown",
                                        attendanceStatus,
                                        markedTime
                                );

                                attendanceRecords.add(record);
                            }
                        }

                        // Sort records: Present first, then by name
                        attendanceRecords.sort((r1, r2) -> {
                            if (r1.getStatus().equals(r2.getStatus())) {
                                return r1.getStudentName().compareToIgnoreCase(r2.getStudentName());
                            }
                            return Constants.ATTENDANCE_PRESENT.equals(r1.getStatus()) ? -1 : 1;
                        });

                        updateAttendanceSummary(totalStudents, presentStudents);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading attendance data: " + error.getMessage());
                        tv_attendance_summary.setText("❌ Error loading attendance data");
                        Toast.makeText(AttendanceReportActivity.this, 
                                "Error loading attendance: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateAttendanceSummary(int total, int present) {
        int absent = total - present;
        double percentage = total > 0 ? (present * 100.0 / total) : 0.0;

        String summary = "📊 Attendance Summary\n\n" +
                "Total Students: " + total + "\n" +
                "Present: " + present + " ✅\n" +
                "Absent: " + absent + " ❌\n" +
                "Attendance: " + String.format("%.1f%%", percentage);

        tv_attendance_summary.setText(summary);
    }

    private String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(new Date());
    }

    private void exportReport() {
        if (attendanceRecords.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Enrollment No,Student Name,Email,Status,Time Marked\n");

        for (AttendanceRecord record : attendanceRecords) {
            csvContent.append(record.getEnrollmentNo()).append(",")
                    .append(record.getStudentName()).append(",")
                    .append(record.getEmail()).append(",")
                    .append(record.getStatus()).append(",")
                    .append(record.getMarkedTime()).append("\n");
        }

        // Share the CSV content
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, csvContent.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - " + sessionId);
        
        startActivity(Intent.createChooser(shareIntent, "Export Attendance Report"));
    }

    // Inner class for attendance record
    public static class AttendanceRecord {
        private String enrollmentNo;
        private String studentName;
        private String email;
        private String status;
        private String markedTime;

        public AttendanceRecord(String enrollmentNo, String studentName, String email, 
                              String status, String markedTime) {
            this.enrollmentNo = enrollmentNo;
            this.studentName = studentName;
            this.email = email;
            this.status = status;
            this.markedTime = markedTime;
        }

        // Getters
        public String getEnrollmentNo() { return enrollmentNo; }
        public String getStudentName() { return studentName; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getMarkedTime() { return markedTime; }
    }
}