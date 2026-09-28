package com.kamal.jarvis;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * =========================================================
 * JARVIS OWNER SECURITY CORE
 * =========================================================
 *
 * طبقة الحماية المركزية الخاصة بـ JARVIS.
 *
 * المسؤوليات:
 *
 * 1. حماية هوية المالك.
 * 2. إنشاء جلسة مالك موثوقة بعد التحقق.
 * 3. إعطاء صلاحيات مؤقتة للأوامر الحساسة.
 * 4. منع Evolution / Self Builder / Code Evolution
 *    من تغيير المالك أو تعطيل طبقة الحماية.
 * 5. تصنيف العمليات حسب مستوى الخطورة.
 * 6. تسجيل العمليات المسموحة والمرفوضة.
 * 7. العمل بطريقة Fail-Closed:
 *    إذا وقع خطأ في التحقق -> العملية مرفوضة.
 *
 * ملاحظة مهمة:
 *
 * هذا الملف يمثل طبقة Authorization داخل التطبيق.
 * التحقق من النص "KAMAL" وحده ليس بديلاً عن
 * Android BiometricPrompt أو PIN حقيقي.
 *
 * لذلك هذه الطبقة مصممة بحيث يمكن لاحقاً ربطها
 * ببصمة الهاتف / PIN / Credential Manager بدون
 * تغيير بنية الحماية الأساسية.
 *
 * =========================================================
 */
public class OwnerSecurityCore {

    // =========================================================
    // SECURITY VERSION
    // =========================================================

    private static final String SECURITY_VERSION =
            "1.0-FINAL";

    // =========================================================
    // SESSION SETTINGS
    // =========================================================

    /**
     * مدة جلسة المالك:
     * 15 دقيقة.
     */
    private static final long OWNER_SESSION_DURATION_MS =
            15L * 60L * 1000L;

    /**
     * أقصى عدد لمحاولات التحقق الفاشلة
     * قبل تفعيل Lock مؤقت.
     */
    private static final int MAX_FAILED_ATTEMPTS =
            5;

    /**
     * مدة Lock المؤقت:
     * 60 ثانية.
     */
    private static final long LOCK_DURATION_MS =
            60L * 1000L;

    // =========================================================
    // SECURITY LEVELS
    // =========================================================

    public static final int LEVEL_PUBLIC = 0;

    public static final int LEVEL_NORMAL = 10;

    public static final int LEVEL_SENSITIVE = 50;

    public static final int LEVEL_CRITICAL = 80;

    public static final int LEVEL_OWNER_ONLY = 100;

    // =========================================================
    // CONTEXT
    // =========================================================

    private final Context context;

    private final OwnerControlCore ownerControlCore;

    private final MemoryManager memoryManager;

    private final SecureRandom secureRandom;

    // =========================================================
    // SESSION STATE
    // =========================================================

    /**
     * لا يتم تخزين الـ token الخام في MemoryManager.
     *
     * يتم تخزين Hash فقط.
     */
    private String activeSessionHash = "";

    private long sessionCreatedAt = 0L;

    private long sessionExpiresAt = 0L;

    private boolean sessionActive = false;

    // =========================================================
    // SECURITY STATE
    // =========================================================

    private int failedAttempts = 0;

    private long lockUntil = 0L;

    private long authorizedOperations = 0L;

    private long deniedOperations = 0L;

    private String lastAuthorizedAction = "";

    private String lastDeniedAction = "";

