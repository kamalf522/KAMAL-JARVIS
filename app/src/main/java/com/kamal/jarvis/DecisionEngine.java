package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS Decision Engine
 *
 * مسؤول عن:
 * - تحليل الأمر
 * - تحديد النية
 * - حساب الثقة
 * - تحديد الأولوية
 * - الاستفادة من الذاكرة والخطط والمهام
 * - حفظ آخر قرار
 *
 * هذا النظام لا ينفذ الأمر بنفسه.
 * هو طبقة القرار قبل التنفيذ.
 */
public class DecisionEngine {

    private static final String LAST_DECISION_KEY =
            "__last_decision__";

    private static final String DECISION_COUNT_KEY =
            "__decision_count__";

    private static final String LAST_TYPE_KEY =
            "__decision_last_type__";

    private static final String LAST_CONFIDENCE_KEY =
            "__decision_last_confidence__";

    private static final String LAST_PRIORITY_KEY =
            "__decision_last_priority__";

    private static final String LAST_COMMAND_KEY =
            "__decision_last_command__";

    private static final int MIN_CONFIDENCE = 20;
    private static final int MAX_CONFIDENCE = 99;

    private final Context context;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;
    private final PlanningEngine planningEngine;
    private final CommandLearningEngine commandLearningEngine;

