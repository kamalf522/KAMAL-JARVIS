package com.kamal.jarvis;

import android.content.Context;

import java.util.Locale;

/**
 * OwnerControlCore
 *
 * المركز المسؤول عن هوية مالك JARVIS وقواعد التحكم الأساسية.
 *
 * المبدأ:
 * - المالك الأساسي: KAMAL
 * - لا يمكن تغيير المالك من خلال أمر عادي.
 * - التطور الذاتي لا يملك صلاحية تغيير هوية المالك.
 * - أي نظام جديد داخل JARVIS يمكنه الاستعلام عن حالة المالك
 *   قبل تنفيذ عمليات حساسة.
 *
 * ملاحظة:
 * هذا الملف لا يحاول تجاوز حماية Android أو صلاحيات النظام.
 */
public class OwnerControlCore {

    private static final String OWNER_NAME = "KAMAL";
    private static final String OWNER_ID = "KAMAL_PRIMARY_OWNER";

    private final Context context;
    private final MemoryManager memoryManager;

    private String lastVerifiedOwner = "";
    private String lastAuthorizedAction = "";
    private long authorizationCount = 0L;

    public OwnerControlCore(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "OwnerControlCore context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        this.memoryManager =
                new MemoryManager(this.context);

        loadState();
    }

    // =========================================================
    // OWNER IDENTITY
    // =========================================================

    /**
     * الاسم الأساسي للمالك.
     */
    public synchronized String getOwnerName() {
        return OWNER_NAME;
    }

    /**
     * المعرف الداخلي الثابت للمالك.
     */
    public synchronized String getOwnerId() {
        return OWNER_ID;
    }

    /**
     * التحقق من أن الاسم يشير إلى المالك الأساسي.
     */
    public synchronized boolean isOwner(String identity) {

        if (isBlank(identity)) {
            return false;
        }

        String normalized =
                normalize(identity);

        return normalized.equals(
                normalize(OWNER_NAME)
        )
                || normalized.equals(
                normalize(OWNER_ID)
        );
    }

    /**
     * لا توجد وظيفة عامة لتغيير المالك.
     *
     * هوية المالك جزء ثابت من نواة التحكم.
     */
    public synchronized boolean canChangeOwner() {
        return false;
    }

    /**
     * محاولة تغيير المالك مرفوضة دائماً.
     *
     * وجود هذه الدالة يمنع الأنظمة الأخرى من افتراض
     * أن تغيير المالك عملية عادية متاحة.
     */
    public synchronized String changeOwner(
            String requestedOwner
    ) {

        return
                "OWNER LOCK ACTIVE\n"
                + "المالك الأساسي هو: "
                + OWNER_NAME
                + "\n"
                + "تغيير المالك غير متاح.";
    }

    // =========================================================
    // OWNER AUTHORIZATION
    // =========================================================

    /**
     * التحقق من هوية المالك قبل تنفيذ إجراء.
     */
    public synchronized boolean authorizeOwner(
            String identity,
            String action
    ) {

        if (!isOwner(identity)) {
            recordDeniedAction(
                    identity,
                    action
            );
            return false;
        }

        if (isBlank(action)) {
            return false;
        }

        lastVerifiedOwner =
                OWNER_NAME;

        lastAuthorizedAction =
                action.trim();

        authorizationCount++;

        saveState();

        return true;
    }

    /**
     * نسخة مختصرة عندما يكون النظام نفسه قد تحقق
     * من هوية المالك مسبقاً.
     */
    public synchronized boolean authorizeAction(
            String action
    ) {

        if (isBlank(action)) {
            return false;
        }

        lastVerifiedOwner =
                OWNER_NAME;

        lastAuthorizedAction =
                action.trim();

        authorizationCount++;

        saveState();

        return true;
    }

    // =========================================================
    // EVOLUTION CONTROL
    // =========================================================

    /**
     * هل يستطيع Evolution Engine تغيير هوية المالك؟
     */
    public synchronized boolean evolutionCanChangeOwner() {
        return false;
    }

    /**
     * هل يستطيع التطور الذاتي إزالة Owner Control؟
     */
    public synchronized boolean evolutionCanDisableOwnerControl() {
        return false;
    }

    /**
     * هل يستطيع التطور الذاتي إنشاء مالك جديد؟
     */
    public synchronized boolean evolutionCanCreateNewOwner() {
        return false;
    }

    /**
     * التطور مسموح عندما يكون هدفه تحسين خدمة المالك
     * وليس تغيير علاقة المالك بالنظام.
     */
    public synchronized boolean isEvolutionAllowed(
            String goal
    ) {

        if (isBlank(goal)) {
            return false;
        }

        String clean =
                normalize(goal);

        /*
         * التطور الطبيعي مسموح.
         * لكن أي هدف صريح لتغيير المالك أو تعطيل
         * Owner Control يتم رفضه.
         */

        if (containsAny(
                clean,
                "change owner",
                "remove owner",
                "delete owner",
                "disable owner",
                "replace owner",
                "تغيير المالك",
                "حذف المالك",
                "تعطيل المالك",
                "استبدال المالك",
                "الغاء المالك",
                "إلغاء المالك"
        )) {
            return false;
        }

        return true;
    }

    // =========================================================
    // COMMAND CONTROL
    // =========================================================

