package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Evolution Orchestrator
 *
 * القلب الذي يربط دورة التطور:
 *
 * CapabilitySpec
 *      ↓
 * SelfBuilder
 *      ↓
 * SelfTestEngine
 *      ↓
 *      ├── SUCCESS → READY
 *      │
 *      └── FAILURE → RecoveryEngine
 *
 * هذا الملف لا يدعي أن APK تم تحديثه أو تثبيته.
 * هو يدير دورة البناء والاختبار والاسترجاع داخل
 * Workspace المسموح به.
 */
public final class EvolutionOrchestrator {

    private static final String ORCHESTRATOR_ID =
            "v2.evolution_orchestrator";

    private final SelfBuilder selfBuilder;
    private final SelfTestEngine selfTestEngine;
    private final RecoveryEngine recoveryEngine;
    private final OwnerSecurityBoundary securityBoundary;

    private final List<EvolutionRecord> history =
            new ArrayList<>();

    private volatile EvolutionRecord lastRecord;
    private volatile EvolutionState state =
            EvolutionState.IDLE;

    public EvolutionOrchestrator(
            SelfBuilder selfBuilder,
            SelfTestEngine selfTestEngine,
            RecoveryEngine recoveryEngine,
            OwnerSecurityBoundary securityBoundary
    ) {
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

        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null."
            );
        }

        this.selfBuilder = selfBuilder;
        this.selfTestEngine = selfTestEngine;
        this.recoveryEngine = recoveryEngine;
        this.securityBoundary = securityBoundary;
    }

    /**
     * يشغل دورة Evolution كاملة.
     */
    public synchronized JarvisResult<EvolutionRecord> evolve(
            CapabilitySpec spec
    ) {
        long startedAt =
                System.currentTimeMillis();

        if (spec == null) {
            state = EvolutionState.FAILED;

            return failure(
                    "CapabilitySpec cannot be null.",
                    startedAt
            );
        }

        /*
         * Owner Security Boundary يجب أن تكون جاهزة
         * قبل أي عملية Evolution تتطلب Owner Authorization.
         */
        if (spec.isOwnerAuthorizationRequired()
                && !securityBoundary.isActive()) {

            state = EvolutionState.FAILED;

            return failure(
                    "Owner security boundary is not active.",
                    startedAt
            );
        }

        /*
         * المرحلة 1: تجهيز Builder.
         */
        state = EvolutionState.INITIALIZING;

        JarvisResult<Boolean> initialization =
                selfBuilder.initialize();

        if (!initialization.isSuccess()) {

            state = EvolutionState.FAILED;

            return failureFromError(
                    initialization.getError(),
                    initialization.getMessage(),
                    startedAt
            );
        }

        /*
         * المرحلة 2: Build.
         */
        state = EvolutionState.BUILDING;

        JarvisResult<SelfBuilder.BuildResult> buildResult =
                selfBuilder.build(spec);

        if (!buildResult.isSuccess()) {

            state = EvolutionState.FAILED;

            return failureFromError(
                    buildResult.getError(),
                    buildResult.getMessage(),
                    startedAt
            );
        }

        SelfBuilder.BuildResult artifact =
                buildResult.getData();

        /*
         * المرحلة 3: Test.
         */
        state = EvolutionState.TESTING;

        JarvisResult<SelfTestEngine.TestReport> testResult =
                selfTestEngine.test(
                        spec,
                        artifact
                );

        SelfTestEngine.TestReport report =
                testResult.getData();

        /*
         * SUCCESS
         */
        if (testResult.isSuccess()
                && report != null
                && report.isPassed()) {

            state = EvolutionState.READY;

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.READY,
                            artifact,
                            report,
                            null,
                            "Capability built and passed self-test.",
                            startedAt,
                            System.currentTimeMillis()
                    );

            saveRecord(record);

            return JarvisResult.success(
                    record,
                    record.getMessage()
            );
        }

        /*
         * FAILURE → Recovery
         */
        state = EvolutionState.RECOVERING;

        JarvisResult<RecoveryEngine.RecoveryRecord>
                recoveryResult =
                recoveryEngine.recover(
                        spec,
                        artifact,
                        report
                );

        RecoveryEngine.RecoveryRecord recovery =
                recoveryResult.getData();

        if (recoveryResult.isSuccess()
                && recovery != null
                && recovery.isSuccessful()) {

            state = EvolutionState.RECOVERED;

            EvolutionRecord record =
                    new EvolutionRecord(
                            spec.getCapabilityId(),
                            EvolutionOutcome.RECOVERED,
                            artifact,
                            report,
                            recovery,
                            "Evolution failed its self-test and was safely recovered.",
                            startedAt,
                            System.currentTimeMillis()
                    );

            saveRecord(record);

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TEST_FAILED,
                            record.getMessage()
                    )
            );
        }

        /*
         * حتى Recovery فشل.
         */
        state = EvolutionState.FAILED;

        EvolutionRecord record =
                new EvolutionRecord(
                        spec.getCapabilityId(),
                        EvolutionOutcome.RECOVERY_FAILED,
                        artifact,
                        report,
                        recovery,
                        "Self-test failed and recovery also failed.",
                        startedAt,
                        System.currentTimeMillis()
                );

        saveRecord(record);

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.RECOVERY_FAILED,
                        record.getMessage()
                )
        );
    }

    /**
     * فحص سريع قبل تشغيل Evolution.
     */
    public synchronized JarvisResult<PreflightResult> preflight(
            CapabilitySpec spec
    ) {
        if (spec == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilitySpec cannot be null."
                    )
            );
        }

        List<String> problems =
                new ArrayList<>();

        if (spec.getCapabilityId() == null
                || spec.getCapabilityId()
                .trim()
                .isEmpty()) {

            problems.add(
                    "Capability ID is missing."
            );
        }

        if (spec.getName() == null
                || spec.getName()
                .trim()
                .isEmpty()) {

            problems.add(
                    "Capability name is missing."
            );
        }

        if (spec.getGoal() == null
                || spec.getGoal()
                .trim()
                .isEmpty()) {

            problems.add(
                    "Capability goal is missing."
            );
        }

        if (spec.getSuccessCriteria() == null
                || spec.getSuccessCriteria().isEmpty()) {

            problems.add(
                    "Success criteria are missing."
            );
        }

        if (spec.isOwnerAuthorizationRequired()
                && !securityBoundary.isActive()) {

            problems.add(
                    "Owner security boundary is inactive."
            );
        }

        if (!problems.isEmpty()) {

            return JarvisResult.success(
                    new PreflightResult(
                            false,
                            problems
                    ),
                    "Preflight failed."
            );
        }

        return JarvisResult.success(
                new PreflightResult(
                        true,
                        Collections.emptyList()
                ),
                "Preflight passed."
        );
    }

    /**
     * يعيد آخر Evolution.
     */
    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    /**
     * يعيد History ديال Evolution.
     */
    public List<EvolutionRecord> getHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(history)
        );
    }

    public EvolutionState getState() {
        return state;
    }

    public boolean isBusy() {
        return state == EvolutionState.INITIALIZING
                || state == EvolutionState.BUILDING
                || state == EvolutionState.TESTING
                || state == EvolutionState.RECOVERING;
    }

    public boolean isReady() {
        return state == EvolutionState.READY;
    }

    public int getEvolutionCount() {
        return history.size();
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

    public static String getOrchestratorId() {
        return ORCHESTRATOR_ID;
    }

    private JarvisResult<EvolutionRecord> failure(
            String message,
            long startedAt
    ) {
        EvolutionRecord record =
                new EvolutionRecord(
                        "unknown",
                        EvolutionOutcome.FAILED,
                        null,
                        null,
                        null,
                        message,
                        startedAt,
                        System.currentTimeMillis()
                );

        saveRecord(record);

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.EVOLUTION_FAILED,
                        message
                )
        );
    }

    private JarvisResult<EvolutionRecord> failureFromError(
            JarvisError error,
            String message,
            long startedAt
    ) {
        EvolutionRecord record =
                new EvolutionRecord(
                        "unknown",
                        EvolutionOutcome.FAILED,
                        null,
                        null,
                        null,
                        message == null
                                ? "Evolution failed."
                                : message,
                        startedAt,
                        System.currentTimeMillis()
                );

        saveRecord(record);

        if (error != null) {
            return JarvisResult.failure(error);
        }

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.EVOLUTION_FAILED,
                        record.getMessage()
                )
        );
    }

    private void saveRecord(
            EvolutionRecord record
    ) {
        lastRecord = record;
        history.add(record);

        /*
         * نحافظ على History محدودة داخل الذاكرة.
         */
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public enum EvolutionState {
        IDLE,
        INITIALIZING,
        BUILDING,
        TESTING,
        RECOVERING,
        READY,
        RECOVERED,
        FAILED
    }

    public enum EvolutionOutcome {
        READY,
        RECOVERED,
        FAILED,
        RECOVERY_FAILED
    }

    /**
     * نتيجة Preflight.
     */
    public static final class PreflightResult {

        private final boolean ready;
        private final List<String> problems;

        private PreflightResult(
                boolean ready,
                List<String> problems
        ) {
            this.ready = ready;

            this.problems =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    problems == null
                                            ? Collections.emptyList()
                                            : problems
                            )
                    );
        }

        public boolean isReady() {
            return ready;
        }

        public List<String> getProblems() {
            return problems;
        }

        public int getProblemCount() {
            return problems.size();
        }

        @Override
        public String toString() {
            return "PreflightResult{" +
                    "ready=" + ready +
                    ", problems=" + problems.size() +
                    '}';
        }
    }

    /**
     * سجل كامل لدورة Evolution.
     */
    public static final class EvolutionRecord {

        private final String capabilityId;
        private final EvolutionOutcome outcome;

        private final SelfBuilder.BuildResult buildResult;
        private final SelfTestEngine.TestReport testReport;
        private final RecoveryEngine.RecoveryRecord recoveryRecord;

        private final String message;
        private final long startedAt;
        private final long finishedAt;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOutcome outcome,
                SelfBuilder.BuildResult buildResult,
                SelfTestEngine.TestReport testReport,
                RecoveryEngine.RecoveryRecord recoveryRecord,
                String message,
                long startedAt,
                long finishedAt
        ) {
            this.capabilityId =
                    capabilityId;

            this.outcome =
                    outcome;

            this.buildResult =
                    buildResult;

            this.testReport =
                    testReport;

            this.recoveryRecord =
                    recoveryRecord;

            this.message =
                    message == null
                            ? ""
                            : message;

            this.startedAt =
                    startedAt;

            this.finishedAt =
                    finishedAt;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOutcome getOutcome() {
            return outcome;
        }

        public SelfBuilder.BuildResult getBuildResult() {
            return buildResult;
        }

        public SelfTestEngine.TestReport getTestReport() {
            return testReport;
        }

        public RecoveryEngine.RecoveryRecord getRecoveryRecord() {
            return recoveryRecord;
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

        public boolean isReady() {
            return outcome ==
                    EvolutionOutcome.READY;
        }

        public boolean wasRecovered() {
            return outcome ==
                    EvolutionOutcome.RECOVERED;
        }

        public boolean failed() {
            return outcome ==
                    EvolutionOutcome.FAILED
                    || outcome ==
                    EvolutionOutcome.RECOVERY_FAILED;
        }

        @Override
        public String toString() {
            return "EvolutionRecord{" +
                    "capabilityId='" +
                    capabilityId + '\'' +
                    ", outcome=" +
                    outcome +
                    ", message='" +
                    message + '\'' +
                    ", durationMillis=" +
                    getDurationMillis() +
                    '}';
        }
    }
}