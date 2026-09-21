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
 * - فهم بعض صيغ الدارجة والعربية.
 * - تنظيف أوامر الصوت.
 * - حفظ آخر المحادثات محليا.
 * - الرد على بعض الأسئلة الحوارية.
 * - تجهيز طبقة يمكن ربطها لاحقا بنموذج AI حقيقي.
 */
public class JarvisIntelligenceEngine {

    private static final String PREFS_NAME =
            "JARVIS_INTELLIGENCE";

    private static final String HISTORY_KEY =
            "conversation_history";

    private static final int MAX_HISTORY = 20;

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
    // COMMAND PREPARATION
    // =========================================================

    public String prepareCommand(
            String command
    ) {

        if (command == null) {
            return "";
        }

        String value =
                clean(command);

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
                "يلا "
        };

        for (String prefix : prefixes) {

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

        // =====================================================
        // DARija ACTION ALIASES
        // =====================================================

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
                );

        // =====================================================
        // VOICE VARIANTS
        // =====================================================

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
                );

        return value.trim();
    }

    // =========================================================
    // CONVERSATION INTERCEPTION
    // =========================================================

    public String intercept(
            String command
    ) {

        if (command == null
                || command.trim().isEmpty()) {

            return null;
        }

        String value =
                clean(command);

        // -----------------------------------------------------
        // PREVIOUS CONVERSATION
        // -----------------------------------------------------

        if (contains(
                value,
                "شنو قلت ليك قبل",
                "شنو قلت لك قبل",
                "اش قلت ليك قبل",
                "اخر حاجة قلت ليك",
                "آخر حاجة قلت ليك"
        )) {

            return getHistorySummary();
        }

        // -----------------------------------------------------
        // MEMORY QUESTION
        // -----------------------------------------------------

        if (contains(
                value,
                "واش باقي فاكر",
                "باقي فاكرني",
                "شنو عارف عليا"
        )) {

            return
                    "كنقدر نحتافظ بالمعلومات اللي كتسجل ليا "
                    + "داخل ذاكرة JARVIS. "
                    + "قول ليا: شنو حافظ، باش نوريك الذاكرة.";
        }

        // -----------------------------------------------------
        // CHAT
        // -----------------------------------------------------

        if (contains(
                value,
                "بغيت نهضر معاك",
                "بغيت غير نهضر",
                "غير نهضر معاك",
                "نهدرو شوية"
        )) {

            return
                    "أكيد كمال. أنا معاك. "
                    + "قول ليا شنو فبالك.";
        }

        // -----------------------------------------------------
        // PRESENCE
        // -----------------------------------------------------

        if (contains(
                value,
                "واش نتا هنا",
                "واش كاين",
                "جارفيس واش هنا",
                "jarvis واش هنا",
                "jarvis are you there"
        )) {

            return
                    "هنا كمال. JARVIS حاضر.";
        }

        // -----------------------------------------------------
        // IDENTITY
        // -----------------------------------------------------

        if (contains(
                value,
                "شكون نتا",
                "من نتا",
                "شنو نتا",
                "عرفني عليك"
        )) {

            return
                    "أنا Kamal JARVIS، "
                    + "مساعد محلي للهاتف. "
                    + "كنفهم أوامر بالعربية والدارجة، "
                    + "وكنربطها بالذاكرة والمهام "
                    + "والتحكم المتاح فالهاتف.";
        }

        // -----------------------------------------------------
        // CAPABILITIES
        // -----------------------------------------------------

        if (contains(
                value,
                "شنو تقدر دير ليا",
                "اش تقدر دير ليا",
                "ماذا تستطيع"
        )) {

            return
                    "نقدر نعاونك فالأوامر ديال الهاتف، "
                    + "الذاكرة، المهام، التخطيط، الشاشة، "
                    + "الإشعارات، والأوامر اللي كتعلمنيها. "
                    + "بعض قدرات الهاتف كتحتاج صلاحيات Android.";
        }

        return null;
    }

    // =========================================================
    // SAVE CONVERSATION
    // =========================================================

    public void recordTurn(
            String command,
            String response
    ) {

        if (command == null
                || command.trim().isEmpty()) {

            return;
        }

        String item =
                command.trim()
                + " => "
                + (
                    response == null
                            ? ""
                            : response.trim()
                );

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
                .apply();
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

    // =========================================================
    // HISTORY SUMMARY
    // =========================================================

    private String getHistorySummary() {

        List<String> history =
                getHistory();

        if (history.isEmpty()) {

            return
                    "مازال ما عنديش محادثة محفوظة.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "آخر المحادثات اللي عندي محليا:\n"
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

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // CLEAN TEXT
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
                .replace("؟", "")
                .replace("!", "")
                .replace("،", " ")
                .replaceAll("\\s+", " ");
    }

    // =========================================================
    // CONTAINS
    // =========================================================

    private boolean contains(
            String text,
            String... values
    ) {

        if (text == null) {
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
}