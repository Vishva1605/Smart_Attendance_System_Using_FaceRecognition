package com.example.smart_attendance_system.ui.faculty;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.ui.auth.ResetPasswordActivity;
import com.example.smart_attendance_system.ui.auth.LoginActivity;
import com.example.smart_attendance_system.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

public class TakeAttendanceActivity extends AppCompatActivity {

    private Spinner spinner_branch, spinner_section, spinner_year, spinner_subject, spinner_duration;
    private Button btn_start_session, btn_view_reports, btn_logout;
    private Button btn_reset_password;
    private TextView tv_welcome_faculty, tv_instructions;
    private String[] branchArray, yearArray, sectionArray, subjectArray, durationArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);

        initializeViews();
        setupSpinners();
        setupClickListeners();
        displayWelcomeMessage();
    }

    private void initializeViews() {
        spinner_branch = findViewById(R.id.spinner_branch);
        spinner_section = findViewById(R.id.spinner_section);
        spinner_year = findViewById(R.id.spinner_year);
        spinner_subject = findViewById(R.id.spinner_subject);
        spinner_duration = findViewById(R.id.spinner_duration);

        btn_start_session = findViewById(R.id.btn_start_session);
        btn_view_reports = findViewById(R.id.btn_view_reports);
        btn_logout = findViewById(R.id.btn_logout);
        btn_reset_password = findViewById(R.id.btn_reset_password);

        tv_welcome_faculty = findViewById(R.id.tv_welcome_faculty);
        tv_instructions = findViewById(R.id.tv_instructions);
    }

    private void displayWelcomeMessage() {
        String facultyEmail = PreferenceManager.getFacultyEmail(this);
        if (facultyEmail != null) {
            tv_welcome_faculty.setText("Welcome, " + facultyEmail.split("@")[0]);
        } else {
            tv_welcome_faculty.setText("Welcome, Faculty");
        }

        tv_instructions.setText("Select class details below to start an attendance session.\n" +
                "Students will be able to mark their attendance once the session is active.");
    }

    private void setupSpinners() {
        // Branch Spinner
        branchArray = new String[]{
                "Select Branch",
                "CSE", "ECE", "EEE", "IT", "MECH", "CIVIL"
        };
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, branchArray);
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_branch.setAdapter(branchAdapter);

        // Year Spinner
        yearArray = new String[]{
                "Select Year", "1", "2", "3", "4"
        };
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, yearArray);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_year.setAdapter(yearAdapter);

        // Section Spinner
        sectionArray = new String[]{
                "Select Section", "A", "B", "C", "D", "E"
        };
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sectionArray);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_section.setAdapter(sectionAdapter);

        // Subject Spinner
        subjectArray = new String[]{
                "Select Subject",
                "Data Structures", "Database Management Systems", "Operating Systems",
                "Computer Networks", "Software Engineering", "Web Development",
                "Mobile Application Development", "Machine Learning",
                "Artificial Intelligence", "Mathematics", "Physics",
                "Digital Electronics", "Microprocessors", "Computer Graphics",
                "Compiler Design", "Theory of Computation", "Algorithms"
        };
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subjectArray);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_subject.setAdapter(subjectAdapter);

        // Duration Spinner
        durationArray = new String[]{
                "Auto-Close: 30 Mins (Default)",
                "15 Minutes",
                "45 Minutes",
                "1 Hour",
                "2 Hours",
                "Never"
        };
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, durationArray);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_duration.setAdapter(durationAdapter);
    }

    private void setupClickListeners() {
        btn_start_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAttendanceSession();
            }
        });

        btn_view_reports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewAttendanceReports();
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutConfirmation();
            }
        });
        
        btn_reset_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TakeAttendanceActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startAttendanceSession() {
        if (!isSelectionValid()) {
            Toast.makeText(this, "Please select all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedBranch = spinner_branch.getSelectedItem().toString();
        String selectedYear = spinner_year.getSelectedItem().toString();
        String selectedSection = spinner_section.getSelectedItem().toString();
        String selectedSubject = spinner_subject.getSelectedItem().toString();
        String selectedDuration = spinner_duration.getSelectedItem().toString();

        long durationMillis = 30 * 60 * 1000; // Default 30 mins
        switch (selectedDuration) {
            case "15 Minutes": durationMillis = 15 * 60 * 1000; break;
            case "45 Minutes": durationMillis = 45 * 60 * 1000; break;
            case "1 Hour": durationMillis = 60 * 60 * 1000; break;
            case "2 Hours": durationMillis = 2 * 60 * 60 * 1000; break;
            case "Never": durationMillis = -1; break; // -1 for no auto-close
        }
        
        final long finalDuration = durationMillis;

        // Show confirmation dialog
        showSessionConfirmation(selectedBranch, selectedYear, selectedSection, selectedSubject, finalDuration);
    }

    private void showSessionConfirmation(String branch, String year, String section, String subject, long duration) {
        String message = "Start attendance session for:\n\n" +
                "Branch: " + branch + "\n" +
                "Year: " + year + "\n" +
                "Section: " + section + "\n" +
                "Subject: " + subject + "\n\n" +
                "Are you sure you want to continue?";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Session")
                .setMessage(message)
                .setPositiveButton("Start Session", (dialog, which) -> {
                    Intent intent = new Intent(this, SessionActivity.class);
                    intent.putExtra(Constants.EXTRA_BRANCH, branch);
                    intent.putExtra(Constants.EXTRA_YEAR, year);
                    intent.putExtra(Constants.EXTRA_SECTION, section);
                    intent.putExtra(Constants.EXTRA_SUBJECT, subject);
                    intent.putExtra(Constants.EXTRA_AUTO_CLOSE_DURATION, duration);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void viewAttendanceReports() {
        Intent intent = new Intent(this, FacultyHistoryActivity.class);
        startActivity(intent);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        PreferenceManager.clearFacultyInfo(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isSelectionValid() {
        return spinner_branch.getSelectedItemPosition() > 0 &&
                spinner_year.getSelectedItemPosition() > 0 &&
                spinner_section.getSelectedItemPosition() > 0 &&
                spinner_subject.getSelectedItemPosition() > 0;
    }

    @Override
    public void onBackPressed() {
        showLogoutConfirmation();
    }
}



















