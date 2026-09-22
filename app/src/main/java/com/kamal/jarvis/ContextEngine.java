package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * JARVIS Context Engine
 *
 * مسؤول على بناء السياق الحالي ديال JARVIS:
 * - آخر الأوامر
 * - آخر الردود
 * - الهدف الحالي
 * - الخطة الحالية
 * - آخر قرار
 * - آخر عملية
 * - هدف التطوير
 * - نتائج العمليات
 *
 * الهدف:
 * مايبقاش JARVIS كيتعامل مع كل أمر بوحدو،
 * ولكن يفهم شنو كان واقع قبل الأمر الحالي.
 */
public class ContextEngine {

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

    private static final String KEY_LAST_ACTION =
            "__context_last_action__";

    private static final String KEY_LAST_RESULT =
            "__context_last_result__";

    private static final String KEY_COMMAND_COUNT =
            "__context_command_count__";

    private static final String KEY_CONTEXT_VERSION =
            "__context_version__";

    private static final String KEY_HISTORY =
            "__context_history__";

    private static final String KEY_LAST_INTENT =
            "__context_last_intent__";

    private static final String KEY_LAST_CONFIDENCE =
            "__context_last_confidence__";

    private final Context context;

    private final MemoryManager memoryManager;
    private final ActionHistoryManager actionHistoryManager;
    private final DecisionEngine decisionEngine;
    private final PlanningEngine planningEngine;
    private final EvolutionEngine evolutionEngine;

