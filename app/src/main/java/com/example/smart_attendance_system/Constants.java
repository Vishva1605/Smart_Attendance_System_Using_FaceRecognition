package com.example.smart_attendance_system;

public class Constants {
    
    // Firebase Database References
    public static final String STUDENTS_REF = "students";
    public static final String FACULTY_REF = "faculty";
    public static final String ATTENDANCE_REPORT_REF = "attendance_sessions";
    public static final String ATTENDANCE_RECORDS_REF = "attendance_records";
    
    // Student Fields
    public static final String STUDENT_NAME = "name";
    public static final String STUDENT_EMAIL = "email";
    public static final String STUDENT_BRANCH = "branch";
    public static final String STUDENT_YEAR = "year";
    public static final String STUDENT_SECTION = "section";
    public static final String STUDENT_HARDWARE_ID = "hardware_id";
    public static final String STUDENT_FACE_DATA = "face_data";
    public static final String STUDENT_ATTENDANCE = "attendance";
    
    // Faculty Fields
    public static final String FACULTY_NAME = "name";
    public static final String FACULTY_EMAIL = "email";
    public static final String FACULTY_DEPARTMENT = "department";
    
    // Session Fields
    public static final String SESSION_ID = "session_id";
    public static final String SESSION_BRANCH = "branch";
    public static final String SESSION_YEAR = "year";
    public static final String SESSION_SECTION = "section";
    public static final String SESSION_SUBJECT = "subject";
    public static final String SESSION_DATE = "date";
    public static final String SESSION_START_TIME = "start_time";
    public static final String SESSION_END_TIME = "end_time";
    public static final String SESSION_STATUS = "status";
    public static final String SESSION_FACULTY_ID = "faculty_id";
    public static final String SESSION_CREATED_AT = "created_at";
    
    // Session Status Values
    public static final String SESSION_ACTIVE = "active";
    public static final String SESSION_ENDED = "ended";
    public static final String SESSION_EXPIRED = "expired";
    
    // Attendance Status
    public static final String ATTENDANCE_PRESENT = "present";
    public static final String ATTENDANCE_ABSENT = "absent";
    
    // Intent Extras
    public static final String EXTRA_BRANCH = "extra_branch";
    public static final String EXTRA_YEAR = "extra_year";
    public static final String EXTRA_SECTION = "extra_section";
    public static final String EXTRA_SUBJECT = "extra_subject";
    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_SESSION_SUBJECT = "extra_session_subject";
    public static final String EXTRA_SESSION_DATE = "extra_session_date";
    public static final String EXTRA_SESSION_TIME = "extra_session_time";
    public static final String EXTRA_IS_REGISTRATION = "extra_is_registration";
    public static final String EXTRA_IS_ATTENDANCE = "extra_is_attendance";
    public static final String EXTRA_IS_LIVE_VIEW = "extra_is_live_view";
    public static final String EXTRA_FACE_VERIFIED = "extra_face_verified";
    public static final String EXTRA_FACE_CAPTURE_SUCCESS = "extra_face_capture_success";
    public static final String EXTRA_FACE_IMAGE_BASE64 = "extra_face_image_base64";
    public static final String EXTRA_IS_FIRST_TIME_REGISTRATION = "extra_is_first_time_registration";
    
    // Validation Constants
    public static final String REQUIRED_EMAIL_DOMAIN = "saec.ac.in";
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final float FACE_MATCH_THRESHOLD = 0.75f;
    public static final int FACE_JPEG_QUALITY = 80;
    
    // Network Constants
    public static final String UNIVERSITY_WIFI_BSSID = "98:25:4a:25:7a:b7";
    public static final String PLACEHOLDER_BSSID = "02:00:00:00:00:00";
    
    // Date Formats
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    public static final String TIME_FORMAT = "HH:mm";
    
    // Preferences Name
    public static final String PREF_NAME = "SmartAttendancePrefs";
    
