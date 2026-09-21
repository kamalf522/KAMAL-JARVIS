package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * JARVIS Autonomous Evolution Engine
 *
 * نظام التطور المستقل ديال JARVIS.
 *
 * عندو جوج مسارات:
 *
 * 1) INTERNAL EVOLUTION
 *    تطوير مباشر بلا APK:
 *    - Memory
 *    - Learning
 *    - Skills
 *    - Capabilities
 *    - Tasks
 *    - Evolution Rules
 *
 * 2) CODE EVOLUTION
 *    تغيير حقيقي في ملفات المصدر:
 *    Analyze
 *    -> Test
 *    -> Snapshot
 *    -> Modify
 *    -> Verify
 *    -> Test
 *    -> Rollback عند الفشل
 *    -> Build Request
 *
 * مهم:
 * Internal Evolution كتغير المعرفة والسلوك المسجل داخل JARVIS
 * مباشرة، أما تغيير Java/XML التنفيذي فيحتاج Build/Update للتطبيق.
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

        this.preferences =
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
                "Engine: READY\n"
        );

        result.append(
                "Internal Evolution: "
        )
                .append(
                        isInternalEvolutionHealthy()
                                ? "READY"
                                : "NOT READY"
                )
                .append("\n");

        result.append(
                "Self Builder: "
        )
                .append(
                        selfBuilderEngine.isHealthy()
                                ? "READY"
                                : "NOT READY"
                )
                .append("\n");

        result.append(
                "Code Evolution: "
        )
                .append(
                        codeEvolutionEngine.isHealthy()
                                ? "READY"
                                : "NOT READY"
                )
                .append("\n");

        result.append(
                "Self Test: "
        )
                .append(
                        selfTestEngine.isHealthy()
                                ? "READY"
                                : "NOT READY"
                )
                .append("\n");

        result.append(
                "APK Builder: "
        )
                .append(
                        apkBuilderEngine.isHealthy()
                                ? "READY"
                                : "READY / BUILD ENVIRONMENT NEEDED"
                )
                .append("\n");

        result.append(
                "\nSkills: "
        )
                .append(
                        skillManager.getSkillCount()
                )
                .append("\n");

        result.append(
                "Capabilities: "
        )
                .append(
                        capabilityManager.getCount()
                )
                .append("\n");

        result.append(
                "Memory: "
        )
                .append(
                        memoryManager.getMemoryCount()
                )
                .append("\n");

        result.append(
                "Pending Tasks: "
        )
                .append(
                        taskManager.getPendingTaskCount()
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

        return selfBuilderEngine != null
                && selfTestEngine != null
                && codeEvolutionEngine != null
                && diagnosisManager != null
                && skillManager != null
                && capabilityManager != null
                && learningEngine != null
                && taskManager != null
                && memoryManager != null;
    }

    // =========================================================
    // INTERNAL EVOLUTION STATUS
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
                    "Internal Evolution: ERROR ⚠";
        }

        return
                "INTERNAL EVOLUTION: ONLINE ✓\n"
                        + "Skills: "
                        + skillManager.getSkillCount()
                        + "\n"
                        + "Capabilities: "
                        + capabilityManager.getCount()
                        + "\n"
                        + "Memory: "
                        + memoryManager.getMemoryCount()
                        + "\n"
                        + "Pending Tasks: "
                        + taskManager.getPendingTaskCount();
    }

    // =========================================================
    // ANALYZE BEFORE EVOLUTION
    // =========================================================

    public String analyzeBeforeEvolution(
            String goal
    ) {

        try {

            if (goal == null ||
                    goal.trim().isEmpty()) {

                goal =
                        "تحسين JARVIS";
            }

            String diagnosis =
                    diagnosisManager.runDiagnosis();

            String project =
                    codeEvolutionEngine.analyzeProject();

            String plan =
                    codeEvolutionEngine
                            .generateDevelopmentPlan(
                                    goal
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
                    goal
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

            saveMemory(
                    "__autonomous_last_goal__",
                    goal
            );

            return result.toString();

        } catch (Exception e) {

            return
                    "Evolution Analysis Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // PREPARE EVOLUTION
    // =========================================================

    public String prepareEvolution(
            String goal
    ) {

        try {

            if (goal == null ||
                    goal.trim().isEmpty()) {

                goal =
                        "تحسين JARVIS";
            }

            String analysis =
                    analyzeBeforeEvolution(
                            goal
                    );

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "قبل التطوير المستقل: "
                                    + goal
                    );

            saveMemory(
                    "__autonomous_last_snapshot__",
                    snapshot
            );

            recordHistory(
                    "PREPARE",
                    goal
                            + " | "
                            + snapshot
            );

            return
                    "EVOLUTION PREPARED ✓\n\n"
                            + analysis
                            + "\n\n"
                            + "SNAPSHOT:\n"
                            + snapshot;

        } catch (Exception e) {

            return
                    "Evolution Preparation Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // INTERNAL AUTONOMOUS EVOLUTION
    // =========================================================

    public String runInternalEvolution(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            goal =
                    "تحسين JARVIS";
        }

        String cleanGoal =
                goal.trim();

        try {

            // -------------------------------------------------
            // 1. System test before evolution
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 2. Determine evolution domain
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 3. Register capability
            // -------------------------------------------------

            boolean capabilityAdded =
                    capabilityManager.addCapability(
                            capabilityName,
                            description
                    );

            // -------------------------------------------------
            // 4. Register skill
            // -------------------------------------------------

            boolean skillAdded =
                    skillManager.addSkill(
                            skillName,
                            description
                    );

            // -------------------------------------------------
            // 5. Teach JARVIS the evolution knowledge
            // -------------------------------------------------

            String learningSubject =
                    "evolution_" + domain;

            String learningInformation =
                    "الهدف: "
                            + cleanGoal
                            + "\n"
                            + "المجال: "
                            + domain
                            + "\n"
                            + "القاعدة: "
                            + description
                            + "\n"
                            + "تم تطوير هذا المجال داخليا بدون APK.";

            String learningResult =
                    learningEngine.learn(
                            learningSubject,
                            learningInformation
                    );

            // -------------------------------------------------
            // 6. Save permanent evolution rule
            // -------------------------------------------------

            String ruleKey =
                    "__evolution_rule__"
                            + domain;

            String oldRule =
                    memoryManager.getMemory(
                            ruleKey
                    );

            int evolutionCount =
                    getEvolutionCount(
                            domain
                    ) + 1;

            String rule =
                    "domain="
                            + domain
                            + "\n"
                            + "count="
                            + evolutionCount
                            + "\n"
                            + "last_goal="
                            + cleanGoal
                            + "\n"
                            + "last_update="
                            + System.currentTimeMillis()
                            + "\n"
                            + "previous="
                            + oldRule;

            memoryManager.saveMemory(
                    ruleKey,
                    rule
            );

            // -------------------------------------------------
            // 7. Create development task
            // -------------------------------------------------

            String taskTitle =
                    "تطوير JARVIS: "
                            + domain;

            String taskResult =
                    taskManager.addTask(
                            taskTitle
                    );

            // -------------------------------------------------
            // 8. Mark skill/capability as successful
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 9. Save evolution state
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 10. History
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 11. Final verification
            // -------------------------------------------------

            String finalTests =
                    selfTestEngine.runAllTests();

            if (!testPassed(finalTests)) {

                return
                        "INTERNAL EVOLUTION تمت ولكن الاختبار النهائي فيه مشكل ⚠\n\n"
                                + finalTests;
            }

            return
                    "INTERNAL EVOLUTION SUCCESS ✓\n\n"
                            + "الهدف:\n"
                            + cleanGoal
                            + "\n\n"
                            + "المجال:\n"
                            + domain
                            + "\n\n"
                            + "Skill:\n"
                            + skillName
                            + (skillAdded
                            ? " ← جديدة ✓"
                            : " ← موجودة ومحدثة")
                            + "\n\n"
                            + "Capability:\n"
                            + capabilityName
                            + (capabilityAdded
                            ? " ← جديدة ✓"
                            : " ← موجودة")
                            + "\n\n"
                            + "Learning:\n"
                            + "تم حفظ المعرفة ✓"
                            + "\n\n"
                            + "Evolution Rule:\n"
                            + "تم تحديث القاعدة الداخلية ✓"
                            + "\n\n"
                            + "Task:\n"
                            + taskResult
                            + "\n\n"
                            + "APK:\n"
                            + "ما محتاجش APK جديد لهذا التطور ✓"
                            + "\n\n"
                            + "System Test:\n"
                            + "PASSED ✓";

        } catch (Exception e) {

            recordHistory(
                    "INTERNAL_FAILED",
                    cleanGoal
                            + " | "
                            + safeError(e)
            );

            return
                    "Internal Evolution Failed ⚠\n\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // DOMAIN DETECTION
    // =========================================================

    private String detectInternalDomain(
            String goal
    ) {

        String value =
                goal.toLowerCase(
                        Locale.ROOT
                );

        if (containsAny(
                value,
                "تعلم",
                "learning",
                "learn",
                "معرف",
                "معلومة"
        )) {

            return "learning";
        }

        if (containsAny(
                value,
                "ذاكرة",
                "memory",
                "تذكر"
        )) {

            return "memory";
        }

        if (containsAny(
                value,
                "مهار",
                "skill"
        )) {

            return "skills";
        }

        if (containsAny(
                value,
                "قدر",
                "capabil",
                "ability"
        )) {

            return "capabilities";
        }

        if (containsAny(
                value,
                "مهم",
                "task",
                "plan",
                "خطة"
        )) {

            return "planning";
        }

        if (containsAny(
                value,
                "إشعار",
                "اشعار",
                "notification"
        )) {

            return "notifications";
        }

        if (containsAny(
                value,
                "شاشة",
                "screen"
        )) {

            return "screen";
        }

        if (containsAny(
                value,
                "android",
                "هاتف",
                "تطبيق",
                "settings"
        )) {

            return "android";
        }

        if (containsAny(
                value,
                "صوت",
                "voice",
                "كلام"
        )) {

            return "voice";
        }

        if (containsAny(
                value,
                "تذكير",
                "reminder"
        )) {

            return "reminders";
        }

        if (containsAny(
                value,
                "ذكاء",
                "intelligence",
                "decision",
                "قرار"
        )) {

            return "intelligence";
        }

        return "general";
    }

    private String buildSkillName(
            String domain
    ) {

        return
                "autonomous_"
                        + domain
                        + "_evolution";
    }

    private String buildCapabilityName(
            String domain
    ) {

        return
                "autonomous_"
                        + domain
                        + "_capability";
    }

    private String buildEvolutionDescription(
            String domain,
            String goal
    ) {

        return
                "قدرة داخلية تطورت تلقائيا في مجال "
                        + domain
                        + ". الهدف الحالي: "
                        + goal;
    }

    private int getEvolutionCount(
            String domain
    ) {

        try {

            String value =
                    memoryManager.getMemory(
                            "__evolution_rule__"
                                    + domain
                    );

            if (value == null ||
                    value.trim().isEmpty()) {

                return 0;
            }

            String marker =
                    "count=";

            int index =
                    value.indexOf(
                            marker
                    );

            if (index < 0) {
                return 0;
            }

            String number =
                    value.substring(
                            index + marker.length()
                    );

            int newline =
                    number.indexOf(
                            '\n'
                    );

            if (newline >= 0) {

                number =
                        number.substring(
                                0,
                                newline
                        );
            }

            return Integer.parseInt(
                    number.trim()
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // SAFE SOURCE EVOLUTION
    // =========================================================

    public String evolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        if (path == null ||
                path.trim().isEmpty()) {

            return
                    "Evolution مرفوض: path فارغ.";
        }

        if (oldText == null ||
                oldText.isEmpty()) {

            return
                    "Evolution مرفوض: oldText فارغ.";
        }

        if (newText == null) {
            newText = "";
        }

        if (reason == null ||
                reason.trim().isEmpty()) {

            reason =
                    "Autonomous code evolution";
        }

        try {

            if (!selfBuilderEngine.fileExists(
                    path
            )) {

                return
                        "Evolution مرفوض:\n"
                                + "الملف غير موجود في Workspace:\n"
                                + path;
            }

            String baseline =
                    selfTestEngine.runAllTests();

            if (!testPassed(baseline)) {

                recordHistory(
                        "BASELINE_FAILED",
                        path
                );

                return
                        "Evolution توقف ⚠\n\n"
                                + "Baseline tests فاشلين.\n\n"
                                + baseline;
            }

            String snapshotResult =
                    selfBuilderEngine.createSnapshot(
                            "قبل Autonomous Evolution: "
                                    + reason
                    );

            String snapshotId =
                    extractSnapshotId(
                            snapshotResult
                    );

            if (snapshotId == null) {

                return
                        "Evolution توقف ⚠\n\n"
                                + "فشل إنشاء Snapshot.\n"
                                + snapshotResult;
            }

            String evolutionResult =
                    selfBuilderEngine.evolveSourceFile(
                            path,
                            oldText,
                            newText,
                            reason
                    );

            if (evolutionResult == null ||
                    !evolutionResult.contains(
                            "EVOLUTION SUCCESS"
                    )) {

                recordHistory(
                        "MODIFICATION_FAILED",
                        path
                                + " | "
                                + evolutionResult
                );

                return
                        "Evolution فشل ⚠\n\n"
                                + evolutionResult
                                + "\n\n"
                                + "Snapshot محفوظ:\n"
                                + snapshotId;
            }

            String after =
                    selfBuilderEngine.readSourceFile(
                            path
                    );

            if (after == null ||
                    after.trim().isEmpty()) {

                String rollback =
                        selfBuilderEngine.rollback(
                                snapshotId
                        );

                recordHistory(
                        "ROLLBACK_EMPTY_FILE",
                        path
                );

                return
                        "Evolution فشل.\n\n"
                                + "الملف أصبح فارغا.\n\n"
                                + "ROLLBACK:\n"
                                + rollback;
            }

            String afterTests =
                    selfTestEngine.runAllTests();

            if (!testPassed(afterTests)) {

                String rollback =
                        selfBuilderEngine.rollback(
                                snapshotId
                        );

                recordHistory(
                        "ROLLBACK_TEST_FAILED",
                        path
                );

                String recoveryTests =
                        selfTestEngine.runAllTests();

                return
                        "Evolution تم رفضه ⚠\n\n"
                                + "التعديل تسبب في فشل الاختبارات.\n\n"
                                + "ROLLBACK:\n"
                                + rollback
                                + "\n\n"
                                + "TEST AFTER ROLLBACK:\n"
                                + recoveryTests;
            }

            String buildRequest =
                    apkBuilderEngine.prepareBuildRequest(
                            "Successful autonomous evolution: "
                                    + reason
                    );

            saveMemory(
                    "__last_autonomous_evolution_file__",
                    path
            );

            saveMemory(
                    "__last_autonomous_evolution_reason__",
                    reason
            );

            saveMemory(
                    "__last_autonomous_evolution_snapshot__",
                    snapshotId
            );

            saveMemory(
                    "__last_autonomous_evolution_result__",
                    "SUCCESS"
            );

            recordHistory(
                    "SUCCESS",
                    path
                            + " | reason="
                            + reason
                            + " | snapshot="
                            + snapshotId
            );

            return
                    "AUTONOMOUS EVOLUTION SUCCESS ✓\n\n"
                            + "File:\n"
                            + path
                            + "\n\n"
                            + "Reason:\n"
                            + reason
                            + "\n\n"
                            + "Snapshot:\n"
                            + snapshotId
                            + "\n\n"
                            + "System Tests:\n"
                            + "PASSED ✓"
                            + "\n\n"
                            + "APK BUILD REQUEST:\n"
                            + buildRequest;

        } catch (Exception e) {

            return
                    "Autonomous Evolution Failed ⚠\n\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // CREATE NEW SOURCE FILE SAFELY
    // =========================================================

    public String createSourceFile(
            String path,
            String content,
            String reason
    ) {

        if (path == null ||
                path.trim().isEmpty()) {

            return
                    "إنشاء الملف مرفوض: path فارغ.";
        }

        if (content == null) {
            content = "";
        }

        if (reason == null ||
                reason.trim().isEmpty()) {

            reason =
                    "Autonomous new capability";
        }

        try {

            String baseline =
                    selfTestEngine.runAllTests();

            if (!testPassed(baseline)) {

                return
                        "إنشاء الملف توقف.\n\n"
                                + "Baseline tests failed.\n\n"
                                + baseline;
            }

            if (selfBuilderEngine.fileExists(
                    path
            )) {

                return
                        "إنشاء الملف مرفوض.\n\n"
                                + "الملف موجود أصلا:\n"
                                + path;
            }

            String snapshotResult =
                    selfBuilderEngine.createSnapshot(
                            "قبل إنشاء ملف جديد: "
                                    + reason
                    );

            String snapshotId =
                    extractSnapshotId(
                            snapshotResult
                    );

            if (snapshotId == null) {

                return
                        "فشل Snapshot:\n"
                                + snapshotResult;
            }

            String writeResult =
                    selfBuilderEngine.writeSourceFile(
                            path,
                            content
                    );

            if (writeResult == null ||
                    !writeResult.contains(
                            "تم تعديل الملف"
                    )) {

                selfBuilderEngine.rollback(
                        snapshotId
                );

                return
                        "فشل إنشاء الملف.\n\n"
                                + writeResult;
            }

            String tests =
                    selfTestEngine.runAllTests();

            if (!testPassed(tests)) {

                String rollback =
                        selfBuilderEngine.rollback(
                                snapshotId
                        );

                return
                        "الملف الجديد سبب فشل الاختبارات.\n\n"
                                + "ROLLBACK:\n"
                                + rollback;
            }

            String buildRequest =
                    apkBuilderEngine.prepareBuildRequest(
                            "New source file: "
                                    + path
                    );

            saveMemory(
                    "__last_created_source_file__",
                    path
            );

            recordHistory(
                    "CREATE_SUCCESS",
                    path
                            + " | "
                            + reason
            );

            return
                    "NEW SOURCE FILE CREATED ✓\n\n"
                            + "File:\n"
                            + path
                            + "\n\n"
                            + "Snapshot:\n"
                            + snapshotId
                            + "\n\n"
                            + "Tests: PASSED ✓\n\n"
                            + "Build:\n"
                            + buildRequest;

        } catch (Exception e) {

            return
                    "Create Source Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // FULL EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle(
            String goal
    ) {

        try {

            if (goal == null ||
                    goal.trim().isEmpty()) {

                goal =
                        diagnosisManager
                                .getNextDevelopmentTarget();
            }

            String cleanGoal =
                    goal.trim();

            /*
             * أولوية التطور الداخلي.
             *
             * ما دام الهدف يقدر يتحقق بالمعرفة،
             * المهارات، القدرات، الذاكرة أو التخطيط،
             * ما كاين حتى سبب نبنيو APK جديد.
             */

            if (canUseInternalEvolution(
                    cleanGoal
            )) {

                return runInternalEvolution(
                        cleanGoal
                );
            }

            /*
             * إلا كان الهدف كيتطلب تغيير حقيقي
             * في كود التطبيق، كنرجعو للمسار الآمن.
             */

            String analysis =
                    analyzeBeforeEvolution(
                            cleanGoal
                    );

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "Full autonomous cycle: "
                                    + cleanGoal
                    );

            String snapshotId =
                    extractSnapshotId(
                            snapshot
                    );

            if (snapshotId == null) {

                return
                        "Evolution Cycle توقف.\n\n"
                                + snapshot;
            }

            String tests =
                    selfTestEngine.runAllTests();

            if (!testPassed(tests)) {

                return
                        "Evolution Cycle توقف.\n\n"
                                + "النظام محتاج إصلاح قبل تعديل الكود.\n\n"
                                + tests
                                + "\n\nSnapshot:\n"
                                + snapshotId;
            }

            String build =
                    apkBuilderEngine.prepareBuildRequest(
                            "Autonomous code cycle: "
                                    + cleanGoal
                    );

            recordHistory(
                    "CODE_CYCLE_PREPARED",
                    cleanGoal
                            + " | snapshot="
                            + snapshotId
            );

            return
                    "AUTONOMOUS CODE EVOLUTION READY ✓\n\n"
                            + "GOAL:\n"
                            + cleanGoal
                            + "\n\n"
                            + "ANALYSIS:\n"
                            + analysis
                            + "\n\n"
                            + "SNAPSHOT:\n"
                            + snapshotId
                            + "\n\n"
                            + "BASELINE TEST:\n"
                            + "PASSED ✓"
                            + "\n\n"
                            + "BUILD REQUEST:\n"
                            + build
                            + "\n\n"
                            + "ملاحظة:\n"
                            + "هاد النوع محتاج تغيير حقيقي في كود التطبيق، "
                            + "لذلك APK جديد غادي يكون مطلوب من بعد.";

        } catch (Exception e) {

            return
                    "Evolution Cycle Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // DECIDE INTERNAL OR CODE EVOLUTION
    // =========================================================

    private boolean canUseInternalEvolution(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return true;
        }

        String value =
                goal.toLowerCase(
                        Locale.ROOT
                );

        /*
         * هاد المجالات نقدروا نطوروها مباشرة
         * بلا تغيير executable code.
         */

        if (containsAny(
                value,
                "تعلم",
                "learn",
                "learning",
                "معرف",
                "معلومة",
                "ذاكرة",
                "memory",
                "تذكر",
                "مهار",
                "skill",
                "قدر",
                "capability",
                "planning",
                "plan",
                "خطة",
                "مهم",
                "task",
                "decision",
                "قرار",
                "ذكاء",
                "intelligence",
                "تطور داخلي",
                "تعلم راسو",
                "طور راسك",
                "طور نفسك",
                "طور جارفيس",
                "طور جارڤيس",
                "evolve yourself",
                "self improve",
                "self improvement"
        )) {

            return true;
        }

        /*
         * هاد الكلمات غالبا كتدل على تغيير
         * تنفيذي داخل التطبيق.
         */

        if (containsAny(
                value,
                "غير الكود",
                "عدل الكود",
                "عدّل الكود",
                "source code",
                "java",
                "xml",
                "gradle",
                "apk",
                "build apk",
                "أنشئ ملف",
                "انشئ ملف",
                "create source",
                "modify source",
                "برمج",
                "كود جديد"
        )) {

            return false;
        }

        /*
         * الافتراضي:
         * نبدأ بالتطور الداخلي لأنه أكثر أمانا
         * وما كيحتاجش APK.
         */

        return true;
    }

    // =========================================================
    // INTERNAL HISTORY
    // =========================================================

    private void recordInternalHistory(
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
                    "skill_added",
                    skillAdded
            );

            item.put(
                    "capability_added",
                    capabilityAdded
            );

            item.put(
                    "time",
                    System.currentTimeMillis()
            );

            history.put(item);

            while (
                    history.length() > 100
            ) {

                JSONArray trimmed =
                        new JSONArray();

                for (
                        int i = 1;
                        i < history.length();
                        i++
                ) {

                    trimmed.put(
                            history.get(i)
                    );
                }

                history =
                        trimmed;
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

    public String getInternalHistory() {

        return preferences.getString(
                INTERNAL_HISTORY_KEY,
                "لا توجد تطورات داخلية بعد."
        );
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

        String skill =
                memoryManager.getMemory(
                        "__last_internal_evolution_skill__"
                );

        String capability =
                memoryManager.getMemory(
                        "__last_internal_evolution_capability__"
                );

        if (goal == null ||
                goal.trim().isEmpty()) {

            return
                    "مازال ما دار JARVIS حتى Internal Evolution.";
        }

        return
                "LAST INTERNAL EVOLUTION\n\n"
                        + "Goal: "
                        + goal
                        + "\n"
                        + "Domain: "
                        + domain
                        + "\n"
                        + "Skill: "
                        + skill
                        + "\n"
                        + "Capability: "
                        + capability;
    }

    // =========================================================
    // ROLLBACK LAST EVOLUTION
    // =========================================================

    public String rollbackLastEvolution() {

        String snapshot =
                memoryManager.getMemory(
                        "__last_autonomous_evolution_snapshot__"
                );

        if (snapshot == null ||
                snapshot.trim().isEmpty()) {

            snapshot =
                    memoryManager.getMemory(
                            "__autonomous_last_snapshot__"
                    );
        }

        String snapshotId =
                extractSnapshotId(
                        snapshot
                );

        if (snapshotId == null &&
                snapshot != null) {

            snapshotId =
                    snapshot.trim();
        }

        if (snapshotId == null ||
                snapshotId.isEmpty()) {

            return
                    "ما عنديش Snapshot صالح للـ Rollback.";
        }

        String result =
                selfBuilderEngine.rollback(
                        snapshotId
                );

        recordHistory(
                "MANUAL_ROLLBACK",
                snapshotId
        );

        return result;
    }

    // =========================================================
    // GET HISTORY
    // =========================================================

    public String getHistory() {

        return preferences.getString(
                HISTORY_KEY,
                "لا توجد عمليات Evolution بعد."
        );
    }

    // =========================================================
    // LAST RESULT
    // =========================================================

    public String getLastResult() {

        return memoryManager.getMemory(
                "__last_autonomous_evolution_result__"
        );
    }

    // =========================================================
    // LAST FILE
    // =========================================================

    public String getLastEvolutionFile() {

        return memoryManager.getMemory(
                "__last_autonomous_evolution_file__"
        );
    }

    // =========================================================
    // SNAPSHOT ID EXTRACTION
    // =========================================================

    private String extractSnapshotId(
            String result
    ) {

        if (result == null) {
            return null;
        }

        String marker =
                "ID:";

        int index =
                result.indexOf(
                        marker
                );

        if (index < 0) {

            marker =
                    "ID: ";

            index =
                    result.indexOf(
                            marker
                    );
        }

        if (index < 0) {
            return null;
        }

        String value =
                result.substring(
                        index + marker.length()
                );

        int newline =
                value.indexOf(
                        '\n'
                );

        if (newline >= 0) {

            value =
                    value.substring(
                            0,
                            newline
                    );
        }

        return value.trim();
    }

    // =========================================================
    // TEST RESULT
    // =========================================================

    private boolean testPassed(
            String result
    ) {

        if (result == null ||
                result.trim().isEmpty()) {

            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (lower.contains("failed")
                || lower.contains("فشل")
                || lower.contains("error")
                || lower.contains("خطأ")) {

            return false;
        }

        return lower.contains("passed")
                || lower.contains("healthy")
                || lower.contains("نجاح")
                || lower.contains("سليم");
    }

    // =========================================================
    // TEXT MATCHING
    // =========================================================

    private boolean containsAny(
            String value,
            String... terms
    ) {

        if (value == null) {
            return false;
        }

        for (String term : terms) {

            if (term == null ||
                    term.trim().isEmpty()) {

                continue;
            }

            if (value.contains(
                    term.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // MEMORY
    // =========================================================

    private void saveMemory(
            String key,
            String value
    ) {

        if (key == null ||
                value == null) {

            return;
        }

        memoryManager.saveMemory(
                key,
                value
        );
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void recordHistory(
            String type,
            String details
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
                    "details",
                    details == null
                            ? ""
                            : details
            );

            item.put(
                    "time",
                    System.currentTimeMillis()
            );

            history.put(item);

            while (
                    history.length() > 100
            ) {

                JSONArray trimmed =
                        new JSONArray();

                for (
                        int i = 1;
                        i < history.length();
                        i++
                ) {

                    trimmed.put(
                            history.get(i)
                    );
                }

                history =
                        trimmed;
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
    // SAFE ERROR
    // =========================================================

    private String safeError(
            Exception e
    ) {

        if (e == null) {
            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }
}