package com.kamal.jarvis;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderEngine {

    private static final String PREFIX =
            "__reminder__";

    private static final String LAST_REMINDER =
            "__last_reminder__";

    private final Context context;
    private final MemoryManager memoryManager;

    public ReminderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);
    }

    public synchronized String createReminder(
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

            AlarmManager alarmManager =
                    (AlarmManager)
                            context.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (alarmManager == null) {

                return
                        "Alarm Manager غير متوفر.";
            }

            String cleanTitle =
                    title.trim();

            int requestCode =
                    (int)
                            (System.currentTimeMillis()
                                    & 0x7fffffff);

            Intent intent =
                    new Intent(
                            context,
                            ReminderReceiver.class
                    );

            intent.putExtra(
                    "reminder_title",
                    cleanTitle
            );

            intent.putExtra(
                    "reminder_id",
                    requestCode
            );

            intent.putExtra(
                    "reminder_time",
                    triggerTime
            );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            /*
             * نخلي Android يصحي الجهاز
             * ملي يوصل وقت التذكير.
             */
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            String formatted =
                    formatTime(triggerTime);

            memoryManager.saveMemory(
                    PREFIX + requestCode,
                    cleanTitle
                            + " | "
                            + formatted
            );

            memoryManager.saveMemory(
                    LAST_REMINDER,
                    cleanTitle
                            + " | "
                            + formatted
            );

            return
                    "تم إنشاء التذكير ✓\n\n"
                    + cleanTitle
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
                        LAST_REMINDER
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

    public String getReminder(
            int reminderId
    ) {

        String reminder =
                memoryManager.getMemory(
                        PREFIX + reminderId
                );

        if (reminder == null ||
                reminder.trim().isEmpty()) {

            return
                    "ما لقيتش هاد التذكير.";
        }

        return
                "التذكير:\n"
                + reminder;
    }

    public String cancelReminder(
            int reminderId
    ) {

        try {

            AlarmManager alarmManager =
                    (AlarmManager)
                            context.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (alarmManager == null) {

                return
                        "Alarm Manager غير متوفر.";
            }

            Intent intent =
                    new Intent(
                            context,
                            ReminderReceiver.class
                    );

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            reminderId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            alarmManager.cancel(
                    pendingIntent
            );

            pendingIntent.cancel();

            memoryManager.removeMemory(
                    PREFIX + reminderId
            );

            return
                    "تم إلغاء التذكير ✓";

        } catch (Exception e) {

            return
                    "فشل إلغاء التذكير: "
                    + safeError(e);
        }
    }

    public String clearLastReminder() {

        memoryManager.removeMemory(
                LAST_REMINDER
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

    private String formatTime(
            long time
    ) {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
        ).format(
                new Date(time)
        );
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