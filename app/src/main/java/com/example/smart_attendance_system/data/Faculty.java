package com.example.smart_attendance_system.data;
import com.example.smart_attendance_system.data.Faculty;

/**
 * Represents a faculty member in the system.
 */
public class Faculty {
    private String id;
    private String name;
    private String email;
    private String department;
    private boolean isAdmin; // Admin role flag for elevated permissions
    
    public Faculty() {
        this.isAdmin = false;
    }
    
    public Faculty(String id, String name, String email, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.isAdmin = false;
    }

    public Faculty(String name, String email, String department, boolean isAdmin) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.isAdmin = isAdmin;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}



















