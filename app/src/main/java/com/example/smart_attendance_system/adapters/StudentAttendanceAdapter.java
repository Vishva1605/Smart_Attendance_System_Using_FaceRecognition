package com.example.smart_attendance_system.adapters;

import com.example.smart_attendance_system.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import com.example.smart_attendance_system.data.AttendanceRecord;
import com.example.smart_attendance_system.utils.Constants;

public class StudentAttendanceAdapter extends BaseAdapter {
    
    private Context context;
    private List<AttendanceRecord> attendanceRecords;
    private LayoutInflater inflater;

    public StudentAttendanceAdapter(Context context, List<AttendanceRecord> attendanceRecords) {
        this.context = context;
        this.attendanceRecords = attendanceRecords;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return attendanceRecords.size();
    }

    @Override
    public AttendanceRecord getItem(int position) {
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

        AttendanceRecord record = attendanceRecords.get(position);
        
        // Use fields from the unified AttendanceRecord model.
        // Identify which fields map to what. 
        // In Student View, we often show Session Subject and Date.
        // Our unified model has these fields (added in constructor for student view compatibility)
        
        // PROCEEDING WITH assumption that I will update AttendanceRecord first/next.
        // But to avoid compilation error in this step, I will use placeholders or getters I intend to add.
        
        // Actually, looking at my previous view_file of AttendanceRecord.java (Step 43), it DOES NOT have subject/date.
        // I must update AttendanceRecord.java FIRST before finishing this adapter properly.
        // However, I am in replace_file_content for Adapter. 
        // I will write this code assuming the getters exist, and then immediately update AttendanceRecord.java.
        // Or better yet, I will cancel this tool call? No, I can't. 
        
        // I will use "getStudentName" as placeholder? No that's bad.
        // I will use "Branch" for Subject? No.
        
        // I will add the code assuming getSubject() and getDate() exist. 
        // Then I will immediately add them to the model.
        
        holder.tv_subject.setText(record.getSubject());
        holder.tv_date.setText(record.getDate());
        holder.tv_time.setText(record.getMarkedTime()); // Assuming markedTime is the time field in AttendanceRecord
        
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
    
    // RETHINKING: writing invalid code is bad.
    // I should update AttendanceRecord.java FIRST.
    // But I'm already inside the tool call for Adapter.
    // I will write the adapter to safely CAST or check, or just implement it and fix the model in the next step.
    // Java is strict. The code won't compile if methods don't exist.
    // But since I'm not running a compiler, I can do it in order. 
    
    // I shall update AttendanceRecord.java in the NEXT step.
    // Here is the adapter code assuming the model IS updated.

    private static class ViewHolder {
        TextView tv_subject;
        TextView tv_date;
        TextView tv_time;
        TextView tv_status;
    }
}
