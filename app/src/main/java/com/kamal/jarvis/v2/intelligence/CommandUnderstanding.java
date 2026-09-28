package com.kamal.jarvis.v2.intelligence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * JARVIS V2 - Command Understanding
 *
 * طبقة فهم الأوامر.
 *
 * المسؤوليات:
 * 1. تنظيف الأمر.
 * 2. استخراج الكلمات المهمة.
 * 3. تحديد Intent.
 * 4. تحديد Action.
 * 5. تحديد الهدف العام.
 * 6. اكتشاف مستوى الحساسية.
 * 7. استخراج المتطلبات المعروفة.
 *
 * هذه الطبقة لا تنفذ أي أمر.
 * ولا تمنح أي صلاحية.
 * ولا تتجاوز Security.
 *
 * هي فقط تحول كلام المستخدم إلى
 * CommandUnderstanding.Result منظمة يمكن
 * للـBrain استعمالها.
 */
public final class CommandUnderstanding {

    private static final String ENGINE_ID =
            "v2.command_understanding";

    private static final String[] REMINDER_WORDS = {
            "تذكر",
            "ذكرني",
            "تذكير",
            "منبه",
            "remind",
            "reminder",
            "alarm"
    };

    private static final String[] FILE_WORDS = {
            "ملف",
            "ملفات",
            "folder",
            "folders",
            "file",
            "files",
            "مجلد",
            "مجلدات"
    };

    private static final String[] DELETE_WORDS = {
            "حذف",
            "احذف",
            "مسح",
            "امسح",
            "ازالة",
            "إزالة",
            "delete",
            "remove"
    };

    private static final String[] OPEN_WORDS = {
            "افتح",
            "فتح",
            "شغل",
            "شغل لي",
            "launch",
            "open",
            "start",
            "run"
    };

    private static final String[] SEARCH_WORDS = {
            "ابحث",
            "بحث",
            "قلب",
            "قلب ليا",
            "لقى",
            "find",
            "search",
            "look up"
    };

    private static final String[] VOICE_WORDS = {
            "صوت",
            "صوتي",
            "سمع",
            "ميكروفون",
            "microphone",
            "voice"
    };

    private static final String[] DEVELOPMENT_WORDS = {
            "بني",
            "ابني",
            "صوب",
            "طور",
            "طوّر",
            "برمج",
            "برمجة",
            "كود",
            "كودات",
            "ملف جافا",
            "build",
            "develop",
            "development",
            "code",
            "program"
    };

    private static final String[] SETTINGS_WORDS = {
            "اعدادات",
            "إعدادات",
            "ضبط",
            "setting",
            "settings",
            "config",
            "configuration"
    };

    private static final String[] INFORMATION_WORDS = {
            "شنو",
            "اشنو",
            "ما هو",
            "ماهي",
            "معلومات",
            "شرح",
            "كيف",
            "علاش",
            "what",
            "why",
            "how",
            "information",
            "explain"
    };

    private static final String[] NETWORK_WORDS = {
            "انترنت",
            "الانترنت",
            "نت",
            "web",
            "internet",
            "online"
    };

    /**
     * يحلل الأمر.
     */
    public Result analyze(String command) {

        String normalized =
                normalize(command);

        if (normalized.isEmpty()) {
            return Result.invalid(
                    command,
                    "الأمر فارغ."
            );
        }

        IntentType intentType =
                detectIntent(normalized);

        ActionType actionType =
                detectAction(normalized, intentType);

        boolean destructive =
                containsAny(
                        normalized,
                        DELETE_WORDS
                );

        boolean sensitive =
                destructive
                        || intentType == IntentType.DEVELOPMENT
                        || intentType == IntentType.SETTINGS;

        Set<String> keywords =
                extractKeywords(normalized);

        List<String> targets =
                extractTargets(
                        normalized,
                        intentType
                );

        List<Requirement> requirements =
                detectRequirements(
                        normalized,
                        intentType,
                        destructive
                );

        String capabilityId =
                createCapabilityId(
                        intentType
                );

        String goal =
                createGoal(
                        normalized,
                        intentType,
                        actionType
                );

        return Result.valid(
                command,
                normalized,
                intentType,
                actionType,
                capabilityId,
                goal,
                keywords,
                targets,
                requirements,
                sensitive,
                destructive
        );
    }

    /**
     * تنظيف وتوحيد الأمر.
     */
    public String normalize(String command) {

        if (command == null) {
            return "";
        }

        String value =
                command
                        .trim()
                        .replaceAll("\\s+", " ");

        if (value.isEmpty()) {
            return "";
        }

        return value.toLowerCase(
                Locale.ROOT
        );
    }

