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
 * المسار الأساسي:
 *
 * Preflight
 * -> SelfBuilder
 * -> Verification
 * -> Build
 * -> Verification
 * -> SelfTest
 * -> Recovery عند الفشل
 *
 * والمسار البرمجي:
 *
 * Preflight
 * -> SourceEvolutionEngine
 * -> Build
 * -> Verification
 * -> SelfTest
 * -> Recovery عند الفشل
 *
 * Security Boundary مستقل عن Evolution.
 *
 * مهم:
 * هذا المحرك لا يقوم بتوليد Java source من نفسه.
 * عندما يصل ChangeSet من طبقة التخطيط/التوليد،
 * هذا المحرك هو المسؤول عن تنفيذه بشكل آمن
 * ثم اختباره وبناء المشروع والتحقق من النتيجة.
 */
public final class EvolutionOrchestrator {

    private static final String ENGINE_ID =
            "v2.evolution_orchestrator";

    private final OwnerSecurityBoundary securityBoundary;

    private final SelfBuilder selfBuilder;
    private final SelfTestEngine selfTestEngine;
    private final RecoveryEngine recoveryEngine;

    private final CodeEvolutionEngine codeEvolutionEngine;
    private final SourceEvolutionEngine sourceEvolutionEngine;
    private final BuildEngine buildEngine;
    private final EvolutionVerificationEngine verificationEngine;

    private final List<EvolutionRecord> history =
            new ArrayList<>();

    private volatile EvolutionState state =
            EvolutionState.IDLE;

    private volatile EvolutionRecord lastRecord;

    private volatile boolean busy;

