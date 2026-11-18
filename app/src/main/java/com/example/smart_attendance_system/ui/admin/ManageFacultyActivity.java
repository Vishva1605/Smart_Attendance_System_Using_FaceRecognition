package com.example.smart_attendance_system.ui.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.adapters.AdminFacultyAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageFacultyActivity extends AppCompatActivity {

    private RecyclerView rvFaculty;
    private FloatingActionButton fabAddFaculty;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    
    private DatabaseReference facultyRef;
    private List<Faculty> facultyList;
    private AdminFacultyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_faculty);

        initializeViews();
        loadFaculty();
    }

    private void initializeViews() {
        rvFaculty = findViewById(R.id.rv_faculty);
        fabAddFaculty = findViewById(R.id.fab_add_faculty);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progressBar);

        rvFaculty.setLayoutManager(new LinearLayoutManager(this));
        facultyList = new ArrayList<>();
        adapter = new AdminFacultyAdapter(this, facultyList);
        rvFaculty.setAdapter(adapter);
        
        facultyRef = FirebaseDatabase.getInstance().getReference(Constants.FACULTY_REF);

        btnBack.setOnClickListener(v -> finish());
        fabAddFaculty.setOnClickListener(v -> showAddFacultyDialog());
    }

    private void loadFaculty() {
        progressBar.setVisibility(View.VISIBLE);
        facultyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                facultyList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Faculty faculty = data.getValue(Faculty.class);
                    // Ensure ID is set from key if not in object
                    if (faculty != null) {
                        faculty.setId(data.getKey());
                        facultyList.add(faculty);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageFacultyActivity.this, "Error loading faculty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddFacultyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Faculty");
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_faculty, null);
        builder.setView(view);
        
        final EditText etName = view.findViewById(R.id.et_faculty_name);
        final EditText etEmail = view.findViewById(R.id.et_faculty_email);  
        final EditText etDepartment = view.findViewById(R.id.et_faculty_department);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String department = etDepartment.getText().toString().trim();
            
            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            addNewFaculty(name, email, department);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNewFaculty(String name, String email, String department) {
        String facultyId = facultyRef.push().getKey();
        if (facultyId != null) {
            Faculty newFaculty = new Faculty(facultyId, name, email, department);
            facultyRef.child(facultyId).setValue(newFaculty)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Faculty Added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add faculty", Toast.LENGTH_SHORT).show());
        }
    }
}
