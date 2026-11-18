package com.example.smart_attendance_system.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.utils.Constants;

import java.util.List;

public class AdminSessionAdapter extends RecyclerView.Adapter<AdminSessionAdapter.SessionViewHolder> {

    private Context context;
    private List<AttendanceSession> sessionList;

    public AdminSessionAdapter(Context context, List<AttendanceSession> sessionList) {
        this.context = context;
        this.sessionList = sessionList;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        AttendanceSession session = sessionList.get(position);

        holder.tvSubject.setText(session.getSubject());
        holder.tvFaculty.setText(session.getFacultyName());
        holder.tvDate.setText("Date: " + session.getDate() + " | " + session.getStartTime());
        holder.tvClassInfo.setText(session.getBranch() + " - " + session.getYear() + " - " + session.getSection());

        if (Constants.SESSION_STATUS_ACTIVE.equals(session.getStatus())) {
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setTextColor(Color.parseColor("#388E3C")); // Green
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
        } else {
             holder.tvStatus.setText(session.getStatus().toUpperCase());
             holder.tvStatus.setTextColor(Color.GRAY);
             holder.tvStatus.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }
    
    /**
     * Update the session list with filtered data
     */
    public void updateList(List<AttendanceSession> newList) {
        this.sessionList.clear();
        this.sessionList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvFaculty, tvDate, tvClassInfo, tvStatus;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvFaculty = itemView.findViewById(R.id.tv_faculty_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvClassInfo = itemView.findViewById(R.id.tv_class_info);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
