package com.example.smart_attendance_system;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SessionAutoCloseService extends Service {

    private static final String TAG = "SessionAutoClose";
    private static final long AUTO_CLOSE_DELAY = 30 * 60 * 1000; // 30 minutes in milliseconds

    private Handler handler;
    private Runnable autoCloseRunnable;
    private String sessionId;
    private DatabaseReference attendanceSessionsRef;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        attendanceSessionsRef = FirebaseDatabase.getInstance().getReference(Constants.ATTENDANCE_REPORT_REF);
        Log.d(TAG, "SessionAutoCloseService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            sessionId = intent.getStringExtra(Constants.EXTRA_SESSION_ID);
            
            if (sessionId != null) {
                Log.d(TAG, "Starting auto-close timer for session: " + sessionId);
                startAutoCloseTimer();
            } else {
                Log.e(TAG, "Session ID is null, stopping service");
                stopSelf();
            }
        }
        
        return START_NOT_STICKY; // Don't restart if killed
    }

    private void startAutoCloseTimer() {
        // Cancel any existing timer
        if (autoCloseRunnable != null) {
            handler.removeCallbacks(autoCloseRunnable);
        }

        autoCloseRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Auto-close timer triggered for session: " + sessionId);
                autoCloseSession();
            }
        };

        // Schedule auto-close after 30 minutes
        handler.postDelayed(autoCloseRunnable, AUTO_CLOSE_DELAY);
        Log.d(TAG, "Auto-close timer scheduled for 30 minutes");
    }

    private void autoCloseSession() {
        if (sessionId == null) {
            Log.e(TAG, "Cannot auto-close: session ID is null");
            stopSelf();
            return;
        }

        // First check if session is still active
        attendanceSessionsRef.child(sessionId).child(Constants.SESSION_STATUS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String currentStatus = dataSnapshot.getValue(String.class);
                        
                        if (Constants.SESSION_ACTIVE.equals(currentStatus)) {
                            Log.d(TAG, "Session is still active, proceeding with auto-close");
                            performAutoClose();
                        } else {
                            Log.d(TAG, "Session is no longer active (status: " + currentStatus + "), stopping service");
                            stopSelf();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking session status: " + error.getMessage());
                        stopSelf();
                    }
                });
    }

    private void performAutoClose() {
        // Update session status and add auto-close information
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.SESSION_STATUS, Constants.SESSION_ENDED);
        updates.put("ended_at", System.currentTimeMillis());
        updates.put("auto_closed", true);
        updates.put("auto_close_reason", "Session automatically closed after 30 minutes");

        attendanceSessionsRef.child(sessionId).updateChildren(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Session auto-closed successfully: " + sessionId);
                            
                            // Send broadcast to notify activities
                            Intent broadcastIntent = new Intent(Constants.ACTION_SESSION_AUTO_CLOSED);
                            broadcastIntent.putExtra(Constants.EXTRA_SESSION_ID, sessionId);
                            sendBroadcast(broadcastIntent);
                            
                        } else {
                            Log.e(TAG, "Failed to auto-close session: " + task.getException());
                        }
                        
                        stopSelf();
                    }
                });
    }

    public void cancelAutoClose() {
        Log.d(TAG, "Cancelling auto-close timer for session: " + sessionId);
        if (autoCloseRunnable != null) {
            handler.removeCallbacks(autoCloseRunnable);
            autoCloseRunnable = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (autoCloseRunnable != null) {
            handler.removeCallbacks(autoCloseRunnable);
        }
        Log.d(TAG, "SessionAutoCloseService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }
}