    // Additional Preference Keys
    public static final String PREF_ENROLLMENT_NO = "enrollment_no";
    public static final String PREF_LAST_LOGIN = "last_login";
    
    // Animation Durations
    public static final int ANIMATION_DURATION_SHORT = 300;
    public static final int ANIMATION_DURATION_MEDIUM = 500;
    public static final int ANIMATION_DURATION_LONG = 800;
    
    // Success Messages
    public static final String SUCCESS_LOGIN = "Login successful!";
    public static final String SUCCESS_ATTENDANCE_MARKED = "Attendance marked successfully!";
    public static final String SUCCESS_SESSION_CREATED = "Attendance session created successfully!";
    public static final String SUCCESS_SESSION_ENDED = "Session ended successfully!";
    public static final String SUCCESS_FACE_REGISTERED = "Face registered successfully!";
    public static final String SUCCESS_FACE_VERIFIED = "Face verification successful!";
    
    // Error Messages
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed. Please check your credentials.";
    public static final String ERROR_NETWORK_UNAVAILABLE = "Network connection unavailable. Please check your internet connection.";
    public static final String ERROR_STUDENT_NOT_FOUND = "Student record not found. Please contact administrator.";
    public static final String ERROR_FACULTY_NOT_FOUND = "Faculty record not found. Please contact administrator.";
    public static final String ERROR_DEVICE_NOT_AUTHORIZED = "This device is not authorized for your account. Please use your registered device.";
    public static final String ERROR_WIFI_NOT_CONNECTED = "Please connect to university WiFi network to mark attendance.";
    public static final String ERROR_LOCATION_PERMISSION_REQUIRED = "Location permission is required to detect WiFi network.";
    public static final String ERROR_INVALID_EMAIL_DOMAIN = "Please use your university email address (@saec.ac.in)";
    public static final String ERROR_ATTENDANCE_ALREADY_MARKED = "You have already marked attendance for this session.";
    public static final String ERROR_SESSION_NOT_FOUND = "No active session found for your class.";
    public static final String ERROR_SESSION_EXPIRED = "The attendance session has expired.";
    public static final String ERROR_FACE_NOT_DETECTED = "No face detected. Please ensure proper lighting and positioning.";
    public static final String ERROR_FACE_VERIFICATION_FAILED = "Face verification failed. Please try again.";
    public static final String ERROR_FACE_NOT_REGISTERED = "Face not registered. Please register your face first.";

    // Preferences Keys
    public static final String PREF_STUDENT_ENROLLMENT = "student_enrollment";
    public static final String PREF_STUDENT_NAME = "student_name";
    public static final String PREF_STUDENT_EMAIL = "student_email";
    public static final String PREF_STUDENT_BRANCH = "student_branch";
    public static final String PREF_STUDENT_YEAR = "student_year";
    public static final String PREF_STUDENT_SECTION = "student_section";
    public static final String PREF_FACULTY_ID = "faculty_id";
    public static final String PREF_FACULTY_EMAIL = "faculty_email";
    public static final String PREF_FACE_REGISTERED = "face_registered";
    public static final String PREF_FACE_REGISTRATION_COMPLETED = "face_registration_completed";
    public static final String PREF_FIRST_LOGIN_COMPLETED = "first_login_completed";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_USER_TYPE = "user_type";
    
    // User Types
    public static final String USER_TYPE_STUDENT = "student";
    public static final String USER_TYPE_FACULTY = "faculty";
    
    // Broadcast Actions
    public static final String ACTION_SESSION_AUTO_CLOSED = "com.example.smart_attendance_system.SESSION_AUTO_CLOSED";
    
    // Auto-close settings
    public static final long SESSION_AUTO_CLOSE_TIME = 30 * 60 * 1000; // 30 minutes
    
    // Success Messages
    public static final String SUCCESS_PASSWORD_RESET = "Password reset successfully!";
    public static final String SUCCESS_FACE_REGISTRATION_FIRST_TIME = "Face registered successfully! You can now mark attendance using face verification.";
}