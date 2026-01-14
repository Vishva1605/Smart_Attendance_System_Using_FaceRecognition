package com.example.smart_attendance_system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class AttendanceListAdapter extends BaseAdapter {
    
    private Context context;
    private List<AttendanceReportActivity.AttendanceRecord> attendanceRecords;
    private LayoutInflater inflater;

    public AttendanceListAdapter(Context context, List<AttendanceReportActivity.AttendanceRecord> attendanceRecords) {
        this.context = context;
        this.attendanceRecords = attendanceRecords;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return attendanceRecords.size();
    }

    @Override
    public Object getItem(int position) {
        return attendanceRecords.get(position);
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
            holder.tv_status = convertView.findViewById(R.id.tv_status);
            holder.tv_marked_time = convertView.findViewById(R.id.tv_marked_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AttendanceReportActivity.AttendanceRecord record = attendanceRecords.get(position);
        
        holder.tv_student_name.setText(record.getStudentName());
        holder.tv_enrollment_no.setText(record.getEnrollmentNo());
        holder.tv_marked_time.setText(record.getMarkedTime());
        
        // Set status with appropriate styling
        if (Constants.ATTENDANCE_PRESENT.equals(record.getStatus())) {
            holder.tv_status.setText("✅ Present");
            holder.tv_status.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
        } else {
            holder.tv_status.setText("❌ Absent");
            holder.tv_status.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tv_student_name;
        TextView tv_enrollment_no;
        TextView tv_status;
        TextView tv_marked_time;
    }
}