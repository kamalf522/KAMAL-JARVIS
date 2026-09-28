package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisEvent;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.CapabilityPlan;
import com.kamal.jarvis.v2.evolution.EvolutionCore;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JARVIS V2 - Brain
 *
 * العقل الأعلى المسؤول عن استقبال أمر المستخدم
 * وتحويله إلى هدف قابل للتنفيذ داخل منظومة JARVIS.
 *
 * Brain لا ينفذ Android operations بنفسه.
 * Brain:
 *
 * 1. يستقبل الأمر.
 * 2. ينظف ويفهم الأمر الأساسي.
 * 3. يحدد نوع الهدف.
 * 4. ينشئ CapabilityRequirement.
 * 5. يمرر الهدف إلى EvolutionCore.
 * 6. EvolutionCore يقرر:
 *      - تنفيذ مباشر
 *      - تنفيذ بديل
 *      - طلب صلاحية
 *      - بناء Capability
 *      - عدم توفر مسار
 * 7. Brain يرجع نتيجة صادقة للمستوى الأعلى.
 *
 * مهم:
 * Brain لا يعتبر الأمر ناجحاً إلا إذا رجعت
 * المنظومة نتيجة نجاح حقيقية.
 */
public final class JarvisBrain {

    private static final String BRAIN_ID =
            "v2.jarvis_brain";

