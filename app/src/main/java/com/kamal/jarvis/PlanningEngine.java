package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlanningEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;

    private static final String ACTIVE_PLAN_KEY =
            "__active_plan__";

    private static final String PLAN_STEPS_KEY =
            "__plan_steps__";

    private static final String PLAN_TYPE_KEY =
            "__plan_type__";

    private static final String PLAN_PROGRESS_KEY =
            "__plan_progress__";

    private static final String PLAN_CREATED_KEY =
            "__plan_created__";

    private static final String PLAN_UPDATED_KEY =
            "__plan_updated__";

    private static final String PLAN_HISTORY_KEY =
            "__plan_history__";

    private static final String PLAN_VERSION_KEY =
            "__plan_version__";

    public PlanningEngine(Context context) {

        this.context =
                context == null
                        ? null
                        : context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);
    }

    // =========================================================
    // CREATE PLAN
    // =========================================================

    public synchronized String createPlan(String goal) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "خاصك تحدد الهدف اللي بغيتي JARVIS يخطط ليه.";
        }

        String cleanGoal =
                goal.trim();

        String type =
                detectGoalType(cleanGoal);

        List<String> steps =
                generateSteps(
                        cleanGoal,
                        type
                );

        if (steps.isEmpty()) {

            return "ما قدرتش نبني خطة صالحة لهاد الهدف.";
        }

        String oldPlan =
                memoryManager.getMemory(
                        ACTIVE_PLAN_KEY
                );

        if (oldPlan != null &&
                !oldPlan.trim().isEmpty()) {

            savePlanHistory(oldPlan);
        }

        String now =
                String.valueOf(
                        System.currentTimeMillis()
                );

        memoryManager.saveMemory(
                ACTIVE_PLAN_KEY,
                cleanGoal
        );

        memoryManager.saveMemory(
                PLAN_STEPS_KEY,
                serializeSteps(steps)
        );

        memoryManager.saveMemory(
                PLAN_TYPE_KEY,
                type
        );

        memoryManager.saveMemory(
                PLAN_PROGRESS_KEY,
                "0"
        );

        memoryManager.saveMemory(
                PLAN_CREATED_KEY,
                now
        );

        memoryManager.saveMemory(
                PLAN_UPDATED_KEY,
                now
        );

        memoryManager.saveMemory(
                PLAN_VERSION_KEY,
                "1"
        );

        return buildPlanReport(
                cleanGoal,
                type,
                steps,
                0
        );
    }

    // =========================================================
    // GOAL TYPE
    // =========================================================

    private String detectGoalType(String goal) {

        String lower =
                normalize(goal);

        if (containsAny(
                lower,
                "تعلم",
                "نتعلم",
                "قرا",
                "قراءة",
                "دراسة",
                "دراسه",
                "learn",
                "study",
                "skill",
                "مهارة",
                "مهاره",
                "كورس",
                "course"
        )) {
            return "LEARNING";
        }

        if (containsAny(
                lower,
                "فلوس",
                "مال",
                "دخل",
                "ربح",
                "مشروع",
                "خدمة",
                "خدمه",
                "business",
                "money",
                "income",
                "profit",
                "client",
                "clients"
        )) {
            return "BUSINESS";
        }

        if (containsAny(
                lower,
                "جسم",
                "عضلات",
                "رياضة",
                "رياضه",
                "وزن",
                "لياقة",
                "لياقه",
                "fitness",
                "muscle",
                "body",
                "workout",
                "gym"
        )) {
            return "FITNESS";
        }

        if (containsAny(
                lower,
                "تطبيق",
                "برنامج",
                "كود",
                "جارفيس",
                "جارڤيس",
                "apk",
                "app",
                "code",
                "software",
                "برمجة",
                "برمجه",
                "تطوير",
                "development"
        )) {
            return "DEVELOPMENT";
        }

        if (containsAny(
                lower,
                "نظم",
                "تنظيم",
                "وقت",
                "روتين",
                "عادة",
                "عاده",
                "انضباط",
                "routine",
                "organize",
                "schedule",
                "discipline"
        )) {
            return "PERSONAL";
        }

        return "GENERAL";
    }

    // =========================================================
    // GENERATE STEPS
    // =========================================================

    private List<String> generateSteps(
            String goal,
            String type
    ) {

        List<String> steps =
                new ArrayList<>();

        steps.add(
                "حلل الهدف وحدد النتيجة النهائية القابلة للقياس."
        );

        if ("LEARNING".equals(type)) {

            steps.add(
                    "حدد المهارة أو المعرفة الأساسية المطلوبة."
            );

            steps.add(
                    "قسم التعلم إلى وحدات صغيرة وواضحة."
            );

            steps.add(
                    "حدد أول مصدر أو درس مناسب للبداية."
            );

            steps.add(
                    "طبق ما تعلمته في تمرين عملي."
            );

            steps.add(
                    "اختبر مستواك واكتشف نقاط الضعف."
            );

            steps.add(
                    "راجع الأخطاء وحسن طريقة التعلم."
            );

            steps.add(
                    "أنشئ اختباراً عملياً للتأكد من التقدم."
            );

            steps.add(
                    "كرر دورة التعلم حتى يتحقق المستوى المطلوب."
            );

            return steps;
        }

        if ("BUSINESS".equals(type)) {

            steps.add(
                    "حدد المشكلة أو الحاجة التي يمكن تقديم حل لها."
            );

            steps.add(
                    "حدد العميل أو الشخص الذي يحتاج هذا الحل."
            );

            steps.add(
                    "حدد المهارة أو الخدمة التي يمكن تقديمها."
            );

            steps.add(
                    "أنشئ نموذجاً بسيطاً للخدمة أو العرض."
            );

            steps.add(
                    "اختبر العرض مع أشخاص حقيقيين."
            );

            steps.add(
                    "سجل النتائج والاعتراضات والفرص."
            );

            steps.add(
                    "حسن العرض وطريقة الوصول للعملاء."
            );

            steps.add(
                    "كرر الاختبار بناءً على النتائج."
            );

            return steps;
        }

        if ("FITNESS".equals(type)) {

            steps.add(
                    "حدد الهدف الجسدي والمدة المطلوبة."
            );

            steps.add(
                    "حدد نقطة البداية والقدرات الحالية."
            );

            steps.add(
                    "حدد التمارين أو النشاط المناسب."
            );

            steps.add(
                    "حدد نظاماً عملياً يمكن الالتزام به."
            );

            steps.add(
                    "نفذ أول حصة وسجل النتيجة."
            );

            steps.add(
                    "راقب التقدم بشكل منتظم."
            );

            steps.add(
                    "عدل الخطة حسب النتائج والقدرة على الالتزام."
            );

            steps.add(
                    "كرر الدورة مع رفع المستوى تدريجياً."
            );

            return steps;
        }

        if ("DEVELOPMENT".equals(type)) {

            steps.add(
                    "حدد الوظيفة المطلوبة والنتيجة التي يجب أن يعمل بها النظام."
            );

            steps.add(
                    "حلل البنية الحالية والملفات المرتبطة بالميزة."
            );

            steps.add(
                    "حدد الاعتماديات والتأثيرات المحتملة قبل التعديل."
            );

            steps.add(
                    "حدد أقل تغيير آمن يحقق الوظيفة."
            );

            steps.add(
                    "أنشئ Backup أو Snapshot قبل التعديل."
            );

            steps.add(
                    "طبق التغيير على Workspace."
            );

            steps.add(
                    "اختبر التغيير وتحقق من عدم كسر الأنظمة الموجودة."
            );

            steps.add(
                    "إذا نجح الاختبار اعتمد التغيير."
            );

            steps.add(
                    "إذا فشل الاختبار نفذ Rollback وحلل سبب الفشل."
            );

            return steps;
        }

        if ("PERSONAL".equals(type)) {

            steps.add(
                    "حدد الوضع الحالي والمشكلة التي تريد تغييرها."
            );

            steps.add(
                    "اختر عادة أو سلوكاً واحداً كبداية."
            );

            steps.add(
                    "حدد وقتاً واضحاً لتنفيذ السلوك."
            );

            steps.add(
                    "ابدأ بأصغر إجراء يمكن الالتزام به."
            );

            steps.add(
                    "سجل الالتزام والنتائج."
            );

            steps.add(
                    "اكتشف سبب التعثر وعدل الخطة."
            );

            steps.add(
                    "راجع النتائج بشكل دوري."
            );

            steps.add(
                    "كرر النظام حتى يصبح السلوك ثابتاً."
            );

            return steps;
        }

        steps.add(
                "حدد الموارد والمعلومات المطلوبة لتحقيق الهدف."
        );

        steps.add(
                "قسم الهدف إلى مراحل صغيرة قابلة للتنفيذ."
        );

        steps.add(
                "حدد الأولويات والقيود التي يمكن أن تؤثر على التنفيذ."
        );

        steps.add(
                "اختر أول خطوة يمكن تنفيذها الآن."
        );

        steps.add(
                "نفذ الخطوة وسجل النتيجة."
        );

        steps.add(
                "حلل النتيجة واكتشف الأخطاء."
        );

        steps.add(
                "عدل الخطة بناءً على النتائج."
        );

        steps.add(
                "كرر دورة التنفيذ والتحسين حتى يتحقق الهدف."
        );

        return steps;
    }

    // =========================================================
    // ADD STEP AS TASK
    // =========================================================

    public String addPlanStepAsTask(String step) {

        if (step == null ||
                step.trim().isEmpty()) {

            return "المهمة فارغة.";
        }

        return taskManager.addTask(
                step.trim()
        );
    }

    // =========================================================
    // ADD CURRENT STEP
    // =========================================================

    public synchronized String addCurrentStepAsTask() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {
            return "ما كايناش خطة نشطة.";
        }

        int progress =
                getProgress();

        if (progress >= steps.size()) {
            return "الخطة كاملة، ما بقا حتى Step.";
        }

        return taskManager.addTask(
                steps.get(progress)
        );
    }

    // =========================================================
    // CURRENT STEP
    // =========================================================

    public String getCurrentStep() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {
            return "ما كايناش خطة نشطة.";
        }

        int progress =
                getProgress();

        if (progress >= steps.size()) {
            return "الخطة مكتملة ✓";
        }

        return steps.get(progress);
    }

    // =========================================================
    // COMPLETE STEP
    // =========================================================

    public synchronized String completeCurrentStep() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {
            return "ما كايناش خطة نشطة.";
        }

        int progress =
                getProgress();

        if (progress >= steps.size()) {
            return "الخطة راه كاملة ✓";
        }

        progress++;

        saveProgress(progress);

        if (progress >= steps.size()) {

            return
                    "تم إكمال آخر خطوة ✓\n\n"
                    + "الهدف تحقق حسب الخطة الحالية.";
        }

        return
                "تم إكمال الخطوة ✓\n\n"
                + "التقدم: "
                + progress
                + "/"
                + steps.size()
                + "\n\n"
                + "الخطوة التالية:\n"
                + steps.get(progress);
    }

    // =========================================================
    // SET PROGRESS
    // =========================================================

    public synchronized String setProgress(
            int progress
    ) {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {
            return "ما كايناش خطة نشطة.";
        }

        if (progress < 0) {
            progress = 0;
        }

        if (progress > steps.size()) {
            progress = steps.size();
        }

        saveProgress(progress);

        return getProgressReport();
    }

    // =========================================================
    // ACTIVE PLAN
    // =========================================================

    public String getActivePlan() {

        String goal =
                memoryManager.getMemory(
                        ACTIVE_PLAN_KEY
                );

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "ما كاين حتى Plan نشطة حاليا.";
        }

        String type =
                memoryManager.getMemory(
                        PLAN_TYPE_KEY
                );

        List<String> steps =
                getStoredSteps();

        int progress =
                getProgress();

        return buildPlanReport(
                goal,
                type,
                steps,
                progress
        );
    }

    public String getActiveGoal() {

        String goal =
                memoryManager.getMemory(
                        ACTIVE_PLAN_KEY
                );

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "";
        }

        return goal;
    }

    // =========================================================
    // PROGRESS REPORT
    // =========================================================

    public String getProgressReport() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {
            return "ما كايناش خطة باش نحسب التقدم.";
        }

        int progress =
                getProgress();

        int total =
                steps.size();

        int percentage =
                total == 0
                        ? 0
                        : (progress * 100) / total;

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS PLAN PROGRESS\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "التقدم: "
        );

        result.append(
                percentage
        );

        result.append(
                "% ("
        );

        result.append(
                progress
        );

        result.append(
                "/"
        );

        result.append(
                total
        );

        result.append(
                ")\n\n"
        );

        for (int i = 0;
             i < steps.size();
             i++) {

            boolean completed =
                    i < progress;

            result.append(
                    completed
                            ? "✓ "
                            : "○ "
            );

            result.append(
                    i + 1
            );

            result.append(
                    ". "
            );

            result.append(
                    steps.get(i)
            );

            result.append(
                    "\n"
            );
        }

        return result.toString().trim();
    }

    // =========================================================
    // CLEAR PLAN
    // =========================================================

    public synchronized String clearActivePlan() {

        String oldPlan =
                memoryManager.getMemory(
                        ACTIVE_PLAN_KEY
                );

        if (oldPlan != null &&
                !oldPlan.trim().isEmpty()) {

            savePlanHistory(oldPlan);
        }

        memoryManager.removeMemory(
                ACTIVE_PLAN_KEY
        );

        memoryManager.removeMemory(
                PLAN_STEPS_KEY
        );

        memoryManager.removeMemory(
                PLAN_TYPE_KEY
        );

        memoryManager.removeMemory(
                PLAN_PROGRESS_KEY
        );

        memoryManager.removeMemory(
                PLAN_CREATED_KEY
        );

        memoryManager.removeMemory(
                PLAN_UPDATED_KEY
        );

        memoryManager.removeMemory(
                PLAN_VERSION_KEY
        );

        return "تم حذف الخطة النشطة ✓";
    }

    // =========================================================
    // PLAN REPORT
    // =========================================================

    private String buildPlanReport(
            String goal,
            String type,
            List<String> steps,
            int progress
    ) {

        StringBuilder plan =
                new StringBuilder();

        plan.append(
                "JARVIS PLANNING ENGINE\n"
        );

        plan.append(
                "============================\n\n"
        );

        plan.append(
                "الهدف:\n"
        );

        plan.append(
                goal
        );

        plan.append(
                "\n\n"
        );

        plan.append(
                "نوع الهدف: "
        );

        plan.append(
                type == null ||
                        type.isEmpty()
                        ? "GENERAL"
                        : type
        );

        plan.append(
                "\n\n"
        );

        plan.append(
                "الخطة:\n"
        );

        for (int i = 0;
             i < steps.size();
             i++) {

            boolean completed =
                    i < progress;

            plan.append(
                    completed
                            ? "✓ "
                            : "○ "
            );

            plan.append(
                    i + 1
            );

            plan.append(
                    ". "
            );

            plan.append(
                    steps.get(i)
            );

            if (i == progress &&
                    progress < steps.size()) {

                plan.append(
                        "  ← NEXT"
                );
            }

            plan.append(
                    "\n"
            );
        }

        int percentage =
                steps.isEmpty()
                        ? 0
                        : (progress * 100)
                                / steps.size();

        plan.append(
                "\nالتقدم: "
        );

        plan.append(
                percentage
        );

        plan.append(
                "%"
        );

        return plan.toString().trim();
    }

    // =========================================================
    // STORED STEPS
    // =========================================================

    private List<String> getStoredSteps() {

        String stored =
                memoryManager.getMemory(
                        PLAN_STEPS_KEY
                );

        return deserializeSteps(stored);
    }

    private String serializeSteps(
            List<String> steps
    ) {

        if (steps == null ||
                steps.isEmpty()) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (String step : steps) {

            if (step == null) {
                continue;
            }

            String clean =
                    step
                            .replace("\\", "\\\\")
                            .replace("|", "\\|")
                            .replace("\n", " ");

            if (result.length() > 0) {
                result.append("|");
            }

            result.append(clean);
        }

        return result.toString();
    }

    private List<String> deserializeSteps(
            String stored
    ) {

        List<String> steps =
                new ArrayList<>();

        if (stored == null ||
                stored.trim().isEmpty()) {

            return steps;
        }

        StringBuilder current =
                new StringBuilder();

        boolean escaped = false;

        for (int i = 0;
             i < stored.length();
             i++) {

            char c =
                    stored.charAt(i);

            if (escaped) {

                current.append(c);
                escaped = false;

            } else if (c == '\\') {

                escaped = true;

            } else if (c == '|') {

                String step =
                        current.toString().trim();

                if (!step.isEmpty()) {
                    steps.add(step);
                }

                current.setLength(0);

            } else {

                current.append(c);
            }
        }

        if (escaped) {
            current.append('\\');
        }

        String last =
                current.toString().trim();

        if (!last.isEmpty()) {
            steps.add(last);
        }

        return steps;
    }

    // =========================================================
    // PROGRESS STORAGE
    // =========================================================

    private int getProgress() {

        String value =
                memoryManager.getMemory(
                        PLAN_PROGRESS_KEY
                );

        if (value == null ||
                value.trim().isEmpty()) {

            return 0;
        }

        try {

            int progress =
                    Integer.parseInt(
                            value.trim()
                    );

            if (progress < 0) {
                return 0;
            }

            List<String> steps =
                    getStoredSteps();

            if (!steps.isEmpty() &&
                    progress > steps.size()) {

                return steps.size();
            }

            return progress;

        } catch (Exception e) {

            return 0;
        }
    }

    private void saveProgress(
            int progress
    ) {

        memoryManager.saveMemory(
                PLAN_PROGRESS_KEY,
                String.valueOf(progress)
        );

        memoryManager.saveMemory(
                PLAN_UPDATED_KEY,
                String.valueOf(
                        System.currentTimeMillis()
                )
        );
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void savePlanHistory(
            String oldPlan
    ) {

        if (oldPlan == null ||
                oldPlan.trim().isEmpty()) {

            return;
        }

        String history =
                memoryManager.getMemory(
                        PLAN_HISTORY_KEY
                );

        if (history == null) {
            history = "";
        }

        String entry =
                System.currentTimeMillis()
                + ":"
                + oldPlan.trim();

        String newHistory;

        if (history.trim().isEmpty()) {

            newHistory = entry;

        } else {

            newHistory =
                    history
                            + "\n"
                            + entry;
        }

        String[] lines =
                newHistory.split("\n");

        StringBuilder limited =
                new StringBuilder();

        int start =
                Math.max(
                        0,
                        lines.length - 20
                );

        for (int i = start;
             i < lines.length;
             i++) {

            if (lines[i] == null ||
                    lines[i].trim().isEmpty()) {

                continue;
            }

            if (limited.length() > 0) {
                limited.append("\n");
            }

            limited.append(
                    lines[i]
            );
        }

        memoryManager.saveMemory(
                PLAN_HISTORY_KEY,
                limited.toString()
        );
    }

    public String getPlanHistory() {

        String history =
                memoryManager.getMemory(
                        PLAN_HISTORY_KEY
                );

        if (history == null ||
                history.trim().isEmpty()) {

            return "ما كاين حتى Plan قديمة.";
        }

        return
                "JARVIS PLAN HISTORY\n"
                + "============================\n\n"
                + history;
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private boolean containsAny(
            String value,
            String... words
    ) {

        if (value == null ||
                words == null) {

            return false;
        }

        for (String word : words) {

            if (word == null ||
                    word.trim().isEmpty()) {

                continue;
            }

            if (value.contains(
                    normalize(word)
            )) {

                return true;
            }
        }

        return false;
    }

    private String normalize(
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
                .replace("ى", "ي")
                .replace("ؤ", "و")
                .replace("ئ", "ي")
                .replace("ـ", "")
                .replaceAll("\\s+", " ");
    }
}