package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class PlanningEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;

    public PlanningEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);
    }

    public String createPlan(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "خاصك تحدد الهدف اللي بغيتي نخطط ليه.";
        }

        String cleanGoal =
                goal.trim();

        List<String> steps =
                generateSteps(cleanGoal);

        StringBuilder plan =
                new StringBuilder();

        plan.append("JARVIS PLANNING ENGINE\n");
        plan.append("============================\n\n");

        plan.append("الهدف:\n");
        plan.append(cleanGoal);
        plan.append("\n\n");

        plan.append("الخطة:\n");

        for (int i = 0;
             i < steps.size();
             i++) {

            plan.append(i + 1);
            plan.append(". ");
            plan.append(steps.get(i));
            plan.append("\n");
        }

        memoryManager.saveMemory(
                "__active_plan__",
                cleanGoal
        );

        return plan.toString();
    }

    private List<String> generateSteps(
            String goal
    ) {

        List<String> steps =
                new ArrayList<>();

        steps.add(
                "حدد النتيجة النهائية بدقة."
        );

        steps.add(
                "قسم الهدف إلى مهام صغيرة."
        );

        steps.add(
                "حدد أول مهمة يمكن تنفيذها الآن."
        );

        steps.add(
                "نفذ المهمة وسجل النتيجة."
        );

        steps.add(
                "راجع النتيجة واكتشف الأخطاء."
        );

        steps.add(
                "حسن الخطة بناءً على النتيجة."
        );

        steps.add(
                "كرر الدورة حتى يتحقق الهدف."
        );

        return steps;
    }

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

    public String getActivePlan() {

        String plan =
                memoryManager.getMemory(
                        "__active_plan__"
                );

        if (plan == null ||
                plan.trim().isEmpty()) {

            return
                    "ما كاين حتى Plan نشطة حاليا.";
        }

        return
                "الخطة النشطة:\n\n"
                + plan;
    }

    public String clearActivePlan() {

        memoryManager.removeMemory(
                "__active_plan__"
        );

        return
                "تم حذف الخطة النشطة ✓";
    }

    public String getStatus() {

        try {

            taskManager.getTaskCount();

            return
                    "Planning Engine: ONLINE ✓";

        } catch (Exception e) {

            return
                    "Planning Engine: ERROR ⚠";
        }
    }

    public boolean isHealthy() {

        try {

            taskManager.getTaskCount();

            memoryManager.getMemoryCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}