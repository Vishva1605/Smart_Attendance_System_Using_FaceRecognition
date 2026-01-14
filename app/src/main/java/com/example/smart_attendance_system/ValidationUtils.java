package com.example.smart_attendance_system;

import android.util.Patterns;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final String TAG = "ValidationUtils";

    /**
     * Validate email domain for university emails.
     */
    public static boolean isValidEmailDomain(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return email.toLowerCase().endsWith("@" + Constants.REQUIRED_EMAIL_DOMAIN);
    }

    /**
     * Validate email format and domain.
     */
    public static boolean isValidUniversityEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Check email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        // Check domain
        return isValidEmailDomain(email);
    }

    /**
     * Validate password strength.
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return false;
        }

        // Add more password validation rules if needed
        return true;
    }

    /**
     * Validate enrollment number format.
     */
    public static boolean isValidEnrollmentNumber(String enrollmentNo) {
        if (enrollmentNo == null || enrollmentNo.trim().isEmpty()) {
            return false;
        }

        // Example format: 2021CS001 (Year + Branch + Number)
        Pattern pattern = Pattern.compile("^\\d{4}[A-Z]{2,3}\\d{3}$");
        return pattern.matcher(enrollmentNo.toUpperCase()).matches();
    }

    /**
     * Validate student name.
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Check minimum length
        if (name.trim().length() < 2) {
            return false;
        }

        // Check for valid characters (letters, spaces, dots, hyphens)
        Pattern pattern = Pattern.compile("^[a-zA-Z\\s.-]+$");
        return pattern.matcher(name).matches();
    }

    /**
     * Validate branch code.
     */
    public static boolean isValidBranch(String branch) {
        if (branch == null || branch.trim().isEmpty()) {
            return false;
        }

        String[] validBranches = {"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL"};
        String upperBranch = branch.toUpperCase();

        for (String validBranch : validBranches) {
            if (validBranch.equals(upperBranch)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate year.
     */
    public static boolean isValidYear(String year) {
        if (year == null || year.trim().isEmpty()) {
            return false;
        }

        try {
            int yearInt = Integer.parseInt(year);
            return yearInt >= 1 && yearInt <= 4;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate section.
     */
    public static boolean isValidSection(String section) {
        if (section == null || section.trim().isEmpty()) {
            return false;
        }

        // Sections A-Z
        Pattern pattern = Pattern.compile("^[A-Z]$");
        return pattern.matcher(section.toUpperCase()).matches();
    }

    /**
     * Get validation error message for email.
     */
    public static String getEmailValidationError(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email cannot be empty";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address";
        }

        if (!isValidEmailDomain(email)) {
            return Constants.ERROR_INVALID_EMAIL_DOMAIN;
        }

        return null; // No error
    }

    /**
     * Get validation error message for password.
     */
    public static String getPasswordValidationError(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters";
        }

        return null; // No error
    }
}