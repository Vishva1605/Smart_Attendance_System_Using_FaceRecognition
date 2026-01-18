# 📱 Smart Attendance System (Android)

This repository contains an Android-based **Smart Attendance System** developed using Java and Firebase.  
The application is designed for academic institutions to manage attendance securely and efficiently, with features to prevent proxy attendance.

---

## 📂 Project Structure Overview

The project follows the standard **Android application structure**, organized for clarity and maintainability.

app/
├── .gitignore
├── build.gradle
├── google-services.json
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── res/
    │   │   ├── layout/
    │   │   ├── drawable/
    │   │   ├── mipmap-*/
    │   │   ├── values/
    │   │   ├── values-night/
    │   │   └── xml/
    │   └── java/
    │       └── com/
    │           └── example/
    │               └── smart_attendance_system/
    ├── test/
    └── androidTest/

---

## 🧩 Java Package Structure

All core logic is located under:

java/com/example/smart_attendance_system/


### 🔐 Authentication & Entry
- `MainActivity.java` – Application entry point
- `LoginActivity.java` – Common login handler
- `LoginStudentActivity.java` – Student login
- `LoginFacultyActivity.java` – Faculty login
- `ResetPasswordActivity.java` – Password recovery

---

### 👨‍🎓 Student Module
- `StudentDashboardActivity.java` – Student home screen
- `FaceRegistrationActivity.java` – One-time face registration
- `FaceVerificationActivity.java` – Face verification before attendance
- `PresenceRecordedActivity.java` – Attendance confirmation screen
- `ActivityAttendanceReportStudent.java` – Student attendance report view
- `StudentAttendanceAdapter.java` – Adapter for student attendance lists

---

### 👨‍🏫 Faculty Module
- `SessionActivity.java` – Start and manage attendance sessions
- `SelectAttendanceActivity.java` – Select class/section/subject
- `TakeAttendanceActivity.java` – Attendance monitoring
- `AttendanceReportActivity.java` – Faculty attendance reports
- `AttendanceListAdapter.java` – Attendance list adapter
- `AttendanceReportAdapter.java` – Report adapter

---

### 📷 Camera & Face Recognition
- `CameraActivity.java` – Camera handling and face capture
- `FaceRecognitionUtils.java` – Face recognition helper utilities  
- `CloudFaceRecognitionService.java` – *(Not used; retained only for reference)*

---

### ⚙️ Utilities & Helpers
- `DatabaseHelper.java` – Firebase database operations
- `DeviceUtils.java` – Device identification utilities
- `NetworkUtils.java` – Network and Wi-Fi validation
- `PreferenceManager.java` – Local data storage
- `ValidationUtils.java` – Input validation helpers
- `Constants.java` – Application-wide constants
- `SessionAutoCloseService.java` – Auto-closes attendance sessions

---

## 🎨 Resources (`res/`)

### 📐 Layouts
Contains XML layouts for all activities, dialogs, list items, and UI components, including:
- Login screens
- Dashboards
- Attendance screens
- Camera and face registration screens

### 🖼️ Drawables & Images
Includes:
- Icons
- Backgrounds
- Buttons
- Illustrations used across the app

### 🎨 Values
- `colors.xml` – App color palette
- `styles.xml` – UI styles
- `themes.xml` – Light/Dark themes
- `strings.xml` – All UI strings

---

## 🧪 Testing
- `test/` – Unit tests
- `androidTest/` – Instrumentation tests

---

## 🛠️ Tech Stack

- **Language:** Java
- **Platform:** Android
- **Backend:** Firebase Realtime Database
- **Authentication:** Firebase Authentication
- **IDE:** Android Studio

---

## 🎓 Purpose

This project is developed for **academic use** to demonstrate:
- Android application development
- Firebase integration
- Secure attendance management
- Modular and maintainable app structure

---
## 📸 Output Screenshots

This section shows the output screens of the Smart Attendance System application.

---

### 🔐 Login Screen
<img src="app/screenshots/Screenshot_20251220-115257_Smart%20Attendance%20System.jpg" width="300"/>

---

### 👨‍🎓 Student Dashboard
<img src="app/screenshots/Screenshot_20251220-115315_Smart%20Attendance%20System.jpg" width="300"/>

---

### 📷 Face Registration
<img src="app/screenshots/Screenshot_20251220-115456_Smart%20Attendance%20System.jpg" width="300"/>

<img src="app/screenshots/Screenshot_20251220-115511_Smart%20Attendance%20System.jpg" width="300"/>

---

### ✅ Face Verification
<img src="app/screenshots/Screenshot_20251220-115528_Smart%20Attendance%20System.jpg" width="300"/>

<img src="app/screenshots/Screenshot_20251220-115545_Smart%20Attendance%20System.jpg" width="300"/>

---

### 📝 Attendance Marked Successfully
<img src="app/screenshots/Screenshot_20251220-115609_Smart%20Attendance%20System.jpg" width="300"/>

<img src="app/screenshots/Screenshot_20251220-115631_Smart%20Attendance%20System.jpg" width="300"/>

---

### 👨‍🏫 Faculty Attendance Session
<img src="app/screenshots/Screenshot_20251220-115659_Smart%20Attendance%20System.jpg" width="300"/>

<img src="app/screenshots/Screenshot_20251220-115728_Smart%20Attendance%20System.jpg" width="300"/>

---

### 📊 Attendance Report
<img src="app/screenshots/Screenshot_20251220-115748_Smart%20Attendance%20System.jpg" width="300"/>

<img src="app/screenshots/Screenshot_20251220-115841_Smart%20Attendance%20System.jpg" width="300"/>

---

### ✅ Final Output
<img src="app/screenshots/Screenshot_20251220-120017_Smart%20Attendance%20System.jpg" width="300"/>

## 📜 License
This project is licensed under the MIT License.

## 👤 Author

Vishva K S  
Undergraduate Student  
Smart Attendance System – Academic Project
