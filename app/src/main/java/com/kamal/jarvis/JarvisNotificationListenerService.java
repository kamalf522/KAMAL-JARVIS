package com.kamal.jarvis;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class JarvisNotificationListenerService
        extends NotificationListenerService {

    private static JarvisNotificationListenerService instance;

    private String lastPackageName = "";
    private String lastTitle = "";
    private String lastMessage = "";

    private String lastNotificationKey = "";

    private long lastNotificationTime = 0L;

    private int receivedCount = 0;
    private int processedCount = 0;
    private int ignoredCount = 0;

    private static final long DUPLICATE_WINDOW_MS =
            3000L;

    private long lastProcessedTime = 0L;

    // =========================================================
    // CONNECTION
    // =========================================================

    @Override
    public void onListenerConnected() {

        super.onListenerConnected();

        instance = this;

        NotificationIntelligence
                .setServiceConnected(true);
    }

    @Override
    public void onListenerDisconnected() {

        NotificationIntelligence
                .setServiceConnected(false);

        if (instance == this) {
            instance = null;
        }

        super.onListenerDisconnected();
    }

    // =========================================================
    // NOTIFICATION POSTED
    // =========================================================

    @Override
    public void onNotificationPosted(
            StatusBarNotification statusBarNotification
    ) {

        receivedCount++;

        if (statusBarNotification == null) {

            ignoredCount++;

            return;
        }

        try {

            String packageName =
                    statusBarNotification
                            .getPackageName();

            if (packageName == null) {
                packageName = "";
            }

            Notification notification =
                    statusBarNotification
                            .getNotification();

            if (notification == null) {

                ignoredCount++;

                return;
            }

            Bundle extras =
                    notification.extras;

            String title =
                    extractTitle(extras);

            String message =
                    extractMessage(extras);

            /*
             * بعض الإشعارات كتكون عندها فقط
             * text أو title.
             */
            if (title.isEmpty() &&
                    message.isEmpty()) {

                ignoredCount++;

                return;
            }

            String notificationKey =
                    packageName
                    + "|"
                    + title
                    + "|"
                    + message;

            long now =
                    System.currentTimeMillis();

            /*
             * منع نفس الإشعار من التكرار
             * في مدة قصيرة.
             */
            if (notificationKey.equals(
                    lastNotificationKey
            )
                    && now - lastProcessedTime
                    < DUPLICATE_WINDOW_MS) {

                ignoredCount++;

                return;
            }

            lastNotificationKey =
                    notificationKey;

            lastProcessedTime =
                    now;

            lastPackageName =
                    packageName;

            lastTitle =
                    title;

            lastMessage =
                    message;

            lastNotificationTime =
                    now;

            processedCount++;

            NotificationIntelligence
                    .receiveNotification(
                            lastPackageName,
                            lastTitle,
                            lastMessage
                    );

        } catch (Exception e) {

            ignoredCount++;

            /*
             * JARVIS ما يطيحش بسبب إشعار
             * فيه بيانات غير متوقعة.
             */
        }
    }

    // =========================================================
    // TITLE EXTRACTION
    // =========================================================

    private String extractTitle(
            Bundle extras
    ) {

        if (extras == null) {
            return "";
        }

        try {

            CharSequence value =
                    extras.getCharSequence(
                            Notification.EXTRA_TITLE
                    );

            if (value != null) {

                String result =
                        value.toString().trim();

                if (!result.isEmpty()) {
                    return result;
                }
            }

            /*
             * بعض التطبيقات كتستعمل title text
             * بدل EXTRA_TITLE.
             */
            CharSequence fallback =
                    extras.getCharSequence(
                            Notification.EXTRA_TITLE_BIG
                    );

            if (fallback != null) {

                return fallback
                        .toString()
                        .trim();
            }

        } catch (Exception e) {

            return "";
        }

        return "";
    }

    // =========================================================
    // MESSAGE EXTRACTION
    // =========================================================

    private String extractMessage(
            Bundle extras
    ) {

        if (extras == null) {
            return "";
        }

        try {

            CharSequence value =
                    extras.getCharSequence(
                            Notification.EXTRA_TEXT
                    );

            if (value != null) {

                String result =
                        value.toString().trim();

                if (!result.isEmpty()) {
                    return result;
                }
            }

            /*
             * fallback للإشعارات اللي كتستعمل
             * BIG_TEXT.
             */
            CharSequence bigText =
                    extras.getCharSequence(
                            Notification.EXTRA_BIG_TEXT
                    );

            if (bigText != null) {

                String result =
                        bigText.toString().trim();

                if (!result.isEmpty()) {
                    return result;
                }
            }

            /*
             * بعض التطبيقات كتستعمل
             * summary text.
             */
            CharSequence summary =
                    extras.getCharSequence(
                            Notification.EXTRA_SUMMARY_TEXT
                    );

            if (summary != null) {

                return summary
                        .toString()
                        .trim();
            }

        } catch (Exception e) {

            return "";
        }

        return "";
    }

    // =========================================================
    // NOTIFICATION REMOVED
    // =========================================================

    @Override
    public void onNotificationRemoved(
            StatusBarNotification statusBarNotification
    ) {

        /*
         * JARVIS يحتفظ بآخر إشعار مهم
         * حتى بعد اختفائه من شريط الإشعارات.
         */
    }

    // =========================================================
    // ACTIVE INSTANCE
    // =========================================================

    public static
    JarvisNotificationListenerService
    getInstance() {

        return instance;
    }

    public boolean isConnected() {

        return instance == this;
    }

    // =========================================================
    // LAST NOTIFICATION
    // =========================================================

    public String getLastPackageName() {

        return lastPackageName;
    }

    public String getLastTitle() {

        return lastTitle;
    }

    public String getLastMessage() {

        return lastMessage;
    }

    public long getLastNotificationTime() {

        return lastNotificationTime;
    }

    public String getLastNotificationSummary() {

        if (lastPackageName.isEmpty() &&
                lastTitle.isEmpty() &&
                lastMessage.isEmpty()) {

            return
                    "لا توجد إشعارات محفوظة.";
        }

        return
                "التطبيق: "
                + lastPackageName
                + "\n"
                + "العنوان: "
                + lastTitle
                + "\n"
                + "الرسالة: "
                + lastMessage;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    public int getReceivedCount() {

        return receivedCount;
    }

    public int getProcessedCount() {

        return processedCount;
    }

    public int getIgnoredCount() {

        return ignoredCount;
    }

    public String getServiceStatus() {

        return
                "JARVIS NOTIFICATION LISTENER\n"
                + "============================\n"
                + "الحالة: "
                + (
                isConnected()
                        ? "CONNECTED ✓"
                        : "DISCONNECTED"
        )
                + "\n"
                + "المستقبلة: "
                + receivedCount
                + "\n"
                + "المعالجة: "
                + processedCount
                + "\n"
                + "المتجاهلة: "
                + ignoredCount;
    }
}