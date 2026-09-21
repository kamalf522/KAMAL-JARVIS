package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DecisionEngine {

    private static final String LAST_DECISION_KEY =
            "__last_decision__";

    private static final String DECISION_COUNT_KEY =
            "__decision_count__";

    private final Context context;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;
    private final PlanningEngine planningEngine;

    public DecisionEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);

        planningEngine =
                new PlanningEngine(this.context);
    }

    public String decide(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما وصلني حتى أمر.";
        }

        String input =
                command.trim();

        String lower =
                input.toLowerCase(Locale.ROOT);

        DecisionResult result =
                analyzeCommand(input, lower);

        saveDecision(result);

        return buildDecisionReport(result);
    }

    private DecisionResult analyzeCommand(
            String command,
            String lower
    ) {

        if (isUrgent(lower)) {

            return new DecisionResult(
                    "URGENT",
                    "تنفيذ الأمر بأولوية عالية",
                    100,
                    "الأمر يحتوي على مؤشر استعجال."
            );
        }

        List<CategoryScore> scores =
                new ArrayList<>();

        scores.add(scoreReminder(lower));
        scores.add(scoreTask(lower));
        scores.add(scoreLearning(lower));
        scores.add(scorePlanning(lower));
        scores.add(scoreDevelopment(lower));
        scores.add(scoreEvolution(lower));
        scores.add(scoreScreen(lower));
        scores.add(scoreAndroid(lower));
        scores.add(scoreFitness(lower));
        scores.add(scoreBusiness(lower));

        CategoryScore best =
                scores.get(0);

        for (CategoryScore score : scores) {

            if (score.score > best.score) {
                best = score;
            }
        }

        if (best.score <= 0) {

            return new DecisionResult(
                    "GENERAL",
                    "تحليل الأمر ثم اختيار النظام المناسب",
                    25,
                    "لم يتم العثور على إشارة قوية لنظام محدد."
            );
        }

        int confidence =
                Math.min(
                        99,
                        35 + (best.score * 8)
                );

        return new DecisionResult(
                best.type,
                best.action,
                confidence,
                best.reason
        );
    }

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
            score += 4;
        }

        if (containsAny(
                text,
                "غدا",
                "غدا",
                "اليوم",
                "بعد",
                "ساعة",
                "دقيقة",
                "وقت"
        )) {
            score += 1;
        }

        return new CategoryScore(
                "REMINDER",
                "استخدام Reminder Engine",
                score,
                "الأمر يبدو مرتبطاً بالتذكيرات أو الوقت."
        );
    }

    private CategoryScore scoreTask(
            String text
    ) {

        int score = 0;

        if (containsAny(
                text,
                "مهمة",
                "المهمة",
                "task",
                "todo",
                "خدمة",
                "دير ليا",
                "قم ب"
        )) {
            score += 3;
        }

        return new CategoryScore(
                "TASK",
                "استخدام Task Manager",
                score,
                "الأمر يبدو مرتبطاً بمهمة قابلة للتنفيذ."
        );
    }

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
            score += 4;
        }

        return new CategoryScore(
                "LEARNING",
                "استخدام Learning Engine",
                score,
                "الأمر مرتبط بالتعلم أو اكتساب مهارة."
        );
    }

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
            score += 4;
        }

        return new CategoryScore(
                "PLANNING",
                "استخدام Planning Engine",
                score,
                "الأمر يطلب بناء خطة أو تنظيم خطوات."
        );
    }

    private CategoryScore scoreDevelopment(
            String text
    ) {

        int score = 0;

        if (containsAny(
                text,
                "كود",
                "برمج",
                "برمجة",
                "تطبيق",
                "apk",
                "ملف",
                "java",
                "android",
                "software",
                "code",
                "develop"
        )) {
            score += 4;
        }

        if (containsAny(
                text,
                "صلح",
                "عدل",
                "بدل",
                "أنشئ",
                "اضف",
                "طور التطبيق"
        )) {
            score += 2;
        }

        return new CategoryScore(
                "DEVELOPMENT",
                "استخدام أنظمة تطوير JARVIS",
                score,
                "الأمر مرتبط بالكود أو تطوير التطبيق."
        );
    }

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
                "تطور",
                "تطوير ذاتي",
                "evolve",
                "evolution",
                "self improve",
                "self improvement"
        )) {
            score += 6;
        }

        if (containsAny(
                text,
                "تعلم من الأخطاء",
                "تعلم من الاخطاء",
                "حسن نفسك",
                "حسن راسك",
                "طور النظام"
        )) {
            score += 2;
        }

        return new CategoryScore(
                "EVOLUTION",
                "استخدام Evolution Engine",
                score,
                "الأمر يطلب تحسين أو تطوير JARVIS نفسه."
        );
    }

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
            score += 4;
        }

        return new CategoryScore(
                "SCREEN",
                "استخدام Screen Intelligence",
                score,
                "الأمر مرتبط بفهم محتوى الشاشة."
        );
    }

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
                "إعدادات",
                "اعدادات",
                "الهاتف",
                "phone",
                "setting",
                "settings"
        )) {
            score += 4;
        }

        return new CategoryScore(
                "ANDROID",
                "استخدام Android Control Engine",
                score,
                "الأمر مرتبط بإعدادات أو تحكم الهاتف."
        );
    }

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
                "استخدام نظام التخطيط والتتبع الرياضي",
                score,
                "الأمر مرتبط بالجسم أو الرياضة."
        );
    }

    private CategoryScore scoreBusiness(
            String text
    ) {

        int score = 0;

        if (containsAny(
                text,
                "فلوس",
                "مال",
                "ربح",
                "دخل",
                "مشروع",
                "زبون",
                "عميل",
                "خدمة مدفوعة",
                "business",
                "money",
                "income",
                "client"
        )) {
            score += 4;
        }

        return new CategoryScore(
                "BUSINESS",
                "استخدام Business Planning",
                score,
                "الأمر مرتبط بالدخل أو مشروع أو عميل."
        );
    }

    private boolean isUrgent(
            String input
    ) {

        return containsAny(
                input,
                "عاجل",
                "ضروري",
                "دابا حالا",
                "دابا",
                "urgent",
                "asap"
        );
    }

    private boolean containsAny(
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

            if (text.contains(
                    value.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }

        return false;
    }

    private void saveDecision(
            DecisionResult result
    ) {

        String data =
                result.type
                        + " | "
                        + result.action
                        + " | confidence="
                        + result.confidence
                        + " | "
                        + result.reason;

        memoryManager.saveMemory(
                LAST_DECISION_KEY,
                data
        );

        int count = 0;

        try {

            String stored =
                    memoryManager.getMemory(
                            DECISION_COUNT_KEY
                    );

            if (stored != null &&
                    !stored.trim().isEmpty()) {

                count =
                        Integer.parseInt(
                                stored.trim()
                        );
            }

        } catch (Exception ignored) {
        }

        memoryManager.saveMemory(
                DECISION_COUNT_KEY,
                String.valueOf(count + 1)
        );
    }

    private String buildDecisionReport(
            DecisionResult result
    ) {

        return
                "JARVIS DECISION ENGINE\n"
                + "============================\n\n"
                + "التصنيف:\n"
                + result.type
                + "\n\n"
                + "القرار:\n"
                + result.action
                + "\n\n"
                + "الثقة:\n"
                + result.confidence
                + "%\n\n"
                + "السبب:\n"
                + result.reason;
    }

    public String getLastDecision() {

        String decision =
                memoryManager.getMemory(
                        LAST_DECISION_KEY
                );

        if (decision == null ||
                decision.trim().isEmpty()) {

            return "ما كاين حتى قرار مسجل.";
        }

        return
                "آخر قرار:\n"
                + decision;
    }

    public String getSystemStatus() {

        try {

            int tasks =
                    taskManager.getTaskCount();

            int memories =
                    memoryManager.getMemoryCount();

            String planStatus =
                    planningEngine.getStatus();

            return
                    "Decision Engine: ONLINE ✓\n"
                    + "Known Tasks: "
                    + tasks
                    + "\n"
                    + "Memories: "
                    + memories
                    + "\n"
                    + "Planning: ONLINE ✓\n"
                    + planStatus;

        } catch (Exception e) {

            return
                    "Decision Engine: ERROR ⚠";
        }
    }

    public String getStatus() {

        return getSystemStatus();
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            taskManager.getTaskCount();

            planningEngine.isHealthy();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getDecisionCount() {

        String count =
                memoryManager.getMemory(
                        DECISION_COUNT_KEY
                );

        if (count == null ||
                count.trim().isEmpty()) {

            return "0";
        }

        return count;
    }

    private static class CategoryScore {

        private final String type;
        private final String action;
        private final int score;
        private final String reason;

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

        private final String type;
        private final String action;
        private final int confidence;
        private final String reason;

        DecisionResult(
                String type,
                String action,
                int confidence,
                String reason
        ) {

            this.type = type;
            this.action = action;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}