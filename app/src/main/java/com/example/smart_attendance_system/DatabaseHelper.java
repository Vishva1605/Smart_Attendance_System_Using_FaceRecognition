package com.example.smart_attendance_system;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper instance;
    private FirebaseDatabase database;

    private DatabaseHelper() {
        database = FirebaseDatabase.getInstance();
        // Enable offline persistence
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Firebase persistence already enabled or failed to enable: " + e.getMessage());
        }
    }

    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    public DatabaseReference getStudentsReference() {
        return database.getReference(Constants.STUDENTS_REF);
    }

    public DatabaseReference getAttendanceReportReference() {
        return database.getReference(Constants.ATTENDANCE_REPORT_REF);
    }

    public DatabaseReference getFacultyReference() {
        return database.getReference(Constants.FACULTY_REF);
    }

    // Helper method to create a new student record
    public void createStudentRecord(String enrollmentNo, String studentName, String email,
                                    String branch, String year, String section) {
        DatabaseReference studentRef = getStudentsReference().child(enrollmentNo);

        Map<String, Object> studentData = new HashMap<>();
        studentData.put(Constants.STUDENT_NAME, studentName);
        studentData.put(Constants.STUDENT_EMAIL, email);
        studentData.put(Constants.STUDENT_BRANCH, branch);
        studentData.put(Constants.STUDENT_YEAR, year);
        studentData.put(Constants.STUDENT_SECTION, section);
        studentData.put("created_at", System.currentTimeMillis());
        studentData.put("updated_at", System.currentTimeMillis());

        studentRef.setValue(studentData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Student record created successfully for: " + enrollmentNo))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create student record for: " + enrollmentNo, e));
    }

    // Helper method to create a faculty record
    public void createFacultyRecord(String facultyId, String facultyName, String email,
                                    String department) {
        DatabaseReference facultyRef = getFacultyReference().child(facultyId);

        Map<String, Object> facultyData = new HashMap<>();
        facultyData.put(Constants.FACULTY_NAME, facultyName);
        facultyData.put(Constants.FACULTY_EMAIL, email);
        facultyData.put(Constants.FACULTY_DEPARTMENT, department);
        facultyData.put("created_at", System.currentTimeMillis());
        facultyData.put("updated_at", System.currentTimeMillis());

        facultyRef.setValue(facultyData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Faculty record created successfully for: " + facultyId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create faculty record for: " + facultyId, e));
    }

    // Helper method to mark attendance
    public void markAttendance(String enrollmentNo, String sessionId, String status) {
        DatabaseReference attendanceRef = getStudentsReference()
                .child(enrollmentNo)
                .child(Constants.STUDENT_ATTENDANCE)
                .child(sessionId);

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("status", status);
        attendanceData.put("marked_at", System.currentTimeMillis());

        attendanceRef.setValue(status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance marked successfully for: " + enrollmentNo + " in session: " + sessionId);
                    // Also update the timestamp
                    attendanceRef.getParent().child("last_updated").setValue(System.currentTimeMillis());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark attendance for: " + enrollmentNo, e));
    }

    // Helper method to create attendance session
    public void createAttendanceSession(String sessionId, String branch, String year,
                                        String section, String subject, String date,
                                        String startTime, String endTime, String facultyEmail) {
        DatabaseReference sessionRef = getAttendanceReportReference().child(sessionId);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put(Constants.SESSION_BRANCH, branch);
        sessionData.put(Constants.SESSION_YEAR, year);
        sessionData.put(Constants.SESSION_SECTION, section);
        sessionData.put(Constants.SESSION_SUBJECT, subject);
        sessionData.put(Constants.SESSION_DATE, date);
        sessionData.put(Constants.SESSION_START_TIME, startTime);
        sessionData.put(Constants.SESSION_END_TIME, endTime);
        sessionData.put(Constants.SESSION_STATUS, Constants.SESSION_ACTIVE);
        sessionData.put("faculty_email", facultyEmail);
        sessionData.put("created_at", System.currentTimeMillis());
        sessionData.put("updated_at", System.currentTimeMillis());

        sessionRef.setValue(sessionData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Attendance session created successfully: " + sessionId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create attendance session: " + sessionId, e));
    }

    // Helper method to end attendance session
    public void endAttendanceSession(String sessionId) {
        DatabaseReference sessionRef = getAttendanceReportReference().child(sessionId);

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.SESSION_STATUS, Constants.SESSION_ENDED);
        updates.put("ended_at", System.currentTimeMillis());
        updates.put("updated_at", System.currentTimeMillis());

        sessionRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Attendance session ended successfully: " + sessionId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to end attendance session: " + sessionId, e));
    }

    // Helper method to update student device ID
    public void updateStudentDeviceId(String enrollmentNo, String deviceId) {
        DatabaseReference studentRef = getStudentsReference().child(enrollmentNo);

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.STUDENT_HARDWARE_ID, deviceId);
        updates.put("device_registered_at", System.currentTimeMillis());
        updates.put("updated_at", System.currentTimeMillis());

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Device ID updated successfully for: " + enrollmentNo))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update device ID for: " + enrollmentNo, e));
    }

    // Helper method to get database reference by path
    public DatabaseReference getReference(String path) {
        return database.getReference(path);
    }

    // Helper method to check database connection
    public void checkConnection() {
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d(TAG, "Connected to Firebase Database");
                } else {
                    Log.d(TAG, "Disconnected from Firebase Database");
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Log.w(TAG, "Database connection listener was cancelled");
            }
        });
    }
}