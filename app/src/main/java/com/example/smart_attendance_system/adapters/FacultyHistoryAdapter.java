package com.example.smart_attendance_system.adapters;

import com.example.smart_attendance_system.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import com.example.smart_attendance_system.data.AttendanceSession;
import com.example.smart_attendance_system.utils.Constants;

public class FacultyHistoryAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> listDates; // Header Titles (Dates)
    private HashMap<String, List<AttendanceSession>> listSessions; // Child Data (Sessions map)

    public FacultyHistoryAdapter(Context context, List<String> listDates, HashMap<String, List<AttendanceSession>> listSessions) {
        this.context = context;
        this.listDates = listDates;
        this.listSessions = listSessions;
    }

    @Override
    public int getGroupCount() {
        return listDates.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return listSessions.get(listDates.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listDates.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listSessions.get(listDates.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String dateTitle = (String) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_history_date_group, null);
        }

        TextView tvDate = convertView.findViewById(R.id.tv_date_group);
        ImageView ivExpand = convertView.findViewById(R.id.iv_expand_arrow);

        tvDate.setText(dateTitle);
        
        // Rotate arrow based on state
        if (isExpanded) {
            ivExpand.setRotation(180);
        } else {
            ivExpand.setRotation(0);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        AttendanceSession session = (AttendanceSession) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_history_session_child, null);
        }

        TextView tvSubject = convertView.findViewById(R.id.tv_subject_name);
        TextView tvDetails = convertView.findViewById(R.id.tv_class_details);
        TextView tvTime = convertView.findViewById(R.id.tv_time_range);
        TextView tvStatus = convertView.findViewById(R.id.tv_session_status);

        tvSubject.setText(session.getSubject());
        tvDetails.setText(session.getBranch() + " " + session.getYear() + " - " + session.getSection());
        tvTime.setText("🕒 " + session.getStartTime() + " - " + session.getEndTime());
        
        if (Constants.SESSION_ACTIVE.equals(session.getStatus())) {
            tvStatus.setText("ACTIVE");
            // Set tint/color via code if needed, but default layout handles basic style
        } else {
            tvStatus.setText("ENDED");
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
