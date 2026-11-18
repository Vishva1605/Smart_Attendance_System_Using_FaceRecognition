package com.example.smart_attendance_system.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.data.Student;
import com.example.smart_attendance_system.utils.Constants;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminStudentAdapter extends RecyclerView.Adapter<AdminStudentAdapter.StudentViewHolder> {

    private Context context;
    private List<Student> studentList;

    public AdminStudentAdapter(Context context, List<Student> studentList) {
        this.context = context;
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);

        holder.tvName.setText(student.getName());
        holder.tvEnrollment.setText(student.getRollNo());
        holder.tvEmail.setText(student.getEmail());
        
        holder.btnResetDevice.setOnClickListener(v -> resetDevice(student));
        holder.btnResetFace.setOnClickListener(v -> resetFace(student));
        holder.btnDelete.setOnClickListener(v -> deleteStudent(student));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }
    
    public void updateList(List<Student> newList) {
        this.studentList = newList;
        notifyDataSetChanged();
    }

    private void resetDevice(Student student) {
        new AlertDialog.Builder(context)
            .setTitle("Reset Device Binding")
            .setMessage("Are you sure you want to reset device binding for " + student.getName() + "?\n\nThey will be able to login from a new device.")
            .setPositiveButton("Reset", (dialog, which) -> {
                FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF)
                    .child(student.getRollNo())
                    .child(Constants.STUDENT_HARDWARE_ID)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Device Reset Successful", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void resetFace(Student student) {
        new AlertDialog.Builder(context)
            .setTitle("Reset Face Data")
            .setMessage("Are you sure you want to delete face data for " + student.getName() + "?\n\nThey will need to register their face again.")
            .setPositiveButton("Reset", (dialog, which) -> {
                 FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF)
                    .child(student.getRollNo())
                    .child(Constants.STUDENT_FACE_DATA)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Face Data Reset Successful", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteStudent(Student student) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to DELETE " + student.getName() + "?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                 FirebaseDatabase.getInstance().getReference(Constants.STUDENTS_REF)
                    .child(student.getRollNo())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Student Deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEnrollment, tvEmail;
        Button btnResetDevice, btnResetFace;
        ImageButton btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvEnrollment = itemView.findViewById(R.id.tv_enrollment_no);
            tvEmail = itemView.findViewById(R.id.tv_student_email);
            btnResetDevice = itemView.findViewById(R.id.btn_reset_device);
            btnResetFace = itemView.findViewById(R.id.btn_reset_face);
            btnDelete = itemView.findViewById(R.id.btn_delete_student);
        }
    }
}
