package com.example.smart_attendance_system;

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

public class ActivityAttendanceReportStudent extends AppCompatActivity {

    private static final String TAG = "StudentAttendanceReport";

    private TextView tv_student_info, tv_attendance_summary;
    private ListView lv_attendance_history;
    private Button btn_back, btn_refresh;

    private DatabaseReference attendanceSessionsRef, studentsRef;
    private String enrollmentNo;
    private String studentBranch, studentYear, studentSection;
    
    private List<StudentAttendanceRecord> attendanceHistory;
    private StudentAttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report_student);

        initializeViews();
        getStudentInfo();
        setupClickListeners();
        loadAttendanceHistory();
    }

    private void initializeViews() {
        tv_student_info = findViewById(R.id.tv_student_info);
        tv_attendance_summary = findViewById(R.id.tv_attendance_summary);
        lv_attendance_history = findViewById(R.id.lv_attendance_history);
        btn_back = findViewById(R.id.btn_back);
        btn_refresh = findViewById(R.id.btn_refresh);

        attendanceSessionsRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        attendanceHistory = new ArrayList<>();
        adapter = new StudentAttendanceAdapter(this, attendanceHistory);
        lv_attendance_history.setAdapter(adapter);
    }

    private void getStudentInfo() {
        enrollmentNo = PreferenceManager.getEnrollmentNo(this);
        String studentName = PreferenceManager.getStudentName(this);
        studentBranch = PreferenceManager.getStudentBranch(this);
        studentYear = PreferenceManager.getStudentYear(this);
        studentSection = PreferenceManager.getStudentSection(this);

        if (enrollmentNo == null || studentBranch == null) {
            Toast.makeText(this, "Student information not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String studentInfo = "👤 My Attendance Report\n\n" +
                "Name: " + (studentName != null ? studentName : "Unknown") + "\n" +
                "Enrollment: " + enrollmentNo + "\n" +
                "Class: " + studentBranch + " - Year " + studentYear + " - Section " + studentSection;

        tv_student_info.setText(studentInfo);
    }

    private void setupClickListeners() {
        btn_back.setOnClickListener(v -> finish());
        
        btn_refresh.setOnClickListener(v -> {
            attendanceHistory.clear();
            adapter.notifyDataSetChanged();
            loadAttendanceHistory();
        });
    }

    private void loadAttendanceHistory() {
        tv_attendance_summary.setText("Loading attendance history...");

        // Load all sessions for student's class
        attendanceSessionsRef.orderByChild(Constants.SESSION_BRANCH).equalTo(studentBranch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        attendanceHistory.clear();
                        
                        int totalSessions = 0;
                        int attendedSessions = 0;

                        for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                            String sessionYear = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
                            String sessionSection = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);

                            // Check if session is for student's class
                            if (studentYear.equals(sessionYear) && studentSection.equals(sessionSection)) {
                                String sessionId = sessionSnapshot.getKey();
                                String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
                                String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
                                String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
                                String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);
                                String status = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);

                                // Only count ended sessions for attendance calculation
                                if (Constants.SESSION_ENDED.equals(status)) {
                                    totalSessions++;
                                    
                                    // Check if student attended this session
                                    checkStudentAttendance(sessionId, subject, date, startTime, endTime, 
                                            totalSessions == 1); // Update summary only for the last check
                                }
                            }
                        }

                        if (totalSessions == 0) {
                            tv_attendance_summary.setText("📊 No completed sessions found for your class");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading sessions: " + error.getMessage());
                        tv_attendance_summary.setText("❌ Error loading attendance history");
                        Toast.makeText(ActivityAttendanceReportStudent.this, 
                                "Error loading sessions: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkStudentAttendance(String sessionId, String subject, String date, 
                                      String startTime, String endTime, boolean updateSummary) {
        studentsRef.child(enrollmentNo).child(Constants.STUDENT_ATTENDANCE).child(sessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String attendanceStatus = Constants.ATTENDANCE_ABSENT;
                        String markedTime = "Not marked";

                        if (dataSnapshot.exists()) {
                            String status = dataSnapshot.getValue(String.class);
                            if (Constants.ATTENDANCE_PRESENT.equals(status)) {
                                attendanceStatus = Constants.ATTENDANCE_PRESENT;
                                markedTime = "Marked"; // You might want to store actual time
                            }
                        }

                        StudentAttendanceRecord record = new StudentAttendanceRecord(
                                sessionId,
                                subject != null ? subject : "Unknown Subject",
                                date != null ? date : "Unknown Date",
                                startTime + " - " + endTime,
                                attendanceStatus,
                                markedTime
                        );

                        attendanceHistory.add(record);
                        
                        // Sort by date (newest first)
                        attendanceHistory.sort((r1, r2) -> r2.getDate().compareToIgnoreCase(r1.getDate()));
                        
                        adapter.notifyDataSetChanged();

                        if (updateSummary) {
                            updateAttendanceSummary();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking attendance for session " + sessionId + ": " + error.getMessage());
                    }
                });
    }

    private void updateAttendanceSummary() {
        int totalSessions = attendanceHistory.size();
        int attendedSessions = 0;

        for (StudentAttendanceRecord record : attendanceHistory) {
            if (Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
                attendedSessions++;
            }
        }

        double percentage = totalSessions > 0 ? (attendedSessions * 100.0 / totalSessions) : 0.0;

        String summary = "📊 My Attendance Summary\n\n" +
                "Total Sessions: " + totalSessions + "\n" +
                "Attended: " + attendedSessions + " ✅\n" +
                "Missed: " + (totalSessions - attendedSessions) + " ❌\n" +
                "Attendance: " + String.format("%.1f%%", percentage);

        tv_attendance_summary.setText(summary);
    }

    // Inner class for student attendance record
    public static class StudentAttendanceRecord {
        private String sessionId;
        private String subject;
        private String date;
        private String time;
        private String status;
        private String markedTime;

        public StudentAttendanceRecord(String sessionId, String subject, String date, 
                                     String time, String status, String markedTime) {
            this.sessionId = sessionId;
            this.subject = subject;
            this.date = date;
            this.time = time;
            this.status = status;
            this.markedTime = markedTime;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getSubject() { return subject; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
        public String getMarkedTime() { return markedTime; }
    }
}