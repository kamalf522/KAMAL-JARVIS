package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.EvolutionCore;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeAcquisitionEngine;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeItem;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeLearningEngine;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeMemory;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeVerificationEngine;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Central Brain
 *
 * المسار المركزي:
 *
 * User
 *   ↓
 * CommandUnderstanding
 *   ↓
 * Learning / Knowledge
 *   ↓
 * ExecutionEngine
 *   ↓
 * PlanningEngine
 *   ↓
 * Direct Tool
 *      أو
 * EvolutionCore
 *   ↓
 * Build / Test / Activate / Execute
 *   ↓
 * Result
 *   ↓
 * Memory
 *
 * هذا العقل لا يعتمد على لائحة مغلقة من الأوامر.
 */
public final class JarvisBrain {

    private static final String BRAIN_ID =
            "v2.jarvis_brain";

    private final EvolutionCore evolutionCore;
    private final JarvisRuntime runtime;
    private final CommandUnderstanding understanding;
    private final KnowledgeLearningEngine learningEngine;
    private final ExecutionEngine executionEngine;

    private volatile BrainState state =
            BrainState.IDLE;

    private volatile BrainResponse lastResponse;

    /**
     * Constructor كامل.
     */
    public JarvisBrain(
            EvolutionCore evolutionCore,
            JarvisRuntime runtime,
            KnowledgeLearningEngine learningEngine,
            ExecutionEngine executionEngine
    ) {

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

        if (executionEngine == null) {
            throw new IllegalArgumentException(
                    "executionEngine cannot be null."
            );
        }

        this.evolutionCore =
                evolutionCore;

        this.runtime =
                runtime;

        this.understanding =
                new CommandUnderstanding();

        this.learningEngine =
                learningEngine == null
                        ? createDefaultLearningEngine()
                        : learningEngine;

        this.executionEngine =
                executionEngine;
    }

    /**
     * إنشاء نظام التعلم الافتراضي.
     *
     * المصادر يتم اكتشافها من طرف
     * KnowledgeAcquisitionEngine.
     */
    private static KnowledgeLearningEngine
    createDefaultLearningEngine() {

        return new KnowledgeLearningEngine(
                new KnowledgeAcquisitionEngine(),
                new KnowledgeVerificationEngine(),
                new KnowledgeMemory()
        );
    }

