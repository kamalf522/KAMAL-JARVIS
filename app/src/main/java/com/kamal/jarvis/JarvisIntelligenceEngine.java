package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS Local Intelligence Engine
 *
 * مسؤول عن:
 * - تنظيف وفهم أوامر العربية والدارجة.
 * - توحيد صيغ الأوامر.
 * - استخراج النية الأساسية من الأمر.
 * - حفظ المحادثات محليا.
 * - استرجاع السياق الأخير.
 * - إعطاء ردود حوارية أساسية.
 * - توفير طبقة ذكاء محلية يمكن لـ JarvisCore استعمالها.
 */
public class JarvisIntelligenceEngine {

    private static final String PREFS_NAME =
            "JARVIS_INTELLIGENCE";

    private static final String HISTORY_KEY =
            "conversation_history";

    private static final String LAST_COMMAND_KEY =
            "last_command";

    private static final String LAST_RESPONSE_KEY =
            "last_response";

    private static final int MAX_HISTORY = 30;

    private final SharedPreferences preferences;

    public JarvisIntelligenceEngine(Context context) {

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    // =========================================================
    // MAIN INTELLIGENCE PIPELINE
    // =========================================================

    public String understand(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "";
        }

        return prepareCommand(command);
    }

    public String prepareCommand(String command) {

        if (command == null) {
            return "";
        }

        String value = clean(command);

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

        for (String prefix : prefixes) {

            String normalizedPrefix =
                    clean(prefix);

            if (value.startsWith(normalizedPrefix)
                    && value.length()
                    > normalizedPrefix.length()) {

                value =
                        value.substring(
                                normalizedPrefix.length()
                        ).trim();

                break;
            }
        }

        // =====================================================
        // DARija ACTION ALIASES
        // =====================================================

        value = value
                .replace("حل ليا ", "افتح ")
                .replace("حل ليا", "افتح")
                .replace("حل لي ", "افتح ")
                .replace("حل لي", "افتح")
                .replace("فتح ليا ", "افتح ")
                .replace("فتح لي ", "افتح ")
                .replace("بغيت نفتح ", "افتح ")
                .replace("بغيت نحل ", "افتح ")
                .replace("مشيني ل ", "افتح ")
                .replace("ديني ل ", "افتح ")
                .replace("كتب ليا ", "اكتب ")
                .replace("كتب لي ", "اكتب ")
                .replace("سيفط ليا ", "اكتب ")
                .replace("قلب ليا على ", "ابحث على ")
                .replace("قلب ليا ف ", "ابحث في جوجل ")
                .replace("قلب ليا في ", "ابحث في جوجل ")
                .replace("قلب ليا ", "ابحث ")
                .replace("شنو هو ", "ما هو ")
                .replace("شنو هي ", "ما هي ");

        // =====================================================
        // VOICE VARIANTS
        // =====================================================

        value = value
                .replace("اليوتوب", "يوتيوب")
                .replace("اليوتيوب", "يوتيوب")
                .replace("الواتس", "واتساب")
                .replace("واتس اب", "واتساب")
                .replace("واتساب", "واتساب")
                .replace("انستا", "انستغرام")
                .replace("انستى", "انستغرام")
                .replace("الفايس", "فيسبوك")
                .replace("فيس بوك", "فيسبوك")
                .replace("كوكل", "جوجل")
                .replace("غوغل", "جوجل");

        return value.trim();
    }

    // =========================================================
    // INTENT DETECTION
    // =========================================================

    public String detectIntent(String command) {

        String value =
                prepareCommand(command);

        if (value.isEmpty()) {
            return "empty";
        }

        if (contains(
                value,
                "شنو قلت ليك قبل",
                "اش قلت ليك قبل",
                "اخر حاجة قلت ليك",
                "آخر حاجة قلت ليك"
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

        if (contains(
                value,
                "شنو حافظ",
                "شنو كتعقل",
                "الذاكرة",
                "memory"
        )) {
            return "memory";
        }

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

        if (contains(
                value,
                "حالة النظام",
                "status",
                "كيف داير",
                "system status"
        )) {
            return "status";
        }

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

        if (contains(
                value,
                "افتح ",
                "شغل ",
                "دخلني ل"
        )) {
            return "open";
        }

        if (contains(
                value,
                "ابحث ",
                "قلب ",
                "search "
        )) {
            return "search";
        }

        if (contains(
                value,
                "اكتب ",
                "كتب "
        )) {
            return "write";
        }

        if (contains(
                value,
                "حفظ ",
                "سجل ",
                "تذكر "
        )) {
            return "memory_save";
        }

        return "unknown";
    }

    // =========================================================
    // CONVERSATION INTERCEPTION
    // =========================================================

    public String intercept(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return null;
        }

        String value =
                prepareCommand(command);

        String intent =
                detectIntent(value);

        switch (intent) {

            case "conversation_history":
                return getHistorySummary();

            case "presence":
                return "هنا كمال. JARVIS حاضر.";

            case "identity":
                return
                        "أنا Kamal JARVIS، "
                                + "مساعد محلي للهاتف. "
                                + "كنفهم الأوامر بالعربية والدارجة، "
                                + "وكنربطها بالذاكرة والمهام "
                                + "والتعلم والقدرات المتاحة فالهاتف.";

            case "capabilities":
                return
                        "نقدر نعاونك فالأوامر، الذاكرة، "
                                + "التعلم، المهام، التخطيط، "
                                + "الشاشة، الإشعارات، الأتمتة "
                                + "والتطور الذاتي حسب الصلاحيات المتاحة.";

            case "chat":
                return
                        "أكيد كمال. أنا معاك. "
                                + "قول ليا شنو فبالك.";

            default:
                return null;
        }
    }

    // =========================================================
    // SAVE CONVERSATION
    // =========================================================

    public void recordTurn(
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

        for (String entry : history) {

            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append(entry);
        }

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
                .apply();
    }

    // =========================================================
    // LAST CONTEXT
    // =========================================================

    public String getLastCommand() {

        return preferences.getString(
                LAST_COMMAND_KEY,
                ""
        );
    }

    public String getLastResponse() {

        return preferences.getString(
                LAST_RESPONSE_KEY,
                ""
        );
    }

    public String getConversationContext() {

        String command =
                getLastCommand();

        String response =
                getLastResponse();

        if (command.isEmpty()) {
            return "مازال ما عنديش سياق سابق.";
        }

        return
                "آخر أمر: "
                        + command
                        + "\nآخر رد: "
                        + response;
    }

    // =========================================================
    // HISTORY
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
                raw.split("\n");

        for (String line : lines) {

            if (!line.trim().isEmpty()) {

                result.add(
                        line.trim()
                );
            }
        }

        return result;
    }

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

            result.append("• ")
                    .append(
                            history.get(i)
                    )
                    .append("\n");
        }

        return
                result.toString().trim();
    }

    // =========================================================
    // TEXT NORMALIZATION
    // =========================================================

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ؤ", "و")
                .replace("ئ", "ي")
                .replace("؟", "")
                .replace("!", "")
                .replace("،", " ")
                .replaceAll("\\s+", " ");
    }

    // =========================================================
    // TEXT MATCHING
    // =========================================================

    private boolean contains(
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

            String target =
                    clean(value);

            if (!target.isEmpty()
                    && text.contains(target)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            return preferences != null;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Intelligence Engine: ONLINE ✓\n"
                            + "Command Normalization: ONLINE ✓\n"
                            + "Intent Detection: ONLINE ✓\n"
                            + "Conversation Memory: ONLINE ✓";
        }

        return
                "Intelligence Engine: ERROR ⚠";
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return null;
    }
}