package com.example.smart_attendance_system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SelectAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "SelectAttendance";

    private Button btn_mark_present, btn_back, btn_refresh;
    private TextView tv_session_info, tv_student_info;
    private DatabaseReference studentsRef, attendanceReportRef;

    private String enrollmentNo;
    private String activeSessionId;
    private String studentBranch, studentYear, studentSection, studentName;
    private boolean isMarkingAttendance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_select_attendance);
            Log.d(TAG, "onCreate: Activity created successfully");
            
            initializeViews();
            setupClickListeners();
            loadStudentInfoAndFindSessions();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing activity", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            btn_mark_present = findViewById(R.id.btn_mark_present);
            btn_back = findViewById(R.id.btn_back);
            btn_refresh = findViewById(R.id.btn_refresh);
            tv_session_info = findViewById(R.id.tv_session_info);
            tv_student_info = findViewById(R.id.tv_student_info);

            // Initialize Firebase references
            studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);
            attendanceReportRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);

            // Initially disable the mark present button
            btn_mark_present.setEnabled(false);
            
            Log.d(TAG, "Views initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e; // Re-throw to be caught in onCreate
        }
    }

    private void setupClickListeners() {
        try {
            btn_mark_present.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Mark present button clicked");
                    markAttendance();
                }
            });

            btn_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Back button clicked");
                    finish();
                }
            });

            btn_refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Refresh button clicked");
                    refreshSessions();
                }
            });
            
            Log.d(TAG, "Click listeners set up successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadStudentInfoAndFindSessions() {
        try {
            // Get student info from preferences
            enrollmentNo = PreferenceManager.getEnrollmentNo(this);
            studentName = PreferenceManager.getStudentName(this);
            studentBranch = PreferenceManager.getStudentBranch(this);
            studentYear = PreferenceManager.getStudentYear(this);
            studentSection = PreferenceManager.getStudentSection(this);

            Log.d(TAG, "Student info loaded - Enrollment: " + enrollmentNo + 
                      ", Branch: " + studentBranch + ", Year: " + studentYear + 
                      ", Section: " + studentSection);

            if (enrollmentNo == null || studentBranch == null || studentYear == null || studentSection == null) {
                Log.e(TAG, "Student information is incomplete");
                Toast.makeText(this, "Student information not found. Please login again.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            updateStudentInfo();
            findActiveSessionsForStudent();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading student info: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading student information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateStudentInfo() {
        try {
            String studentInfo = "👤 Student Information\n" +
                    "Name: " + (studentName != null ? studentName : "Unknown") + "\n" +
                    "Enrollment: " + (enrollmentNo != null ? enrollmentNo : "Unknown") + "\n" +
                    "Class: " + studentBranch + " - Year " + studentYear + " - Section " + studentSection;

            tv_student_info.setText(studentInfo);
            Log.d(TAG, "Student info updated in UI");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating student info: " + e.getMessage(), e);
        }
    }

    private void findActiveSessionsForStudent() {
        try {
            tv_session_info.setText("Searching for active sessions...");
            Log.d(TAG, "Starting to search for active sessions");

            attendanceReportRef.orderByChild(Constants.SESSION_STATUS)
                    .equalTo(Constants.SESSION_ACTIVE)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                Log.d(TAG, "Received session data, count: " + dataSnapshot.getChildrenCount());
                                
                                String validSessionId = null;
                                DataSnapshot validSessionSnapshot = null;

                                // Get current date for comparison
                                SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
                                String currentDate = dateFormat.format(new Date());

                                Log.d(TAG, "Searching sessions for date: " + currentDate);
                                Log.d(TAG, "Student class: " + studentBranch + "-" + studentYear + "-" + studentSection);

                                for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                                    String sessionId = sessionSnapshot.getKey();
                                    String sessionBranch = sessionSnapshot.child(Constants.SESSION_BRANCH).getValue(String.class);
                                    String sessionYear = sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class);
                                    String sessionSection = sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class);
                                    String sessionDate = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
                                    String sessionStatus = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);

                                    Log.d(TAG, "Checking session: " + sessionId +
                                            " - Branch: " + sessionBranch + ", Year: " + sessionYear +
                                            ", Section: " + sessionSection + ", Date: " + sessionDate +
                                            ", Status: " + sessionStatus);

                                    // Check if session matches student's class and is active
                                    boolean branchMatch = sessionBranch != null && sessionBranch.equals(studentBranch);
                                    boolean yearMatch = sessionYear != null && sessionYear.equals(studentYear);
                                    boolean sectionMatch = sessionSection != null && sessionSection.equals(studentSection);
                                    boolean dateMatch = sessionDate != null && sessionDate.equals(currentDate);
                                    boolean statusActive = Constants.SESSION_ACTIVE.equals(sessionStatus);

                                    if (branchMatch && yearMatch && sectionMatch && dateMatch && statusActive) {
                                        // Check if session is currently active (time-based)
                                        if (isSessionTimeActive(sessionSnapshot)) {
                                            validSessionId = sessionId;
                                            validSessionSnapshot = sessionSnapshot;
                                            Log.d(TAG, "Found valid active session: " + sessionId);
                                            break;
                                        } else {
                                            Log.d(TAG, "Session time not active: " + sessionId);
                                        }
                                    }
                                }

                                if (validSessionId != null && validSessionSnapshot != null) {
                                    activeSessionId = validSessionId;
                                    displayActiveSession(validSessionSnapshot);
                                    btn_mark_present.setEnabled(true);
                                } else {
                                    displayNoActiveSession();
                                    btn_mark_present.setEnabled(false);
                                }
                                
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing session data: " + e.getMessage(), e);
                                displayNoActiveSession();
                                btn_mark_present.setEnabled(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error finding sessions: " + error.getMessage());
                            tv_session_info.setText("❌ Error loading sessions\nPlease try again");
                            Toast.makeText(SelectAttendanceActivity.this,
                                    "Error loading sessions: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error in findActiveSessionsForStudent: " + e.getMessage(), e);
            displayNoActiveSession();
        }
    }

    private boolean isSessionTimeActive(DataSnapshot sessionSnapshot) {
        try {
            String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
            String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);

            if (startTime != null && endTime != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date currentTime = new Date();
                Date sessionStart = timeFormat.parse(startTime);
                Date sessionEnd = timeFormat.parse(endTime);

                // Set dates to today for comparison
                Calendar cal = Calendar.getInstance();

                Calendar startCal = Calendar.getInstance();
                startCal.setTime(sessionStart);
                startCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

                Calendar endCal = Calendar.getInstance();
                endCal.setTime(sessionEnd);
                endCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

                // Allow attendance within session time + 30 minutes buffer
                endCal.add(Calendar.MINUTE, 30);

                boolean isActive = currentTime.after(startCal.getTime()) && currentTime.before(endCal.getTime());
                Log.d(TAG, "Session time check - Start: " + startTime + ", End: " + endTime + ", Active: " + isActive);
                return isActive;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing session time: " + e.getMessage());
        }
        return true; // Allow attendance if time parsing fails
    }

    private void displayActiveSession(DataSnapshot sessionSnapshot) {
        try {
            String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
            String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
            String startTime = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
            String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);

            String sessionInfo = "📚 Active Session Found!\n\n" +
                    "Subject: " + (subject != null ? subject : "Unknown") + "\n" +
                    "Date: " + (date != null ? date : "Unknown") + "\n" +
                    "Time: " + (startTime != null ? startTime : "Unknown") + " - " + (endTime != null ? endTime : "Unknown") + "\n" +
                    "Class: " + studentBranch + " - Year " + studentYear + " - Section " + studentSection + "\n\n" +
                    "✅ You can mark your attendance now!";

            tv_session_info.setText(sessionInfo);

            // Update button appearance
            btn_mark_present.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            btn_mark_present.setText("Mark Present");
            
            Log.d(TAG, "Active session displayed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying active session: " + e.getMessage(), e);
        }
    }

    private void displayNoActiveSession() {
        try {
            String message = "📋 No Active Sessions\n\n" +
                    "No active attendance session found for your class.\n\n" +
                    "Possible reasons:\n" +
                    "• No session is currently running\n" +
                    "• Session is not for your branch/year/section\n" +
                    "• You have already marked attendance\n" +
                    "• Session has expired\n\n" +
                    "Please check with your faculty or try refreshing.";

            tv_session_info.setText(message);

            // Update button appearance
            btn_mark_present.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btn_mark_present.setText("No Active Session");
            
            Log.d(TAG, "No active session message displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying no active session: " + e.getMessage(), e);
        }
    }

    private void markAttendance() {
        if (isMarkingAttendance) {
            Log.d(TAG, "Already marking attendance, ignoring duplicate request");
            return;
        }

        isMarkingAttendance = true;
        btn_mark_present.setEnabled(false);
        btn_mark_present.setText("Marking...");

        try {
            Log.d(TAG, "Starting attendance marking process");
            
            // Check if face verification is required and enabled
            boolean requireFaceVerification = PreferenceManager.isFaceRegistered(this);
            boolean faceVerified = getIntent().getBooleanExtra(Constants.EXTRA_FACE_VERIFIED, false);
            
            // Check if face is registered (mandatory for attendance)
            if (!requireFaceVerification) {
                isMarkingAttendance = false;
                btn_mark_present.setEnabled(true);
                btn_mark_present.setText("Mark Present");
                Toast.makeText(this, "Face registration is required before marking attendance. Please register your face first.", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Face verification required: " + requireFaceVerification + ", Face verified: " + faceVerified);

            if (requireFaceVerification && !faceVerified) {
                isMarkingAttendance = false;
                btn_mark_present.setEnabled(true);
                btn_mark_present.setText("Mark Present");
                // Redirect to face verification
                Intent intent = new Intent(this, FaceVerificationActivity.class);
                intent.putExtra(Constants.EXTRA_SESSION_ID, activeSessionId);
                startActivity(intent);
                return;
            }

            if (enrollmentNo == null || activeSessionId == null) {
                isMarkingAttendance = false;
                btn_mark_present.setEnabled(true);
                btn_mark_present.setText("Mark Present");
                Toast.makeText(this, "Unable to mark attendance. Please try again.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Missing enrollment number or session ID");
                return;
            }

            Log.d(TAG, "Marking attendance for student: " + enrollmentNo + ", session: " + activeSessionId);

            // Check if attendance is already marked
            DatabaseReference attendanceRef = studentsRef.child(enrollmentNo)
                    .child(Constants.STUDENT_ATTENDANCE)
                    .child(activeSessionId);

            attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        if (dataSnapshot.exists()) {
                            String existingAttendance = dataSnapshot.getValue(String.class);
                            if (Constants.ATTENDANCE_PRESENT.equals(existingAttendance)) {
                                isMarkingAttendance = false;
                                Toast.makeText(SelectAttendanceActivity.this,
                                        Constants.ERROR_ATTENDANCE_ALREADY_MARKED, Toast.LENGTH_LONG).show();
                                navigateToSuccessScreen();
                            } else {
                                updateAttendanceToPresent(attendanceRef);
                            }
                        } else {
                            updateAttendanceToPresent(attendanceRef);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing attendance check: " + e.getMessage(), e);
                        isMarkingAttendance = false;
                        btn_mark_present.setEnabled(true);
                        btn_mark_present.setText("Mark Present");
                        Toast.makeText(SelectAttendanceActivity.this,
                                "Error checking attendance status", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking attendance: " + error.getMessage());
                    isMarkingAttendance = false;
                    btn_mark_present.setEnabled(true);
                    btn_mark_present.setText("Mark Present");
                    Toast.makeText(SelectAttendanceActivity.this,
                            "Error checking attendance status: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in markAttendance: " + e.getMessage(), e);
            isMarkingAttendance = false;
            btn_mark_present.setEnabled(true);
            btn_mark_present.setText("Mark Present");
            Toast.makeText(this, "Error marking attendance. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAttendanceToPresent(DatabaseReference attendanceRef) {
        try {
            Log.d(TAG, "Updating attendance to present");
            attendanceRef.setValue(Constants.ATTENDANCE_PRESENT)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            isMarkingAttendance = false;
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Attendance marked successfully");
                                Toast.makeText(SelectAttendanceActivity.this,
                                        Constants.SUCCESS_ATTENDANCE_MARKED, Toast.LENGTH_SHORT).show();
                                navigateToSuccessScreen();
                            } else {
                                Log.e(TAG, "Failed to mark attendance: " + task.getException());
                                btn_mark_present.setEnabled(true);
                                btn_mark_present.setText("Mark Present");
                                Toast.makeText(SelectAttendanceActivity.this,
                                        "Failed to mark attendance. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error updating attendance: " + e.getMessage(), e);
            isMarkingAttendance = false;
            btn_mark_present.setEnabled(true);
            btn_mark_present.setText("Mark Present");
            Toast.makeText(this, "Error updating attendance", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToSuccessScreen() {
        try {
            Intent intent = new Intent(this, PresenceRecordedActivity.class);
            intent.putExtra(Constants.EXTRA_SESSION_SUBJECT, "Attendance Session");
            intent.putExtra(Constants.EXTRA_SESSION_DATE,
                    new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT, Locale.getDefault()).format(new Date()));
            intent.putExtra(Constants.EXTRA_SESSION_TIME, 
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to success screen: " + e.getMessage(), e);
            finish();
        }
    }

    private void refreshSessions() {
        try {
            btn_mark_present.setEnabled(false);
            isMarkingAttendance = false;
            findActiveSessionsForStudent();
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing sessions: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isMarkingAttendance = false;
        Log.d(TAG, "Activity destroyed");
    }
}