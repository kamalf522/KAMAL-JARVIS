package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.CapabilityExecutor;
import com.kamal.jarvis.v2.evolution.CapabilityPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Execution Engine
 *
 * طبقة التنفيذ العليا.
 *
 * المسار:
 *
 * User Command
 *      ↓
 * CommandUnderstanding
 *      ↓
 * PlanningEngine
 *      ↓
 * ExecutionEngine
 *      ↓
 * CapabilityExecutor
 *      ↓
 * ToolRegistry / JarvisRuntime
 *      ↓
 * Tool
 *
 * ExecutionEngine لا يتجاوز Security
 * ولا يمنح صلاحيات بنفسه.
 *
 * مهم:
 * لا يتم اعتبار العملية ناجحة إلا إذا
 * رجعت CapabilityExecutor بنتيجة نجاح حقيقية.
 */
public final class ExecutionEngine {

    private static final String ENGINE_ID =
            "v2.execution_engine";

    private final PlanningEngine planningEngine;
    private final CapabilityExecutor capabilityExecutor;
    private final JarvisRuntime runtime;

    private volatile ExecutionState state =
            ExecutionState.IDLE;

    private volatile ExecutionRecord lastRecord;

    public ExecutionEngine(
            PlanningEngine planningEngine,
            CapabilityExecutor capabilityExecutor,
            JarvisRuntime runtime
    ) {

        if (planningEngine == null) {
            throw new IllegalArgumentException(
                    "planningEngine cannot be null."
            );
        }

        if (capabilityExecutor == null) {
            throw new IllegalArgumentException(
                    "capabilityExecutor cannot be null."
            );
        }

        if (runtime == null) {
            throw new IllegalArgumentException(
                    "runtime cannot be null."
            );
        }

        this.planningEngine =
                planningEngine;

        this.capabilityExecutor =
                capabilityExecutor;

        this.runtime =
                runtime;
    }

    /**
     * إنشاء الخطة ثم تنفيذها.
     *
     * هذه هي الطريقة الرئيسية التي يستعملها
     * المستوى الأعلى من JARVIS.
     */
    public synchronized JarvisResult<ExecutionRecord>
    execute(
            CommandUnderstanding.Result understanding,
            ToolContract.ToolInput input
    ) {

        if (understanding == null ||
                !understanding.isValid()) {

            state = ExecutionState.FAILED;

            ExecutionRecord record =
                    createFailureRecord(
                            null,
                            JarvisError.of(
                                    JarvisError.Type.INVALID_REQUEST,
                                    "Invalid command understanding.",
                                    ENGINE_ID
                            )
                    );

            lastRecord = record;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    record.getMessage()
            );

            return JarvisResult.failure(
                    record.getError(),
                    record.getMessage()
            );
        }

        if (input == null) {

            state = ExecutionState.FAILED;

            ExecutionRecord record =
                    createFailureRecord(
                            null,
                            JarvisError.of(
                                    JarvisError.Type.INVALID_REQUEST,
                                    "Tool input cannot be null.",
                                    ENGINE_ID
                            )
                    );

            lastRecord = record;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    record.getMessage()
            );

