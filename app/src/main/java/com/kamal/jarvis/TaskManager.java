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
                new MemoryManager(this.context);

        loadTasks();
    }

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

    public synchronized String completeTask(
            int index
    ) {

        if (index < 0 ||
                index >= tasks.size()) {

            return "رقم المهمة غير صحيح.";
        }

        Task task =
                tasks.get(index);

        task.completed = true;

        saveTasks();

        return
                "تم إنجاز المهمة ✓\n\n"
                + task.title;
    }

    public synchronized String removeTask(
            int index
    ) {

        if (index < 0 ||
                index >= tasks.size()) {

            return "رقم المهمة غير صحيح.";
        }

        Task removed =
                tasks.remove(index);

        saveTasks();

        return
                "تم حذف المهمة ✓\n\n"
                + removed.title;
    }

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

            result.append(i + 1);
            result.append(". ");

            if (task.completed) {

                result.append("✓ ");

            } else {

                result.append("○ ");
            }

            result.append(task.title);
            result.append("\n");
        }

        return result.toString();
    }

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

    public synchronized void clearCompletedTasks() {

        for (int i = tasks.size() - 1;
             i >= 0;
             i--) {

            if (tasks.get(i).completed) {

                tasks.remove(i);
            }
        }

        saveTasks();
    }

    public synchronized void clearAllTasks() {

        tasks.clear();

        saveTasks();
    }

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

    private void saveTasks() {

        StringBuilder data =
                new StringBuilder();

        for (Task task : tasks) {

            data.append(task.id);
            data.append("|");

            data.append(
                    escape(task.title)
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
                    data.split("\n");

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
                        unescape(parts[1]);

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

    private String escape(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n");
    }

    private String unescape(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace("\\n", "\n")
                .replace("\\p", "|")
                .replace("\\\\", "\\");
    }

    private static class Task {

        final long id;
        final String title;
        boolean completed;

        Task(
                long id,
                String title,
                boolean completed
        ) {

            this.id = id;
            this.title = title;
            this.completed = completed;
        }
    }
}