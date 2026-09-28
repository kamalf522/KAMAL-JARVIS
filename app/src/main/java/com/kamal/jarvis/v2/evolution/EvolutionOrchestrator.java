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
 * مسؤول عن تشغيل دورة التطور كاملة:
 *
 * CapabilitySpec
 *      ↓
 * Preflight
 *      ↓
 * SelfBuilder
 *      ↓
 * SelfTestEngine
 *      ↓
 * SUCCESS ─────────────→ READY
 *      ↓
 * FAILURE
 *      ↓
 * RecoveryEngine
 *      ↓
 * RECOVERED / FAILED
 *
 * هذا المكون لا يتجاوز Android Security،
 * ولا يعدل OwnerSecurityBoundary،
 * ولا يدعي تثبيت APK إذا لم يحدث ذلك فعلياً.
 */
public final class EvolutionOrchestrator {

    private static final String ORCHESTRATOR_ID =
            "v2.evolution_orchestrator";

    private static final int MAX_HISTORY = 100;

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
                    JarvisError.Type.INVALID_REQUEST,
                    startedAt
            );
        }

        /*
         * Preflight قبل أي عملية بناء.
         */
        JarvisResult<PreflightResult> preflightResult =
                preflight(spec);

        if (!preflightResult.isSuccess()) {

            state = EvolutionState.FAILED;

            return failureFromError(
                    preflightResult.getError(),
                    preflightResult.getMessage(),
                    spec.getCapabilityId(),
                    startedAt
            );
        }

        PreflightResult preflight =
                preflightResult.getData();

        if (preflight == null
                || !preflight.isReady()) {

            state = EvolutionState.FAILED;

            String message =
                    preflight == null
                            ? "Evolution preflight failed."
                            : preflight.buildProblemMessage();

            return failure(
                    message,
                    JarvisError.Type.VALIDATION_FAILED,
                    startedAt
            );
        }

        /*
         * المرحلة 1:
         * تجهيز Builder.
         */
        state = EvolutionState.INITIALIZING;

        JarvisResult<Boolean> initialization =
                selfBuilder.initialize();

        if (!initialization.isSuccess()) {

            state = EvolutionState.FAILED;

            return failureFromError(
                    initialization.getError(),
                    initialization.getMessage(),
                    spec.getCapabilityId(),
                    startedAt
            );
        }

        /*
         * المرحلة 2:
         * Build.
         */
        state = EvolutionState.BUILDING;

        JarvisResult<SelfBuilder.BuildResult> buildResult =
                selfBuilder.build(spec);

        if (!buildResult.isSuccess()) {

            state = EvolutionState.FAILED;

            return failureFromError(
                    buildResult.getError(),
                    buildResult.getMessage(),
                    spec.getCapabilityId(),
                    startedAt
            );
        }

        SelfBuilder.BuildResult artifact =
                buildResult.getData();

        if (artifact == null) {

            state = EvolutionState.FAILED;

            return failure(
                    "SelfBuilder returned an empty build result.",
                    JarvisError.Type.BUILD_FAILED,
                    startedAt
            );
        }

        /*
         * المرحلة 3:
         * Self Test.
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
         * TEST FAILURE
         *
         * حتى إذا test() نفسها فشلت وما رجعاتش Report،
         * Recovery يبقى قادر يتعامل مع null.
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

        /*
         * Recovery نجح.
         */
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
                            "Self-test failed and the capability was safely recovered.",
                            startedAt,
                            System.currentTimeMillis()
                    );

            saveRecord(record);

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TEST_FAILED,
                            record.getMessage(),
                            ORCHESTRATOR_ID
                    )
            );
        }

        /*
         * Recovery نفسه فشل.
         */
        state = EvolutionState.RECOVERY_FAILED;

        String recoveryMessage;

        if (recoveryResult.getError() != null) {

            recoveryMessage =
                    recoveryResult.getError()
                            .getMessage();

        } else {

            recoveryMessage =
                    "Self-test failed and recovery also failed.";
        }

        EvolutionRecord record =
                new EvolutionRecord(
                        spec.getCapabilityId(),
                        EvolutionOutcome.RECOVERY_FAILED,
                        artifact,
                        report,
                        recovery,
                        recoveryMessage,
                        startedAt,
                        System.currentTimeMillis()
                );

        saveRecord(record);

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.RECOVERY_FAILED,
                        record.getMessage(),
                        ORCHESTRATOR_ID
                )
        );
    }

    /**
     * فحص مسبق قبل Evolution.
     */
    public synchronized JarvisResult<PreflightResult> preflight(
            CapabilitySpec spec
    ) {

        if (spec == null) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilitySpec cannot be null.",
                            ORCHESTRATOR_ID
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

        /*
         * إذا كانت capability كتحتاج Owner Authorization،
         * فالـSecurity Boundary خاصها تكون active.
         */
        if (spec.isOwnerAuthorizationRequired()
                && !securityBoundary.isActive()) {

            problems.add(
                    "Owner security boundary is inactive."
            );
        }

        /*
         * منع Capability من استهداف مناطق Security المحمية.
         */
        if (containsProtectedCapabilityId(
                spec.getCapabilityId()
        )) {

            problems.add(
                    "Capability ID targets a protected security area."
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
     * آخر نتيجة Evolution.
     */
    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    /**
     * History غير قابلة للتعديل من الخارج.
     */
    public synchronized List<EvolutionRecord> getHistory() {

        return Collections.unmodifiableList(
                new ArrayList<>(history)
        );
    }

    public EvolutionState getState() {
        return state;
    }

    public boolean isBusy() {

        EvolutionState current =
                state;

        return current ==
                EvolutionState.INITIALIZING

                || current ==
                EvolutionState.BUILDING

                || current ==
                EvolutionState.TESTING

                || current ==
                EvolutionState.RECOVERING;
    }

    public boolean isReady() {
        return state == EvolutionState.READY;
    }

    public boolean hasFailed() {

        return state ==
                EvolutionState.FAILED

                || state ==
                EvolutionState.RECOVERY_FAILED;
    }

    public int getEvolutionCount() {

        synchronized (this) {
            return history.size();
        }
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

    public OwnerSecurityBoundary getSecurityBoundary() {
        return securityBoundary;
    }

    public static String getOrchestratorId() {
        return ORCHESTRATOR_ID;
    }

    /**
     * Creates a failed evolution result.
     */
    private JarvisResult<EvolutionRecord> failure(
            String message,
            JarvisError.Type errorType,
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
                        errorType,
                        message,
                        ORCHESTRATOR_ID
                )
        );
    }

    /**
     * Creates a failure result while preserving
     * the original error when available.
     */
    private JarvisResult<EvolutionRecord> failureFromError(
            JarvisError error,
            String message,
            String capabilityId,
            long startedAt
    ) {

        String finalMessage =
                message == null
                        || message.trim().isEmpty()
                        ? "Evolution failed."
                        : message;

        EvolutionRecord record =
                new EvolutionRecord(
                        capabilityId == null
                                ? "unknown"
                                : capabilityId,
                        EvolutionOutcome.FAILED,
                        null,
                        null,
                        null,
                        finalMessage,
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
                        finalMessage,
                        ORCHESTRATOR_ID
                )
        );
    }

    private void saveRecord(
            EvolutionRecord record
    ) {

        if (record == null) {
            return;
        }

        synchronized (this) {

            lastRecord = record;

            history.add(record);

            while (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
    }

    /**
     * يمنع IDs التي تحاول استهداف Security.
     */
    private boolean containsProtectedCapabilityId(
            String capabilityId
    ) {

        if (capabilityId == null) {
            return true;
        }

        String normalized =
                capabilityId
                        .trim()
                        .replace('\\', '/')
                        .toLowerCase();

        return normalized.contains("../")
                || normalized.contains("..\\")
                || normalized.contains("ownersecurityboundary")
                || normalized.contains("securityboundary")
                || normalized.contains("owneridentity")
                || normalized.contains("authorization")
                || normalized.contains("security_policy");
    }

    public enum EvolutionState {

        IDLE,

        INITIALIZING,

        BUILDING,

        TESTING,

        RECOVERING,

        READY,

        RECOVERED,

        FAILED,

        RECOVERY_FAILED
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

        public boolean hasProblems() {
            return !problems.isEmpty();
        }

        public String buildProblemMessage() {

            if (problems.isEmpty()) {
                return "Preflight passed.";
            }

            StringBuilder builder =
                    new StringBuilder(
                            "Preflight problems:"
                    );

            for (String problem :
                    problems) {

                builder.append('\n')
                        .append("- ")
                        .append(problem);
            }

            return builder.toString();
        }

        @Override
        public String toString() {

            return "PreflightResult{" +
                    "ready=" +
                    ready +
                    ", problems=" +
                    problems.size() +
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
            return Math.max(
                    0L,
                    finishedAt - startedAt
            );
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
                    capabilityId +
                    '\'' +
                    ", outcome=" +
                    outcome +
                    ", message='" +
                    message +
                    '\'' +
                    ", durationMillis=" +
                    getDurationMillis() +
                    '}';
        }
    }
}