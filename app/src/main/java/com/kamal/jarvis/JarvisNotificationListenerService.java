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

    @Override
    public void onListenerConnected() {

        super.onListenerConnected();

        instance = this;

        NotificationIntelligence
                .setServiceConnected(true);
    }

    @Override
    public void onNotificationPosted(
            StatusBarNotification statusBarNotification
    ) {

        if (statusBarNotification == null) {
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
                return;
            }

            String title = "";
            String message = "";

            Bundle extras =
                    notification.extras;

            if (extras != null) {

                CharSequence titleValue =
                        extras.getCharSequence(
                                Notification.EXTRA_TITLE
                        );

                CharSequence textValue =
                        extras.getCharSequence(
                                Notification.EXTRA_TEXT
                        );

                if (titleValue != null) {

                    title =
                            titleValue
                                    .toString()
                                    .trim();
                }

                if (textValue != null) {

                    message =
                            textValue
                                    .toString()
                                    .trim();
                }
            }

            if (title.isEmpty() &&
                    message.isEmpty()) {

                return;
            }

            String notificationKey =
                    packageName
                    + "|"
                    + title
                    + "|"
                    + message;

            if (notificationKey.equals(
                    lastNotificationKey
            )) {

                return;
            }

            lastNotificationKey =
                    notificationKey;

            lastPackageName =
                    packageName;

            lastTitle =
                    title;

            lastMessage =
                    message;

            NotificationIntelligence
                    .receiveNotification(
                            lastPackageName,
                            lastTitle,
                            lastMessage
                    );

        } catch (Exception e) {

            // JARVIS لا يتوقف بسبب إشعار غير صالح.
        }
    }

    @Override
    public void onNotificationRemoved(
            StatusBarNotification statusBarNotification
    ) {

        // JARVIS يحتفظ بآخر إشعار مهم
        // حتى يتمكن نظام الذكاء من تحليله.
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

    public static
    JarvisNotificationListenerService
    getInstance() {

        return instance;
    }

    public boolean isConnected() {

        return instance != null;
    }

    public String getLastPackageName() {

        return lastPackageName;
    }

    public String getLastTitle() {

        return lastTitle;
    }

    public String getLastMessage() {

        return lastMessage;
    }

    public String getLastNotificationSummary() {

        if (lastPackageName.isEmpty() &&
                lastTitle.isEmpty() &&
                lastMessage.isEmpty()) {

            return "لا توجد إشعارات محفوظة.";
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
}