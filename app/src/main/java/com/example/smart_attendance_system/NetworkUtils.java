package com.example.smart_attendance_system;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
        }
        return false;
    }

    public static boolean isWifiConnected(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return wifiInfo != null && wifiInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi connection: " + e.getMessage());
        }
        return false;
    }

    public static String getCurrentWifiBSSID(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    return wifiInfo.getBSSID();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while getting WiFi BSSID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi BSSID: " + e.getMessage());
        }
        return null;
    }

    public static String getCurrentWifiSSID(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    // Remove quotes if present
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    return ssid;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while getting WiFi SSID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi SSID: " + e.getMessage());
        }
        return null;
    }

    public static boolean isConnectedToUniversityWifi(Context context) {
        String currentBSSID = getCurrentWifiBSSID(context);
        return currentBSSID != null &&
                Constants.UNIVERSITY_WIFI_BSSID.equalsIgnoreCase(currentBSSID);
    }

    public static WifiConnectionInfo getWifiConnectionInfo(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }

                    return new WifiConnectionInfo(
                            ssid,
                            wifiInfo.getBSSID(),
                            wifiInfo.getRssi(),
                            wifiInfo.getFrequency()
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi connection info: " + e.getMessage());
        }
        return null;
    }

    public static class WifiConnectionInfo {
        private String ssid;
        private String bssid;
        private int rssi;
        private int frequency;

        public WifiConnectionInfo(String ssid, String bssid, int rssi, int frequency) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.rssi = rssi;
            this.frequency = frequency;
        }

        public String getSSID() { return ssid; }
        public String getBSSID() { return bssid; }
        public int getRSSI() { return rssi; }
        public int getFrequency() { return frequency; }

        public boolean isUniversityWifi() {
            return bssid != null && Constants.UNIVERSITY_WIFI_BSSID.equalsIgnoreCase(bssid);
        }

        @Override
        public String toString() {
            return "WifiConnectionInfo{" +
                    "ssid='" + ssid + '\'' +
                    ", bssid='" + bssid + '\'' +
                    ", rssi=" + rssi +
                    ", frequency=" + frequency +
                    ", isUniversityWifi=" + isUniversityWifi() +
                    '}';
        }
    }
}