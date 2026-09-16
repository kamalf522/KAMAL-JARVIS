package com.kamal.jarvis;

import android.content.Context;

public class ApprovalEngine {

    private static final String APPROVAL_KEY =
            "__jarvis_approval_mode__";

    private final Context context;
    private final MemoryManager memoryManager;

    private boolean approvalRequired = true;

    public ApprovalEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        loadSettings();
    }

    public synchronized void setApprovalRequired(
            boolean required
    ) {

        approvalRequired = required;

        memoryManager.saveMemory(
                APPROVAL_KEY,
                String.valueOf(required)
        );
    }

    public synchronized boolean isApprovalRequired() {

        return approvalRequired;
    }

    public synchronized String requestApproval(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

            return "ما تحدد حتى إجراء.";
        }

        if (!approvalRequired) {

            return
                    "APPROVAL NOT REQUIRED\n"
                    + "ACTION: "
                    + action;
        }

        return
                "JARVIS APPROVAL REQUIRED\n"
                + "============================\n\n"
                + "الإجراء:\n"
                + action
                + "\n\n"
                + "خاص موافقة المستخدم قبل التنفيذ.";
    }

    public synchronized boolean approve(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

            return false;
        }

        memoryManager.saveMemory(
                "__last_approved_action__",
                action.trim()
        );

        return true;
    }

    public synchronized String getLastApprovedAction() {

        String action =
                memoryManager.getMemory(
                        "__last_approved_action__"
                );

        if (action == null ||
                action.trim().isEmpty()) {

            return
                    "ما كاين حتى إجراء تمت الموافقة عليه.";
        }

        return
                "آخر إجراء تمت الموافقة عليه:\n"
                + action;
    }

    public synchronized void clearApproval() {

        memoryManager.removeMemory(
                "__last_approved_action__"
        );
    }

    public boolean isHealthy() {

        return context != null;
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Approval Engine: ONLINE ✓\n"
                    + "Approval Required: "
                    + (approvalRequired
                    ? "YES"
                    : "NO");

        }

        return
                "Approval Engine: ERROR ⚠";
    }

    private void loadSettings() {

        try {

            String saved =
                    memoryManager.getMemory(
                            APPROVAL_KEY
                    );

            if (saved != null &&
                    !saved.trim().isEmpty()) {

                approvalRequired =
                        Boolean.parseBoolean(
                                saved
                        );
            }

        } catch (Exception ignored) {

            approvalRequired = true;
        }
    }
}