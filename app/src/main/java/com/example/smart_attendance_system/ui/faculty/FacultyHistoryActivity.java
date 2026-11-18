package com.example.smart_attendance_system.ui.faculty;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.utils.PreferenceManager;
import com.example.smart_attendance_system.utils.Constants;
import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.adapters.FacultyHistoryAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class FacultyHistoryActivity extends AppCompatActivity {

    private TextView tv_faculty_info, tv_status;
    private ExpandableListView elv_history;
    private android.widget.Button btn_back;

    private DatabaseReference sessionsRef;
    private String facultyEmail;
    
    // Data structures for ExpandableList
    private List<String> listDataHeader; // Dates
    private HashMap<String, List<AttendanceSession>> listDataChild; // Date -> Sessions
    private FacultyHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_history);

        initializeViews();
        loadFacultySessions();
    }

    private void initializeViews() {
        tv_faculty_info = findViewById(R.id.tv_faculty_info);
        tv_status = findViewById(R.id.tv_status);
        elv_history = findViewById(R.id.elv_history);
        btn_back = findViewById(R.id.btn_back);

        sessionsRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        adapter = new FacultyHistoryAdapter(this, listDataHeader, listDataChild);
        elv_history.setAdapter(adapter);

        facultyEmail = PreferenceManager.getFacultyEmail(this);
        if (facultyEmail != null) {
            tv_faculty_info.setText("Logged in as: " + facultyEmail);
        }

        btn_back.setOnClickListener(v -> finish());
        
        // Handle child click (Session click)
        elv_history.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            AttendanceSession selectedSession = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
            openSessionReport(selectedSession.getId());
            return true;
        });
    }

    private void loadFacultySessions() {
        tv_status.setVisibility(View.VISIBLE);
        tv_status.setText("Loading past sessions...");

        sessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listDataHeader.clear();
                listDataChild.clear();
                
                // Temporary map to group by date
                HashMap<String, List<AttendanceSession>> groupedSessions = new HashMap<>();

                for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                    try {
                        String sEmail = sessionSnapshot.child(Constants.SESSION_FACULTY_EMAIL).getValue(String.class);
                        String fId = sessionSnapshot.child(Constants.SESSION_FACULTY_ID).getValue(String.class);
                        
                        // Check ownership
                        boolean isMySession = false;
                        if (facultyEmail != null && facultyEmail.equals(sEmail)) isMySession = true;
                        
                        String myFacultyId = PreferenceManager.getFacultyId(FacultyHistoryActivity.this);
                        if (!isMySession && myFacultyId != null && myFacultyId.equals(fId)) {
                            isMySession = true;
                        }
                        
                        if (isMySession) {
                             String subject = sessionSnapshot.child(Constants.SESSION_SUBJECT).getValue(String.class);
                             String branch = sessionSnapshot.child(Constants.SESSION_BRANCH).getValue(String.class);
                             String date = sessionSnapshot.child(Constants.SESSION_DATE).getValue(String.class);
                             String time = sessionSnapshot.child(Constants.SESSION_START_TIME).getValue(String.class);
                             String endTime = sessionSnapshot.child(Constants.SESSION_END_TIME).getValue(String.class);
                             String status = sessionSnapshot.child(Constants.SESSION_STATUS).getValue(String.class);
                             String id = sessionSnapshot.getKey();
                             
                             AttendanceSession session = new AttendanceSession();
                             session.setId(id);
                             session.setSubject(subject);
                             session.setBranch(branch);
                             session.setDate(date);
                             session.setStartTime(time);
                             session.setEndTime(endTime);
                             session.setSection(sessionSnapshot.child(Constants.SESSION_SECTION).getValue(String.class));
                             session.setYear(sessionSnapshot.child(Constants.SESSION_YEAR).getValue(String.class));
                             session.setStatus(status);
                             
                             // Group Strategy: Use "Date" as key
                             if (!groupedSessions.containsKey(date)) {
                                 groupedSessions.put(date, new ArrayList<>());
                             }
                             groupedSessions.get(date).add(session);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (groupedSessions.isEmpty()) {
                    tv_status.setText("No past sessions found.");
                } else {
                    tv_status.setVisibility(View.GONE);
                    
                    // Sort Dates Descending (Recent first)
                    // Format is dd/MM/yyyy. String sort works for yyyy/MM/dd but not dd/MM.
                    // Let's use custom comparator or simple reverse if format is consistent
                    
                    List<String> sortedDates = new ArrayList<>(groupedSessions.keySet());
                    // Simple Sort (Might need robust date parsing if format varies, but it's likely strict)
                    // For dd/MM/yyyy, simple sort does 01/02 before 30/01 which is wrong year wise.
                    // We need a proper date comparator.
                    
                    Collections.sort(sortedDates, (d1, d2) -> {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(Constants.DATE_FORMAT, java.util.Locale.getDefault());
                            java.util.Date date1 = sdf.parse(d1);
                            java.util.Date date2 = sdf.parse(d2);
                            return date2.compareTo(date1); // Descending
                        } catch (Exception e) {
                            return d2.compareTo(d1);
                        }
                    });
                    
                    listDataHeader.addAll(sortedDates);
                    listDataChild.putAll(groupedSessions);
                    
                    // Also sort sessions within each date by time (Descending)
                    for (String date : listDataHeader) {
                        List<AttendanceSession> sessions = listDataChild.get(date);
                         Collections.sort(sessions, (s1, s2) -> {
                            return s2.getStartTime().compareTo(s1.getStartTime());
                        });
                    }

                    adapter.notifyDataSetChanged();
                    
                    // Optional: Expand the first group (Today)
                    if (!listDataHeader.isEmpty()) {
                       elv_history.expandGroup(0);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_status.setText("Error loading data: " + error.getMessage());
            }
        });
    }

    private void openSessionReport(String sessionId) {
        Intent intent = new Intent(this, AttendanceReportActivity.class);
        intent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
        startActivity(intent);
    }
}



















