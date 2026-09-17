package com.kamal.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class JarvisBackgroundService extends Service {

    private static final String CHANNEL_ID =
            "jarvis_background_channel";

    private static final int NOTIFICATION_ID =
            1001;

    private static JarvisBackgroundService instance;

    private volatile boolean running = false;

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        createNotificationChannel();

        running = true;
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        try {

            Notification notification =
                    createNotification();

            startForeground(
                    NOTIFICATION_ID,
                    notification
            );

            running = true;

        } catch (Exception e) {

            running = false;
        }

        return START_STICKY;
    }

    private Notification createNotification() {

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    );

        } else {

            builder =
                    new Notification.Builder(this);
        }

        return builder
                .setContentTitle(
                        "Kamal JARVIS"
                )
                .setContentText(
                        "JARVIS يعمل في الخلفية"
                )
                .setSmallIcon(
                        android.R.drawable
                                .ic_dialog_info
                )
                .setOngoing(true)
                .setCategory(
                        Notification.CATEGORY_SERVICE
                )
                .setPriority(
                        Notification.PRIORITY_LOW
                )
                .build();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager == null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "JARVIS Background Service",
                        NotificationManager
                                .IMPORTANCE_LOW
                );

        channel.setDescription(
                "خدمة JARVIS الأساسية في الخلفية"
        );

        channel.setShowBadge(false);

        manager.createNotificationChannel(
                channel
        );
    }

    @Override
    public void onDestroy() {

        running = false;

        if (instance == this) {
            instance = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public boolean isRunning() {

        return running;
    }

    public static boolean isServiceRunning() {

        return instance != null
                && instance.running;
    }

    public static JarvisBackgroundService
    getInstance() {

        return instance;
    }

    public String getStatus() {

        return
                "JARVIS BACKGROUND SERVICE\n"
                + "========================\n\n"
                + "الحالة: "
                + (
                        running
                                ? "ONLINE ✓"
                                : "OFFLINE"
                );
    }
}