    public DecisionEngine(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "DecisionEngine context cannot be null"
            );
        }

        this.context = context.getApplicationContext();

        memoryManager = new MemoryManager(this.context);
        taskManager = new TaskManager(this.context);
        planningEngine = new PlanningEngine(this.context);
        commandLearningEngine =
                new CommandLearningEngine(this.context);
    }

    // =========================================================
    // MAIN DECISION
    // =========================================================

    public synchronized String decide(String command) {

        if (command == null || command.trim().isEmpty()) {
            return "ما وصلني حتى أمر.";
        }

        String cleanCommand = normalize(command);

        try {

            DecisionResult result =
                    analyzeCommand(cleanCommand);

            saveDecision(
                    cleanCommand,
                    result
            );

            return buildDecisionReport(
                    cleanCommand,
                    result
            );

        } catch (Exception e) {

            DecisionResult fallback =
                    new DecisionResult(
                            "GENERAL",
                            "NORMAL",
                            "تحليل الأمر بشكل عام",
                            MIN_CONFIDENCE,
                            "تعذر التحليل المتقدم؛ تم استعمال القرار العام.",
                            Collections.singletonList("FALLBACK"),
                            null
                    );

            saveDecision(
                    cleanCommand,
                    fallback
            );

            return buildDecisionReport(
                    cleanCommand,
                    fallback
            );
        }
    }

    // =========================================================
    // ANALYSIS
    // =========================================================

    private DecisionResult analyzeCommand(String command) {

        String lower =
                command.toLowerCase(Locale.ROOT);

        List<CategoryScore> scores =
                new ArrayList<>();

        scores.add(scoreEvolution(lower));
        scores.add(scoreDevelopment(lower));
        scores.add(scoreReminder(lower));
        scores.add(scoreTask(lower));
        scores.add(scorePlanning(lower));
        scores.add(scoreLearning(lower));
        scores.add(scoreKnowledge(lower));
        scores.add(scoreScreen(lower));
        scores.add(scoreAndroid(lower));
        scores.add(scoreAutomation(lower));
        scores.add(scoreFitness(lower));
        scores.add(scoreBusiness(lower));
        scores.add(scoreMemory(lower));

        CategoryScore best =
                findBestScore(scores);

        CategoryScore second =
                findSecondBest(scores, best);

        int contextBoost =
                calculateContextBoost(
                        lower,
                        best
                );

        int learnedBoost =
                calculateLearnedBoost(
                        command,
                        best
                );

        int urgencyBoost =
                isUrgent(lower) ? 2 : 0;

        int finalScore =
                best.score
                        + contextBoost
                        + learnedBoost
                        + urgencyBoost;

        String priority =
                determinePriority(
                        lower,
                        finalScore,
                        best.type
                );

        int confidence =
                calculateConfidence(
                        finalScore,
                        second,
                        contextBoost,
                        learnedBoost
                );

        String reason =
                buildReason(
                        best,
                        second,
                        contextBoost,
                        learnedBoost
                );

        List<String> signals =
                collectSignals(
                        lower,
                        best,
                        contextBoost,
                        learnedBoost
                );

        String learnedAction = null;

        try {
            learnedAction =
                    commandLearningEngine
                            .findLearnedCommand(command);
        } catch (Exception ignored) {
        }

        String action = best.action;

        if (learnedAction != null
                && !learnedAction.trim().isEmpty()) {

            action =
                    "استعمال الإجراء المتعلم: "
                            + learnedAction;
        }

        return new DecisionResult(
                best.type,
                priority,
                action,
                confidence,
                reason,
                signals,
                learnedAction
        );
    }

    // =========================================================
    // CATEGORY SCORING
    // =========================================================

    private CategoryScore scoreEvolution(String text) {

        int score = 0;

        if (containsAny(
                text,
                "طور نفسك",
                "طور راسك",
                "طور جارفيس",
                "طور جارڤيس",
                "طور النظام",
                "تطور ذاتي",
                "تطوير ذاتي",
                "evolve",
                "evolution",
                "self improvement",
                "self improve"
        )) {
            score += 8;
        }

        if (containsAny(
                text,
                "تعلم من الاخطاء",
                "تعلم من الأخطاء",
                "حسن نفسك",
                "حسن راسك",
                "طور قدراتك"
        )) {
            score += 3;
        }

        return new CategoryScore(
                "EVOLUTION",
                "استخدام Evolution Engine",
                score,
                "الأمر مرتبط بالتطور والتحسين الذاتي."
        );
    }

    private CategoryScore scoreDevelopment(String text) {

        int score = 0;

        if (containsAny(
                text,
                "كود",
                "الكود",
                "برمج",
                "برمجة",
                "تطبيق",
                "apk",
                "java",
                "android",
                "software",
                "code",
                "develop",
                "github",
                "ملف"
        )) {
            score += 4;
        }

        if (containsAny(
                text,
                "صلح",
                "اصلح",
                "أصلح",
                "عدل",
                "بدل",
                "غير",
                "انشئ",
                "أنشئ",
                "اضف",
                "أضف",
                "طور التطبيق"
        )) {
            score += 3;
        }

        return new CategoryScore(
                "DEVELOPMENT",
                "استخدام أنظمة تطوير JARVIS",
                score,
                "الأمر مرتبط بالكود أو تطوير التطبيق."
        );
    }

    private CategoryScore scoreReminder(String text) {

        int score = 0;

        if (containsAny(
                text,
                "تذكير",
                "ذكرني",
                "فكرني",
                "reminder",
                "remind me"
        )) {
            score += 6;
        }

        if (containsAny(
                text,
                "غدا",
                "غداً",
                "اليوم",
                "بعد",
                "ساعة",
                "دقيقة",
                "وقت",
                "الساعة"
        )) {
            score += 2;
        }

        return new CategoryScore(
                "REMINDER",
                "استخدام Reminder Engine",
                score,
                "الأمر مرتبط بتذكير أو وقت."
        );
    }

    private CategoryScore scoreTask(String text) {

        int score = 0;

        if (containsAny(
                text,
                "مهمة",
                "المهمة",
                "مهامي",
                "task",
                "todo",
                "دير ليا",
                "قم ب",
                "خاصني ندير"
        )) {
            score += 5;
        }

        return new CategoryScore(
                "TASK",
                "استخدام Task Manager",
                score,
                "الأمر يبدو مرتبطاً بمهمة قابلة للتنفيذ."
        );
    }

    private CategoryScore scorePlanning(String text) {

        int score = 0;

        if (containsAny(
                text,
                "خطط",
                "خطة",
                "خطط لي",
                "برنامج",
                "روتين",
                "plan",
                "planning",
                "schedule"
        )) {
            score += 6;
        }

        return new CategoryScore(
                "PLANNING",
                "استخدام Planning Engine",
                score,
                "الأمر يطلب بناء خطة أو تنظيم خطوات."
        );
    }

    private CategoryScore scoreLearning(String text) {

        int score = 0;

        if (containsAny(
                text,
                "تعلم",
                "نتعلم",
                "تعلمت",
                "قرا",
                "قراءة",
                "دراسة",
                "درس",
                "مهارة",
                "learn",
                "study",
                "skill"
        )) {
            score += 5;
        }

        return new CategoryScore(
                "LEARNING",
                "استخدام Learning Engine",
                score,
                "الأمر مرتبط بالتعلم أو اكتساب مهارة."
        );
    }

    private CategoryScore scoreKnowledge(String text) {

        int score = 0;

        if (containsAny(
                text,
                "شنو كتعرف",
                "ماذا تعرف",
                "المعرفة",
                "معلومة",
                "معلومات",
                "تعرف على",
                "ذكرني ب",
                "knowledge",
                "information"
        )) {
            score += 5;
        }

        return new CategoryScore(
                "KNOWLEDGE",
                "استخدام Knowledge Engine",
                score,
                "الأمر مرتبط بالمعلومات أو المعرفة."
        );
    }

    private CategoryScore scoreScreen(String text) {

        int score = 0;

        if (containsAny(
                text,
                "شاشة",
                "الشاشة",
                "شنو باين",
                "شوف الشاشة",
                "screen",
                "what is on screen"
        )) {
            score += 6;
        }

        return new CategoryScore(
                "SCREEN",
                "استخدام Screen Intelligence",
                score,
                "الأمر مرتبط بفهم محتوى الشاشة."
        );
    }

    private CategoryScore scoreAndroid(String text) {

        int score = 0;

        if (containsAny(
                text,
                "wifi",
                "واي فاي",
                "بلوتوث",
                "bluetooth",
                "اعدادات",
                "إعدادات",
                "الهاتف",
                "phone",
                "setting",
                "settings",
                "بطارية",
                "صوت",
                "شاشة الهاتف"
        )) {
            score += 5;
        }

        return new CategoryScore(
                "ANDROID",
                "استخدام Android Control Engine",
                score,
                "الأمر مرتبط بالهاتف أو إعداداته."
        );
    }

    private CategoryScore scoreAutomation(String text) {

        int score = 0;

        if (containsAny(
                text,
                "اتمتة",
                "الأتمتة",
                "الاتمتة",
                "أتمت",
                "نفذ تلقائيا",
                "نفذ أوتوماتيكيا",
                "automation",
                "automate",
                "automatically"
        )) {
            score += 7;
        }

        return new CategoryScore(
                "AUTOMATION",
                "استخدام Automation Engine",
                score,
                "الأمر يطلب تنفيذ عملية آلية."
        );
    }

    private CategoryScore scoreFitness(String text) {

        int score = 0;

        if (containsAny(
                text,
                "رياضة",
                "رياضه",
                "عضلات",
                "جسم",
                "تمرين",
                "تمارين",
                "وزن",
                "fitness",
                "muscle",
                "workout",
                "body"
        )) {
            score += 4;
        }

        return new CategoryScore(
                "FITNESS",
                "استخدام نظام التخطيط الرياضي",
                score,
                "الأمر مرتبط بالجسم أو الرياضة."
        );
    }

    private CategoryScore scoreBusiness(String text) {

        int score = 0;

        if (containsAny(
                text,
                "فلوس",
                "مال",
                "ربح",
                "بيع",
                "شراء",
                "مشروع",
                "تجارة",
                "business",
                "money",
                "profit",
                "sell",
                "buy"
        )) {
            score += 4;
        }

        return new CategoryScore(
                "BUSINESS",
                "استخدام نظام الأعمال",
                score,
                "الأمر مرتبط بالمال أو التجارة أو المشروع."
        );
    }

    private CategoryScore scoreMemory(String text) {

        int score = 0;

        if (containsAny(
                text,
                "تذكر",
                "ذاكرة",
                "نسى",
                "نسيت",
                "معلومة عليا",
                "memory",
                "remember",
                "forget"
        )) {
            score += 6;
        }

        return new CategoryScore(
                "MEMORY",
                "استخدام Memory Manager",
                score,
                "الأمر مرتبط بالذاكرة أو المعلومات المحفوظة."
        );
    }

    // =========================================================
    // SCORE HELPERS
    // =========================================================

    private CategoryScore findBestScore(
            List<CategoryScore> scores
    ) {

        CategoryScore best = null;

        for (CategoryScore score : scores) {

            if (best == null
                    || score.score > best.score) {

                best = score;
            }
        }

        if (best == null) {

            return new CategoryScore(
                    "GENERAL",
                    "تنفيذ عام",
                    0,
                    "لم يتم العثور على نية محددة."
            );
        }

        return best;
    }

    private CategoryScore findSecondBest(
            List<CategoryScore> scores,
            CategoryScore best
    ) {

        CategoryScore second = null;

        for (CategoryScore score : scores) {

            if (score == best) {
                continue;
            }

            if (second == null
                    || score.score > second.score) {

                second = score;
            }
        }

        if (second == null) {

            return new CategoryScore(
                    "GENERAL",
                    "عام",
                    0,
                    ""
            );
        }

        return second;
    }

    private int calculateContextBoost(
            String text,
            CategoryScore best
    ) {

        int boost = 0;

        try {

            String lastType =
                    memoryManager.getMemory(
                            LAST_TYPE_KEY
                    );

            if (lastType != null
                    && lastType.equalsIgnoreCase(
                    best.type
            )) {
                boost += 3;
            }

        } catch (Exception ignored) {
        }

        if (containsAny(
                text,
                "هذا",
                "هاد",
                "نفس",
                "نكمل",
                "كمل",
                "تابع"
        )) {
            boost += 2;
        }

        try {

            String activePlan =
                    planningEngine.getActivePlan();

            if (activePlan != null
                    && !activePlan.trim().isEmpty()
                    && "PLANNING".equals(best.type)) {

                boost += 2;
            }

        } catch (Exception ignored) {
        }

        return boost;
    }

    private int calculateLearnedBoost(
            String command,
            CategoryScore best
    ) {

        try {

            String learned =
                    commandLearningEngine
                            .findLearnedCommand(
                                    command
                            );

            if (learned != null
                    && !learned.trim().isEmpty()) {

                return 5;
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private int calculateConfidence(
            int score,
            CategoryScore second,
            int contextBoost,
            int learnedBoost
    ) {

        int base =
                MIN_CONFIDENCE + (score * 4);

        int difference =
                score - second.score;

        base += difference * 2;
        base += contextBoost;
        base += learnedBoost;

        if (base < MIN_CONFIDENCE) {
            base = MIN_CONFIDENCE;
        }

        if (base > MAX_CONFIDENCE) {
            base = MAX_CONFIDENCE;
        }

        return base;
    }

    private String determinePriority(
            String text,
            int score,
            String type
    ) {

        if (containsAny(
                text,
                "عاجل",
                "ضروري",
                "دابا",
                "الآن",
                "حالاً",
                "فورا",
                "مهم جدا",
                "urgent",
                "now"
        )) {
            return "HIGH";
        }

        if ("REMINDER".equals(type)
                || "TASK".equals(type)
                || "AUTOMATION".equals(type)) {

            if (score >= 8) {
                return "HIGH";
            }

            return "NORMAL";
        }

        if ("EVOLUTION".equals(type)
                || "DEVELOPMENT".equals(type)) {

            if (score >= 10) {
                return "HIGH";
            }
        }

        if (score <= 1) {
            return "LOW";
        }

        return "NORMAL";
    }

    private boolean isUrgent(String text) {

        return containsAny(
                text,
                "عاجل",
                "ضروري",
                "دابا",
                "الآن",
                "حالاً",
                "فورا",
                "urgent",
                "immediately"
        );
    }

    // =========================================================
    // SIGNALS / REASON
    // =========================================================

    private String buildReason(
            CategoryScore best,
            CategoryScore second,
            int contextBoost,
            int learnedBoost
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(best.reason);

        if (second != null
                && second.score > 0) {

            builder.append(" المنافس الثاني: ")
                    .append(second.type)
                    .append(".");
        }

        if (contextBoost > 0) {

            builder.append(
                    " تم تعزيز القرار بالسياق السابق."
            );
        }

        if (learnedBoost > 0) {

            builder.append(
                    " تم العثور على معرفة متعلمة."
            );
        }

        return builder.toString();
    }

    private List<String> collectSignals(
            String text,
            CategoryScore best,
            int contextBoost,
            int learnedBoost
    ) {

        List<String> signals =
                new ArrayList<>();

        signals.add(
                "TYPE=" + best.type
        );

        signals.add(
                "SCORE=" + best.score
        );

        if (contextBoost > 0) {
            signals.add(
                    "CONTEXT_BOOST=" + contextBoost
            );
        }

        if (learnedBoost > 0) {
            signals.add(
                    "LEARNED_BOOST=" + learnedBoost
            );
        }

        if (isUrgent(text)) {
            signals.add("URGENT");
        }

        return signals;
    }

    // =========================================================
    // SAVE DECISION
    // =========================================================

    private void saveDecision(
            String command,
            DecisionResult result
    ) {

        try {

            memoryManager.saveMemory(
                    LAST_DECISION_KEY,
                    buildCompactDecision(
                            command,
                            result
                    )
            );

            memoryManager.saveMemory(
                    LAST_TYPE_KEY,
                    result.type
            );

            memoryManager.saveMemory(
                    LAST_CONFIDENCE_KEY,
                    String.valueOf(
                            result.confidence
                    )
            );

            memoryManager.saveMemory(
                    LAST_PRIORITY_KEY,
                    result.priority
            );

            memoryManager.saveMemory(
                    LAST_COMMAND_KEY,
                    command
            );

            int count = 0;

            try {

                String saved =
                        memoryManager.getMemory(
                                DECISION_COUNT_KEY
                        );

                if (saved != null) {
                    count =
                            Integer.parseInt(
                                    saved
                            );
                }

            } catch (Exception ignored) {
            }

            count++;

            memoryManager.saveMemory(
                    DECISION_COUNT_KEY,
                    String.valueOf(count)
            );

        } catch (Exception ignored) {
        }
    }

    private String buildCompactDecision(
            String command,
            DecisionResult result
    ) {

        return "command="
                + command
                + "\ntype="
                + result.type
                + "\npriority="
                + result.priority
                + "\nconfidence="
                + result.confidence
                + "\naction="
                + result.action;
    }

    // =========================================================
    // REPORT
    // =========================================================

    private String buildDecisionReport(
            String command,
            DecisionResult result
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("🧠 قرار JARVIS\n\n");

        builder.append("الأمر: ")
                .append(command)
                .append("\n");

        builder.append("النوع: ")
                .append(result.type)
                .append("\n");

        builder.append("الأولوية: ")
                .append(result.priority)
                .append("\n");

        builder.append("الثقة: ")
                .append(result.confidence)
                .append("%\n");

        builder.append("الإجراء: ")
                .append(result.action)
                .append("\n");

        builder.append("السبب: ")
                .append(result.reason)
                .append("\n");

        if (result.learnedAction != null
                && !result.learnedAction.trim().isEmpty()) {

            builder.append("تعلم سابق: ")
                    .append(result.learnedAction)
                    .append("\n");
        }

        if (result.signals != null
                && !result.signals.isEmpty()) {

            builder.append("الإشارات: ");

            for (int i = 0;
                 i < result.signals.size();
                 i++) {

                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(
                        result.signals.get(i)
                );
            }
        }

        return builder.toString();
    }

    // =========================================================
    // PUBLIC STATUS
    // =========================================================

    public synchronized String getStatus() {

        StringBuilder builder =
                new StringBuilder();

        builder.append("Decision Engine: ONLINE\n");

        try {

            String count =
                    memoryManager.getMemory(
                            DECISION_COUNT_KEY
                    );

            builder.append("Decisions: ")
                    .append(
                            count == null
                                    ? "0"
                                    : count
                    )
                    .append("\n");

        } catch (Exception e) {

            builder.append(
                    "Decisions: unavailable\n"
            );
        }

        try {

            String lastType =
                    memoryManager.getMemory(
                            LAST_TYPE_KEY
                    );

            builder.append("Last type: ")
                    .append(
                            lastType == null
                                    ? "NONE"
                                    : lastType
                    )
                    .append("\n");

        } catch (Exception e) {

            builder.append(
                    "Last type: unavailable\n"
            );
        }

        try {

            String confidence =
                    memoryManager.getMemory(
                            LAST_CONFIDENCE_KEY
                    );

            builder.append("Last confidence: ")
                    .append(
                            confidence == null
                                    ? "0"
                                    : confidence
                    )
                    .append("%");

        } catch (Exception e) {

            builder.append(
                    "Last confidence: unavailable"
            );
        }

        return builder.toString();
    }

    public synchronized String getLastDecision() {

        try {

            String value =
                    memoryManager.getMemory(
                            LAST_DECISION_KEY
                    );

            if (value == null
                    || value.trim().isEmpty()) {

                return "لا يوجد قرار محفوظ.";
            }

            return value;

        } catch (Exception e) {

            return "تعذر قراءة آخر قرار.";
        }
    }

    public synchronized int getDecisionCount() {

        try {

            String value =
                    memoryManager.getMemory(
                            DECISION_COUNT_KEY
                    );

            if (value == null) {
                return 0;
            }

            return Integer.parseInt(value);

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        String result =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        return result;
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null || values == null) {
            return false;
        }

        for (String value : values) {

            if (value == null
                    || value.trim().isEmpty()) {
                continue;
            }

            if (text.contains(
                    value.toLowerCase(
                            Locale.ROOT
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // DATA CLASSES
    // =========================================================

    private static class CategoryScore {

        final String type;
        final String action;
        final int score;
        final String reason;

        CategoryScore(
                String type,
                String action,
                int score,
                String reason
        ) {

            this.type = type;
            this.action = action;
            this.score = score;
            this.reason = reason;
        }
    }

    private static class DecisionResult {

        final String type;
        final String priority;
        final String action;
        final int confidence;
        final String reason;
        final List<String> signals;
        final String learnedAction;

        DecisionResult(
                String type,
                String priority,
                String action,
                int confidence,
                String reason,
                List<String> signals,
                String learnedAction
        ) {

            this.type = type;
            this.priority = priority;
            this.action = action;
            this.confidence = confidence;
            this.reason = reason;
            this.signals = signals;
            this.learnedAction = learnedAction;
        }
    }
}