    /**
     * تحديد نوع Intent.
     */
    public IntentType detectIntent(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (containsAny(
                text,
                REMINDER_WORDS
        )) {
            return IntentType.REMINDER;
        }

        if (containsAny(
                text,
                DEVELOPMENT_WORDS
        )) {
            return IntentType.DEVELOPMENT;
        }

        if (containsAny(
                text,
                DELETE_WORDS
        ) && containsAny(
                text,
                FILE_WORDS
        )) {
            return IntentType.FILE_OPERATION;
        }

        if (containsAny(
                text,
                FILE_WORDS
        )) {
            return IntentType.FILE_OPERATION;
        }

        if (containsAny(
                text,
                VOICE_WORDS
        )) {
            return IntentType.VOICE;
        }

        if (containsAny(
                text,
                SEARCH_WORDS
        )) {
            return IntentType.SEARCH;
        }

        if (containsAny(
                text,
                SETTINGS_WORDS
        )) {
            return IntentType.SETTINGS;
        }

        if (containsAny(
                text,
                OPEN_WORDS
        )) {
            return IntentType.APP_ACTION;
        }

        if (containsAny(
                text,
                INFORMATION_WORDS
        )) {
            return IntentType.INFORMATION;
        }

        if (containsAny(
                text,
                NETWORK_WORDS
        )) {
            return IntentType.NETWORK;
        }

        return IntentType.GENERAL;
    }

    /**
     * تحديد Action.
     */
    public ActionType detectAction(
            String normalizedCommand,
            IntentType intentType
    ) {

        String text =
                normalize(normalizedCommand);

        if (intentType == null) {
            return ActionType.UNKNOWN;
        }

        switch (intentType) {

            case REMINDER:
                return ActionType.CREATE;

            case FILE_OPERATION:

                if (containsAny(
                        text,
                        DELETE_WORDS
                )) {
                    return ActionType.DELETE;
                }

                if (containsAny(
                        text,
                        "نقل",
                        "move"
                )) {
                    return ActionType.MOVE;
                }

                if (containsAny(
                        text,
                        "نسخ",
                        "copy"
                )) {
                    return ActionType.COPY;
                }

                if (containsAny(
                        text,
                        "اقرا",
                        "أقرأ",
                        "قرأ",
                        "read"
                )) {
                    return ActionType.READ;
                }

                if (containsAny(
                        text,
                        "كتب",
                        "اكتب",
                        "write"
                )) {
                    return ActionType.WRITE;
                }

                return ActionType.MANAGE;

            case APP_ACTION:
                return ActionType.OPEN;

            case SEARCH:
                return ActionType.SEARCH;

            case VOICE:
                return ActionType.LISTEN;

            case DEVELOPMENT:

                if (containsAny(
                        text,
                        "حذف",
                        "delete",
                        "remove"
                )) {
                    return ActionType.DELETE;
                }

                if (containsAny(
                        text,
                        "صلح",
                        "اصلح",
                        "إصلاح",
                        "fix",
                        "repair"
                )) {
                    return ActionType.REPAIR;
                }

                if (containsAny(
                        text,
                        "اختبر",
                        "test",
                        "tests"
                )) {
                    return ActionType.TEST;
                }

                return ActionType.BUILD;

            case SETTINGS:
                return ActionType.CONFIGURE;

            case INFORMATION:
                return ActionType.EXPLAIN;

            case NETWORK:
                return ActionType.CONNECT;

            case GENERAL:
            default:
                return ActionType.UNKNOWN;
        }
    }

    /**
     * استخراج الكلمات المهمة.
     */
    public Set<String> extractKeywords(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return Collections.emptySet();
        }

        String[] words =
                text.split(" ");

        Set<String> result =
                new LinkedHashSet<>();

        for (String word : words) {

            String cleaned =
                    word
                            .trim()
                            .replaceAll(
                                    "^[،,.!?؛:]+|[،,.!?؛:]+$",
                                    ""
                            );

            if (cleaned.length() >= 2) {
                result.add(cleaned);
            }
        }

