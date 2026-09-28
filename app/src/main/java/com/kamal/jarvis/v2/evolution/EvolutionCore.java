package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;

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
 * المسؤوليات:
 *
 * 1. استقبال الهدف المطلوب.
 * 2. تحليل القدرات الموجودة.
 * 3. اختيار مسار التنفيذ.
 * 4. اختيار البديل عند توفره.
 * 5. تحديد الصلاحيات الناقصة.
 * 6. إنشاء CapabilitySpec عند الحاجة.
 * 7. إطلاق EvolutionOrchestrator.
 * 8. استقبال نتيجة Build/Test/Recovery.
 * 9. تسجيل نتائج Evolution.
 *
 * ملاحظة مهمة:
 *
 * EvolutionCore لا يمنح Android permissions بنفسه،
 * ولا يغير OwnerSecurityBoundary.
 *
 * هو العقل الذي يقرر المسار،
 * بينما كل مكون مسؤول عن اختصاصه.
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
     * يحلل CapabilityRequirement بدون تنفيذ.
     */
    public synchronized JarvisResult<EvolutionAnalysis> analyze(
            CapabilityRequirement requirement
    ) {
        if (requirement == null) {
            state = EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
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

        if (discovered == null) {
            state = EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability discovery returned no result."
            );
        }

        CapabilityPlan plan =
                CapabilityPlan.fromDiscovery(
                        discovered
                );

        if (plan == null) {
            state = EvolutionState.FAILED;

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
     * ينفذ CapabilityRequirement.
     *
     * المسارات:
     *
     * موجودة مباشرة
     *      ↓
     * تنفيذ
     *
     * بديل موجود
     *      ↓
     * تنفيذ البديل
     *
     * Permission ناقصة
     *      ↓
     * يرجع المتطلبات للنظام الأعلى
     *
     * Capability ناقصة
     *      ↓
     * CapabilitySpec
     *      ↓
     * EvolutionOrchestrator
     *      ↓
     * Build
     *      ↓
     * Test
     *      ↓
     * Recovery عند الفشل
     */
    public synchronized JarvisResult<EvolutionExecutionResult> execute(
            CapabilityRequirement requirement,
            ToolContract.ToolInput input
    ) {
        if (requirement == null) {
            state = EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
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

        if (analysis == null) {
            state = EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis returned no data."
            );
        }

        CapabilityPlan plan =
                analysis.getPlan();

        if (plan == null) {
            state = EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis contains no plan."
            );
        }

        /*
         * =========================================================
         * 1. DIRECT / ALTERNATIVE EXECUTION
         * =========================================================
         */
        if (plan.getAction()
                == CapabilityPlan.Action.EXECUTE_DIRECT
                || plan.getAction()
                == CapabilityPlan.Action.EXECUTE_ALTERNATIVE) {

            state =
                    EvolutionState.EXECUTING;

            JarvisResult<ToolContract.ToolOutput>
                    executionResult =
                    executor.execute(
                            plan,
                            input
                    );

            if (!executionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return JarvisResult.failure(
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
         * =========================================================
         * 2. PERMISSION REQUIRED
         * =========================================================
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
                    "Additional permission or capability is required."
            );
        }

        /*
         * =========================================================
         * 3. CAPABILITY MUST BE BUILT
         * =========================================================
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
                                "EvolutionCore",
                                exception
                        )
                );
            }

            JarvisResult<
                    EvolutionOrchestrator.EvolutionRecord
                    > evolutionResult =
                    orchestrator.evolve(spec);

            if (!evolutionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return JarvisResult.failure(
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

            /*
             * لا نعتبر Capability جاهزة
             * إلا إذا قال Orchestrator أنها جاهزة فعلاً.
             */
            if (!evolutionRecord.isReady()) {

                state =
                        EvolutionState.FAILED;

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Evolution completed without producing a ready capability."
                );
            }

            state =
                    EvolutionState.COMPLETED;

            saveRecord(
                    requirement.getCapabilityId(),
                    evolutionRecord
            );

            EvolutionExecutionResult built =
                    EvolutionExecutionResult.built(
                            plan,
                            evolutionRecord,
                            "Capability was built and passed the current evolution checks."
                    );

            return JarvisResult.success(
                    built,
                    built.getMessage()
            );
        }

        /*
         * =========================================================
         * 4. UNAVAILABLE
         * =========================================================
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
     * يحول Requirement إلى CapabilitySpec كاملة.
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

        /*
         * الهدف الحقيقي.
         */
        builder.goal(
                requirement.getDescription()
        );

        /*
         * وصف داخلي.
         */
        builder.description(
                "Capability generated by JARVIS Evolution Core."
        );

        /*
         * الصلاحيات المطلوبة.
         *
         * CapabilityPermission هي Enum،
         * لذلك لا نحولها إلى String.
         */
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

        /*
         * الأدوات المفضلة.
         */
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

        /*
         * الأدوات البديلة.
         */
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

        /*
         * أدوات التنفيذ الحالية تعتبر
         * أيضاً متطلبات مفيدة للـCapability.
         */
        if (plan != null &&
                plan.getToolIds() != null) {

            for (String toolId :
                    plan.getToolIds()) {

                builder.requireTool(
                        toolId
                );
            }
        }

        /*
         * Owner authorization.
         */
        if (requirement.isOwnerAuthorizationRequired()) {

            builder.requireOwnerAuthorization();
        }

        /*
         * إذا سمح Requirement ببناء بديل،
         * نسمح للـSelfBuilder بتعديل ملفات المشروع
         * داخل الحدود الأمنية المسموح بها.
         */
        if (requirement.canBuildAlternative()) {

            builder.allowProjectModification();
        }

        /*
         * أي Capability جديدة يجب أن تدخل
         * Build/Test pipeline.
         */
        builder.requireBuild();
        builder.requireTests();

        /*
         * معايير النجاح الأساسية.
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
        if (capabilityId == null ||
                capabilityId.trim().isEmpty() ||
                record == null) {
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
                        "EvolutionCore"
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
                || state ==
                EvolutionState.EXECUTING
                || state ==
                EvolutionState.BUILDING;
    }

    public boolean isReady() {
        return state ==
                EvolutionState.READY_TO_EXECUTE
                || state ==
                EvolutionState.COMPLETED;
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

        private final CapabilityDiscovery.DiscoveryResult
                discovery;

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
                    && plan.getAction()
                    == CapabilityPlan.Action.BUILD_CAPABILITY;
        }

        public boolean canExecuteDirectly() {
            return plan != null
                    && plan.getAction()
                    == CapabilityPlan.Action.EXECUTE_DIRECT;
        }

        public boolean usesAlternative() {
            return plan != null
                    && plan.getAction()
                    == CapabilityPlan.Action.EXECUTE_ALTERNATIVE;
        }

        public boolean needsPermission() {
            return plan != null
                    && plan.getAction()
                    == CapabilityPlan.Action.REQUEST_PERMISSION;
        }

        public boolean isUnavailable() {
            return plan == null
                    || plan.getAction()
                    == CapabilityPlan.Action.UNAVAILABLE;
        }

        @Override
        public String toString() {
            return "EvolutionAnalysis{" +
                    "capabilityId='" +
                    requirement.getCapabilityId() +
                    '\'' +
                    ", action=" +
                    (plan == null
                            ? "null"
                            : plan.getAction()) +
                    '}';
        }
    }

    /**
     * نتيجة تنفيذ Evolution.
     */
    public static final class EvolutionExecutionResult {

        private final ExecutionOutcome outcome;

        private final CapabilityPlan plan;

        private final ToolContract.ToolOutput
                toolOutput;

        private final EvolutionOrchestrator.EvolutionRecord
                evolutionRecord;

        private final List<CapabilityPermission>
                missingPermissions;

        private final String message;

        private EvolutionExecutionResult(
                ExecutionOutcome outcome,
                CapabilityPlan plan,
                ToolContract.ToolOutput toolOutput,
                EvolutionOrchestrator.EvolutionRecord evolutionRecord,
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
                    list,
                    "Permission or capability is required."
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

        public ToolContract.ToolOutput
        getToolOutput() {
            return toolOutput;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getEvolutionRecord() {
            return evolutionRecord;
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
                    message +
                    '\'' +
                    '}';
        }
    }

    public enum ExecutionOutcome {

        EXECUTED,

        BUILT,

        PERMISSION_REQUIRED
    }

    /**
     * سجل Evolution داخل Core.
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
            this.capabilityId =
                    capabilityId;

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
                    capabilityId +
                    '\'' +
                    ", ready=" +
                    isReady() +
                    '}';
        }
    }
}