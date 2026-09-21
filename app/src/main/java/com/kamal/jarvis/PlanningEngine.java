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

    public PlanningEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);
    }

    // =========================================================
    // CREATE PLAN
    // =========================================================

    public String createPlan(String goal) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return
                    "خاصك تحدد الهدف اللي بغيتي JARVIS يخطط ليه.";
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

            return
                    "ما قدرتش نبني خطة صالحة لهاد الهدف.";
        }

        String serialized =
                serializeSteps(steps);

        memoryManager.saveMemory(
                ACTIVE_PLAN_KEY,
                cleanGoal
        );

        memoryManager.saveMemory(
                PLAN_STEPS_KEY,
                serialized
        );

        memoryManager.saveMemory(
                PLAN_TYPE_KEY,
                type
        );

        memoryManager.saveMemory(
                PLAN_PROGRESS_KEY,
                "0"
        );

        return buildPlanReport(
                cleanGoal,
                type,
                steps,
                0
        );
    }

    // =========================================================
    // SMART GOAL DETECTION
    // =========================================================

    private String detectGoalType(String goal) {

        String lower =
                goal.toLowerCase(
                        Locale.ROOT
                );

        if (containsAny(
                lower,
                "تعلم",
                "نتعلم",
                "قرا",
                "دراسة",
                "دراسه",
                "learn",
                "study",
                "skill",
                "مهارة"
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
                "business",
                "money",
                "income",
                "business",
                "client"
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
                "fitness",
                "muscle",
                "body",
                "workout"
        )) {

            return "FITNESS";
        }

        if (containsAny(
                lower,
                "تطبيق",
                "برنامج",
                "كود",
                "جارفيس",
                "apk",
                "app",
                "code",
                "software"
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
                "routine",
                "organize",
                "schedule"
        )) {

            return "PERSONAL";
        }

        return "GENERAL";
    }

    // =========================================================
    // STEP GENERATION
    // =========================================================

    private List<String> generateSteps(
            String goal,
            String type
    ) {

        List<String> steps =
                new ArrayList<>();

        // -----------------------------------------------------
        // UNIVERSAL ANALYSIS
        // -----------------------------------------------------

        steps.add(
                "حلل الهدف وحدد النتيجة النهائية القابلة للقياس."
        );

        // -----------------------------------------------------
        // LEARNING
        // -----------------------------------------------------

        if ("LEARNING".equals(type)) {

            steps.add(
                    "حدد المهارة أو المعرفة الأساسية المطلوبة."
            );

            steps.add(
                    "قسم التعلم إلى وحدات صغيرة من السهل تنفيذها."
            );

            steps.add(
                    "ابدأ بأول درس أو تمرين عملي."
            );

            steps.add(
                    "طبق ما تعلمته في تجربة حقيقية."
            );

            steps.add(
                    "اختبر المستوى واكتشف نقاط الضعف."
            );

            steps.add(
                    "راجع الأخطاء وحسن طريقة التعلم."
            );

            steps.add(
                    "كرر دورة التعلم حتى يتحقق المستوى المطلوب."
            );

            return steps;
        }

        // -----------------------------------------------------
        // BUSINESS
        // -----------------------------------------------------

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
                    "حسن العرض وكرر عملية الوصول للعملاء."
            );

            return steps;
        }

        // -----------------------------------------------------
        // FITNESS
        // -----------------------------------------------------

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
                    "راقب التقدم وعدل الخطة عند الحاجة."
            );

            steps.add(
                    "كرر الدورة مع رفع المستوى تدريجياً."
            );

            return steps;
        }

        // -----------------------------------------------------
        // DEVELOPMENT
        // -----------------------------------------------------

        if ("DEVELOPMENT".equals(type)) {

            steps.add(
                    "حدد الوظيفة المطلوبة والنتيجة التي يجب أن يعمل بها النظام."
            );

            steps.add(
                    "حلل البنية الحالية والملفات المرتبطة بالميزة."
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
                    "إذا نجح الاختبار سجل التطور، وإذا فشل نفذ Rollback."
            );

            return steps;
        }

        // -----------------------------------------------------
        // PERSONAL
        // -----------------------------------------------------

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
                    "كرر النظام حتى يصبح السلوك ثابتاً."
            );

            return steps;
        }

        // -----------------------------------------------------
        // GENERAL
        // -----------------------------------------------------

        steps.add(
                "حدد الموارد والمعلومات المطلوبة لتحقيق الهدف."
        );

        steps.add(
                "قسم الهدف إلى مراحل صغيرة قابلة للتنفيذ."
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
                "كرر الدورة حتى يتحقق الهدف."
        );

        return steps;
    }

    // =========================================================
    // ADD STEP AS TASK
    // =========================================================

    public String addPlanStepAsTask(
            String step
    ) {

        if (step == null ||
                step.trim().isEmpty()) {

            return "المهمة فارغة.";
        }

        return taskManager.addTask(
                step.trim()
        );
    }

    // =========================================================
    // ADD CURRENT PLAN STEP
    // =========================================================

    public String addCurrentStepAsTask() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {

            return
                    "ما كايناش خطة نشطة.";
        }

        int progress =
                getProgress();

        if (progress >= steps.size()) {

            return
                    "الخطة كاملة، ما بقا حتى Step.";
        }

        return taskManager.addTask(
                steps.get(progress)
        );
    }

    // =========================================================
    // COMPLETE CURRENT STEP
    // =========================================================

    public String completeCurrentStep() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {

            return
                    "ما كايناش خطة نشطة.";
        }

        int progress =
                getProgress();

        if (progress >= steps.size()) {

            return
                    "الخطة راه كاملة ✓";
        }

        progress++;

        memoryManager.saveMemory(
                PLAN_PROGRESS_KEY,
                String.valueOf(progress)
        );

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
    // GET ACTIVE PLAN
    // =========================================================

    public String getActivePlan() {

        String goal =
                memoryManager.getMemory(
                        ACTIVE_PLAN_KEY
                );

        if (goal == null ||
                goal.trim().isEmpty()) {

            return
                    "ما كاين حتى Plan نشطة حاليا.";
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

    // =========================================================
    // PROGRESS
    // =========================================================

    public String getProgressReport() {

        List<String> steps =
                getStoredSteps();

        if (steps.isEmpty()) {

            return
                    "ما كايناش خطة باش نحسب التقدم.";
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

    public String clearActivePlan() {

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

        return
                "تم حذف الخطة النشطة ✓";
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
                type == null || type.isEmpty()
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
                "% ("
        );

        plan.append(
                progress
        );

        plan.append(
                "/"
        );

        plan.append(
                steps.size()
        );

        plan.append(
                ")"
        );

        return plan.toString();
    }

    // =========================================================
    // READ STORED STEPS
    // =========================================================

    private List<String> getStoredSteps() {

        String serialized =
                memoryManager.getMemory(
                        PLAN_STEPS_KEY
                );

        List<String> steps =
                new ArrayList<>();

        if (serialized == null ||
                serialized.trim().isEmpty()) {

            return steps;
        }

        String[] parts =
                serialized.split(
                        "\\|\\|",
                        -1
                );

        for (String part : parts) {

            if (part != null &&
                    !part.trim().isEmpty()) {

                steps.add(
                        part.trim()
                );
            }
        }

        return steps;
    }

    // =========================================================
    // SERIALIZE STEPS
    // =========================================================

    private String serializeSteps(
            List<String> steps
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < steps.size();
             i++) {

            if (i > 0) {
                result.append("||");
            }

            String step =
                    steps.get(i);

            if (step == null) {
                step = "";
            }

            result.append(
                    step.replace(
                            "||",
                            " "
                    )
            );
        }

        return result.toString();
    }

    // =========================================================
    // GET PROGRESS
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

            if (progress > steps.size()) {
                return steps.size();
            }

            return progress;

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        try {

            taskManager.getTaskCount();

            List<String> steps =
                    getStoredSteps();

            int progress =
                    getProgress();

            return
                    "Planning Engine: ONLINE ✓\n"
                    + "Active steps: "
                    + steps.size()
                    + "\n"
                    + "Progress: "
                    + progress
                    + "/"
                    + steps.size();

        } catch (Exception e) {

            return
                    "Planning Engine: ERROR ⚠";
        }
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            taskManager.getTaskCount();

            memoryManager.getMemoryCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // MATCHING
    // =========================================================

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
                    value.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }

        return false;
    }
}