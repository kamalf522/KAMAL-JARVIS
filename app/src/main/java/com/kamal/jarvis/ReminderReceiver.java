package com.kamal.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "jarvis_reminders";

    private static final String CHANNEL_NAME =
            "JARVIS Reminders";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        String title =
                intent.getStringExtra(
                        "reminder_title"
                );

        if (title == null ||
                title.trim().isEmpty()) {

            title =
                    "عندك تذكير من JARVIS";
        }

        int reminderId =
                intent.getIntExtra(
                        "reminder_id",
                        (int)
                                (System.currentTimeMillis()
                                        & 0x7fffffff)
                );

        showNotification(
                context,
                title,
                reminderId
        );
    }

    private void showNotification(
            Context context,
            String title,
            int reminderId
    ) {

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
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
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        reminderId,
                        openIntent,
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

        Notification notification =
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
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        )
                        .setCategory(
                                Notification.CATEGORY_REMINDER
                        )
                        .setPriority(
                                Notification.PRIORITY_HIGH
                        )
                        .build();

        manager.notify(
                reminderId,
                notification
        );
    }

    private void createChannel(
            NotificationManager manager
    ) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

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
                "تنبيهات وتذكيرات JARVIS"
        );

        channel.enableVibration(true);

        manager.createNotificationChannel(
                channel
        );
    }
}