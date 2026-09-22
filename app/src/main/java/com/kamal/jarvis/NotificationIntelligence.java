package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotificationIntelligence {

    private static NotificationIntelligence activeInstance;

    private static boolean serviceConnected = false;

    private final Context context;

    private boolean enabled = false;

    private String lastNotification = "";

    private String lastPackageName = "";

    private String lastTitle = "";

    private String lastMessage = "";

    private String lastCategory = "GENERAL";

    private long lastNotificationTime = 0L;

    private int notificationCount = 0;

    private final List<NotificationRecord> history =
            new ArrayList<>();

    private static final int MAX_HISTORY = 50;

    public NotificationIntelligence(Context context) {

        if (context != null) {
            this.context =
                    context.getApplicationContext();
        } else {
            this.context = null;
        }

        activeInstance = this;

        enabled =
                serviceConnected;
    }

    // =========================================================
    // SERVICE CONNECTION
    // =========================================================

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

    // =========================================================
    // RECEIVE NOTIFICATION
    // =========================================================

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

        packageName =
                clean(
                        packageName,
                        "تطبيق غير معروف"
                );

        title =
                clean(
                        title,
                        "بدون عنوان"
                );

        message =
                clean(
                        message,
                        "بدون محتوى"
                );

        lastPackageName =
                packageName;

        lastTitle =
                title;

        lastMessage =
                message;

        lastCategory =
                classifyNotification(
                        packageName,
                        title,
                        message
                );

        lastNotification =
                packageName
                        + " | "
                        + title
                        + " | "
                        + message;

        lastNotificationTime =
                System.currentTimeMillis();

        notificationCount++;

        addToHistory(
                packageName,
                title,
                message,
                lastCategory,
                lastNotificationTime
        );

        enabled = true;
    }

    // =========================================================
    // MANUAL PROCESSING
    // =========================================================

    public synchronized String
    processNotification(
            String appName,
            String title,
            String message
    ) {

        appName =
                clean(
                        appName,
                        "تطبيق غير معروف"
                );

        title =
                clean(
                        title,
                        "بدون عنوان"
                );

        message =
                clean(
                        message,
                        "بدون محتوى"
                );

        lastPackageName =
                appName;

        lastTitle =
                title;

        lastMessage =
                message;

        lastCategory =
                classifyNotification(
                        appName,
                        title,
                        message
                );

        lastNotification =
                appName
                        + " | "
                        + title
                        + " | "
                        + message;

        lastNotificationTime =
                System.currentTimeMillis();

        notificationCount++;

        addToHistory(
                appName,
                title,
                message,
                lastCategory,
                lastNotificationTime
        );

        enabled = true;

        return buildNotificationReport(
                appName,
                title,
                message,
                lastCategory
        );
    }

    // =========================================================
    // CLASSIFICATION
    // =========================================================

    private String classifyNotification(
            String packageName,
            String title,
            String message
    ) {

        String text =
                normalize(
                        packageName
                                + " "
                                + title
                                + " "
                                + message
                );

        if (containsAny(
                text,
                "رساله",
                "message",
                "messenger",
                "whatsapp",
                "telegram",
                "sms",
                "chat"
        )) {

            return "MESSAGE";
        }

        if (containsAny(
                text,
                "اتصال",
                "مكالمة",
                "call",
                "phone",
                "missed call"
        )) {

            return "CALL";
        }

        if (containsAny(
                text,
                "email",
                "gmail",
                "mail",
                "بريد"
        )) {

            return "EMAIL";
        }

        if (containsAny(
                text,
                "alarm",
                "منبه",
                "reminder",
                "تذكير"
        )) {

            return "REMINDER";
        }

        if (containsAny(
                text,
                "security",
                "امن",
                "security alert",
                "تحذير"
        )) {

            return "SECURITY";
        }

        if (containsAny(
                text,
                "update",
                "تحديث",
                "download",
                "تحميل"
        )) {

            return "SYSTEM";
        }

        if (containsAny(
                text,
                "money",
                "bank",
                "payment",
                "دفع",
                "تحويل",
                "رصيد",
                "transaction"
        )) {

            return "FINANCE";
        }

        if (containsAny(
                text,
                "offer",
                "sale",
                "discount",
                "عرض",
                "تخفيض"
        )) {

            return "PROMOTION";
        }

        return "GENERAL";
    }

    // =========================================================
    // REPORT
    // =========================================================

    private String buildNotificationReport(
            String appName,
            String title,
            String message,
            String category
    ) {

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
                + "\n"
                + "التصنيف: "
                + category
                + "\n"
                + "الوقت: "
                + lastNotificationTime
                + "\n\n"
                + "تم تحليل الإشعار ✓";
    }

    // =========================================================
    // LAST NOTIFICATION
    // =========================================================

    public synchronized String
    getLastNotification() {

        if (lastNotification == null ||
                lastNotification.trim().isEmpty()) {

            return
                    "ما وصل حتى إشعار للتحليل.";
        }

        return
                "آخر إشعار:\n\n"
                + lastNotification
                + "\n"
                + "التصنيف: "
                + lastCategory;
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

    public synchronized String
    getLastCategory() {

        return lastCategory;
    }

    public synchronized long
    getLastNotificationTime() {

        return lastNotificationTime;
    }

    public synchronized int
    getNotificationCount() {

        return notificationCount;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void addToHistory(
            String packageName,
            String title,
            String message,
            String category,
            long timestamp
    ) {

        NotificationRecord record =
                new NotificationRecord(
                        packageName,
                        title,
                        message,
                        category,
                        timestamp
                );

        history.add(record);

        while (history.size() >
                MAX_HISTORY) {

            history.remove(0);
        }
    }

    public synchronized String
    getNotificationHistory() {

        if (history.isEmpty()) {

            return
                    "مازال ما تسجلو حتى إشعار.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS NOTIFICATION HISTORY\n"
        );

        result.append(
                "===========================\n\n"
        );

        int index = 1;

        for (int i =
             history.size() - 1;
             i >= 0;
             i--) {

            NotificationRecord record =
                    history.get(i);

            result.append(
                    index
            );

            result.append(
                    ". ["
            );

            result.append(
                    record.category
            );

            result.append(
                    "] "
            );

            result.append(
                    record.packageName
            );

            result.append(
                    " | "
            );

            result.append(
                    record.title
            );

            result.append(
                    "\n"
            );

            result.append(
                    record.message
            );

            result.append(
                    "\n\n"
            );

            index++;
        }

        return result.toString();
    }

    public synchronized String
    searchNotifications(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return getNotificationHistory();
        }

        String target =
                normalize(query);

        StringBuilder result =
                new StringBuilder();

        int found = 0;

        for (int i =
             history.size() - 1;
             i >= 0;
             i--) {

            NotificationRecord record =
                    history.get(i);

            String searchable =
                    normalize(
                            record.packageName
                                    + " "
                                    + record.title
                                    + " "
                                    + record.message
                                    + " "
                                    + record.category
                    );

            if (searchable.contains(target)) {

                found++;

                result.append(
                        found
                );

                result.append(
                        ". ["
                );

                result.append(
                        record.category
                );

                result.append(
                        "] "
                );

                result.append(
                        record.title
                );

                result.append(
                        "\n"
                );

                result.append(
                        record.message
                );

                result.append(
                        "\n\n"
                );
            }
        }

        if (found == 0) {

            return
                    "ما لقيتش إشعار متعلق بـ: "
                            + query;
        }

        return result.toString();
    }

    public synchronized String
    getNotificationsByCategory(
            String category
    ) {

        if (category == null ||
                category.trim().isEmpty()) {

            return
                    getNotificationHistory();
        }

        String target =
                normalize(category);

        StringBuilder result =
                new StringBuilder();

        int found = 0;

        for (int i =
             history.size() - 1;
             i >= 0;
             i--) {

            NotificationRecord record =
                    history.get(i);

            if (normalize(
                    record.category
            ).equals(target)) {

                found++;

                result.append(
                        found
                );

                result.append(
                        ". "
                );

                result.append(
                        record.title
                );

                result.append(
                        " — "
                );

                result.append(
                        record.message
                );

                result.append(
                        "\n"
                );
            }
        }

        if (found == 0) {

            return
                    "ما كاين حتى إشعار من التصنيف: "
                            + category;
        }

        return result.toString();
    }

    public synchronized void
    clearHistory() {

        history.clear();
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

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

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean
    isHealthy() {

        return context != null;
    }

    // =========================================================
    // STATUS
    // =========================================================

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
        )
                + "\n"
                + "Notifications: "
                + notificationCount
                + "\n"
                + "History: "
                + history.size()
                + "/"
                + MAX_HISTORY
                + "\n"
                + "Last Category: "
                + lastCategory;
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return context;
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "أ",
                        "ا"
                )
                .replace(
                        "إ",
                        "ا"
                )
                .replace(
                        "آ",
                        "ا"
                )
                .replace(
                        "ة",
                        "ه"
                )
                .replace(
                        "ى",
                        "ي"
                );
    }

    // =========================================================
    // MATCHING
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                text.isEmpty()) {

            return false;
        }

        for (String value : values) {

            if (value == null ||
                    value.isEmpty()) {

                continue;
            }

            if (text.contains(
                    normalize(value)
            )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // CLEAN
    // =========================================================

    private String clean(
            String value,
            String fallback
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }

    // =========================================================
    // RECORD
    // =========================================================

    private static class NotificationRecord {

        final String packageName;
        final String title;
        final String message;
        final String category;
        final long timestamp;

        NotificationRecord(
                String packageName,
                String title,
                String message,
                String category,
                long timestamp
        ) {

            this.packageName =
                    packageName;

            this.title =
                    title;

            this.message =
                    message;

            this.category =
                    category;

            this.timestamp =
                    timestamp;
        }
    }
}