package com.example.smart_attendance_system.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.Faculty;
import com.example.smart_attendance_system.utils.Constants;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminFacultyAdapter extends RecyclerView.Adapter<AdminFacultyAdapter.FacultyViewHolder> {

    private Context context;
    private List<Faculty> facultyList;

    public AdminFacultyAdapter(Context context, List<Faculty> facultyList) {
        this.context = context;
        this.facultyList = facultyList;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_faculty, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        Faculty faculty = facultyList.get(position);

        holder.tvName.setText(faculty.getName());
        holder.tvDepartment.setText(faculty.getDepartment());
        holder.tvEmail.setText(faculty.getEmail());
        
        holder.btnDelete.setOnClickListener(v -> deleteFaculty(faculty));
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    private void deleteFaculty(Faculty faculty) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Faculty")
            .setMessage("Are you sure you want to DELETE " + faculty.getName() + "?\n\nThis will remove their access.")
            .setPositiveButton("Delete", (dialog, which) -> {
                 FirebaseDatabase.getInstance().getReference(Constants.FACULTY_REF)
                    .child(faculty.getId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Faculty Deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static class FacultyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDepartment, tvEmail;
        ImageButton btnDelete;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_faculty_name);
            tvDepartment = itemView.findViewById(R.id.tv_department);
            tvEmail = itemView.findViewById(R.id.tv_faculty_email);
            btnDelete = itemView.findViewById(R.id.btn_delete_faculty);
        }
    }
}