    /**
     * نقطة الدخول الرئيسية.
     */
    public synchronized JarvisResult<BrainResponse>
    process(
            String userCommand
    ) {

        if (userCommand == null ||
                userCommand.trim().isEmpty()) {

            state =
                    BrainState.FAILED;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Empty command."
            );

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "JARVIS received an empty command."
            );
        }

        state =
                BrainState.UNDERSTANDING;

        publish(
                JarvisEvent.Type.COMMAND_RECEIVED,
                userCommand
        );

        /*
         * ==========================================
         * 1. فهم الطلب
         * ==========================================
         */

        CommandUnderstanding.Result understood =
                understanding.analyze(
                        userCommand
                );

        if (understood == null ||
                !understood.isValid()) {

            state =
                    BrainState.FAILED;

            String message =
                    understood == null
                            ? "JARVIS could not understand the request."
                            : understood.getMessage();

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    message
            );
        }

        String normalized =
                understood.getNormalizedCommand();

        String goal =
                understood.getGoal();

        if (goal == null ||
                goal.trim().isEmpty()) {

            goal =
                    userCommand;
        }

        /*
         * ==========================================
         * 2. محادثة عادية
         * ==========================================
         */

        if (understood.isConversational() ||
                isIdentityRequest(understood)) {

            BrainResponse response =
                    createConversationResponse(
                            userCommand,
                            normalized,
                            understood
                    );

            lastResponse =
                    response;

            state =
                    BrainState.COMPLETED;

            publish(
                    JarvisEvent.Type.COMMAND_COMPLETED,
                    response.getMessage()
            );

            return JarvisResult.success(
                    response,
                    response.getMessage()
            );
        }

        /*
         * ==========================================
         * 3. سؤال معرفي
         * ==========================================
         *
         * الإنترنت
         * ↓
         * اكتشاف المصادر
         * ↓
         * Trust
         * ↓
         * Verification
         * ↓
         * Memory
         */

        if (understood.getIntentType() ==
                CommandUnderstanding.IntentType.INFORMATION
                &&
                !understood.isEvolutionRequest()) {

            BrainResponse response =
                    answerWithKnowledge(
                            userCommand,
                            normalized,
                            understood,
                            goal
                    );

            lastResponse =
                    response;

            state =
                    BrainState.COMPLETED;

            publish(
                    JarvisEvent.Type.COMMAND_COMPLETED,
                    response.getMessage()
            );

            return JarvisResult.success(
                    response,
                    response.getMessage()
            );
        }

        /*
         * ==========================================
         * 4. إنشاء ToolInput غني
         * ==========================================
         */

        ToolContract.ToolInput input =
                createToolInput(
                        userCommand,
                        normalized,
                        goal,
                        understood
                );

        /*
         * ==========================================
         * 5. ExecutionEngine
         * ==========================================
         *
         * ExecutionEngine هو المسؤول عن:
         *
         * Planning
         * ↓
         * Direct execution
         * أو
         * EvolutionCore
         */

        state =
                BrainState.EXECUTING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                goal
        );

        JarvisResult<
                ExecutionEngine.ExecutionRecord
                > executionResult =
                executionEngine.execute(
                        understood,
                        input
                );

        /*
         * ==========================================
         * 6. إذا لم توجد القدرة
         * ==========================================
         *
         * نتعلم من الإنترنت ثم نحاول مرة ثانية.
         */

        if (shouldLearnBeforeRetry(executionResult)) {

            state =
                    BrainState.LEARNING;

            publish(
                    JarvisEvent.Type.COMMAND_STARTED,
                    "JARVIS is learning what is missing."
            );

            try {

                learningEngine.learn(
                        goal
                );

                KnowledgeItem learned =
                        learningEngine.recallBest(
                                goal
                        );

                if (learned != null &&
                        learned.isVerified()) {

                    input =
                            addKnowledgeToInput(
                                    input,
                                    learned
                            );

                    state =
                            BrainState.EXECUTING;

                    JarvisResult<
                            ExecutionEngine.ExecutionRecord
                            > retryResult =
                            executionEngine.execute(
                                    understood,
                                    input
                            );

                    if (retryResult != null &&
                            retryResult.isSuccess()) {

                        executionResult =
                                retryResult;
                    }
                }

            } catch (Exception ignored) {

                /*
                 * فشل التعلم لا يوقف العقل.
                 */
            }
        }

        /*
         * ==========================================
         * 7. النتيجة
         * ==========================================
         */

        if (executionResult == null) {

            state =
                    BrainState.FAILED;

            return failure(
                    JarvisError.Type.EXECUTION_FAILED,
                    "ExecutionEngine returned no result."
            );
        }

        if (!executionResult.isSuccess()) {

            state =
                    BrainState.FAILED;

            JarvisError error =
                    executionResult.getError();

            String message =
                    executionResult.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        error == null
                                ? "JARVIS execution failed."
                                : error.getMessage();
            }

            if (error == null) {

                error =
                        JarvisError.of(
                                JarvisError.Type.EXECUTION_FAILED,
                                message,
                                BRAIN_ID
                        );
            }

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    message
            );

            return JarvisResult.failure(
                    error,
                    message
            );
        }

        ExecutionEngine.ExecutionRecord record =
                executionResult.getData();

        if (record == null) {

            state =
                    BrainState.FAILED;

            return failure(
                    JarvisError.Type.EXECUTION_FAILED,
                    "ExecutionEngine returned empty execution record."
            );
        }

        /*
         * ==========================================
         * 8. تحويل ExecutionRecord إلى BrainResponse
         * ==========================================
         */

        BrainResponse response =
                createResponse(
                        userCommand,
                        normalized,
                        understood,
                        record
                );

        lastResponse =
                response;

        if (response.isSuccessful()) {

            state =
                    BrainState.COMPLETED;

            publish(
                    JarvisEvent.Type.COMMAND_COMPLETED,
                    response.getMessage()
            );

            return JarvisResult.success(
                    response,
                    response.getMessage()
            );
        }

        state =
                BrainState.WAITING;

        return JarvisResult.success(
                response,
                response.getMessage()
        );
    }

    /**
     * إنشاء ToolInput موحد لكل الطلبات.
     */
    private ToolContract.ToolInput
    createToolInput(
            String originalCommand,
            String normalized,
            String goal,
            CommandUnderstanding.Result understood
    ) {

        Map<String, Object> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "original_command",
                originalCommand
        );

        parameters.put(
                "normalized_command",
                normalized
        );

        parameters.put(
                "goal",
                goal
        );

        parameters.put(
                "capability_id",
                understood.getCapabilityId()
        );

        parameters.put(
                "intent_type",
                understood
                        .getIntentType()
                        .name()
        );

        parameters.put(
                "action_type",
                understood
                        .getActionType()
                        .name()
        );

        parameters.put(
                "question",
                understood.isQuestion()
        );

        parameters.put(
                "conversational",
                understood.isConversational()
        );

        parameters.put(
                "self_referential",
                understood.isSelfReferential()
        );

        parameters.put(
                "evolution_request",
                understood.isEvolutionRequest()
        );

        parameters.put(
                "destructive",
                understood.isDestructive()
        );

        parameters.put(
                "keywords",
                new ArrayList<>(
                        understood.getKeywords()
                )
        );

        parameters.put(
                "targets",
                new ArrayList<>(
                        understood.getTargets()
                )
        );

        return new ToolContract.ToolInput(
                understood
                        .getActionType()
                        .name()
                        .toLowerCase(),
                parameters
        );
    }

    /**
     * إضافة المعرفة الموثوقة إلى الطلب.
     */
    private ToolContract.ToolInput
    addKnowledgeToInput(
            ToolContract.ToolInput input,
            KnowledgeItem knowledge
    ) {

        if (input == null ||
                knowledge == null) {

            return input;
        }

        Map<String, Object> parameters =
                new LinkedHashMap<>();

        Map<String, Object> existing =
                input.getParameters();

        if (existing != null) {
            parameters.putAll(
                    existing
            );
        }

        parameters.put(
                "verified_knowledge",
                knowledge.getContent()
        );

        parameters.put(
                "knowledge_source",
                knowledge.getSourceId()
        );

        parameters.put(
                "knowledge_location",
                knowledge.getSourceLocation()
        );

        parameters.put(
                "knowledge_confidence",
                knowledge.getConfidence()
        );

        parameters.put(
                "knowledge_status",
                knowledge.getStatus().name()
        );

        return new ToolContract.ToolInput(
                input.getAction(),
                parameters
        );
    }

    /**
     * واش خاصنا نتعلم قبل إعادة المحاولة؟
     */
    private boolean shouldLearnBeforeRetry(
            JarvisResult<
                    ExecutionEngine.ExecutionRecord
                    > result
    ) {

        if (result == null) {
            return true;
        }

        if (!result.isSuccess()) {

            JarvisError error =
                    result.getError();

            if (error == null) {
                return true;
            }

            return error.is(
                    JarvisError.Type.TOOL_UNAVAILABLE
            )
                    ||
                    error.is(
                            JarvisError.Type.EVOLUTION_FAILED
                    );
        }

        ExecutionEngine.ExecutionRecord record =
                result.getData();

        if (record == null) {
            return true;
        }

        return record.getMode() ==
                ExecutionEngine.ExecutionMode.FAILED;
    }

    /**
     * الإجابة على سؤال معرفي.
     */
    private BrainResponse answerWithKnowledge(
            String originalCommand,
            String normalized,
            CommandUnderstanding.Result understood,
            String goal
    ) {

        try {

            state =
                    BrainState.LEARNING;

            KnowledgeLearningEngine.LearningResult
                    learningResult =
                    learningEngine.learn(
                            goal
                    );

            KnowledgeItem best =
                    learningEngine.recallBest(
                            goal
                    );

            if (best != null &&
                    best.isVerified()) {

                String content =
                        best.getContent();

                if (content == null) {
                    content = "";
                }

                if (content.length() > 5000) {

                    content =
                            content.substring(
                                    0,
                                    5000
                            );
                }

                String message =
                        "تعلمت وتحققت من المعلومة:\n\n"
                                + content;

                return new BrainResponse(
                        originalCommand,
                        normalized,
                        understood.getIntentType(),
                        understood
                                .getActionType()
                                .name(),
                        understood.getCapabilityId(),
                        EvolutionCore.ExecutionOutcome.EXECUTED,
                        message,
                        true,
                        Collections.emptyList(),
                        null
                );
            }

            String reason =
                    learningResult == null
                            ? ""
                            : learningResult.getMessage();

            return new BrainResponse(
                    originalCommand,
                    normalized,
                    understood.getIntentType(),
                    understood
                            .getActionType()
                            .name(),
                    understood.getCapabilityId(),
                    EvolutionCore.ExecutionOutcome.EXECUTED,
                    "بحثت فشبكة الإنترنت وتحققت من النتائج، ولكن ما لقيتش معرفة موثوقة كافية باش نعطيك جواب بلا تخمين."
                            + (
                            reason == null ||
                                    reason.trim().isEmpty()
                                    ? ""
                                    : "\n" + reason
                    ),
                    true,
                    Collections.emptyList(),
                    null
            );

        } catch (Exception exception) {

            return new BrainResponse(
                    originalCommand,
                    normalized,
                    understood.getIntentType(),
                    understood
                            .getActionType()
                            .name(),
                    understood.getCapabilityId(),
                    EvolutionCore.ExecutionOutcome.EXECUTED,
                    "فشلت دورة التعلم: "
                            + exception.getMessage(),
                    true,
                    Collections.emptyList(),
                    null
            );
        }
    }

    /**
     * رد المحادثة.
     */
    private BrainResponse createConversationResponse(
            String originalCommand,
            String normalized,
            CommandUnderstanding.Result understood
    ) {

        String message;

        if (isIdentityRequest(understood)) {

            message =
                    "أنا JARVIS، المساعد ديالك. "
                            + "النواة ديالي كتخدم على الفهم، "
                            + "التعلم من الشبكة، التخطيط، "
                            + "التنفيذ، واكتشاف القدرات والتطور المستمر.";

        } else {

            message =
                    "فهمتك. هاد الطلب محادثة وما غاديش "
                            + "نحوّلو لأمر تنفيذي بلا سبب.";
        }

        return new BrainResponse(
                originalCommand,
                normalized,
                understood.getIntentType(),
                understood.getActionType().name(),
                understood.getCapabilityId(),
                EvolutionCore.ExecutionOutcome.EXECUTED,
                message,
                true,
                Collections.emptyList(),
                null
        );
    }

    /**
     * معرفة واش المستخدم كيسول على هوية JARVIS.
     */
    private boolean isIdentityRequest(
            CommandUnderstanding.Result result
    ) {

        if (result == null) {
            return false;
        }

        String text =
                result.getNormalizedCommand();

        if (text == null) {
            return false;
        }

        String value =
                text.toLowerCase();

        return value.contains("شكون")
                &&
                (
                        value.contains("جارڤيس")
                                ||
                        value.contains("جارفس")
                                ||
                        value.contains("jarvis")
                );
    }

    /**
     * تحويل نتيجة ExecutionEngine إلى BrainResponse.
     */
    private BrainResponse createResponse(
            String originalCommand,
            String normalized,
            CommandUnderstanding.Result understood,
            ExecutionEngine.ExecutionRecord record
    ) {

        String message =
                record.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            switch (record.getMode()) {

                case DIRECT:

                case EVOLUTION_EXECUTED:

                    message =
                            "JARVIS نفذ الهدف المطلوب بنجاح.";

                    break;

                case EVOLUTION_ACTIVATED:

                    message =
                            "JARVIS طور القدرة المطلوبة وفعّلها بنجاح.";

                    break;

                case EVOLUTION_BUILT:

                    message =
                            "JARVIS بنى وطوّر القدرة المطلوبة ومرت عبر دورة التحقق.";

                    break;

                default:

                    message =
                            "JARVIS ما قدرش يكمل العملية.";
            }
        }

        boolean successful =
                record.isSuccessful();

        EvolutionCore.ExecutionOutcome outcome;

        switch (record.getMode()) {

            case EVOLUTION_BUILT:

                outcome =
                        EvolutionCore.ExecutionOutcome.BUILT;

                break;

            case EVOLUTION_ACTIVATED:

                outcome =
                        EvolutionCore.ExecutionOutcome.ACTIVATED;

                break;

            case DIRECT:

            case EVOLUTION_EXECUTED:

                outcome =
                        EvolutionCore.ExecutionOutcome.EXECUTED;

                break;

            default:

                outcome =
                        EvolutionCore.ExecutionOutcome.EXECUTED;
        }

        List<CapabilityPermission>
                missingPermissions =
                Collections.emptyList();

        if (record.getError() != null &&
                record.getError().is(
                        JarvisError.Type.NOT_AUTHORIZED
                )) {

            message =
                    "هاد العملية محتاجة صلاحية أو تفويض المالك قبل التنفيذ.";
        }

        return new BrainResponse(
                originalCommand,
                normalized,
                understood.getIntentType(),
                understood.getActionType().name(),
                understood.getCapabilityId(),
                outcome,
                message,
                successful,
                missingPermissions,
                record.getToolOutput()
        );
    }

    private void publish(
            JarvisEvent.Type type,
            String message
    ) {

        try {

            runtime.publishEvent(
                    type,
                    BRAIN_ID,
                    message,
                    null
            );

        } catch (Exception ignored) {
            // Event logging cannot stop the brain.
        }
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        BRAIN_ID
                )
        );
    }

    public BrainState getState() {
        return state;
    }

    public BrainResponse getLastResponse() {
        return lastResponse;
    }

    public EvolutionCore getEvolutionCore() {
        return evolutionCore;
    }

    public JarvisRuntime getRuntime() {
        return runtime;
    }

    public CommandUnderstanding getUnderstanding() {
        return understanding;
    }

    public KnowledgeLearningEngine
    getLearningEngine() {
        return learningEngine;
    }

    public ExecutionEngine getExecutionEngine() {
        return executionEngine;
    }

    public static String getBrainId() {
        return BRAIN_ID;
    }

    public enum BrainState {

        IDLE,

        UNDERSTANDING,

        LEARNING,

        PLANNING,

        EXECUTING,

        WAITING,

        COMPLETED,

        FAILED
    }

    /**
     * النتيجة النهائية التي يرجعها Brain.
     */
    public static final class BrainResponse {

        private final String originalCommand;
        private final String normalizedCommand;

        private final CommandUnderstanding.IntentType
                intentType;

        private final String action;
        private final String capabilityId;

        private final EvolutionCore.ExecutionOutcome
                outcome;

        private final String message;
        private final boolean successful;

        private final List<CapabilityPermission>
                missingPermissions;

        private final ToolContract.ToolOutput
                toolOutput;

        private BrainResponse(
                String originalCommand,
                String normalizedCommand,
                CommandUnderstanding.IntentType intentType,
                String action,
                String capabilityId,
                EvolutionCore.ExecutionOutcome outcome,
                String message,
                boolean successful,
                List<CapabilityPermission>
                        missingPermissions,
                ToolContract.ToolOutput toolOutput
        ) {

            this.originalCommand =
                    originalCommand;

            this.normalizedCommand =
                    normalizedCommand;

            this.intentType =
                    intentType;

            this.action =
                    action;

            this.capabilityId =
                    capabilityId;

            this.outcome =
                    outcome;

            this.message =
                    message == null
                            ? ""
                            : message;

            this.successful =
                    successful;

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

            this.toolOutput =
                    toolOutput;
        }

        public String getOriginalCommand() {
            return originalCommand;
        }

        public String getNormalizedCommand() {
            return normalizedCommand;
        }

        public CommandUnderstanding.IntentType
        getIntentType() {
            return intentType;
        }

        public String getAction() {
            return action;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionCore.ExecutionOutcome
        getOutcome() {
            return outcome;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public boolean needsPermission() {

            return outcome ==
                    EvolutionCore.ExecutionOutcome
                            .PERMISSION_REQUIRED;
        }

        public boolean wasExecuted() {

            return outcome ==
                    EvolutionCore.ExecutionOutcome
                            .EXECUTED;
        }

        public boolean wasBuilt() {

            return outcome ==
                    EvolutionCore.ExecutionOutcome
                            .BUILT;
        }

        public boolean wasActivated() {

            return outcome ==
                    EvolutionCore.ExecutionOutcome
                            .ACTIVATED;
        }

        public List<CapabilityPermission>
        getMissingPermissions() {

            return missingPermissions;
        }

        public ToolContract.ToolOutput
        getToolOutput() {

            return toolOutput;
        }

        @Override
        public String toString() {

            return "BrainResponse{" +
                    "intentType=" +
                    intentType +
                    ", action='" +
                    action +
                    '\'' +
                    ", capabilityId='" +
                    capabilityId +
                    '\'' +
                    ", outcome=" +
                    outcome +
                    ", successful=" +
                    successful +
                    ", message='" +
                    message +
                    '\'' +
                    '}';
        }
    }
}