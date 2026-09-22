package com.kamal.jarvis;

import android.content.Context;

public class RecoverySystem {

private final Context context;

private final MemoryManager memoryManager;
private final CapabilityManager capabilityManager;
private final SkillManager skillManager;
private final TaskManager taskManager;
private final SystemMonitor systemMonitor;
private final ActionHistoryManager actionHistoryManager;

public RecoverySystem(Context context) {

    this.context =
            context.getApplicationContext();

    memoryManager =
            new MemoryManager(this.context);

    capabilityManager =
            new CapabilityManager(this.context);

    skillManager =
            new SkillManager(this.context);

    taskManager =
            new TaskManager(this.context);

    systemMonitor =
            new SystemMonitor(this.context);

    actionHistoryManager =
            new ActionHistoryManager(this.context);
}

public String runRecovery() {

    StringBuilder report =
            new StringBuilder();

    int checked = 0;
    int healthy = 0;
    int repaired = 0;

    report.append(
            "JARVIS RECOVERY SYSTEM\n"
    );

    report.append(
            "============================\n\n"
    );

    checked++;

    try {

        memoryManager.getMemoryCount();

        report.append(
                "✓ MEMORY SYSTEM: ONLINE\n"
        );

        healthy++;

    } catch (Exception e) {

        report.append(
                "✗ MEMORY SYSTEM: ERROR\n"
        );

        recordFailure(
                "MemoryManager",
                safeError(e)
        );
    }

    checked++;

    try {

        int count =
                capabilityManager.getCount();

        if (count >= 0) {

            report.append(
                    "✓ CAPABILITY SYSTEM: ONLINE\n"
            );

            healthy++;

        } else {

            report.append(
                    "⚠ CAPABILITY SYSTEM: INVALID\n"
            );
        }

    } catch (Exception e) {

        report.append(
                "✗ CAPABILITY SYSTEM: ERROR\n"
        );

        recordFailure(
                "CapabilityManager",
                safeError(e)
        );
    }

    checked++;

    try {

        int count =
                skillManager.getSkillCount();

        if (count >= 0) {

            report.append(
                    "✓ SKILL SYSTEM: ONLINE\n"
            );

            healthy++;

        } else {

            report.append(
                    "⚠ SKILL SYSTEM: INVALID\n"
            );
        }

    } catch (Exception e) {

        report.append(
                "✗ SKILL SYSTEM: ERROR\n"
        );

        recordFailure(
                "SkillManager",
                safeError(e)
        );
    }

    checked++;

    try {

        if (taskManager.isHealthy()) {

            report.append(
                    "✓ TASK SYSTEM: ONLINE\n"
            );

            healthy++;

        } else {

            report.append(
                    "⚠ TASK SYSTEM: ATTENTION\n"
            );
        }

    } catch (Exception e) {

        report.append(
                "✗ TASK SYSTEM: ERROR\n"
        );

        recordFailure(
                "TaskManager",
                safeError(e)
        );
    }

    checked++;

    try {

        if (systemMonitor.isHealthy()) {

            report.append(
                    "✓ SYSTEM MONITOR: ONLINE\n"
            );

            healthy++;

        } else {

            report.append(
                    "⚠ SYSTEM MONITOR: ATTENTION\n"
            );
        }

    } catch (Exception e) {

        report.append(
                "✗ SYSTEM MONITOR: ERROR\n"
        );

        recordFailure(
                "SystemMonitor",
                safeError(e)
        );
    }

    report.append("\n");

    report.append(
            "SYSTEMS CHECKED: "
    );

    report.append(checked);

    report.append("\n");

    report.append(
            "SYSTEMS HEALTHY: "
    );

    report.append(healthy);

    report.append("\n");

    report.append(
            "RECOVERY ACTIONS: "
    );

    report.append(repaired);

    report.append("\n\n");

    if (healthy == checked) {

        report.append(
                "RECOVERY STATUS: HEALTHY ✓"
        );

    } else {

        report.append(
                "RECOVERY STATUS: PARTIAL ⚠"
        );
    }

    return report.toString();
}

public void recordFailure(
        String source,
        String message
) {

    try {

        String cleanSource =
                source == null
                        || source.trim().isEmpty()
                        ? "unknown"
                        : source.trim();

        String cleanMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "unknown error"
                        : message.trim();

        String key =
                "__recovery_failure__"
                        + System.currentTimeMillis();

        String value =
                "SOURCE="
                        + cleanSource
                        + "\nERROR="
                        + cleanMessage
                        + "\nTIME="
                        + System.currentTimeMillis();

        memoryManager.saveMemory(
                key,
                value
        );

        actionHistoryManager.recordAction(
                "RECOVERY_FAILURE",
                cleanSource
                        + ": "
                        + cleanMessage
        );

    } catch (Exception ignored) {
    }
}

public boolean isHealthy() {

    try {

        return
                memoryManager.getMemoryCount() >= 0
                && capabilityManager.getCount() >= 0
                && skillManager.getSkillCount() >= 0
                && taskManager.isHealthy();

    } catch (Exception e) {

        return false;
    }
}

public String getStatus() {

    if (isHealthy()) {

        return
                "Recovery System: ONLINE ✓";
    }

    return
            "Recovery System: NEEDS ATTENTION ⚠";
}

public String repairMemory() {

    try {

        memoryManager.getMemoryCount();

        return
                "Memory system checked successfully ✓";

    } catch (Exception e) {

        recordFailure(
                "MemoryRecovery",
                safeError(e)
        );

        return
                "Memory system recovery failed ✗";
    }
}

public String repairCapabilities() {

    try {

        int count =
                capabilityManager.getCount();

        if (count >= 0) {

            return
                    "Capability system checked successfully ✓";
        }

        return
                "Capability system is invalid ⚠";

    } catch (Exception e) {

        recordFailure(
                "CapabilityRecovery",
                safeError(e)
        );

        return
                "Capability system recovery failed ✗";
    }
}

public String repairSkills() {

    try {

        int count =
                skillManager.getSkillCount();

        if (count >= 0) {

            return
                    "Skill system checked successfully ✓";
        }

        return
                "Skill system is invalid ⚠";

    } catch (Exception e) {

        recordFailure(
                "SkillRecovery",
                safeError(e)
        );

        return
                "Skill system recovery failed ✗";
    }
}

public String repairTasks() {

    try {

        if (taskManager.isHealthy()) {

            return
                    "Task system checked successfully ✓";
        }

        return
                "Task system needs attention ⚠";

    } catch (Exception e) {

        recordFailure(
                "TaskRecovery",
                safeError(e)
        );

        return
                "Task system recovery failed ✗";
    }
}

public String getRecoveryReport() {

    return runRecovery();
}

public String getFailureHistory() {

    try {

        return
                memoryManager
                        .searchMemory(
                                "__recovery_failure__"
                        );

    } catch (Exception e) {

        return
                "Recovery failure history unavailable.";
    }
}

private String safeError(
        Exception e
) {

    if (e == null) {

        return "Unknown error";
    }

    String message =
            e.getMessage();

    if (message == null
            || message.trim().isEmpty()) {

        return e.getClass()
                .getSimpleName();
    }

    return message;
}

}