package com.example.smart_attendance_system.adapters;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.AttendanceRecord;
import com.example.smart_attendance_system.utils.Constants;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Enhanced Adapter for displaying attendance records in faculty reports
 * Supports sorting, filtering, and proper status visualization with Manual Override.
 * Refactored to extend RecyclerView.Adapter.
 */
public class AttendanceReportAdapter extends RecyclerView.Adapter<AttendanceReportAdapter.ViewHolder> {

    private static final String TAG = "AttendanceReportAdapter";

    private Context context;
    private List<AttendanceRecord> attendanceRecords;
    private List<AttendanceRecord> filteredRecords;
    private String filterStatus = "ALL"; // ALL, PRESENT, ABSENT
    
    // Manual Override
    private OnManualOverrideListener listener;
    private boolean canManualOverride = false;

    public interface OnManualOverrideListener {
        void onManualOverride(String enrollment, String studentName, String studentEmail);
    }

    public AttendanceReportAdapter(Context context, List<AttendanceRecord> attendanceRecords) {
        this.context = context;
        this.attendanceRecords = attendanceRecords;
        this.filteredRecords = new ArrayList<>(attendanceRecords);
    }
    
    public void setOnManualOverrideListener(OnManualOverrideListener listener) {
        this.listener = listener;
    }

