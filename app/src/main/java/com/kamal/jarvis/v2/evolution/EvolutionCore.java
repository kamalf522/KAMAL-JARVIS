package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JARVIS V2 - Evolution Core
 *
 * العقل المركزي لمنظومة Evolution.
 *
 * دورة العمل:
 *
 * REQUEST
 *    ↓
 * DISCOVERY
 *    ↓
 * PLANNING
 *    ↓
 * DIRECT / ALTERNATIVE / PERMISSION / BUILD
 *    ↓
 * EVOLUTION
 *    ↓
 * VERIFICATION
 *    ↓
 * ACTIVATION
 *    ↓
 * READY
 *
 * ملاحظات مهمة:
 *
 * - EvolutionCore لا يمنح Android permissions بنفسه.
 * - EvolutionCore لا يغير OwnerSecurityBoundary.
 * - EvolutionCore لا ينشئ Tool وهمية.
 * - CapabilityActivation لا يتم استعمالها إلا عندما
 *   تكون Tool حقيقية وقابلة للتنفيذ.
 * - إنشاء source code وبناء المشروع يبقى من اختصاص
 *   EvolutionOrchestrator / SourceEvolutionEngine.
 */
public final class EvolutionCore {

    private static final String CORE_ID =
            "v2.evolution_core";

    private final CapabilityDiscovery discovery;
    private final CapabilityExecutor executor;
    private final EvolutionOrchestrator orchestrator;

    /*
     * Activation اختيارية للحفاظ على توافق constructors
     * القديمة.
     *
     * عندما يتم إنشاء EvolutionCore مع ToolRegistry،
     * يصبح Core قادراً على ربط Evolution مع Runtime activation.
     */
    private final CapabilityActivation activation;

    private final Map<String, EvolutionRecord> records =
            new ConcurrentHashMap<>();

    private volatile EvolutionState state =
            EvolutionState.IDLE;

    private volatile EvolutionRecord lastRecord;

