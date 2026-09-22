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
                            + "\nlast_update="
                            + System.currentTimeMillis()
                            + "\nprevious="
                            + safeText(oldRule);

            memoryManager.saveMemory(
                    ruleKey,
                    rule
            );

            String taskResult =
                    taskManager.addTask(
                            "تطوير JARVIS: "
                                    + domain
                    );

            if (skillManager.hasSkill(
                    skillName
            )) {

                skillManager.recordSuccess(
                        skillName
                );
            }

            if (capabilityManager.hasCapability(
                    capabilityName
            )) {

                capabilityManager.recordSuccess(
                        capabilityName
                );
            }

            saveMemory(
                    "__last_internal_evolution_goal__",
                    cleanGoal
            );

            saveMemory(
                    "__last_internal_evolution_domain__",
                    domain
            );

            saveMemory(
                    "__last_internal_evolution_skill__",
                    skillName
            );

            saveMemory(
                    "__last_internal_evolution_capability__",
                    capabilityName
            );

            saveMemory(
                    "__last_internal_evolution_result__",
                    "SUCCESS"
            );

            saveMemory(
                    "__last_internal_learning_result__",
                    learningResult
            );

            saveMemory(
                    "__last_internal_task_result__",
                    taskResult
            );

            recordInternalHistory(
                    domain,
                    cleanGoal,
                    skillAdded,
                    capabilityAdded
            );

            actionHistoryManager.record(
                    "INTERNAL EVOLUTION: "
                            + cleanGoal,
                    "SUCCESS | "
                            + domain
            );

            String finalTests =
                    selfTestEngine.runAllTests();

            if (!testPassed(finalTests)) {

                recordHistory(
                        "INTERNAL_FINAL_TEST_FAILED",
                        domain
                );

                return
                        "INTERNAL EVOLUTION تمت ولكن الاختبار النهائي فيه مشكل ⚠\n\n"
                                + finalTests;
            }

            return
                    "INTERNAL EVOLUTION SUCCESS ✓\n\n"
                            + "الهدف:\n"
                            + cleanGoal
                            + "\n\nالمجال:\n"
                            + domain
                            + "\n\nSkill:\n"
                            + skillName
                            + (skillAdded
                            ? " ← جديدة ✓"
                            : " ← موجودة")
                            + "\n\nCapability:\n"
                            + capabilityName
                            + (capabilityAdded
                            ? " ← جديدة ✓"
                            : " ← موجودة")
                            + "\n\nLearning: ONLINE ✓"
                            + "\nMemory Rule: SAVED ✓"
                            + "\nTask: REGISTERED ✓";

        } catch (Exception e) {

            recordHistory(
                    "INTERNAL_FAILED",
                    cleanGoal
                            + " | "
                            + safeError(e)
            );

            saveMemory(
                    "__last_internal_evolution_result__",
                    "FAILED"
            );

            return
                    "INTERNAL EVOLUTION FAILED ⚠\n\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // SOURCE EVOLUTION
    // =========================================================

    public String evolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        if (isBlank(path)) {
            return "حدد مسار الملف.";
        }

        if (isBlank(oldText)) {
            return "الكود القديم فارغ.";
        }

        try {

            String cleanReason =
                    safeText(reason);

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "قبل Code Evolution: "
                                    + cleanReason
                    );

            saveMemory(
                    "__autonomous_last_snapshot__",
                    snapshot
            );

            String before =
                    selfBuilderEngine.getFileHash(
                            path
                    );

            String result =
                    codeEvolutionEngine.modifyCode(
                            path,
                            oldText,
                            newText,
                            cleanReason
                    );

            boolean success =
                    isSuccess(result);

            if (!success) {

                selfBuilderEngine.rollback(
                        extractSnapshotId(
                                snapshot
                        )
                );

                recordHistory(
                        "CODE_EVOLUTION_FAILED",
                        path
                );

                return
                        "CODE EVOLUTION FAILED ⚠\n\n"
                                + result
                                + "\n\nRollback attempted.";
            }

            String after =
                    selfBuilderEngine.getFileHash(
                            path
                    );

            saveMemory(
                    "__last_code_evolution_file__",
                    path
            );

            saveMemory(
                    "__last_code_evolution_before_hash__",
                    before
            );

            saveMemory(
                    "__last_code_evolution_after_hash__",
                    after
            );

            saveMemory(
                    "__last_code_evolution_reason__",
                    cleanReason
            );

            saveMemory(
                    "__last_code_evolution_result__",
                    "SUCCESS"
            );

            recordHistory(
                    "CODE_EVOLUTION_SUCCESS",
                    path
                            + " | "
                            + cleanReason
            );

            actionHistoryManager.record(
                    "CODE EVOLUTION: "
                            + path,
                    "SUCCESS"
            );

            return
                    "AUTONOMOUS CODE EVOLUTION SUCCESS ✓\n\n"
                            + result
                            + "\n\n"
                            + "Before Hash:\n"
                            + before
                            + "\n\n"
                            + "After Hash:\n"
                            + after;

        } catch (Exception e) {

            recordHistory(
                    "CODE_EVOLUTION_ERROR",
                    path
                            + " | "
                            + safeError(e)
            );

            return
                    "Code Evolution Error:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // CREATE SOURCE
    // =========================================================

    public String createSourceFile(
            String path,
            String content,
            String reason
    ) {

        if (isBlank(path)) {
            return "حدد مسار الملف.";
        }

        if (content == null) {
            return "محتوى الملف فارغ.";
        }

        try {

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "قبل إنشاء source file: "
                                    + safeText(reason)
                    );

            String result =
                    selfBuilderEngine.writeSourceFile(
                            path,
                            content
                    );

            if (!isSuccess(result)) {

                return
                        "SOURCE CREATION FAILED ⚠\n\n"
                                + result;
            }

            saveMemory(
                    "__last_created_source_file__",
                    path
            );

            saveMemory(
                    "__last_code_evolution_result__",
                    "SOURCE_CREATED"
            );

            recordHistory(
                    "SOURCE_CREATED",
                    path
            );

            return
                    "SOURCE FILE CREATED ✓\n\n"
                            + result
                            + "\n\nSnapshot:\n"
                            + snapshot;

        } catch (Exception e) {

            recordHistory(
                    "SOURCE_CREATE_FAILED",
                    path
            );

            return
                    "Source creation failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // FULL EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle(
            String goal
    ) {

        String cleanGoal =
                normalizeGoal(goal);

        try {

            String analysis =
                    analyzeBeforeEvolution(
                            cleanGoal
                    );

            String baseline =
                    selfTestEngine.runAllTests();

            if (!testPassed(baseline)) {

                return
                        "EVOLUTION CYCLE STOPPED ⚠\n\n"
                                + "Baseline tests failed.\n\n"
                                + baseline;
            }

            String prepare =
                    prepareEvolution(
                            cleanGoal
                    );

            saveMemory(
                    "__autonomous_cycle_goal__",
                    cleanGoal
            );

            saveMemory(
                    "__autonomous_cycle_started__",
                    String.valueOf(
                            System.currentTimeMillis()
                    )
            );

            /*
             * المرحلة الداخلية كتخلي JARVIS يطور
             * المعرفة والمهارات بلا APK.
             */
            String internal =
                    runInternalEvolution(
                            cleanGoal
                    );

            if (!isSuccess(internal)) {

                recordHistory(
                        "CYCLE_INTERNAL_FAILED",
                        cleanGoal
                );

                return
                        "EVOLUTION CYCLE PARTIAL ⚠\n\n"
                                + analysis
                                + "\n\n"
                                + prepare
                                + "\n\n"
                                + internal;
            }

            /*
             * ما غاديش نغير Java عشوائيا.
             * Code Evolution كتكون عبر الهدف/الملف المحدد
             * باستعمال evolveSourceFile().
             */
            recordHistory(
                    "CYCLE_INTERNAL_COMPLETE",
                    cleanGoal
            );

            saveMemory(
                    "__autonomous_last_cycle_result__",
                    "INTERNAL_EVOLUTION_SUCCESS"
            );

            return
                    "AUTONOMOUS EVOLUTION CYCLE COMPLETE ✓\n\n"
                            + "Analysis: DONE ✓\n"
                            + "Baseline Test: PASSED ✓\n"
                            + "Snapshot: CREATED ✓\n"
                            + "Internal Evolution: SUCCESS ✓\n\n"
                            + internal
                            + "\n\n"
                            + "Code Evolution:\n"
                            + "READY — خاصو target file + change محدد باش يتطبق بأمان.";

        } catch (Exception e) {

            recordHistory(
                    "CYCLE_FAILED",
                    cleanGoal
                            + " | "
                            + safeError(e)
            );

            saveMemory(
                    "__autonomous_last_cycle_result__",
                    "FAILED"
            );

            return
                    "AUTONOMOUS EVOLUTION CYCLE FAILED ⚠\n\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // APK BUILD
    // =========================================================

    public String prepareApkBuild(
            String reason
    ) {

        try {

            String validation =
                    apkBuilderEngine.validateProject();

            String request =
                    apkBuilderEngine.prepareBuildRequest(
                            safeText(reason)
                    );

            recordHistory(
                    "APK_BUILD_REQUEST",
                    safeText(reason)
            );

            return
                    "APK BUILD PREPARATION\n\n"
                            + "Validation:\n"
                            + validation
                            + "\n\nBuild Request:\n"
                            + request;

        } catch (Exception e) {

            return
                    "APK Build preparation failed:\n"
                            + safeError(e);
        }
    }

    public String buildApk() {

        try {

            String validation =
                    apkBuilderEngine.validateProject();

            if (!isSuccess(validation)) {

                return
                        "APK BUILD STOPPED ⚠\n\n"
                                + validation;
            }

            String result =
                    apkBuilderEngine.buildDebugApk();

            recordHistory(
                    "APK_BUILD",
                    result
            );

            saveMemory(
                    "__last_apk_build_result__",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "APK_BUILD_FAILED",
                    safeError(e)
            );

            return
                    "APK Build failed:\n"
                            + safeError(e);
        }
    }

    public String getApkStatus() {

        try {

            return apkBuilderEngine.getStatus();

        } catch (Exception e) {

            return
                    "APK Builder status unavailable:\n"
                            + safeError(e);
        }
    }

    public String getLatestApk() {

        try {

            return apkBuilderEngine.findLatestApk();

        } catch (Exception e) {

            return
                    "تعذر البحث عن APK:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // ROLLBACK
    // =========================================================

    public String rollbackLastEvolution() {

        try {

            String snapshot =
                    memoryManager.getMemory(
                            "__autonomous_last_snapshot__"
                    );

            if (isBlank(snapshot)) {

                return
                        "ما كاين حتى Snapshot محفوظ للتراجع.";
            }

            String snapshotId =
                    extractSnapshotId(
                            snapshot
                    );

            if (isBlank(snapshotId)) {

                return
                        "تعذر استخراج Snapshot ID.";
            }

            String result =
                    selfBuilderEngine.rollback(
                            snapshotId
                    );

            saveMemory(
                    "__last_rollback_result__",
                    result
            );

            recordHistory(
                    "ROLLBACK",
                    snapshotId
            );

            return result;

        } catch (Exception e) {

            return
                    "Rollback failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public String getInternalHistory() {

        return preferences.getString(
                INTERNAL_HISTORY_KEY,
                "[]"
        );
    }

    public String getHistory() {

        return preferences.getString(
                HISTORY_KEY,
                "[]"
        );
    }

    // =========================================================
    // LAST RESULTS
    // =========================================================

    public String getLastResult() {

        String result =
                memoryManager.getMemory(
                        "__last_internal_evolution_result__"
                );

        if (isBlank(result)) {

            result =
                    memoryManager.getMemory(
                            "__last_code_evolution_result__"
                    );
        }

        if (isBlank(result)) {
            return "ما كاين حتى نتيجة تطور محفوظة.";
        }

        return result;
    }

    public String getLastInternalEvolution() {

        String goal =
                memoryManager.getMemory(
                        "__last_internal_evolution_goal__"
                );

        String domain =
                memoryManager.getMemory(
                        "__last_internal_evolution_domain__"
                );

        String result =
                memoryManager.getMemory(
                        "__last_internal_evolution_result__"
                );

        return
                "LAST INTERNAL EVOLUTION\n\n"
                        + "Goal: "
                        + safeText(goal)
                        + "\n"
                        + "Domain: "
                        + safeText(domain)
                        + "\n"
                        + "Result: "
                        + safeText(result);
    }

    public String getLastEvolutionFile() {

        String path =
                memoryManager.getMemory(
                        "__last_code_evolution_file__"
                );

        if (isBlank(path)) {
            return "ما كاين حتى ملف تطور محفوظ.";
        }

        return path;
    }

    // =========================================================
    // DOMAIN DETECTION
    // =========================================================

    private String detectInternalDomain(
            String goal
    ) {

        String value =
                normalize(
                        goal
                );

        if (containsAny(
                value,
                "تعلم",
                "learn",
                "knowledge",
                "معرف",
                "معلوم"
        )) {
            return "learning";
        }

        if (containsAny(
                value,
                "ذاكر",
                "memory",
                "تذكر",
                "نسيان",
                "remember"
        )) {
            return "memory";
        }

        if (containsAny(
                value,
                "مهار",
                "skill",
                "قدرات",
                "capability"
        )) {
            return "skills";
        }

        if (containsAny(
                value,
                "مهم",
                "task",
                "تنظيم",
                "planning",
                "plan"
        )) {
            return "planning";
        }

        if (containsAny(
                value,
                "صوت",
                "voice",
                "كلام",
                "نطق"
        )) {
            return "voice";
        }

        if (containsAny(
                value,
                "هاتف",
                "android",
                "تطبيق",
                "app",
                "phone"
        )) {
            return "android";
        }

        if (containsAny(
                value,
                "كود",
                "code",
                "برمج",
                "تطوير",
                "java"
        )) {
            return "code";
        }

        return "general";
    }

    private String buildSkillName(
            String domain
    ) {

        return
                "Autonomous "
                        + capitalize(domain)
                        + " Evolution";
    }

    private String buildCapabilityName(
            String domain
    ) {

        return
                "autonomous_"
                        + domain
                        + "_evolution";
    }

    private String buildEvolutionDescription(
            String domain,
            String goal
    ) {

        return
                "قدرة تطور ذاتي في مجال "
                        + domain
                        + ". الهدف الأخير: "
                        + goal;
    }

    // =========================================================
    // EVOLUTION COUNT
    // =========================================================

    private int getEvolutionCount(
            String domain
    ) {

        String value =
                memoryManager.getMemory(
                        "__evolution_rule__"
                                + domain
                );

        if (isBlank(value)) {
            return 0;
        }

        String marker =
                "count=";

        int index =
                value.indexOf(marker);

        if (index < 0) {
            return 0;
        }

        String number =
                value.substring(
                        index
                                + marker.length()
                );

        int end =
                number.indexOf("\n");

        if (end >= 0) {

            number =
                    number.substring(
                            0,
                            end
                    );
        }

        try {

            return Integer.parseInt(
                    number.trim()
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // INTERNAL HISTORY
    // =========================================================

    private synchronized void recordInternalHistory(
            String domain,
            String goal,
            boolean skillAdded,
            boolean capabilityAdded
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            preferences.getString(
                                    INTERNAL_HISTORY_KEY,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "domain",
                    domain
            );

            item.put(
                    "goal",
                    goal
            );

            item.put(
                    "skillAdded",
                    skillAdded
            );

            item.put(
                    "capabilityAdded",
                    capabilityAdded
            );

            item.put(
                    "time",
                    System.currentTimeMillis()
            );

            history.put(item);

            while (history.length() > 100) {
                history.remove(0);
            }

            preferences.edit()
                    .putString(
                            INTERNAL_HISTORY_KEY,
                            history.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // GENERAL HISTORY
    // =========================================================

    private synchronized void recordHistory(
            String type,
            String message
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            preferences.getString(
                                    HISTORY_KEY,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "type",
                    type
            );

            item.put(
                    "message",
                    safeText(message)
            );

            item.put(
                    "time",
                    System.currentTimeMillis()
            );

            history.put(item);

            while (history.length() > 200) {
                history.remove(0);
            }

            preferences.edit()
                    .putString(
                            HISTORY_KEY,
                            history.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // MEMORY
    // =========================================================

    private void saveMemory(
            String key,
            String value
    ) {

        try {

            memoryManager.saveMemory(
                    key,
                    safeText(value)
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SNAPSHOT ID
    // =========================================================

    private String extractSnapshotId(
            String value
    ) {

        if (isBlank(value)) {
            return "";
        }

        String[] markers = {
                "ID: ",
                "Snapshot ID: ",
                "snapshot="
        };

        for (String marker : markers) {

            int index =
                    value.indexOf(marker);

            if (index < 0) {
                continue;
            }

            String result =
                    value.substring(
                            index
                                    + marker.length()
                    );

            int end =
                    result.indexOf("\n");

            if (end >= 0) {

                result =
                        result.substring(
                                0,
                                end
                        );
            }

            result =
                    result.trim();

            if (!result.isEmpty()) {
                return result;
            }
        }

        return "";
    }

    // =========================================================
    // TEST RESULT
    // =========================================================

    private boolean testPassed(
            String result
    ) {

        if (isBlank(result)) {
            return false;
        }

        String value =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (value.contains(
                "critical failure"
        )) {
            return false;
        }

        if (value.contains(
                "tests failed"
        )) {
            return false;
        }

        if (value.contains(
                "failed: "
        )
                && !value.contains(
                "failed: 0"
        )) {
            return false;
        }

        if (value.contains(
                "error:"
        )) {
            return false;
        }

        return
                value.contains("passed")
                        || value.contains("success")
                        || value.contains("online")
                        || value.contains("healthy")
                        || value.contains("✓");
    }

    // =========================================================
    // SUCCESS RESULT
    // =========================================================

    private boolean isSuccess(
            String result
    ) {

        if (isBlank(result)) {
            return false;
        }

        String value =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (value.contains("failed")) {
            return false;
        }

        if (value.contains("failure")) {
            return false;
        }

        if (value.contains("error")) {
            return false;
        }

        if (value.contains("فشل")) {
            return false;
        }

        if (value.contains("خطأ")) {
            return false;
        }

        return
                value.contains("success")
                        || value.contains("successful")
                        || value.contains("created")
                        || value.contains("جاهز")
                        || value.contains("تم ")
                        || value.contains("✓");
    }

    // =========================================================
    // HEALTH HELPERS
    // =========================================================

    private boolean safeHealth(
            SelfBuilderEngine engine
    ) {

        try {
            return engine != null
                    && engine.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeHealth(
            SelfTestEngine engine
    ) {

        try {
            return engine != null
                    && engine.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeHealth(
            CodeEvolutionEngine engine
    ) {

        try {
            return engine != null
                    && engine.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeHealth(
            ApkBuilderEngine engine
    ) {

        try {
            return engine != null
                    && engine.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // COUNTERS
    // =========================================================

    private int safeSkillCount() {

        try {
            return skillManager.getSkillCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeCapabilityCount() {

        try {
            return capabilityManager.getCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeMemoryCount() {

        try {
            return memoryManager.getMemoryCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private int safePendingTasks() {

        try {
            return taskManager.getPendingTaskCount();
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private String normalizeGoal(
            String goal
    ) {

        if (isBlank(goal)) {
            return "تحسين JARVIS";
        }

        return goal.trim();
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "أ",
                        "ا"
                )
                .replace(
                        "إ",
                        "ا"
                )
                .replace(
                        "آ",
                        "ا"
                )
                .replace(
                        "ة",
                        "ه"
                );
    }

    private boolean containsAny(
            String value,
            String... words
    ) {

        if (value == null) {
            return false;
        }

        String normalized =
                normalize(value);

        for (String word : words) {

            if (normalized.contains(
                    normalize(word)
            )) {
                return true;
            }
        }

        return false;
    }

    private String capitalize(
            String value
    ) {

        if (isBlank(value)) {
            return "General";
        }

        String clean =
                value.trim();

        return
                Character.toUpperCase(
                        clean.charAt(0)
                )
                        + clean.substring(1);
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String safeText(
            String value
    ) {

        if (isBlank(value)) {
            return "unspecified";
        }

        return value.trim();
    }

    private String safeError(
            Exception e
    ) {

        if (e == null) {
            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (isBlank(message)) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {
        return context;
    }
}