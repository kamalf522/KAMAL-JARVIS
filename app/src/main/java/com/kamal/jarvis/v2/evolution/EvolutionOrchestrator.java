package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Evolution Orchestrator
 *
 * المنسق الرئيسي لدورة التطور.
 *
 * الدورة:
 *
 * 1. Preflight
 * 2. SelfBuilder
 * 3. CodeEvolutionEngine
 * 4. BuildEngine
 * 5. EvolutionVerificationEngine
 * 6. SelfTestEngine
 * 7. RecoveryEngine عند الفشل
 * 8. حفظ نتيجة الدورة
 *
 * الهدف:
 *
 * JARVIS لا يقول "تطورت" فقط لأن ملفاً تم إنشاؤه.
 *
 * التطور الناجح يحتاج إلى دليل مناسب:
 *
 * - الملفات موجودة
 * - الكود/الملفات تم تعديلها
 * - Build نجح إذا كان مطلوباً
 * - APK موجود إذا كان Build مطلوباً
 * - Verification نجح
 * - SelfTest نجح
 *
 * Security Boundary منفصل عن Evolution.
 * Evolution لا يستطيع تعديل Owner/Security.
 */
public final class EvolutionOrchestrator {

    private static final String ENGINE_ID =
            "v2.evolution_orchestrator";

    private final OwnerSecurityBoundary securityBoundary;
    private final SelfBuilder selfBuilder;
    private final SelfTestEngine selfTestEngine;
    private final RecoveryEngine recoveryEngine;

    private final CodeEvolutionEngine codeEvolutionEngine;
    private final BuildEngine buildEngine;
    private final EvolutionVerificationEngine verificationEngine;

    private final List<EvolutionRecord> history =
            new ArrayList<>();

    private volatile EvolutionState state =
            EvolutionState.IDLE;

    private volatile EvolutionRecord lastRecord;

    private volatile boolean busy;

    /**
     * Constructor القديم/compatibility.
     *
     * يبقى موجوداً باش ما نكسرش أي ملف قديم
     * مازال كيستعمل الـ4 dependencies الأساسية.
     *
     * إذا استعمل هذا constructor، المحرك سيستعمل
     * دورة التحقق المتاحة بدون CodeEvolution/Build
     * الخارجيين.
     */
    public EvolutionOrchestrator(
            OwnerSecurityBoundary securityBoundary,
            SelfBuilder selfBuilder,
            SelfTestEngine selfTestEngine,
            RecoveryEngine recoveryEngine
    ) {

        this(
                securityBoundary,
                selfBuilder,
                selfTestEngine,
                recoveryEngine,
                createCodeEvolutionEngine(
                        selfBuilder,
                        securityBoundary
                ),
                createBuildEngine(
                        selfBuilder,
                        securityBoundary
                ),
                createVerificationEngine(
                        selfBuilder,
                        securityBoundary
                )
        );
    }

