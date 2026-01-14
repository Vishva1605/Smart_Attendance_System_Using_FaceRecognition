package com.example.smart_attendance_system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class StudentAttendanceAdapter extends BaseAdapter {
    
    private Context context;
    private List<ActivityAttendanceReportStudent.StudentAttendanceRecord> attendanceRecords;
    private LayoutInflater inflater;

    public StudentAttendanceAdapter(Context context, List<ActivityAttendanceReportStudent.StudentAttendanceRecord> attendanceRecords) {
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
            convertView = inflater.inflate(R.layout.item_student_attendance_record, parent, false);
            holder = new ViewHolder();
            holder.tv_subject = convertView.findViewById(R.id.tv_subject);
            holder.tv_date = convertView.findViewById(R.id.tv_date);
            holder.tv_time = convertView.findViewById(R.id.tv_time);
            holder.tv_status = convertView.findViewById(R.id.tv_status);
            convertView.setTag(holder);
      } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ActivityAttendanceReportStudent.StudentAttendanceRecord record = attendanceRecords.get(position);
        
        holder.tv_subject.setText(record.getSubject());
        holder.tv_date.setText(record.getDate());
        holder.tv_time.setText(record.getTime());
        
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
        TextView tv_subject;
        TextView tv_date;
        TextView tv_time;
        TextView tv_status;
    }
}