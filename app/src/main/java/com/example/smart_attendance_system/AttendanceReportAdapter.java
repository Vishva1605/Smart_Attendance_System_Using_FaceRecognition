package com.example.smart_attendance_system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.graphics.Color;

import java.util.List;

/**
 * Enhanced Adapter for displaying attendance records in faculty reports
 * Supports sorting, filtering, and proper status visualization
 */
public class AttendanceReportAdapter extends BaseAdapter {

    private static final String TAG = "AttendanceReportAdapter";

    private Context context;
    private List<AttendanceRecord> attendanceRecords;
    private List<AttendanceRecord> filteredRecords;
    private LayoutInflater inflater;
    private String filterStatus = "ALL"; // ALL, PRESENT, ABSENT

    public AttendanceReportAdapter(Context context, List<AttendanceRecord> attendanceRecords) {
        this.context = context;
        this.attendanceRecords = attendanceRecords;
        this.filteredRecords = attendanceRecords;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredRecords != null ? filteredRecords.size() : 0;
    }

    @Override
    public AttendanceRecord getItem(int position) {
        return filteredRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_attendance_record, parent, false);
            holder = new ViewHolder();
            holder.tv_student_name = convertView.findViewById(R.id.tv_student_name);
            holder.tv_enrollment_no = convertView.findViewById(R.id.tv_enrollment_no);
            holder.tv_student_email = convertView.findViewById(R.id.tv_student_email);
            holder.tv_status = convertView.findViewById(R.id.tv_status);
            holder.tv_marked_time = convertView.findViewById(R.id.tv_marked_time);
            holder.tv_serial_number = convertView.findViewById(R.id.tv_serial_number);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AttendanceRecord record = filteredRecords.get(position);

        // Set serial number
        holder.tv_serial_number.setText(String.valueOf(position + 1));

        // Set student information
        holder.tv_student_name.setText(record.getStudentName());
        holder.tv_enrollment_no.setText(record.getEnrollmentNo());
        holder.tv_student_email.setText(record.getEmail());
        holder.tv_marked_time.setText(record.getMarkedTime());

        // Set status with appropriate styling
        if (Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
            holder.tv_status.setText("✅ Present");
            holder.tv_status.setTextColor(Color.parseColor("#4CAF50")); // Green
            convertView.setBackgroundColor(Color.parseColor("#E8F5E8")); // Light green background
        } else {
            holder.tv_status.setText("❌ Absent");
            holder.tv_status.setTextColor(Color.parseColor("#F44336")); // Red
            convertView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background
        }

        return convertView;
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
            filteredRecords = attendanceRecords;
        } else {
            filteredRecords = new java.util.ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();

            for (AttendanceRecord record : attendanceRecords) {
                if (record.getStudentName().toLowerCase().contains(lowerQuery) ||
                        record.getEnrollmentNo().toLowerCase().contains(lowerQuery) ||
                        record.getEmail().toLowerCase().contains(lowerQuery)) {
                    filteredRecords.add(record);
                }
            }
        }

        applyFilter();
        notifyDataSetChanged();
    }

    /**
     * Apply status filter to current records
     */
    private void applyFilter() {
        if ("ALL".equals(filterStatus)) {
            // filteredRecords already set by search
            return;
        }

        List<AttendanceRecord> statusFiltered = new java.util.ArrayList<>();
        for (AttendanceRecord record : filteredRecords) {
            if (filterStatus.equals(record.getStatus().toUpperCase())) {
                statusFiltered.add(record);
            }
        }
        filteredRecords = statusFiltered;
    }

    /**
     * Sort records by different criteria
     */
    public void sortRecords(SortCriteria criteria) {
        if (filteredRecords == null || filteredRecords.isEmpty()) {
            return;
        }

        switch (criteria) {
            case NAME_ASC:
                filteredRecords.sort((r1, r2) ->
                        r1.getStudentName().compareToIgnoreCase(r2.getStudentName()));
                break;
            case NAME_DESC:
                filteredRecords.sort((r1, r2) ->
                        r2.getStudentName().compareToIgnoreCase(r1.getStudentName()));
                break;
            case ENROLLMENT_ASC:
                filteredRecords.sort((r1, r2) ->
                        r1.getEnrollmentNo().compareToIgnoreCase(r2.getEnrollmentNo()));
                break;
            case ENROLLMENT_DESC:
                filteredRecords.sort((r1, r2) ->
                        r2.getEnrollmentNo().compareToIgnoreCase(r1.getEnrollmentNo()));
                break;
            case STATUS_PRESENT_FIRST:
                filteredRecords.sort((r1, r2) -> {
                    if (r1.getStatus().equals(r2.getStatus())) {
                        return r1.getStudentName().compareToIgnoreCase(r2.getStudentName());
                    }
                    return Constants.ATTENDANCE_PRESENT.equals(r1.getStatus()) ? -1 : 1;
                });
                break;
            case STATUS_ABSENT_FIRST:
                filteredRecords.sort((r1, r2) -> {
                    if (r1.getStatus().equals(r2.getStatus())) {
                        return r1.getStudentName().compareToIgnoreCase(r2.getStudentName());
                    }
                    return Constants.ATTENDANCE_ABSENT.equals(r1.getStatus()) ? -1 : 1;
                });
                break;
        }

        notifyDataSetChanged();
    }

    /**
     * Get attendance statistics
     */
    public AttendanceStats getAttendanceStats() {
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
        this.attendanceRecords = newRecords;
        this.filteredRecords = newRecords;
        notifyDataSetChanged();
    }

    /**
     * Clear all filters and show all records
     */
    public void clearFilters() {
        this.filterStatus = "ALL";
        this.filteredRecords = this.attendanceRecords;
        notifyDataSetChanged();
    }

    // ViewHolder pattern for better performance
    private static class ViewHolder {
        TextView tv_serial_number;
        TextView tv_student_name;
        TextView tv_enrollment_no;
        TextView tv_student_email;
        TextView tv_status;
        TextView tv_marked_time;
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

        @Override
        public String toString() {
            return String.format("Total: %d, Present: %d (%.1f%%), Absent: %d",
                    total, present, percentage, absent);
        }
    }

    // Data class for attendance record
    public static class AttendanceRecord {
        private String enrollmentNo;
        private String studentName;
        private String email;
        private String status;
        private String markedTime;
        private String branch;
        private String year;
        private String section;

        public AttendanceRecord(String enrollmentNo, String studentName, String email,
                                String status, String markedTime) {
            this.enrollmentNo = enrollmentNo;
            this.studentName = studentName;
            this.email = email;
            this.status = status;
            this.markedTime = markedTime;
        }

        public AttendanceRecord(String enrollmentNo, String studentName, String email,
                                String status, String markedTime, String branch,
                                String year, String section) {
            this(enrollmentNo, studentName, email, status, markedTime);
            this.branch = branch;
            this.year = year;
            this.section = section;
        }

        // Getters
        public String getEnrollmentNo() { return enrollmentNo; }
        public String getStudentName() { return studentName; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getMarkedTime() { return markedTime; }
        public String getBranch() { return branch; }
        public String getYear() { return year; }
        public String getSection() { return section; }

        // Setters
        public void setStatus(String status) { this.status = status; }
        public void setMarkedTime(String markedTime) { this.markedTime = markedTime; }

        @Override
        public String toString() {
            return String.format("%s - %s (%s)", enrollmentNo, studentName, status);
        }
    }
}