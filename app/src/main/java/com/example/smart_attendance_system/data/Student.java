package com.example.smart_attendance_system.data;
import com.example.smart_attendance_system.data.Student;

public class Student {
    private String rollNo;
    private String name;
    private String email;
    private String branch;
    private String year;
    private String section;
    private String faceEncoding;

    public Student() {
        // Default constructor required for Firebase
    }

    public Student(String rollNo, String name, String email, String branch, String year, String section) {
        this.rollNo = rollNo;
        this.name = name;
        this.email = email;
        this.branch = branch;
        this.year = year;
        this.section = section;
    }

    // Getters and Setters
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }
}

