package com.example.smart_attendance_system;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    
    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // Student Info Methods
    public static void saveStudentInfo(Context context, String enrollmentNo, String email, 
                                     String name, String branch, String year, String section) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(Constants.PREF_STUDENT_ENROLLMENT, enrollmentNo);
        editor.putString(Constants.PREF_STUDENT_EMAIL, email);
        editor.putString(Constants.PREF_STUDENT_NAME, name);
        editor.putString(Constants.PREF_STUDENT_BRANCH, branch);
        editor.putString(Constants.PREF_STUDENT_YEAR, year);
        editor.putString(Constants.PREF_STUDENT_SECTION, section);
        editor.putString(Constants.PREF_USER_TYPE, Constants.USER_TYPE_STUDENT);
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    public static String getEnrollmentNo(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_ENROLLMENT, null);
    }
    
    public static String getStudentName(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_NAME, null);
    }
    
    public static String getStudentEmail(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_EMAIL, null);
    }
    
    public static String getStudentBranch(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_BRANCH, null);
    }
    
    public static String getStudentYear(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_YEAR, null);
    }
    
    public static String getStudentSection(Context context) {
        return getPreferences(context).getString(Constants.PREF_STUDENT_SECTION, null);
    }
    
    public static void clearStudentInfo(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.remove(Constants.PREF_STUDENT_ENROLLMENT);
        editor.remove(Constants.PREF_STUDENT_EMAIL);
        editor.remove(Constants.PREF_STUDENT_NAME);
        editor.remove(Constants.PREF_STUDENT_BRANCH);
        editor.remove(Constants.PREF_STUDENT_YEAR);
        editor.remove(Constants.PREF_STUDENT_SECTION);
        editor.remove(Constants.PREF_IS_LOGGED_IN);
        editor.remove(Constants.PREF_USER_TYPE);
        editor.apply();
    }
    
    // Faculty Info Methods
    public static void saveFacultyInfo(Context context, String facultyId, String email) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(Constants.PREF_FACULTY_ID, facultyId);
        editor.putString(Constants.PREF_FACULTY_EMAIL, email);
        editor.putString(Constants.PREF_USER_TYPE, Constants.USER_TYPE_FACULTY);
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    public static String getFacultyId(Context context) {
        return getPreferences(context).getString(Constants.PREF_FACULTY_ID, null);
    }
    
    public static String getFacultyEmail(Context context) {
        return getPreferences(context).getString(Constants.PREF_FACULTY_EMAIL, null);
    }
    
    public static void clearFacultyInfo(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.remove(Constants.PREF_FACULTY_ID);
        editor.remove(Constants.PREF_FACULTY_EMAIL);
        editor.remove(Constants.PREF_IS_LOGGED_IN);
        editor.remove(Constants.PREF_USER_TYPE);
        editor.apply();
    }
    
    // Face Registration Methods
    public static void setFaceRegistered(Context context, boolean registered) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(Constants.PREF_FACE_REGISTERED, registered);
        editor.apply();
    }
    
    public static boolean isFaceRegistered(Context context) {
        return getPreferences(context).getBoolean(Constants.PREF_FACE_REGISTERED, false);
    }
    
    // First Login Methods
    public static void setFirstLoginCompleted(Context context, boolean completed) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(Constants.PREF_FIRST_LOGIN_COMPLETED, completed);
        editor.apply();
    }
    
    public static boolean isFirstLoginCompleted(Context context) {
        return getPreferences(context).getBoolean(Constants.PREF_FIRST_LOGIN_COMPLETED, false);
    }
    
    // General Methods
    public static boolean isLoggedIn(Context context) {
        return getPreferences(context).getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }
    
    public static String getUserType(Context context) {
        return getPreferences(context).getString(Constants.PREF_USER_TYPE, null);
    }
    
    public static void clearAllPreferences(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.clear();
        editor.apply();
    }
}