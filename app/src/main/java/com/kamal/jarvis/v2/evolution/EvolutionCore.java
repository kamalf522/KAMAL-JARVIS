package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EvolutionCore
 *
 * The central controller for JARVIS capability evolution.
 *
 * Responsibilities:
 *
 * 1. Receive a capability goal.
 * 2. Discover what is currently available.
 * 3. Convert discovery into an execution plan.
 * 4. Execute immediately when possible.
 * 5. Detect missing permissions.
 * 6. Detect when a new capability must be built.
 * 7. Keep a persistent runtime record of evolution attempts.
 *
 * This class does NOT bypass Android security.
 * It also does NOT directly modify the security boundary.
 *
 * Builders, testers and recovery systems will be connected
 * through dedicated components later.
 */
public final class EvolutionCore {

    public enum State {
        IDLE,
        ANALYZING,
        READY_TO_EXECUTE,
        EXECUTING,
        WAITING_FOR_PERMISSION,
        BUILD_REQUIRED,
        FAILED,
        COMPLETED
    }

    public enum Outcome {
        EXECUTED,
        ALTERNATIVE_EXECUTED,
        PERMISSION_REQUIRED,
        EVOLUTION_REQUIRED,
        UNAVAILABLE,
        FAILED,
        INVALID
    }

    private final CapabilityDiscovery discovery;
    private final CapabilityExecutor executor;

    private final Map<String, EvolutionRecord> records =
            new ConcurrentHashMap<>();