    public ContextEngine(Context context) {

        if (context == null) {

            throw new IllegalArgumentException(
                    "ContextEngine context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(
                        this.context
                );

        actionHistoryManager =
                new ActionHistoryManager(
                        this.context
                );

        decisionEngine =
                new DecisionEngine(
                        this.context
                );

        planningEngine =
                new PlanningEngine(
                        this.context
                );

        evolutionEngine =
                new EvolutionEngine(
                        this.context
                );

        initializeContext();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeContext() {

        try {

            if (memoryManager.getMemory(
                    KEY_CONTEXT_VERSION
            ) == null) {

                memoryManager.saveMemory(
                        KEY_CONTEXT_VERSION,
                        "2.0"
                );
            }

            if (memoryManager.getMemory(
                    KEY_COMMAND_COUNT
            ) == null) {

                memoryManager.saveMemory(
                        KEY_COMMAND_COUNT,
                        "0"
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // UPDATE COMMAND
    // =========================================================

    public synchronized void updateCommand(
            String command,
            String response
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String cleanCommand =
                command.trim();

        memoryManager.saveMemory(
                KEY_LAST_COMMAND,
                cleanCommand
        );

        if (response != null &&
                !response.trim().isEmpty()) {

            memoryManager.saveMemory(
                    KEY_LAST_RESPONSE,
                    response.trim()
            );
        }

        incrementCommandCount();

        addHistoryEntry(
                "COMMAND",
                cleanCommand
        );
    }

    // =========================================================
    // UPDATE GOAL
    // =========================================================

    public synchronized void updateGoal(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return;
        }

        String cleanGoal =
                goal.trim();

        memoryManager.saveMemory(
                KEY_ACTIVE_GOAL,
                cleanGoal
        );

        addHistoryEntry(
                "GOAL",
                cleanGoal
        );
    }

    // =========================================================
    // UPDATE PLAN
    // =========================================================

    public synchronized void updatePlan(
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

        addHistoryEntry(
                "PLAN",
                plan.trim()
        );
    }

    // =========================================================
    // UPDATE DECISION
    // =========================================================

    public synchronized void updateDecision(
            String decision
    ) {

        if (decision == null ||
                decision.trim().isEmpty()) {

            return;
        }

        String cleanDecision =
                decision.trim();

        memoryManager.saveMemory(
                KEY_LAST_DECISION,
                cleanDecision
        );

        extractDecisionData(
                cleanDecision
        );

        addHistoryEntry(
                "DECISION",
                cleanDecision
        );
    }

    // =========================================================
    // UPDATE ACTION
    // =========================================================

    public synchronized void updateAction(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

            return;
        }

        String cleanAction =
                action.trim();

        memoryManager.saveMemory(
                KEY_LAST_ACTION,
                cleanAction
        );

        addHistoryEntry(
                "ACTION",
                cleanAction
        );
    }

    // =========================================================
    // UPDATE RESULT
    // =========================================================

    public synchronized void updateResult(
            String result
    ) {

        if (result == null ||
                result.trim().isEmpty()) {

            return;
        }

        String cleanResult =
                result.trim();

        memoryManager.saveMemory(
                KEY_LAST_RESULT,
                cleanResult
        );

        addHistoryEntry(
                "RESULT",
                cleanResult
        );
    }

    // =========================================================
    // GET LAST COMMAND
    // =========================================================

    public String getLastCommand() {

        return memoryManager.getMemory(
                KEY_LAST_COMMAND
        );
    }

    // =========================================================
    // GET LAST RESPONSE
    // =========================================================

    public String getLastResponse() {

        return memoryManager.getMemory(
                KEY_LAST_RESPONSE
        );
    }

    // =========================================================
    // GET ACTIVE GOAL
    // =========================================================

    public String getActiveGoal() {

        return memoryManager.getMemory(
                KEY_ACTIVE_GOAL
        );
    }

    // =========================================================
    // GET ACTIVE PLAN
    // =========================================================

    public String getActivePlan() {

        return memoryManager.getMemory(
                KEY_ACTIVE_PLAN
        );
    }

    // =========================================================
    // GET LAST DECISION
    // =========================================================

    public String getLastDecision() {

        return memoryManager.getMemory(
                KEY_LAST_DECISION
        );
    }

    // =========================================================
    // GET LAST ACTION
    // =========================================================

    public String getLastAction() {

        String stored =
                memoryManager.getMemory(
                        KEY_LAST_ACTION
                );

        if (stored != null &&
                !stored.trim().isEmpty()) {

            return stored;
        }

        try {

            return actionHistoryManager
                    .getLastAction();

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GET LAST RESULT
    // =========================================================

    public String getLastResult() {

        return memoryManager.getMemory(
                KEY_LAST_RESULT
        );
    }

    // =========================================================
    // GET LAST INTENT
    // =========================================================

    public String getLastIntent() {

        return memoryManager.getMemory(
                KEY_LAST_INTENT
        );
    }

    // =========================================================
    // GET LAST CONFIDENCE
    // =========================================================

    public String getLastConfidence() {

        return memoryManager.getMemory(
                KEY_LAST_CONFIDENCE
        );
    }

    // =========================================================
    // GET COMMAND COUNT
    // =========================================================

    public String getCommandCount() {

        String count =
                memoryManager.getMemory(
                        KEY_COMMAND_COUNT
                );

        if (count == null ||
                count.trim().isEmpty()) {

            return "0";
        }

        return count;
    }

    // =========================================================
    // GET DEVELOPMENT TARGET
    // =========================================================

    public String getCurrentDevelopmentTarget() {

        try {

            String target =
                    evolutionEngine
                            .getActiveDevelopmentTarget();

            if (target == null ||
                    target.trim().isEmpty()) {

                return "ما كاين حتى هدف تطوير حالي.";
            }

            return target;

        } catch (Exception e) {

            return "هدف التطوير غير متوفر.";
        }
    }

    // =========================================================
    // ANALYZE CURRENT CONTEXT
    // =========================================================

    public synchronized String analyzeCurrentContext() {

        String command =
                getLastCommand();

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "ما كاين حتى أمر حالي لتحليله.";
        }

        try {

            String decision =
                    decisionEngine.decide(
                            command
                    );

            updateDecision(
                    decision
            );

            return
                    "تم تحليل السياق ✓\n\n"
                    + "الأمر:\n"
                    + command
                    + "\n\n"
                    + "القرار:\n"
                    + decision;

        } catch (Exception e) {

            return
                    "تعذر تحليل السياق: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // CREATE CONTEXT PLAN
    // =========================================================

    public synchronized String createContextPlan() {

        String goal =
                getActiveGoal();

        if (goal == null ||
                goal.trim().isEmpty()) {

            return
                    "ما كاين حتى هدف حالي باش نصاوب ليه خطة.";
        }

        try {

            String plan =
                    planningEngine.createPlan(
                            goal
                    );

            updatePlan(
                    plan
            );

            return plan;

        } catch (Exception e) {

            return
                    "تعذر إنشاء الخطة: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // FULL CONTEXT
    // =========================================================

    public synchronized String getFullContext() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "=== JARVIS CONTEXT ENGINE ===\n\n"
        );

        report.append(
                "السياق: ACTIVE ✓\n"
        );

        report.append(
                "الإصدار: "
        );

        report.append(
                safe(
                        memoryManager.getMemory(
                                KEY_CONTEXT_VERSION
                        )
                )
        );

        report.append(
                "\nالأوامر المعالجة: "
        );

        report.append(
                getCommandCount()
        );

        report.append(
                "\n\nآخر أمر:\n"
        );

        report.append(
                safe(
                        getLastCommand()
                )
        );

        report.append(
                "\n\nآخر رد:\n"
        );

        report.append(
                safe(
                        getLastResponse()
                )
        );

        report.append(
                "\n\nالهدف الحالي:\n"
        );

        report.append(
                safe(
                        getActiveGoal()
                )
        );

        report.append(
                "\n\nالخطة الحالية:\n"
        );

        report.append(
                safe(
                        getActivePlan()
                )
        );

        report.append(
                "\n\nآخر قرار:\n"
        );

        report.append(
                safe(
                        getLastDecision()
                )
        );

        report.append(
                "\n\nآخر Intent:\n"
        );

        report.append(
                safe(
                        getLastIntent()
                )
        );

        report.append(
                "\n\nدرجة الثقة:\n"
        );

        report.append(
                safe(
                        getLastConfidence()
                )
        );

        report.append(
                "\n\nآخر عملية:\n"
        );

        report.append(
                safe(
                        getLastAction()
                )
        );

        report.append(
                "\n\nآخر نتيجة:\n"
        );

        report.append(
                safe(
                        getLastResult()
                )
        );

        report.append(
                "\n\nهدف التطوير الحالي:\n"
        );

        report.append(
                safe(
                        getCurrentDevelopmentTarget()
                )
        );

        return report.toString();
    }

    // =========================================================
    // CONTEXT SUMMARY
    // =========================================================

    public synchronized String getContextSummary() {

        return
                "السياق الحالي ديال JARVIS:\n\n"
                        + "الأمر الأخير: "
                        + safe(
                                getLastCommand()
                        )
                        + "\n"
                        + "الهدف: "
                        + safe(
                                getActiveGoal()
                        )
                        + "\n"
                        + "الخطة: "
                        + safe(
                                getActivePlan()
                        )
                        + "\n"
                        + "القرار: "
                        + safe(
                                getLastIntent()
                        )
                        + "\n"
                        + "الثقة: "
                        + safe(
                                getLastConfidence()
                        )
                        + "\n"
                        + "آخر نتيجة: "
                        + safe(
                                getLastResult()
                        );
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public synchronized String getContextHistory() {

        String history =
                memoryManager.getMemory(
                        KEY_HISTORY
                );

        if (history == null ||
                history.trim().isEmpty()) {

            return
                    "مازال ما كاين حتى تاريخ للسياق.";
        }

        return
                "=== CONTEXT HISTORY ===\n\n"
                        + history;
    }

    // =========================================================
    // CLEAR CONTEXT
    // =========================================================

    public synchronized String clearContext() {

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

        memoryManager.removeMemory(
                KEY_LAST_ACTION
        );

        memoryManager.removeMemory(
                KEY_LAST_RESULT
        );

        memoryManager.removeMemory(
                KEY_LAST_INTENT
        );

        memoryManager.removeMemory(
                KEY_LAST_CONFIDENCE
        );

        memoryManager.removeMemory(
                KEY_HISTORY
        );

        memoryManager.saveMemory(
                KEY_COMMAND_COUNT,
                "0"
        );

        return
                "تم تنظيف السياق الحالي ✓";
    }

    // =========================================================
    // BUILD CONTEXT FOR DECISION
    // =========================================================

    public synchronized String buildDecisionContext(
            String newCommand
    ) {

        StringBuilder context =
                new StringBuilder();

        context.append(
                "=== DECISION CONTEXT ===\n"
        );

        context.append(
                "CURRENT COMMAND: "
        )
                .append(
                        safe(newCommand)
                );

        context.append(
                "\nLAST COMMAND: "
        )
                .append(
                        safe(
                                getLastCommand()
                        )
                );

        context.append(
                "\nACTIVE GOAL: "
        )
                .append(
                        safe(
                                getActiveGoal()
                        )
                );

        context.append(
                "\nACTIVE PLAN: "
        )
                .append(
                        safe(
                                getActivePlan()
                        )
                );

        context.append(
                "\nLAST INTENT: "
        )
                .append(
                        safe(
                                getLastIntent()
                        )
                );

        context.append(
                "\nLAST RESULT: "
        )
                .append(
                        safe(
                                getLastResult()
                        )
                );

        context.append(
                "\nDEVELOPMENT TARGET: "
        )
                .append(
                        safe(
                                getCurrentDevelopmentTarget()
                        )
                );

        return context.toString();
    }

    // =========================================================
    // DECISION DATA EXTRACTION
    // =========================================================

    private void extractDecisionData(
            String decision
    ) {

        if (decision == null) {
            return;
        }

        String intent =
                extractValue(
                        decision,
                        "التصنيف:"
                );

        if (intent != null &&
                !intent.isEmpty()) {

            memoryManager.saveMemory(
                    KEY_LAST_INTENT,
                    intent
            );
        }

        String confidence =
                extractValue(
                        decision,
                        "الثقة:"
                );

        if (confidence != null &&
                !confidence.isEmpty()) {

            memoryManager.saveMemory(
                    KEY_LAST_CONFIDENCE,
                    confidence
            );
        }
    }

    // =========================================================
    // EXTRACT VALUE
    // =========================================================

    private String extractValue(
            String source,
            String marker
    ) {

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

            if (value.endsWith("%")) {

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        ).trim();
            }

            return value;

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // HISTORY ENTRY
    // =========================================================

    private void addHistoryEntry(
            String type,
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return;
        }

        try {

            String current =
                    memoryManager.getMemory(
                            KEY_HISTORY
                    );

            List<String> entries =
                    new ArrayList<>();

            if (current != null &&
                    !current.trim().isEmpty()) {

                String[] oldEntries =
                        current.split(
                                "\\n"
                        );

                for (String entry :
                        oldEntries) {

                    if (entry != null &&
                            !entry.trim().isEmpty()) {

                        entries.add(
                                entry.trim()
                        );
                    }
                }
            }

            String entry =
                    type
                            + ": "
                            + compact(
                                    value
                            );

            entries.add(
                    entry
            );

            /*
             * نحافظو على آخر 20 حدث فقط
             * باش الذاكرة ما تكبرش بلا حدود.
             */
            while (
                    entries.size() > 20
            ) {

                entries.remove(0);
            }

            StringBuilder history =
                    new StringBuilder();

            for (String item :
                    entries) {

                if (history.length() > 0) {
                    history.append("\n");
                }

                history.append(item);
            }

            memoryManager.saveMemory(
                    KEY_HISTORY,
                    history.toString()
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // INCREMENT COMMAND COUNT
    // =========================================================

    private void incrementCommandCount() {

        int count = 0;

        try {

            String stored =
                    memoryManager.getMemory(
                            KEY_COMMAND_COUNT
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
                KEY_COMMAND_COUNT,
                String.valueOf(
                        count + 1
                )
        );
    }

    // =========================================================
    // COMPACT TEXT
    // =========================================================

    private String compact(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String clean =
                value
                        .replace(
                                "\n",
                                " "
                        )
                        .replace(
                                "\r",
                                " "
                        )
                        .trim();

        if (clean.length() > 180) {

            return clean.substring(
                    0,
                    180
            ) + "...";
        }

        return clean;
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
    // SAFE ERROR
    // =========================================================

    private String safeMessage(
            Exception exception
    ) {

        if (exception == null) {
            return "خطأ غير معروف";
        }

        String message =
                exception.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            actionHistoryManager.getHistory();

            decisionEngine.getStatus();

            planningEngine.getStatus();

            evolutionEngine
                    .getEvolutionStatus();

            return true;

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
                    "Context Engine: ONLINE ✓";
        }

        return
                "Context Engine: ERROR ⚠";
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return context;
    }
}