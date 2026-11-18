package com.example.smart_attendance_system.ui.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.adapters.AdminStudentAdapter;
import com.example.smart_attendance_system.data.Student;
import com.example.smart_attendance_system.utils.Constants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageStudentsActivity extends AppCompatActivity {

    private RecyclerView rvStudents;
    private FloatingActionButton fabAddStudent;
    private ImageButton btnBack;
    private EditText etSearch;
    private ProgressBar progressBar;
    
    private DatabaseReference studentsRef;
    private List<Student> studentList;
    private List<Student> filteredStudentList;
    private AdminStudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        initializeViews();
        loadStudents();
    }

    private void initializeViews() {
        rvStudents = findViewById(R.id.rv_students);
        fabAddStudent = findViewById(R.id.fab_add_student);
        btnBack = findViewById(R.id.btn_back);
        etSearch = findViewById(R.id.et_search_student);
        progressBar = findViewById(R.id.progressBar);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        filteredStudentList = new ArrayList<>();
        
        // Initialize adapter with filtered list
        adapter = new AdminStudentAdapter(this, filteredStudentList);
        rvStudents.setAdapter(adapter);
        
        studentsRef = FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF);

        btnBack.setOnClickListener(v -> finish());
        
        fabAddStudent.setOnClickListener(v -> showAddStudentDialog());
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStudents() {
        progressBar.setVisibility(View.VISIBLE);
        studentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Student student = data.getValue(Student.class);
                    if (student != null) {
                        // Set the enrollment number from the Firebase key
                        student.setRollNo(data.getKey());
                        studentList.add(student);
                    }
                }
                // Apply current filter (or show all if search is empty)
                filterStudents(etSearch.getText().toString());
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageStudentsActivity.this, "Error loading students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterStudents(String query) {
        filteredStudentList.clear();
        
        if (query.isEmpty()) {
            // Show all students
            filteredStudentList.addAll(studentList);
        } else {
            // Filter by name or enrollment number
            String lowerCaseQuery = query.toLowerCase();
            for (Student student : studentList) {
                if (student.getName().toLowerCase().contains(lowerCaseQuery) ||
                    student.getRollNo().toLowerCase().contains(lowerCaseQuery) ||
                    (student.getEmail() != null && student.getEmail().toLowerCase().contains(lowerCaseQuery))) {
                    filteredStudentList.add(student);
                }
            }
        }
        
        adapter.updateList(filteredStudentList);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Student");
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        builder.setView(view);
        
        final EditText etName = view.findViewById(R.id.et_student_name);
        final EditText etEnrollment = view.findViewById(R.id.et_enrollment);
        final EditText etEmail = view.findViewById(R.id.et_email);
        final EditText etBranch = view.findViewById(R.id.et_branch);
        final EditText etYear = view.findViewById(R.id.et_year);
        final EditText etSection = view.findViewById(R.id.et_section);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String enrollment = etEnrollment.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String branch = etBranch.getText().toString().trim().toUpperCase();
            String year = etYear.getText().toString().trim();
            String section = etSection.getText().toString().trim().toUpperCase();
            
            if (name.isEmpty() || enrollment.isEmpty() || email.isEmpty() || branch.isEmpty() || year.isEmpty() || section.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate year (1-4)
            if (!year.matches("[1-4]")) {
                Toast.makeText(this, "Year must be 1, 2, 3, or 4", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate section (A-Z)
            if (!section.matches("[A-Z]")) {
                Toast.makeText(this, "Section must be a single letter (A-Z)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            addNewStudent(enrollment, name, email, branch, year, section);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNewStudent(String enrollment, String name, String email, String branch, String year, String section) {
        // Constructor: (rollNo, name, email, branch, year, section)
        Student newStudent = new Student(enrollment, name, email, branch, year, section);
        studentsRef.child(enrollment).setValue(newStudent)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Student Added Successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            
        // Note: This only creates the DB record. Authentication account must be created separately 
        // or by a separate logic if we want to handle Auth creation.
    }
}