    /**
     * Constructor الكامل.
     *
     * هذا هو constructor المستهدف للمعمارية الجديدة.
     */
    public EvolutionOrchestrator(
            OwnerSecurityBoundary securityBoundary,
            SelfBuilder selfBuilder,
            SelfTestEngine selfTestEngine,
            RecoveryEngine recoveryEngine,
            CodeEvolutionEngine codeEvolutionEngine,
            BuildEngine buildEngine,
            EvolutionVerificationEngine verificationEngine
    ) {

        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null."
            );
        }

        if (selfBuilder == null) {
            throw new IllegalArgumentException(
                    "selfBuilder cannot be null."
            );
        }

        if (selfTestEngine == null) {
            throw new IllegalArgumentException(
                    "selfTestEngine cannot be null."
            );
        }

        if (recoveryEngine == null) {
            throw new IllegalArgumentException(
                    "recoveryEngine cannot be null."
            );
        }

        if (codeEvolutionEngine == null) {
            throw new IllegalArgumentException(
                    "codeEvolutionEngine cannot be null."
            );
        }

        if (buildEngine == null) {
            throw new IllegalArgumentException(
                    "buildEngine cannot be null."
            );
        }

        if (verificationEngine == null) {
            throw new IllegalArgumentException(
                    "verificationEngine cannot be null."
            );
        }

        this.securityBoundary =
                securityBoundary;

        this.selfBuilder =
                selfBuilder;

        this.selfTestEngine =
                selfTestEngine;

        this.recoveryEngine =
                recoveryEngine;

        this.codeEvolutionEngine =
                codeEvolutionEngine;

        this.buildEngine =
                buildEngine;

        this.verificationEngine =
                verificationEngine;
    }

    /**
     * دورة التطور الكاملة.
     */
    public synchronized JarvisResult<EvolutionRecord>
    evolve(
            CapabilitySpec spec
    ) {

        if (busy) {

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Another evolution cycle is already running."
            );
        }

        busy = true;

        long startedAt =
                System.currentTimeMillis();

        BuildResultHolder buildHolder =
                new BuildResultHolder();

        try {

            state =
                    EvolutionState.PREFLIGHT;

            JarvisResult<PreflightResult>
                    preflight =
                    preflight(
                            spec
                    );

            if (preflight == null ||
                    !preflight.isSuccess()) {

                return finishFailure(
                        spec,
                        EvolutionOutcome.PREFLIGHT_FAILED,
                        preflight == null
                                ? "Preflight returned no result."
                                : preflight.getMessage(),
                        startedAt,
                        null,
                        null,
                        null
                );
            }

            /*
             * -----------------------------------------
             * 1. SelfBuilder
             * -----------------------------------------
             */
            state =
                    EvolutionState.BUILDING_CAPABILITY;

            JarvisResult<SelfBuilder.BuildResult>
                    builderResult =
                    selfBuilder.build(
                            spec
                    );

            if (builderResult == null ||
                    !builderResult.isSuccess()) {

                return finishFailure(
                        spec,
                        EvolutionOutcome.BUILD_FAILED,
                        builderResult == null
                                ? "SelfBuilder returned no result."
                                : builderResult.getMessage(),
                        startedAt,
                        null,
                        null,
                        null
                );
            }

            SelfBuilder.BuildResult
                    built =
                    builderResult.getData();

            /*
             * -----------------------------------------
             * 2. Verification of generated artifacts
             * -----------------------------------------
             */
            state =
                    EvolutionState.VERIFYING_ARTIFACTS;

            JarvisResult<
                    EvolutionVerificationEngine.VerificationReport
                    > artifactVerification =
                    verificationEngine.verify(
                            spec,
                            built
                    );

            if (artifactVerification == null ||
                    !artifactVerification.isSuccess()) {

                return recoverAfterFailure(
                        spec,
                        built,
                        null,
                        EvolutionOutcome.VERIFICATION_FAILED,
                        artifactVerification == null
                                ? "Artifact verification returned no result."
                                : artifactVerification.getMessage(),
                        startedAt
                );
            }

            /*
             * -----------------------------------------
             * 3. Build حقيقي إذا كان مطلوباً
             * -----------------------------------------
             */
            if (spec.requiresBuild()) {

                state =
                        EvolutionState.BUILDING_PROJECT;

                JarvisResult<BuildEngine.BuildRecord>
                        buildResult =
                        buildEngine.buildDebug();

                if (buildResult == null ||
                        !buildResult.isSuccess()) {

                    return recoverAfterFailure(
                            spec,
                            built,
                            null,
                            EvolutionOutcome.BUILD_FAILED,
                            buildResult == null
                                    ? "BuildEngine returned no result."
                                    : buildResult.getMessage(),
                            startedAt
                    );
                }

                BuildEngine.BuildRecord
                        buildRecord =
                        buildResult.getData();

                buildHolder.record =
                        buildRecord;

                /*
                 * -------------------------------------
                 * 4. Final verification مع Build evidence
                 * -------------------------------------
                 */
                state =
                        EvolutionState.VERIFYING_BUILD;

                JarvisResult<
                        EvolutionVerificationEngine.VerificationReport
                        > finalVerification =
                        verificationEngine.verifyWithBuild(
                                spec,
                                built,
                                buildRecord
                        );

                if (finalVerification == null ||
                        !finalVerification.isSuccess()) {

                    return recoverAfterFailure(
                            spec,
                            built,
                            buildRecord,
                            EvolutionOutcome.VERIFICATION_FAILED,
                            finalVerification == null
                                    ? "Final verification returned no result."
                                    : finalVerification.getMessage(),
                            startedAt
                    );
                }
            }

            /*
             * -----------------------------------------
             * 5. SelfTest
             * -----------------------------------------
             */
            if (spec.requiresTests()) {

                state =
                        EvolutionState.TESTING;

                JarvisResult<SelfTestEngine.TestReport>
                        testResult =
                        selfTestEngine.test(
                                spec,
                                built
                        );

                if (testResult == null ||
                        !testResult.isSuccess()) {

                    return recoverAfterFailure(
                            spec,
                            built,
                            buildHolder.record,
                            EvolutionOutcome.TEST_FAILED,
                            testResult == null
                                    ? "SelfTestEngine returned no result."
                                    : testResult.getMessage(),
                            startedAt
                    );
                }

                SelfTestEngine.TestReport
                        testReport =
                        testResult.getData();

                if (testReport == null ||
                        !testReport.isPassed()) {

                    return recoverAfterFailure(
                            spec,
                            built,
                            buildHolder.record,
                            EvolutionOutcome.TEST_FAILED,
                            testReport == null
                                    ? "SelfTest did not produce a passing report."
                                    : testReport.getSummary(),
                            startedAt
                    );
                }
            }

            /*
             * -----------------------------------------
             * 6. نجاح كامل
             * -----------------------------------------
             */
            state =
                    EvolutionState.READY;

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.SUCCESS,
                            "Evolution completed successfully.",
                            startedAt,
                            System.currentTimeMillis(),
                            built,
                            buildHolder.record,
                            null
                    );

            saveRecord(
                    record
            );

            state =
                    EvolutionState.COMPLETED;

            return JarvisResult.success(
                    record,
                    record.getMessage()
            );

        } catch (Exception e) {

            state =
                    EvolutionState.FAILED;

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec == null
                                    ? ""
                                    : spec.getCapabilityId(),
                            EvolutionOutcome.INTERNAL_ERROR,
                            "Evolution crashed: "
                                    + e.getMessage(),
                            startedAt,
                            System.currentTimeMillis(),
                            null,
                            buildHolder.record,
                            e
                    );

            saveRecord(
                    record
            );

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.EVOLUTION_FAILED,
                            record.getMessage(),
                            ENGINE_ID,
                            e
                    )
            );

        } finally {

            busy =
                    false;
        }
    }

    /**
     * Preflight قوي قبل أي تغيير.
     */
    public synchronized JarvisResult<PreflightResult>
    preflight(
            CapabilitySpec spec
    ) {

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (!spec.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability specification is invalid."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Owner security boundary is not active."
            );
        }

        String capabilityId =
                spec.getCapabilityId();

        if (capabilityId == null ||
                capabilityId.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability ID cannot be empty."
            );
        }

        if (containsProtectedCapabilityId(
                capabilityId
        )) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Capability ID targets a protected security area."
            );
        }

        /*
         * إذا كان التغيير يحتاج Project modification،
         * نتحقق من Security Boundary قبل التنفيذ.
         */
        if (spec.canModifyProjectFiles()) {

            JarvisResult<Boolean>
                    authorization =
                    securityBoundary
                            .authorizeEvolutionChange(
                                    "PROJECT_FILE_EVOLUTION"
                            );

            if (authorization == null ||
                    !authorization.isSuccess()) {

                return failure(
                        JarvisError.Type.NOT_AUTHORIZED,
                        "Project file evolution was not authorized."
                );
            }
        }

        PreflightResult result =
                new PreflightResult(
                        true,
                        "Preflight passed.",
                        spec.requiresBuild(),
                        spec.requiresTests(),
                        spec.canModifyProjectFiles()
                );

        return JarvisResult.success(
                result,
                result.getMessage()
        );
    }

    /**
     * Recovery بعد فشل أي مرحلة.
     */
    private JarvisResult<EvolutionRecord>
    recoverAfterFailure(
            CapabilitySpec spec,
            SelfBuilder.BuildResult buildResult,
            BuildEngine.BuildRecord buildRecord,
            EvolutionOutcome failureOutcome,
            String reason,
            long startedAt
    ) {

        state =
                EvolutionState.RECOVERING;

        try {

            JarvisResult<
                    RecoveryEngine.RecoveryRecord
                    > recoveryResult =
                    recoveryEngine.recover(
                            spec,
                            buildResult,
                            null
                    );

            if (recoveryResult != null &&
                    recoveryResult.isSuccess()) {

                EvolutionRecord record =
                        new EvolutionRecord(
                                spec.getCapabilityId(),
                                EvolutionOutcome.RECOVERED,
                                reason
                                        + " Recovery completed.",
                                startedAt,
                                System.currentTimeMillis(),
                                buildResult,
                                buildRecord,
                                null
                        );

                saveRecord(
                        record
                );

                state =
                        EvolutionState.RECOVERED;

                return JarvisResult.failure(
                        JarvisError.of(
                                mapFailureType(
                                        failureOutcome
                                ),
                                reason
                                        + " Recovery completed; "
                                        + "the failed evolution was not activated.",
                                ENGINE_ID
                        ),
                        record.getMessage()
                );
            }

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.RECOVERY_FAILED,
                            reason
                                    + " Recovery failed.",
                            startedAt,
                            System.currentTimeMillis(),
                            buildResult,
                            buildRecord,
                            null
                    );

            saveRecord(
                    record
            );

            state =
                    EvolutionState.RECOVERY_FAILED;

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.RECOVERY_FAILED,
                            record.getMessage(),
                            ENGINE_ID
                    )
            );

        } catch (Exception e) {

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.RECOVERY_FAILED,
                            reason
                                    + " Recovery crashed: "
                                    + e.getMessage(),
                            startedAt,
                            System.currentTimeMillis(),
                            buildResult,
                            buildRecord,
                            e
                    );

            saveRecord(
                    record
            );

            state =
                    EvolutionState.RECOVERY_FAILED;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.RECOVERY_FAILED,
                            record.getMessage(),
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    private JarvisError.Type mapFailureType(
            EvolutionOutcome outcome
    ) {

        if (outcome == null) {
            return JarvisError.Type.EVOLUTION_FAILED;
        }

        switch (outcome) {

            case BUILD_FAILED:
                return JarvisError.Type.BUILD_FAILED;

            case TEST_FAILED:
            case VERIFICATION_FAILED:
                return JarvisError.Type.TEST_FAILED;

            case PREFLIGHT_FAILED:
                return JarvisError.Type.VALIDATION_FAILED;

            default:
                return JarvisError.Type.EVOLUTION_FAILED;
        }
    }

    private JarvisResult<EvolutionRecord>
    finishFailure(
            CapabilitySpec spec,
            EvolutionOutcome outcome,
            String message,
            long startedAt,
            SelfBuilder.BuildResult buildResult,
            BuildEngine.BuildRecord buildRecord,
            Throwable cause
    ) {

        state =
                EvolutionState.FAILED;

        EvolutionRecord record =
                new EvolutionRecord(
                        spec == null
                                ? ""
                                : spec.getCapabilityId(),
                        outcome,
                        message,
                        startedAt,
                        System.currentTimeMillis(),
                        buildResult,
                        buildRecord,
                        cause
                );

        saveRecord(
                record
        );

        return JarvisResult.failure(
                JarvisError.of(
                        mapFailureType(
                                outcome
                        ),
                        message,
                        ENGINE_ID
                )
        );
    }

    private void saveRecord(
            EvolutionRecord record
    ) {

        if (record == null) {
            return;
        }

        lastRecord =
                record;

        history.add(
                record
        );

        /*
         * نحتفظ بآخر 100 دورة فقط.
         */
        while (
                history.size() > 100
        ) {

            history.remove(
                    0
            );
        }
    }

    private boolean containsProtectedCapabilityId(
            String capabilityId
    ) {

        String id =
                capabilityId
                        .trim()
                        .toLowerCase();

        String[] protectedTokens = {
                "owner",
                "security",
                "authorization",
                "evolutionsecurity",
                "recoverysecurity"
        };

        for (String token :
                protectedTokens) {

            if (id.contains(
                    token
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates default engines from SelfBuilder workspace.
     */
    private static CodeEvolutionEngine
    createCodeEvolutionEngine(
            SelfBuilder builder,
            OwnerSecurityBoundary securityBoundary
    ) {

        File workspace =
                builder.getWorkspaceRoot();

        return new CodeEvolutionEngine(
                workspace,
                securityBoundary
        );
    }

    private static BuildEngine
    createBuildEngine(
            SelfBuilder builder,
            OwnerSecurityBoundary securityBoundary
    ) {

        return new BuildEngine(
                builder.getWorkspaceRoot(),
                securityBoundary
        );
    }

    private static EvolutionVerificationEngine
    createVerificationEngine(
            SelfBuilder builder,
            OwnerSecurityBoundary securityBoundary
    ) {

        return new EvolutionVerificationEngine(
                builder.getWorkspaceRoot(),
                securityBoundary
        );
    }

    public EvolutionState getState() {
        return state;
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isReady() {
        return state == EvolutionState.READY
                || state == EvolutionState.COMPLETED;
    }

    public boolean hasFailed() {
        return state == EvolutionState.FAILED
                || state == EvolutionState.RECOVERY_FAILED;
    }

    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    public List<EvolutionRecord> getHistory() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        history
                )
        );
    }

    public int getEvolutionCount() {
        return history.size();
    }

    public OwnerSecurityBoundary
    getSecurityBoundary() {
        return securityBoundary;
    }

    public SelfBuilder getSelfBuilder() {
        return selfBuilder;
    }

    public SelfTestEngine getSelfTestEngine() {
        return selfTestEngine;
    }

    public RecoveryEngine getRecoveryEngine() {
        return recoveryEngine;
    }

    public CodeEvolutionEngine
    getCodeEvolutionEngine() {
        return codeEvolutionEngine;
    }

    public BuildEngine getBuildEngine() {
        return buildEngine;
    }

    public EvolutionVerificationEngine
    getVerificationEngine() {
        return verificationEngine;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    private <T>
    JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        ENGINE_ID
                )
        );
    }

    private static final class
    BuildResultHolder {

        private BuildEngine.BuildRecord record;
    }

    /**
     * حالة الدورة.
     */
    public enum EvolutionState {

        IDLE,

        PREFLIGHT,

        BUILDING_CAPABILITY,

        VERIFYING_ARTIFACTS,

        BUILDING_PROJECT,

        VERIFYING_BUILD,

        TESTING,

        RECOVERING,

        READY,

        COMPLETED,

        RECOVERED,

        FAILED,

        RECOVERY_FAILED
    }

    /**
     * نتيجة دورة التطور.
     */
    public enum EvolutionOutcome {

        SUCCESS,

        PREFLIGHT_FAILED,

        BUILD_FAILED,

        VERIFICATION_FAILED,

        TEST_FAILED,

        RECOVERED,

        RECOVERY_FAILED,

        INTERNAL_ERROR
    }

    /**
     * معلومات Preflight.
     */
    public static final class PreflightResult {

        private final boolean passed;
        private final String message;
        private final boolean requiresBuild;
        private final boolean requiresTests;
        private final boolean modifiesProject;

        private PreflightResult(
                boolean passed,
                String message,
                boolean requiresBuild,
                boolean requiresTests,
                boolean modifiesProject
        ) {

            this.passed =
                    passed;

            this.message =
                    message;

            this.requiresBuild =
                    requiresBuild;

            this.requiresTests =
                    requiresTests;

            this.modifiesProject =
                    modifiesProject;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }

        public boolean requiresBuild() {
            return requiresBuild;
        }

        public boolean requiresTests() {
            return requiresTests;
        }

        public boolean modifiesProject() {
            return modifiesProject;
        }
    }

    /**
     * سجل دورة تطور كاملة.
     */
    public static final class EvolutionRecord {

        private final String capabilityId;
        private final EvolutionOutcome outcome;
        private final String message;
        private final long startedAt;
        private final long finishedAt;

        private final SelfBuilder.BuildResult
                buildResult;

        private final BuildEngine.BuildRecord
                buildRecord;

        private final Throwable error;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOutcome outcome,
                String message,
                long startedAt,
                long finishedAt,
                SelfBuilder.BuildResult buildResult,
                BuildEngine.BuildRecord buildRecord,
                Throwable error
        ) {

            this.capabilityId =
                    capabilityId;

            this.outcome =
                    outcome;

            this.message =
                    message;

            this.startedAt =
                    startedAt;

            this.finishedAt =
                    finishedAt;

            this.buildResult =
                    buildResult;

            this.buildRecord =
                    buildRecord;

            this.error =
                    error;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOutcome getOutcome() {
            return outcome;
        }

        public String getMessage() {
            return message;
        }

        public long getStartedAt() {
            return startedAt;
        }

        public long getFinishedAt() {
            return finishedAt;
        }

        public long getDurationMillis() {
            return finishedAt - startedAt;
        }

        public SelfBuilder.BuildResult
        getBuildResult() {
            return buildResult;
        }

        public BuildEngine.BuildRecord
        getBuildRecord() {
            return buildRecord;
        }

        public Throwable getError() {
            return error;
        }

        public boolean isSuccess() {
            return outcome ==
                    EvolutionOutcome.SUCCESS;
        }

        public boolean isRecovered() {
            return outcome ==
                    EvolutionOutcome.RECOVERED;
        }

        public boolean isFailure() {
            return !isSuccess();
        }
    }
}