    private final EvolutionCore evolutionCore;
    private final JarvisRuntime runtime;

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
    }

    /**
     * نقطة الدخول الرئيسية للأوامر.
     */
    public synchronized JarvisResult<BrainResponse> process(
            String userCommand
    ) {

        if (userCommand == null ||
                userCommand.trim().isEmpty()) {

            state = BrainState.FAILED;

            JarvisResult<BrainResponse> result =
                    failure(
                            JarvisError.Type.INVALID_REQUEST,
                            "JARVIS received an empty command."
                    );

            lastResponse = null;
            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Empty command received."
            );

            return result;
        }

        String normalized =
                normalize(userCommand);

        state = BrainState.UNDERSTANDING;

        publish(
                JarvisEvent.Type.COMMAND_RECEIVED,
                normalized
        );

        Intent intent =
                understand(normalized);

        if (intent == null) {

            state = BrainState.FAILED;

            JarvisResult<BrainResponse> result =
                    failure(
                            JarvisError.Type.INTERNAL_ERROR,
                            "JARVIS could not create an intent."
                    );

            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Intent creation failed."
            );

            return result;
        }

        CapabilityRequirement requirement =
                buildRequirement(intent);

        if (requirement == null) {

            state = BrainState.FAILED;

            JarvisResult<BrainResponse> result =
                    failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "JARVIS could not create a capability requirement."
                    );

            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Capability requirement creation failed."
            );

            return result;
        }

        state = BrainState.PLANNING;

        publish(
                JarvisEvent.Type.COMMAND_STARTED,
                "Planning capability: "
                        + requirement.getCapabilityId()
        );

        /*
         * ToolInput يحتوي الأمر الأصلي والمعلومات
         * التي يحتاجها التنفيذ.
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
                "capability_id",
                requirement.getCapabilityId()
        );

        parameters.put(
                "intent_type",
                intent.getType().name()
        );

        parameters.put(
                "goal",
                requirement.getDescription()
        );

        ToolContract.ToolInput input =
                new ToolContract.ToolInput(
                        intent.getAction(),
                        parameters
                );

        state = BrainState.EXECUTING;

        JarvisResult<EvolutionCore.EvolutionExecutionResult>
                executionResult =
                evolutionCore.execute(
                        requirement,
                        input
                );

        if (executionResult == null) {

            state = BrainState.FAILED;

            JarvisResult<BrainResponse> result =
                    failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "EvolutionCore returned no result."
                    );

            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "EvolutionCore returned null."
            );

            return result;
        }

        if (!executionResult.isSuccess()) {

            state = BrainState.FAILED;

            JarvisError error =
                    executionResult.getError();

            String message =
                    error == null
                            ? "JARVIS execution failed."
                            : error.getMessage();

            JarvisResult<BrainResponse> result =
                    JarvisResult.failure(
                            error != null
                                    ? error
                                    : JarvisError.of(
                                            JarvisError.Type.EVOLUTION_FAILED,
                                            message,
                                            BRAIN_ID
                                    ),
                            message
                    );

            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    message
            );

            return result;
        }

        EvolutionCore.EvolutionExecutionResult
                evolutionResult =
                executionResult.getData();

        if (evolutionResult == null) {

            state = BrainState.FAILED;

            JarvisResult<BrainResponse> result =
                    failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "JARVIS received an empty evolution result."
                    );

            lastResponse = null;

            publish(
                    JarvisEvent.Type.COMMAND_FAILED,
                    "Empty evolution result."
            );

            return result;
        }

        BrainResponse response =
                createResponse(
                        userCommand,
                        normalized,
                        intent,
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

        return JarvisResult.success(
                response,
                response.getMessage()
        );
    }

    /**
     * يفهم الأمر بشكل deterministic.
     *
     * هذا ليس LLM.
     * هذه طبقة فهم أولية مستقلة ويمكن تطويرها لاحقاً
     * بدون تغيير EvolutionCore.
     */
    private Intent understand(
            String command
    ) {

        String text =
                normalize(command);

        if (containsAny(
                text,
                "تذكر",
                "ذكرني",
                "تذكير",
                "remind",
                "reminder"
        )) {

            return new Intent(
                    IntentType.REMINDER,
                    "create_reminder",
                    "إنشاء نظام تذكير وتنفيذ التذكير المطلوب.",
                    false
            );
        }

        if (containsAny(
                text,
                "ملف",
                "ملفات",
                "file",
                "files"
        )) {

            boolean sensitive =
                    containsAny(
                            text,
                            "حذف",
                            "مسح",
                            "delete",
                            "remove"
                    );

            return new Intent(
                    IntentType.FILE_OPERATION,
                    "file_operation",
                    "تنفيذ عملية الملفات المطلوبة.",
                    sensitive
            );
        }

        if (containsAny(
                text,
                "افتح",
                "فتح",
                "شغل",
                "launch",
                "open",
                "start"
        )) {

            return new Intent(
                    IntentType.APP_ACTION,
                    "open_or_launch",
                    "فتح أو تشغيل المورد المطلوب.",
                    false
            );
        }

        if (containsAny(
                text,
                "ابحث",
                "بحث",
                "قلب",
                "search",
                "find"
        )) {

            return new Intent(
                    IntentType.SEARCH,
                    "search",
                    "البحث عن المعلومات أو المورد المطلوب.",
                    false
            );
        }

        if (containsAny(
                text,
                "صوت",
                "سمع",
                "ميكروفون",
                "microphone",
                "voice"
        )) {

            return new Intent(
                    IntentType.VOICE,
                    "voice_operation",
                    "تنفيذ العملية الصوتية المطلوبة.",
                    false
            );
        }

        if (containsAny(
                text,
                "بني",
                "ابني",
                "صوب",
                "طور",
                "طور ليا",
                "برمج",
                "كود",
                "build",
                "develop",
                "code"
        )) {

            return new Intent(
                    IntentType.DEVELOPMENT,
                    "develop_capability",
                    "تطوير أو بناء القدرة المطلوبة.",
                    true
            );
        }

        /*
         * أي أمر غير معروف لا يتم إسقاطه.
         * يتم تحويله إلى هدف عام حتى تستطيع
         * طبقات Evolution التعامل معه.
         */
        return new Intent(
                IntentType.GENERAL,
                "general_command",
                text,
                false
        );
    }

    /**
     * يحول Intent إلى Requirement كامل.
     */
    private CapabilityRequirement buildRequirement(
            Intent intent
    ) {

        if (intent == null) {
            return null;
        }

        CapabilityRequirement.Builder builder =
                CapabilityRequirement.builder(
                        intent.getCapabilityId(),
                        intent.getDescription()
                );

        switch (intent.getType()) {

            case REMINDER:

                builder.requirePermission(
                        CapabilityPermission.NOTIFICATIONS
                );

                builder.requirePermission(
                        CapabilityPermission.BACKGROUND_EXECUTION
                );

                builder.preferTool(
                        "android.reminders"
                );

                builder.alternativeTool(
                        "android.notifications"
                );

                builder.allowAlternativeBuilding();

                break;

            case FILE_OPERATION:

                builder.requirePermission(
                        CapabilityPermission.FILE_READ
                );

                builder.requirePermission(
                        CapabilityPermission.FILE_WRITE
                );

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

                builder.requirePermission(
                        CapabilityPermission.NETWORK
                );

                builder.preferTool(
                        "network.search"
                );

                builder.alternativeTool(
                        "network.http"
                );

                builder.allowAlternativeBuilding();

                break;

            case VOICE:

                builder.requirePermission(
                        CapabilityPermission.MICROPHONE
                );

                builder.preferTool(
                        "android.microphone"
                );

                builder.allowAlternativeBuilding();

                break;

            case DEVELOPMENT:

                builder.requirePermission(
                        CapabilityPermission.PROJECT_READ
                );

                builder.requirePermission(
                        CapabilityPermission.PROJECT_WRITE
                );

                builder.requirePermission(
                        CapabilityPermission.BUILD_PROJECT
                );

                builder.requirePermission(
                        CapabilityPermission.RUN_TESTS
                );

                builder.preferTool(
                        "project.inspector"
                );

                builder.alternativeTool(
                        "project.builder"
                );

                builder.requireOwnerAuthorization();

                builder.allowAlternativeBuilding();

                break;

            case GENERAL:
            default:

                /*
                 * لا نعطي صلاحيات حساسة تلقائياً
                 * للأوامر غير المعروفة.
                 */
                builder.preferTool(
                        "jarvis.general"
                );

                builder.alternativeTool(
                        "jarvis.capability"
                );

                builder.allowAlternativeBuilding();

                break;
        }

        return builder.build();
    }

    /**
     * إنشاء النتيجة النهائية.
     */
    private BrainResponse createResponse(
            String originalCommand,
            String normalizedCommand,
            Intent intent,
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
                        "JARVIS نفذ القدرة المطلوبة بنجاح.";

                break;

            case BUILT:

                message =
                        "JARVIS بنى القدرة المطلوبة ومرت عبر "
                                + "دورة البناء والاختبار الحالية.";

                break;

            case PERMISSION_REQUIRED:

                List<CapabilityPermission>
                        missing =
                        evolutionResult
                                .getMissingPermissions();

                message =
                        buildPermissionMessage(
                                missing
                        );

                break;

            default:

                message =
                        evolutionResult.getMessage();

                break;
        }

        return new BrainResponse(
                originalCommand,
                normalizedCommand,
                intent.getType(),
                intent.getAction(),
                requirement.getCapabilityId(),
                outcome,
                message,
                outcome == EvolutionCore.ExecutionOutcome.EXECUTED
                        || outcome == EvolutionCore.ExecutionOutcome.BUILT,
                evolutionResult.getMissingPermissions(),
                evolutionResult.getToolOutput()
        );
    }

    private String buildPermissionMessage(
            List<CapabilityPermission> missing
    ) {

        if (missing == null ||
                missing.isEmpty()) {

            return "JARVIS يحتاج قدرة أو صلاحية إضافية لإكمال الهدف.";
        }

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "JARVIS حدد المتطلبات الناقصة: "
        );

        for (int i = 0; i < missing.size(); i++) {

            CapabilityPermission permission =
                    missing.get(i);

            if (i > 0) {
                builder.append(", ");
            }

            builder.append(
                    permission.getId()
            );
        }

        builder.append(
                ". لم يتم اعتبار الأمر منفذاً."
        );

        return builder.toString();
    }

    private String normalize(
            String command
    ) {

        if (command == null) {
            return "";
        }

        return command
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                values == null) {

            return false;
        }

        for (String value : values) {

            if (value != null &&
                    !value.isEmpty() &&
                    text.contains(
                            value.toLowerCase(Locale.ROOT)
                    )) {

                return true;
            }
        }

        return false;
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
             * Event logging must never break
             * the actual Brain flow.
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

    public static String getBrainId() {
        return BRAIN_ID;
    }

    /**
     * حالة Brain.
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
     * أنواع النوايا الأساسية.
     */
    public enum IntentType {

        REMINDER,

        FILE_OPERATION,

        APP_ACTION,

        SEARCH,

        VOICE,

        DEVELOPMENT,

        GENERAL
    }

    /**
     * Intent داخلي.
     */
    public static final class Intent {

        private final IntentType type;
        private final String action;
        private final String description;
        private final boolean ownerAuthorizationRequired;

        private Intent(
                IntentType type,
                String action,
                String description,
                boolean ownerAuthorizationRequired
        ) {

            this.type = type;
            this.action = action;
            this.description = description;
            this.ownerAuthorizationRequired =
                    ownerAuthorizationRequired;
        }

        public IntentType getType() {
            return type;
        }

        public String getAction() {
            return action;
        }

        public String getDescription() {
            return description;
        }

        public boolean isOwnerAuthorizationRequired() {
            return ownerAuthorizationRequired;
        }

        public String getCapabilityId() {

            return "brain."
                    + type.name().toLowerCase(
                    Locale.ROOT
            );
        }

        @Override
        public String toString() {

            return "Intent{" +
                    "type=" + type +
                    ", action='" + action + '\'' +
                    ", description='" + description + '\'' +
                    ", ownerAuthorizationRequired=" +
                    ownerAuthorizationRequired +
                    '}';
        }
    }

    /**
     * النتيجة التي يرجعها Brain.
     */
    public static final class BrainResponse {

        private final String originalCommand;
        private final String normalizedCommand;
        private final IntentType intentType;
        private final String action;
        private final String capabilityId;
        private final EvolutionCore.ExecutionOutcome outcome;
        private final String message;
        private final boolean successful;
        private final List<CapabilityPermission>
                missingPermissions;
        private final ToolContract.ToolOutput
                toolOutput;

        private BrainResponse(
                String originalCommand,
                String normalizedCommand,
                IntentType intentType,
                String action,
                String capabilityId,
                EvolutionCore.ExecutionOutcome outcome,
                String message,
                boolean successful,
                List<CapabilityPermission>
                        missingPermissions,
                ToolContract.ToolOutput
                        toolOutput
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

        public IntentType getIntentType() {
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
                    action + '\'' +
                    ", capabilityId='" +
                    capabilityId + '\'' +
                    ", outcome=" +
                    outcome +
                    ", successful=" +
                    successful +
                    ", message='" +
                    message + '\'' +
                    '}';
        }
    }
}