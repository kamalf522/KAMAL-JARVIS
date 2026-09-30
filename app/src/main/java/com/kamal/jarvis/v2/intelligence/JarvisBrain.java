package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.EvolutionCore;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeItem;
import com.kamal.jarvis.v2.intelligence.learning.KnowledgeLearningEngine;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Central Brain
 *
 * المسار:
 *
 * المستخدم
 *    ↓
 * فهم الطلب
 *    ↓
 * تحديد الهدف
 *    ↓
 * معرفة موجودة؟
 *    ↓
 * تعلم من الإنترنت + التحقق
 *    ↓
 * اكتشاف القدرة المطلوبة
 *    ↓
 * EvolutionCore
 *    ↓
 * تنفيذ / بناء / اختبار / استرجاع
 *    ↓
 * تعلم من النتيجة
 *    ↓
 * تطور مستمر
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

    private volatile BrainState state =
            BrainState.IDLE;

    private volatile BrainResponse lastResponse;

    /**
     * Constructor القديم للحفاظ على التوافق.
     */
    public JarvisBrain(
            EvolutionCore evolutionCore,
            JarvisRuntime runtime
    ) {

        this(
                evolutionCore,
                runtime,
                createDefaultLearningEngine()
        );
    }

    /**
     * Constructor كامل.
     */
    public JarvisBrain(
            EvolutionCore evolutionCore,
            JarvisRuntime runtime,
            KnowledgeLearningEngine learningEngine
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
    }

    /**
     * إنشاء نظام التعلم المستقل.
     *
     * لا يحتاج المستخدم لإضافة المصادر.
     */
    private static KnowledgeLearningEngine
    createDefaultLearningEngine() {

        return new KnowledgeLearningEngine(
                new com.kamal.jarvis.v2.intelligence.learning
                        .KnowledgeAcquisitionEngine(),
                new com.kamal.jarvis.v2.intelligence.learning
                        .KnowledgeVerificationEngine(),
                new com.kamal.jarvis.v2.intelligence.learning
                        .KnowledgeMemory()
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
         * المرحلة 1:
         * الفهم العام.
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
         * -------------------------------------------------
         * المحادثة
         * -------------------------------------------------
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
         * -------------------------------------------------
         * المعلومات
         * -------------------------------------------------
         *
         * هنا JARVIS ما غاديش يخترع جواب.
         *
         * غادي:
         *
         * Internet
         * → source discovery
         * → trust
         * → acquisition
         * → verification
         * → memory
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
         * -------------------------------------------------
         * بناء متطلبات القدرة
         * -------------------------------------------------
         */
        CapabilityRequirement requirement =
                buildRequirement(
                        understood
                );

        if (requirement == null) {

            state =
                    BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "JARVIS could not create the capability requirement."
            );
        }

        state =
                BrainState.PLANNING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                goal
        );

        /*
         * -------------------------------------------------
         * المعلومات اللي غادي تمشي للـ EvolutionCore
         * -------------------------------------------------
         */
        Map<String, Object> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "original_command",
                userCommand
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

        ToolContract.ToolInput input =
                new ToolContract.ToolInput(
                        understood
                                .getActionType()
                                .name()
                                .toLowerCase(),
                        parameters
                );

        /*
         * -------------------------------------------------
         * Evolution
         * -------------------------------------------------
         */
        state =
                BrainState.EXECUTING;

        JarvisResult<
                EvolutionCore.EvolutionExecutionResult
                > result =
                evolutionCore.execute(
                        requirement,
                        input
                );

        /*
         * -------------------------------------------------
         * إذا فشل بسبب قدرة ناقصة:
         *
         * JARVIS يتعلم من الإنترنت أولا،
         * ثم يعاود محاولة المسار مرة واحدة.
         *
         * هذا هو الربط بين:
         *
         * Learning
         * +
         * Evolution
         * -------------------------------------------------
         */
        if (shouldLearnBeforeRetry(result)) {

            state =
                    BrainState.LEARNING;

            publish(
                    JarvisEvent.Type.COMMAND_STARTED,
                    "JARVIS is learning what is missing."
            );

            try {

                KnowledgeLearningEngine.LearningResult
                        learningResult =
                        learningEngine.learn(
                                goal
                        );

                KnowledgeItem learned =
                        learningEngine.recallBest(
                                goal
                        );

                if (learned != null &&
                        learned.isVerified()) {

                    parameters.put(
                            "verified_knowledge",
                            learned.getContent()
                    );

                    parameters.put(
                            "knowledge_source",
                            learned.getSourceId()
                    );

                    parameters.put(
                            "knowledge_confidence",
                            learned.getConfidence()
                    );

                    input =
                            new ToolContract.ToolInput(
                                    understood
                                            .getActionType()
                                            .name()
                                            .toLowerCase(),
                                    parameters
                            );
                }

                /*
                 * إعادة محاولة Evolution.
                 *
                 * محاولة واحدة فقط باش ما ندخلوش
                 * فـ loop لا نهائي.
                 */
                state =
                        BrainState.EXECUTING;

                JarvisResult<
                        EvolutionCore.EvolutionExecutionResult
                        > retryResult =
                        evolutionCore.execute(
                                requirement,
                                input
                        );

                if (retryResult != null &&
                        retryResult.isSuccess()) {

                    result =
                            retryResult;
                }

            } catch (Exception ignored) {

                /*
                 * فشل التعلم ما خاصوش يطيح
                 * العقل كامل.
                 */
            }
        }

        if (result == null) {

            state =
                    BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "EvolutionCore returned no result."
            );
        }

        if (!result.isSuccess()) {

            state =
                    BrainState.FAILED;

            JarvisError error =
                    result.getError();

            String message =
                    error == null
                            ? "JARVIS evolution failed."
                            : error.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        "JARVIS evolution failed.";
            }

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    message
            );

            return JarvisResult.failure(
                    error != null
                            ? error
                            : JarvisError.of(
                                    JarvisError.Type.EVOLUTION_FAILED,
                                    message,
                                    BRAIN_ID
                            ),
                    message
            );
        }

        EvolutionCore.EvolutionExecutionResult
                evolutionResult =
                result.getData();

        if (evolutionResult == null) {

            state =
                    BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Empty evolution result."
            );
        }

        BrainResponse response =
                createResponse(
                        userCommand,
                        normalized,
                        understood,
                        evolutionResult
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
     * واش خاص JARVIS يتعلم قبل إعادة المحاولة.
     */
    private boolean shouldLearnBeforeRetry(
            JarvisResult<
                    EvolutionCore.EvolutionExecutionResult
                    > result
    ) {

        if (result == null ||
                !result.isSuccess()) {

            return false;
        }

        EvolutionCore.EvolutionExecutionResult
                data =
                result.getData();

        if (data == null ||
                data.getPlan() == null) {

            return true;
        }

        CapabilityPlan.Action action =
                data.getPlan().getAction();

        return action ==
                CapabilityPlan.Action.UNAVAILABLE;
    }

    /**
     * الإجابة من المعرفة المكتسبة.
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
                            + (reason.isEmpty()
                            ? ""
                            : "\n" + reason),
                    true,
                    Collections.emptyList(),
                    null
            );

        } catch (Exception e) {

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
                            + e.getMessage(),
                    true,
                    Collections.emptyList(),
                    null
            );
        }
    }

    /**
     * بناء Requirement عام.
     */
    private CapabilityRequirement buildRequirement(
            CommandUnderstanding.Result understood
    ) {

        if (understood == null) {
            return null;
        }

        String capabilityId =
                safeCapabilityId(
                        understood.getCapabilityId()
                );

        String goal =
                understood.getGoal();

        if (goal == null ||
                goal.trim().isEmpty()) {

            goal =
                    understood.getOriginalCommand();
        }

        CapabilityRequirement.Builder builder =
                CapabilityRequirement.builder(
                        capabilityId,
                        goal
                );

        List<
                CommandUnderstanding.Requirement
                > requirements =
                understood.getRequirements();

        if (requirements != null) {

            for (
                    CommandUnderstanding.Requirement
                            requirement
                    : requirements
            ) {

                addRequirement(
                        builder,
                        requirement
                );
            }
        }

        if (understood.isEvolutionRequest()) {

            builder.requireOwnerAuthorization();
            builder.allowAlternativeBuilding();
        }

        if (understood.isDestructive()) {

            builder.requireOwnerAuthorization();
        }

        switch (
                understood.getIntentType()
        ) {

            case REMINDER:

                builder.preferTool(
                        "android.reminders"
                );

                builder.alternativeTool(
                        "android.notifications"
                );

                builder.allowAlternativeBuilding();

                break;

            case FILE_OPERATION:

                builder.preferTool(
                        "android.files"
                );

                builder.alternativeTool(
                        "workspace.files"
                );

                builder.allowAlternativeBuilding();

                break;

            case APP_ACTION:

                builder.preferTool(
                        "android.app_launcher"
                );

                builder.alternativeTool(
                        "android.intent"
                );

                builder.allowAlternativeBuilding();

                break;

            case SEARCH:

                builder.preferTool(
                        "network.search"
                );

                builder.alternativeTool(
                        "network.http"
                );

                builder.requirePermission(
                        CapabilityPermission.NETWORK
                );

                builder.allowAlternativeBuilding();

                break;

            case NETWORK:

                builder.preferTool(
                        "network.http"
                );

                builder.requirePermission(
                        CapabilityPermission.NETWORK
                );

                builder.allowAlternativeBuilding();

                break;

            case VOICE:

                builder.preferTool(
                        "android.microphone"
                );

                builder.allowAlternativeBuilding();

                break;

            case DEVELOPMENT:

                builder.preferTool(
                        "project.inspector"
                );

                builder.alternativeTool(
                        "project.builder"
                );

                builder.requireOwnerAuthorization();

                builder.allowAlternativeBuilding();

                break;

            case SETTINGS:
            case INFORMATION:
            case GENERAL:

            default:

                /*
                 * ما كاينش tool وهمي.
                 *
                 * JARVIS يقدر يكتشف أو يبني
                 * القدرة اللي ناقصة.
                 */
                builder.allowAlternativeBuilding();

                break;
        }

        return builder.build();
    }

    /**
     * تحويل متطلبات الفهم إلى صلاحيات.
     */
    private void addRequirement(
            CapabilityRequirement.Builder builder,
            CommandUnderstanding.Requirement requirement
    ) {

        if (builder == null ||
                requirement == null) {

            return;
        }

        switch (requirement) {

            case NOTIFICATIONS:

                builder.requirePermission(
                        CapabilityPermission.NOTIFICATIONS
                );

                break;

            case BACKGROUND_EXECUTION:

                builder.requirePermission(
                        CapabilityPermission.BACKGROUND_EXECUTION
                );

                break;

            case FILE_ACCESS:

                builder.requirePermission(
                        CapabilityPermission.FILE_READ
                );

                builder.requirePermission(
                        CapabilityPermission.FILE_WRITE
                );

                break;

            case DESTRUCTIVE_OPERATION:

                builder.requireOwnerAuthorization();

                break;

            case NETWORK:

                builder.requirePermission(
                        CapabilityPermission.NETWORK
                );

                break;

            case MICROPHONE:

                builder.requirePermission(
                        CapabilityPermission.MICROPHONE
                );

                break;

            case PROJECT_ACCESS:

                builder.requirePermission(
                        CapabilityPermission.PROJECT_READ
                );

                builder.requirePermission(
                        CapabilityPermission.PROJECT_WRITE
                );

                break;

            case BUILD_ACCESS:

                builder.requirePermission(
                        CapabilityPermission.BUILD_PROJECT
                );

                break;

            case TEST_ACCESS:

                builder.requirePermission(
                        CapabilityPermission.RUN_TESTS
                );

                break;

            case OWNER_AUTHORIZATION:

                builder.requireOwnerAuthorization();

                break;

            default:

                break;
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
                            + "التعلم من الشبكة، اكتشاف القدرات، "
                            + "والتطور المستمر مع احترام سيطرة المالك.";

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
     * النتيجة النهائية.
     */
    private BrainResponse createResponse(
            String originalCommand,
            String normalized,
            CommandUnderstanding.Result understood,
            EvolutionCore.EvolutionExecutionResult
                    evolutionResult
    ) {

        EvolutionCore.ExecutionOutcome outcome =
                evolutionResult.getOutcome();

        String message;

        switch (outcome) {

            case EXECUTED:

                message =
                        "JARVIS نفذ الهدف المطلوب بنجاح.";

                break;

            case BUILT:

                message =
                        "JARVIS بنى/طور القدرة المطلوبة "
                                + "ومرت عبر دورة التطور والتحقق.";

                break;

            case PERMISSION_REQUIRED:

                message =
                        "هاد العملية محتاجة صلاحية أو "
                                + "تفويض المالك قبل التنفيذ.";

                break;

            default:

                message =
                        evolutionResult.getMessage();
        }

        return new BrainResponse(
                originalCommand,
                normalized,
                understood.getIntentType(),
                understood.getActionType().name(),
                understood.getCapabilityId(),
                outcome,
                message,
                outcome ==
                        EvolutionCore.ExecutionOutcome.EXECUTED
                        ||
                        outcome ==
                                EvolutionCore.ExecutionOutcome.BUILT,
                evolutionResult
                        .getMissingPermissions(),
                evolutionResult
                        .getToolOutput()
        );
    }

    private String safeCapabilityId(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "goal.unknown";
        }

        return value
                .trim()
                .replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
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