            return JarvisResult.failure(
                    record.getError(),
                    record.getMessage()
            );
        }

        state =
                ExecutionState.PLANNING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                "Creating execution plan."
        );

        JarvisResult<PlanningEngine.Plan>
                planningResult =
                planningEngine.createPlan(
                        understanding
                );

        if (planningResult == null) {

            return fail(
                    null,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "PlanningEngine returned no result.",
                            ENGINE_ID
                    )
            );
        }

        if (!planningResult.isSuccess()) {

            JarvisError error =
                    planningResult.getError();

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                planningResult.getMessage(),
                                ENGINE_ID
                        );
            }

            return fail(
                    null,
                    error
            );
        }

        PlanningEngine.Plan plan =
                planningResult.getData();

        if (plan == null) {

            return fail(
                    null,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "PlanningEngine returned an empty plan.",
                            ENGINE_ID
                    )
            );
        }

        state =
                ExecutionState.VALIDATING;

        JarvisResult<ExecutionRecord>
                validation =
                validatePlan(plan);

        if (validation == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Execution plan validation failed.",
                            ENGINE_ID
                    )
            );
        }

        if (!validation.isSuccess()) {

            JarvisError error =
                    validation.getError();

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.VALIDATION_FAILED,
                                validation.getMessage(),
                                ENGINE_ID
                        );
            }

            return fail(
                    plan,
                    error
            );
        }

        /*
         * طلب الصلاحية لا يتم اعتباره نجاحاً.
         *
         * وكذلك BUILD_CAPABILITY لا يتم تنفيذه
         * هنا بشكل مزيف.
         *
         * EvolutionCore هو المسؤول عن مسار البناء.
         */
        if (plan.requiresPermission()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            buildPermissionMessage(
                                    plan
                            ),
                            ENGINE_ID
                    )
            );
        }

        if (plan.requiresEvolution()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "This plan requires the evolution pipeline before execution.",
                            ENGINE_ID
                    )
            );
        }

        if (!plan.canExecute()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.TOOL_UNAVAILABLE,
                            "The current plan has no executable path.",
                            ENGINE_ID
                    )
            );
        }

        CapabilityPlan capabilityPlan =
                plan.getCapabilityPlan();

        if (capabilityPlan == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "CapabilityPlan is missing.",
                            ENGINE_ID
                    )
            );
        }

        state =
                ExecutionState.EXECUTING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                "Executing capability: "
                        + plan.getCapabilityId()
        );

        JarvisResult<
                ToolContract.ToolOutput
                > executionResult =
                capabilityExecutor.execute(
                        capabilityPlan,
                        input
                );

        if (executionResult == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EXECUTION_FAILED,
                            "CapabilityExecutor returned no result.",
                            ENGINE_ID
                    )
            );
        }

        if (!executionResult.isSuccess()) {

            JarvisError error =
                    executionResult.getError();

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.EXECUTION_FAILED,
                                executionResult.getMessage(),
                                ENGINE_ID
                        );
            }

            ExecutionRecord record =
                    createFailureRecord(
                            plan,
                            error
                    );

            lastRecord = record;
            state =
                    ExecutionState.FAILED;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    record.getMessage()
            );

            return JarvisResult.failure(
                    error,
                    record.getMessage()
            );
        }

        ToolContract.ToolOutput
                output =
                executionResult.getData();

        /*
         * النجاح الحقيقي يحتاج Output.
         *
         * إذا كان الـExecutor قال success لكن أعطى
         * output فارغ، لا نخلي Brain يعتقد أن كل شيء
         * تنفذ بلا دليل.
         */
        if (output == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EXECUTION_FAILED,
                            "Execution reported success but returned no tool output.",
                            ENGINE_ID
                    )
            );
        }

        state =
                ExecutionState.COMPLETED;

        ExecutionRecord record =
                createSuccessRecord(
                        plan,
                        output
                );

        lastRecord =
                record;

        publish(
                JarvisEvent.Type.COMMAND_COMPLETED,
                record.getMessage()
        );

        return JarvisResult.success(
                record,
                record.getMessage()
        );
    }

    /**
     * تنفيذ خطة جاهزة بدون إعادة التخطيط.
     */
    public synchronized JarvisResult<ExecutionRecord>
    executePlan(
            PlanningEngine.Plan plan,
            ToolContract.ToolInput input
    ) {

        if (plan == null) {

            return fail(
                    null,
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Plan cannot be null.",
                            ENGINE_ID
                    )
            );
        }

        if (input == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool input cannot be null.",
                            ENGINE_ID
                    )
            );
        }

        JarvisResult<ExecutionRecord>
                validation =
                validatePlan(plan);

        if (!validation.isSuccess()) {

            return fail(
                    plan,
                    validation.getError()
            );
        }

        if (plan.requiresPermission()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            buildPermissionMessage(
                                    plan
                            ),
                            ENGINE_ID
                    )
            );
        }

        if (plan.requiresEvolution()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Plan requires evolution before execution.",
                            ENGINE_ID
                    )
            );
        }

        if (!plan.canExecute()) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.TOOL_UNAVAILABLE,
                            "Plan cannot currently be executed.",
                            ENGINE_ID
                    )
            );
        }

        CapabilityPlan capabilityPlan =
                plan.getCapabilityPlan();

        if (capabilityPlan == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "CapabilityPlan is missing.",
                            ENGINE_ID
                    )
            );
        }

        state =
                ExecutionState.EXECUTING;

        JarvisResult<
                ToolContract.ToolOutput
                > result =
                capabilityExecutor.execute(
                        capabilityPlan,
                        input
                );

        if (result == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EXECUTION_FAILED,
                            "CapabilityExecutor returned no result.",
                            ENGINE_ID
                    )
            );
        }

        if (!result.isSuccess()) {

            JarvisError error =
                    result.getError();

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.EXECUTION_FAILED,
                                result.getMessage(),
                                ENGINE_ID
                        );
            }

            return fail(
                    plan,
                    error
            );
        }

        ToolContract.ToolOutput output =
                result.getData();

        if (output == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EXECUTION_FAILED,
                            "Execution succeeded without tool output.",
                            ENGINE_ID
                    )
            );
        }

        state =
                ExecutionState.COMPLETED;

        ExecutionRecord record =
                createSuccessRecord(
                        plan,
                        output
                );

        lastRecord =
                record;

        publish(
                JarvisEvent.Type.COMMAND_COMPLETED,
                record.getMessage()
        );

        return JarvisResult.success(
                record,
                record.getMessage()
        );
    }

    /**
     * فحص الخطة قبل التنفيذ.
     */
    private JarvisResult<ExecutionRecord>
    validatePlan(
            PlanningEngine.Plan plan
    ) {

        if (plan == null) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Plan is null.",
                            ENGINE_ID
                    )
            );
        }

        if (plan.getCapabilityPlan() == null) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "CapabilityPlan is missing.",
                            ENGINE_ID
                    )
            );
        }

        if (plan.getCapabilityId() == null ||
                plan.getCapabilityId()
                        .trim()
                        .isEmpty()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Capability ID is empty.",
                            ENGINE_ID
                    )
            );
        }

        if (plan.getToolIds() == null ||
                plan.getToolIds().isEmpty()) {

            if (!plan.requiresEvolution()) {

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.TOOL_UNAVAILABLE,
                                "No execution tools exist in the plan.",
                                ENGINE_ID
                        )
                );
            }
        }

        return JarvisResult.success(
                null,
                "Plan validation passed."
        );
    }

    private ExecutionRecord createSuccessRecord(
            PlanningEngine.Plan plan,
            ToolContract.ToolOutput output
    ) {

        String message =
                "Capability executed successfully: "
                        + plan.getCapabilityId();

        return new ExecutionRecord(
                true,
                plan.getCapabilityId(),
                plan.getAction(),
                plan.getToolIds(),
                output,
                null,
                message
        );
    }

    private ExecutionRecord createFailureRecord(
            PlanningEngine.Plan plan,
            JarvisError error
    ) {

        String capabilityId =
                plan == null
                        ? ""
                        : plan.getCapabilityId();

        PlanningEngine.PlanAction action =
                plan == null
                        ? PlanningEngine.PlanAction.UNAVAILABLE
                        : plan.getAction();

        List<String> tools =
                plan == null
                        ? Collections.emptyList()
                        : plan.getToolIds();

        String message =
                error == null
                        ? "Execution failed."
                        : error.getMessage();

        return new ExecutionRecord(
                false,
                capabilityId,
                action,
                tools,
                null,
                error,
                message
        );
    }

    private JarvisResult<ExecutionRecord> fail(
            PlanningEngine.Plan plan,
            JarvisError error
    ) {

        if (error == null) {

            error =
                    JarvisError.of(
                            JarvisError.Type.EXECUTION_FAILED,
                            "Unknown execution failure.",
                            ENGINE_ID
                    );
        }

        state =
                ExecutionState.FAILED;

        ExecutionRecord record =
                createFailureRecord(
                        plan,
                        error
                );

        lastRecord =
                record;

        publish(
                JarvisEvent.Type.COMMAND_FAILED,
                record.getMessage()
        );

        return JarvisResult.failure(
                error,
                record.getMessage()
        );
    }

    private String buildPermissionMessage(
            PlanningEngine.Plan plan
    ) {

        if (plan == null ||
                plan.getMissingPermissions() == null ||
                plan.getMissingPermissions().isEmpty()) {

            return "Execution requires authorization or an additional capability.";
        }

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Missing permissions: "
        );

        int index = 0;

        for (
                com.kamal.jarvis.v2.permissions
                        .CapabilityPermission permission
                : plan.getMissingPermissions()
        ) {

            if (index > 0) {
                builder.append(", ");
            }

            builder.append(
                    permission.getId()
            );

            index++;
        }

        return builder.toString();
    }

    private void publish(
            JarvisEvent.Type type,
            String message
    ) {

        try {

            runtime.publishEvent(
                    type,
                    ENGINE_ID,
                    message,
                    null
            );

        } catch (Exception ignored) {
            /*
             * Logging failure must never
             * break execution flow.
             */
        }
    }

    public ExecutionState getState() {
        return state;
    }

    public String getStatus() {
        return state.name();
    }

    public boolean isExecuting() {
        return state ==
                ExecutionState.EXECUTING;
    }

    public boolean isCompleted() {
        return state ==
                ExecutionState.COMPLETED;
    }

    public boolean hasFailed() {
        return state ==
                ExecutionState.FAILED;
    }

    public ExecutionRecord getLastRecord() {
        return lastRecord;
    }

    public PlanningEngine getPlanningEngine() {
        return planningEngine;
    }

    public CapabilityExecutor
    getCapabilityExecutor() {
        return capabilityExecutor;
    }

    public JarvisRuntime getRuntime() {
        return runtime;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    public enum ExecutionState {

        IDLE,

        PLANNING,

        VALIDATING,

        EXECUTING,

        COMPLETED,

        FAILED
    }

    /**
     * السجل النهائي للعملية.
     */
    public static final class ExecutionRecord {

        private final boolean successful;
        private final String capabilityId;
        private final PlanningEngine.PlanAction action;
        private final List<String> toolIds;
        private final ToolContract.ToolOutput toolOutput;
        private final JarvisError error;
        private final String message;

        private ExecutionRecord(
                boolean successful,
                String capabilityId,
                PlanningEngine.PlanAction action,
                List<String> toolIds,
                ToolContract.ToolOutput toolOutput,
                JarvisError error,
                String message
        ) {

            this.successful =
                    successful;

            this.capabilityId =
                    capabilityId == null
                            ? ""
                            : capabilityId;

            this.action =
                    action == null
                            ? PlanningEngine.PlanAction.UNAVAILABLE
                            : action;

            this.toolIds =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    toolIds == null
                                            ? Collections.emptyList()
                                            : toolIds
                            )
                    );

            this.toolOutput =
                    toolOutput;

            this.error =
                    error;

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public boolean isFailed() {
            return !successful;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public PlanningEngine.PlanAction
        getAction() {
            return action;
        }

        public List<String> getToolIds() {
            return toolIds;
        }

        public ToolContract.ToolOutput
        getToolOutput() {
            return toolOutput;
        }

        public JarvisError getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasToolOutput() {
            return toolOutput != null;
        }

        @Override
        public String toString() {

            return "ExecutionRecord{" +
                    "successful=" +
                    successful +
                    ", capabilityId='" +
                    capabilityId + '\'' +
                    ", action=" +
                    action +
                    ", toolIds=" +
                    toolIds +
                    ", message='" +
                    message + '\'' +
                    '}';
        }
    }
}