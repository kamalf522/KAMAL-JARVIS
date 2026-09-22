package com.kamal.jarvis;

import android.content.Context;

public class ApprovalEngine {

    private static final String APPROVAL_KEY =
            "__jarvis_approval_mode__";

    private static final String LAST_APPROVED_KEY =
            "__last_approved_action__";

    private static final String PENDING_KEY =
            "__pending_approval_action__";

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

    // =========================================================
    // SETTINGS
    // =========================================================

    public synchronized void setApprovalRequired(
            boolean required
    ) {

        approvalRequired = required;

        memoryManager.saveMemory(
                APPROVAL_KEY,
                String.valueOf(required)
        );

        if (!required) {
            clearApproval();
        }
    }

    public synchronized boolean isApprovalRequired() {

        return approvalRequired;
    }

    // =========================================================
    // REQUEST APPROVAL
    // =========================================================

    public synchronized String requestApproval(
            String action
    ) {

        if (isBlank(action)) {

            return
                    "ما تحدد حتى إجراء.";
        }

        String cleanAction =
                action.trim();

        if (!approvalRequired) {

            memoryManager.saveMemory(
                    LAST_APPROVED_KEY,
                    cleanAction
            );

            memoryManager.removeMemory(
                    PENDING_KEY
            );

            return
                    "APPROVAL NOT REQUIRED\n"
                    + "ACTION: "
                    + cleanAction;
        }

        memoryManager.saveMemory(
                PENDING_KEY,
                cleanAction
        );

        return
                "JARVIS APPROVAL REQUIRED\n"
                + "============================\n\n"
                + "الإجراء:\n"
                + cleanAction
                + "\n\n"
                + "خاص موافقة المستخدم قبل التنفيذ.";
    }

    // =========================================================
    // APPROVE
    // =========================================================

    public synchronized boolean approve(
            String action
    ) {

        if (isBlank(action)) {

            action =
                    memoryManager.getMemory(
                            PENDING_KEY
                    );
        }

        if (isBlank(action)) {

            return false;
        }

        String cleanAction =
                action.trim();

        memoryManager.saveMemory(
                LAST_APPROVED_KEY,
                cleanAction
        );

        memoryManager.removeMemory(
                PENDING_KEY
        );

        return true;
    }

    // =========================================================
    // APPROVE PENDING
    // =========================================================

    public synchronized boolean approvePending() {

        String pending =
                memoryManager.getMemory(
                        PENDING_KEY
                );

        if (isBlank(pending)) {

            return false;
        }

        return approve(pending);
    }

    // =========================================================
    // CHECK APPROVAL
    // =========================================================

    public synchronized boolean isApproved(
            String action
    ) {

        if (isBlank(action)) {

            return false;
        }

        if (!approvalRequired) {

            return true;
        }

        String approved =
                memoryManager.getMemory(
                        LAST_APPROVED_KEY
                );

        if (isBlank(approved)) {

            return false;
        }

        return normalize(approved)
                .equals(
                        normalize(action)
                );
    }

    // =========================================================
    // PENDING ACTION
    // =========================================================

    public synchronized String getPendingAction() {

        String action =
                memoryManager.getMemory(
                        PENDING_KEY
                );

        if (isBlank(action)) {

            return
                    "ما كاين حتى إجراء كيتسنى الموافقة.";
        }

        return
                "الإجراء المعلق:\n"
                + action;
    }

    public synchronized boolean hasPendingApproval() {

        String action =
                memoryManager.getMemory(
                        PENDING_KEY
                );

        return !isBlank(action);
    }

    // =========================================================
    // LAST APPROVAL
    // =========================================================

    public synchronized String getLastApprovedAction() {

        String action =
                memoryManager.getMemory(
                        LAST_APPROVED_KEY
                );

        if (isBlank(action)) {

            return
                    "ما كاين حتى إجراء تمت الموافقة عليه.";
        }

        return
                "آخر إجراء تمت الموافقة عليه:\n"
                + action;
    }

    // =========================================================
    // CLEAR
    // =========================================================

    public synchronized void clearApproval() {

        memoryManager.removeMemory(
                LAST_APPROVED_KEY
        );

        memoryManager.removeMemory(
                PENDING_KEY
        );
    }

    public synchronized void clearPendingApproval() {

        memoryManager.removeMemory(
                PENDING_KEY
        );
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            return context != null
                    && memoryManager != null;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        if (!isHealthy()) {

            return
                    "Approval Engine: ERROR ⚠";
        }

        boolean pending =
                hasPendingApproval();

        return
                "Approval Engine: ONLINE ✓\n"
                + "Approval Required: "
                + (approvalRequired
                ? "YES"
                : "NO")
                + "\n"
                + "Pending Approval: "
                + (pending
                ? "YES"
                : "NO");
    }

    // =========================================================
    // SETTINGS LOAD
    // =========================================================

    private void loadSettings() {

        try {

            String saved =
                    memoryManager.getMemory(
                            APPROVAL_KEY
                    );

            if (!isBlank(saved)) {

                approvalRequired =
                        Boolean.parseBoolean(
                                saved.trim()
                        );
            }

        } catch (Exception ignored) {

            approvalRequired = true;
        }
    }

    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String normalize(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }
}