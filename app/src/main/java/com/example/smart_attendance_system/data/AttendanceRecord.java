package com.example.smart_attendance_system.data;

/**
 * Represents a single attendance record for a student in a specific session.
 * Supports both automatic (face detection) and manual (faculty override) marking.
 */
public class AttendanceRecord {
    private String enrollmentNo;
    private String studentName;
    private String email;
    private String status; // "present" or "absent"
    private String markedTime;
    
    // Session context (optional, helpful for flattened lists)
    private String branch;
    private String year;
    private String section;
    private String subject; // Added for student view
    private String date;    // Added for student view

    // Manual override audit trail fields
    private boolean manualOverride; // true if marked manually by faculty/admin
    private String markedBy; // faculty name who did manual override
    private String markedByEmail; // faculty email for audit purposes
    private String remarks; // Reason for manual override
    private long timestamp;

    public AttendanceRecord() {
        this.manualOverride = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for basic record
    public AttendanceRecord(String enrollmentNo, String studentName, String email, String status, String markedTime) {
        this.enrollmentNo = enrollmentNo;
        this.studentName = studentName;
        this.email = email;
        this.status = status;
        this.markedTime = markedTime;
        this.manualOverride = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for record with session info
    public AttendanceRecord(String enrollmentNo, String studentName, String email, 
                           String status, String markedTime, String branch, 
                           String year, String section) {
        this(enrollmentNo, studentName, email, status, markedTime);
        this.branch = branch;
        this.year = year;
        this.section = section;
    }

    // Constructor for Student View (Subject/Date focused)
    public AttendanceRecord(String subject, String date, String status, String markedTime) {
        this.subject = subject;
        this.date = date;
        this.status = status;
        this.markedTime = markedTime;
        this.manualOverride = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for manual override
    public AttendanceRecord(String enrollmentNo, String studentName, String email, String status, 
                           boolean manualOverride, String markedBy, String markedByEmail, String remarks, long timestamp) {
        this.enrollmentNo = enrollmentNo;
        this.studentName = studentName;
        this.email = email;
        this.status = status;
        this.manualOverride = manualOverride;
        this.markedBy = markedBy;
        this.markedByEmail = markedByEmail;
        this.remarks = remarks;
        this.timestamp = timestamp;
        this.markedTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
    }

    // Getters and Setters
    public String getEnrollmentNo() { return enrollmentNo; }
    public void setEnrollmentNo(String enrollmentNo) { this.enrollmentNo = enrollmentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMarkedTime() { return markedTime; }
    public void setMarkedTime(String markedTime) { this.markedTime = markedTime; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isManualOverride() { return manualOverride; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }

    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }

    public String getMarkedByEmail() { return markedByEmail; }
    public void setMarkedByEmail(String markedByEmail) { this.markedByEmail = markedByEmail; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", enrollmentNo, studentName, status);
    }
}
