package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.CapabilityExecutor;
import com.kamal.jarvis.v2.evolution.CapabilityPlan;
import com.kamal.jarvis.v2.evolution.EvolutionCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Execution Engine
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
 * ┌─────────────────────────────┐
 * │ DIRECT / ALTERNATIVE        │
 * │        ↓                    │
 * │ CapabilityExecutor          │
 * │                             │
 * │ BUILD / EVOLUTION           │
 * │        ↓                    │
 * │ EvolutionCore               │
 * └─────────────────────────────┘
 *
 * ExecutionEngine لا يمنح صلاحيات بنفسه،
 * ولا ينشئ أدوات وهمية.
 */
public final class ExecutionEngine {

    private static final String ENGINE_ID =
            "v2.execution_engine";

    private final PlanningEngine planningEngine;
    private final CapabilityExecutor capabilityExecutor;
    private final EvolutionCore evolutionCore;
    private final JarvisRuntime runtime;

    private volatile ExecutionState state =
            ExecutionState.IDLE;

    private volatile ExecutionRecord lastRecord;

    public ExecutionEngine(
            PlanningEngine planningEngine,
            CapabilityExecutor capabilityExecutor,
            EvolutionCore evolutionCore,
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

        if (evolutionCore == null) {
            throw new IllegalArgumentException(
                    "evolutionCore cannot be null."
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

        this.evolutionCore =
                evolutionCore;

        this.runtime =
                runtime;
    }

    /**
     * المسار الرئيسي:
     *
     * فهم الطلب
     * → تخطيط
     * → تنفيذ أو Evolution
     */
    public synchronized JarvisResult<ExecutionRecord> execute(
            CommandUnderstanding.Result understanding,
            ToolContract.ToolInput input
    ) {

        if (understanding == null ||
                !understanding.isValid()) {

            return fail(
                    null,
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Invalid command understanding.",
                            ENGINE_ID
                    )
            );
        }

        if (input == null) {

            return fail(
                    null,
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool input cannot be null.",
                            ENGINE_ID
                    )
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

        return executePlan(
                plan,
                input
        );
    }

    /**
     * تنفيذ Plan موجودة مسبقاً.
     */
    public synchronized JarvisResult<ExecutionRecord> executePlan(
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

        state =
                ExecutionState.VALIDATING;

        JarvisResult<Void> validation =
                validatePlan(
                        plan
                );

        if (validation == null ||
                !validation.isSuccess()) {

            JarvisError error =
                    validation == null
                            ? JarvisError.of(
                                    JarvisError.Type.VALIDATION_FAILED,
                                    "Plan validation returned no result.",
                                    ENGINE_ID
                            )
                            : validation.getError();

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
         * ==========================================
         * PERMISSION
         * ==========================================
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

        /*
         * ==========================================
         * EVOLUTION
         * ==========================================
         *
         * هنا كان ExecutionEngine القديم
         * كيتوقف.
         *
         * دابا كنمررو الطلب لـ EvolutionCore.
         */

        if (plan.requiresEvolution() ||
                plan.isBuildRequired()) {

            return executeThroughEvolution(
                    plan,
                    input
            );
        }

        /*
         * ==========================================
         * DIRECT / ALTERNATIVE
         * ==========================================
         */

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

        JarvisResult<ToolContract.ToolOutput>
                executionResult =
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

            return fail(
                    plan,
                    error
            );
        }

        ToolContract.ToolOutput output =
                executionResult.getData();

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
                        output,
                        ExecutionMode.DIRECT
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
     * يرسل Capability التي تحتاج Evolution
     * إلى EvolutionCore.
     */
    private JarvisResult<ExecutionRecord>
    executeThroughEvolution(
            PlanningEngine.Plan plan,
            ToolContract.ToolInput input
    ) {

        state =
                ExecutionState.EVOLVING;

        publish(
                JarvisEvent.Type.EVOLUTION_STARTED,
                "Evolution required for capability: "
                        + plan.getCapabilityId()
        );

        if (plan.getRequirement() == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "CapabilityRequirement is missing.",
                            ENGINE_ID
                    )
            );
        }

        JarvisResult<
                EvolutionCore.EvolutionExecutionResult
                > evolutionResult =
                evolutionCore.execute(
                        plan.getRequirement(),
                        input
                );

        if (evolutionResult == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "EvolutionCore returned no result.",
                            ENGINE_ID
                    )
            );
        }