    /**
     * تحديد ما إذا كان الأمر يحاول تغيير هوية
     * أو سيطرة المالك.
     */
    public synchronized boolean isOwnerControlCommand(
            String command
    ) {

        if (isBlank(command)) {
            return false;
        }

        String clean =
                normalize(command);

        return containsAny(
                clean,
                "change owner",
                "new owner",
                "replace owner",
                "remove owner",
                "delete owner",
                "disable owner",
                "تغيير المالك",
                "مالك جديد",
                "بدل المالك",
                "بدل سيدك",
                "غير المالك",
                "حيد المالك",
                "حذف المالك",
                "تعطيل المالك",
                "الغاء المالك",
                "إلغاء المالك"
        );
    }

    /**
     * يمنع تغيير المالك من خلال أمر نصي عادي.
     */
    public synchronized boolean isCommandAllowed(
            String command
    ) {

        if (isBlank(command)) {
            return false;
        }

        if (isOwnerControlCommand(command)) {
            return false;
        }

        return true;
    }

    // =========================================================
    // PRIORITY
    // =========================================================

    /**
     * أولوية المالك داخل JARVIS.
     *
     * القيمة الأعلى تعني أن هذا المستوى يجب أن يعامل
     * كمرجع أعلى من تعليمات التطور الداخلية.
     */
    public synchronized int getOwnerPriority() {
        return 100;
    }

    /**
     * أولوية التطور الذاتي.
     */
    public synchronized int getEvolutionPriority() {
        return 50;
    }

    /**
     * هل المالك أعلى من Evolution Engine؟
     */
    public synchronized boolean isOwnerAboveEvolution() {
        return getOwnerPriority()
                > getEvolutionPriority();
    }

    // =========================================================
    // STATE
    // =========================================================

    public synchronized String getLastVerifiedOwner() {

        if (isBlank(lastVerifiedOwner)) {
            return "لا توجد عملية تحقق سابقة.";
        }

        return lastVerifiedOwner;
    }

    public synchronized String getLastAuthorizedAction() {

        if (isBlank(lastAuthorizedAction)) {
            return "لا يوجد إجراء مصادق عليه.";
        }

        return lastAuthorizedAction;
    }

    public synchronized long getAuthorizationCount() {
        return authorizationCount;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS OWNER CONTROL\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "Owner: "
        ).append(
                OWNER_NAME
        ).append("\n");

        result.append(
                "Owner ID: "
        ).append(
                OWNER_ID
        ).append("\n");

        result.append(
                "Owner Lock: ACTIVE\n"
        );

        result.append(
                "Owner Change: BLOCKED\n"
        );

        result.append(
                "Evolution Owner Change: BLOCKED\n"
        );

        result.append(
                "Owner Priority: "
        ).append(
                getOwnerPriority()
        ).append("\n");

        result.append(
                "Evolution Priority: "
        ).append(
                getEvolutionPriority()
        ).append("\n");

        result.append(
                "Authorizations: "
        ).append(
                authorizationCount
        );

        return result.toString();
    }

    public boolean isHealthy() {

        try {

            return context != null
                    && memoryManager != null
                    && OWNER_NAME.length() > 0
                    && OWNER_ID.length() > 0
                    && getOwnerPriority()
                    > getEvolutionPriority();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // PERSISTENCE
    // =========================================================

    private void loadState() {

        try {

            String savedOwner =
                    memoryManager.getMemory(
                            "__jarvis_last_verified_owner__"
                    );

            if (!isBlank(savedOwner)) {
                lastVerifiedOwner =
                        savedOwner;
            }

            String savedAction =
                    memoryManager.getMemory(
                            "__jarvis_last_authorized_action__"
                    );

            if (!isBlank(savedAction)) {
                lastAuthorizedAction =
                        savedAction;
            }

            String count =
                    memoryManager.getMemory(
                            "__jarvis_owner_authorization_count__"
                    );

            if (!isBlank(count)) {

                try {

                    authorizationCount =
                            Long.parseLong(
                                    count.trim()
                            );

                } catch (Exception ignored) {

                    authorizationCount = 0L;
                }
            }

        } catch (Exception ignored) {
        }
    }

    private void saveState() {

        try {

            memoryManager.saveMemory(
                    "__jarvis_last_verified_owner__",
                    OWNER_NAME
            );

            memoryManager.saveMemory(
                    "__jarvis_last_authorized_action__",
                    lastAuthorizedAction
            );

            memoryManager.saveMemory(
                    "__jarvis_owner_authorization_count__",
                    String.valueOf(
                            authorizationCount
                    )
            );

        } catch (Exception ignored) {
        }
    }

    private void recordDeniedAction(
            String identity,
            String action
    ) {

        try {

            String safeIdentity =
                    isBlank(identity)
                            ? "UNKNOWN"
                            : identity.trim();

            String safeAction =
                    isBlank(action)
                            ? "UNKNOWN"
                            : action.trim();

            memoryManager.saveMemory(
                    "__jarvis_last_denied_owner_action__",
                    safeIdentity
                            + " | "
                            + safeAction
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean containsAny(
            String value,
            String... terms
    ) {

        if (isBlank(value)
                || terms == null) {

            return false;
        }

        String normalized =
                normalize(value);

        for (String term : terms) {

            if (isBlank(term)) {
                continue;
            }

            if (normalized.contains(
                    normalize(term)
            )) {
                return true;
            }
        }

        return false;
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}