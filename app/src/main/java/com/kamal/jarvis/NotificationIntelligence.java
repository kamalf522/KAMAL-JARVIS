package com.kamal.jarvis;

import android.content.Context;

public class NotificationIntelligence {

    private static NotificationIntelligence activeInstance;

    private static boolean serviceConnected = false;

    private final Context context;

    private boolean enabled = false;

    private String lastNotification = "";

    private String lastPackageName = "";

    private String lastTitle = "";

    private String lastMessage = "";

    public NotificationIntelligence(Context context) {

        if (context != null) {
            this.context =
                    context.getApplicationContext();
        } else {
            this.context = null;
        }

        activeInstance = this;
    }

    public static synchronized void
    setServiceConnected(boolean connected) {

        serviceConnected = connected;

        if (activeInstance != null) {

            activeInstance.enabled =
                    connected;
        }
    }

    public static synchronized boolean
    isServiceConnected() {

        return serviceConnected;
    }

    public static synchronized void
    receiveNotification(
            String packageName,
            String title,
            String message
    ) {

        if (activeInstance == null) {
            return;
        }

        activeInstance.processIncomingNotification(
                packageName,
                title,
                message
        );
    }

    private synchronized void
    processIncomingNotification(
            String packageName,
            String title,
            String message
    ) {

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            packageName =
                    "تطبيق غير معروف";
        }

        if (title == null ||
                title.trim().isEmpty()) {

            title =
                    "بدون عنوان";
        }

        if (message == null ||
                message.trim().isEmpty()) {

            message =
                    "بدون محتوى";
        }

        lastPackageName =
                packageName;

        lastTitle =
                title;

        lastMessage =
                message;

        lastNotification =
                packageName
                        + " | "
                        + title
                        + " | "
                        + message;

        enabled = true;
    }

    public synchronized String
    processNotification(
            String appName,
            String title,
            String message
    ) {

        if (appName == null ||
                appName.trim().isEmpty()) {

            appName =
                    "تطبيق غير معروف";
        }

        if (title == null ||
                title.trim().isEmpty()) {

            title =
                    "بدون عنوان";
        }

        if (message == null ||
                message.trim().isEmpty()) {

            message =
                    "بدون محتوى";
        }

        lastPackageName =
                appName;

        lastTitle =
                title;

        lastMessage =
                message;

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

    public synchronized String
    getLastNotification() {

        if (lastNotification == null ||
                lastNotification.trim().isEmpty()) {

            return
                    "ما وصل حتى إشعار للتحليل.";
        }

        return
                "آخر إشعار:\n\n"
                + lastNotification;
    }

    public synchronized String
    getLastPackageName() {

        return lastPackageName;
    }

    public synchronized String
    getLastTitle() {

        return lastTitle;
    }

    public synchronized String
    getLastMessage() {

        return lastMessage;
    }

    public synchronized String enable() {

        enabled = true;

        return
                "Notification Intelligence: ENABLED ✓";
    }

    public synchronized String disable() {

        enabled = false;

        return
                "Notification Intelligence: DISABLED";
    }

    public synchronized boolean
    isEnabled() {

        return enabled;
    }

    public synchronized boolean
    isHealthy() {

        return context != null;
    }

    public synchronized String
    getStatus() {

        if (!isHealthy()) {

            return
                    "Notification Intelligence: ERROR ⚠";
        }

        return
                "Notification Intelligence: ONLINE ✓\n"
                + "Listener: "
                + (
                    serviceConnected
                    ? "CONNECTED ✓"
                    : "DISCONNECTED"
                )
                + "\n"
                + "Mode: "
                + (
                    enabled
                    ? "ACTIVE"
                    : "STANDBY"
                );
    }
}