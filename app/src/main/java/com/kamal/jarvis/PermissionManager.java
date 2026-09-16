package com.kamal.jarvis;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

public class PermissionManager {

    private final Context context;

    public PermissionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasMicrophonePermission() {
        return context.checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasInternetPermission() {
        return context.checkSelfPermission(
                Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public String getPermissionStatus() {

        StringBuilder result = new StringBuilder();

        result.append("JARVIS PERMISSION SYSTEM\n");
        result.append("============================\n\n");

        result.append("Microphone: ");

        result.append(
                hasMicrophonePermission()
                        ? "GRANTED ✓"
                        : "NOT GRANTED ⚠"
        );

        result.append("\n");

        result.append("Internet: ");

        result.append(
                hasInternetPermission()
                        ? "AVAILABLE ✓"
                        : "NOT AVAILABLE ⚠"
        );

        result.append("\n");

        return result.toString();
    }

    public boolean isHealthy() {
        return hasInternetPermission();
    }

    public String getStatus() {

        if (isHealthy()) {
            return "Permission Manager: ONLINE ✓";
        }

        return "Permission Manager: NEEDS ATTENTION ⚠";
    }
}