    /**
     * Constructor أساسي.
     *
     * ينشئ المحركات التابعة تلقائياً من Workspace ديال SelfBuilder.
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
                null,
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
     * Constructor الكامل القديم/المتوافق.
     *
     * SourceEvolutionEngine يتم إنشاؤه تلقائياً
     * فوق CodeEvolutionEngine.
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

        this(
                securityBoundary,
                selfBuilder,
                selfTestEngine,
                recoveryEngine,
                codeEvolutionEngine,
                createSourceEvolutionEngine(
                        codeEvolutionEngine
                ),
                buildEngine,
                verificationEngine
        );
    }

    /**
     * Constructor الكامل جداً.
     *
     * يسمح بتمرير SourceEvolutionEngine جاهز.
     */
    public EvolutionOrchestrator(
            OwnerSecurityBoundary securityBoundary,
            SelfBuilder selfBuilder,
            SelfTestEngine selfTestEngine,
            RecoveryEngine recoveryEngine,
            CodeEvolutionEngine codeEvolutionEngine,
            SourceEvolutionEngine sourceEvolutionEngine,
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

        if (sourceEvolutionEngine == null) {
            throw new IllegalArgumentException(
                    "sourceEvolutionEngine cannot be null."
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

        this.sourceEvolutionEngine =
                sourceEvolutionEngine;

        this.buildEngine =
                buildEngine;

        this.verificationEngine =
                verificationEngine;
    }

    /**
     * دورة التطور الأساسية.
     *
     * هذه الطريقة تحافظ على API القديمة.
     */
    public synchronized JarvisResult<EvolutionRecord>
    evolve(
            CapabilitySpec spec
    ) {

        return runEvolution(
                spec,
                null
        );
    }

    /**
     * دورة التطور البرمجي الحقيقية.
     *
     * هنا JARVIS يستطيع استقبال ChangeSet
     * وتطبيقه على Source قبل Build/Test.
     */
    public synchronized JarvisResult<EvolutionRecord>
    evolve(
            CapabilitySpec spec,
            SourceEvolutionEngine.ChangeSet changeSet
    ) {

        if (changeSet == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "ChangeSet cannot be null for source evolution."
            );
        }

        return runEvolution(
                spec,
                changeSet
        );
    }

    /**
     * التنفيذ الداخلي الموحد.
     */
    private JarvisResult<EvolutionRecord>
    runEvolution(
            CapabilitySpec spec,
            SourceEvolutionEngine.ChangeSet changeSet
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

        BuildEngine.BuildRecord buildRecord =
                null;

        SelfBuilder.BuildResult capabilityBuild =
                null;

        try {

            /*
             * =========================================
             * 1. PREFLIGHT
             * =========================================
             */

            state =
                    EvolutionState.PREFLIGHT;

            JarvisResult<PreflightResult>
                    preflightResult =
                    preflight(
                            spec
                    );

            if (preflightResult == null ||
                    !preflightResult.isSuccess()) {

                return finishFailure(
                        spec,
                        EvolutionOutcome.PREFLIGHT_FAILED,
                        preflightResult == null
                                ? "Preflight returned no result."
                                : preflightResult.getMessage(),
                        startedAt,
                        null,
                        null,
                        null
                );
            }

            /*
             * =========================================
             * 2. SOURCE EVOLUTION
             * =========================================
             *
             * إذا كان عندنا ChangeSet،
             * هنا فقط يتم تعديل source.
             */

            if (changeSet != null) {

                state =
                        EvolutionState.EVOLVING_SOURCE;

                JarvisResult<
                        SourceEvolutionEngine.ChangeResult
                        > sourceResult =
                        sourceEvolutionEngine.apply(
                                changeSet
                        );

                if (sourceResult == null ||
                        !sourceResult.isSuccess()) {

                    String message =
                            sourceResult == null
                                    ? "SourceEvolutionEngine returned no result."
                                    : sourceResult.getMessage();

                    /*
                     * SourceEvolutionEngine عند الفشل
                     * يقوم بمحاولة rollback الداخلي ديالو.
                     */
                    return finishFailure(
                            spec,
                            EvolutionOutcome.SOURCE_EVOLUTION_FAILED,
                            message,
                            startedAt,
                            null,
                            null,
                            null
                    );
                }
            }

            /*
             * =========================================
             * 3. SELF BUILDER
             * =========================================
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

                return recoverAfterFailure(
                        spec,
                        null,
                        null,
                        EvolutionOutcome.BUILD_FAILED,
                        builderResult == null
                                ? "SelfBuilder returned no result."
                                : builderResult.getMessage(),
                        startedAt
                );
            }

            capabilityBuild =
                    builderResult.getData();

            if (capabilityBuild == null) {

                return recoverAfterFailure(
                        spec,
                        null,
                        null,
                        EvolutionOutcome.BUILD_FAILED,
                        "SelfBuilder returned empty build data.",
                        startedAt
                );
            }

            /*
             * =========================================
             * 4. ARTIFACT VERIFICATION
             * =========================================
             */

            state =
                    EvolutionState.VERIFYING_ARTIFACTS;

            JarvisResult<
                    EvolutionVerificationEngine.VerificationReport
                    > artifactVerification =
                    verificationEngine.verify(
                            spec,
                            capabilityBuild
                    );

            if (artifactVerification == null ||
                    !artifactVerification.isSuccess()) {

                return recoverAfterFailure(
                        spec,
                        capabilityBuild,
                        null,
                        EvolutionOutcome.VERIFICATION_FAILED,
                        artifactVerification == null
                                ? "Artifact verification returned no result."
                                : artifactVerification.getMessage(),
                        startedAt
                );
            }

            /*
             * =========================================
             * 5. REAL PROJECT BUILD
             * =========================================
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
                            capabilityBuild,
                            null,
                            EvolutionOutcome.BUILD_FAILED,
                            buildResult == null
                                    ? "BuildEngine returned no result."
                                    : buildResult.getMessage(),
                            startedAt
                    );
                }

                buildRecord =
                        buildResult.getData();

                if (buildRecord == null) {

                    return recoverAfterFailure(
                            spec,
                            capabilityBuild,
                            null,
                            EvolutionOutcome.BUILD_FAILED,
                            "BuildEngine returned empty build record.",
                            startedAt
                    );
                }

                /*
                 * =====================================
                 * 6. BUILD VERIFICATION
                 * =====================================
                 */

                state =
                        EvolutionState.VERIFYING_BUILD;

                JarvisResult<
                        EvolutionVerificationEngine.VerificationReport
                        > finalVerification =
                        verificationEngine.verifyWithBuild(
                                spec,
                                capabilityBuild,
                                buildRecord
                        );

                if (finalVerification == null ||
                        !finalVerification.isSuccess()) {

                    return recoverAfterFailure(
                            spec,
                            capabilityBuild,
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
             * =========================================
             * 7. SELF TEST
             * =========================================
             */

            if (spec.requiresTests()) {

                state =
                        EvolutionState.TESTING;

                JarvisResult<SelfTestEngine.TestReport>
                        testResult =
                        selfTestEngine.test(
                                spec,
                                capabilityBuild
                        );

                if (testResult == null ||
                        !testResult.isSuccess()) {

                    return recoverAfterFailure(
                            spec,
                            capabilityBuild,
                            buildRecord,
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
                            capabilityBuild,
                            buildRecord,
                            EvolutionOutcome.TEST_FAILED,
                            testReport == null
                                    ? "SelfTest did not produce a passing report."
                                    : testReport.getSummary(),
                            startedAt
                    );
                }
            }

            /*
             * =========================================
             * 8. SUCCESS
             * =========================================
             */

            state =
                    EvolutionState.READY;

            String successMessage;

            if (changeSet != null) {

                successMessage =
                        "Source evolution, build, verification and testing completed successfully.";

            } else {

                successMessage =
                        "Evolution completed successfully.";
            }

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.SUCCESS,
                            successMessage,
                            startedAt,
                            System.currentTimeMillis(),
                            capabilityBuild,
                            buildRecord,
                            null,
                            changeSet
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
                            capabilityBuild,
                            buildRecord,
                            e,
                            changeSet
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
     * Preflight قبل أي تغيير.
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
     * Recovery بعد فشل دورة التطور.
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
                                spec == null
                                        ? ""
                                        : spec.getCapabilityId(),
                                EvolutionOutcome.RECOVERED,
                                reason
                                        + " Recovery completed.",
                                startedAt,
                                System.currentTimeMillis(),
                                buildResult,
                                buildRecord,
                                null,
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
                            spec == null
                                    ? ""
                                    : spec.getCapabilityId(),
                            EvolutionOutcome.RECOVERY_FAILED,
                            reason
                                    + " Recovery failed.",
                            startedAt,
                            System.currentTimeMillis(),
                            buildResult,
                            buildRecord,
                            null,
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
                            spec == null
                                    ? ""
                                    : spec.getCapabilityId(),
                            EvolutionOutcome.RECOVERY_FAILED,
                            reason
                                    + " Recovery crashed: "
                                    + e.getMessage(),
                            startedAt,
                            System.currentTimeMillis(),
                            buildResult,
                            buildRecord,
                            e,
                            null
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

    /**
     * تحويل نتيجة التطور إلى نوع الخطأ المناسب.
     */
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

            case SOURCE_EVOLUTION_FAILED:
                return JarvisError.Type.FILE_OPERATION_FAILED;

            case PREFLIGHT_FAILED:
                return JarvisError.Type.VALIDATION_FAILED;

            case RECOVERY_FAILED:
                return JarvisError.Type.RECOVERY_FAILED;

            default:
                return JarvisError.Type.EVOLUTION_FAILED;
        }
    }

    /**
     * فشل قبل الدخول في Recovery.
     */
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
                        cause,
                        null
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

    /**
     * حفظ سجل التطور.
     */
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

        while (
                history.size() > 100
        ) {

            history.remove(
                    0
            );
        }
    }

    /**
     * حماية أسماء Owner/Security.
     */
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

            if (id.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private static CodeEvolutionEngine
    createCodeEvolutionEngine(
            SelfBuilder builder,
            OwnerSecurityBoundary securityBoundary
    ) {

        return new CodeEvolutionEngine(
                builder.getWorkspaceRoot(),
                securityBoundary
        );
    }

    private static SourceEvolutionEngine
    createSourceEvolutionEngine(
            CodeEvolutionEngine codeEvolutionEngine
    ) {

        return new SourceEvolutionEngine(
                codeEvolutionEngine
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

        return state ==
                EvolutionState.READY
                || state ==
                EvolutionState.COMPLETED;
    }

    public boolean hasFailed() {

        return state ==
                EvolutionState.FAILED
                || state ==
                EvolutionState.RECOVERY_FAILED;
    }

    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    public List<EvolutionRecord>
    getHistory() {

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

    public SelfBuilder
    getSelfBuilder() {

        return selfBuilder;
    }

    public SelfTestEngine
    getSelfTestEngine() {

        return selfTestEngine;
    }

    public RecoveryEngine
    getRecoveryEngine() {

        return recoveryEngine;
    }

    public CodeEvolutionEngine
    getCodeEvolutionEngine() {

        return codeEvolutionEngine;
    }

    public SourceEvolutionEngine
    getSourceEvolutionEngine() {

        return sourceEvolutionEngine;
    }

    public BuildEngine
    getBuildEngine() {

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

    /**
     * حالة الدورة.
     */
    public enum EvolutionState {

        IDLE,

        PREFLIGHT,

        EVOLVING_SOURCE,

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
     * نتائج دورة التطور.
     */
    public enum EvolutionOutcome {

        SUCCESS,

        PREFLIGHT_FAILED,

        SOURCE_EVOLUTION_FAILED,

        BUILD_FAILED,

        VERIFICATION_FAILED,

        TEST_FAILED,

        RECOVERED,

        RECOVERY_FAILED,

        INTERNAL_ERROR
    }

    /**
     * نتيجة Preflight.
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
     * سجل كامل لدورة التطور.
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

        private final SourceEvolutionEngine.ChangeSet
                sourceChangeSet;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOutcome outcome,
                String message,
                long startedAt,
                long finishedAt,
                SelfBuilder.BuildResult buildResult,
                BuildEngine.BuildRecord buildRecord,
                Throwable error,
                SourceEvolutionEngine.ChangeSet sourceChangeSet
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

            this.sourceChangeSet =
                    sourceChangeSet;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOutcome
        getOutcome() {

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

            return finishedAt -
                    startedAt;
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

        public SourceEvolutionEngine.ChangeSet
        getSourceChangeSet() {

            return sourceChangeSet;
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

        public boolean
        hasSourceEvolution() {

            return sourceChangeSet != null;
        }
    }
}