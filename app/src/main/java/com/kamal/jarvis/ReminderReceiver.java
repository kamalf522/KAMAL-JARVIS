package com.kamal.jarvis;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "jarvis_reminders";

    private static final String CHANNEL_NAME =
            "JARVIS Reminders";

    private static final String CHANNEL_DESCRIPTION =
            "تنبيهات وتذكيرات JARVIS";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        if (context == null) {
            return;
        }

        try {

            String title =
                    intent.getStringExtra(
                            "reminder_title"
                    );

            if (title == null ||
                    title.trim().isEmpty()) {

                title =
                        "عندك تذكير من JARVIS";
            }

            title = title.trim();

            int reminderId =
                    intent.getIntExtra(
                            "reminder_id",
                            generateFallbackId()
                    );

            long reminderTime =
                    intent.getLongExtra(
                            "reminder_time",
                            System.currentTimeMillis()
                    );

            showNotification(
                    context.getApplicationContext(),
                    title,
                    reminderId,
                    reminderTime
            );

        } catch (Exception ignored) {
            // ما نخليوش BroadcastReceiver يطيح بسبب التذكير
        }
    }

    // =========================================================
    // SHOW NOTIFICATION
    // =========================================================

    private void showNotification(
            Context context,
            String title,
            int reminderId,
            long reminderTime
    ) {

        if (context == null) {
            return;
        }

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        /*
         * Android 13+
         * خاص صلاحية الإشعارات تكون مفعلة.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        createChannel(manager);

        Intent openIntent =
                new Intent(
                        context,
                        MainActivity.class
                );

        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        openIntent.putExtra(
                "jarvis_reminder_id",
                reminderId
        );

        openIntent.putExtra(
                "jarvis_reminder_title",
                title
        );

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        context,
                        reminderId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        Intent deleteIntent =
                new Intent(
                        context,
                        ReminderReceiver.class
                );

        deleteIntent.setAction(
                "com.kamal.jarvis.REMINDER_DISMISSED"
        );

        deleteIntent.putExtra(
                "reminder_id",
                reminderId
        );

        PendingIntent deletePendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        reminderId + 100000,
                        deleteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            context,
                            CHANNEL_ID
                    );

        } else {

            builder =
                    new Notification.Builder(
                            context
                    );
        }

        builder
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                        "JARVIS 🔔"
                )
                .setContentText(
                        title
                )
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(
                                        title
                                )
                )
                .setContentIntent(
                        contentIntent
                )
                .setDeleteIntent(
                        deletePendingIntent
                )
                .setAutoCancel(true)
                .setCategory(
                        Notification.CATEGORY_REMINDER
                )
                .setPriority(
                        Notification.PRIORITY_HIGH
                )
                .setWhen(
                        reminderTime
                )
                .setShowWhen(true)
                .setOnlyAlertOnce(false);

        if (Build.VERSION.SDK_INT < 26) {

            builder
                    .setSound(
                            RingtoneManager.getDefaultUri(
                                    RingtoneManager.TYPE_NOTIFICATION
                            )
                    )
                    .setVibrate(
                            new long[]{
                                    0,
                                    250,
                                    150,
                                    250
                            }
                    );
        }

        Notification notification =
                builder.build();

        manager.notify(
                reminderId,
                notification
        );
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private void createChannel(
            NotificationManager manager
    ) {

        if (manager == null ||
                Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.O) {

            return;
        }

        NotificationChannel existing =
                manager.getNotificationChannel(
                        CHANNEL_ID
                );

        if (existing != null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager
                                .IMPORTANCE_HIGH
                );

        channel.setDescription(
                CHANNEL_DESCRIPTION
        );

        channel.enableVibration(true);

        channel.setVibrationPattern(
                new long[]{
                        0,
                        250,
                        150,
                        250
                }
        );

        Uri sound =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes
                                        .USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes
                                        .CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        channel.setSound(
                sound,
                audioAttributes
        );

        manager.createNotificationChannel(
                channel
        );
    }

    // =========================================================
    // FALLBACK ID
    // =========================================================

    private int generateFallbackId() {

        int id =
                (int)
                        (
                                System.currentTimeMillis()
                                        & 0x7fffffff
                        );

        if (id <= 0) {
            return 1;
        }

        return id;
    }
}