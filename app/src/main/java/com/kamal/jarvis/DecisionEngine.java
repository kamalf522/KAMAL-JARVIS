package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS Decision Engine
 *
 * الدور:
 * - تحليل الأمر
 * - تحديد النية الأساسية
 * - حساب درجة الثقة
 * - تحديد الأولوية
 * - الاستفادة من الذاكرة والخطة والمهام والأوامر المتعلمة
 * - حفظ آخر قرار
 *
 * ملاحظة:
 * هذا النظام لا ينفذ الأمر.
 * هو طبقة القرار التي تعطي باقي الأنظمة معلومات أفضل
 * قبل مرحلة التنفيذ.
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

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(
                        this.context
                );

        taskManager =
                new TaskManager(
                        this.context
                );

        planningEngine =
                new PlanningEngine(
                        this.context
                );

        commandLearningEngine =
                new CommandLearningEngine(
                        this.context
                );
    }

    // =========================================================
    // MAIN DECISION
    // =========================================================

    public synchronized String decide(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما وصلني حتى أمر.";
        }

        String cleanCommand =
                normalize(command);

        try {

            DecisionResult result =
                    analyzeCommand(
                            cleanCommand
                    );

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
                            20,
                            "تعذر التحليل المتقدم؛ تم استعمال القرار العام.",
                            Collections.singletonList(
                                    "FALLBACK"
                            ),
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

    private DecisionResult analyzeCommand(
            String command
    ) {

        String lower =
                command.toLowerCase(
                        Locale.ROOT
                );

        List<CategoryScore> scores =
                new ArrayList<>();

        scores.add(
                scoreEvolution(lower)
        );

        scores.add(
                scoreDevelopment(lower)
        );

        scores.add(
                scoreReminder(lower)
        );

        scores.add(
                scoreTask(lower)
        );

        scores.add(
                scorePlanning(lower)
        );

        scores.add(
                scoreLearning(lower)
        );

        scores.add(
                scoreKnowledge(lower)
        );

        scores.add(
                scoreScreen(lower)
        );

        scores.add(
                scoreAndroid(lower)
        );

        scores.add(
                scoreAutomation(lower)
        );

        scores.add(
                scoreFitness(lower)
        );

        scores.add(
                scoreBusiness(lower)
        );

        scores.add(
                scoreMemory(lower)
        );

        CategoryScore best =
                findBestScore(
                        scores
                );

        CategoryScore second =
                findSecondBest(
                        scores,
                        best
                );

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
                isUrgent(lower)
                        ? 2
                        : 0;

        int finalScore =
                best.score
                        + contextBoost
                        + learnedBoost
                        + urgencyBoost;

        String type =
                best.type;

        String priority =
                determinePriority(
                        lower,
                        finalScore,
                        type
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

        String learnedAction =
                null;

        try {

            learnedAction =
                    commandLearningEngine
                            .findLearnedCommand(
                                    command
                            );

        } catch (Exception ignored) {
        }

        String action =
                best.action;

        if (learnedAction != null &&
                !learnedAction.trim().isEmpty()) {

            action =
                    "استعمال الإجراء المتعلم: "
                            + learnedAction;
        }

        return new DecisionResult(
                type,
                priority,
                action,
                confidence,
                reason,
                signals,
                learnedAction
        );
    }

    // =========================================================
    // EVOLUTION
    // =========================================================

    private CategoryScore scoreEvolution(
            String text
    ) {

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

    // =========================================================
    // DEVELOPMENT
    // =========================================================

    private CategoryScore scoreDevelopment(
            String text
    ) {

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

    // =========================================================
    // REMINDER
    // =========================================================

    private CategoryScore scoreReminder(
            String text
    ) {

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
                "غدا",
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

    // =========================================================
    // TASK
    // =========================================================

    private CategoryScore scoreTask(
            String text
    ) {

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

    // =========================================================
    // PLANNING
    // =========================================================

    private CategoryScore scorePlanning(
            String text
    ) {

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

    // =========================================================
    // LEARNING
    // =========================================================

    private CategoryScore scoreLearning(
            String text
    ) {

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

    // =========================================================
    // KNOWLEDGE
    // =========================================================

    private CategoryScore scoreKnowledge(
            String text
    ) {

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

    // =========================================================
    // SCREEN
    // =========================================================

    private CategoryScore scoreScreen(
            String text
    ) {

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

    // =========================================================
    // ANDROID
    // =========================================================

    private CategoryScore scoreAndroid(
            String text
    ) {

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

    // =========================================================
    // AUTOMATION
    // =========================================================

    private CategoryScore scoreAutomation(
            String text
    ) {

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

    // =========================================================
    // FITNESS
    // =========================================================

    private CategoryScore scoreFitness(
            String text
    ) {

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
                "الأمر مرتبط بالجسم أو