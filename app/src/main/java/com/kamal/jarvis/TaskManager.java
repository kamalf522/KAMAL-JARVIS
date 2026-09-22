package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

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
                +