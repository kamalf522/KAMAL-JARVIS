package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.EvolutionCore;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Jarvis Brain
 *
 * العقل المركزي ديال JARVIS.
 *
 * المبدأ:
 *
 * المستخدم
 *    ↓
 * CommandUnderstanding
 *    ↓
 * فهم الهدف الكامل
 *    ↓
 * Brain
 *    ↓
 * تحديد واش:
 *    - محادثة / سؤال
 *    - طلب تنفيذ
 *    - طلب تطور
 *    - قدرة جديدة غير معروفة
 *    ↓
 * CapabilityRequirement
 *    ↓
 * EvolutionCore
 *    ↓
 * Discovery → Planning → Building → Testing → Recovery
 *
 * مهم:
 *
 * Brain ما عندوش لائحة محدودة ديال الأوامر.
 *
 * CommandUnderstanding كتخرج المعنى الأولي،
 * والطلب الكامل كيبقى محفوظ باش الطبقات العليا
 * تقدر تخطط وتطور قدرة جديدة عند الحاجة.
 */
public final class JarvisBrain {

    private static final String BRAIN_ID =
            "v2.jarvis_brain";

    private final EvolutionCore evolutionCore;
    private final JarvisRuntime runtime;
    private final CommandUnderstanding understanding;

    private volatile BrainState state =
            BrainState.IDLE;

    private volatile BrainResponse lastResponse;

    public JarvisBrain(
            EvolutionCore evolutionCore,
            JarvisRuntime runtime
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

        this.evolutionCore = evolutionCore;
        this.runtime = runtime;
        this.understanding = new CommandUnderstanding();
    }

