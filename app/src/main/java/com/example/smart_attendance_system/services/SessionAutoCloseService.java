package com.example.smart_attendance_system.services;

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

import com.example.smart_attendance_system.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class SessionAutoCloseService extends Service {

    private static final String TAG = "SessionAutoClose";
    private long autoCloseDelay = 30 * 60 * 1000; // Default 30 mins
    private long targetEndTime;

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
                autoCloseDelay = intent.getLongExtra(Constants.EXTRA_AUTO_CLOSE_DURATION, 30 * 60 * 1000);
                long sessionStartTime = intent.getLongExtra(Constants.SESSION_CREATED_AT, System.currentTimeMillis());
                targetEndTime = sessionStartTime + autoCloseDelay;

                Log.d(TAG, "Starting auto-close timer for session: " + sessionId + " Duration: " + autoCloseDelay + " Target: " + new java.util.Date(targetEndTime));
                
                startForegroundService(); // Start as foreground immediately
                startAutoCloseTimer();
            } else {
                Log.e(TAG, "Session ID is null, stopping service");
                stopSelf();
            }
        }
        
        return START_NOT_STICKY; 
    }

    private void startForegroundService() {
        String channelId = "session_auto_close_channel";
        String channelName = "Session Auto-Close";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_LOW
            );
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Attendance Session Active")
                .setContentText("Session will auto-close in " + (autoCloseDelay / 60000) + " minutes")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon if available
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void startAutoCloseTimer() {
        // Cancel any existing timer
        if (autoCloseRunnable != null) {
            handler.removeCallbacks(autoCloseRunnable);
        }
        
        Log.d(TAG, "Auto-close scheduled for: " + new java.util.Date(targetEndTime));

        autoCloseRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long timeRemaining = targetEndTime - currentTime;
                
                if (timeRemaining <= 0) {
                    Log.d(TAG, "Time expired! Auto-closing session: " + sessionId);
                    autoCloseSession();
                } else {
                    // Update notification text (optional but good for UX)
                    updateNotification((int)(timeRemaining / 60000));
                    
                    // Check again in 1 minute (or less if close)
                    long nextCheck = Math.min(60000, timeRemaining); 
                    handler.postDelayed(this, nextCheck);
                }
            }
        };

        // Start immediately
        if (autoCloseDelay > 0) {
            handler.post(autoCloseRunnable);
        } else {
             Log.d(TAG, "Auto-close disabled (Duration <= 0)");
        }
    }

    private void updateNotification(int minutesRemaining) {
        String channelId = "session_auto_close_channel";
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Attendance Session Active")
                .setContentText("Closing in " + (minutesRemaining + 1) + " mins") // +1 to round up
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setOnlyAlertOnce(true) // Don't buzz every update
                .build();

        android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
        if (manager != null) {
            manager.notify(1, notification);
        }
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
        updates.put("auto_close_reason", "Session automatically closed after " + (autoCloseDelay / 60000) + " minutes");

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