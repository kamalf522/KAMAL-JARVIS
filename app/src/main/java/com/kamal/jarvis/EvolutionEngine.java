package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * JARVIS Evolution Engine
 *
 * الهدف:
 * - إدارة أهداف التطور
 * - إدارة المهارات والقدرات
 * - التشخيص الذاتي
 * - ربط Intelligence / Self Builder / Code Evolution
 * - التطور الذاتي Autonomous Evolution
 * - Snapshot / Rollback
 * - بناء APK
 * - Self Testing
 * - Approval system
 * - حفظ تاريخ التطور
 *
 * ملاحظة:
 * التطوير الداخلي للبيانات والقدرات يمكن أن يحدث بدون APK جديد.
 * أما تغيير Java/XML الحقيقي للتطبيق فيحتاج Build/Update جديد.
 */
public class EvolutionEngine {

    private static final String PREFS_NAME = "jarvis_evolution";

    private static final String KEY_GOALS = "goals";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_PENDING = "pending_approvals";
    private static final String KEY_VERSION = "engine_version";
    private static final String KEY_ACTIVE_TARGET = "active_development_target";
    private static final String KEY_LAST_RESULT = "last_evolution_result";
    private static final String KEY_LAST_SUCCESS = "last_successful_evolution";

    private static final int MAX_HISTORY = 150;
    private static final int MAX_GOALS = 50;

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

        this.context = context.getApplicationContext();

        prefs = this.context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        memoryManager = new MemoryManager(this.context);
        skillManager = new SkillManager(this.context);
        capabilityManager = new CapabilityManager(this.context);
        diagnosisManager = new SelfDiagnosisManager(this.context);

        selfBuilderEngine = new SelfBuilderEngine(this.context);
        selfTestEngine = new SelfTestEngine(this.context);
        apkBuilderEngine = new ApkBuilderEngine(this.context);
        codeEvolutionEngine = new CodeEvolutionEngine(this.context);
        autonomousEvolutionEngine =
                new AutonomousEvolutionEngine(this.context);

