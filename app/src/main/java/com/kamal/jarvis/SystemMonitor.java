package com.kamal.jarvis;

import android.content.Context;

public class SystemMonitor {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final TaskManager taskManager;
    private final PermissionManager permissionManager;

    public SystemMonitor(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        taskManager =
                new TaskManager(this.context);

        permissionManager =
                new PermissionManager(this.context);
    }

    public String getFullStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS SYSTEM MONITOR\n"
        );

        report.append(
                "============================\n\n"
        );

        report.append(
                "CORE SYSTEMS\n"
        );

        report.append(
                "Memory: "
        );

        report.append(
                memoryManager.getMemoryCount()
        );

        report.append("\n");

        report.append(
                "Skills: "
        );

        report.append(
                skillManager.getSkillCount()
        );

        report.append("\n");

        report.append(
                "Capabilities: "
        );

        report.append(
                capabilityManager.getCount()
        );

        report.append("\n");

        report.append(
                "Tasks: "
        );

        report.append(
                taskManager.getTaskCount()
        );

        report.append("\n");

        report.append(
                "Pending Tasks: "
        );

        report.append(
                taskManager.getPendingTaskCount()
        );

        report.append("\n");

        report.append(
                "Completed Tasks: "
        );

        report.append(
                taskManager.getCompletedTaskCount()
        );

        report.append("\n\n");

        report.append(
                "PERMISSIONS\n"
        );

        report.append(
                permissionManager
                        .getPermissionStatus()
        );

        report.append("\n\n");

        report.append(
                "SYSTEM STATUS\n"
        );

        if (isHealthy()) {

            report.append(
                    "JARVIS CORE: HEALTHY ✓"
            );

        } else {

            report.append(
                    "JARVIS CORE: ATTENTION REQUIRED ⚠"
            );
        }

        return report.toString();
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            skillManager.getSkillCount();

            capabilityManager.getCount();

            taskManager.getTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "System Monitor: ONLINE ✓";

        }

        return
                "System Monitor: ISSUES DETECTED ⚠";
    }

    public int getHealthScore() {

        int score = 0;

        try {

            memoryManager.getMemoryCount();

            score += 25;

        } catch (Exception ignored) {
        }

        try {

            skillManager.getSkillCount();

            score += 25;

        } catch (Exception ignored) {
        }

        try {

            if (capabilityManager.getCount() > 0) {

                score += 25;
            }

        } catch (Exception ignored) {
        }

        try {

            taskManager.getTaskCount();

            score += 25;

        } catch (Exception ignored) {
        }

        return score;
    }

    public String getHealthReport() {

        return
                "JARVIS HEALTH\n"
                + "============================\n\n"
                + "Health Score: "
                + getHealthScore()
                + "/100\n\n"
                + getStatus();
    }
}