        return Collections.unmodifiableSet(
                result
        );
    }

    /**
     * استخراج هدف تقريبي من الأمر.
     *
     * لا يدعي أنه فهم اللغة الطبيعية بالكامل.
     * الهدف هنا إعطاء طبقة التخطيط مادة منظمة
     * يمكن تطويرها لاحقاً.
     */
    public List<String> extractTargets(
            String normalizedCommand,
            IntentType intentType
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result =
                new ArrayList<>();

        switch (intentType) {

            case APP_ACTION:
                addAfterKeywords(
                        text,
                        result,
                        OPEN_WORDS
                );
                break;

            case SEARCH:
                addAfterKeywords(
                        text,
                        result,
                        SEARCH_WORDS
                );
                break;

            case REMINDER:
                addAfterKeywords(
                        text,
                        result,
                        REMINDER_WORDS
                );
                break;

            case FILE_OPERATION:
                addAfterKeywords(
                        text,
                        result,
                        FILE_WORDS
                );
                break;

            case DEVELOPMENT:
                addAfterKeywords(
                        text,
                        result,
                        DEVELOPMENT_WORDS
                );
                break;

            default:
                break;
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * اكتشاف المتطلبات المعروفة.
     */
    public List<Requirement> detectRequirements(
            String normalizedCommand,
            IntentType intentType,
            boolean destructive
    ) {

        String text =
                normalize(normalizedCommand);

        List<Requirement> result =
                new ArrayList<>();

        if (intentType == IntentType.REMINDER) {

            result.add(
                    Requirement.NOTIFICATIONS
            );

            result.add(
                    Requirement.BACKGROUND_EXECUTION
            );
        }

        if (intentType == IntentType.FILE_OPERATION) {

            result.add(
                    Requirement.FILE_ACCESS
            );

            if (destructive) {
                result.add(
                        Requirement.DESTRUCTIVE_OPERATION
                );
            }
        }

        if (intentType == IntentType.SEARCH
                || containsAny(
                text,
                NETWORK_WORDS
        )) {

            result.add(
                    Requirement.NETWORK
            );
        }

        if (intentType == IntentType.VOICE) {

            result.add(
                    Requirement.MICROPHONE
            );
        }

        if (intentType == IntentType.DEVELOPMENT) {

            result.add(
                    Requirement.PROJECT_ACCESS
            );

            result.add(
                    Requirement.BUILD_ACCESS
            );

            result.add(
                    Requirement.TEST_ACCESS
            );

            result.add(
                    Requirement.OWNER_AUTHORIZATION
            );
        }

        if (intentType == IntentType.SETTINGS) {

            result.add(
                    Requirement.OWNER_AUTHORIZATION
            );
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * إنشاء ID ثابت للقدرة.
     */
    public String createCapabilityId(
            IntentType intentType
    ) {

        if (intentType == null) {
            return "brain.general";
        }

        return "brain."
                + intentType
                .name()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    /**
     * إنشاء الهدف الذي سيرسل للطبقات الأعلى.
     */
    public String createGoal(
            String normalizedCommand,
            IntentType intentType,
            ActionType actionType
    ) {

        String command =
                normalize(normalizedCommand);

        if (command.isEmpty()) {
            return "";
        }

        String intent =
                intentType == null
                        ? "GENERAL"
                        : intentType.name();

        String action =
                actionType == null
                        ? "UNKNOWN"
                        : actionType.name();

        return "Execute user request. "
                + "Intent=" + intent
                + ", Action=" + action
                + ", Command=" + command;
    }

    private void addAfterKeywords(
            String text,
            List<String> result,
            String[] keywords
    ) {

        for (String keyword : keywords) {

            String normalizedKeyword =
                    normalize(keyword);

            if (normalizedKeyword.isEmpty()) {
                continue;
            }

            int index =
                    text.indexOf(
                            normalizedKeyword
                    );

            if (index < 0) {
                continue;
            }

            int start =
                    index
                            + normalizedKeyword.length();

            if (start >= text.length()) {
                continue;
            }

            String remaining =
                    text.substring(start)
                            .trim();

            if (!remaining.isEmpty()) {
                result.add(remaining);
                return;
            }
        }
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

            if (value == null) {
                continue;
            }

            String normalizedValue =
                    normalize(value);

            if (!normalizedValue.isEmpty()
                    && text.contains(
                    normalizedValue
            )) {
                return true;
            }
        }

        return false;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * أنواع Intent.
     */
    public enum IntentType {

        REMINDER,

        FILE_OPERATION,

        APP_ACTION,

        SEARCH,

        VOICE,

        DEVELOPMENT,

        SETTINGS,

        INFORMATION,

        NETWORK,

        GENERAL
    }

    /**
     * أنواع العمليات.
     */
    public enum ActionType {

        CREATE,

        READ,

        WRITE,

        DELETE,

        MOVE,

        COPY,

        MANAGE,

        OPEN,

        SEARCH,

        LISTEN,

        BUILD,

        REPAIR,

        TEST,

        CONFIGURE,

        EXPLAIN,

        CONNECT,

        UNKNOWN
    }

    /**
     * المتطلبات المعروفة.
     */
    public enum Requirement {

        NOTIFICATIONS,

        BACKGROUND_EXECUTION,

        FILE_ACCESS,

        DESTRUCTIVE_OPERATION,

        NETWORK,

        MICROPHONE,

        PROJECT_ACCESS,

        BUILD_ACCESS,

        TEST_ACCESS,

        OWNER_AUTHORIZATION
    }

    /**
     * نتيجة التحليل.
     */
    public static final class Result {

        private final boolean valid;
        private final String originalCommand;
        private final String normalizedCommand;
        private final IntentType intentType;
        private final ActionType actionType;
        private final String capabilityId;
        private final String goal;
        private final Set<String> keywords;
        private final List<String> targets;
        private final List<Requirement> requirements;
        private final boolean sensitive;
        private final boolean destructive;
        private final String message;

        private Result(
                boolean valid,
                String originalCommand,
                String normalizedCommand,
                IntentType intentType,
                ActionType actionType,
                String capabilityId,
                String goal,
                Set<String> keywords,
                List<String> targets,
                List<Requirement> requirements,
                boolean sensitive,
                boolean destructive,
                String message
        ) {

            this.valid = valid;
            this.originalCommand = originalCommand;
            this.normalizedCommand = normalizedCommand;
            this.intentType = intentType;
            this.actionType = actionType;
            this.capabilityId = capabilityId;
            this.goal = goal;

            this.keywords =
                    Collections.unmodifiableSet(
                            new LinkedHashSet<>(
                                    keywords
                            )
                    );

            this.targets =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    targets
                            )
                    );

            this.requirements =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    requirements
                            )
                    );

            this.sensitive = sensitive;
            this.destructive = destructive;
            this.message =
                    message == null
                            ? ""
                            : message;
        }

        private static Result valid(
                String originalCommand,
                String normalizedCommand,
                IntentType intentType,
                ActionType actionType,
                String capabilityId,
                String goal,
                Set<String> keywords,
                List<String> targets,
                List<Requirement> requirements,
                boolean sensitive,
                boolean destructive
        ) {

            return new Result(
                    true,
                    originalCommand,
                    normalizedCommand,
                    intentType,
                    actionType,
                    capabilityId,
                    goal,
                    keywords,
                    targets,
                    requirements,
                    sensitive,
                    destructive,
                    "Command understood."
            );
        }

        private static Result invalid(
                String originalCommand,
                String message
        ) {

            return new Result(
                    false,
                    originalCommand,
                    "",
                    IntentType.GENERAL,
                    ActionType.UNKNOWN,
                    "brain.invalid",
                    "",
                    Collections.emptySet(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false,
                    false,
                    message
            );
        }

        public boolean isValid() {
            return valid;
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

        public ActionType getActionType() {
            return actionType;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public String getGoal() {
            return goal;
        }

        public Set<String> getKeywords() {
            return keywords;
        }

        public List<String> getTargets() {
            return targets;
        }

        public List<Requirement> getRequirements() {
            return requirements;
        }

        public boolean isSensitive() {
            return sensitive;
        }

        public boolean isDestructive() {
            return destructive;
        }

        public boolean requiresOwnerAuthorization() {

            return requirements.contains(
                    Requirement.OWNER_AUTHORIZATION
            );
        }

        public boolean requiresNetwork() {

            return requirements.contains(
                    Requirement.NETWORK
            );
        }

        public boolean requiresMicrophone() {

            return requirements.contains(
                    Requirement.MICROPHONE
            );
        }

        public boolean requiresFileAccess() {

            return requirements.contains(
                    Requirement.FILE_ACCESS
            );
        }

        public boolean requiresBuild() {

            return requirements.contains(
                    Requirement.BUILD_ACCESS
            );
        }

        public boolean requiresTesting() {

            return requirements.contains(
                    Requirement.TEST_ACCESS
            );
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {

            return "CommandUnderstanding.Result{" +
                    "valid=" + valid +
                    ", intentType=" +
                    intentType +
                    ", actionType=" +
                    actionType +
                    ", capabilityId='" +
                    capabilityId + '\'' +
                    ", sensitive=" +
                    sensitive +
                    ", destructive=" +
                    destructive +
                    '}';
        }
    }
}