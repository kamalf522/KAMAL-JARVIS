package com.kamal.jarvis;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

public class PermissionManager {

    private final Context context;

    public PermissionManager(Context context) {

        if (context != null) {

            this.context =
                    context.getApplicationContext();

        } else {

            this.context = null;
        }
    }

    // =========================================================
    // MICROPHONE
    // =========================================================

    public boolean hasMicrophonePermission() {

        if (context == null) {
            return false;
        }

        return context.checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // =========================================================
    // INTERNET
    // =========================================================

    public boolean hasInternetPermission() {

        /*
         * INTERNET is a normal permission.
         * إذا كانت موجودة فـ Manifest فهي متاحة
         * وما كتحتاجش طلب runtime.
         */
        if (context == null) {
            return false;
        }

        return context.getPackageManager()
                .checkPermission(
                        Manifest.permission.INTERNET,
                        context.getPackageName()
                )
                == PackageManager.PERMISSION_GRANTED;
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    public boolean hasNotificationPermission() {

        if (context == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU) {

            return true;
        }

        return context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // =========================================================
    // ACCESSIBILITY
    // =========================================================

    public boolean hasAccessibilityPermission() {

        try {

            if (context == null) {
                return false;
            }

            String enabledServices =
                    Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    );

            if (enabledServices == null ||
                    enabledServices.trim().isEmpty()) {

                return false;
            }

            String expected =
                    context.getPackageName()
                            + "/"
                            + JarvisAccessibilityService.class
                                    .getName();

            String normalized =
                    enabledServices
                            .toLowerCase();

            return normalized.contains(
                    expected.toLowerCase()
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // NOTIFICATION LISTENER
    // =========================================================

    public boolean hasNotificationListenerPermission() {

        try {

            if (context == null) {
                return false;
            }

            String enabledListeners =
                    Settings.Secure.getString(
                            context.getContentResolver(),
                            "enabled_notification_listeners"
                    );

            if (enabledListeners == null ||
                    enabledListeners.trim().isEmpty()) {

                return false;
            }

            String expected =
                    context.getPackageName()
                            + "/"
                            + JarvisNotificationListenerService.class
                                    .getName();

            return enabledListeners
                    .toLowerCase()
                    .contains(
                            expected.toLowerCase()
                    );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // OPEN ACCESSIBILITY SETTINGS
    // =========================================================

    public String openAccessibilitySettings() {

        if (context == null) {

            return
                    "Permission Manager غير جاهز.";
        }

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return
                    "تم فتح إعدادات Accessibility ✓";

        } catch (Exception e) {

            return
                    "فشل فتح Accessibility: "
                            + safeError(e);
        }
    }

    // =========================================================
    // OPEN NOTIFICATION LISTENER SETTINGS
    // =========================================================

    public String openNotificationListenerSettings() {

        if (context == null) {

            return
                    "Permission Manager غير جاهز.";
        }

        try {

            Intent intent =
                    new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return
                    "تم فتح إعدادات Notification Access ✓";

        } catch (Exception e) {

            return
                    "فشل فتح إعدادات الإشعارات: "
                            + safeError(e);
        }
    }

    // =========================================================
    // NOTIFICATION MANAGER
    // =========================================================

    public boolean areNotificationsEnabled() {

        if (context == null) {
            return false;
        }

        try {

            NotificationManager manager =
                    (NotificationManager)
                            context.getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            if (manager == null) {
                return false;
            }

            return manager.areNotificationsEnabled();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // ALL PERMISSIONS STATUS
    // =========================================================

    public String getPermissionStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS PERMISSION SYSTEM\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "Microphone: "
        );

        result.append(
                hasMicrophonePermission()
                        ? "GRANTED ✓"
                        : "NOT GRANTED ⚠"
        );

        result.append("\n");

        result.append(
                "Internet: "
        );

        result.append(
                hasInternetPermission()
                        ? "AVAILABLE ✓"
                        : "NOT AVAILABLE ⚠"
        );

        result.append("\n");

        result.append(
                "Notifications: "
        );

        result.append(
                hasNotificationPermission()
                        ? "GRANTED ✓"
                        : "NOT GRANTED ⚠"
        );

        result.append("\n");

        result.append(
                "Accessibility: "
        );

        result.append(
                hasAccessibilityPermission()
                        ? "CONNECTED ✓"
                        : "NOT CONNECTED ⚠"
        );

        result.append("\n");

        result.append(
                "Notification Listener: "
        );

        result.append(
                hasNotificationListenerPermission()
                        ? "CONNECTED ✓"
                        : "NOT CONNECTED ⚠"
        );

        result.append("\n");

        result.append(
                "Notifications Enabled: "
        );

        result.append(
                areNotificationsEnabled()
                        ? "YES ✓"
                        : "NO ⚠"
        );

        result.append("\n");

        return result.toString();
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        return context != null
                && hasInternetPermission();
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        if (context == null) {

            return
                    "Permission Manager: OFFLINE ⚠";
        }

        if (isHealthy()) {

            return
                    "Permission Manager: ONLINE ✓\n"
                    + getShortPermissionSummary();
        }

        return
                "Permission Manager: NEEDS ATTENTION ⚠\n"
                + getShortPermissionSummary();
    }

    // =========================================================
    // SHORT SUMMARY
    // =========================================================

    private String getShortPermissionSummary() {

        int granted = 0;
        int total = 4;

        if (hasMicrophonePermission()) {
            granted++;
        }

        if (hasNotificationPermission()) {
            granted++;
        }

        if (hasAccessibilityPermission()) {
            granted++;
        }

        if (hasNotificationListenerPermission()) {
            granted++;
        }

        return
                "Core permissions: "
                        + granted
                        + "/"
                        + total;
    }

    // =========================================================
    // SAFE ERROR
    // =========================================================

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return
                    e.getClass()
                            .getSimpleName();
        }

        return message;
    }
}