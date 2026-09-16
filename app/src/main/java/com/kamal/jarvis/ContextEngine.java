package com.kamal.jarvis;

import android.content.Context;

public class ContextEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final ActionHistoryManager actionHistoryManager;
    private final DecisionEngine decisionEngine;
    private final PlanningEngine planningEngine;
    private final EvolutionEngine evolutionEngine;

    private static final String KEY_LAST_COMMAND =
            "__context_last_command__";

    private static final String KEY_LAST_RESPONSE =
            "__context_last_response__";

    private static final String KEY_ACTIVE_GOAL =
            "__context_active_goal__";

    private static final String KEY_ACTIVE_PLAN =
            "__context_active_plan__";

    private static final String KEY_LAST_DECISION =
            "__context_last_decision__";

    public ContextEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        actionHistoryManager =
                new ActionHistoryManager(this.context);

        decisionEngine =
                new DecisionEngine(this.context);

        planningEngine =
                new PlanningEngine(this.context);

        evolutionEngine =
                new EvolutionEngine(this.context);
    }

    public void updateCommand(
            String command,
            String response
    ) {

        if (command != null &&
                !command.trim().isEmpty()) {

            memoryManager.saveMemory(
                    KEY_LAST_COMMAND,
                    command.trim()
            );
        }

        if (response != null &&
                !response.trim().isEmpty()) {

            memoryManager.saveMemory(
                    KEY_LAST_RESPONSE,
                    response.trim()
            );
        }
    }

    public void updateGoal(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return;
        }

        memoryManager.saveMemory(
                KEY_ACTIVE_GOAL,
                goal.trim()
        );
    }

    public void updatePlan(
            String plan
    ) {

        if (plan == null ||
                plan.trim().isEmpty()) {

            return;
        }

        memoryManager.saveMemory(
                KEY_ACTIVE_PLAN,
                plan.trim()
        );
    }

    public void updateDecision(
            String decision
    ) {

        if (decision == null ||
                decision.trim().isEmpty()) {

            return;
        }

        memoryManager.saveMemory(
                KEY_LAST_DECISION,
                decision.trim()
        );
    }

    public String getLastCommand() {

        return memoryManager.getMemory(
                KEY_LAST_COMMAND
        );
    }

    public String getLastResponse() {

        return memoryManager.getMemory(
                KEY_LAST_RESPONSE
        );
    }

    public String getActiveGoal() {

        return memoryManager.getMemory(
                KEY_ACTIVE_GOAL
        );
    }

    public String getActivePlan() {

        return memoryManager.getMemory(
                KEY_ACTIVE_PLAN
        );
    }

    public String getLastDecision() {

        return memoryManager.getMemory(
                KEY_LAST_DECISION
        );
    }

    public String getLastAction() {

        return actionHistoryManager.getLastAction();
    }

    public String getCurrentDevelopmentTarget() {

        return evolutionEngine
                .getActiveDevelopmentTarget();
    }

    public String getFullContext() {

        StringBuilder contextReport =
                new StringBuilder();

        contextReport.append(
                "=== JARVIS CONTEXT ===\n\n"
        );

        contextReport.append(
                "آخر أمر:\n"
        );

        contextReport.append(
                safe(getLastCommand())
        );

        contextReport.append(
                "\n\nآخر رد:\n"
        );

        contextReport.append(
                safe(getLastResponse())
        );

        contextReport.append(
                "\n\nالهدف الحالي:\n"
        );

        contextReport.append(
                safe(getActiveGoal())
        );

        contextReport.append(
                "\n\nالخطة الحالية:\n"
        );

        contextReport.append(
                safe(getActivePlan())
        );

        contextReport.append(
                "\n\nآخر قرار:\n"
        );

        contextReport.append(
                safe(getLastDecision())
        );

        contextReport.append(
                "\n\nآخر عملية:\n"
        );

        contextReport.append(
                safe(getLastAction())
        );

        contextReport.append(
                "\n\nهدف التطوير الحالي:\n"
        );

        contextReport.append(
                safe(getCurrentDevelopmentTarget())
        );

        return contextReport.toString();
    }

    public String getContextSummary() {

        return
                "السياق الحالي ديال JARVIS:\n\n"
                + "الأمر الأخير: "
                + safe(getLastCommand())
                + "\n"
                + "الهدف: "
                + safe(getActiveGoal())
                + "\n"
                + "القرار الأخير: "
                + safe(getLastDecision())
                + "\n"
                + "التطوير الحالي: "
                + safe(getCurrentDevelopmentTarget());
    }

    public String analyzeCurrentContext() {

        String command =
                getLastCommand();

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "ما كاين حتى أمر حالي لتحليله.";
        }

        String decision =
                decisionEngine
                        .decide(command);

        updateDecision(decision);

        return
                "تم تحليل السياق ✓\n\n"
                + "الأمر:\n"
                + command
                + "\n\n"
                + "القرار:\n"
                + decision;
    }

    public String createContextPlan() {

        String goal =
                getActiveGoal();

        if (goal == null ||
                goal.trim().isEmpty()) {

            return
                    "ما كاين حتى هدف حالي باش نصاوب ليه خطة.";
        }

        String plan =
                planningEngine
                        .createPlan(goal);

        updatePlan(plan);

        return plan;
    }

    public String clearContext() {

        memoryManager.removeMemory(
                KEY_LAST_COMMAND
        );

        memoryManager.removeMemory(
                KEY_LAST_RESPONSE
        );

        memoryManager.removeMemory(
                KEY_ACTIVE_GOAL
        );

        memoryManager.removeMemory(
                KEY_ACTIVE_PLAN
        );

        memoryManager.removeMemory(
                KEY_LAST_DECISION
        );

        return
                "تم تنظيف السياق الحالي ✓";
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            actionHistoryManager.getHistory();

            decisionEngine.getStatus();

            planningEngine.getStatus();

            evolutionEngine.getEvolutionStatus();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Context Engine: ONLINE ✓";
        }

        return
                "Context Engine: ERROR ⚠";
    }

    private String safe(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "غير متوفر";
        }

        return value;
    }
}