    /**
     * Constructor قديم للحفاظ على compatibility.
     *
     * لا يتم إنشاء Activation بدون ToolRegistry.
     */
    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor,
            EvolutionOrchestrator orchestrator
    ) {

        this(
                discovery,
                executor,
                orchestrator,
                null
        );
    }

    /**
     * Constructor الكامل.
     *
     * هذا هو المسار المفضل عندما يكون EvolutionCore
     * مربوطاً بالـRuntime ToolRegistry.
     */
    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor,
            EvolutionOrchestrator orchestrator,
            ToolRegistry toolRegistry
    ) {

        if (discovery == null) {
            throw new IllegalArgumentException(
                    "discovery cannot be null."
            );
        }

        if (executor == null) {
            throw new IllegalArgumentException(
                    "executor cannot be null."
            );
        }

        if (orchestrator == null) {
            throw new IllegalArgumentException(
                    "orchestrator cannot be null."
            );
        }

        this.discovery =
                discovery;

        this.executor =
                executor;

        this.orchestrator =
                orchestrator;

        this.activation =
                toolRegistry == null
                        ? null
                        : new CapabilityActivation(
                                toolRegistry
                        );
    }

    /**
     * تحليل CapabilityRequirement بدون تنفيذ.
     */
    public synchronized JarvisResult<EvolutionAnalysis> analyze(
            CapabilityRequirement requirement
    ) {

        if (requirement == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
            );
        }

        state =
                EvolutionState.ANALYZING;

        CapabilityDiscovery.DiscoveryResult discovered =
                discovery.discover(
                        requirement
                );

        if (discovered == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability discovery returned no result."
            );
        }

        if (!discovered.isValid()) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    discovered.getMessage()
            );
        }

        CapabilityPlan plan =
                CapabilityPlan.fromDiscovery(
                        discovered
                );

        if (plan == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability plan could not be created."
            );
        }

        EvolutionAnalysis analysis =
                new EvolutionAnalysis(
                        requirement,
                        discovered,
                        plan
                );

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:
            case EXECUTE_ALTERNATIVE:

                state =
                        EvolutionState.READY_TO_EXECUTE;

                break;

            case REQUEST_PERMISSION:

                state =
                        EvolutionState.WAITING_FOR_PERMISSION;

                break;

            case BUILD_CAPABILITY:

                state =
                        EvolutionState.BUILD_REQUIRED;

                break;

            case UNAVAILABLE:
            default:

                state =
                        EvolutionState.FAILED;

                break;
        }

        return JarvisResult.success(
                analysis,
                "Evolution analysis completed."
        );
    }

    /**
     * تنفيذ CapabilityRequirement.
     */
    public synchronized JarvisResult<EvolutionExecutionResult> execute(
            CapabilityRequirement requirement,
            ToolContract.ToolInput input
    ) {

        if (requirement == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
            );
        }

        JarvisResult<EvolutionAnalysis> analysisResult =
                analyze(
                        requirement
                );

        if (analysisResult == null ||
                !analysisResult.isSuccess()) {

            state =
                    EvolutionState.FAILED;

            return analysisResult == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Evolution analysis returned no result."
                    )
                    : JarvisResult.failure(
                            analysisResult.getError()
                    );
        }

        EvolutionAnalysis analysis =
                analysisResult.getData();

        if (analysis == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis returned no data."
            );
        }

        CapabilityPlan plan =
                analysis.getPlan();

        if (plan == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis contains no plan."
            );
        }

        /*
         * =========================================
         * DIRECT / ALTERNATIVE
         * =========================================
         */
        if (plan.getAction()
                == CapabilityPlan.Action.EXECUTE_DIRECT
                ||
                plan.getAction()
                == CapabilityPlan.Action.EXECUTE_ALTERNATIVE) {

            state =
                    EvolutionState.EXECUTING;

            JarvisResult<ToolContract.ToolOutput>
                    executionResult =
                    executor.execute(
                            plan,
                            input
                    );

            if (executionResult == null ||
                    !executionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return executionResult == null
                        ? failure(
                                JarvisError.Type.EXECUTION_FAILED,
                                "Capability executor returned no result."
                        )
                        : JarvisResult.failure(
                                executionResult.getError()
                        );
            }

            state =
                    EvolutionState.COMPLETED;

            EvolutionExecutionResult result =
                    EvolutionExecutionResult.executed(
                            plan,
                            executionResult.getData(),
                            executionResult.getMessage()
                    );

            return JarvisResult.success(
                    result,
                    result.getMessage()
            );
        }

        /*
         * =========================================
         * PERMISSION
         * =========================================
         */
        if (plan.getAction()
                == CapabilityPlan.Action.REQUEST_PERMISSION) {

            state =
                    EvolutionState.WAITING_FOR_PERMISSION;

            EvolutionExecutionResult waiting =
                    EvolutionExecutionResult.permissionRequired(
                            plan,
                            plan.getMissingPermissions()
                    );

            return JarvisResult.success(
                    waiting,
                    waiting.getMessage()
            );
        }

        /*
         * =========================================
         * BUILD CAPABILITY
         * =========================================
         */
        if (plan.getAction()
                == CapabilityPlan.Action.BUILD_CAPABILITY) {

            state =
                    EvolutionState.BUILDING;

            CapabilitySpec spec;

            try {

                spec =
                        createCapabilitySpec(
                                requirement,
                                plan
                        );

            } catch (Exception exception) {

                state =
                        EvolutionState.FAILED;

                return JarvisResult.failure(
                        JarvisError.fromException(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Failed to create CapabilitySpec.",
                                CORE_ID,
                                exception
                        )
                );
            }

            if (spec == null ||
                    !spec.isValid()) {

                state =
                        EvolutionState.FAILED;

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Generated CapabilitySpec is invalid."
                );
            }

            /*
             * إطلاق Evolution pipeline.
             */
            JarvisResult<
                    EvolutionOrchestrator.EvolutionRecord
                    > evolutionResult =
                    orchestrator.evolve(
                            spec
                    );

            if (evolutionResult == null ||
                    !evolutionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return evolutionResult == null
                        ? failure(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "EvolutionOrchestrator returned no result."
                        )
                        : JarvisResult.failure(
                                evolutionResult.getError()
                        );
            }

            EvolutionOrchestrator.EvolutionRecord
                    evolutionRecord =
                    evolutionResult.getData();

            if (evolutionRecord == null) {

                state =
                        EvolutionState.FAILED;

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Evolution returned no record."
                );
            }

            if (!evolutionRecord.isReady()) {

                state =
                        EvolutionState.FAILED;

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Evolution completed without producing a ready capability."
                );
            }

            /*
             * =========================================
             * ACTIVATION
             * =========================================
             *
             * مهم:
             *
             * EvolutionOrchestrator الحالي لا ينتج
             * ToolContract جديدة executable من تلقاء نفسه.
             *
             * لذلك لا ننشئ Tool وهمية.
             *
             * إذا كانت Tool المطلوبة موجودة فعلاً في
             * ToolRegistry، نحاول تفعيل Capability.
             *
             * إذا لم تكن موجودة، نسجل أن Evolution وصل
             * إلى READY لكن activation executable
             * مازال ينتظر Tool حقيقية.
             */
            CapabilityActivation.ActivationRecord
                    activationRecord = null;

            if (activation != null) {

                JarvisResult<
                        CapabilityActivation.ActivationRecord
                        > activationResult =
                        activation.activate(
                                spec
                        );

                if (activationResult != null &&
                        activationResult.isSuccess()) {

                    activationRecord =
                            activationResult.getData();

                    if (activationRecord == null ||
                            !activationRecord.isActive()) {

                        state =
                                EvolutionState.FAILED;

                        return failure(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Capability activation returned an invalid active state."
                        );
                    }

                    state =
                            EvolutionState.ACTIVATED;

                } else {

                    /*
                     * لا نفشل Evolution نفسه فقط لأن
                     * لا توجد Tool executable حالياً.
                     *
                     * Build/verification نجحو،
                     * ولكن Runtime activation غير ممكن
                     * بدون Tool حقيقية.
                     */
                    state =
                            EvolutionState.READY;
                }

            } else {

                state =
                        EvolutionState.READY;
            }

            saveRecord(
                    requirement.getCapabilityId(),
                    evolutionRecord,
                    activationRecord
            );

            EvolutionExecutionResult built =
                    EvolutionExecutionResult.built(
                            plan,
                            evolutionRecord,
                            activationRecord,
                            activationRecord != null
                                    && activationRecord.isActive()
                                    ? "Capability was built, verified and activated."
                                    : "Capability was built and verified. Runtime activation is waiting for an executable tool."
                    );

            return JarvisResult.success(
                    built,
                    built.getMessage()
            );
        }

        /*
         * =========================================
         * UNAVAILABLE
         * =========================================
         */
        state =
                EvolutionState.FAILED;

        return failure(
                JarvisError.Type.TOOL_UNAVAILABLE,
                "No valid execution or evolution path is available for capability: "
                        + requirement.getCapabilityId()
        );
    }

    /**
     * تفعيل Tool حقيقية مرتبطة بـCapability
     * بعد أن تكون قد تم إنشاؤها والتحقق منها
     * بواسطة طبقة Evolution أخرى.
     */
    public synchronized JarvisResult<
            CapabilityActivation.ActivationRecord>
    activateGeneratedTool(
            CapabilitySpec spec,
            ToolContract generatedTool
    ) {

        if (activation == null) {

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability activation is not connected to a ToolRegistry."
            );
        }

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (generatedTool == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Generated Tool cannot be null."
            );
        }

        state =
                EvolutionState.ACTIVATING;

        JarvisResult<
                CapabilityActivation.ActivationRecord>
                result =
                activation.activate(
                        spec,
                        generatedTool
                );

        if (result == null ||
                !result.isSuccess()) {

            state =
                    EvolutionState.FAILED;

            return result == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Capability activation returned no result."
                    )
                    : JarvisResult.failure(
                            result.getError()
                    );
        }

        CapabilityActivation.ActivationRecord
                record =
                result.getData();

        if (record == null ||
                !record.isActive()) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Generated capability was not activated."
            );
        }

        state =
                EvolutionState.ACTIVATED;

        return result;
    }

    /**
     * إنشاء CapabilitySpec كاملة.
     */
    private CapabilitySpec createCapabilitySpec(
            CapabilityRequirement requirement,
            CapabilityPlan plan
    ) {

        CapabilitySpec.Builder builder =
                CapabilitySpec.builder(
                        requirement.getCapabilityId(),
                        requirement.getCapabilityId()
                );

        builder.goal(
                requirement.getDescription()
        );

        builder.description(
                "Capability generated by JARVIS Evolution Core."
        );

        Set<CapabilityPermission>
                requiredPermissions =
                requirement.getRequiredPermissions();

        if (requiredPermissions != null) {

            for (CapabilityPermission permission :
                    requiredPermissions) {

                builder.requirePermission(
                        permission
                );
            }
        }

        List<String> preferredTools =
                requirement.getPreferredToolIds();

        if (preferredTools != null) {

            for (String toolId :
                    preferredTools) {

                builder.preferTool(
                        toolId
                );
            }
        }

        List<String> alternativeTools =
                requirement.getAlternativeToolIds();

        if (alternativeTools != null) {

            for (String toolId :
                    alternativeTools) {

                builder.alternativeTool(
                        toolId
                );
            }
        }

        if (plan != null &&
                plan.getToolIds() != null) {

            for (String toolId :
                    plan.getToolIds()) {

                builder.requireTool(
                        toolId
                );
            }
        }

        if (requirement.isOwnerAuthorizationRequired()) {

            builder.requireOwnerAuthorization();
        }

        if (requirement.canBuildAlternative()) {

            builder.allowProjectModification();
        }

        builder.requireBuild();
        builder.requireTests();

        builder.successCriterion(
                "Generated capability specification exists."
        );

        builder.successCriterion(
                "Generated capability files are readable."
        );

        builder.successCriterion(
                "Generated capability contains its identifier."
        );

        return builder.build();
    }

    private void saveRecord(
            String capabilityId,
            EvolutionOrchestrator.EvolutionRecord record
    ) {

        saveRecord(
                capabilityId,
                record,
                null
        );
    }

    private void saveRecord(
            String capabilityId,
            EvolutionOrchestrator.EvolutionRecord record,
            CapabilityActivation.ActivationRecord activationRecord
    ) {

        if (capabilityId == null ||
                capabilityId.trim().isEmpty() ||
                record == null) {

            return;
        }

        EvolutionRecord wrapper =
                new EvolutionRecord(
                        capabilityId,
                        record,
                        activationRecord
                );

        records.put(
                capabilityId,
                wrapper
        );

        lastRecord =
                wrapper;
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        CORE_ID
                )
        );
    }

    public EvolutionRecord getRecord(
            String capabilityId
    ) {

        if (capabilityId == null) {
            return null;
        }

        return records.get(
                capabilityId
        );
    }

    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    public List<EvolutionRecord> getRecords() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        records.values()
                )
        );
    }

    public EvolutionState getState() {
        return state;
    }

    public boolean isBusy() {

        return state ==
                EvolutionState.ANALYZING
                ||
                state ==
                EvolutionState.EXECUTING
                ||
                state ==
                EvolutionState.BUILDING
                ||
                state ==
                EvolutionState.ACTIVATING;
    }

    public boolean isReady() {

        return state ==
                EvolutionState.READY_TO_EXECUTE
                ||
                state ==
                EvolutionState.READY
                ||
                state ==
                EvolutionState.ACTIVATED
                ||
                state ==
                EvolutionState.COMPLETED;
    }

    public boolean isActivationConnected() {
        return activation != null;
    }

    public CapabilityActivation getActivation() {
        return activation;
    }

    public CapabilityDiscovery getDiscovery() {
        return discovery;
    }

    public CapabilityExecutor getExecutor() {
        return executor;
    }

    public EvolutionOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public static String getCoreId() {
        return CORE_ID;
    }

    public enum EvolutionState {

        IDLE,

        ANALYZING,

        READY_TO_EXECUTE,

        WAITING_FOR_PERMISSION,

        BUILD_REQUIRED,

        BUILDING,

        ACTIVATING,

        ACTIVATED,

        READY,

        EXECUTING,

        COMPLETED,

        FAILED
    }

    /**
     * نتيجة التحليل.
     */
    public static final class EvolutionAnalysis {

        private final CapabilityRequirement requirement;
        private final CapabilityDiscovery.DiscoveryResult discovery;
        private final CapabilityPlan plan;

        private EvolutionAnalysis(
                CapabilityRequirement requirement,
                CapabilityDiscovery.DiscoveryResult discovery,
                CapabilityPlan plan
        ) {

            this.requirement =
                    requirement;

            this.discovery =
                    discovery;

            this.plan =
                    plan;
        }

        public CapabilityRequirement
        getRequirement() {
            return requirement;
        }

        public CapabilityDiscovery.DiscoveryResult
        getDiscovery() {
            return discovery;
        }

        public CapabilityPlan getPlan() {
            return plan;
        }

        public boolean requiresBuild() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.BUILD_CAPABILITY;
        }

        public boolean canExecuteDirectly() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.EXECUTE_DIRECT;
        }

        public boolean usesAlternative() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.EXECUTE_ALTERNATIVE;
        }

        public boolean needsPermission() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.REQUEST_PERMISSION;
        }

        public boolean isUnavailable() {

            return plan == null
                    ||
                    plan.getAction()
                            == CapabilityPlan.Action.UNAVAILABLE;
        }

        @Override
        public String toString() {

            return "EvolutionAnalysis{" +
                    "capabilityId='" +
                    requirement.getCapabilityId() +
                    '\'' +
                    ", action=" +
                    (
                            plan == null
                                    ? "null"
                                    : plan.getAction()
                    ) +
                    '}';
        }
    }

    /**
     * نتيجة تنفيذ Evolution.
     */
    public static final class EvolutionExecutionResult {

        private final ExecutionOutcome outcome;
        private final CapabilityPlan plan;
        private final ToolContract.ToolOutput toolOutput;
        private final EvolutionOrchestrator.EvolutionRecord evolutionRecord;
        private final CapabilityActivation.ActivationRecord activationRecord;
        private final List<CapabilityPermission> missingPermissions;
        private final String message;

        private EvolutionExecutionResult(
                ExecutionOutcome outcome,
                CapabilityPlan plan,
                ToolContract.ToolOutput toolOutput,
                EvolutionOrchestrator.EvolutionRecord evolutionRecord,
                CapabilityActivation.ActivationRecord activationRecord,
                List<CapabilityPermission> missingPermissions,
                String message
        ) {

            this.outcome =
                    outcome;

            this.plan =
                    plan;

            this.toolOutput =
                    toolOutput;

            this.evolutionRecord =
                    evolutionRecord;

            this.activationRecord =
                    activationRecord;

            List<CapabilityPermission> copy =
                    missingPermissions == null
                            ? Collections.emptyList()
                            : new ArrayList<>(
                                    missingPermissions
                            );

            this.missingPermissions =
                    Collections.unmodifiableList(
                            copy
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        private static EvolutionExecutionResult executed(
                CapabilityPlan plan,
                ToolContract.ToolOutput output,
                String message
        ) {

            return new EvolutionExecutionResult(
                    ExecutionOutcome.EXECUTED,
                    plan,
                    output,
                    null,
                    null,
                    Collections.emptyList(),
                    message
            );
        }

        private static EvolutionExecutionResult
        permissionRequired(
                CapabilityPlan plan,
                Set<CapabilityPermission>
                        missingPermissions
        ) {

            List<CapabilityPermission> list =
                    missingPermissions == null
                            ? Collections.emptyList()
                            : new ArrayList<>(
                                    missingPermissions
                            );

            return new EvolutionExecutionResult(
                    ExecutionOutcome.PERMISSION_REQUIRED,
                    plan,
                    null,
                    null,
                    null,
                    list,
                    "Permission or capability is required."
            );
        }

        private static EvolutionExecutionResult built(
                CapabilityPlan plan,
                EvolutionOrchestrator.EvolutionRecord record,
                CapabilityActivation.ActivationRecord activationRecord,
                String message
        ) {

            return new EvolutionExecutionResult(
                    activationRecord != null
                            && activationRecord.isActive()
                            ? ExecutionOutcome.ACTIVATED
                            : ExecutionOutcome.BUILT,
                    plan,
                    null,
                    record,
                    activationRecord,
                    Collections.emptyList(),
                    message
            );
        }

        public ExecutionOutcome getOutcome() {
            return outcome;
        }

        public CapabilityPlan getPlan() {
            return plan;
        }

        public ToolContract.ToolOutput
        getToolOutput() {
            return toolOutput;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getEvolutionRecord() {
            return evolutionRecord;
        }

        public CapabilityActivation.ActivationRecord
        getActivationRecord() {
            return activationRecord;
        }

        public List<CapabilityPermission>
        getMissingPermissions() {
            return missingPermissions;
        }

        public String getMessage() {
            return message;
        }

        public boolean wasExecuted() {

            return outcome ==
                    ExecutionOutcome.EXECUTED;
        }

        public boolean wasBuilt() {

            return outcome ==
                    ExecutionOutcome.BUILT
                    ||
                    outcome ==
                    ExecutionOutcome.ACTIVATED;
        }

        public boolean wasActivated() {

            return outcome ==
                    ExecutionOutcome.ACTIVATED;
        }

        public boolean needsPermission() {

            return outcome ==
                    ExecutionOutcome.PERMISSION_REQUIRED;
        }

        @Override
        public String toString() {

            return "EvolutionExecutionResult{" +
                    "outcome=" +
                    outcome +
                    ", message='" +
                    message +
                    '\'' +
                    '}';
        }
    }

    public enum ExecutionOutcome {

        EXECUTED,

        BUILT,

        ACTIVATED,

        PERMISSION_REQUIRED
    }

    /**
     * سجل Evolution داخل Core.
     */
    public static final class EvolutionRecord {

        private final String capabilityId;

        private final EvolutionOrchestrator.EvolutionRecord
                orchestratorRecord;

        private final CapabilityActivation.ActivationRecord
                activationRecord;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOrchestrator.EvolutionRecord
                        orchestratorRecord,
                CapabilityActivation.ActivationRecord
                        activationRecord
        ) {

            this.capabilityId =
                    capabilityId;

            this.orchestratorRecord =
                    orchestratorRecord;

            this.activationRecord =
                    activationRecord;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getOrchestratorRecord() {
            return orchestratorRecord;
        }

        public CapabilityActivation.ActivationRecord
        getActivationRecord() {
            return activationRecord;
        }

        public boolean isActivated() {

            return activationRecord != null
                    &&
                    activationRecord.isActive();
        }

        public boolean isReady() {

            return orchestratorRecord != null
                    &&
                    orchestratorRecord.isReady();
        }

        public boolean wasRecovered() {

            return orchestratorRecord != null
                    &&
                    orchestratorRecord.wasRecovered();
        }

        public boolean failed() {

            return orchestratorRecord == null
                    ||
                    orchestratorRecord.failed();
        }

        @Override
        public String toString() {

            return "EvolutionRecord{" +
                    "capabilityId='" +
                    capabilityId +
                    '\'' +
                    ", ready=" +
                    isReady() +
                    ", activated=" +
                    isActivated() +
                    '}';
        }
    }
}