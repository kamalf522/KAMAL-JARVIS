package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * JARVIS Autonomous Evolution Engine
 *
 * المسؤول على دورة التطور الذاتي:
 *
 * ANALYZE
 * -> PREPARE
 * -> INTERNAL EVOLUTION
 * -> CODE EVOLUTION
 * -> VERIFY
 * -> ROLLBACK عند الفشل
 * -> LEARN
 * -> HISTORY
 *
 * التطور الداخلي يقدر يخدم بلا APK جديد.
 * تعديل Java/XML الحقيقي يحتاج Build وتحديث التطبيق.
 */
public class AutonomousEvolutionEngine {

    private static final String PREFS =
            "JARVIS_AUTONOMOUS_EVOLUTION";

    private static final String HISTORY_KEY =
            "history";

    private static final String INTERNAL_HISTORY_KEY =
            "internal_history";

    private static final String LAST_RESULT_KEY =
            "last_result";

    private static final String LAST_GOAL_KEY =
            "last_goal";

    private static final String LAST_SNAPSHOT_KEY =
            "last_snapshot";

    private static final int MAX_HISTORY = 150;

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

    private final SharedPreferences preferences;

    public AutonomousEvolutionEngine(Context context) {

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
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS AUTONOMOUS EVOLUTION\n"
        );

        result.append(
                "==============================\n"
        );

        result.append(
                "Engine: "
        ).append(
                isHealthy()
                        ? "ONLINE ✓"
                        : "ATTENTION ⚠"
        ).append("\n");

        result.append(
                "Internal Evolution: "
        ).append(
                isInternalEvolutionHealthy()
                        ? "READY ✓"
                        : "CHECK ⚠"
        ).append("\n");

        result.append(
                "Self Builder: "
        ).append(
                safeHealth(
                        selfBuilderEngine
                )
                        ? "READY ✓"
                        : "CHECK ⚠"
        ).append("\n");

        result.append(
                "Code Evolution: "
        ).append(
                safeHealth(
                        codeEvolutionEngine
                )
                        ? "READY ✓"
                        : "CHECK ⚠"
        ).append("\n");

        result.append(
                "Self Test: "
        ).append(
                safeHealth(
                        selfTestEngine
                )
                        ? "READY ✓"
                        : "CHECK ⚠"
        ).append("\n");

        result.append(
                "APK Builder: "
        ).append(
                safeHealth(
                        apkBuilderEngine
                )
                        ? "READY ✓"
                        : "BUILD ENVIRONMENT NEEDED"
        ).append("\n\n");

        result.append(
                "Skills: "
        ).append(
                safeSkillCount()
        ).append("\n");

        result.append(
                "Capabilities: "
        ).append(
                safeCapabilityCount()
        ).append("\n");

        result.append(
                "Memory: "
        ).append(
                safeMemoryCount()
        ).append("\n");

        result.append(
                "Pending Tasks: "
        ).append(
                safePendingTasks()
        ).append("\n\n");

        result.append(
                "Workspace:\n"
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
                            + "\nالوصف: "
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

            int count =
                    getEvolutionCount(domain)
                            + 1;

            String rule =
                    "domain="
                            + domain
                            + "\ncount="
                            + count
                            + "\nlast_goal="
                            + cleanGoal
                            + "\ntime="
                            + now();

            memoryManager.saveMemory(
                    ruleKey,
                    rule
            );

            memoryManager.saveMemory(
                    "__last_internal_evolution__",
                    cleanGoal
            );

            recordInternalHistory(
                    domain,
                    cleanGoal,
                    skillAdded,
                    capabilityAdded,
                    learningResult
            );

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "INTERNAL EVOLUTION SUCCESS ✓\n\n"
            );

            result.append(
                    "Goal: "
            ).append(
                    cleanGoal
            ).append("\n");

            result.append(
                    "Domain: "
            ).append(
                    domain
            ).append("\n");

            result.append(
                    "Skill: "
            ).append(
                    skillAdded
                            ? "ADDED ✓"
                            : "EXISTS ✓"
            ).append("\n");

            result.append(
                    "Capability: "
            ).append(
                    capabilityAdded
                            ? "ADDED ✓"
                            : "EXISTS ✓"
            ).append("\n");

