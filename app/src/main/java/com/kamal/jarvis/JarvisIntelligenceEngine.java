package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JarvisIntelligenceEngine {

    private static final String PREFS_NAME =
            "JARVIS_INTELLIGENCE";

    private static final String HISTORY_KEY =
            "conversation_history";

    private static final String LAST_COMMAND_KEY =
            "last_command";

    private static final String LAST_RESPONSE_KEY =
            "last_response";

    private static final String LAST_INTENT_KEY =
            "last_intent";

    private static final String LAST_CONFIDENCE_KEY =
            "last_confidence";

    private static final String COMMAND_COUNT_KEY =
            "command_count";

    private static final int MAX_HISTORY = 30;

    private final Context context;

    private final SharedPreferences preferences;

    private final MemoryManager memoryManager;

    private final CommandLearningEngine commandLearningEngine;

    private final DecisionEngine decisionEngine;

    private final ContextEngine contextEngine;

    public JarvisIntelligenceEngine(
            Context context
    ) {

        if (context == null) {

            throw new IllegalArgumentException(
                    "JarvisIntelligenceEngine context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        preferences =
                this.context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        memoryManager =
                new MemoryManager(
                        this.context
                );

        commandLearningEngine =
                new CommandLearningEngine(
                        this.context
                );

        decisionEngine =
                new DecisionEngine(
                        this.context
                );

        contextEngine =
                new ContextEngine(
                        this.context
                );
    }

    // =========================================================
    // UNDERSTAND
    // =========================================================

    public String understand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "";
        }

        return prepareCommand(
                command
        );
    }

    // =========================================================
    // PREPARE COMMAND
    // =========================================================

    public String prepareCommand(
            String command
    ) {

        if (command == null) {

            return "";
        }

        String value =
                clean(command);

        if (value.isEmpty()) {

            return "";
        }

        /*
         * نحيدو الكلمات اللي ما كتغيرش المعنى.
         */
        String[] prefixes = {

                "عفاك ",
                "عافاك ",
                "من فضلك ",
                "بغيتك ",
                "بغيت منك ",
                "ممكن ",
                "واش تقدر ",
                "تقدر ",
                "يلاه ",
                "يلا ",
                "جارفيس ",
                "jarvis "
        };

        for (String prefix :
                prefixes) {

            String normalizedPrefix =
                    clean(prefix);

            if (value.startsWith(
                    normalizedPrefix
            )
                    && value.length()
                    > normalizedPrefix.length()) {

                value =
                        value.substring(
                                normalizedPrefix.length()
                        ).trim();

                break;
            }
        }

        /*
         * تحويل صيغ دارجة مختلفة
         * لصيغ يفهمها Router.
         */
        value = value
                .replace(
                        "حل ليا ",
                        "افتح "
                )
                .replace(
                        "حل ليا",
                        "افتح"
                )
                .replace(
                        "حل لي ",
                        "افتح "
                )
                .replace(
                        "حل لي",
                        "افتح"
                )
                .replace(
                        "فتح ليا ",
                        "افتح "
                )
                .replace(
                        "فتح لي ",
                        "افتح "
                )
                .replace(
                        "بغيت نفتح ",
                        "افتح "
                )
                .replace(
                        "بغيت نحل ",
                        "افتح "
                )
                .replace(
                        "مشيني ل ",
                        "افتح "
                )
                .replace(
                        "ديني ل ",
                        "افتح "
                )
                .replace(
                        "كتب ليا ",
                        "اكتب "
                )
                .replace(
                        "كتب لي ",
                        "اكتب "
                )
                .replace(
                        "سيفط ليا ",
                        "اكتب "
                )
                .replace(
                        "قلب ليا على ",
                        "ابحث على "
                )
                .replace(
                        "قلب ليا ف ",
                        "ابحث في جوجل "
                )
                .replace(
                        "قلب ليا في ",
                        "ابحث في جوجل "
                )
                .replace(
                        "قلب ليا ",
                        "ابحث "
                )
                .replace(
                        "شنو هو ",
                        "ما هو "
                )
                .replace(
                        "شنو هي ",
                        "ما هي "
                );

        /*
         * توحيد أسماء التطبيقات.
         */
        value = value
                .replace(
                        "اليوتوب",
                        "يوتيوب"
                )
                .replace(
                        "اليوتيوب",
                        "يوتيوب"
                )
                .replace(
                        "الواتس",
                        "واتساب"
                )
                .replace(
                        "واتس اب",
                        "واتساب"
                )
                .replace(
                        "انستا",
                        "انستغرام"
                )
                .replace(
                        "انستى",
                        "انستغرام"
                )
                .replace(
                        "الفايس",
                        "فيسبوك"
                )
                .replace(
                        "فيس بوك",
                        "فيسبوك"
                )
                .replace(
                        "كوكل",
                        "جوجل"
                )
                .replace(
                        "غوغل",
                        "جوجل"
                );

        return normalizeSpaces(
                value
        );
    }

    // =========================================================
    // DETECT INTENT
    // =========================================================

    public String detectIntent(
            String command
    ) {

        String value =
                prepareCommand(
                        command
                );

        if (value.isEmpty()) {

            return "empty";
        }

        /*
         * أولا: الأوامر المتعلمة.
         */
        try {

            if (commandLearningEngine
                    .isCommandLearned(value)) {

                return "learned_command";
            }

        } catch (Exception ignored) {
        }

        /*
         * المحادثة والسياق.
         */
        if (contains(
                value,
                "شنو قلت ليك قبل",
                "اش قلت ليك قبل",
                "اخر حاجة قلت ليك",
                "آخر حاجة قلت ليك",
                "شنو وقع قبل"
        )) {

            return "conversation_history";
        }

        if (contains(
                value,
                "واش نتا هنا",
                "واش كاين",
                "جارفيس واش هنا",
                "jarvis are you there"
        )) {

            return "presence";
        }

        if (contains(
                value,
                "شكون نتا",
                "من نتا",
                "شنو نتا",
                "عرفني عليك"
        )) {

            return "identity";
        }

        if (contains(
                value,
                "شنو تقدر دير",
                "اش تقدر دير",
                "ماذا تستطيع",
                "شنو القدرات"
        )) {

            return "capabilities";
        }

        if (contains(
                value,
                "بغيت نهضر معاك",
                "بغيت غير نهضر",
                "غير نهضر معاك",
                "نهدرو شوية"
        )) {

            return "chat";
        }

        /*
         * الذاكرة.
         */
        if (contains(
                value,
                "شنو حافظ",
                "شنو كتعقل",
                "الذاكرة",
                "memory"
        )) {

            return "memory";
        }

        /*
         * التطور.
         */
        if (contains(
                value,
                "طور نفسك",
                "طور راسك",
                "بدا التطور",
                "evolution",
                "evolve"
        )) {

            return "evolution";
        }

        /*
         * الاختبار والتشخيص.
         */
        if (contains(
                value,
                "اختبر نفسك",
                "اختبار النظام",
                "اختبر النظام",
                "self test",
                "self-test"
        )) {

            return "self_test";
        }

        if (contains(
                value,
                "تشخيص",
                "شخص نفسك",
                "التشخيص الذاتي",
                "diagnose"
        )) {

            return "diagnosis";
        }

        /*
         * الحالة.
         */
        if (contains(
                value,
                "حالة النظام",
                "status",
                "كيف داير",
                "system status"
        )) {

            return "status";
        }

        /*
         * APK.
         */
        if (contains(
                value,
                "بني apk",
                "ابني apk",
                "بناء apk",
                "build apk",
                "assemble debug"
        )) {

            return "build_apk";
        }

        /*
         * فتح التطبيقات.
         */
        if (contains(
                value,
                "افتح ",
                "شغل ",
                "دخلني ل"
        )) {

            return "open";
        }

        /*
         * البحث.
         */
        if (contains(
                value,
                "ابحث ",
                "قلب ",
                "search "
        )) {

            return "search";
        }

        /*
         * الكتابة.
         */
        if (contains(
                value,
                "اكتب ",
                "كتب "
        )) {

            return "write";
        }

        /*
         * حفظ المعلومات.
         */
        if (contains(
                value,
                "حفظ ",
                "سجل ",
                "تذكر "
        )) {

            return "memory_save";
        }

        /*
         * المهام.
         */
        if (contains(
                value,
                "مهمة",
                "المهام",
                "task",
                "tasks"
        )) {

            return "task";
        }

        /*
         * التذكيرات.
         */
        if (contains(
                value,
                "ذكرني",
                "فكرني",
                "التذكير",
                "التذكيرات",
                "reminder"
        )) {

            return "reminder";
        }

        /*
         * التخطيط.
         */
        if (contains(
                value,
                "خطط ليا",
                "صاوب ليا خطة",
                "دير ليا خطة",
                "الخطة ديالي",
                "التخطيط",
                "plan",
                "planning"
        )) {

            return "planning";
        }

        /*
         * التعلم.
         */
        if (contains(
                value,
                "تعلم",
                "علمني",
                "بغيت نتعلم",
                "learning",
                "learn"
        )) {

            return "learning";
        }

        /*
         * الشاشة.
         */
        if (contains(
                value,
                "شوف الشاشة",
                "اقرا الشاشة",
                "اقرأ الشاشة",
                "شنو كاين فالشاشة",
                "حلل الشاشة",
                "screen"
        )) {

            return "screen";
        }

        /*
         * الأتمتة.
         */
        if (contains(
                value,
                "أتمتة",
                "اتمته",
                "اوتوماتيك",
                "أوتوماتيك",
                "automation"
        )) {

            return "automation";
        }

        /*
         * Android.
         */
        if (contains(
                value,
                "رجع للخلف",
                "رجع",
                "back"
        )) {

            return "back";
        }

        if (contains(
                value,
                "دير الرئيسية",
                "رجع للرئيسية",
                "الصفحة الرئيسية",
                "home"
        )) {

            return "home";
        }

        return "unknown";
    }

    // =========================================================
    // INTERCEPT
    // =========================================================

    public String intercept(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return null;
        }

        String value =
                prepareCommand(
                        command
                );

        if (value.isEmpty()) {

            return null;
        }

        /*
         * الأوامر المتعلمة عندها الأولوية.
         */
        try {

            String learned =
                    commandLearningEngine
                            .findLearnedCommand(
                                    value
                            );

            if (learned != null &&
                    !learned.trim().isEmpty()) {

                return
                        "تعلمت هاد الأمر من قبل ✓\n\n"
                        + learned.trim();
            }

        } catch (Exception ignored) {
        }

        String intent =
                detectIntent(
                        value
                );

        switch (intent) {

            case "conversation_history":

                return getHistorySummary();

            case "presence":

                return
                        "هنا كمال. JARVIS حاضر.";

            case "identity":

                return
                        "أنا Kamal JARVIS، "
                                + "مساعد محلي للهاتف. "
                                + "كنفهم الأوامر بالعربية والدارجة، "
                                + "وكنربطها بالذاكرة والتعلم "
                                + "والقرارات والمهام والقدرات "
                                + "المتاحة فالهاتف.";

            case "capabilities":

                return
                        "نقدر نعاونك فالأوامر، الذاكرة، "
                                + "التعلم، المهام، التخطيط، "
                                + "الشاشة، الإشعارات، الأتمتة "
                                + "والتطور الذاتي حسب الصلاحيات "
                                + "المتاحة.";

            case "chat":

                return
                        "أكيد كمال. أنا معاك. "
                                + "قول ليا شنو فبالك.";

            case "memory":

                return getMemorySummary();

            case "status":

                return getIntelligenceSummary();

            default:

                return null;
        }
    }

    // =========================================================
    // RECORD TURN
    // =========================================================

    public synchronized void recordTurn(
            String command,
            String response
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String cleanCommand =
                command.trim();

        String cleanResponse =
                response == null
                        ? ""
                        : response.trim();

        String intent =
                detectIntent(
                        cleanCommand
                );

        /*
         * نحاول ناخدو القرار الحقيقي
         * من DecisionEngine.
         */
        String confidence = "";

        try {

            String decision =
                    decisionEngine.decide(
                            cleanCommand
                    );

            confidence =
                    extractDecisionValue(
                            decision,
                            "الثقة:"
                    );

        } catch (Exception ignored) {
        }

        String item =
                cleanCommand
                        + " => "
                        + cleanResponse;

        List<String> history =
                getHistory();

        history.add(
                item.replace(
                        "\n",
                        " "
                )
        );

        while (
                history.size()
                        > MAX_HISTORY
        ) {

            history.remove(0);
        }

        StringBuilder builder =
                new StringBuilder();

        for (String entry :
                history) {

            if (builder.length() > 0) {

                builder.append(
                        "\n"
                );
            }

            builder.append(
                    entry
            );
        }

        int count =
                preferences.getInt(
                        COMMAND_COUNT_KEY,
                        0
                );

        preferences
                .edit()
                .putString(
                        HISTORY_KEY,
                        builder.toString()
                )
                .putString(
                        LAST_COMMAND_KEY,
                        cleanCommand
                )
                .putString(
                        LAST_RESPONSE_KEY,
                        cleanResponse
                )
                .putString(
                        LAST_INTENT_KEY,
                        intent
                )
                .putString(
                        LAST_CONFIDENCE_KEY,
                        confidence
                )
                .putInt(
                        COMMAND_COUNT_KEY,
                        count + 1
                )
                .apply();

        /*
         * تحديث ContextEngine.
         */
        try {

            contextEngine.updateCommand(
                    cleanCommand,
                    cleanResponse
            );

            contextEngine.updateResult(
                    cleanResponse
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // GET LAST COMMAND
    // =========================================================

    public String getLastCommand() {

        return preferences.getString(
                LAST_COMMAND_KEY,
                ""
        );
    }

    // =========================================================
    // GET LAST RESPONSE
    // =========================================================

    public String getLastResponse() {

        return preferences.getString(
                LAST_RESPONSE_KEY,
                ""
        );
    }

    // =========================================================
    // GET LAST INTENT
    // =========================================================

    public String getLastIntent() {

        return preferences.getString(
                LAST_INTENT_KEY,
                "unknown"
        );
    }

    // =========================================================
    // GET LAST CONFIDENCE
    // =========================================================

    public String getLastConfidence() {

        return preferences.getString(
                LAST_CONFIDENCE_KEY,
                ""
        );
    }

    // =========================================================
    // GET COMMAND COUNT
    // =========================================================

    public int getCommandCount() {

        return preferences.getInt(
                COMMAND_COUNT_KEY,
                0
        );
    }

    // =========================================================
    // GET CONVERSATION CONTEXT
    // =========================================================

    public String getConversationContext() {

        String command =
                getLastCommand();

        String response =
                getLastResponse();

        String intent =
                getLastIntent();

        if (command.isEmpty()) {

            return
                    "مازال ما عنديش سياق سابق.";
        }

        return
                "آخر أمر: "
                        + command
                        + "\nالنية: "
                        + intent
                        + "\nالثقة: "
                        + getLastConfidence()
                        + "\nآخر رد: "
                        + response;
    }

    // =========================================================
    // GET HISTORY
    // =========================================================

    private List<String> getHistory() {

        String raw =
                preferences.getString(
                        HISTORY_KEY,
                        ""
                );

        List<String> result =
                new ArrayList<>();

        if (raw.isEmpty()) {

            return result;
        }

        String[] lines =
                raw.split(
                        "\\n"
                );

        for (String line :
                lines) {

            if (line != null &&
                    !line.trim().isEmpty()) {

                result.add(
                        line.trim()
                );
            }
        }

        return result;
    }

    // =========================================================
    // HISTORY SUMMARY
    // =========================================================

    public String getHistorySummary() {

        List<String> history =
                getHistory();

        if (history.isEmpty()) {

            return
                    "مازال ما عنديش محادثة محفوظة.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "آخر المحادثات المحفوظة محليا:\n"
        );

        int start =
                Math.max(
                        0,
                        history.size() - 5
                );

        for (
                int i = start;
                i < history.size();
                i++
        ) {

            result.append(
                    "• "
            )
                    .append(
                            history.get(i)
                    )
                    .append(
                            "\n"
                    );
        }

        return
                result.toString().trim();
    }

    // =========================================================
    // MEMORY SUMMARY
    // =========================================================

    private String getMemorySummary() {

        try {

            return
                    "ذاكرة JARVIS:\n\n"
                            + "عدد الذكريات: "
                            + memoryManager
                            .getMemoryCount()
                            + "\n"
                            + "الأوامر المتعلمة: "
                            + commandLearningEngine
                            .getLearnedCommandCount()
                            + "\n"
                            + "آخر أمر: "
                            + safe(
                                    getLastCommand()
                            );

        } catch (Exception e) {

            return
                    "ذاكرة JARVIS متاحة، "
                            + "ولكن تعذر استخراج التقرير دابا.";
        }
    }

    // =========================================================
    // INTELLIGENCE SUMMARY
    // =========================================================

    private String getIntelligenceSummary() {

        return
                "=== JARVIS INTELLIGENCE ===\n\n"
                        + "الفهم: ONLINE ✓\n"
                        + "تطبيع الأوامر: ONLINE ✓\n"
                        + "اكتشاف النية: ONLINE ✓\n"
                        + "التعلم: "
                        + commandLearningEngine
                        .getStatus()
                        + "\n"
                        + "القرار: "
                        + decisionEngine
                        .getStatus()
                        + "\n"
                        + "السياق: "
                        + contextEngine
                        .getStatus()
                        + "\n"
                        + "الأوامر المعالجة: "
                        + getCommandCount();
    }

    // =========================================================
    // CLEAN
    // =========================================================

    private String clean(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "أ",
                        "ا"
                )
                .replace(
                        "إ",
                        "ا"
                )
                .replace(
                        "آ",
                        "ا"
                )
                .replace(
                        "ة",
                        "ه"
                )
                .replace(
                        "ؤ",
                        "و"
                )
                .replace(
                        "ئ",
                        "ي"
                )
                .replace(
                        "؟",
                        ""
                )
                .replace(
                        "!",
                        ""
                )
                .replace(
                        "،",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    // =========================================================
    // NORMALIZE SPACES
    // =========================================================

    private String normalizeSpaces(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    // =========================================================
    // CONTAINS
    // =========================================================

    private boolean contains(
            String text,
            String... values
    ) {

        if (text == null ||
                values == null) {

            return false;
        }

        String normalizedText =
                clean(text);

        for (String value :
                values) {

            if (value == null) {

                continue;
            }

            String target =
                    clean(value);

            if (!target.isEmpty()
                    && normalizedText
                    .contains(target)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // EXTRACT DECISION VALUE
    // =========================================================

    private String extractDecisionValue(
            String source,
            String marker
    ) {

        if (source == null ||
                marker == null) {

            return "";
        }

        try {

            int start =
                    source.indexOf(
                            marker
                    );

            if (start < 0) {

                return "";
            }

            start +=
                    marker.length();

            int end =
                    source.indexOf(
                            "\n",
                            start
                    );

            if (end < 0) {

                end =
                        source.length();
            }

            String value =
                    source.substring(
                            start,
                            end
                    ).trim();

            return value;

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "غير متوفر";
        }

        return value.trim();
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            return context != null
                    && preferences != null
                    && memoryManager != null
                    && commandLearningEngine
                    .isHealthy()
                    && decisionEngine
                    .isHealthy()
                    && contextEngine
                    .isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Intelligence Engine: ONLINE ✓\n"
                            + "Command Normalization: ONLINE ✓\n"
                            + "Intent Detection: ONLINE ✓\n"
                            + "Command Learning: ONLINE ✓\n"
                            + "Decision Integration: ONLINE ✓\n"
                            + "Context Integration: ONLINE ✓\n"
                            + "Conversation Memory: ONLINE ✓";
        }

        return
                "Intelligence Engine: ERROR ⚠";
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return context;
    }
}