package com.kamal.jarvis;

import android.content.Context;

public class DecisionEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;

    public DecisionEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);
    }

    public String decide(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما وصلني حتى أمر.";
        }

        String input =
                command.trim();

        String lower =
                input.toLowerCase();

        if (isUrgent(lower)) {

            return createDecision(
                    "URGENT",
                    "تنفيذ الأمر مباشرة",
                    input
            );
        }

        if (contains(lower, "تذكير")
                || contains(lower, "ذكرني")
                || contains(lower, "reminder")) {

            return createDecision(
                    "REMINDER",
                    "استخدام Reminder Engine",
                    input
            );
        }

        if (contains(lower, "مهمة")
                || contains(lower, "task")
                || contains(lower, "خدمة")) {

            return createDecision(
                    "TASK",
                    "استخدام Task Manager",
                    input
            );
        }

        if (contains(lower, "تعلم")
                || contains(lower, "تعلمت")
                || contains(lower, "learn")) {

            return createDecision(
                    "LEARNING",
                    "استخدام Learning Engine",
                    input
            );
        }

        if (contains(lower, "خطط")
                || contains(lower, "خطة")
                || contains(lower, "plan")) {

            return createDecision(
                    "PLANNING",
                    "استخدام Planning Engine",
                    input
            );
        }

        if (contains(lower, "شاشة")
                || contains(lower, "screen")) {

            return createDecision(
                    "SCREEN",
                    "استخدام Screen Intelligence",
                    input
            );
        }

        if (contains(lower, "إعدادات")
                || contains(lower, "wifi")
                || contains(lower, "bluetooth")) {

            return createDecision(
                    "ANDROID",
                    "استخدام Android Control Engine",
                    input
            );
        }

        if (contains(lower, "طور")
                || contains(lower, "تطور")
                || contains(lower, "طور نفسك")) {

            return createDecision(
                    "EVOLUTION",
                    "استخدام Evolution Engine",
                    input
            );
        }

        return createDecision(
                "GENERAL",
                "تحليل الأمر ثم اختيار النظام المناسب",
                input
        );
    }

    private boolean isUrgent(
            String input
    ) {

        return contains(input, "عاجل")
                || contains(input, "ضروري")
                || contains(input, "urgent");
    }

    private boolean contains(
            String text,
            String value
    ) {

        return text.contains(value);
    }

    private String createDecision(
            String type,
            String action,
            String command
    ) {

        memoryManager.saveMemory(
                "__last_decision__",
                type + " | " + action
        );

        return
                "JARVIS DECISION ENGINE\n"
                + "============================\n\n"
                + "الأمر:\n"
                + command
                + "\n\n"
                + "التصنيف: "
                + type
                + "\n"
                + "القرار: "
                + action;
    }

    public String getLastDecision() {

        String decision =
                memoryManager.getMemory(
                        "__last_decision__"
                );

        if (decision == null ||
                decision.trim().isEmpty()) {

            return
                    "ما كاين حتى قرار مسجل.";
        }

        return
                "آخر قرار:\n"
                + decision;
    }

    public String getSystemStatus() {

        try {

            int tasks =
                    taskManager.getTaskCount();

            return
                    "Decision Engine: ONLINE ✓\n"
                    + "Known Tasks: "
                    + tasks;

        } catch (Exception e) {

            return
                    "Decision Engine: ERROR ⚠";
        }
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            taskManager.getTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}