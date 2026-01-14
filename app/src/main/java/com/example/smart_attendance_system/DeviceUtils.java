package com.example.smart_attendance_system;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.security.MessageDigest;
import java.util.UUID;

public class DeviceUtils {
    
    private static final String TAG = "DeviceUtils";
    
    /**
     * Get unique device identifier
     */
    public static String getDeviceId(Context context) {
        try {
            // Use Android ID as primary identifier
            String androidId = Settings.Secure.getString(context.getContentResolver(), 
                    Settings.Secure.ANDROID_ID);
            
            if (androidId != null && !androidId.equals("9774d56d682e549c")) {
                return hashString(androidId);
            }
            
            // Fallback to random UUID if Android ID is not available
            return generateFallbackId(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID: " + e.getMessage());
            return generateFallbackId(context);
        }
    }
    
    /**
     * Generate fallback device ID using UUID
     */
    private static String generateFallbackId(Context context) {
        // Check if we already have a stored UUID
        String storedUuid = PreferenceManager.getPreferences(context)
                .getString("device_uuid", null);
        
        if (storedUuid == null) {
            storedUuid = UUID.randomUUID().toString();
            PreferenceManager.getPreferences(context).edit()
                    .putString("device_uuid", storedUuid)
                    .apply();
        }
        
        return hashString(storedUuid);
    }
    
    /**
     * Hash string using SHA-256
     */
    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16); // Return first 16 characters
        } catch (Exception e) {
            Log.e(TAG, "Error hashing string: " + e.getMessage());
            return input.substring(0, Math.min(input.length(), 16));
        }
    }
    
    /**
     * Get device model information
     */
    public static String getDeviceModel() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }
    
    /**
     * Get Android version
     */
    public static String getAndroidVersion() {
        return android.os.Build.VERSION.RELEASE;
    }
    
    /**
     * Get device info string
     */
    public static String getDeviceInfo() {
        return getDeviceModel() + " (Android " + getAndroidVersion() + ")";
    }
}