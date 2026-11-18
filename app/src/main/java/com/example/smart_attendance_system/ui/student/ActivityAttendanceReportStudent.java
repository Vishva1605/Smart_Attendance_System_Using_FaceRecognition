package com.example.smart_attendance_system.ui.student;
import com.example.smart_attendance_system.data.Student;
import com.example.smart_attendance_system.data.AttendanceRecord;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.adapters.StudentAttendanceAdapter;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityAttendanceReportStudent extends AppCompatActivity {

    private static final String TAG = "StudentAttendanceReport";

    private TextView tv_student_info, tv_attendance_summary;
    private ListView lv_attendance_history;
    private Button btn_back, btn_refresh;

    private DatabaseReference attendanceSessionsRef, studentsRef;
    private String enrollmentNo;
    private String studentBranch, studentYear, studentSection;
    
    // Use global AttendanceRecord model
    private List<AttendanceRecord> attendanceHistory;
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

        // Optimization: Fetch Student's Attendance Map FIRST (1 call)
        studentsRef.child(enrollmentNo).child(Constants.STUDENT_ATTENDANCE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentAttendanceSnapshot) {
                        // Store student's attendance in a Map for fast lookup
                        // Map<SessionID, AttendanceRecordObject>
                        final java.util.Map<String, AttendanceRecord> studentAttendanceMap = new java.util.HashMap<>();
                        
                        for (DataSnapshot recordSnapshot : studentAttendanceSnapshot.getChildren()) {
                             String sessionId = recordSnapshot.getKey();
                             if (recordSnapshot.hasChild("status")) {
                                 AttendanceRecord record = recordSnapshot.getValue(AttendanceRecord.class);
                                 if (record != null) studentAttendanceMap.put(sessionId, record);
                             } else {
                                 // Old format
                                 String status = recordSnapshot.getValue(String.class);
                                 if (Constants.ATTENDANCE_PRESENT.equals(status)) {
                                     AttendanceRecord record = new AttendanceRecord();
                                     record.setStatus(Constants.ATTENDANCE_PRESENT);
                                     record.setMarkedTime("Marked");
                                     studentAttendanceMap.put(sessionId, record);
                                 }
                             }
                        }
                        
                        // Now Loop Sessions (Call 2)
                        fetchSessionsAndCorrelate(studentAttendanceMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                         Log.e(TAG, "Error loading student attendance: " + error.getMessage());
                         tv_attendance_summary.setText("❌ Error loading data");
                    }
                });
    }

    private void fetchSessionsAndCorrelate(java.util.Map<String, AttendanceRecord> studentAttendanceMap) {
         attendanceSessionsRef.orderByChild(Constants.SESSION_BRANCH).equalTo(studentBranch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        attendanceHistory.clear();
                        
                        for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                            String sessionYear = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
                            String sessionSection = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);

                            if (studentYear.equals(sessionYear) && studentSection.equals(sessionSection)) {
                                String status = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);
                                
                                if (Constants.SESSION_ENDED.equals(status)) {
                                    String sessionId = sessionSnapshot.getKey();
                                    String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
                                    String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
                                    String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
                                    String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);
                                    
                                    // Correlate with memory map
                                    String attendanceStatus = Constants.ATTENDANCE_ABSENT;
                                    String markedTime = "Not marked";
                                    
                                    if (studentAttendanceMap.containsKey(sessionId)) {
                                        AttendanceRecord record = studentAttendanceMap.get(sessionId);
                                        if (record != null && Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
                                            attendanceStatus = Constants.ATTENDANCE_PRESENT;
                                            markedTime = record.getMarkedTime() != null ? record.getMarkedTime() : "Marked";
                                        }
                                    }

                                    AttendanceRecord displayRecord = new AttendanceRecord(
                                            subject != null ? subject : "Unknown Subject",
                                            date != null ? date : "Unknown Date",
                                            attendanceStatus,
                                            markedTime + " (" + startTime + " - " + endTime + ")"
                                    );
                                    attendanceHistory.add(displayRecord);
                                }
                            }
                        }

                        // Sort: Newest date first
                        Collections.sort(attendanceHistory, (r1, r2) -> {
                             return r2.getDate().compareTo(r1.getDate());
                        });
                        
                        updateAttendanceSummary();
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading sessions: " + error.getMessage());
                        tv_attendance_summary.setText("❌ Error loading sessions");
                    }
                });
    }

    // Removed checkStudentAttendance as it is merged into fetchSessionsAndCorrelate

    private void updateAttendanceSummary() {
        int totalSessions = attendanceHistory.size();
        int attendedSessions = 0;

        for (AttendanceRecord record : attendanceHistory) {
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
}



















