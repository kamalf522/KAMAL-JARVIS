package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * JARVIS Autonomous Evolution Engine
 *
 * المحرك المركزي للتطور الذاتي ديال JARVIS.
 *
 * عندو جوج مسارات:
 *
 * 1. INTERNAL EVOLUTION
 *    - Memory
 *    - Learning
 *    - Skills
 *    - Capabilities
 *    - Tasks
 *
 * 2. CODE EVOLUTION
 *    - Analysis
 *    - Snapshot
 *    - Source modification
 *    - Verification
 *    - Tests
 *    - Rollback
 *    - APK build request
 *
 * ملاحظة:
 * التطوير الداخلي يقدر يوقع بلا APK.
 * أما تغيير Java/XML التنفيذي فيحتاج Build وتحديث التطبيق.
 */
public class AutonomousEvolutionEngine {

    private static final String PREFS =
            "JARVIS_AUTONOMOUS_EVOLUTION";

    private static final String HISTORY_KEY =
            "history";

    private static final String INTERNAL_HISTORY_KEY =
            "internal_history";

    private final Context context;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SelfTestEngine selfTestEngine;
    private final CodeEvolutionEngine codeEvolutionEngine;
    private final SelfDiagnosisManager diagnosisManager;
    private final ApkBuilderEngine apkBuilderEngine;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final LearningEngine learningEngine;
    private final TaskManager taskManager;
    private final ActionHistoryManager actionHistoryManager;

    private final SharedPreferences preferences;

    public AutonomousEvolutionEngine(
            Context context
    ) {

        this.context =
                context.getApplicationContext();

        preferences =
                this.context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        selfBuilderEngine =
                new SelfBuilderEngine(
                        this.context
                );

        selfTestEngine =
                new SelfTestEngine(
                        this.context
                );

        codeEvolutionEngine =
                new CodeEvolutionEngine(
                        this.context
                );

        diagnosisManager =
                new SelfDiagnosisManager(
                        this.context
                );

        apkBuilderEngine =
                new ApkBuilderEngine(
                        this.context
                );

        memoryManager =
                new MemoryManager(
                        this.context
                );

        skillManager =
                new SkillManager(
                        this.context
                );

        capabilityManager =
                new CapabilityManager(
                        this.context
                );

        learningEngine =
                new LearningEngine(
                        this.context
                );

        taskManager =
                new TaskManager(
                        this.context
                );

        actionHistoryManager =
                new ActionHistoryManager(
                        this.context
                );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS AUTONOMOUS EVOLUTION\n\n"
        );

        result.append(
                "Engine: "
        )
                .append(
                        isHealthy()
                                ? "ONLINE ✓"
                                : "ATTENTION ⚠"
                )
                .append("\n");

        result.append(
                "Internal Evolution: "
        )
                .append(
                        isInternalEvolutionHealthy()
                                ? "READY ✓"
                                : "ERROR ⚠"
                )
                .append("\n");

        result.append(
                "Self Builder: "
        )
                .append(
                        safeHealth(
                                selfBuilderEngine
                        )
                                ? "READY ✓"
                                : "ERROR ⚠"
                )
                .append("\n");

        result.append(
                "Code Evolution: "
        )
                .append(
                        safeHealth(
                                codeEvolutionEngine
                        )
                                ? "READY ✓"
                                : "ERROR ⚠"
                )
                .append("\n");

        result.append(
                "Self Test: "
        )
                .append(
                        safeHealth(
                                selfTestEngine
                        )
                                ? "READY ✓"
                                : "ERROR ⚠"
                )
                .append("\n");

        result.append(
                "APK Builder: "
        )
                .append(
                        safeHealth(
                                apkBuilderEngine
                        )
                                ? "READY ✓"
                                : "BUILD ENVIRONMENT NEEDED"
                )
                .append("\n");

        result.append(
                "\nSkills: "
        )
                .append(
                        safeSkillCount()
                )
                .append("\n");

        result.append(
                "Capabilities: "
        )
                .append(
                        safeCapabilityCount()
                )
                .append("\n");

        result.append(
                "Memory: "
        )
                .append(
                        safeMemoryCount()
                )
                .append("\n");

        result.append(
                "Pending Tasks: "
        )
                .append(
                        safePendingTasks()
                )
                .append("\n");

        result.append(
                "\nWorkspace:\n"
        );

        result.append(
                selfBuilderEngine.getWorkspacePath()
        );