            result.append(
                    "Learning: "
            ).append(
                    safeText(learningResult)
            );

            recordHistory(
                    "INTERNAL_EVOLUTION_SUCCESS",
                    cleanGoal
            );

            return result.toString();

        } catch (Exception e) {

            recordHistory(
                    "INTERNAL_EVOLUTION_FAILED",
                    safeError(e)
            );

            return
                    "INTERNAL EVOLUTION FAILED:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // ANALYSIS
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

            return
                    "JARVIS EVOLUTION ANALYSIS\n\n"
                            + "GOAL:\n"
                            + cleanGoal
                            + "\n\nDIAGNOSIS:\n"
                            + diagnosis
                            + "\n\nPROJECT:\n"
                            + project
                            + "\n\nPLAN:\n"
                            + plan;

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
                    LAST_SNAPSHOT_KEY,
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
                    safeError(e)
            );

            return
                    "Evolution Preparation Failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // REAL SOURCE EVOLUTION
    // =========================================================

    public synchronized String evolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        try {

            String cleanReason =
                    isBlank(reason)
                            ? "Autonomous Evolution"
                            : reason.trim();

            String result =
                    selfBuilderEngine.evolveSourceFile(
                            path,
                            oldText,
                            newText,
                            cleanReason
                    );

            boolean success =
                    isSuccess(result);

            recordHistory(
                    success
                            ? "SOURCE_EVOLUTION_SUCCESS"
                            : "SOURCE_EVOLUTION_FAILED",
                    path
                            + " | "
                            + cleanReason
            );

            saveMemory(
                    LAST_RESULT_KEY,
                    result
            );

            if (success) {

                saveMemory(
                        "__last_evolution_file__",
                        path
                );

                saveMemory(
                        "__last_evolution_reason__",
                        cleanReason
                );
            }

            return result;

        } catch (Exception e) {

            String error =
                    "Evolution failed:\n"
                            + safeError(e);

            saveMemory(
                    LAST_RESULT_KEY,
                    error
            );

            recordHistory(
                    "SOURCE_EVOLUTION_ERROR",
                    error
            );

            return error;
        }
    }

    // =========================================================
    // CREATE SOURCE
    // =========================================================

    public synchronized String createSourceFile(
            String path,
            String content,
            String reason
    ) {

        try {

            if (isBlank(path)) {

                return
                        "إنشاء Source مرفوض: المسار فارغ.";
            }

            String cleanReason =
                    isBlank(reason)
                            ? "Autonomous Source Creation"
                            : reason.trim();

            String result =
                    selfBuilderEngine.writeSourceFile(
                            path.trim(),
                            content == null
                                    ? ""
                                    : content
                    );

            saveMemory(
                    LAST_RESULT_KEY,
                    result
            );

            recordHistory(
                    isSuccess(result)
                            ? "SOURCE_CREATE_SUCCESS"
                            : "SOURCE_CREATE_FAILED",
                    path
            );

            return result;

        } catch (Exception e) {

            String error =
                    "Source creation failed:\n"
                            + safeError(e);

            saveMemory(
                    LAST_RESULT_KEY,
                    error
            );

            return error;
        }
    }

    // =========================================================
    // MAIN AUTONOMOUS CYCLE
    // =========================================================

    public synchronized String runEvolutionCycle(
            String goal
    ) {

        String cleanGoal =
                normalizeGoal(goal);

        if (cleanGoal.isEmpty()) {

            cleanGoal =
                    "تحليل JARVIS والبحث عن أقرب تطوير آمن";
        }

        try {

            saveMemory(
                    LAST_GOAL_KEY,
                    cleanGoal
            );

            recordHistory(
                    "CYCLE_STARTED",
                    cleanGoal
            );

            /*
             * المرحلة 1:
             * تحليل الحالة الحالية.
             */
            String analysis =
                    analyzeBeforeEvolution(
                            cleanGoal
                    );

            /*
             * المرحلة 2:
             * التطور الداخلي.
             * هذا المسار لا يحتاج APK.
             */
            String internal =
                    runInternalEvolution(
                            cleanGoal
                    );

            /*
             * المرحلة 3:
             * Snapshot قبل أي تغيير Source.
             */
            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "Autonomous Cycle: "
                                    + cleanGoal
                    );

            saveMemory(
                    LAST_SNAPSHOT_KEY,
                    snapshot
            );

            /*
             * المرحلة 4:
             * Self Test بعد التطور الداخلي.
             */
            String verification =
                    selfTestEngine.runAllTests();

            boolean passed =
                    testPassed(
                            verification
                    );

            if (!passed) {

                String rollback =
                        selfBuilderEngine.rollback(
                                snapshot
                        );

                String result =
                        "AUTONOMOUS EVOLUTION STOPPED ⚠\n\n"
                                + "Goal:\n"
                                + cleanGoal
                                + "\n\n"
                                + "Reason: Self Test failed.\n\n"
                                + verification
                                + "\n\n"
                                + "Rollback:\n"
                                + rollback;

                saveMemory(
                        LAST_RESULT_KEY,
                        result
                );

                recordHistory(
                        "CYCLE_ROLLED_BACK",
                        cleanGoal
                );

                return result;
            }

            String result =
                    "AUTONOMOUS EVOLUTION READY ✓\n\n"
                            + "Goal:\n"
                            + cleanGoal
                            + "\n\n"
                            + "Analysis:\n"
                            + analysis
                            + "\n\n"
                            + "Internal Evolution:\n"
                            + internal
                            + "\n\n"
                            + "Verification:\n"
                            + verification
                            + "\n\n"
                            + "Snapshot:\n"
                            + snapshot
                            + "\n\n"
                            + "Source Evolution:\n"
                            + "READY — awaiting a verified source change.";

            saveMemory(
                    LAST_RESULT_KEY,
                    result
            );

            recordHistory(
                    "CYCLE_SUCCESS",
                    cleanGoal
            );

            return result;

        } catch (Exception e) {

            String error =
                    "AUTONOMOUS EVOLUTION FAILED:\n"
                            + safeError(e);

            saveMemory(
                    LAST_RESULT_KEY,
                    error
            );

            recordHistory(
                    "CYCLE_FAILED",
                    error
            );

            return error;
        }
    }

    // =========================================================
    // ROLLBACK
    // =========================================================

    public synchronized String rollbackLastEvolution() {

        try {

            String snapshot =
                    preferences.getString(
                            LAST_SNAPSHOT_KEY,
                            ""
                    );

            if (isBlank(snapshot)) {

                snapshot =
                        memoryManager.getMemory(
                                "__last_evolution_snapshot__"
                        );
            }

            if (isBlank(snapshot)) {

                return
                        "ما كاين حتى Snapshot صالح للتراجع.";
            }

            String result =
                    selfBuilderEngine.rollback(
                            snapshot
                    );

            saveMemory(
                    LAST_RESULT_KEY,
                    result
            );

            recordHistory(
                    "MANUAL_ROLLBACK",
                    snapshot
            );

            return result;

        } catch (Exception e) {

            return
                    "Rollback failed:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // LAST RESULT
    // =========================================================

    public String getLastResult() {

        return preferences.getString(
                LAST_RESULT_KEY,
                "لا توجد نتيجة Evolution حتى الآن."
        );
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public synchronized String getHistory() {

        try {

            JSONArray history =
                    new JSONArray(
                            preferences.getString(
                                    HISTORY_KEY,
                                    "[]"
                            )
                    );

            if (history.length() == 0) {

                return
                        "لا يوجد تاريخ للتطور الذاتي.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS AUTONOMOUS EVOLUTION HISTORY\n\n"
            );

            int start =
                    Math.max(
                            0,
                            history.length() - 50
                    );

            for (int i = start;
                 i < history.length();
                 i++) {

                JSONObject item =
                        history.getJSONObject(i);

                result.append("[")
                        .append(
                                item.optString(
                                        "time"
                                )
                        )
                        .append("] ");

                result.append(
                        item.optString(
                                "type"
                        )
                );

                result.append(": ")
                        .append(
                                item.optString(
                                        "message"
                                )
                        )
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {

            return
                    "تعذر قراءة Evolution History:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // INTERNAL HISTORY
    // =========================================================

    private synchronized void recordInternalHistory(
            String domain,
            String goal,
            boolean skillAdded,
            boolean capabilityAdded,
            String learning
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
                    "time",
                    now()
            );

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
                    "learning",
                    safeText(learning)
            );

            history.put(item);

            while (
                    history.length()
                            > MAX_HISTORY
            ) {

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
    // HISTORY WRITER
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
                    safeText(type)
            );

            item.put(
                    "message",
                    safeText(message)
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            while (
                    history.length()
                            > MAX_HISTORY
            ) {

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

            if (!isBlank(key)) {

                memoryManager.saveMemory(
                        key,
                        value == null
                                ? ""
                                : value
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // DOMAIN DETECTION
    // =========================================================

    private String detectInternalDomain(
            String goal
    ) {

        String lower =
                safeText(goal)
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (containsAny(
                lower,
                "voice",
                "صوت",
                "كلام",
                "نطق"
        )) {

            return "voice";
        }

        if (containsAny(
                lower,
                "memory",
                "ذاكرة",
                "تذكر",
                "حفظ"
        )) {

            return "memory";
        }

        if (containsAny(
                lower,
                "learn",
                "learning",
                "تعلم",
                "تعليم"
        )) {

            return "learning";
        }

        if (containsAny(
                lower,
                "automation",
                "أتمتة",
                "اوتوماتيك",
                "تشغيل"
        )) {

            return "automation";
        }

        if (containsAny(
                lower,
                "screen",
                "شاشة",
                "رؤية"
        )) {

            return "screen";
        }

        if (containsAny(
                lower,
                "security",
                "أمان",
                "حماية"
        )) {

            return "security";
        }

        if (containsAny(
                lower,
                "intelligence",
                "ذكاء",
                "تفكير"
        )) {

            return "intelligence";
        }

        if (containsAny(
                lower,
                "evolution",
                "تطور",
                "تطوير"
        )) {

            return "evolution";
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
                "قدرة تطور ذاتي للمجال "
                        + domain
                        + ". الهدف الحالي: "
                        + goal;
    }

    // =========================================================
    // COUNTS
    // =========================================================

    private int getEvolutionCount(
            String domain
    ) {

        try {

            String raw =
                    memoryManager.getMemory(
                            "__evolution_rule__"
                                    + domain
                    );

            if (isBlank(raw)) {
                return 0;
            }

            String marker =
                    "count=";

            int index =
                    raw.indexOf(marker);

            if (index < 0) {
                return 0;
            }

            String value =
                    raw.substring(
                            index + marker.length()
                    );

            int end =
                    value.indexOf("\n");

            if (end >= 0) {

                value =
                        value.substring(
                                0,
                                end
                        );
            }

            return Integer.parseInt(
                    value.trim()
            );

        } catch (Exception e) {

            return 0;
        }
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
    // TEST RESULT
    // =========================================================

    private boolean testPassed(
            String result
    ) {

        if (isBlank(result)) {
            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (lower.contains("exception")
                || lower.contains("error")
                || lower.contains("failed")
                || lower.contains("failure")
                || lower.contains("فشل")
                || lower.contains("خطأ")) {

            return false;
        }

        return lower.contains("pass")
                || lower.contains("passed")
                || lower.contains("success")
                || lower.contains("successful")
                || lower.contains("ready")
                || lower.contains("online")
                || lower.contains("نجح")
                || lower.contains("ناجح")
                || lower.contains("تم");
    }

    private boolean isSuccess(
            String result
    ) {

        if (isBlank(result)) {
            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        if (lower.contains("exception")
                || lower.contains("error")
                || lower.contains("failed")
                || lower.contains("failure")
                || lower.contains("فشل")
                || lower.contains("خطأ")
                || lower.contains("مرفوض")) {

            return false;
        }

        return lower.contains(
                "success"
        )
                || lower.contains(
                "successful"
        )
                || lower.contains(
                "ناجح"
        )
                || lower.contains(
                "تم تعديل"
        )
                || lower.contains(
                "تم إنشاء"
        );
    }

    // =========================================================
    // TEXT
    // =========================================================

    private String normalizeGoal(
            String goal
    ) {

        if (isBlank(goal)) {
            return "";
        }

        return goal.trim();
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
            return "غير محدد";
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

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (isBlank(text)
                || values == null) {

            return false;
        }

        for (String value : values) {

            if (!isBlank(value)
                    && text.contains(
                    value.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }

        return false;
    }

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }
}