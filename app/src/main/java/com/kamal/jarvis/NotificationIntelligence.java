package com.kamal.jarvis;

import android.content.Context;

public class NotificationIntelligence {

    private final Context context;

    private boolean enabled = false;
    private String lastNotification = "";

    public NotificationIntelligence(Context context) {

        this.context =
                context.getApplicationContext();
    }

    public String processNotification(
            String appName,
            String title,
            String message
    ) {

        if (title == null ||
                title.trim().isEmpty()) {

            title = "بدون عنوان";
        }

        if (message == null ||
                message.trim().isEmpty()) {

            message = "بدون محتوى";
        }

        if (appName == null ||
                appName.trim().isEmpty()) {

            appName = "تطبيق غير معروف";
        }

        lastNotification =
                appName
                        + " | "
                        + title
                        + " | "
                        + message;

        enabled = true;

        return
                "JARVIS NOTIFICATION INTELLIGENCE\n"
                + "============================\n\n"
                + "التطبيق: "
                + appName
                + "\n"
                + "العنوان: "
                + title
                + "\n"
                + "المحتوى: "
                + message
                + "\n\n"
                + "تم تحليل الإشعار ✓";
    }

    public String getLastNotification() {

        if (lastNotification == null ||
                lastNotification.trim().isEmpty()) {

            return
                    "ما وصل حتى إشعار للتحليل.";
        }

        return
                "آخر إشعار:\n\n"
                + lastNotification;
    }

    public String enable() {

        enabled = true;

        return
                "Notification Intelligence: ENABLED ✓";
    }

    public String disable() {

        enabled = false;

        return
                "Notification Intelligence: DISABLED";
    }

    public boolean isEnabled() {

        return enabled;
    }

    public boolean isHealthy() {

        return context != null;
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Notification Intelligence: ONLINE ✓\n"
                    + "Mode: "
                    + (enabled
                    ? "ACTIVE"
                    : "STANDBY");
        }

        return
                "Notification Intelligence: ERROR ⚠";
    }
}