        return result.toString();
    }

    public boolean isHealthy() {

        try {

            return selfBuilderEngine != null
                    && selfTestEngine != null
                    && codeEvolutionEngine != null
                    && diagnosisManager != null
                    && apkBuilderEngine != null
                    && memoryManager != null
                    && skillManager != null
                    && capabilityManager != null
                    && learningEngine != null
                    && taskManager != null;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // INTERNAL EVOLUTION
    // =========================================================

    public boolean isInternalEvolutionHealthy() {

        try {

            return skillManager.getSkillCount() >= 0
                    && capabilityManager.getCount() >= 0
                    && memoryManager.getMemoryCount() >= 0
                    && learningEngine.isHealthy()
                    && taskManager.isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    public String getInternalEvolutionStatus() {

        if (!isInternalEvolutionHealthy()) {

            return
                    "INTERNAL EVOLUTION: ERROR ⚠";
        }

        return
                "INTERNAL EVOLUTION: ONLINE ✓\n"
                        + "Skills: "
                        + safeSkillCount()
                        + "\n"
                        + "Capabilities: "
                        + safeCapabilityCount()
                        + "\n"
                        + "Memory: "
                        + safeMemoryCount()
                        + "\n"
                        + "Pending Tasks: "
                        + safePendingTasks();
    }

    // =========================================================
    // ANALYZE
    // =========================================================

    public String analyzeBeforeEvolution(
            String goal
    ) {

        String cleanGoal =
                normalizeGoal(goal);

        try {

            String diagnosis =
                    diagnosisManager.runDiagnosis();

            String project =
                    codeEvolutionEngine.analyzeProject();

            String plan =
                    codeEvolutionEngine
                            .generateDevelopmentPlan(
                                    cleanGoal
                            );

            saveMemory(
                    "__autonomous_last_goal__",
                    cleanGoal
            );

            saveMemory(
                    "__autonomous_last_analysis__",
                    project
            );

            recordHistory(
                    "ANALYZE",
                    cleanGoal
            );

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS EVOLUTION ANALYSIS\n\n"
            );

            result.append(
                    "GOAL:\n"
            );

            result.append(
                    cleanGoal
            );

            result.append(
                    "\n\nDIAGNOSIS:\n"
            );

            result.append(
                    diagnosis
            );

            result.append(
                    "\n\nPROJECT:\n"
            );

            result.append(
                    project
            );

            result.append(
                    "\n\nPLAN:\n"
            );

            result.append(
                    plan
            );

            return result.toString();

        } catch (Exception e) {

            recordHistory(
                    "ANALYZE_FAILED",
                    safeError(e)
            );

            return
                    "Evolution Analysis Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // PREPARE
    // =========================================================

    public String prepareEvolution(
            String goal
    ) {

        String cleanGoal =
                normalizeGoal(goal);

        try {

            String analysis =
                    analyzeBeforeEvolution(
                            cleanGoal
                    );

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "قبل التطوير المستقل: "
                                    + cleanGoal
                    );

            saveMemory(
                    "__autonomous_last_snapshot__",
                    snapshot
            );

            recordHistory(
                    "PREPARE",
                    cleanGoal
                            + " | "
                            + snapshot
            );

            return
                    "EVOLUTION PREPARED ✓\n\n"
                            + analysis
                            + "\n\nSNAPSHOT:\n"
                            + snapshot;

        } catch (Exception e) {

            recordHistory(
                    "PREPARE_FAILED",
                    cleanGoal
                            + " | "
                            + safeError(e)
            );

            return
                    "Evolution Preparation Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // INTERNAL EVOLUTION
    // =========================================================

    public String runInternalEvolution(
            String goal
    ) {

        String cleanGoal =
                normalizeGoal(goal);

        try {

            String baseline =
                    selfTestEngine.runAllTests();

            if (!testPassed(baseline)) {

                recordHistory(
                        "INTERNAL_BASELINE_FAILED",
                        cleanGoal
                );

                return
                        "INTERNAL EVOLUTION توقف ⚠\n\n"
                                + "النظام محتاج إصلاح أولا.\n\n"
                                + baseline;
            }

            String domain =
                    detectInternalDomain(
                            cleanGoal
                    );

            String skillName =
                    buildSkillName(
                            domain
                    );

            String capabilityName =
                    buildCapabilityName(
                            domain
                    );

            String description =
                    buildEvolutionDescription(
                            domain,
                            cleanGoal
                    );

            boolean capabilityAdded =
                    capabilityManager.addCapability(
                            capabilityName,
                            description
                    );

            boolean skillAdded =
                    skillManager.addSkill(
                            skillName,
                            description
                    );

            String learningSubject =
                    "evolution_" + domain;

            String learningInformation =
                    "الهدف: "
                            + cleanGoal
                            + "\nالمجال: "
                            + domain
                            + "\nالقاعدة: "
                            + description
                            + "\nالتطور: داخلي بدون APK.";

            String learningResult =
                    learningEngine.learn(
                            learningSubject,
                            learningInformation
                    );

            String ruleKey =
                    "__evolution_rule__"
                            + domain;

            String oldRule =
                    memoryManager.getMemory(
                            ruleKey
                    );

            int count =
                    getEvolutionCount(
                            domain
                    ) + 1;

            String rule =
                    "domain="
                            + domain
                            + "\ncount="
                            + count
                            + "\nlast_goal="
                            + cleanGoal
                            + "\