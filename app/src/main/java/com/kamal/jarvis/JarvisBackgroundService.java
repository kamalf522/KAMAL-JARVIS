package com.kamal.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class JarvisBackgroundService extends Service {

    private static final String CHANNEL_ID =
            "jarvis_background_channel";

    private static final int NOTIFICATION_ID =
            1001;

    private static final String ACTION_START =
            "com.kamal.jarvis.action.START";

    private static final String ACTION_STOP =
            "com.kamal.jarvis.action.STOP";

    private static JarvisBackgroundService instance;

    private volatile boolean running = false;

    private long startedAt = 0L;

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        createNotificationChannel();

        startedAt = System.currentTimeMillis();

        running = true;
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent != null) {

            String action =
                    intent.getAction();

            if (ACTION_STOP.equals(action)) {

                stopServiceSafely();

                return START_NOT_STICKY;
            }
        }

        try {

            Notification notification =
                    createNotification();

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );
            }

            running = true;

        } catch (Exception e) {

            running = false;
        }

        return START_STICKY;
    }

    // =========================================================
    // START
    // =========================================================

    public static void start(
            Context context
    ) {

        if (context == null) {
            return;
        }

        try {

            Context appContext =
                    context.getApplicationContext();

            Intent intent =
                    new Intent(
                            appContext,
                            JarvisBackgroundService.class
                    );

            intent.setAction(
                    ACTION_START
            );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                appContext.startForegroundService(
                        intent
                );

            } else {

                appContext.startService(
                        intent
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    public static void stop(
            Context context
    ) {

        if (context == null) {
            return;
        }

        try {

            Context appContext =
                    context.getApplicationContext();

            Intent intent =
                    new Intent(
                            appContext,
                            JarvisBackgroundService.class
                    );

            intent.setAction(
                    ACTION_STOP
            );

            appContext.startService(
                    intent
            );

        } catch (Exception ignored) {

            try {

                context.stopService(
                        new Intent(
                                context,
                                JarvisBackgroundService.class
                        )
                );

            } catch (Exception ignoredAgain) {
            }
        }
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

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
                    new Notification.Builder(
                            this
                    );
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
                .setAutoCancel(false)
                .setCategory(
                        Notification.CATEGORY_SERVICE
                )
                .setPriority(
                        Notification.PRIORITY_LOW
                )
                .setShowWhen(false)
                .build();
    }

    // =========================================================
    // CHANNEL
    // =========================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        try {

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

            channel.setSound(
                    null,
                    null
            );

            manager.createNotificationChannel(
                    channel
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // STOP INTERNAL
    // =========================================================

    private void stopServiceSafely() {

        running = false;

        try {

            stopForeground(
                    true
            );

        } catch (Exception ignored) {
        }

        try {

            stopSelf();

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        running = false;

        startedAt = 0L;

        if (instance == this) {

            instance = null;
        }

        try {

            stopForeground(
                    true
            );

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    // =========================================================
    // BIND
    // =========================================================

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }

    // =========================================================
    // STATUS
    // =========================================================

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

    public long getStartedAt() {

        return startedAt;
    }

    public long getUptimeMillis() {

        if (!running ||
                startedAt <= 0L) {

            return 0L;
        }

        return
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - startedAt
                );
    }

    public String getStatus() {

        if (!running) {

            return
                    "JARVIS BACKGROUND SERVICE\n"
                    + "========================\n\n"
                    + "الحالة: OFFLINE";
        }

        long uptime =
                getUptimeMillis();

        long seconds =
                uptime / 1000L;

        long minutes =
                seconds / 60L;

        long hours =
                minutes / 60L;

        minutes =
                minutes % 60L;

        seconds =
                seconds % 60L;

        return
                "JARVIS BACKGROUND SERVICE\n"
                + "========================\n\n"
                + "الحالة: ONLINE ✓\n"
                + "مدة التشغيل: "
                + hours
                + "h "
                + minutes
                + "m "
                + seconds
                + "s";
    }

    public static String
    getServiceStatus() {

        if (instance == null) {

            return
                    "JARVIS BACKGROUND SERVICE\n"
                    + "========================\n\n"
                    + "الحالة: OFFLINE";
        }

        return instance.getStatus();
    }
}