    /**
     * نقطة الدخول الرئيسية.
     */
    public synchronized JarvisResult<BrainResponse> process(
            String userCommand
    ) {

        if (userCommand == null ||
                userCommand.trim().isEmpty()) {

            state = BrainState.FAILED;
            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Empty command."
            );

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "JARVIS received an empty command."
            );
        }

        state = BrainState.UNDERSTANDING;

        publish(
                JarvisEvent.Type.COMMAND_RECEIVED,
                userCommand
        );

        /*
         * الفهم الحقيقي الأولي كيدوز من
         * CommandUnderstanding.
         *
         * ما بقيناش نستعملو understand()
         * قديم داخل Brain.
         */
        CommandUnderstanding.Result understood =
                understanding.analyze(
                        userCommand
                );

        if (understood == null ||
                !understood.isValid()) {

            state = BrainState.FAILED;

            String message =
                    understood == null
                            ? "JARVIS could not understand the request."
                            : understood.getMessage();

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    message
            );

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    message
            );
        }

        /*
         * الطلب كامل محفوظ هنا.
         */
        String normalized =
                understood.getNormalizedCommand();

        String goal =
                understood.getGoal();

        String capabilityId =
                understood.getCapabilityId();

        /*
         * -------------------------------------------------
         * 1. المحادثة والأسئلة
         * -------------------------------------------------
         *
         * ماشي كل كلام خاصو يتحول إلى Tool.
         *
         * مثال:
         *
         * "سلام جارڤيس"
         * "شكون نتا؟"
         *
         * ما خاصناش نقلبوهم إلى:
         *
         * jarvis.general
         *
         * لأن هذا هو السبب اللي كان كيخرج:
         * الأمر غير منفذ.
         */
        if (understood.isConversational() ||
                isIdentityRequest(understood)) {

            BrainResponse response =
                    createConversationResponse(
                            userCommand,
                            normalized,
                            understood
                    );

            lastResponse = response;
            state = BrainState.COMPLETED;

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
         * 2. سؤال معلوماتي
         * -------------------------------------------------
         *
         * السؤال ماشي بالضرورة Action.
         *
         * إلا كان عند JARVIS مصدر معرفة/قدرة مناسبة،
         * الطبقات العليا تقدر تضيفها لاحقاً.
         *
         * ما كنحاولوش نحولو السؤال تلقائياً
         * إلى أمر Android.
         */
        if (understood.getIntentType() ==
                CommandUnderstanding.IntentType.INFORMATION
                &&
                !understood.isEvolutionRequest()) {

            BrainResponse response =
                    createInformationResponse(
                            userCommand,
                            normalized,
                            understood
                    );

            lastResponse = response;
            state = BrainState.WAITING;

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
         * 3. بناء Requirement
         * -------------------------------------------------
         *
         * هنا الفرق الكبير:
         *
         * ما كنستعملوش:
         *
         * jarvis.general
         *
         * بل كنستعملو capabilityId
         * والهدف الكامل اللي عطانا المستخدم.
         */
        CapabilityRequirement requirement =
                buildRequirement(
                        understood
                );

        if (requirement == null) {

            state = BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "JARVIS could not create the capability requirement."
            );
        }

        state = BrainState.PLANNING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                "Planning goal: "
                        + goal
        );

        /*
         * -------------------------------------------------
         * 4. معلومات التنفيذ
         * -------------------------------------------------
         *
         * كنمررو النص الأصلي + المعنى + الهدف
         * للطبقات اللي تحت.
         *
         * هكذا ما كيبقاش EvolutionCore عارف
         * غير كلمة صغيرة من الطلب.
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
                capabilityId
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
         * 5. EvolutionCore
         * -------------------------------------------------
         *
         * هنا كيبدا المسار الحقيقي:
         *
         * Discover
         * Plan
         * Execute
         * Build إذا كانت القدرة ناقصة
         * Test
         * Verify
         * Recover عند الفشل
         */
        state = BrainState.EXECUTING;

        JarvisResult<
                EvolutionCore.EvolutionExecutionResult
                > executionResult =
                evolutionCore.execute(
                        requirement,
                        input
                );

        if (executionResult == null) {

            state = BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "EvolutionCore returned no result."
            );
        }

        if (!executionResult.isSuccess()) {

            state = BrainState.FAILED;

            JarvisError error =
                    executionResult.getError();

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
                executionResult.getData();

        if (evolutionResult == null) {

            state = BrainState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "JARVIS received an empty evolution result."
            );
        }

        BrainResponse response =
                createResponse(
                        userCommand,
                        normalized,
                        understood,
                        requirement,
                        evolutionResult
                );

        lastResponse = response;

        if (response.isSuccessful()) {

            state = BrainState.COMPLETED;

            publish(
                    JarvisEvent.Type.COMMAND_COMPLETED,
                    response.getMessage()
            );

            return JarvisResult.success(
                    response,
                    response.getMessage()
            );
        }

        state = BrainState.WAITING;

        publish(
                JarvisEvent.Type.COMMAND_FAILED,
                response.getMessage()
        );

        /*
         * النتيجة هنا ماشي crash.
         *
         * ممكن تكون:
         * - permission required
         * - capability unavailable
         * - evolution failed
         *
         * وكنرجعو السبب الحقيقي.
         */
        return JarvisResult.success(
                response,
                response.getMessage()
        );
    }

    /**
     * بناء Requirement من الفهم العام.
     *
     * ما كايناش هنا لائحة مغلقة ديال القدرات.
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
                    understood
                            .getOriginalCommand();
        }

        CapabilityRequirement.Builder builder =
                CapabilityRequirement.builder(
                        capabilityId,
                        goal
                );

        /*
         * المتطلبات اللي اكتاشفتها طبقة الفهم.
         */
        List<CommandUnderstanding.Requirement>
                requirements =
                understood.getRequirements();

        if (requirements != null) {

            for (
                    CommandUnderstanding.Requirement requirement
                    : requirements
            ) {

                addRequirement(
                        builder,
                        requirement
                );
            }
        }

        /*
         * طلب التطور الذاتي حساس.
         *
         * ما كنسمحوش للتطور يتجاوز Owner Security.
         */
        if (understood.isEvolutionRequest()) {

            builder.requireOwnerAuthorization();

            builder.allowAlternativeBuilding();
        }

        /*
         * الطلبات الجديدة وغير المصنفة:
         *
         * ما كنرميهاش.
         *
         * ما كنربطوهاش بأداة وهمية.
         *
         * كنخليو EvolutionCore يكتشف واش كاينة
         * قدرة مناسبة أو خاص بناء قدرة جديدة.
         */
        if (understood.getIntentType() ==
                CommandUnderstanding.IntentType.GENERAL) {

            builder.allowAlternativeBuilding();
        }

        /*
         * أدوات مفضلة فقط إذا كانت طبقة الفهم
         * فعلاً عارفة نوع القدرة.
         *
         * ما كاين حتى jarvis.general.
         */
        switch (understood.getIntentType()) {

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

                builder.allowAlternativeBuilding();

                break;

            case NETWORK:

                builder.preferTool(
                        "network.http"
                );

                builder.allowAlternativeBuilding();

                break;

            case INFORMATION:

                builder.allowAlternativeBuilding();

                break;

            case GENERAL:

            default:

                builder.allowAlternativeBuilding();

                break;
        }

        return builder.build();
    }

    /**
     * تحويل Requirement الخاص بالفهم إلى صلاحية.
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
     * معالجة الكلام العادي/التحية/الهوية.
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
                            + "النواة ديالي مبنية باش نفهم الهدف ديالك، "
                            + "نخطط ليه، ونطور القدرات اللي خاصاني "
                            + "بشكل مستمر مع احترام صلاحياتك وأمان النظام.";

        } else {

            message =
                    "سمعتك وفهمت أن الطلب ديالك محادثة. "
                            + "الطلب محفوظ عندي وما غاديش نحولو "
                            + "لأمر تنفيذي بلا سبب.";
        }

        return new BrainResponse(
                originalCommand,
                normalized,
                understood.getIntentType(),
                understood.getActionType()
                        .name(),
                understood.getCapabilityId(),
                EvolutionCore.ExecutionOutcome.EXECUTED,
                message,
                true,
                Collections.emptyList(),
                null
        );
    }

    /**
     * معالجة الأسئلة المعلوماتية.
     *
     * حالياً ما كنخترعوش جواب من راسنا.
     * كنرجعو الهدف الكامل باش طبقة المعرفة/الذكاء
     * اللي غادي تتطور من بعد تقدر تتعامل معاه.
     */
    private BrainResponse createInformationResponse(
            String originalCommand,
            String normalized,
            CommandUnderstanding.Result understood
    ) {

        String goal =
                understood.getGoal();

        if (goal == null ||
                goal.trim().isEmpty()) {

            goal = originalCommand;
        }

        String message =
                "فهمت السؤال ديالك: "
                        + goal
                        + ". "
                        + "الطلب ما اعتبرتوش أمراً فاشلاً "
                        + "وما حولتوش لأداة غير موجودة.";

        return new BrainResponse(
                originalCommand,
                normalized,
                understood.getIntentType(),
                understood.getActionType()
                        .name(),
                understood.getCapabilityId(),
                EvolutionCore.ExecutionOutcome.EXECUTED,
                message,
                true,
                Collections.emptyList(),
                null
        );
    }

    /**
     * تحديد واش الطلب متعلق بهوية JARVIS.
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
                && (
                value.contains("جارڤيس")
                        || value.contains("جارفس")
                        || value.contains("jarvis")
        );
    }

    /**
     * إنشاء النتيجة النهائية للتنفيذ/التطور.
     */
    private BrainResponse createResponse(
            String originalCommand,
            String normalizedCommand,
            CommandUnderstanding.Result understood,
            CapabilityRequirement requirement,
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
                        "JARVIS طور/بنى القدرة المطلوبة "
                                + "ومرت عبر دورة التطوير الحالية.";

                break;

            case PERMISSION_REQUIRED:

                message =
                        buildPermissionMessage(
                                evolutionResult
                                        .getMissingPermissions()
                        );

                break;

            default:

                message =
                        evolutionResult.getMessage();

                if (message == null ||
                        message.trim().isEmpty()) {

                    message =
                            "الهدف ما تنفذش. "
                                    + "JARVIS حافظ على النتيجة الحقيقية "
                                    + "بدون ما يدعي أنه نفذها.";
                }

                break;
        }

        boolean successful =
                outcome ==
                        EvolutionCore.ExecutionOutcome.EXECUTED
                        ||
                        outcome ==
                                EvolutionCore.ExecutionOutcome.BUILT;

        return new BrainResponse(
                originalCommand,
                normalizedCommand,
                understood.getIntentType(),
                understood.getActionType()
                        .name(),
                requirement.getCapabilityId(),
                outcome,
                message,
                successful,
                evolutionResult
                        .getMissingPermissions(),
                evolutionResult
                        .getToolOutput()
        );
    }

    private String buildPermissionMessage(
            List<CapabilityPermission> missing
    ) {

        if (missing == null ||
                missing.isEmpty()) {

            return
                    "JARVIS يحتاج صلاحية إضافية "
                            + "قبل إكمال الهدف.";
        }

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "JARVIS يحتاج الصلاحيات التالية: "
        );

        for (int i = 0;
             i < missing.size();
             i++) {

            if (i > 0) {
                builder.append(", ");
            }

            builder.append(
                    missing.get(i).getId()
            );
        }

        builder.append(
                ". لم يتم اعتبار الهدف منفذاً."
        );

        return builder.toString();
    }

    /**
     * Capability ID آمن.
     */
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

            /*
             * Event logging ما خاصوش يوقف Brain.
             */
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

    public static String getBrainId() {
        return BRAIN_ID;
    }

    /**
     * حالات العقل.
     */
    public enum BrainState {

        IDLE,

        UNDERSTANDING,

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