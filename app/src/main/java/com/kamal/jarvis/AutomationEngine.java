package com.kamal.jarvis;

import android.content.Context;

public class AutomationEngine {

    private final Context context;

    private final DecisionEngine decisionEngine;
    private final TaskManager taskManager;
    private final ReminderEngine reminderEngine;
    private final AndroidControlEngine androidControlEngine;
    private final ContextEngine contextEngine;

    public AutomationEngine(Context context) {

        this.context =
                context.getApplicationContext();

        decisionEngine =
                new DecisionEngine(this.context);

        taskManager =
                new TaskManager(this.context);

        reminderEngine =
                new ReminderEngine(this.context);

        androidControlEngine =
                new AndroidControlEngine(this.context);

        contextEngine =
                new ContextEngine(this.context);
    }

    public String analyzeCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        String decision =
                decisionEngine.decide(
                        cleanCommand
                );

        contextEngine.updateCommand(
                cleanCommand,
                decision
        );

        contextEngine.updateDecision(
                decision
        );

        return
                "تم تحليل الأمر ✓\n\n"
                + "الأمر:\n"
                + cleanCommand
                + "\n\n"
                + "نوع العملية:\n"
                + decision;
    }

    public String createTask(
            String task
    ) {

        if (task == null ||
                task.trim().isEmpty()) {

            return "حدد المهمة اللي بغيتي نضيف.";
        }

        String result =
                taskManager.addTask(
                        task.trim()
                );

        contextEngine.updateGoal(
                task.trim()
        );

        return result;
    }

    public String completeTask(
            int index
    ) {

        return taskManager.completeTask(
                index
        );
    }

    public String removeTask(
            int index
    ) {

        return taskManager.removeTask(
                index
        );
    }

    public String getTasks() {

        return taskManager.getTasks();
    }

    public String clearCompletedTasks() {

        return taskManager.clearCompleted();
    }

    public String clearAllTasks() {

        return taskManager.clearAll();
    }

    public String createPlan(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "حدد الهدف الأول.";
        }

        contextEngine.updateGoal(
                goal.trim()
        );

        return contextEngine.createContextPlan();
    }

    public String createReminder(
            String title,
            long triggerTime
    ) {

        if (title == null ||
                title.trim().isEmpty()) {

            return "خاصك تحدد اسم التذكير.";
        }

        return reminderEngine.createReminder(
                title.trim(),
                triggerTime
        );
    }

    public String getLastReminder() {

        return reminderEngine.getLastReminder();
    }

    public String openWifi() {

        return androidControlEngine.openWifiSettings();
    }

    public String openBluetooth() {

        return androidControlEngine.openBluetoothSettings();
    }

    public String openDeviceInfo() {

        return androidControlEngine.openDeviceInfo();
    }

    public String openAppSettings() {

        return androidControlEngine.openAppSettings();
    }

    public String openNotificationSettings() {

        return androidControlEngine
                .openNotificationSettings();
    }

    public String openDisplaySettings() {

        return androidControlEngine
                .openDisplaySettings();
    }

    public String openSoundSettings() {

        return androidControlEngine
                .openSoundSettings();
    }

    public String openBatterySettings() {

        return androidControlEngine
                .openBatterySettings();
    }

    public String getAutomationStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "=== AUTOMATION ENGINE ===\n\n"
        );

        report.append(
                "Decision Engine: "
        );

        report.append(
                safe(decisionEngine.getStatus())
        );

        report.append("\n");

        report.append(
                "Task Manager: "
        );

        report.append(
                safe(taskManager.getStatus())
        );

        report.append("\n");

        report.append(
                "Reminder Engine: "
        );

        report.append(
                safe(reminderEngine.getStatus())
        );

        report.append("\n");

        report.append(
                "Android Control: "
        );

        report.append(
                safe(androidControlEngine.getStatus())
        );

        report.append("\n");

        report.append(
                "Context Engine: "
        );

        report.append(
                safe(contextEngine.getStatus())
        );

        return report.toString();
    }

    public String execute(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        String analysis =
                decisionEngine.decide(
                        cleanCommand
                );

        contextEngine.updateCommand(
                cleanCommand,
                analysis
        );

        contextEngine.updateDecision(
                analysis
        );

        return
                "تم استقبال الأمر ✓\n\n"
                + "التصنيف:\n"
                + analysis
                + "\n\n"
                + "الأمر جاهز للتنفيذ من خلال النظام المناسب.";
    }

    public boolean isHealthy() {

        try {

            decisionEngine.getStatus();
            taskManager.getStatus();
            reminderEngine.getStatus();
            androidControlEngine.getStatus();
            contextEngine.getStatus();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Automation Engine: ONLINE ✓";
        }

        return
                "Automation Engine: ERROR ⚠";
    }

    private String safe(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value;
    }
}