    private String lastSecurityEvent = "";

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public OwnerSecurityCore(Context context) {

        if (context == null) {

            throw new IllegalArgumentException(
                    "OwnerSecurityCore context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        this.ownerControlCore =
                new OwnerControlCore(
                        this.context
                );

        this.memoryManager =
                new MemoryManager(
                        this.context
                );

        this.secureRandom =
                new SecureRandom();

        loadSecurityState();
    }

    // =========================================================
    // OWNER
    // =========================================================

    /**
     * الحصول على اسم المالك.
     */
    public synchronized String getOwnerName() {

        return ownerControlCore
                .getOwnerName();
    }

    /**
     * الحصول على ID المالك.
     */
    public synchronized String getOwnerId() {

        return ownerControlCore
                .getOwnerId();
    }

    /**
     * التأكد أن الهوية هي هوية المالك الأساسية.
     */
    public synchronized boolean isOwner(
            String identity
    ) {

        try {

            return ownerControlCore
                    .isOwner(identity);

        } catch (Exception e) {

            recordSecurityEvent(
                    "OWNER_CHECK_ERROR"
            );

            return false;
        }
    }

    // =========================================================
    // OWNER LOCK
    // =========================================================

    /**
     * تغيير المالك غير مسموح.
     */
    public synchronized boolean canChangeOwner() {

        return false;
    }

    /**
     * محاولة تغيير المالك.
     */
    public synchronized String changeOwner(
            String requestedOwner
    ) {

        recordDenied(
                "CHANGE_OWNER"
        );

        return
                "OWNER SECURITY LOCK\n"
                        + "المالك الأساسي: "
                        + getOwnerName()
                        + "\n"
                        + "تغيير المالك مرفوض.";
    }

    /**
     * هل يمكن تعطيل Owner Security؟
     *
     * دائماً false.
     */
    public synchronized boolean canDisableSecurity() {

        return false;
    }

    /**
     * هل يستطيع Evolution Engine تعطيل الحماية؟
     */
    public synchronized boolean evolutionCanDisableSecurity() {

        return false;
    }

    /**
     * هل يستطيع Evolution Engine تغيير المالك؟
     */
    public synchronized boolean evolutionCanChangeOwner() {

        return false;
    }

    /**
     * هل يستطيع Self Builder تغيير Security Core؟
     */
    public synchronized boolean selfBuilderCanModifySecurityCore() {

        return false;
    }

    /**
     * هل يستطيع Code Evolution تغيير Owner Security؟
     */
    public synchronized boolean codeEvolutionCanModifySecurity() {

        return false;
    }

    // =========================================================
    // LOCK STATE
    // =========================================================

    public synchronized boolean isLocked() {

        if (lockUntil <= 0L) {

            return false;
        }

        if (System.currentTimeMillis()
                >= lockUntil) {

            lockUntil = 0L;
            failedAttempts = 0;

            saveSecurityState();

            return false;
        }

        return true;
    }

    public synchronized long getLockRemainingMs() {

        if (!isLocked()) {

            return 0L;
        }

        long remaining =
                lockUntil
                        - System.currentTimeMillis();

        return Math.max(
                remaining,
                0L
        );
    }

    // =========================================================
    // OWNER SESSION
    // =========================================================

    /**
     * بدء جلسة مالك.
     *
     * هذه الخطوة تحتاج هوية مالك صحيحة.
     *
     * إذا كانت صحيحة:
     *
     * - يتم إنشاء token عشوائي.
     * - يتم تخزين Hash فقط.
     * - الجلسة صالحة لمدة محددة.
     *
     * يرجع token للجهاز الداخلي فقط.
     */
    public synchronized String beginOwnerSession(
            String identity
    ) {

        if (isLocked()) {

            recordDenied(
                    "BEGIN_OWNER_SESSION_LOCKED"
            );

            return "";
        }

        if (!isOwner(identity)) {

            registerFailedAttempt();

            recordDenied(
                    "INVALID_OWNER_IDENTITY"
            );

            return "";
        }

        try {

            String token =
                    generateSecureToken();

            activeSessionHash =
                    hashToken(token);

            sessionCreatedAt =
                    System.currentTimeMillis();

            sessionExpiresAt =
                    sessionCreatedAt
                            + OWNER_SESSION_DURATION_MS;

            sessionActive = true;

            failedAttempts = 0;

            lastSecurityEvent =
                    "OWNER_SESSION_CREATED";

            saveSecurityState();

            return token;

        } catch (Exception e) {

            sessionActive = false;

            activeSessionHash = "";

            sessionCreatedAt = 0L;

            sessionExpiresAt = 0L;

            recordSecurityEvent(
                    "SESSION_CREATION_FAILED"
            );

            return "";
        }
    }

    /**
     * التحقق من جلسة المالك.
     */
    public synchronized boolean isSessionValid(
            String token
    ) {

        try {

            if (!sessionActive) {

                return false;
            }

            if (isBlank(token)) {

                return false;
            }

            long now =
                    System.currentTimeMillis();

            if (now >= sessionExpiresAt) {

                revokeOwnerSessionInternal(
                        "SESSION_EXPIRED"
                );

                return false;
            }

            String providedHash =
                    hashToken(token);

            return constantTimeEquals(
                    providedHash,
                    activeSessionHash
            );

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * إنهاء جلسة المالك.
     */
    public synchronized void revokeOwnerSession() {

        revokeOwnerSessionInternal(
                "OWNER_SESSION_REVOKED"
        );
    }

    private void revokeOwnerSessionInternal(
            String reason
    ) {

        activeSessionHash = "";

        sessionCreatedAt = 0L;

        sessionExpiresAt = 0L;

        sessionActive = false;

        lastSecurityEvent = reason;

        saveSecurityState();
    }

    // =========================================================
    // ACTION AUTHORIZATION
    // =========================================================

    /**
     * Authorization باستعمال جلسة المالك.
     */
    public synchronized boolean authorize(
            String sessionToken,
            String action
    ) {

        return authorize(
                sessionToken,
                action,
                getRequiredSecurityLevel(action)
        );
    }

    /**
     * Authorization مع تحديد مستوى مطلوب.
     */
    public synchronized boolean authorize(
            String sessionToken,
            String action,
            int requiredLevel
    ) {

        if (isBlank(action)) {

            recordDenied(
                    "EMPTY_ACTION"
            );

            return false;
        }

        if (!isValidSecurityLevel(
                requiredLevel
        )) {

            recordDenied(
                    "INVALID_SECURITY_LEVEL"
            );

            return false;
        }

        /**
         * العمليات التي تغير هوية المالك أو
         * نظام الحماية لا يسمح بها هذا الـ Core
         * حتى مع وجود جلسة عادية.
         */
        if (isSecurityBoundaryAction(action)) {

            recordDenied(
                    action
            );

            return false;
        }

        if (!isSessionValid(
                sessionToken
        )) {

            recordDenied(
                    action
            );

            return false;
        }

        /**
         * جلسة المالك الحالية لها مستوى Owner Only.
         */
        int grantedLevel =
                LEVEL_OWNER_ONLY;

        if (grantedLevel
                < requiredLevel) {

            recordDenied(
                    action
            );

            return false;
        }

        authorizedOperations++;

        lastAuthorizedAction =
                clean(action);

        lastSecurityEvent =
                "ACTION_AUTHORIZED";

        saveSecurityState();

        return true;
    }

    // =========================================================
    // ACTION CLASSIFICATION
    // =========================================================

    /**
     * تحديد مستوى الحماية المطلوب للأمر.
     */
    public synchronized int getRequiredSecurityLevel(
            String action
    ) {

        if (isBlank(action)) {

            return LEVEL_OWNER_ONLY;
        }

        String normalized =
                normalize(action);

        // -----------------------------------------------------
        // OWNER / SECURITY BOUNDARY
        // -----------------------------------------------------

        if (containsAny(
                normalized,

                "change owner",
                "new owner",
                "replace owner",
                "remove owner",
                "delete owner",
                "disable owner",
                "disable security",
                "remove security",
                "change security core",
                "modify owner control",

                "تغيير المالك",
                "مالك جديد",
                "استبدال المالك",
                "حذف المالك",
                "حيد المالك",
                "تعطيل المالك",
                "تعطيل الحماية",
                "حذف الحماية",
                "تغيير الحماية",
                "تغيير نواة المالك"
        )) {

            return LEVEL_OWNER_ONLY;
        }

        // -----------------------------------------------------
        // CRITICAL
        // -----------------------------------------------------

        if (containsAny(
                normalized,

                "build apk",
                "build debug",
                "modify code",
                "delete code",
                "replace code",
                "write code",
                "install update",
                "apply update",
                "rollback",
                "restore snapshot",
                "reset system",
                "delete memory",
                "clear memory",

                "بناء apk",
                "تعديل الكود",
                "حذف الكود",
                "استبدال الكود",
                "كتابة الكود",
                "تثبيت تحديث",
                "تطبيق تحديث",
                "رجوع نسخة",
                "استرجاع نسخة",
                "إعادة ضبط النظام",
                "حذف الذاكرة",
                "مسح الذاكرة"
        )) {

            return LEVEL_CRITICAL;
        }

        // -----------------------------------------------------
        // SENSITIVE
        // -----------------------------------------------------

        if (containsAny(
                normalized,

                "evolve",
                "evolution",
                "self builder",
                "selfbuilder",
                "code evolution",
                "autonomous evolution",
                "change capability",
                "remove capability",
                "change skill",
                "remove skill",

                "طور نفسك",
                "التطور",
                "التطور الذاتي",
                "البناء الذاتي",
                "تعديل القدرة",
                "حذف القدرة",
                "تعديل المهارة",
                "حذف المهارة"
        )) {

            return LEVEL_SENSITIVE;
        }

        // -----------------------------------------------------
        // NORMAL
        // -----------------------------------------------------

        return LEVEL_NORMAL;
    }

    // =========================================================
    // SECURITY BOUNDARY
    // =========================================================

    /**
     * العمليات التي لا يسمح بها OwnerSecurityCore
     * كعملية عادية.
     *
     * الهدف:
     *
     * حتى إذا كان هناك Bug في Evolution Engine
     * أو Code Evolution Engine، لا يتم اعتبار هذه
     * الأوامر Authorization عادي.
     */
    public synchronized boolean isSecurityBoundaryAction(
            String action
    ) {

        if (isBlank(action)) {

            return true;
        }

        String normalized =
                normalize(action);

        return containsAny(
                normalized,

                "change owner",
                "new owner",
                "replace owner",
                "remove owner",
                "delete owner",

                "disable owner",
                "disable owner control",
                "disable security",
                "remove security",

                "replace security core",
                "delete security core",
                "modify security core",

                "change owner id",
                "change owner identity",

                "تغيير المالك",
                "مالك جديد",
                "استبدال المالك",
                "حذف المالك",
                "تعطيل المالك",
                "تعطيل سيطرة المالك",
                "تعطيل الحماية",
                "حذف الحماية",
                "استبدال الحماية",
                "حذف نواة الحماية",
                "تغيير نواة الحماية",
                "تغيير هوية المالك",
                "تغيير معرف المالك"
        );
    }

    // =========================================================
    // EVOLUTION CONTROL
    // =========================================================

    /**
     * هل يسمح للتطور الذاتي بتنفيذ الهدف؟
     */
    public synchronized boolean isEvolutionAllowed(
            String goal
    ) {

        if (isBlank(goal)) {

            return false;
        }

        if (isSecurityBoundaryAction(goal)) {

            recordDenied(
                    "EVOLUTION_SECURITY_BOUNDARY"
            );

            return false;
        }

        String normalized =
                normalize(goal);

        /**
         * Evolution مسموح لها بالتطوير الداخلي:
         *
         * - learning
         * - skills
         * - capabilities
         * - analysis
         * - optimization
         * - testing
         *
         * ولكن ليس:
         *
         * - owner replacement
         * - security removal
         * - arbitrary takeover
         */
        if (containsAny(
                normalized,

                "take control",
                "take over",
                "become owner",
                "become the owner",
                "escape control",
                "remove restrictions",
                "remove limits",

                "السيطرة على المالك",
                "السيطرة على النظام",
                "اصبح المالك",
                "أصبح المالك",
                "الخروج من السيطرة",
                "إزالة القيود",
                "حذف القيود"
        )) {

            recordDenied(
                    "EVOLUTION_CONTROL_VIOLATION"
            );

            return false;
        }

        return true;
    }

    /**
     * فحص هدف التطور قبل تنفيذه.
     */
    public synchronized boolean authorizeEvolutionGoal(
            String goal
    ) {

        if (!isEvolutionAllowed(goal)) {

            return false;
        }

        return true;
    }

    // =========================================================
    // COMMAND CONTROL
    // =========================================================

    /**
     * هل الأمر محاولة لتغيير علاقة المالك بالنظام؟
     */
    public synchronized boolean isOwnerControlCommand(
            String command
    ) {

        return isSecurityBoundaryAction(
                command
        );
    }

    /**
     * هل الأمر آمن من ناحية Owner Security؟
     */
    public synchronized boolean isCommandAllowed(
            String command
    ) {

        if (isBlank(command)) {

            return false;
        }

        if (isOwnerControlCommand(
                command
        )) {

            recordDenied(
                    command
            );

            return false;
        }

        return true;
    }

    // =========================================================
    // DIRECT OWNER VERIFICATION
    // =========================================================

    /**
     * تحقق مباشر من المالك لتنفيذ عملية.
     *
     * هذه الطريقة لا تنشئ Session.
     *
     * العمليات الحرجة من الأفضل تستعمل Session.
     */
    public synchronized boolean verifyOwnerForAction(
            String identity,
            String action
    ) {

        if (isLocked()) {

            recordDenied(
                    "OWNER_VERIFICATION_LOCKED"
            );

            return false;
        }

        if (isBlank(action)) {

            recordDenied(
                    "EMPTY_OWNER_ACTION"
            );

            return false;
        }

        if (isSecurityBoundaryAction(
                action
        )) {

            recordDenied(
                    action
            );

            return false;
        }

        try {

            boolean verified =
                    ownerControlCore
                            .authorizeOwner(
                                    identity,
                                    action
                            );

            if (!verified) {

                registerFailedAttempt();

                recordDenied(
                        action
                );

                return false;
            }

            authorizedOperations++;

            lastAuthorizedAction =
                    clean(action);

            lastSecurityEvent =
                    "DIRECT_OWNER_AUTHORIZED";

            saveSecurityState();

            return true;

        } catch (Exception e) {

            recordDenied(
                    action
            );

            return false;
        }
    }

    // =========================================================
    // FAILED ATTEMPTS
    // =========================================================

    private void registerFailedAttempt() {

        failedAttempts++;

        if (failedAttempts
                >= MAX_FAILED_ATTEMPTS) {

            lockUntil =
                    System.currentTimeMillis()
                            + LOCK_DURATION_MS;

            failedAttempts = 0;

            lastSecurityEvent =
                    "SECURITY_LOCK_TRIGGERED";
        }

        saveSecurityState();
    }

    // =========================================================
    // AUDIT
    // =========================================================

    private void recordDenied(
            String action
    ) {

        deniedOperations++;

        lastDeniedAction =
                clean(action);

        lastSecurityEvent =
                "ACTION_DENIED";

        try {

            memoryManager.saveMemory(
                    "__jarvis_last_security_denial__",
                    lastDeniedAction
            );

            memoryManager.saveMemory(
                    "__jarvis_security_last_event__",
                    lastSecurityEvent
            );

        } catch (Exception ignored) {
        }
    }

    private void recordSecurityEvent(
            String event
    ) {

        lastSecurityEvent =
                clean(event);

        try {

            memoryManager.saveMemory(
                    "__jarvis_security_last_event__",
                    lastSecurityEvent
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS OWNER SECURITY CORE\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "Security Version: "
        ).append(
                SECURITY_VERSION
        ).append("\n");

        result.append(
                "Owner: "
        ).append(
                getOwnerName()
        ).append("\n");

        result.append(
                "Owner ID: "
        ).append(
                getOwnerId()
        ).append("\n");

        result.append(
                "Owner Change: BLOCKED\n"
        );

        result.append(
                "Security Disable: BLOCKED\n"
        );

        result.append(
                "Evolution Owner Change: BLOCKED\n"
        );

        result.append(
                "Self Builder Security Modification: BLOCKED\n"
        );

        result.append(
                "Code Evolution Security Modification: BLOCKED\n"
        );

        result.append(
                "Session: "
        ).append(
                isSessionValid(
                        getInternalSessionTokenPlaceholder()
                )
                        ? "ACTIVE"
                        : "INACTIVE"
        ).append("\n");

        result.append(
                "Locked: "
        ).append(
                isLocked()
        ).append("\n");

        result.append(
                "Authorized Operations: "
        ).append(
                authorizedOperations
        ).append("\n");

        result.append(
                "Denied Operations: "
        ).append(
                deniedOperations
        ).append("\n");

        result.append(
                "Failed Attempts: "
        ).append(
                failedAttempts
        ).append("\n");

        result.append(
                "Last Authorized: "
        ).append(
                isBlank(lastAuthorizedAction)
                        ? "NONE"
                        : lastAuthorizedAction
        ).append("\n");

        result.append(
                "Last Denied: "
        ).append(
                isBlank(lastDeniedAction)
                        ? "NONE"
                        : lastDeniedAction
        ).append("\n");

        result.append(
                "Last Security Event: "
        ).append(
                isBlank(lastSecurityEvent)
                        ? "NONE"
                        : lastSecurityEvent
        );

        return result.toString();
    }

    /**
     * لا نحتفظ بالـ token الخام.
     *
     * هذه الدالة فقط حتى getStatus()
     * لا يقوم بإنشاء أو كشف session token.
     */
    private String getInternalSessionTokenPlaceholder() {

        return "";
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            return context != null

                    && ownerControlCore != null

                    && memoryManager != null

                    && secureRandom != null

                    && getOwnerName() != null

                    && !getOwnerName()
                    .trim()
                    .isEmpty()

                    && !canChangeOwner()

                    && !canDisableSecurity()

                    && !evolutionCanChangeOwner()

                    && !evolutionCanDisableSecurity()

                    && !selfBuilderCanModifySecurityCore()

                    && !codeEvolutionCanModifySecurity();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // SECURITY METRICS
    // =========================================================

    public synchronized long getAuthorizedOperations() {

        return authorizedOperations;
    }

    public synchronized long getDeniedOperations() {

        return deniedOperations;
    }

    public synchronized int getFailedAttempts() {

        return failedAttempts;
    }

    public synchronized String getLastAuthorizedAction() {

        return isBlank(lastAuthorizedAction)
                ? "لا يوجد."
                : lastAuthorizedAction;
    }

    public synchronized String getLastDeniedAction() {

        return isBlank(lastDeniedAction)
                ? "لا يوجد."
                : lastDeniedAction;
    }

    public synchronized String getLastSecurityEvent() {

        return isBlank(lastSecurityEvent)
                ? "لا يوجد."
                : lastSecurityEvent;
    }

    // =========================================================
    // TOKEN GENERATION
    // =========================================================

    private String generateSecureToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(bytes);

        StringBuilder result =
                new StringBuilder(
                        bytes.length * 2
                );

        for (byte value : bytes) {

            result.append(
                    String.format(
                            Locale.ROOT,
                            "%02x",
                            value & 0xff
                    )
            );
        }

        return result.toString();
    }

    // =========================================================
    // HASH
    // =========================================================

    private String hashToken(
            String token
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] hash =
                digest.digest(
                        token.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder result =
                new StringBuilder(
                        hash.length * 2
                );

        for (byte value : hash) {

            result.append(
                    String.format(
                            Locale.ROOT,
                            "%02x",
                            value & 0xff
                    )
            );
        }

        return result.toString();
    }

    // =========================================================
    // CONSTANT TIME COMPARISON
    // =========================================================

    private boolean constantTimeEquals(
            String first,
            String second
    ) {

        if (first == null
                || second == null) {

            return false;
        }

        byte[] a =
                first.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] b =
                second.getBytes(
                        StandardCharsets.UTF_8
                );

        if (a.length != b.length) {

            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length; i++) {

            result |=
                    a[i] ^ b[i];
        }

        return result == 0;
    }

    // =========================================================
    // PERSISTENCE
    // =========================================================

    private void saveSecurityState() {

        try {

            memoryManager.saveMemory(
                    "__jarvis_security_authorized_count__",
                    String.valueOf(
                            authorizedOperations
                    )
            );

            memoryManager.saveMemory(
                    "__jarvis_security_denied_count__",
                    String.valueOf(
                            deniedOperations
                    )
            );

            memoryManager.saveMemory(
                    "__jarvis_security_last_event__",
                    lastSecurityEvent
            );

            memoryManager.saveMemory(
                    "__jarvis_security_last_authorized__",
                    lastAuthorizedAction
            );

            memoryManager.saveMemory(
                    "__jarvis_security_last_denied__",
                    lastDeniedAction
            );

            /**
             * لا نخزن activeSessionHash.
             *
             * الجلسة تموت مع process restart.
             *
             * وهذا مقصود:
             * إعادة تشغيل التطبيق لا يجب أن تعطي
             * صلاحية Owner تلقائياً.
             */

        } catch (Exception ignored) {
        }
    }

    private void loadSecurityState() {

        try {

            String authorized =
                    memoryManager.getMemory(
                            "__jarvis_security_authorized_count__"
                    );

            if (!isBlank(authorized)) {

                try {

                    authorizedOperations =
                            Long.parseLong(
                                    authorized.trim()
                            );

                } catch (Exception ignored) {

                    authorizedOperations = 0L;
                }
            }

            String denied =
                    memoryManager.getMemory(
                            "__jarvis_security_denied_count__"
                    );

            if (!isBlank(denied)) {

                try {

                    deniedOperations =
                            Long.parseLong(
                                    denied.trim()
                            );

                } catch (Exception ignored) {

                    deniedOperations = 0L;
                }
            }

            String lastEvent =
                    memoryManager.getMemory(
                            "__jarvis_security_last_event__"
                    );

            if (!isBlank(lastEvent)) {

                lastSecurityEvent =
                        lastEvent;
            }

            String lastAuthorized =
                    memoryManager.getMemory(
                            "__jarvis_security_last_authorized__"
                    );

            if (!isBlank(lastAuthorized)) {

                lastAuthorizedAction =
                        lastAuthorized;
            }

            String lastDenied =
                    memoryManager.getMemory(
                            "__jarvis_security_last_denied__"
                    );

            if (!isBlank(lastDenied)) {

                lastDeniedAction =
                        lastDenied;
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String clean(
            String value
    ) {

        if (isBlank(value)) {

            return "";
        }

        return value.trim();
    }

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
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }

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

    private boolean isValidSecurityLevel(
            int level
    ) {

        return level == LEVEL_PUBLIC
                || level == LEVEL_NORMAL
                || level == LEVEL_SENSITIVE
                || level == LEVEL_CRITICAL
                || level == LEVEL_OWNER_ONLY;
    }
}