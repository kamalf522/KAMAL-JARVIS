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

    private static final String INDEX_KEY =
            "__reminder_index__";

    private static final String LAST_REMINDER =
            "__last_reminder__";

    private static final String COUNT_KEY =
            "__reminder_count__";

    private final Context context;
    private final MemoryManager memoryManager;

    private int reminderCount = 0;

    public ReminderEngine(Context context) {

        if (context != null) {
            this.context =
                    context.getApplicationContext();
        } else {
            this.context = null;
        }

        if (this.context != null) {

            memoryManager =
                    new MemoryManager(
                            this.context
                    );

            reminderCount =
                    loadCount();

        } else {

            memoryManager = null;
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    public synchronized String createReminder(
            String title,
            long triggerTime
    ) {

        if (!isReady()) {
            return "Reminder Engine غير جاهز.";
        }

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
                    getAlarmManager();

            if (alarmManager == null) {
                return "Alarm Manager غير متوفر.";
            }

            String cleanTitle =
                    title.trim();

            int requestCode =
                    generateReminderId();

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

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            String formatted =
                    formatTime(triggerTime);

            String record =
                    cleanTitle
                            + " | "
                            + formatted
                            + " | "
                            + triggerTime;

            memoryManager.saveMemory(
                    PREFIX + requestCode,
                    record
            );

            addToIndex(
                    requestCode
            );

            memoryManager.saveMemory(
                    LAST_REMINDER,
                    cleanTitle
                            + " | "
                            + formatted
            );

            reminderCount++;

            saveCount();

            return
                    "تم إنشاء التذكير ✓\n\n"
                            + cleanTitle
                            + "\n"
                            + formatted
                            + "\nID: "
                            + requestCode;

        } catch (Exception e) {

            return
                    "فشل إنشاء التذكير: "
                            + safeError(e);
        }
    }

    // =========================================================
    // COMPLETE / FIRE
    // =========================================================

    public synchronized String completeReminder(
            int reminderId
    ) {

        if (!isReady()) {
            return "Reminder Engine غير جاهز.";
        }

        try {

            cancelAlarmOnly(
                    reminderId
            );

            String reminder =
                    memoryManager.getMemory(
                            PREFIX + reminderId
                    );

            if (reminder == null ||
                    reminder.trim().isEmpty()) {

                removeFromIndex(
                        reminderId
                );

                return
                        "التذكير غير موجود أو سبق إكماله.";
            }

            memoryManager.removeMemory(
                    PREFIX + reminderId
            );

            removeFromIndex(
                    reminderId
            );

            if (reminderCount > 0) {
                reminderCount--;
            }

            saveCount();

            return
                    "تم تنفيذ التذكير ✓";

        } catch (Exception e) {

            return
                    "فشل إكمال التذكير: "
                            + safeError(e);
        }
    }

    // =========================================================
    // GET LAST
    // =========================================================

    public synchronized String getLastReminder() {

        if (memoryManager == null) {
            return "Reminder Engine غير جاهز.";
        }

        String reminder =
                memoryManager.getMemory(
                        LAST_REMINDER
                );

        if (reminder == null ||
                reminder.trim().isEmpty()) {

            return "ما كاين حتى تذكير محفوظ.";
        }

        return
                "آخر تذكير:\n"
                        + reminder;
    }

    // =========================================================
    // GET ONE
    // =========================================================

    public synchronized String getReminder(
            int reminderId
    ) {

        if (memoryManager == null) {
            return "Reminder Engine غير جاهز.";
        }

        String reminder =
                memoryManager.getMemory(
                        PREFIX + reminderId
                );

        if (reminder == null ||
                reminder.trim().isEmpty()) {

            return "ما لقيتش هاد التذكير.";
        }

        return
                "التذكير:\n"
                        + reminder;
    }

    // =========================================================
    // LIST
    // =========================================================

    public synchronized String getAllReminders() {

        if (memoryManager == null) {
            return "Reminder Engine غير جاهز.";
        }

        String index =
                memoryManager.getMemory(
                        INDEX_KEY
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return "ما كاين حتى تذكير نشط.";
        }

        String[] ids =
                index.split(",");

        StringBuilder result =
                new StringBuilder();

        int found = 0;

        result.append(
                "JARVIS REMINDERS\n"
        );

        result.append(
                "================\n\n"
        );

        for (String idText : ids) {

            if (idText == null ||
                    idText.trim().isEmpty()) {
                continue;
            }

            try {

                int id =
                        Integer.parseInt(
                                idText.trim()
                        );

                String reminder =
                        memoryManager.getMemory(
                                PREFIX + id
                        );

                if (reminder == null ||
                        reminder.trim().isEmpty()) {
                    continue;
                }

                found++;

                result.append(
                        found
                );

                result.append(
                        ". "
                );

                result.append(
                        reminder
                );

                result.append(
                        "\nID: "
                );

                result.append(
                        id
                );

                result.append(
                        "\n\n"
                );

            } catch (Exception ignored) {
            }
        }

        if (found == 0) {
            return "ما كاين حتى تذكير نشط.";
        }

        return result.toString();
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public synchronized String searchReminders(
            String query
    ) {

        if (memoryManager == null) {
            return "Reminder Engine غير جاهز.";
        }

        if (query == null ||
                query.trim().isEmpty()) {

            return getAllReminders();
        }

        String target =
                normalize(query);

        String index =
                memoryManager.getMemory(
                        INDEX_KEY
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return "ما كاين حتى تذكير.";
        }

        StringBuilder result =
                new StringBuilder();

        int found = 0;

        String[] ids =
                index.split(",");

        for (String idText : ids) {

            if (idText == null ||
                    idText.trim().isEmpty()) {
                continue;
            }

            try {

                int id =
                        Integer.parseInt(
                                idText.trim()
                        );

                String reminder =
                        memoryManager.getMemory(
                                PREFIX + id
                        );

                if (reminder == null) {
                    continue;
                }

                if (normalize(
                        reminder
                ).contains(target)) {

                    found++;

                    result.append(
                            found
                    );

                    result.append(
                            ". "
                    );

                    result.append(
                            reminder
                    );

                    result.append(
                            "\nID: "
                    );

                    result.append(
                            id
                    );

                    result.append(
                            "\n\n"
                    );
                }

            } catch (Exception ignored) {
            }
        }

        if (found == 0) {

            return
                    "ما لقيتش تذكير متعلق بـ: "
                            + query;
        }

        return result.toString();
    }

    // =========================================================
    // CANCEL
    // =========================================================

    public synchronized String cancelReminder(
            int reminderId
    ) {

        if (!isReady()) {
            return "Reminder Engine غير جاهز.";
        }

        try {

            String existing =
                    memoryManager.getMemory(
                            PREFIX + reminderId
                    );

            if (existing == null ||
                    existing.trim().isEmpty()) {

                return "ما لقيتش هاد التذكير.";
            }

            cancelAlarmOnly(
                    reminderId
            );

            memoryManager.removeMemory(
                    PREFIX + reminderId
            );

            removeFromIndex(
                    reminderId
            );

            if (reminderCount > 0) {
                reminderCount--;
            }

            saveCount();

            return
                    "تم إلغاء التذكير ✓";

        } catch (Exception e) {

            return
                    "فشل إلغاء التذكير: "
                            + safeError(e);
        }
    }

    // =========================================================
    // CLEAR LAST
    // =========================================================

    public synchronized String clearLastReminder() {

        if (memoryManager != null) {

            memoryManager.removeMemory(
                    LAST_REMINDER
            );
        }

        return
                "تم حذف معلومات آخر تذكير ✓";
    }

    // =========================================================
    // CLEAR ALL
    // =========================================================

    public synchronized String clearAllReminders() {

        if (!isReady()) {
            return "Reminder Engine غير جاهز.";
        }

        try {

            String index =
                    memoryManager.getMemory(
                            INDEX_KEY
                    );

            if (index != null &&
                    !index.trim().isEmpty()) {

                String[] ids =
                        index.split(",");

                for (String idText : ids) {

                    try {

                        int id =
                                Integer.parseInt(
                                        idText.trim()
                                );

                        cancelAlarmOnly(id);

                        memoryManager.removeMemory(
                                PREFIX + id
                        );

                    } catch (Exception ignored) {
                    }
                }
            }

            memoryManager.removeMemory(
                    INDEX_KEY
            );

            memoryManager.removeMemory(
                    LAST_REMINDER
            );

            reminderCount = 0;

            saveCount();

            return
                    "تم حذف جميع التذكيرات ✓";

        } catch (Exception e) {

            return
                    "فشل حذف التذكيرات: "
                            + safeError(e);
        }
    }

    // =========================================================
    // COUNT
    // =========================================================

    public synchronized int getReminderCount() {

        return reminderCount;
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            if (context == null ||
                    memoryManager == null) {

                return false;
            }

            return getAlarmManager() != null;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        if (!isHealthy()) {

            return
                    "Reminder Engine: OFFLINE ⚠";
        }

        return
                "Reminder Engine: ONLINE ✓\n"
                        + "Reminders: "
                        + reminderCount;
    }

    // =========================================================
    // READY
    // =========================================================

    private boolean isReady() {

        return context != null &&
                memoryManager != null;
    }

    // =========================================================
    // ALARM MANAGER
    // =========================================================

    private AlarmManager getAlarmManager() {

        if (context == null) {
            return null;
        }

        return
                (AlarmManager)
                        context.getSystemService(
                                Context.ALARM_SERVICE
                        );
    }

    // =========================================================
    // CANCEL ALARM ONLY
    // =========================================================

    private void cancelAlarmOnly(
            int reminderId
    ) {

        AlarmManager alarmManager =
                getAlarmManager();

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(
                        context,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "reminder_id",
                reminderId
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
    }

    // =========================================================
    // ID
    // =========================================================

    private int generateReminderId() {

        int id =
                (int)
                        (
                                System.currentTimeMillis()
                                        & 0x7fffffff
                        );

        if (id <= 0) {
            id = 1;
        }

        while (
                memoryManager != null &&
                memoryManager.getMemory(
                        PREFIX + id
                ) != null
        ) {

            id++;

            if (id <= 0) {
                id = 1;
            }
        }

        return id;
    }

    // =========================================================
    // INDEX ADD
    // =========================================================

    private void addToIndex(
            int reminderId
    ) {

        if (memoryManager == null) {
            return;
        }

        String index =
                memoryManager.getMemory(
                        INDEX_KEY
                );

        String id =
                String.valueOf(
                        reminderId
                );

        if (index == null ||
                index.trim().isEmpty()) {

            memoryManager.saveMemory(
                    INDEX_KEY,
                    id
            );

            return;
        }

        String[] ids =
                index.split(",");

        for (String existing : ids) {

            if (id.equals(
                    existing.trim()
            )) {

                return;
            }
        }

        memoryManager.saveMemory(
                INDEX_KEY,
                index + "," + id
        );
    }

    // =========================================================
    // INDEX REMOVE
    // =========================================================

    private void removeFromIndex(
            int reminderId
    ) {

        if (memoryManager == null) {
            return;
        }

        String index =
                memoryManager.getMemory(
                        INDEX_KEY
                );

        if (index == null ||
                index.trim().isEmpty()) {
            return;
        }

        String target =
                String.valueOf(
                        reminderId
                );

        StringBuilder newIndex =
                new StringBuilder();

        String[] ids =
                index.split(",");

        for (String id : ids) {

            if (id == null ||
                    id.trim().isEmpty()) {
                continue;
            }

            if (target.equals(
                    id.trim()
            )) {
                continue;
            }

            if (newIndex.length() > 0) {
                newIndex.append(",");
            }

            newIndex.append(
                    id.trim()
            );
        }

        if (newIndex.length() == 0) {

            memoryManager.removeMemory(
                    INDEX_KEY
            );

        } else {

            memoryManager.saveMemory(
                    INDEX_KEY,
                    newIndex.toString()
            );
        }
    }

    // =========================================================
    // COUNT
    // =========================================================

    private int loadCount() {

        if (memoryManager == null) {
            return 0;
        }

        try {

            String value =
                    memoryManager.getMemory(
                            COUNT_KEY
                    );

            if (value == null ||
                    value.trim().isEmpty()) {

                return 0;
            }

            return Math.max(
                    0,
                    Integer.parseInt(
                            value.trim()
                    )
            );

        } catch (Exception e) {

            return 0;
        }
    }

    private void saveCount() {

        if (memoryManager == null) {
            return;
        }

        memoryManager.saveMemory(
                COUNT_KEY,
                String.valueOf(
                        reminderCount
                )
        );
    }

    // =========================================================
    // FORMAT
    // =========================================================

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

    // =========================================================
    // NORMALIZE
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
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }

    // =========================================================
    // ERROR
    // =========================================================

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

            return
                    e.getClass()
                            .getSimpleName();
        }

        return message;
    }
}