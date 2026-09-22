package com.kamal.jarvis;

import android.content.Context;

public class SystemMonitor {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final TaskManager taskManager;
    private final PermissionManager permissionManager;
    private final ActionHistoryManager actionHistoryManager;
    private final ApprovalEngine approvalEngine;

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

        actionHistoryManager =
                new ActionHistoryManager(this.context);

        approvalEngine =
                new ApprovalEngine(this.context);
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

        report.append("\n");

        report.append(
                "Action Records: "
        );

        report.append(
                actionHistoryManager.getHistoryCount()
        );

        report.append("\n\n");

        report.append(
                "PERMISSIONS\n"
        );

        report.append(
                permissionManager
                        .getPermissionStatus()
        );

        report.append("\n");

        report.append(
                "APPROVAL SYSTEM\n"
        );

        report.append(
                approvalEngine.getStatus()
        );

        report.append("\n\n");

        report.append(
                "SUBSYSTEM STATUS\n"
        );

        report.append(
                "Memory Manager: "
        );

        report.append(
                isMemoryHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Skill Manager: "
        );

        report.append(
                isSkillHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Capability Manager: "
        );

        report.append(
                isCapabilityHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Task Manager: "
        );

        report.append(
                taskManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Action History: "
        );

        report.append(
                actionHistoryManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Permissions: "
        );

        report.append(
                permissionManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ATTENTION ⚠"
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

            return
                    isMemoryHealthy()
                    && isSkillHealthy()
                    && isCapabilityHealthy()
                    && taskManager.isHealthy()
                    && actionHistoryManager.isHealthy();

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

        if (isMemoryHealthy()) {
            score += 20;
        }

        if (isSkillHealthy()) {
            score += 20;
        }

        if (isCapabilityHealthy()) {
            score += 20;
        }

        try {

            if (taskManager.isHealthy()) {
                score += 15;
            }

        } catch (Exception ignored) {
        }

        try {

            if (actionHistoryManager.isHealthy()) {
                score += 15;
            }

        } catch (Exception ignored) {
        }

        try {

            if (permissionManager.isHealthy()) {
                score += 10;
            }

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

    public String getSubsystemReport() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS SUBSYSTEMS\n"
        );

        report.append(
                "============================\n\n"
        );

        report.append(
                "Memory: "
        );

        report.append(
                isMemoryHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Skills: "
        );

        report.append(
                isSkillHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Capabilities: "
        );

        report.append(
                isCapabilityHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Tasks: "
        );

        report.append(
                taskManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Action History: "
        );

        report.append(
                actionHistoryManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append("\n");

        report.append(
                "Permissions: "
        );

        report.append(
                permissionManager.isHealthy()
                        ? "ONLINE ✓"
                        : "ATTENTION ⚠"
        );

        return report.toString();
    }

    private boolean isMemoryHealthy() {

        try {

            memoryManager.getMemoryCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    private boolean isSkillHealthy() {

        try {

            skillManager.getSkillCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    private boolean isCapabilityHealthy() {

        try {

            capabilityManager.getCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}