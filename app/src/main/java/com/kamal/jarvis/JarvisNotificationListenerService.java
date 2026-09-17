package com.kamal.jarvis;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;

public class JarvisNotificationListenerService
        extends NotificationListenerService {

    private static JarvisNotificationListenerService instance;

    private String lastPackageName = "";
    private String lastTitle = "";
    private String lastMessage = "";

    @Override
    public void onListenerConnected() {

        super.onListenerConnected();

        instance = this;

        NotificationIntelligence.setServiceConnected(true);
    }

    @Override
    public void onNotificationPosted(
            StatusBarNotification statusBarNotification
    ) {

        if (statusBarNotification == null) {
            return;
        }

        String packageName =
                statusBarNotification.getPackageName();

        String title = "";
        String message = "";

        Notification notification =
                statusBarNotification.getNotification();

        if (notification != null &&
                notification.extras != null) {

            Bundle extras =
                    notification.extras;

            CharSequence titleValue =
                    extras.getCharSequence(
                            Notification.EXTRA_TITLE
                    );

            CharSequence textValue =
                    extras.getCharSequence(
                            Notification.EXTRA_TEXT
                    );

            if (titleValue != null) {
                title = titleValue.toString();
            }

            if (textValue != null) {
                message = textValue.toString();
            }
        }

        lastPackageName =
                packageName == null
                        ? ""
                        : packageName;

        lastTitle = title;
        lastMessage = message;

        NotificationIntelligence
                .receiveNotification(
                        lastPackageName,
                        lastTitle,
                        lastMessage
                );
    }

    @Override
    public void onNotificationRemoved(
            StatusBarNotification statusBarNotification
    ) {

        // JARVIS keeps the last notification
        // for intelligence and memory purposes.
    }

    @Override
    public void onListenerDisconnected() {

        NotificationIntelligence
                .setServiceConnected(false);

        instance = null;

        super.onListenerDisconnected();
    }

    public static
    JarvisNotificationListenerService getInstance() {

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
}