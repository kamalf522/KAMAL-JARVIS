package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JARVIS V2 - Evolution Core
 *
 * المستوى الأعلى لمنظومة Evolution.
 *
 * المسؤوليات:
 *
 * 1. استقبال CapabilityRequirement.
 * 2. تحليل القدرة المطلوبة.
 * 3. تحديد هل التنفيذ المباشر ممكن.
 * 4. إذا كانت Capability ناقصة:
 *      - إنشاء CapabilitySpec
 *      - إطلاق EvolutionOrchestrator
 * 5. بناء Capability.
 * 6. اختبارها.
 * 7. Recovery عند الفشل.
 * 8. حفظ نتيجة كل Evolution.
 *
 * EvolutionCore لا يتجاوز OwnerSecurityBoundary.
 */
public final class EvolutionCore {

    private static final String CORE_ID =
            "v2.evolution_core";

    private final CapabilityDiscovery discovery;
    private final CapabilityExecutor executor;
    private final EvolutionOrchestrator orchestrator;

    private final Map<String, EvolutionRecord> records =
            new ConcurrentHashMap<>();

    private volatile EvolutionState state =
            EvolutionState.IDLE;

    private volatile EvolutionRecord lastRecord;

    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor,
            EvolutionOrchestrator orchestrator
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

