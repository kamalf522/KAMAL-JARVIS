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
 * طبقة الفهم العامة.
 *
 * الهدف الأساسي:
 *
 * كلام المستخدم
 *      ↓
 * فهم الطلب
 *      ↓
 * استخراج الهدف الكامل
 *      ↓
 * تحديد الفعل والسياق والمتطلبات
 *      ↓
 * تسليم المعنى للـ Brain / Planning / Evolution
 *
 * هذه الطبقة لا تنفذ الأوامر.
 * ولا تمنح الصلاحيات.
 * ولا تتجاوز Security Boundary.
 *
 * مبدأ مهم:
 *
 * أنواع Intent الموجودة هنا ليست حدوداً لقدرات JARVIS.
 * هي فقط تصنيفات مساعدة.
 *
 * أي طلب جديد غير معروف يجب أن يبقى قابلاً للفهم
 * والتخطيط والتطوير، بدل اعتباره أمراً غير صالح.
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
            "مجلد",
            "مجلدات",
            "folder",
            "folders",
            "file",
            "files"
    };

    private static final String[] DELETE_WORDS = {
            "حذف",
            "احذف",
            "مسح",
            "امسح",
            "ازالة",
            "إزالة",
            "حيد",
            "حيّد",
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
            "لقيا",
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
            "طور راسك",
            "طور نفسك",
            "طور ذاتك",
            "برمج",
            "برمجة",
            "كود",
            "كودات",
            "ملف جافا",
            "build",
            "develop",
            "development",
            "code",
            "program",
            "improve",
            "evolve",
            "evolution",
            "upgrade"
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
            "أشنو",
            "ما هو",
            "ماهي",
            "معلومات",
            "شرح",
            "كيف",
            "علاش",
            "لماذا",
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
            "ويب",
            "web",
            "internet",
            "online"
    };

    private static final String[] QUESTION_WORDS = {
            "شنو",
            "اشنو",
            "أشنو",
            "شكون",
            "فين",
            "فاش",
            "كيفاش",
            "علاش",
            "واش",
            "ما",
            "ماذا",
            "من",
            "أين",
            "متى",
            "لماذا",
            "كيف",
            "what",
            "who",
            "where",
            "when",
            "why",
            "how"
    };

    private static final String[] SELF_REFERENCE_WORDS = {
            "جارڤيس",
            "جارفس",
            "jarvis",
            "راسك",
            "نفسك",
            "ذاتك",
            "قدراتك",
            "ذكاءك",
            "فهمك",
            "yourself",
            "your",
            "you"
    };

    private static final String[] CONVERSATION_WORDS = {
            "سلام",
            "اهلا",
            "أهلا",
            "مرحبا",
            "مراحب",
            "صباح الخير",
            "مساء الخير",
            "شكرا",
            "شكراً",
            "thanks",
            "hello",
            "hi",
            "hey"
    };

    /**
     * تحليل شامل للطلب.
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
                detectAction(
                        normalized,
                        intentType
                );

        boolean question =
                isQuestion(normalized);

        boolean conversational =
                isConversational(normalized);

        boolean selfReference =
                containsAny(
                        normalized,
                        SELF_REFERENCE_WORDS
                );

        boolean evolutionRequest =
                containsAny(
                        normalized,
                        DEVELOPMENT_WORDS
                )
                && (
                        selfReference
                                || containsAny(
                                normalized,
                                "طور",
                                "طوّر",
                                "تطور",
                                "طور راسك",
                                "طور نفسك",
                                "evolve",
                                "improve",
                                "upgrade"
                        )
                );

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

        /*
         * أي طلب جديد يجب أن يبقى قابلاً للتطور.
         *
         * لا نعتبر GENERAL = خطأ.
         */
        String capabilityId =
                createCapabilityId(
                        normalized,
                        intentType
                );

        String goal =
                createGoal(
                        normalized,
                        intentType,
                        actionType,
                        question,
                        conversational,
                        evolutionRequest
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
                destructive,
                question,
                conversational,
                selfReference,
                evolutionRequest
        );
    }

    /**
     * تنظيف النص وتوحيده.
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
     * تحديد المجال العام للطلب.
     *
     * هذا التصنيف ليس حدوداً لقدرات JARVIS.
     */
    public IntentType detectIntent(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return IntentType.GENERAL;
        }

        /*
         * التطور الذاتي له أولوية عندما يكون المستخدم
         * يطلب من JARVIS تطوير نفسه.
         */
        if (containsAny(
                text,
                DEVELOPMENT_WORDS
        )) {
            return IntentType.DEVELOPMENT;
        }

        if (containsAny(
                text,
                REMINDER_WORDS
        )) {
            return IntentType.REMINDER;
        }

        if (containsAny(
                text,
                DELETE_WORDS
        )
                && containsAny(
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
                NETWORK_WORDS
        )) {
            return IntentType.NETWORK;
        }

        if (containsAny(
                text,
                INFORMATION_WORDS
        )
                || isQuestion(text)
                || isConversational(text)) {
            return IntentType.INFORMATION;
        }

        /*
         * أي شيء آخر يبقى GENERAL.
         *
         * GENERAL لا يعني:
         * "لا أفهم".
         *
         * بل يعني:
         * "لم أستطع تصنيف المجال مسبقاً،
         * لذلك يجب تمرير الطلب الكامل للطبقات
         * الأعلى لتحديد الهدف والقدرة المطلوبة."
         */
        return IntentType.GENERAL;
    }

    /**
     * تحديد الفعل العام.
     */
    public ActionType detectAction(
            String normalizedCommand,
            IntentType intentType
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty() ||
                intentType == null) {
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
                        "اختبار",
                        "test",
                        "tests"
                )) {
                    return ActionType.TEST;
                }

                return ActionType.BUILD;

            case SETTINGS:
                return ActionType.CONFIGURE;

            case INFORMATION:

                if (isQuestion(text)) {
                    return ActionType.EXPLAIN;
                }

                return ActionType.READ;

            case NETWORK:
                return ActionType.CONNECT;

            case GENERAL:

            default:

                /*
                 * لا نخترع Action.
                 *
                 * الطبقات الأعلى ستقرر ما يجب فعله
                 * بناءً على الهدف الكامل.
                 */
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
                                    "^[،,.!?؛:؟]+|[،,.!?؛:؟]+$",
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
     * استخراج هدف تقريبي.
     *
     * لا نعتبر هذا الفهم النهائي.
     * الهدف هو إعطاء النظام مادة خام
     * للـPlanning/Evolution.
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

        if (intentType == null) {
            result.add(text);

            return Collections.unmodifiableList(
                    result
            );
        }

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

                /*
                 * في التطور نريد الطلب الكامل،
                 * لأنه قد يصف قدرة جديدة بالكامل.
                 */
                result.add(text);

                break;

            case INFORMATION:

                /*
                 * السؤال نفسه هو الهدف.
                 */
                result.add(text);

                break;

            case NETWORK:

                result.add(text);

                break;

            case VOICE:

                result.add(text);

                break;

            case SETTINGS:

                result.add(text);

                break;

            case GENERAL:

            default:

                /*
                 * لا نرمي الطلب فقط لأنه غير مصنف.
                 */
                result.add(text);

                break;
        }

        if (result.isEmpty()) {
            result.add(text);
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
                || intentType == IntentType.NETWORK
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

        /*
         * العمليات المدمرة دائماً حساسة.
         */
        if (destructive
                && !result.contains(
                Requirement.DESTRUCTIVE_OPERATION
        )) {

            result.add(
                    Requirement.DESTRUCTIVE_OPERATION
            );
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * إنشاء ID للقدرة.
     *
     * المجالات المعروفة تستعمل IDs مستقرة.
     *
     * الطلبات العامة تستعمل بصمة مبنية على الطلب،
     * حتى لا تتحول جميع الطلبات المختلفة إلى
     * brain.general واحد.
     */
    public String createCapabilityId(
            String normalizedCommand,
            IntentType intentType
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return "brain.empty";
        }

        if (intentType == null ||
                intentType == IntentType.GENERAL) {

            return "goal."
                    + createStableFingerprint(
                    text
            );
        }

        return "brain."
                + intentType
                .name()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    /**
     * توافق مع الكود القديم.
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
     * إنشاء هدف غني للطبقات الأعلى.
     */
    public String createGoal(
            String normalizedCommand,
            IntentType intentType,
            ActionType actionType
    ) {

        return createGoal(
                normalizedCommand,
                intentType,
                actionType,
                isQuestion(normalizedCommand),
                isConversational(normalizedCommand),
                containsAny(
                        normalize(normalizedCommand),
                        DEVELOPMENT_WORDS
                )
        );
    }

    /**
     * إنشاء الهدف الكامل.
     *
     * الأهم هنا:
     *
     * لا نختصر طلب المستخدم في اسم Intent.
     *
     * الطلب الكامل يبقى محفوظاً داخل Goal
     * حتى تستطيع طبقات التخطيط والتطور استعماله.
     */
    public String createGoal(
            String normalizedCommand,
            IntentType intentType,
            ActionType actionType,
            boolean question,
            boolean conversational,
            boolean evolutionRequest
    ) {

        String command =
                normalize(normalizedCommand);

        if (command.isEmpty()) {
            return "";
        }

        String intent =
                intentType == null
                        ? IntentType.GENERAL.name()
                        : intentType.name();

        String action =
                actionType == null
                        ? ActionType.UNKNOWN.name()
                        : actionType.name();

        StringBuilder goal =
                new StringBuilder();

        goal.append(
                "Understand and fulfill the user's actual goal. "
        );

        goal.append(
                "UserRequest=\""
        );

        goal.append(command);

        goal.append(
                "\". "
        );

        goal.append(
                "Intent="
        );

        goal.append(intent);

        goal.append(
                ". Action="
        );

        goal.append(action);

        goal.append(
                ". Question="
        );

        goal.append(question);

        goal.append(
                ". Conversational="
        );

        goal.append(conversational);

        goal.append(
                ". EvolutionRequest="
        );

        goal.append(evolutionRequest);

        goal.append(
                ". Preserve the complete request as the source of truth; "
                        + "do not assume that the request is limited to "
                        + "predefined capabilities."
        );

        return goal.toString();
    }

    /**
     * هل النص سؤال؟
     */
    public boolean isQuestion(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return false;
        }

        if (text.contains("?")
                || text.contains("؟")) {
            return true;
        }

        return containsAny(
                text,
                QUESTION_WORDS
        );
    }

    /**
     * هل النص محادثة طبيعية؟
     */
    public boolean isConversational(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return false;
        }

        return containsAny(
                text,
                CONVERSATION_WORDS
        );
    }

    /**
     * هل المستخدم يتحدث عن JARVIS نفسه؟
     */
    public boolean isSelfReferential(
            String normalizedCommand
    ) {

        return containsAny(
                normalize(normalizedCommand),
                SELF_REFERENCE_WORDS
        );
    }

    /**
     * هل الطلب يطلب تطوير JARVIS أو قدراته؟
     */
    public boolean isEvolutionRequest(
            String normalizedCommand
    ) {

        String text =
                normalize(normalizedCommand);

        if (text.isEmpty()) {
            return false;
        }

        return containsAny(
                text,
                DEVELOPMENT_WORDS
        );
    }

    /**
     * بصمة بسيطة مستقرة للطلبات العامة.
     *
     * ليست تشفيراً أمنياً.
     * تستعمل فقط لإنشاء هوية مستقرة للهدف.
     */
    private String createStableFingerprint(
            String text
    ) {

        int hash =
                text.hashCode();

        String value =
                Integer.toHexString(
                        hash
                );

        if (value.startsWith("-")) {

            value =
                    value.substring(1);
        }

        return "general_"
                + value;
    }

    private void addAfterKeywords(
            String text,
            List<String> result,
            String[] keywords
    ) {

        if (text == null ||
                result == null ||
                keywords == null) {
            return;
        }

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

                result.add(
                        remaining
                );

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

        String normalizedText =
                normalize(text);

        if (normalizedText.isEmpty()) {
            return false;
        }

        for (String value : values) {

            if (value == null) {
                continue;
            }

            String normalizedValue =
                    normalize(value);

            if (!normalizedValue.isEmpty()
                    && normalizedText.contains(
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
     * أنواع المجالات المعروفة.
     *
     * هذه ليست حدوداً لقدرات JARVIS.
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
     * أنواع العمليات المعروفة.
     *
     * UNKNOWN تعني أن الفعل يحتاج تخطيطاً أعلى،
     * وليس أن الطلب غير صالح.
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

        private final boolean question;
        private final boolean conversational;
        private final boolean selfReferential;
        private final boolean evolutionRequest;

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
                boolean question,
                boolean conversational,
                boolean selfReferential,
                boolean evolutionRequest,
                String message
        ) {

            this.valid =
                    valid;

            this.originalCommand =
                    originalCommand;

            this.normalizedCommand =
                    normalizedCommand;

            this.intentType =
                    intentType;

            this.actionType =
                    actionType;

            this.capabilityId =
                    capabilityId;

            this.goal =
                    goal;

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

            this.sensitive =
                    sensitive;

            this.destructive =
                    destructive;

            this.question =
                    question;

            this.conversational =
                    conversational;

            this.selfReferential =
                    selfReferential;

            this.evolutionRequest =
                    evolutionRequest;

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
                boolean destructive,
                boolean question,
                boolean conversational,
                boolean selfReferential,
                boolean evolutionRequest
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
                    question,
                    conversational,
                    selfReferential,
                    evolutionRequest,
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
                    false,
                    false,
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

        public boolean isQuestion() {
            return question;
        }

        public boolean isConversational() {
            return conversational;
        }

        public boolean isSelfReferential() {
            return selfReferential;
        }

        public boolean isEvolutionRequest() {
            return evolutionRequest;
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
                    "valid=" +
                    valid +
                    ", intentType=" +
                    intentType +
                    ", actionType=" +
                    actionType +
                    ", capabilityId='" +
                    capabilityId +
                    '\'' +
                    ", question=" +
                    question +
                    ", conversational=" +
                    conversational +
                    ", selfReferential=" +
                    selfReferential +
                    ", evolutionRequest=" +
                    evolutionRequest +
                    ", sensitive=" +
                    sensitive +
                    ", destructive=" +
                    destructive +
                    '}';
        }
    }
}