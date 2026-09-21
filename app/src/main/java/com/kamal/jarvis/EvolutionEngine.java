package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EvolutionEngine {

    private static final String PREFS_NAME =
            "jarvis_evolution";

    private static final String KEY_GOALS =
            "goals";

    private static final String KEY_HISTORY =
            "history";

    private static final String KEY_PENDING =
            "pending_approvals";

    private static final String KEY_VERSION =
            "engine_version";

    private static final String KEY_ACTIVE_TARGET =
            "active_development_target";

    private static final String KEY_LAST_RESULT =
            "last_evolution_result";

    private static final String KEY_LAST_SUCCESS =
            "last_successful_evolution";

    private final Context context;
    private final SharedPreferences prefs;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final SelfDiagnosisManager diagnosisManager;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SelfTestEngine selfTestEngine;
    private final ApkBuilderEngine apkBuilderEngine;
    private final CodeEvolutionEngine codeEvolutionEngine;
    private final AutonomousEvolutionEngine autonomousEvolutionEngine;

    public EvolutionEngine(Context context) {

        this.context =
                context.getApplicationContext();

        prefs =
                this.context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        diagnosisManager =
                new SelfDiagnosisManager(this.context);

        selfBuilderEngine =
                new SelfBuilderEngine(this.context);

        selfTestEngine =
                new SelfTestEngine(this.context);

        apkBuilderEngine =
                new ApkBuilderEngine(this.context);

        codeEvolutionEngine =
                new CodeEvolutionEngine(this.context);

        autonomousEvolutionEngine =
                new AutonomousEvolutionEngine(this.context);

        if (!prefs.contains(KEY_VERSION)) {

            prefs.edit()
                    .putString(
                            KEY_VERSION,
                            "10.0"
                    )
                    .apply();
        }
    }

    // =========================================================
    // TIME
    // =========================================================

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }

    // =========================================================
    // GOALS
    // =========================================================

    public void createEvolutionGoal(
            String goal
    ) {

        if (isBlank(goal)) {
            return;
        }

        try {

            String cleanGoal =
                    goal.trim();

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "goal",
                    cleanGoal
            );

            item.put(
                    "status",
                    "pending"
            );

            item.put(
                    "created",
                    now()
            );

            goals.put(item);

            prefs.edit()
                    .putString(
                            KEY_GOALS,
                            goals.toString()
                    )
                    .apply();

            prefs.edit()
                    .putString(
                            KEY_ACTIVE_TARGET,
                            cleanGoal
                    )
                    .apply();

            memoryManager.saveMemory(
                    "__active_evolution_goal__",
                    cleanGoal
            );

            recordHistory(
                    "GOAL_CREATED",
                    cleanGoal
            );

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل إنشاء الهدف: "
                            + safeError(e)
            );
        }
    }

    public String getGoals() {

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            if (goals.length() == 0) {

                return
                        "لا توجد أهداف تطور مسجلة حاليا.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "أهداف JARVIS:\n\n"
            );

            for (int i = 0;
                 i < goals.length();
                 i++) {

                JSONObject item =
                        goals.getJSONObject(i);

                result.append("• ")
                        .append(
                                item.optString(
                                        "goal"
                                )
                        )
                        .append("\n");

                result.append(
                        "الحالة: "
                )
                        .append(
                                item.optString(
                                        "status"
                                )
                        )
                        .append("\n");

                result.append(
                        "التاريخ: "
                )
                        .append(
                                item.optString(
                                        "created"
                                )
                        )
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return
                    "تعذر قراءة الأهداف:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // SKILLS
    // =========================================================

    public void registerSkill(
            String name,
            String description
    ) {

        if (isBlank(name)) {
            return;
        }

        boolean added =
                skillManager.addSkill(
                        name.trim(),
                        description
                );

        recordHistory(
                "SKILL_ADDED",
                added
                        ? "تم تسجيل مهارة: "
                        + name
                        : "المهارة موجودة مسبقا: "
                        + name
        );
    }

    public void removeSkill(
            String name
    ) {

        boolean removed =
                skillManager.removeSkill(
                        name
                );

        recordHistory(
                "SKILL_REMOVED",
                removed
                        ? "تم حذف مهارة: "
                        + name
                        : "المهارة غير موجودة: "
                        + name
        );
    }

    public void recordSkillSuccess(
            String name
    ) {

        skillManager.recordSuccess(
                name
        );
    }

    public void recordSkillFailure(
            String name
    ) {

        skillManager.recordFailure(
                name
        );
    }

    public String getSkillsStatus() {

        return skillManager.getReport();
    }

    public String getSkillNames() {

        List<String> names =
                skillManager.getSkillNames();

        if (names == null ||
                names.isEmpty()) {

            return
                    "لا توجد مهارات مسجلة.";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < names.size();
             i++) {

            if (i > 0) {
                result.append("\n");
            }

            result.append(i + 1)
                    .append(". ")
                    .append(names.get(i));
        }

        return result.toString();
    }

    // =========================================================
    // CAPABILITIES
    // =========================================================

    public CapabilityManager
    getCapabilityManager() {

        return capabilityManager;
    }

    public void registerCapability(
            String name,
            String description
    ) {

        if (isBlank(name)) {
            return;
        }

        boolean added =
                capabilityManager.addCapability(
                        name.trim(),
                        description
                );

        recordHistory(
                "CAPABILITY_ADDED",
                added
                        ? "تم تسجيل قدرة: "
                        + name
                        : "القدرة موجودة مسبقا: "
                        + name
        );
    }

    public void enableCapability(
            String name
    ) {

        capabilityManager.setStatus(
                name,
                "active"
        );
    }

    public void disableCapability(
            String name
    ) {

        capabilityManager.setStatus(
                name,
                "disabled"
        );
    }

    public void recordCapabilitySuccess(
            String name
    ) {

        capabilityManager.recordSuccess(
                name
        );
    }

    public void recordCapabilityFailure(
            String name
    ) {

        capabilityManager.recordFailure(
                name
        );
    }

    public String getCapabilitiesStatus() {

        return capabilityManager.getReport();
    }

    // =========================================================
    // SELF DIAGNOSIS
    // =========================================================

    public String runFullDiagnosis() {

        return diagnosisManager.runDiagnosis();
    }

    public String selfDiagnosis() {

        return diagnosisManager.runDiagnosis();
    }

    public String getNextDevelopmentTarget() {

        return diagnosisManager
                .getNextDevelopmentTarget();
    }

    public String getDevelopmentPlan() {

        return diagnosisManager
                .getDevelopmentPlan();
    }

    // =========================================================
    // SELF BUILDER
    // =========================================================

    public String initializeSelfBuilder() {

        try {

            String result =
                    selfBuilderEngine.initialize();

            recordHistory(
                    "BUILDER_INITIALIZED",
                    result
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل تشغيل Self Builder:\n"
                            + safeError(e);
        }
    }

    public String getSelfBuilderStatus() {

        try {

            return selfBuilderEngine
                    .getStatus();

        } catch (Exception e) {

            return
                    "Self Builder غير متاح:\n"
                            + safeError(e);
        }
    }

    public String getSelfBuilderWorkspace() {

        try {

            return selfBuilderEngine
                    .getWorkspacePath();

        } catch (Exception e) {

            return
                    "Workspace غير متاح.";
        }
    }

    public SelfBuilderEngine
    getSelfBuilderEngine() {

        return selfBuilderEngine;
    }

    // =========================================================
    // CODE EVOLUTION
    // =========================================================

    public CodeEvolutionEngine
    getCodeEvolutionEngine() {

        return codeEvolutionEngine;
    }

    public String getCodeEvolutionStatus() {

        return codeEvolutionEngine
                .getStatus();
    }

    public boolean isCodeEvolutionHealthy() {

        return codeEvolutionEngine
                .isHealthy();
    }

    public String analyzeProjectCode() {

        return codeEvolutionEngine
                .analyzeProject();
    }

    public String analyzeCodeFile(
            String path
    ) {

        return codeEvolutionEngine
                .readCode(path);
    }

    public String searchCode(
            String query
    ) {

        return codeEvolutionEngine
                .searchCode(query);
    }

    public String generateJavaClass(
            String packageName,
            String className,
            String description
    ) {

        return codeEvolutionEngine
                .generateJavaClass(
                        packageName,
                        className,
                        description
                );
    }

    public String generateJavaInterface(
            String packageName,
            String interfaceName,
            String description
    ) {

        return codeEvolutionEngine
                .generateJavaInterface(
                        packageName,
                        interfaceName,
                        description
                );
    }

    public String generateDevelopmentPlan(
            String goal
    ) {

        return codeEvolutionEngine
                .generateDevelopmentPlan(
                        goal
                );
    }

    public String createCodeFile(
            String path,
            String content,
            String reason
    ) {

        return codeEvolutionEngine
                .createCodeFile(
                        path,
                        content,
                        reason
                );
    }

    public String evolveCode(
            String path,
            String oldCode,
            String newCode,
            String reason
    ) {

        String result =
                codeEvolutionEngine.modifyCode(
                        path,
                        oldCode,
                        newCode,
                        reason
                );

        recordHistory(
                "CODE_ENGINE_EVOLUTION",
                path
        );

        return result;
    }

    public String getCodeEvolutionHistory() {

        return codeEvolutionEngine
                .getHistory();
    }

    // =========================================================
    // REAL SOURCE EVOLUTION
    // =========================================================

    public String evolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        try {

            String target =
                    getActiveDevelopmentTarget();

            String finalReason =
                    isBlank(reason)
                            ? "Evolution target: "
                            + target
                            : reason;

            String result =
                    selfBuilderEngine
                            .evolveSourceFile(
                                    path,
                                    oldText,
                                    newText,
                                    finalReason
                            );

            recordHistory(
                    "SOURCE_EVOLUTION",
                    path
            );

            if (result != null &&
                    result.contains(
                            "EVOLUTION SUCCESS"
                    )) {

                markActiveGoalSuccess(
                        target,
                        result
                );
            }

            return result;

        } catch (Exception e) {

            recordHistory(
                    "SOURCE_EVOLUTION_ERROR",
                    safeError(e)
            );

            return
                    "فشل التطوير:\n"
                            + safeError(e);
        }
    }

    public String writeSourceFile(
            String path,
            String content
    ) {

        try {

            String result =
                    selfBuilderEngine
                            .writeSourceFile(
                                    path,
                                    content
                            );

            recordHistory(
                    "SOURCE_WRITE",
                    path
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل كتابة Source:\n"
                            + safeError(e);
        }
    }

    public String readSourceFile(
            String path
    ) {

        try {

            return selfBuilderEngine
                    .readSourceFile(path);

        } catch (Exception e) {

            return
                    "فشل قراءة Source:\n"
                            + safeError(e);
        }
    }

    public String searchSource(
            String query
    ) {

        try {

            return selfBuilderEngine
                    .searchSource(query);

        } catch (Exception e) {

            return
                    "فشل البحث داخل Source:\n"
                            + safeError(e);
        }
    }

    public String createDevelopmentSnapshot(
            String reason
    ) {

        try {

            String result =
                    selfBuilderEngine
                            .createSnapshot(reason);

            recordHistory(
                    "SNAPSHOT",
                    result
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل Snapshot:\n"
                            + safeError(e);
        }
    }

    public String rollbackDevelopment(
            String snapshotId
    ) {

        try {

            String result =
                    selfBuilderEngine
                            .rollback(snapshotId);

            recordHistory(
                    "ROLLBACK",
                    snapshotId
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل Rollback:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // AUTONOMOUS EVOLUTION
    // =========================================================

    public AutonomousEvolutionEngine
    getAutonomousEvolutionEngine() {

        return autonomousEvolutionEngine;
    }

    public String getAutonomousEvolutionStatus() {

        try {

            return autonomousEvolutionEngine
                    .getStatus();

        } catch (Exception e) {

            return
                    "Autonomous Evolution غير متاح:\n"
                            + safeError(e);
        }
    }

    public boolean isAutonomousEvolutionHealthy() {

        try {

            return autonomousEvolutionEngine
                    .isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    public String analyzeAutonomousEvolution(
            String goal
    ) {

        try {

            return autonomousEvolutionEngine
                    .analyzeBeforeEvolution(
                            goal
                    );

        } catch (Exception e) {

            return
                    "فشل تحليل التطور الذاتي:\n"
                            + safeError(e);
        }
    }

    public String prepareAutonomousEvolution(
            String goal
    ) {

        try {

            return autonomousEvolutionEngine
                    .prepareEvolution(
                            goal
                    );

        } catch (Exception e) {

            return
                    "فشل تجهيز التطور الذاتي:\n"
                            + safeError(e);
        }
    }

    public String autonomousEvolveSourceFile(
            String path,
            String oldText,
            String newText,
            String reason
    ) {

        try {

            return autonomousEvolutionEngine
                    .evolveSourceFile(
                            path,
                            oldText,
                            newText,
                            reason
                    );

        } catch (Exception e) {

            return
                    "فشل التطور الذاتي:\n"
                            + safeError(e);
        }
    }

    public String autonomousCreateSourceFile(
            String path,
            String content,
            String reason
    ) {

        try {

            return autonomousEvolutionEngine
                    .createSourceFile(
                            path,
                            content,
                            reason
                    );

        } catch (Exception e) {

            return
                    "فشل إنشاء Source بالتطور الذاتي:\n"
                            + safeError(e);
        }
    }

    public String runAutonomousEvolution(
            String goal
    ) {

        try {

            String cleanGoal =
                    isBlank(goal)
                            ? "تحليل JARVIS والبحث عن أقرب تطوير آمن"
                            : goal.trim();

            createEvolutionGoal(
                    cleanGoal
            );

            String result =
                    autonomousEvolutionEngine
                            .runEvolutionCycle(
                                    cleanGoal
                            );

            prefs.edit()
                    .putString(
                            KEY_LAST_RESULT,
                            result
                    )
                    .apply();

            recordHistory(
                    "AUTONOMOUS_EVOLUTION",
                    cleanGoal
            );

            memoryManager.saveMemory(
                    "__last_autonomous_goal__",
                    cleanGoal
            );

            memoryManager.saveMemory(
                    "__last_autonomous_time__",
                    now()
            );

            if (isSuccessfulEvolution(result)) {

                prefs.edit()
                        .putString(
                                KEY_LAST_SUCCESS,
                                now()
                        )
                        .apply();

                markActiveGoalSuccess(
                        cleanGoal,
                        result
                );
            }

            return result;

        } catch (Exception e) {

            String error =
                    "فشل Autonomous Evolution:\n"
                            + safeError(e);

            recordHistory(
                    "AUTONOMOUS_EVOLUTION_ERROR",
                    error
            );

            return error;
        }
    }

    public String rollbackAutonomousEvolution() {

        try {

            String result =
                    autonomousEvolutionEngine
                            .rollbackLastEvolution();

            recordHistory(
                    "AUTONOMOUS_ROLLBACK",
                    result
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل التراجع عن آخر تطور:\n"
                            + safeError(e);
        }
    }

    public String getAutonomousEvolutionHistory() {

        try {

            return autonomousEvolutionEngine
                    .getHistory();

        } catch (Exception e) {

            return
                    "تعذر قراءة سجل التطور الذاتي:\n"
                            + safeError(e);
        }
    }

    public String getLastAutonomousEvolutionResult() {

        try {

            return autonomousEvolutionEngine
                    .getLastResult();

        } catch (Exception e) {

            return
                    "لا توجد نتيجة متاحة.";
        }
    }

    // =========================================================
    // APK BUILDER
    // =========================================================

    public String validateApkProject() {

        try {

            return apkBuilderEngine
                    .validateProject();

        } catch (Exception e) {

            return
                    "فشل فحص APK Project:\n"
                            + safeError(e);
        }
    }

    public String prepareApkBuild(
            String reason
    ) {

        try {

            return apkBuilderEngine
                    .prepareBuildRequest(
                            reason
                    );

        } catch (Exception e) {

            return
                    "فشل تجهيز APK Build:\n"
                            + safeError(e);
        }
    }

    public String buildDebugApk() {

        try {

            String result =
                    apkBuilderEngine
                            .buildDebugApk();

            recordHistory(
                    "APK_BUILD",
                    result
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل بناء APK:\n"
                            + safeError(e);
        }
    }

    public String getApkBuildStatus() {

        return apkBuilderEngine
                .getStatus();
    }

    public String getLatestApk() {

        return apkBuilderEngine
                .findLatestApk();
    }

    public String getApkBuildHistory() {

        return apkBuilderEngine
                .getBuildHistory();
    }

    public ApkBuilderEngine
    getApkBuilderEngine() {

        return apkBuilderEngine;
    }

    // =========================================================
    // SELF TEST
    // =========================================================

    public String runSelfTests() {

        try {

            String result =
                    selfTestEngine
                            .runAllTests();

            recordHistory(
                    "SELF_TEST",
                    result
            );

            return result;

        } catch (Exception e) {

            return
                    "فشل Self Test:\n"
                            + safeError(e);
        }
    }

    public SelfTestEngine
    getSelfTestEngine() {

        return selfTestEngine;
    }

    // =========================================================
    // MAIN EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle() {

        String target =
                getActiveDevelopmentTarget();

        if (isBlank(target) ||
                target.contains(
                        "لا يوجد هدف"
                )) {

            target =
                    getNextDevelopmentTarget();
        }

        if (isBlank(target)) {

            target =
                    "تحليل JARVIS والبحث عن أقرب تطوير آمن";
        }

        return runAutonomousEvolution(
                target
        );
    }

    // =========================================================
    // ACTIVE TARGET
    // =========================================================

    public String getActiveDevelopmentTarget() {

        String target =
                prefs.getString(
                        KEY_ACTIVE_TARGET,
                        ""
                );

        if (!isBlank(target)) {
            return target;
        }

        String memoryTarget =
                memoryManager.getMemory(
                        "__active_evolution_goal__"
                );

        if (!isBlank(memoryTarget)) {
            return memoryTarget;
        }

        return
                "لا يوجد هدف تطوير نشط حاليا.";
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public void requestApproval(
            String action
    ) {

        if (isBlank(action)) {
            return;
        }

        try {

            JSONArray pending =
                    new JSONArray(
                            prefs.getString(
                                    KEY_PENDING,
                                    "[]"
                            )
                    );

            JSONObject request =
                    new JSONObject();

            request.put(
                    "action",
                    action.trim()
            );

            request.put(
                    "status",
                    "pending"
            );

            request.put(
                    "created",
                    now()
            );

            pending.put(request);

            prefs.edit()
                    .putString(
                            KEY_PENDING,
                            pending.toString()
                    )
                    .apply();

            recordHistory(
                    "APPROVAL_REQUEST",
                    action
            );

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل طلب الموافقة: "
                            + safeError(e)
            );
        }
    }

    public boolean approve(
            String action
    ) {

        return updateApproval(
                action,
                "approved"
        );
    }

    public boolean reject(
            String action
    ) {

        return updateApproval(
                action,
                "rejected"
        );
    }

    private boolean updateApproval(
            String action,
            String status
    ) {

        if (isBlank(action)) {
            return false;
        }

        try {

            JSONArray pending =
                    new JSONArray(
                            prefs.getString(
                                    KEY_PENDING,
                                    "[]"
                            )
                    );

            boolean found = false;

            for (int i = 0;
                 i < pending.length();
                 i++) {

                JSONObject item =
                        pending.getJSONObject(i);

                if (action.trim().equals(
                        item.optString(
                                "action"
                        )
                )
                &&
                        "pending".equals(
                                item.optString(
                                        "status"
                                )
                        )
                ) {

                    item.put(
                            "status",
                            status
                    );

                    item.put(
                            status,
                            now()
                    );

                    found = true;
                }
            }

            prefs.edit()
                    .putString(
                            KEY_PENDING,
                            pending.toString()
                    )
                    .apply();

            if (found) {

                recordHistory(
                        "APPROVAL_" +
                                status.toUpperCase(
                                        Locale.ROOT
                                ),
                        action
                );
            }

            return found;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getEvolutionStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS EVOLUTION STATUS\n"
        );

        result.append(
                "============================\n"
        );

        result.append(
                "Engine: "
        )
                .append(
                        prefs.getString(
                                KEY_VERSION,
                                "10.0"
                        )
                )
                .append("\n");

        result.append(
                "Memory: ONLINE\n"
        );

        result.append(
                "Skills: "
        )
                .append(
                        skillManager
                                .getSkillCount()
                )
                .append("\n");

        result.append(
                "Capabilities: "
        )
                .append(
                        capabilityManager
                                .getCount()
                )
                .append("\n");

        result.append(
                "Code Evolution: "
        )
                .append(
                        codeEvolutionEngine
                                .isHealthy()
                                ? "ONLINE"
                                : "ATTENTION"
                )
                .append("\n");

        result.append(
                "Self Builder: "
        )
                .append(
                        selfBuilderEngine
                                .isHealthy()
                                ? "ONLINE"
                                : "ATTENTION"
                )
                .append("\n");

        result.append(
                "Autonomous Evolution: "
        )
                .append(
                        isAutonomousEvolutionHealthy()
                                ? "ONLINE"
                                : "ATTENTION"
                )
                .append("\n");

        result.append(
                "APK Builder: "
        )
                .append(
                        apkBuilderEngine
                                .isHealthy()
                                ? "ONLINE"
                                : "ATTENTION"
                )
                .append("\n");

        result.append(
                "Self Test: "
        )
                .append(
                        selfTestEngine
                                .isHealthy()
                                ? "HEALTHY"
                                : "ATTENTION"
                )
                .append("\n");

        result.append(
                "Readiness: "
        )
                .append(
                        diagnosisManager
                                .getReadinessScore()
                )
                .append("%\n");

        result.append(
                "Goals: "
        )
                .append(
                        getGoalCount()
                )
                .append("\n");

        result.append(
                "History: "
        )
                .append(
                        getHistoryCount()
                )
                .append("\n");

        result.append(
                "Active Target: "
        )
                .append(
                        getActiveDevelopmentTarget()
                )
                .append("\n");

        result.append(
                "Last Success: "
        )
                .append(
                        prefs.getString(
                                KEY_LAST_SUCCESS,
                                "NONE"
                        )
                )
                .append("\n");

        result.append(
                "\nCORE STATUS: ONLINE"
        );

        return result.toString();
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void recordHistory(
            String type,
            String message
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
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
                    message == null
                            ? ""
                            : message
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            while (history.length() > 150) {
                history.remove(0);
            }

            prefs.edit()
                    .putString(
                            KEY_HISTORY,
                            history.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    public String getEvolutionHistory() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            if (history.length() == 0) {

                return
                        "لا توجد سجلات تطور بعد.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS EVOLUTION HISTORY\n\n"
            );

            int start =
                    Math.max(
                            0,
                            history.length() - 30
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

                result.append(": ");

                result.append(
                        item.optString(
                                "message"
                        )
                );

                result.append("\n");
            }

            return result.toString();

        } catch (Exception e) {

            return
                    "تعذر قراءة سجل التطور:\n"
                            + safeError(e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isSuccessfulEvolution(
            String result
    ) {

        if (result == null) {
            return false;
        }

        String value =
                result.toLowerCase(
                        Locale.ROOT
                );

        return value.contains(
                "evolution success"
        )
                || value.contains(
                "evolution ناجح"
        )
                || value.contains(
                "internal evolution success"
        );
    }

    private void markActiveGoalSuccess(
            String goal,
            String result
    ) {

        if (isBlank(goal)) {
            return;
        }

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            for (int i = 0;
                 i < goals.length();
                 i++) {

                JSONObject item =
                        goals.getJSONObject(i);

                if (goal.equals(
                        item.optString(
                                "goal"
                        )
                )
                &&
                        "pending".equals(
                                item.optString(
                                        "status"
                                )
                        )
                ) {

                    item.put(
                            "status",
                            "completed"
                    );

                    item.put(
                            "completed",
                            now()
                    );

                    item.put(
                            "result",
                            result == null
                                    ? ""
                                    : result
                    );
                }
            }

            prefs.edit()
                    .putString(
                            KEY_GOALS,
                            goals.toString()
                    )
                    .apply();

            recordHistory(
                    "GOAL_COMPLETED",
                    goal
            );

        } catch (Exception e) {

            recordHistory(
                    "GOAL_COMPLETION_ERROR",
                    safeError(e)
            );
        }
    }

    private int getGoalCount() {

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            return goals.length();

        } catch (Exception e) {

            return 0;
        }
    }

    private int getHistoryCount() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            return history.length();

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // ACCESSORS
    // =========================================================

    public MemoryManager getMemoryManager() {

        return memoryManager;
    }

    public SkillManager getSkillManager() {

        return skillManager;
    }

    public SelfDiagnosisManager
    getDiagnosisManager() {

        return diagnosisManager;
    }

    // =========================================================
    // ERROR
    // =========================================================

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

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}