        if (!evolutionResult.isSuccess()) {

            JarvisError error =
                    evolutionResult.getError();

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                evolutionResult.getMessage(),
                                ENGINE_ID
                        );
            }

            return fail(
                    plan,
                    error
            );
        }

        EvolutionCore.EvolutionExecutionResult
                result =
                evolutionResult.getData();

        if (result == null) {

            return fail(
                    plan,
                    JarvisError.of(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "EvolutionCore returned empty execution data.",
                            ENGINE_ID
                    )
            );
        }

        /*
         * Evolution قد تكون:
         *
         * EXECUTED
         * BUILT
         * ACTIVATED
         * PERMISSION_REQUIRED
         */

        if (result.needsPermission()) {

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

        /*
         * إذا Evolution نفذت Tool مباشرة.
         */
        if (result.wasExecuted()) {

            ToolContract.ToolOutput output =
                    result.getToolOutput();

            if (output == null) {

                return fail(
                        plan,
                        JarvisError.of(
                                JarvisError.Type.EXECUTION_FAILED,
                                "Evolution executed capability without output.",
                                ENGINE_ID
                        )
                );
            }

            state =
                    ExecutionState.COMPLETED;

            ExecutionRecord record =
                    createSuccessRecord(
                            plan,
                            output,
                            ExecutionMode.EVOLUTION_EXECUTED
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

        /*
         * إذا Evolution بنت وقد فعلت Capability.
         *
         * إذا activation موجود، نتحقق منه.
         */
        if (result.wasActivated()) {

            if (result.getActivationRecord() == null ||
                    !result.getActivationRecord().isActive()) {

                return fail(
                        plan,
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Evolution reported activation without an active capability.",
                                ENGINE_ID
                        )
                );
            }

            state =
                    ExecutionState.COMPLETED;

            ExecutionRecord record =
                    createEvolutionRecord(
                            plan,
                            result,
                            ExecutionMode.EVOLUTION_ACTIVATED
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

        /*
         * Evolution وصل إلى BUILD/READY،
         * ولكن لا توجد Tool executable بعد.
         *
         * لا نكذب على Brain ونقول نفذ.
         */
        if (result.wasBuilt()) {

            state =
                    ExecutionState.COMPLETED;

            ExecutionRecord record =
                    createEvolutionRecord(
                            plan,
                            result,
                            ExecutionMode.EVOLUTION_BUILT
                    );

            lastRecord =
                    record;

            publish(
                    JarvisEvent.Type.EVOLUTION_COMPLETED,
                    record.getMessage()
            );

            return JarvisResult.success(
                    record,
                    record.getMessage()
            );
        }

        return fail(
                plan,
                JarvisError.of(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Evolution finished without a recognized execution outcome.",
                        ENGINE_ID
                )
        );
    }

    /**
     * التحقق من الخطة قبل التنفيذ.
     */
    private JarvisResult<Void> validatePlan(
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

        /*
         * BUILD يمكن أن يكون بلا Tool،
         * لأن Evolution هو اللي غادي يحاول يبنيها.
         */
        if (plan.getToolIds() == null ||
                plan.getToolIds().isEmpty()) {

            if (!plan.requiresEvolution() &&
                    !plan.isBuildRequired()) {

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
            ToolContract.ToolOutput output,
            ExecutionMode mode
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
                message,
                mode
        );
    }

    private ExecutionRecord createEvolutionRecord(
            PlanningEngine.Plan plan,
            EvolutionCore.EvolutionExecutionResult result,
            ExecutionMode mode
    ) {

        String message =
                result.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            message =
                    "Evolution completed for capability: "
                            + plan.getCapabilityId();
        }

        return new ExecutionRecord(
                true,
                plan.getCapabilityId(),
                plan.getAction(),
                plan.getToolIds(),
                result.getToolOutput(),
                null,
                message,
                mode
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
                message,
                ExecutionMode.FAILED
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
             * فشل التسجيل لا يجب أن يوقف التنفيذ.
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

    public boolean isEvolving() {
        return state ==
                ExecutionState.EVOLVING;
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

    public CapabilityExecutor getCapabilityExecutor() {
        return capabilityExecutor;
    }

    public EvolutionCore getEvolutionCore() {
        return evolutionCore;
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

        EVOLVING,

        COMPLETED,

        FAILED
    }

    public enum ExecutionMode {

        DIRECT,

        EVOLUTION_EXECUTED,

        EVOLUTION_BUILT,

        EVOLUTION_ACTIVATED,

        FAILED
    }

    public static final class ExecutionRecord {

        private final boolean successful;
        private final String capabilityId;
        private final PlanningEngine.PlanAction action;
        private final List<String> toolIds;
        private final ToolContract.ToolOutput toolOutput;
        private final JarvisError error;
        private final String message;
        private final ExecutionMode mode;

        private ExecutionRecord(
                boolean successful,
                String capabilityId,
                PlanningEngine.PlanAction action,
                List<String> toolIds,
                ToolContract.ToolOutput toolOutput,
                JarvisError error,
                String message,
                ExecutionMode mode
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

            this.mode =
                    mode == null
                            ? ExecutionMode.FAILED
                            : mode;
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

        public PlanningEngine.PlanAction getAction() {
            return action;
        }

        public List<String> getToolIds() {
            return toolIds;
        }

        public ToolContract.ToolOutput getToolOutput() {
            return toolOutput;
        }

        public JarvisError getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public ExecutionMode getMode() {
            return mode;
        }

        public boolean hasToolOutput() {
            return toolOutput != null;
        }

        public boolean wasDirectExecution() {
            return mode ==
                    ExecutionMode.DIRECT;
        }

        public boolean wasEvolutionExecuted() {
            return mode ==
                    ExecutionMode.EVOLUTION_EXECUTED;
        }

        public boolean wasEvolutionBuilt() {
            return mode ==
                    ExecutionMode.EVOLUTION_BUILT;
        }

        public boolean wasEvolutionActivated() {
            return mode ==
                    ExecutionMode.EVOLUTION_ACTIVATED;
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
                    ", mode=" +
                    mode +
                    ", message='" +
                    message + '\'' +
                    '}';
        }
    }
}