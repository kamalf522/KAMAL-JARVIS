package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private final Context context;
    private final MemoryManager memoryManager;

    private final List<Task> tasks =
            new ArrayList<>();

    public TaskManager(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(
                        this.context
                );

        loadTasks();
    }

    // =========================================================
    // ADD TASK
    // =========================================================

    public synchronized String addTask(
            String title
    ) {

        if (title == null ||
                title.trim().isEmpty()) {

            return
                    "خاصك تكتب اسم المهمة.";
        }

        String cleanTitle =
                title.trim();

        Task task =
                new Task(
                        System.currentTimeMillis(),
                        cleanTitle,
                        false
                );

        tasks.add(task);

        saveTasks();

        return
                "تمت إضافة المهمة ✓\n\n"
                + cleanTitle;
    }

    // =========================================================
    // COMPLETE TASK
    // =========================================================

    public synchronized String completeTask(
            int index
    ) {

        if (index < 0 ||
                index >= tasks.size()) {

            return
                    "رقم المهمة غير صحيح.";
        }

        Task task =
                tasks.get(index);

        task.completed = true;

        saveTasks();

        return
                "تم إنجاز المهمة ✓\n\n"
                + task.title;
    }

    // =========================================================
    // REMOVE TASK
    // =========================================================

    public synchronized String removeTask(
            int index
    ) {

        if (index < 0 ||
                index >= tasks.size()) {

            return
                    "رقم المهمة غير صحيح.";
        }

        Task removed =
                tasks.remove(index);

        saveTasks();

        return
                "تم حذف المهمة ✓\n\n"
                + removed.title;
    }

    // =========================================================
    // GET TASKS
    // =========================================================

    public synchronized String getTasks() {

        if (tasks.isEmpty()) {

            return
                    "ما كايناش مهام حاليا.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS TASK MANAGER\n"
        );

        result.append(
                "============================\n\n"
        );

        for (int i = 0;
             i < tasks.size();
             i++) {

            Task task =
                    tasks.get(i);

            result.append(
                    i + 1
            );

            result.append(". ");

            if (task.completed) {

                result.append("✓ ");

            } else {

                result.append("○ ");
            }

            result.append(
                    task.title
            );

            result.append("\n");
        }

        return result.toString();
    }

    // =========================================================
    // COUNTERS
    // =========================================================

    public synchronized int getTaskCount() {

        return tasks.size();
    }

    public synchronized int getPendingTaskCount() {

        int count = 0;

        for (Task task : tasks) {

            if (!task.completed) {

                count++;
            }
        }

        return count;
    }

    public synchronized int getCompletedTaskCount() {

        int count = 0;

        for (Task task : tasks) {

            if (task.completed) {

                count++;
            }
        }

        return count;
    }

    // =========================================================
    // CLEAR COMPLETED
    // =========================================================

    public synchronized String clearCompletedTasks() {

        int removedCount = 0;

        for (int i = tasks.size() - 1;
             i >= 0;
             i--) {

            if (tasks.get(i).completed) {

                tasks.remove(i);

                removedCount++;
            }
        }

        saveTasks();

        if (removedCount == 0) {

            return
                    "ما كايناش مهام مكتملة باش نحيدها.";
        }

        return
                "تم حذف "
                + removedCount
                + " مهمة مكتملة ✓";
    }

    /*
     * Compatibility method.
     *
     * مهم:
     * كان void من قبل، ولكن AutomationEngine
     * كيرجع النتيجة ديالو للمستخدم.
     *
     * تبديل void إلى String ما كيضرش الاستعمالات
     * اللي كتنادي عليه بلا ما تستعمل النتيجة.
     */
    public synchronized String clearCompleted() {

        return clearCompletedTasks();
    }

    // =========================================================
    // CLEAR ALL
    // =========================================================

    public synchronized String clearAllTasks() {

        int count =
                tasks.size();

        tasks.clear();

        saveTasks();

        if (count == 0) {

            return
                    "ما كايناش مهام باش نحيدها.";
        }

        return
                "تم حذف جميع المهام ✓\n"
                + "العدد: "
                + count;
    }

    /*
     * Compatibility method.
     *
     * نفس السبب: AutomationEngine محتاج
     * نتيجة نصية باش يرجعها للمستخدم.
     */
    public synchronized String clearAll() {

        return clearAllTasks();
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            getTaskCount();

            getPendingTaskCount();

            getCompletedTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Task Manager: ONLINE ✓\n"
                    + "Tasks: "
                    + getTaskCount()
                    + "\nPending: "
                    + getPendingTaskCount()
                    + "\nCompleted: "
                    + getCompletedTaskCount();
        }

        return
                "Task Manager: ERROR ⚠";
    }

    // =========================================================
    // SAVE
    // =========================================================

    private void saveTasks() {

        StringBuilder data =
                new StringBuilder();

        for (Task task : tasks) {

            data.append(
                    task.id
            );

            data.append("|");

            data.append(
                    escape(
                            task.title
                    )
            );

            data.append("|");

            data.append(
                    task.completed
            );

            data.append("\n");
        }

        memoryManager.saveMemory(
                "__jarvis_tasks__",
                data.toString()
        );
    }

    // =========================================================
    // LOAD
    // =========================================================

    private void loadTasks() {

        try {

            String data =
                    memoryManager.getMemory(
                            "__jarvis_tasks__"
                    );

            if (data == null ||
                    data.trim().isEmpty()) {

                return;
            }

            String[] lines =
                    data.split(
                            "\\n"
                    );

            for (String line : lines) {

                if (line.trim().isEmpty()) {

                    continue;
                }

                String[] parts =
                        line.split(
                                "\\|",
                                3
                        );

                if (parts.length < 3) {

                    continue;
                }

                long id =
                        Long.parseLong(
                                parts[0]
                        );

                String title =
                        unescape(
                                parts[1]
                        );

                boolean completed =
                        Boolean.parseBoolean(
                                parts[2]
                        );

                tasks.add(
                        new Task(
                                id,
                                title,
                                completed
                        )
                );
            }

        } catch (Exception ignored) {

            tasks.clear();
        }
    }

    // =========================================================
    // ESCAPE
    // =========================================================

    private String escape(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "|",
                        "\\p"
                )
                .replace(
                        "\n",
                        "\\n"
                );
    }

    // =========================================================
    // UNESCAPE
    // =========================================================

    private String unescape(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "\\n",
                        "\n"
                )
                .replace(
                        "\\p",
                        "|"
                )
                .replace(
                        "\\\\",
                        "\\"
                );
    }

    // =========================================================
    // TASK MODEL
    // =========================================================

    private static class Task {

        final long id;
        final String title;

        boolean completed;

        Task(
                long id,
                String title,
                boolean completed
        ) {

            this.id =
                    id;

            this.title =
                    title;

            this.completed =
                    completed;
        }
    }
}