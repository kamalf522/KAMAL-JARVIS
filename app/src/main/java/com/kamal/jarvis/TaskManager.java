package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskManager {

    private final Context context;
    private final MemoryManager memoryManager;

    private final List<Task> tasks =
            new ArrayList<>();

    private static final String TASKS_KEY =
            "__jarvis_tasks__";

    public TaskManager(Context context) {

        if (context != null) {
            this.context =
                    context.getApplicationContext();
        } else {
            this.context = null;
        }

        memoryManager =
                new MemoryManager(this.context);

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

            return "خاصك تكتب اسم المهمة.";
        }

        String cleanTitle =
                title.trim();

        Task task =
                new Task(
                        createUniqueId(),
                        cleanTitle,
                        false
                );

        tasks.add(task);

        if (!saveTasks()) {

            tasks.remove(task);

            return "فشل حفظ المهمة.";
        }

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

        if (!isValidIndex(index)) {

            return "رقم المهمة غير صحيح.";
        }

        Task task =
                tasks.get(index);

        if (task.completed) {

            return
                    "المهمة مكتملة أصلا ✓\n\n"
                            + task.title;
        }

        task.completed = true;

        if (!saveTasks()) {

            task.completed = false;

            return "فشل حفظ حالة المهمة.";
        }

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

        if (!isValidIndex(index)) {

            return "رقم المهمة غير صحيح.";
        }

        Task removed =
                tasks.remove(index);

        if (!saveTasks()) {

            tasks.add(index, removed);

            return "فشل حذف المهمة.";
        }

        return
                "تم حذف المهمة ✓\n\n"
                        + removed.title;
    }

    // =========================================================
    // GET TASKS
    // =========================================================

    public synchronized String getTasks() {

        if (tasks.isEmpty()) {

            return "ما عندك حتى مهمة حاليا.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append("📋 المهام:\n\n");

        for (int i = 0;
             i < tasks.size();
             i++) {

            Task task =
                    tasks.get(i);

            result.append(i + 1)
                    .append(". ");

            if (task.completed) {
                result.append("✓ ");
            } else {
                result.append("○ ");
            }

            result.append(task.title)
                    .append("\n");
        }

        return result.toString().trim();
    }

    // =========================================================
    // SEARCH TASKS
    // =========================================================

    public synchronized String searchTasks(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return getTasks();
        }

        String cleanQuery =
                query.trim()
                        .toLowerCase();

        StringBuilder result =
                new StringBuilder();

        int found = 0;

        for (int i = 0;
             i < tasks.size();
             i++) {

            Task task =
                    tasks.get(i);

            if (task.title
                    .toLowerCase()
                    .contains(cleanQuery)) {

                found++;

                result.append(i + 1)
                        .append(". ");

                result.append(
                        task.completed
                                ? "✓ "
                                : "○ "
                );

                result.append(task.title)
                        .append("\n");
            }
        }

        if (found == 0) {

            return
                    "ما لقيتش مهمة بهاد البحث.";
        }

        return result.toString().trim();
    }

    // =========================================================
    // TASK COUNT
    // =========================================================

    public synchronized int getTaskCount() {

        return tasks.size();
    }

    // =========================================================
    // PENDING COUNT
    // =========================================================

    public synchronized int getPendingTaskCount() {

        int count = 0;

        for (Task task : tasks) {

            if (!task.completed) {
                count++;
            }
        }

        return count;
    }

    // =========================================================
    // COMPLETED COUNT
    // =========================================================

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

        int removed = 0;

        for (int i = tasks.size() - 1;
             i >= 0;
             i--) {

            if (tasks.get(i).completed) {

                tasks.remove(i);
                removed++;
            }
        }

        if (!saveTasks()) {

            loadTasks();

            return
                    "فشل حذف المهام المكتملة.";
        }

        return
                "تم حذف "
                        + removed
                        + " مهمة مكتملة ✓";
    }

    // =========================================================
    // CLEAR ALL
    // =========================================================

    public synchronized String clearTasks() {

        List<Task> backup =
                new ArrayList<>(tasks);

        tasks.clear();

        if (!saveTasks()) {

            tasks.addAll(backup);

            return "فشل مسح المهام.";
        }

        return "تم مسح جميع المهام ✓";
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            return
                    memoryManager != null &&
                    tasks != null;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "📋 Task Manager\n\n"
        );

        report.append("الحالة: ")
                .append(
                        isHealthy()
                                ? "جاهز ✓"
                                : "خطأ ✗"
                )
                .append("\n");

        report.append("إجمالي المهام: ")
                .append(getTaskCount())
                .append("\n");

        report.append("المعلقة: ")
                .append(getPendingTaskCount())
                .append("\n");

        report.append("المكتملة: ")
                .append(getCompletedTaskCount());

        return report.toString();
    }

    // =========================================================
    // SAVE
    // =========================================================

    private synchronized boolean saveTasks() {

        try {

            StringBuilder data =
                    new StringBuilder();

            for (Task task : tasks) {

                data.append(
                        encode(task.id)
                );

                data.append("|");

                data.append(
                        encode(task.title)
                );

                data.append("|");

                data.append(
                        task.completed
                                ? "1"
                                : "0"
                );

                data.append("\n");
            }

            memoryManager.saveMemory(
                    TASKS_KEY,
                    data.toString()
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // LOAD
    // =========================================================

    private synchronized void loadTasks() {

        tasks.clear();

        try {

            String data =
                    memoryManager.getMemory(
                            TASKS_KEY
                    );

            if (data == null ||
                    data.trim().isEmpty()) {

                return;
            }

            String[] lines =
                    data.split("\\n");

            for (String line : lines) {

                if (line == null ||
                        line.trim().isEmpty()) {

                    continue;
                }

                String[] parts =
                        line.split(
                                "\\|",
                                -1
                        );

                if (parts.length < 3) {
                    continue;
                }

                String id =
                        decode(parts[0]);

                String title =
                        decode(parts[1]);

                boolean completed =
                        "1".equals(parts[2]);

                if (title == null ||
                        title.trim().isEmpty()) {

                    continue;
                }

                tasks.add(
                        new Task(
                                id,
                                title,
                                completed
                        )
                );
            }

        } catch (Exception e) {

            tasks.clear();
        }
    }

    // =========================================================
    // UNIQUE ID
    // =========================================================

    private String createUniqueId() {

        return
                System.currentTimeMillis()
                        + "_"
                        + UUID.randomUUID()
                        .toString();
    }

    // =========================================================
    // INDEX VALIDATION
    // =========================================================

    private boolean isValidIndex(
            int index
    ) {

        return
                index >= 0 &&
                index < tasks.size();
    }

    // =========================================================
    // ENCODE
    // =========================================================

    private String encode(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // =========================================================
    // DECODE
    // =========================================================

    private String decode(
            String value
    ) {

        if (value == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        boolean escaped = false;

        for (int i = 0;
             i < value.length();
             i++) {

            char c =
                    value.charAt(i);

            if (escaped) {

                if (c == 'p') {
                    result.append("|");
                } else if (c == 'n') {
                    result.append("\n");
                } else if (c == 'r') {
                    result.append("\r");
                } else {
                    result.append(c);
                }

                escaped = false;

            } else if (c == '\\') {

                escaped = true;

            } else {

                result.append(c);
            }
        }

        if (escaped) {
            result.append("\\");
        }

        return result.toString();
    }

    // =========================================================
    // TASK MODEL
    // =========================================================

    public static class Task {

        public final String id;

        public final String title;

        public boolean completed;

        public Task(
                String id,
                String title,
                boolean completed
        ) {

            this.id =
                    id == null
                            ? ""
                            : id;

            this.title =
                    title == null
                            ? ""
                            : title;

            this.completed =
                    completed;
        }

        @Override
        public String toString() {

            return
                    (completed
                            ? "✓ "
                            : "○ ")
                            + title;
        }
    }
}