    public void setCanManualOverride(boolean canManualOverride) {
        this.canManualOverride = canManualOverride;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = filteredRecords.get(position);

        // Set student information
        if (holder.tv_student_name != null) holder.tv_student_name.setText(record.getStudentName());
        if (holder.tv_enrollment_no != null) holder.tv_enrollment_no.setText(record.getEnrollmentNo());
        if (holder.tv_student_email != null) holder.tv_student_email.setText(record.getEmail()); // Bind Email
        if (holder.tv_marked_time != null) holder.tv_marked_time.setText(record.getMarkedTime());

        // Set status with appropriate styling
        if (Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
            if (holder.tv_status != null) {
                holder.tv_status.setText("✅ Present");
                holder.tv_status.setTextColor(Color.parseColor("#1B5E20")); // Darker Green for better visibility
            }
            holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E8")); // Light green background
            
            // Check for manual override flag
             if (record.isManualOverride() && holder.tv_status != null) {
                String statusText = "✋ Marked by Faculty";
                if (record.getRemarks() != null && !record.getRemarks().isEmpty()) {
                    statusText += "\n(" + record.getRemarks() + ")";
                }
                holder.tv_status.setText(statusText);
            }
            
        } else {
            if (holder.tv_status != null) {
                holder.tv_status.setText("❌ Absent");
                holder.tv_status.setTextColor(Color.parseColor("#B71C1C")); // Darker Red for better visibility
            }
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background
            
            if (canManualOverride) {
                holder.itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onManualOverride(record.getEnrollmentNo(), record.getStudentName(), record.getEmail());
                    }
                    return true; // Consume the long click
                });
                // Remove simple click listener to avoid conflict/confusion
                holder.itemView.setOnClickListener(null);
            } else {
                holder.itemView.setOnLongClickListener(null);
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return filteredRecords != null ? filteredRecords.size() : 0;
    }

    public AttendanceRecord getItem(int position) {
        return filteredRecords.get(position);
    }

    /**
     * Filter records by attendance status
     */
    public void filterByStatus(String status) {
        this.filterStatus = status;
        applyFilter();
    }

    /**
     * Search records by student name or enrollment number
     */
    public void searchRecords(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredRecords = new ArrayList<>(attendanceRecords);
        } else {
            filteredRecords = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();

            for (AttendanceRecord record : attendanceRecords) {
                if ((record.getStudentName() != null && record.getStudentName().toLowerCase().contains(lowerQuery)) ||
                    (record.getEnrollmentNo() != null && record.getEnrollmentNo().toLowerCase().contains(lowerQuery)) ||
                    (record.getEmail() != null && record.getEmail().toLowerCase().contains(lowerQuery))) {
                    filteredRecords.add(record);
                }
            }
        }

        applyFilter(); // Re-apply status filter on search results
        notifyDataSetChanged();
    }

    /**
     * Apply status filter to current records
     */
    private void applyFilter() {
        if ("ALL".equals(filterStatus)) {
            // No status filter, keep search results as is
            notifyDataSetChanged();
            return;
        }

        List<AttendanceRecord> tempList = new ArrayList<>();
        for (AttendanceRecord record : filteredRecords) {
             if (record.getStatus() != null && filterStatus.equalsIgnoreCase(record.getStatus())) {
                tempList.add(record);
            }
        }
        filteredRecords = tempList;
        notifyDataSetChanged();
    }

    /**
     * Sort records by different criteria
     */
    public void sortRecords(SortCriteria criteria) {
        if (filteredRecords == null || filteredRecords.isEmpty()) {
            return;
        }

        Collections.sort(filteredRecords, (r1, r2) -> {
            switch (criteria) {
                case NAME_ASC:
                    return compareStrings(r1.getStudentName(), r2.getStudentName());
                case NAME_DESC:
                    return compareStrings(r2.getStudentName(), r1.getStudentName());
                case ENROLLMENT_ASC:
                    return compareStrings(r1.getEnrollmentNo(), r2.getEnrollmentNo());
                case ENROLLMENT_DESC:
                    return compareStrings(r2.getEnrollmentNo(), r1.getEnrollmentNo());
                case STATUS_PRESENT_FIRST:
                    return compareStatus(r1, r2, Constants.ATTENDANCE_PRESENT);
                case STATUS_ABSENT_FIRST:
                    return compareStatus(r1, r2, Constants.ATTENDANCE_ABSENT);
                default:
                    return 0;
            }
        });

        notifyDataSetChanged();
    }
    
    private int compareStrings(String s1, String s2) {
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareToIgnoreCase(s2);
    }
    
    private int compareStatus(AttendanceRecord r1, AttendanceRecord r2, String targetStatus) {
        boolean r1Target = targetStatus.equals(r1.getStatus());
        boolean r2Target = targetStatus.equals(r2.getStatus());
        
        if (r1Target && !r2Target) return -1;
        if (!r1Target && r2Target) return 1;
        return compareStrings(r1.getStudentName(), r2.getStudentName());
    }

    /**
     * Get attendance statistics
     */
    public AttendanceStats getAttendanceStats() {
        if (attendanceRecords == null) return new AttendanceStats(0,0,0,0);
        
        int total = attendanceRecords.size();
        int present = 0;
        int absent = 0;

        for (AttendanceRecord record : attendanceRecords) {
            if (Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
                present++;
            } else {
                absent++;
            }
        }

        double percentage = total > 0 ? (present * 100.0 / total) : 0.0;
        return new AttendanceStats(total, present, absent, percentage);
    }

    /**
     * Update the entire dataset
     */
    public void updateRecords(List<AttendanceRecord> newRecords) {
        this.attendanceRecords = new ArrayList<>(newRecords);
        this.filteredRecords = new ArrayList<>(newRecords);
        this.filterStatus = "ALL"; 
        notifyDataSetChanged();
    }

    /**
     * Clear all filters and show all records
     */
    public void clearFilters() {
        this.filterStatus = "ALL";
        this.filteredRecords = new ArrayList<>(this.attendanceRecords);
        notifyDataSetChanged();
    }

    // ViewHolder pattern
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_student_name;
        TextView tv_enrollment_no;
        TextView tv_student_email; // Added Email TextView
        TextView tv_status;
        TextView tv_marked_time;

        ViewHolder(View itemView) {
            super(itemView);
            tv_student_name = itemView.findViewById(R.id.tv_student_name);
            tv_enrollment_no = itemView.findViewById(R.id.tv_enrollment_no);
            tv_student_email = itemView.findViewById(R.id.tv_student_email); // Find Email TextView
            tv_status = itemView.findViewById(R.id.tv_status);
            tv_marked_time = itemView.findViewById(R.id.tv_marked_time);
        }
    }

    // Enum for sorting criteria
    public enum SortCriteria {
        NAME_ASC,
        NAME_DESC,
        ENROLLMENT_ASC,
        ENROLLMENT_DESC,
        STATUS_PRESENT_FIRST,
        STATUS_ABSENT_FIRST
    }

    // Data class for attendance statistics
    public static class AttendanceStats {
        private int total;
        private int present;
        private int absent;
        private double percentage;

        public AttendanceStats(int total, int present, int absent, double percentage) {
            this.total = total;
            this.present = present;
            this.absent = absent;
            this.percentage = percentage;
        }

        // Getters
        public int getTotal() { return total; }
        public int getPresent() { return present; }
        public int getAbsent() { return absent; }
        public double getPercentage() { return percentage; }
    }
}