        this.discovery = discovery;
        this.executor = executor;
        this.orchestrator = orchestrator;
    }

    /**
     * يحلل Requirement فقط بدون تنفيذ.
     */
    public synchronized JarvisResult<EvolutionAnalysis> analyze(
            CapabilityRequirement requirement
    ) {
        if (requirement == null) {
            state = EvolutionState.FAILED;

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilityRequirement cannot be null."
                    )
            );
        }

        state = EvolutionState.ANALYZING;

        JarvisResult<CapabilityDiscovery.DiscoveryResult>
                discoveryResult =
                discovery.discover(requirement);

        if (!discoveryResult.isSuccess()) {
            state = EvolutionState.FAILED;

            return JarvisResult.failure(
                    discoveryResult.getError()
            );
        }

        CapabilityDiscovery.DiscoveryResult discovered =
                discoveryResult.getData();

        CapabilityPlan plan =
                CapabilityPlan.fromDiscovery(
                        discovered
                );

        EvolutionAnalysis analysis =
                new EvolutionAnalysis(
                        requirement,
                        discovered,
                        plan
                );

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:
                state = EvolutionState.READY_TO_EXECUTE;
                break;

            case EXECUTE_ALTERNATIVE:
                state = EvolutionState.READY_TO_EXECUTE;
                break;

            case REQUEST_PERMISSION:
                state = EvolutionState.WAITING_FOR_PERMISSION;
                break;

            case BUILD_CAPABILITY:
                state = EvolutionState.BUILD_REQUIRED;
                break;

            case UNAVAILABLE:
            default:
                state = EvolutionState.FAILED;
                break;
        }

        return JarvisResult.success(
                analysis,
                "Evolution analysis completed."
        );
    }

    /**
     * ينفذ Requirement.
     *
     * إذا كانت Capability موجودة:
     *     execute مباشرة.
     *
     * إذا كانت ناقصة:
     *     يبنيها عبر EvolutionOrchestrator.
     */
    public synchronized JarvisResult<EvolutionExecutionResult> execute(
            CapabilityRequirement requirement,
            com.kamal.jarvis.v2.core.ToolContract.ToolInput input
    ) {
        if (requirement == null) {
            state = EvolutionState.FAILED;

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilityRequirement cannot be null."
                    )
            );
        }

        JarvisResult<EvolutionAnalysis> analysisResult =
                analyze(requirement);

        if (!analysisResult.isSuccess()) {
            return JarvisResult.failure(
                    analysisResult.getError()
            );
        }

        EvolutionAnalysis analysis =
                analysisResult.getData();

        CapabilityPlan plan =
                analysis.getPlan();

        /*
         * Capability موجودة أصلاً.
         */
        if (plan.getAction()
                == CapabilityPlan.Action.EXECUTE_DIRECT
                || plan.getAction()
                == CapabilityPlan.Action.EXECUTE_ALTERNATIVE) {

            state = EvolutionState.EXECUTING;

            JarvisResult<
                    com.kamal.jarvis.v2.core.ToolContract.ToolOutput
                    > result =
                    executor.execute(
                            plan,
                            input
                    );

            if (result.isSuccess()) {

                state = EvolutionState.COMPLETED;

                EvolutionExecutionResult execution =
                        EvolutionExecutionResult.executed(
                                plan,
                                result.getData(),
                                result.getMessage()
                        );

                return JarvisResult.success(
                        execution,
                        result.getMessage()
                );
            }

            state = EvolutionState.FAILED;

            return JarvisResult.failure(
                    result.getError()
            );
        }

        /*
         * Permission ناقصة.
         *
         * ما غاديش نزورو ونقولو "مايمكنش".
         * هنا كنرجعو Requirement واضح للنظام الأعلى
         * باش PermissionManager / AndroidPermissionBridge
         * يتصرفو بالطريقة المشروعة.
         */
        if (plan.getAction()
                == CapabilityPlan.Action.REQUEST_PERMISSION) {

            state = EvolutionState.WAITING_FOR_PERMISSION;

            EvolutionExecutionResult waiting =
                    EvolutionExecutionResult.permissionRequired(
                            plan,
                            plan.getMissingPermissions()
                    );

            return JarvisResult.success(
                    waiting,
                    "Additional permission is required."
            );
        }

        /*
         * Capability ناقصة:
         *
         * هنا ندخل فعلياً إلى:
         *
         * CapabilitySpec
         *      ↓
         * SelfBuilder
         *      ↓
         * SelfTest
         *      ↓
         * Recovery
         */
        if (plan.getAction()
                == CapabilityPlan.Action.BUILD_CAPABILITY) {

            state = EvolutionState.BUILDING;

            CapabilitySpec spec =
                    createCapabilitySpec(
                            requirement,
                            plan
                    );

            JarvisResult<
                    EvolutionOrchestrator.EvolutionRecord
                    > evolutionResult =
                    orchestrator.evolve(spec);

            if (!evolutionResult.isSuccess()) {

                state = EvolutionState.FAILED;

                /*
                 * إذا رجع Orchestrator نتيجة
                 * فشل الاختبار أو Recovery، نحافظ
                 * على الحقيقة ولا نقول بأن Capability
                 * أصبحت جاهزة.
                 */
                return JarvisResult.failure(
                        evolutionResult.getError()
                );
            }

            EvolutionOrchestrator.EvolutionRecord
                    evolutionRecord =
                    evolutionResult.getData();

            if (evolutionRecord == null
                    || !evolutionRecord.isReady()) {

                state = EvolutionState.FAILED;

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Evolution did not produce a ready capability."
                        )
                );
            }

            state = EvolutionState.COMPLETED;

            EvolutionExecutionResult built =
                    EvolutionExecutionResult.built(
                            plan,
                            evolutionRecord,
                            "Capability was built and passed self-test."
                    );

            saveRecord(
                    requirement.getCapabilityId(),
                    evolutionRecord
            );

            return JarvisResult.success(
                    built,
                    built.getMessage()
            );
        }

        state = EvolutionState.FAILED;

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Capability is unavailable."
                )
        );
    }

    /**
     * يبني CapabilitySpec من Requirement + Plan.
     *
     * هنا كنحوّلو الحاجة المجردة إلى مواصفات
     * يستطيع SelfBuilder العمل عليها.
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

        if (requirement.getRequiredPermissions()
                != null) {

            for (String permission :
                    requirement.getRequiredPermissions()) {

                builder.requirePermission(
                        permission
                );
            }
        }

        if (requirement.getPreferredToolIds()
                != null) {

            for (String tool :
                    requirement.getPreferredToolIds()) {

                builder.preferTool(tool);
            }
        }

        if (requirement.getAlternativeToolIds()
                != null) {

            for (String tool :
                    requirement.getAlternativeToolIds()) {

                builder.alternativeTool(tool);
            }
        }

        if (requirement.isOwnerAuthorizationRequired()) {
            builder.requireOwnerAuthorization();
        }

        /*
         * Capability الجديدة مسموح لها بالبناء
         * إذا Requirement سمح بذلك.
         */
        if (requirement.canBuildAlternative()) {
            builder.allowProjectModification();
        }

        /*
         * كل Capability يتم بناؤها يجب أن يكون
         * عندها معيار نجاح واضح.
         */
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
        if (capabilityId == null || record == null) {
            return;
        }

        EvolutionRecord wrapper =
                new EvolutionRecord(
                        capabilityId,
                        record
                );

        records.put(
                capabilityId,
                wrapper
        );

        lastRecord = wrapper;
    }

    public EvolutionRecord getRecord(
            String capabilityId
    ) {
        if (capabilityId == null) {
            return null;
        }

        return records.get(capabilityId);
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
        return state == EvolutionState.ANALYZING
                || state == EvolutionState.EXECUTING
                || state == EvolutionState.BUILDING;
    }

    public boolean isReady() {
        return state == EvolutionState.READY_TO_EXECUTE
                || state == EvolutionState.COMPLETED;
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
            this.requirement = requirement;
            this.discovery = discovery;
            this.plan = plan;
        }

        public CapabilityRequirement getRequirement() {
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
            return plan.getAction()
                    == CapabilityPlan.Action.BUILD_CAPABILITY;
        }

        public boolean canExecuteDirectly() {
            return plan.getAction()
                    == CapabilityPlan.Action.EXECUTE_DIRECT;
        }

        public boolean needsPermission() {
            return plan.getAction()
                    == CapabilityPlan.Action.REQUEST_PERMISSION;
        }

        @Override
        public String toString() {
            return "EvolutionAnalysis{" +
                    "capabilityId='" +
                    requirement.getCapabilityId() +
                    '\'' +
                    ", action=" +
                    plan.getAction() +
                    '}';
        }
    }

    /**
     * نتيجة تنفيذ Evolution.
     */
    public static final class EvolutionExecutionResult {

        private final ExecutionOutcome outcome;
        private final CapabilityPlan plan;

        private final com.kamal.jarvis.v2.core.ToolContract.ToolOutput
                toolOutput;

        private final EvolutionOrchestrator.EvolutionRecord
                evolutionRecord;

        private final List<String> missingPermissions;

        private final String message;

        private EvolutionExecutionResult(
                ExecutionOutcome outcome,
                CapabilityPlan plan,
                com.kamal.jarvis.v2.core.ToolContract.ToolOutput
                        toolOutput,
                EvolutionOrchestrator.EvolutionRecord
                        evolutionRecord,
                List<String> missingPermissions,
                String message
        ) {
            this.outcome = outcome;
            this.plan = plan;
            this.toolOutput = toolOutput;
            this.evolutionRecord = evolutionRecord;

            this.missingPermissions =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    missingPermissions == null
                                            ? Collections.emptyList()
                                            : missingPermissions
                            )
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        private static EvolutionExecutionResult executed(
                CapabilityPlan plan,
                com.kamal.jarvis.v2.core.ToolContract.ToolOutput output,
                String message
        ) {
            return new EvolutionExecutionResult(
                    ExecutionOutcome.EXECUTED,
                    plan,
                    output,
                    null,
                    Collections.emptyList(),
                    message
            );
        }

        private static EvolutionExecutionResult
        permissionRequired(
                CapabilityPlan plan,
                List<String> missingPermissions
        ) {
            return new EvolutionExecutionResult(
                    ExecutionOutcome.PERMISSION_REQUIRED,
                    plan,
                    null,
                    null,
                    missingPermissions,
                    "Permission required."
            );
        }

        private static EvolutionExecutionResult built(
                CapabilityPlan plan,
                EvolutionOrchestrator.EvolutionRecord record,
                String message
        ) {
            return new EvolutionExecutionResult(
                    ExecutionOutcome.BUILT,
                    plan,
                    null,
                    record,
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

        public com.kamal.jarvis.v2.core.ToolContract.ToolOutput
        getToolOutput() {
            return toolOutput;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getEvolutionRecord() {
            return evolutionRecord;
        }

        public List<String> getMissingPermissions() {
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
                    ExecutionOutcome.BUILT;
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
                    message + '\'' +
                    '}';
        }
    }

    public enum ExecutionOutcome {
        EXECUTED,
        BUILT,
        PERMISSION_REQUIRED
    }

    /**
     * سجل Evolution مختصر داخل Core.
     */
    public static final class EvolutionRecord {

        private final String capabilityId;
        private final EvolutionOrchestrator.EvolutionRecord
                orchestratorRecord;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOrchestrator.EvolutionRecord
                        orchestratorRecord
        ) {
            this.capabilityId = capabilityId;
            this.orchestratorRecord =
                    orchestratorRecord;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getOrchestratorRecord() {
            return orchestratorRecord;
        }

        public boolean isReady() {
            return orchestratorRecord != null
                    && orchestratorRecord.isReady();
        }

        public boolean wasRecovered() {
            return orchestratorRecord != null
                    && orchestratorRecord.wasRecovered();
        }

        public boolean failed() {
            return orchestratorRecord == null
                    || orchestratorRecord.failed();
        }

        @Override
        public String toString() {
            return "EvolutionRecord{" +
                    "capabilityId='" +
                    capabilityId + '\'' +
                    ", ready=" +
                    isReady() +
                    '}';
        }
    }
}