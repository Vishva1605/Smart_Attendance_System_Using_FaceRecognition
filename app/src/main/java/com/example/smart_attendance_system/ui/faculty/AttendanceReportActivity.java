package com.example.smart_attendance_system.ui.faculty;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.data.AttendanceRecord;
import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.utils.ExcelExportUtils;
import com.example.smart_attendance_system.adapters.AttendanceReportAdapter;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

public class AttendanceReportActivity extends AppCompatActivity implements AttendanceReportAdapter.OnManualOverrideListener {

    private static final String TAG = "AttendanceReport";

    private TextView tv_session_info, tv_attendance_summary;
    private RecyclerView rv_attendance_list;
    private Button btn_back, btn_refresh, btn_export;

    private DatabaseReference attendanceSessionsRef, studentsRef;

    private String sessionId;
    private AttendanceSession currentSession;
    private boolean isLiveView = false;

    private List<AttendanceRecord> attendanceRecords;
    private AttendanceReportAdapter adapter; // Switched to enhanced adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        initializeViews();
        getIntentData();
        setupClickListeners();

        if (sessionId != null) {
            loadSessionAndAttendance();
        }
    }

    private void initializeViews() {
        tv_session_info = findViewById(R.id.tv_session_info);
        tv_attendance_summary = findViewById(R.id.tv_attendance_summary);
        rv_attendance_list = findViewById(R.id.rv_attendance_list);
        btn_back = findViewById(R.id.btn_back);
        btn_refresh = findViewById(R.id.btn_refresh);
        btn_export = findViewById(R.id.btn_export);

        attendanceSessionsRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        attendanceRecords = new ArrayList<>();
        // Use the enhanced adapter
        adapter = new AttendanceReportAdapter(this, attendanceRecords);
        adapter.setOnManualOverrideListener(this); // Set listener for manual overrides
        
        rv_attendance_list.setLayoutManager(new LinearLayoutManager(this));
        rv_attendance_list.setAdapter(adapter);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            sessionId = intent.getStringExtra(Constants.EXTRA_SESSION_ID);
            isLiveView = intent.getBooleanExtra(Constants.EXTRA_IS_LIVE_VIEW, false);
        }

        if (sessionId == null) {
            Toast.makeText(this, "Error: Session ID missing", Toast.LENGTH_LONG).show();
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
            // attendanceRecords.clear(); // Adapter handles updates
            if (sessionId != null) loadSessionAndAttendance();
        });

        btn_export.setOnClickListener(v -> exportReport());
    }

    private void loadSessionAndAttendance() {
        if (sessionId == null) return;

        tv_session_info.setText("Loading session information...");
        tv_attendance_summary.setText("Loading attendance data...");

        // Load session info
        attendanceSessionsRef.child(sessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot sessionSnapshot) {
                        if (sessionSnapshot.exists()) {
                            // Map snapshot to AttendanceSession object (or manually if fields differ)
                            // For safety, let's map manually to our new class structure or read fields
                             String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
                             String branch = sessionSnapshot.child(Constants.SESSION_BRANCH).getValue(String.class);
                             String year = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
                             String section = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);
                             String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
                             String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
                             String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);
                             String status = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);
                             String facultyId = sessionSnapshot.child(Constants.SESSION_FACULTY_ID).getValue(String.class);
                             String facultyEmail = sessionSnapshot.child(Constants.SESSION_FACULTY_EMAIL).getValue(String.class); // Correct constant
                             Long createdAt = sessionSnapshot.child(Constants.SESSION_CREATED_AT).getValue(Long.class);

                            // Fetch Faculty Name
                            DatabaseReference facultyRef = FirebaseDatabase.getInstance().getReference(Constants.FACULTY_REF);
                            if (facultyId != null) {
                                facultyRef.child(facultyId).child(Constants.FACULTY_NAME).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String facultyName = snapshot.getValue(String.class);
                                        if (facultyName == null) facultyName = "Unknown Faculty";

                                        currentSession = new AttendanceSession(branch, year, section, subject, date,
                                                startTime, endTime, status, createdAt != null ? createdAt : 0, facultyEmail, facultyId, facultyName);
                                        currentSession.setId(sessionId);

                                        displaySessionInfo();
                                        checkManualOverridePermission();
                                        loadAttendanceData();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error fetching faculty name", error.toException());
                                        // Proceed with default/null name
                                        currentSession = new AttendanceSession(branch, year, section, subject, date,
                                                startTime, endTime, status, createdAt != null ? createdAt : 0, facultyEmail, facultyId, "Unknown Faculty");
                                        currentSession.setId(sessionId);
                                        
                                        displaySessionInfo();
                                        checkManualOverridePermission(); 
                                        loadAttendanceData();
                                    }
                                });
                            } else {
                                // No faculty ID, proceed
                                currentSession = new AttendanceSession(branch, year, section, subject, date,
                                        startTime, endTime, status, createdAt != null ? createdAt : 0, facultyEmail, facultyId, "Unknown Faculty");
                                currentSession.setId(sessionId);

                                displaySessionInfo();
                                checkManualOverridePermission(); // Check permissions after loading session
                                loadAttendanceData();
                            }
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
                    }
                });
    }

    private void displaySessionInfo() {
        if (currentSession == null) return;

        String sessionInfo = "📚 Session Information\n\n" +
                "Subject: " + (currentSession.getSubject() != null ? currentSession.getSubject() : "N/A") + "\n" +
                "Class: " + (currentSession.getBranch() != null ? currentSession.getBranch() : "N/A") + 
                " - Year " + (currentSession.getYear() != null ? currentSession.getYear() : "N/A") +
                " - Section " + (currentSession.getSection() != null ? currentSession.getSection() : "N/A") + "\n" +
                "Date: " + (currentSession.getDate() != null ? currentSession.getDate() : "N/A") + "\n" +
                "Time: " + (currentSession.getStartTime() != null ? currentSession.getStartTime() : "N/A") + 
                " - " + (currentSession.getEndTime() != null ? currentSession.getEndTime() : "N/A") + "\n" +
                "Status: " + (Constants.SESSION_ACTIVE.equals(currentSession.getStatus()) ? "🟢 Active" : "🔴 Ended");

        tv_session_info.setText(sessionInfo);
    }

    /**
     * Check if current user can manually override attendance
     * Faculty can override their own sessions, Admins can override any session
     */
    private void checkManualOverridePermission() {
        boolean canOverride = false;
        
        String currentUserEmail = PreferenceManager.getFacultyEmail(this);
        boolean isAdmin = PreferenceManager.isAdmin(this);
        
        if (currentSession != null && currentUserEmail != null) {
            // Admins can override any session
            if (isAdmin) {
                canOverride = true;
            }
            // Faculty can override their own sessions
            else if (currentUserEmail.equals(currentSession.getFacultyEmail()) || 
                     currentUserEmail.equals(currentSession.getFacultyId())) {
                canOverride = true;
            }
        }
        
        // Update adapter
        if (adapter != null) {
            adapter.setCanManualOverride(canOverride);
            if (canOverride) {
                Toast.makeText(this, "Tip: Long press an absent student to mark them present.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadAttendanceData() {
        if (currentSession == null) return;

        String branch = currentSession.getBranch();
        String year = currentSession.getYear();
        String section = currentSession.getSection();

        if (branch == null || year == null || section == null) return;

        studentsRef.orderByChild(Constants.STUDENT_BRANCH).equalTo(branch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<AttendanceRecord> newRecords = new ArrayList<>();
                        int totalStudents = 0;
                        int presentStudents = 0;

                        for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                            String studentYear = studentSnapshot.child(Constants.STUDENT_YEAR).getValue(String.class);
                            String studentSection = studentSnapshot.child(Constants.STUDENT_SECTION).getValue(String.class);

                            if (year.equals(studentYear) && section.equals(studentSection)) {
                                totalStudents++;
                                String enrollmentNo = studentSnapshot.getKey();
                                String studentName = studentSnapshot.child(Constants.STUDENT_NAME).getValue(String.class);
                                String studentEmail = studentSnapshot.child(Constants.STUDENT_EMAIL).getValue(String.class);

                                DataSnapshot attendanceSnapshot = studentSnapshot
                                        .child(Constants.STUDENT_ATTENDANCE)
                                        .child(sessionId);

                                String attendanceStatus = Constants.ATTENDANCE_ABSENT;
                                String markedTime = "Not marked";
                                boolean manualOverride = false;
                                String markedBy = "";

                                if (attendanceSnapshot.exists()) {
                                    // It could be a String (old format) or Object (new format)
                                    // Try to parse as object first? Or check children
                                    if (attendanceSnapshot.hasChild("status")) {
                                        // New format
                                        AttendanceRecord dbRecord = attendanceSnapshot.getValue(AttendanceRecord.class);
                                        if (dbRecord != null) {
                                            attendanceStatus = dbRecord.getStatus();
                                            markedTime = dbRecord.getMarkedTime();
                                            manualOverride = dbRecord.isManualOverride();
                                            markedBy = dbRecord.getMarkedBy();
                                        }
                                    } else {
                                        // Old format (String directly)
                                        String status = attendanceSnapshot.getValue(String.class);
                                        if (Constants.ATTENDANCE_PRESENT.equals(status)) {
                                            attendanceStatus = Constants.ATTENDANCE_PRESENT;
                                            markedTime = "Marked"; 
                                        }
                                    }
                                    
                                    if (Constants.ATTENDANCE_PRESENT.equals(attendanceStatus)) {
                                        presentStudents++;
                                    }
                                }

                                AttendanceRecord record = new AttendanceRecord(
                                        enrollmentNo,
                                        studentName != null ? studentName : "Unknown",
                                        studentEmail != null ? studentEmail : "Unknown",
                                        attendanceStatus,
                                        markedTime,
                                        branch, year, section
                                );
                                record.setManualOverride(manualOverride);
                                record.setMarkedBy(markedBy);
                                
                                newRecords.add(record);
                            }
                        }

                        // update adapter
                        adapter.updateRecords(newRecords);
                        
                         // Sort: Present first, then by name (Default view)
                        adapter.sortRecords(AttendanceReportAdapter.SortCriteria.STATUS_PRESENT_FIRST);

                        updateAttendanceSummary(totalStudents, presentStudents);
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
    
    // --- Manual Override Implementation ---

    @Override
    public void onManualOverride(String enrollmentNo, String studentName, String studentEmail) {
        // Show dialog with remarks input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mark Manual Attendance");
        builder.setMessage("Mark " + studentName + " as present manually?");

        // Set up the input
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter remarks (e.g., Medical Leave, Late)");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Mark Present", (dialog, which) -> {
            String remarks = input.getText().toString().trim();
            if (remarks.isEmpty()) {
                remarks = "Manual Override by Faculty"; // Default remark
            }
            performManualOverride(enrollmentNo, studentName, studentEmail, remarks);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void performManualOverride(String enrollment, String studentName, String studentEmail, String remarks) {
        if (sessionId == null) return;
        
        // Use Faculty ID if name is not available, or hardcode "Faculty"
        String facultyId = PreferenceManager.getFacultyId(this);
        String facultyEmail = PreferenceManager.getFacultyEmail(this);
        String facultyName = "Faculty (" + (facultyId != null ? facultyId : "Admin") + ")";
        
        long timestamp = System.currentTimeMillis();
        
        // Create full record with all 9 required parameters
        // Constructor: (enrollment, name, email, status, manualOverride, markedBy, markedByEmail, remarks, timestamp)
        AttendanceRecord record = new AttendanceRecord(
            enrollment, 
            studentName, 
            studentEmail != null ? studentEmail : "", 
            Constants.ATTENDANCE_PRESENT,
            true, // manualOverride flag
            facultyName, // markedBy
            facultyEmail != null ? facultyEmail : "",
            remarks,
            timestamp
        );
        
        // Save to Firebase
        studentsRef.child(enrollment).child(Constants.STUDENT_ATTENDANCE)
                .child(sessionId).setValue(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AttendanceReportActivity.this, 
                            "Marked " + studentName + " as present", Toast.LENGTH_SHORT).show();
                    // Refresh data
                    loadAttendanceData();
                })
                .addOnFailureListener(e -> {
                     Toast.makeText(AttendanceReportActivity.this, 
                            "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return timeFormat.format(new Date());
    }

    private void exportReport() {
        if (adapter == null || adapter.getItemCount() == 0) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AttendanceRecord> attendanceRecords = new ArrayList<>();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            attendanceRecords.add(adapter.getItem(i));
        }

        try {
            Log.d(TAG, "Starting export process...");
            
            // Validate session
            if (currentSession == null) {
                Toast.makeText(this, "Session data is not fully loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate Excel file
            java.io.File file = ExcelExportUtils.exportAttendanceReport(this, currentSession, attendanceRecords);

            if (file != null && file.exists()) {
                Log.d(TAG, "File created successfully: " + file.getAbsolutePath());
                // Share file using FileProvider
                try {
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            file);
    
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); 
                    
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); 
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                    
                    shareIntent.setClipData(android.content.ClipData.newRawUri("", uri));
    
                    String subject = currentSession.getSubject() != null ? currentSession.getSubject() : "Attendance";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - " + subject);
    
                    Intent chooserIntent = Intent.createChooser(shareIntent, "Export Attendance Report");
                    chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    chooserIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    
                    startActivity(chooserIntent);
                    
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "FileProvider error", e);
                    Toast.makeText(this, "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "File creation returned null or file does not exist");
                Toast.makeText(this, "Error generating Excel file. Please check storage permissions.", Toast.LENGTH_LONG).show();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Export failed VERY BADLY", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}



