        if (!prefs.contains(KEY_VERSION)) {
            prefs.edit()
                    .putString(KEY_VERSION, "11.0-FINAL")
                    .apply();
        }
    }

    // =========================================================
    // BASIC HELPERS
    // =========================================================

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String clean(String value) {
        if (isBlank(value)) {
            return "";
        }

        return value.trim();
    }

    private String now() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String safeError(Exception e) {

        if (e == null) {
            return "Unknown error";
        }

        String message = e.getMessage();

        if (isBlank(message)) {
            return e.getClass().getSimpleName();
        }

        return message;
    }

    // =========================================================
    // EVOLUTION GOALS
    // =========================================================

    public synchronized void createEvolutionGoal(String goal) {

        String cleanGoal = clean(goal);

        if (cleanGoal.isEmpty()) {
            return;
        }

        try {

            JSONArray goals = new JSONArray(
                    prefs.getString(KEY_GOALS, "[]")
            );

            JSONObject item = new JSONObject();

            item.put("goal", cleanGoal);
            item.put("status", "active");
            item.put("created", now());

            goals.put(item);

            while (goals.length() > MAX_GOALS) {
                goals.remove(0);
            }

            prefs.edit()
                    .putString(KEY_GOALS, goals.toString())
                    .putString(KEY_ACTIVE_TARGET, cleanGoal)
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
                    "GOAL_ERROR",
                    safeError(e)
            );
        }
    }

    public String getGoals() {

        try {

            JSONArray goals = new JSONArray(
                    prefs.getString(KEY_GOALS, "[]")
            );

            if (goals.length() == 0) {
                return "لا توجد أهداف تطور حاليا.";
            }

            StringBuilder result = new StringBuilder();

            result.append("أهداف JARVIS:\n\n");

            for (int i = 0; i < goals.length(); i++) {

                JSONObject goal = goals.getJSONObject(i);

                result.append(i + 1)
                        .append(". ")
                        .append(goal.optString("goal"))
                        .append("\n");

                result.append("الحالة: ")
                        .append(goal.optString("status"))
                        .append("\n");

                result.append("التاريخ: ")
                        .append(goal.optString("created"))
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return "تعذر قراءة أهداف التطور:\n"
                    + safeError(e);
        }
    }

    private void markActiveGoalSuccess(
            String goal,
            String result
    ) {

        if (isBlank(goal)) {
            return;
        }

        try {

            JSONArray goals = new JSONArray(
                    prefs.getString(KEY_GOALS, "[]")
            );

            for (int i = 0; i < goals.length(); i++) {

                JSONObject item =
                        goals.getJSONObject(i);

                if (goal.trim().equals(
                        item.optString("goal")
                )) {

                    item.put("status", "completed");
                    item.put("completed", now());

                    item.put(
                            "result",
                            isBlank(result)
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

            memoryManager.saveMemory(
                    "__last_successful_evolution_goal__",
                    goal
            );

        } catch (Exception e) {

            recordHistory(
                    "GOAL_UPDATE_ERROR",
                    safeError(e)
            );
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
                "SKILL_REGISTER",
                added
                        ? "تمت إضافة المهارة: " + name
                        : "المهارة موجودة: " + name
        );
    }

    public void removeSkill(String name) {

        if (isBlank(name)) {
            return;
        }

        boolean removed =
                skillManager.removeSkill(name.trim());

        recordHistory(
                "SKILL_REMOVE",
                removed
                        ? "تم حذف: " + name
                        : "غير موجودة: " + name
        );
    }

    public void recordSkillSuccess(String name) {

        if (!isBlank(name)) {
            skillManager.recordSuccess(name);
        }
    }

    public void recordSkillFailure(String name) {

        if (!isBlank(name)) {
            skillManager.recordFailure(name);
        }
    }

    public String getSkillsStatus() {
        return skillManager.getReport();
    }

    public String getSkillNames() {

        if (skillManager.getSkillNames().isEmpty()) {
            return "لا توجد مهارات.";
        }

        StringBuilder result = new StringBuilder();

        int index = 1;

        for (String name : skillManager.getSkillNames()) {

            result.append(index++)
                    .append(". ")
                    .append(name)
                    .append("\n");
        }

        return result.toString().trim();
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    // =========================================================
    // CAPABILITIES
    // =========================================================

    public CapabilityManager getCapabilityManager() {
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
                "CAPABILITY_REGISTER",
                added
                        ? "تمت إضافة القدرة: " + name
                        : "القدرة موجودة: " + name
        );
    }

    public void enableCapability(String name) {

        if (!isBlank(name)) {
            capabilityManager.setStatus(
                    name,
                    "active"
            );
        }
    }

    public void disableCapability(String name) {

        if (!isBlank(name)) {
            capabilityManager.setStatus(
                    name,
                    "disabled"
            );
        }
    }

    public void recordCapabilitySuccess(String name) {

        if (!isBlank(name)) {
            capabilityManager.recordSuccess(name);
        }
    }

    public void recordCapabilityFailure(String name) {

        if (!isBlank(name)) {
            capabilityManager.recordFailure(name);
        }
    }

    public String getCapabilitiesStatus() {
        return capabilityManager.getReport();
    }

    // =========================================================
    // SELF DIAGNOSIS
    // =========================================================

    public String runFullDiagnosis() {

        try {
            return diagnosisManager.runDiagnosis();
        } catch (Exception e) {
            return "فشل التشخيص:\n" + safeError(e);
        }
    }

    public String selfDiagnosis() {
        return runFullDiagnosis();
    }

    public String getNextDevelopmentTarget() {

        try {
            return diagnosisManager
                    .getNextDevelopmentTarget();
        } catch (Exception e) {
            return "لا يوجد هدف تطوير حاليا.";
        }
    }

    public String getDevelopmentPlan() {

        try {
            return diagnosisManager
                    .getDevelopmentPlan();
        } catch (Exception e) {
            return "خطة التطوير غير متاحة.";
        }
    }

    public SelfDiagnosisManager getDiagnosisManager() {
        return diagnosisManager;
    }

    // =========================================================
    // SELF BUILDER
    // =========================================================

    public String initializeSelfBuilder() {

        try {

            String result =
                    selfBuilderEngine.initialize();

            recordHistory(
                    "SELF_BUILDER_INIT",
                    result
            );

            return result;

        } catch (Exception e) {

            return "فشل تشغيل Self Builder:\n"
                    + safeError(e);
        }
    }

    public String getSelfBuilderStatus() {

        try {
            return selfBuilderEngine.getStatus();
        } catch (Exception e) {
            return "Self Builder غير متاح.";
        }
    }

    public String getSelfBuilderWorkspace() {

        try {
            return selfBuilderEngine.getWorkspacePath();
        } catch (Exception e) {
            return "Workspace غير متاح.";
        }
    }

    public SelfBuilderEngine getSelfBuilderEngine() {
        return selfBuilderEngine;
    }

    // =========================================================
    // CODE EVOLUTION
    // =========================================================

    public CodeEvolutionEngine getCodeEvolutionEngine() {
        return codeEvolutionEngine;
    }

    public String getCodeEvolutionStatus() {
        return codeEvolutionEngine.getStatus();
    }

    public boolean isCodeEvolutionHealthy() {
        return codeEvolutionEngine.isHealthy();
    }

    public String analyzeProjectCode() {

        try {
            return codeEvolutionEngine.analyzeProject();
        } catch (Exception e) {
            return "فشل تحليل المشروع:\n"
                    + safeError(e);
        }
    }

    public String analyzeCodeFile(String path) {

        try {
            return codeEvolutionEngine.readCode(path);
        } catch (Exception e) {
            return "فشل قراءة الملف:\n"
                    + safeError(e);
        }
    }

    public String searchCode(String query) {

        try {
            return codeEvolutionEngine.searchCode(query);
        } catch (Exception e) {
            return "فشل البحث:\n"
                    + safeError(e);
        }
    }

    public String generateJavaClass(
            String packageName,
            String className,
            String description
    ) {

        try {

            return codeEvolutionEngine.generateJavaClass(
                    packageName,
                    className,
                    description
            );

        } catch (Exception e) {

            return "فشل توليد Java Class:\n"
                    + safeError(e);
        }
    }

    public String generateJavaInterface(
            String packageName,
            String interfaceName,
            String description
    ) {

        try {

            return codeEvolutionEngine.generateJavaInterface(
                    packageName,
                    interfaceName,
                    description
            );

        } catch (Exception e) {

            return "فشل توليد Interface:\n"
                    + safeError(e);
        }
    }

    public String generateDevelopmentPlan(
            String goal
    ) {

        try {
            return codeEvolutionEngine
                    .generateDevelopmentPlan(goal);
        } catch (Exception e) {
            return "فشل إنشاء خطة الكود:\n"
                    + safeError(e);
        }
    }

    public String createCodeFile(
            String path,
            String content,
            String reason
    ) {

        try {

            String result =
                    codeEvolutionEngine.createCodeFile(
                            path,
                            content,
                            reason
                    );

            recordHistory(
                    "CODE_FILE_CREATE",
                    path
            );

            return result;

        } catch (Exception e) {

            return "فشل إنشاء Code File:\n"
                    + safeError(e);
        }
    }

    public String evolveCode(
            String path,
            String oldCode,
            String newCode,
            String reason
    ) {

        try {

            String result =
                    codeEvolutionEngine.modifyCode(
                            path,
                            oldCode,
                            newCode,
                            reason
                    );

            recordHistory(
                    "CODE_EVOLUTION",
                    path
            );

            return result;

        } catch (Exception e) {

            return "فشل Code Evolution:\n"
                    + safeError(e);
        }
    }

    public String getCodeEvolutionHistory() {
        return codeEvolutionEngine.getHistory();
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
                            ? "Evolution target: " + target
                            : reason;

            String result =
                    selfBuilderEngine.evolveSourceFile(
                            path,
                            oldText,
                            newText,
                            finalReason
                    );

            recordHistory(
                    "SOURCE_EVOLUTION",
                    path
            );

            if (isSuccessfulEvolution(result)) {

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

            return "فشل تطوير Source:\n"
                    + safeError(e);
        }
    }

    public String writeSourceFile(
            String path,
            String content
    ) {

        try {

            String result =
                    selfBuilderEngine.writeSourceFile(
                            path,
                            content
                    );

            recordHistory(
                    "SOURCE_WRITE",
                    path
            );

            return result;

        } catch (Exception e) {

            return "فشل كتابة Source:\n"
                    + safeError(e);
        }
    }

    public String readSourceFile(String path) {

        try {
            return selfBuilderEngine.readSourceFile(path);
        } catch (Exception e) {
            return "فشل قراءة Source:\n"
                    + safeError(e);
        }
    }

    public String searchSource(String query) {

        try {
            return selfBuilderEngine.searchSource(query);
        } catch (Exception e) {
            return "فشل البحث داخل Source:\n"
                    + safeError(e);
        }
    }

    public String createDevelopmentSnapshot(
            String reason
    ) {

        try {

            String result =
                    selfBuilderEngine.createSnapshot(reason);

            recordHistory(
                    "SNAPSHOT",
                    result
            );

            return result;

        } catch (Exception e) {

            return "فشل Snapshot:\n"
                    + safeError(e);
        }
    }

    public String rollbackDevelopment(
            String snapshotId
    ) {

        try {

            String result =
                    selfBuilderEngine.rollback(
                            snapshotId
                    );

            recordHistory(
                    "ROLLBACK",
                    snapshotId
            );

            return result;

        } catch (Exception e) {

            return "فشل Rollback:\n"
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
            return autonomousEvolutionEngine.getStatus();
        } catch (Exception e) {
            return "Autonomous Evolution غير متاح.";
        }
    }

    public boolean isAutonomousEvolutionHealthy() {

        try {
            return autonomousEvolutionEngine.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    public String analyzeAutonomousEvolution(
            String goal
    ) {

        try {

            return autonomousEvolutionEngine
                    .analyzeBeforeEvolution(goal);

        } catch (Exception e) {

            return "فشل تحليل التطور الذاتي:\n"
                    + safeError(e);
        }
    }

    public String prepareAutonomousEvolution(
            String goal
    ) {

        try {

            return autonomousEvolutionEngine
                    .prepareEvolution(goal);

        } catch (Exception e) {

            return "فشل تجهيز التطور الذاتي:\n"
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

            return "فشل التطور الذاتي:\n"
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

            return "فشل إنشاء Source:\n"
                    + safeError(e);
        }
    }

    public String runAutonomousEvolution(
            String goal
    ) {

        String cleanGoal = clean(goal);

        if (cleanGoal.isEmpty()) {

            cleanGoal =
                    "تحليل JARVIS والبحث عن أقرب تطوير آمن";
        }

        try {

            createEvolutionGoal(cleanGoal);

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

            memoryManager.saveMemory(
                    "__last_autonomous_goal__",
                    cleanGoal
            );

            memoryManager.saveMemory(
                    "__last_autonomous_time__",
                    now()
            );

            recordHistory(
                    "AUTONOMOUS_EVOLUTION",
                    cleanGoal
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

            return "فشل التراجع:\n"
                    + safeError(e);
        }
    }

    public String getAutonomousEvolutionHistory() {

        try {
            return autonomousEvolutionEngine.getHistory();
        } catch (Exception e) {
            return "تعذر قراءة سجل التطور الذاتي:\n"
                    + safeError(e);
        }
    }

    public String getLastAutonomousEvolutionResult() {

        try {
            return autonomousEvolutionEngine.getLastResult();
        } catch (Exception e) {
            return prefs.getString(
                    KEY_LAST_RESULT,
                    "لا توجد نتيجة."
            );
        }
    }

    // =========================================================
    // APK BUILD
    // =========================================================

    public String validateApkProject() {

        try {
            return apkBuilderEngine.validateProject();
        } catch (Exception e) {
            return "فشل فحص APK Project:\n"
                    + safeError(e);
        }
    }

    public String prepareApkBuild(String reason) {

        try {

            return apkBuilderEngine
                    .prepareBuildRequest(reason);

        } catch (Exception e) {

            return "فشل تجهيز APK Build:\n"
                    + safeError(e);
        }
    }

    public String buildDebugApk() {

        try {

            String result =
                    apkBuilderEngine.buildDebugApk();

            recordHistory(
                    "APK_BUILD",
                    result
            );

            return result;

        } catch (Exception e) {

            return "فشل بناء APK:\n"
                    + safeError(e);
        }
    }

    public String getApkBuildStatus() {
        return apkBuilderEngine.getStatus();
    }

    public String getLatestApk() {
        return apkBuilderEngine.findLatestApk();
    }

    public String getApkBuildHistory() {
        return apkBuilderEngine.getBuildHistory();
    }

    public ApkBuilderEngine getApkBuilderEngine() {
        return apkBuilderEngine;
    }

    // =========================================================
    // SELF TEST
    // =========================================================

    public String runSelfTests() {

        try {

            String result =
                    selfTestEngine.runAllTests();

            recordHistory(
                    "SELF_TEST",
                    result
            );

            return result;

        } catch (Exception e) {

            return "فشل Self Test:\n"
                    + safeError(e);
        }
    }

    public SelfTestEngine getSelfTestEngine() {
        return selfTestEngine;
    }

    // =========================================================
    // MAIN EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle() {

        String target =
                getActiveDevelopmentTarget();

        if (isBlank(target)
                || target.contains("لا يوجد هدف")) {

            target =
                    getNextDevelopmentTarget();
        }

        if (isBlank(target)) {

            target =
                    "تحليل JARVIS والبحث عن أقرب تطوير آمن";
        }

        return runAutonomousEvolution(target);
    }

    // =========================================================
    // ACTIVE DEVELOPMENT TARGET
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

        return "لا يوجد هدف تطوير نشط حاليا.";
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public synchronized void requestApproval(
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
                    "APPROVAL_ERROR",
                    safeError(e)
            );
        }
    }

    public boolean approve(String action) {

        return updateApproval(
                action,
                "approved"
        );
    }

    public boolean reject(String action) {

        return updateApproval(
                action,
                "rejected"
        );
    }

    private synchronized boolean updateApproval(
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
                        item.optString("action")
                )
                        && "pending".equals(
                        item.optString("status")
                )) {

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
                        "APPROVAL_"
                                + status.toUpperCase(
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
                "JARVIS EVOLUTION SYSTEM\n"
        );

        result.append(
                "==============================\n"
        );

        result.append(
                "Engine: "
        ).append(
                prefs.getString(
                        KEY_VERSION,
                        "11.0-FINAL"
                )
        ).append("\n");

        result.append(
                "Active Target: "
        ).append(
                getActiveDevelopmentTarget()
        ).append("\n\n");

        result.append(
                "Diagnosis: "
        ).append(
                diagnosisManager.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n");

        result.append(
                "Self Builder: "
        ).append(
                selfBuilderEngine.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n");

        result.append(
                "Code Evolution: "
        ).append(
                codeEvolutionEngine.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n");

        result.append(
                "Autonomous Evolution: "
        ).append(
                autonomousEvolutionEngine.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n");

        result.append(
                "Self Test: "
        ).append(
                selfTestEngine.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n");

        result.append(
                "APK Builder: "
        ).append(
                apkBuilderEngine.isHealthy()
                        ? "READY"
                        : "CHECK"
        ).append("\n\n");

        result.append(
                "Last Result: "
        ).append(
                prefs.getString(
                        KEY_LAST_RESULT,
                        "لا توجد نتيجة."
                )
        ).append("\n");

        result.append(
                "Last Success: "
        ).append(
                prefs.getString(
                        KEY_LAST_SUCCESS,
                        "لا يوجد."
                )
        );

        return result.toString();
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public synchronized String getEvolutionHistory() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            if (history.length() == 0) {
                return "لا يوجد تاريخ Evolution.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS EVOLUTION HISTORY\n\n"
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

            return "تعذر قراءة History:\n"
                    + safeError(e);
        }
    }

    private synchronized void recordHistory(
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
                    isBlank(type)
                            ? "UNKNOWN"
                            : type
            );

            item.put(
                    "message",
                    isBlank(message)
                            ? ""
                            : message
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            while (history.length()
                    > MAX_HISTORY) {

                history.remove(0);
            }

            prefs.edit()
                    .putString(
                            KEY_HISTORY,
                            history.toString()
                    )
                    .apply();

            memoryManager.saveMemory(
                    "__evolution_last_action__",
                    type + " | " + message
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // EVOLUTION SUCCESS
    // =========================================================

    private boolean isSuccessfulEvolution(
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

        return lower.contains("success")
                || lower.contains("successful")
                || lower.contains("completed")
                || lower.contains("evolution success")
                || lower.contains("تم")
                || lower.contains("نجح");
    }

    // =========================================================
    // MANAGERS
    // =========================================================

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            return diagnosisManager.isHealthy()
                    && selfBuilderEngine.isHealthy()
                    && codeEvolutionEngine.isHealthy()
                    && autonomousEvolutionEngine.isHealthy()
                    && selfTestEngine.isHealthy()
                    && apkBuilderEngine.isHealthy();

        } catch (Exception e) {

            return false;
        }
    }
}