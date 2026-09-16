package com.kamal.jarvis;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderEngine {

    private final Context context;
    private final MemoryManager memoryManager;

    public ReminderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);
    }

    public String createReminder(
            String title,
            long triggerTime
    ) {

        if (title == null ||
                title.trim().isEmpty()) {

            return "خاصك تكتب شنو بغيتي نتذكرك به.";
        }

        if (triggerTime <=
                System.currentTimeMillis()) {

            return "وقت التذكير خاصو يكون فالمستقبل.";
        }

        try {

            Intent intent =
                    new Intent(
                            context,
                            ReminderReceiver.class
                    );

            intent.putExtra(
                    "reminder_title",
                    title.trim()
            );

            int requestCode =
                    (int)
                            (System.currentTimeMillis()
                                    & 0x7fffffff);

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            AlarmManager alarmManager =
                    (AlarmManager)
                            context.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (alarmManager == null) {

                return
                        "Alarm Manager غير متوفر.";
            }

            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            String formatted =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date(triggerTime)
                    );

            memoryManager.saveMemory(
                    "__last_reminder__",
                    title.trim()
                            + " | "
                            + formatted
            );

            return
                    "تم إنشاء التذكير ✓\n\n"
                    + title.trim()
                    + "\n"
                    + formatted;

        } catch (Exception e) {

            return
                    "فشل إنشاء التذكير: "
                    + safeError(e);
        }
    }

    public String getLastReminder() {

        String reminder =
                memoryManager.getMemory(
                        "__last_reminder__"
                );

        if (reminder == null ||
                reminder.trim().isEmpty()) {

            return
                    "ما كاين حتى تذكير محفوظ.";
        }

        return
                "آخر تذكير:\n"
                + reminder;
    }

    public String clearLastReminder() {

        memoryManager.removeMemory(
                "__last_reminder__"
        );

        return
                "تم حذف معلومات آخر تذكير ✓";
    }

    public boolean isHealthy() {

        try {

            AlarmManager alarmManager =
                    (AlarmManager)
                            context.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            return alarmManager != null;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Reminder Engine: ONLINE ✓";

        }

        return
                "Reminder Engine: OFFLINE ⚠";
    }

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }
}