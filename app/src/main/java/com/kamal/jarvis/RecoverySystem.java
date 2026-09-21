package com.kamal.jarvis;

import android.content.Context;

public class RecoverySystem {

    private final Context context;
    private final MemoryManager memoryManager;
    private final CapabilityManager capabilityManager;

    public RecoverySystem(Context context) {

        this.context = context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);
    }

    public String runRecovery() {

        StringBuilder report =
                new StringBuilder();

        int repaired = 0;
        int checked = 0;

        report.append("JARVIS RECOVERY SYSTEM\n");
        report.append("============================\n\n");

        checked++;

        try {

            memoryManager.getMemoryCount();

            report.append(
                    "✓ MEMORY SYSTEM: ONLINE\n"
            );

            repaired++;

        } catch (Exception e) {

            report.append(
                    "✗ MEMORY SYSTEM: ERROR\n"
            );
        }

        checked++;

        try {

            int count =
                    capabilityManager.getCount();

            if (count > 0) {

                report.append(
                        "✓ CAPABILITY SYSTEM: ONLINE\n"
                );

                repaired++;

            } else {

                report.append(
                        "⚠ CAPABILITY SYSTEM: EMPTY\n"
                );
            }

        } catch (Exception e) {

            report.append(
                    "✗ CAPABILITY SYSTEM: ERROR\n"
            );
        }

        report.append("\n");
        report.append("============================\n");

        report.append(
                "SYSTEMS CHECKED: "
        );

        report.append(checked);

        report.append("\n");

        report.append(
                "SYSTEMS ONLINE: "
        );

        report.append(repaired);

        report.append("\n\n");

        if (repaired == checked) {

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

    /**
     * تسجيل أي خطأ كيقدر JARVIS يرجع ليه
     * من بعد أثناء التشخيص والتطور الذاتي.
     */
    public void recordFailure(
            String source,
            String message
    ) {

        try {

            String cleanSource =
                    source == null ||
                            source.trim().isEmpty()
                            ? "unknown"
                            : source.trim();

            String cleanMessage =
                    message == null ||
                            message.trim().isEmpty()
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

        } catch (Exception ignored) {
            // نظام الاسترجاع ما خاصوش يطيح بسبب تسجيل الخطأ
        }
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            int capabilities =
                    capabilityManager.getCount();

            return capabilities > 0;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return "Recovery System: ONLINE ✓";

        }

        return "Recovery System: NEEDS ATTENTION ⚠";
    }

    public String repairMemory() {

        try {

            memoryManager.getMemoryCount();

            return
                    "Memory system checked successfully ✓";

        } catch (Exception e) {

            return
                    "Memory system recovery failed ✗";
        }
    }

    public String repairCapabilities() {

        try {

            int count =
                    capabilityManager.getCount();

            if (count > 0) {

                return
                        "Capability system checked successfully ✓";
            }

            return
                    "Capability registry is empty ⚠";

        } catch (Exception e) {

            return
                    "Capability system recovery failed ✗";
        }
    }

    public String getRecoveryReport() {

        return runRecovery();
    }
}