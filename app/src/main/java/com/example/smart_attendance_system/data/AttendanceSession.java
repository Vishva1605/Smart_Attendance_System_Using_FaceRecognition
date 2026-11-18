package com.example.smart_attendance_system.data;

/**
 * Represents a classroom attendance session.
 */
public class AttendanceSession {
    private String id;
    private String branch;
    private String year;
    private String section;
    private String subject;
    private String date;
    private String startTime;
    private String endTime;
    private String status; // "active", "ended", "expired"
    private long createdAt;
    private String facultyEmail;
    private String facultyId;
    private String facultyName;
    
    public AttendanceSession() {}
    
    public AttendanceSession(String branch, String year, String section, String subject,
                           String date, String startTime, String endTime, String status,
                           long createdAt, String facultyEmail, String facultyId, String facultyName) {
        this.branch = branch;
        this.year = year;
        this.section = section;
        this.subject = subject;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
        this.facultyEmail = facultyEmail;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public String getFacultyEmail() { return facultyEmail; }
    public void setFacultyEmail(String facultyEmail) { this.facultyEmail = facultyEmail; }
    
    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
}