    private volatile State state = State.IDLE;

    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor
    ) {
        if (discovery == null) {
            throw new IllegalArgumentException(
                    "CapabilityDiscovery cannot be null."
            );
        }

        if (executor == null) {
            throw new IllegalArgumentException(
                    "CapabilityExecutor cannot be null."
            );
        }

        this.discovery = discovery;
        this.executor = executor;
    }

    /**
     * Analyzes a capability without executing it.
     *
     * Useful when JARVIS wants to know what must happen
     * before committing to an action.
     */
    public EvolutionAnalysis analyze(
            CapabilityRequirement requirement
    ) {
        if (requirement == null) {
            state = State.FAILED;

            return EvolutionAnalysis.invalid(
                    "Capability requirement cannot be null."
            );
        }

        state = State.ANALYZING;

        CapabilityDiscovery.DiscoveryResult result =
                discovery.discover(requirement);

        CapabilityPlan plan =
                CapabilityPlan.fromDiscovery(result);

        State nextState =
                determineState(plan);

        state = nextState;

        return new EvolutionAnalysis(
                UUID.randomUUID().toString(),
                requirement,
                result,
                plan,
                nextState
        );
    }

    /**
     * Analyzes and then executes the capability when
     * the discovered plan permits immediate execution.
     *
     * If evolution is required, this method returns an
     * EVOLUTION_REQUIRED result instead of pretending that
     * the capability was built.
     */
    public JarvisResult<ToolContract.ToolOutput> execute(
            CapabilityRequirement requirement,
            ToolContract.ToolInput input
    ) {
        EvolutionAnalysis analysis =
                analyze(requirement);

        if (!analysis.isValid()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            analysis.getMessage()
                    )
            );
        }

        CapabilityPlan plan =
                analysis.getPlan();

        if (plan == null) {
            state = State.FAILED;

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INTERNAL_ERROR,
                            "No capability plan was generated."
                    )
            );
        }

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:

                state = State.EXECUTING;

                JarvisResult<ToolContract.ToolOutput> directResult =
                        executor.execute(plan, input);

                if (directResult.isSuccess()) {
                    state = State.COMPLETED;
                    saveRecord(
                            analysis,
                            Outcome.EXECUTED,
                            directResult.getMessage()
                    );
                } else {
                    state = State.FAILED;
                    saveRecord(
                            analysis,
                            Outcome.FAILED,
                            directResult.getMessage()
                    );
                }

                return directResult;

            case EXECUTE_ALTERNATIVE:

                state = State.EXECUTING;

                JarvisResult<ToolContract.ToolOutput> alternativeResult =
                        executor.execute(plan, input);

                if (alternativeResult.isSuccess()) {
                    state = State.COMPLETED;
                    saveRecord(
                            analysis,
                            Outcome.ALTERNATIVE_EXECUTED,
                            alternativeResult.getMessage()
                    );
                } else {
                    state = State.FAILED;
                    saveRecord(
                            analysis,
                            Outcome.FAILED,
                            alternativeResult.getMessage()
                    );
                }

                return alternativeResult;

            case REQUEST_PERMISSION:

                state = State.WAITING_FOR_PERMISSION;

                saveRecord(
                        analysis,
                        Outcome.PERMISSION_REQUIRED,
                        plan.getReason()
                );

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.NOT_AUTHORIZED,
                                plan.getReason()
                        )
                );

            case BUILD_CAPABILITY:

                state = State.BUILD_REQUIRED;

                saveRecord(
                        analysis,
                        Outcome.EVOLUTION_REQUIRED,
                        plan.getReason()
                );

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "A new capability must be built before execution."
                        )
                );

            case UNAVAILABLE:
            default:

                state = State.FAILED;

                saveRecord(
                        analysis,
                        Outcome.UNAVAILABLE,
                        plan.getReason()
                );

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.TOOL_UNAVAILABLE,
                                plan.getReason()
                        )
                );
        }
    }

    /**
     * Returns the current Evolution Core state.
     */
    public State getState() {
        return state;
    }

    /**
     * Returns an evolution record by ID.
     */
    public EvolutionRecord getRecord(
            String id
    ) {
        if (id == null) {
            return null;
        }

        return records.get(id);
    }

    /**
     * Returns the number of recorded evolution attempts.
     */
    public int getRecordCount() {
        return records.size();
    }

    /**
     * Clears runtime records.
     *
     * This only clears the in-memory history of this core.
     * It does not modify security, owner identity,
     * project files or capabilities.
     */
    public void clearRuntimeRecords() {
        records.clear();
    }

    private State determineState(
            CapabilityPlan plan
    ) {
        if (plan == null) {
            return State.FAILED;
        }

        if (plan.shouldExecute()) {
            if (plan.getAction() ==
                    CapabilityPlan.Action.EXECUTE_ALTERNATIVE) {
                return State.READY_TO_EXECUTE;
            }

            return State.READY_TO_EXECUTE;
        }

        if (plan.shouldRequestPermission()) {
            return State.WAITING_FOR_PERMISSION;
        }

        if (plan.shouldBuildCapability()) {
            return State.BUILD_REQUIRED;
        }

        return State.FAILED;
    }

    private void saveRecord(
            EvolutionAnalysis analysis,
            Outcome outcome,
            String message
    ) {
        if (analysis == null) {
            return;
        }

        EvolutionRecord record =
                new EvolutionRecord(
                        analysis.getId(),
                        analysis.getRequirement()
                                .getCapabilityId(),
                        outcome,
                        message
                );

        records.put(
                analysis.getId(),
                record
        );
    }

    /**
     * Result of the analysis stage.
     */
    public static final class EvolutionAnalysis {

        private final String id;
        private final CapabilityRequirement requirement;
        private final CapabilityDiscovery.DiscoveryResult discoveryResult;
        private final CapabilityPlan plan;
        private final State state;
        private final String message;

        private EvolutionAnalysis(
                String id,
                CapabilityRequirement requirement,
                CapabilityDiscovery.DiscoveryResult discoveryResult,
                CapabilityPlan plan,
                State state
        ) {
            this.id = id;
            this.requirement = requirement;
            this.discoveryResult = discoveryResult;
            this.plan = plan;
            this.state = state;
            this.message = buildMessage();
        }

        private EvolutionAnalysis(
                String message
        ) {
            this.id = "";
            this.requirement = null;
            this.discoveryResult = null;
            this.plan = null;
            this.state = State.FAILED;
            this.message = message;
        }

        public static EvolutionAnalysis invalid(
                String message
        ) {
            return new EvolutionAnalysis(message);
        }

        private String buildMessage() {

            if (plan == null) {
                return "No plan generated.";
            }

            return plan.getReason();
        }

        public String getId() {
            return id;
        }

        public CapabilityRequirement getRequirement() {
            return requirement;
        }

        public CapabilityDiscovery.DiscoveryResult
        getDiscoveryResult() {
            return discoveryResult;
        }

        public CapabilityPlan getPlan() {
            return plan;
        }

        public State getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValid() {
            return requirement != null &&
                    discoveryResult != null &&
                    plan != null;
        }

        public boolean canExecute() {
            return isValid() &&
                    plan.shouldExecute();
        }

        public boolean needsPermission() {
            return isValid() &&
                    plan.shouldRequestPermission();
        }

        public boolean needsEvolution() {
            return isValid() &&
                    plan.shouldBuildCapability();
        }
    }

    /**
     * Persistent-in-runtime record of an evolution attempt.
     */
    public static final class EvolutionRecord {

        private final String analysisId;
        private final String capabilityId;
        private final Outcome outcome;
        private final String message;
        private final long timestamp;

        private EvolutionRecord(
                String analysisId,
                String capabilityId,
                Outcome outcome,
                String message
        ) {
            this.analysisId = analysisId;
            this.capabilityId = capabilityId;
            this.outcome = outcome;
            this.message = message == null ? "" : message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getAnalysisId() {
            return analysisId;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}