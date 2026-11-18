package com.example.smart_attendance_system.ui.admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.adapters.AdminSessionAdapter;
import com.example.smart_attendance_system.utils.ExcelExportUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdminAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvSessions;
    private ImageButton btnBack, btnExportAll;
    private ProgressBar progressBar;
    private Spinner spinnerBranch, spinnerYear, spinnerSection;
    private Button btnSelectDate, btnClearFilters;
    private TextView tvFilterCount;
    
    private DatabaseReference sessionsRef;
    private List<AttendanceSession> sessionList;
    private List<AttendanceSession> filteredSessionList;
    private AdminSessionAdapter adapter;
    
    // Filter states
    private String selectedBranch = "All";
    private String selectedYear = "All";
    private String selectedSection = "All";
    private String selectedDate = null;  // null = All

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance);

        initializeViews();
        setupFilters();
        loadAllSessions();
    }

    private void initializeViews() {
        rvSessions = findViewById(R.id.rv_sessions);
        btnBack = findViewById(R.id.btn_back);
        btnExportAll = findViewById(R.id.btn_export_all);
        progressBar = findViewById(R.id.progressBar);
        spinnerBranch = findViewById(R.id.spinner_branch);
        spinnerYear = findViewById(R.id.spinner_year);
        spinnerSection = findViewById(R.id.spinner_section);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnClearFilters = findViewById(R.id.btn_clear_filters);
        tvFilterCount = findViewById(R.id.tv_filter_count);

        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        sessionList = new ArrayList<>();
        filteredSessionList = new ArrayList<>();
        adapter = new AdminSessionAdapter(this, filteredSessionList);
        rvSessions.setAdapter(adapter);
        
        sessionsRef = FirebaseDatabase.getInstance().getReference(Constants.DB_SESSIONS);

        btnBack.setOnClickListener(v -> finish());
        btnExportAll.setOnClickListener(v -> exportFilteredData());
        btnClearFilters.setOnClickListener(v -> clearFilters());
        btnSelectDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupFilters() {
        // Branch spinner
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,
            new String[]{"All Branches", "CSE", "IT", "ECE", "EEE", "MECH", "CIVIL"});
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBranch.setAdapter(branchAdapter);
        
        // Year spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,
            new String[]{"All Years", "1", "2", "3", "4"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        
        // Section spinner
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,
            new String[]{"All Sections", "A", "B", "C", "D", "E"});
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);
        
        // Spinner listeners
        spinnerBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBranch = position == 0 ? "All" : parent.getItemAtPosition(position).toString();
                filterSessions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = position == 0 ? "All" : parent.getItemAtPosition(position).toString();
                filterSessions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSection = position == 0 ? "All" : parent.getItemAtPosition(position).toString();
                filterSessions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
                selectedDate = dateFormat.format(calendar.getTime());
                btnSelectDate.setText(selectedDate);
                filterSessions();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void clearFilters() {
        selectedBranch = "All";
        selectedYear = "All";
        selectedSection = "All";
        selectedDate = null;
        
        spinnerBranch.setSelection(0);
        spinnerYear.setSelection(0);
        spinnerSection.setSelection(0);
        btnSelectDate.setText("Select Date");
        
        filterSessions();
    }

    private void loadAllSessions() {
        progressBar.setVisibility(View.VISIBLE);
        sessionsRef.orderByChild(Constants.SESSION_DATE)
            .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                sessionList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    AttendanceSession session = data.getValue(AttendanceSession.class);
                    if (session != null) {
                        session.setId(data.getKey());
                        sessionList.add(session);
                    }
                }
                // Firebase orderBy is ascending, so reverse for latest first
                Collections.reverse(sessionList);
                
                filterSessions();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAttendanceActivity.this, "Error loading sessions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterSessions() {
        filteredSessionList.clear();
        
        for (AttendanceSession session : sessionList) {
            boolean matches = true;
            
            // Filter by branch
            if (!selectedBranch.equals("All") && !session.getBranch().equals(selectedBranch)) {
                matches = false;
            }
            
            // Filter by year
            if (!selectedYear.equals("All") && !session.getYear().equals(selectedYear)) {
                matches = false;
            }
            
            // Filter by section
            if (!selectedSection.equals("All") && !session.getSection().equals(selectedSection)) {
                matches = false;
            }
            
            // Filter by date
            if (selectedDate != null && !session.getDate().equals(selectedDate)) {
                matches = false;
            }
            
            if (matches) {
                filteredSessionList.add(session);
            }
        }
        
        adapter.updateList(filteredSessionList);
        updateFilterCount();
    }

    private void updateFilterCount() {
        String countText;
        if (filteredSessionList.size() == sessionList.size()) {
            countText = "Showing all " + sessionList.size() + " sessions";
        } else {
            countText = "Showing " + filteredSessionList.size() + " of " + sessionList.size() + " sessions";
        }
        tvFilterCount.setText(countText);
    }

    private void exportFilteredData() {
        if (filteredSessionList.isEmpty()) {
            Toast.makeText(this, "No sessions to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Exporting " + filteredSessionList.size() + " sessions...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            java.io.File file = ExcelExportUtils.exportMasterSessionList(this, filteredSessionList);
            runOnUiThread(() -> {
                if (file != null) {
                    Toast.makeText(this, "Export Saved: " + file.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
