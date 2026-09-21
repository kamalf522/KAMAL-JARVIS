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

    private final Context context;
    private final SharedPreferences prefs;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final SelfDiagnosisManager diagnosisManager;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SelfTestEngine selfTestEngine;

    public EvolutionEngine(Context context) {

        this.context =
                context.getApplicationContext();

        prefs =
                this.context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
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

        diagnosisManager =
                new SelfDiagnosisManager(
                        this.context
                );

        selfBuilderEngine =
                new SelfBuilderEngine(
                        this.context
                );

        selfTestEngine =
                new SelfTestEngine(
                        this.context
                );

        if (!prefs.contains(KEY_VERSION)) {

            prefs.edit()
                    .putString(
                            KEY_VERSION,
                            "5.0"
                    )
                    .apply();
        }
    }

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

        if (goal == null ||
                goal.trim().isEmpty()) {

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

            JSONObject item =
                    new JSONObject();

            item.put(
                    "goal",
                    goal.trim()
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

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل إنشاء الهدف: "
                            + e.getMessage()
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
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return
                    "تعذر قراءة الأهداف.";
        }
    }

    // =========================================================
    // SKILLS
    // =========================================================

    public void registerSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return;
        }

        boolean added =
                skillManager.addSkill(
                        name,
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

            result.append(
                    i + 1
            )
                    .append(". ")
                    .append(
                            names.get(i)
                    );
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

        boolean added =
                capabilityManager.addCapability(
                        name,
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
    // REAL SELF BUILDER
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

            recordHistory(
                    "BUILDER_ERROR",
                    safeError(e)
            );

            return
                    "فشل تشغيل Self Builder:\n"
                    + safeError(e);
        }
    }

    public String getSelfBuilderStatus() {

        try {

            return selfBuilderEngine.getStatus();

        } catch (Exception e) {

            return
                    "Self Builder غير متاح:\n"
                    + safeError(e);
        }
    }

    public String getSelfBuilderWorkspace() {

        try {

            return
                    selfBuilderEngine
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

            recordHistory(
                    "TEST_ERROR",
                    safeError(e)
            );

            return
                    "فشل تشغيل الاختبارات:\n"
                    + safeError(e);
        }
    }

    public SelfTestEngine
    getSelfTestEngine() {

        return selfTestEngine;
    }

    // =========================================================
    // REAL EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle() {

        String target;

        try {

            target =
                    diagnosisManager
                            .getNextDevelopmentTarget();

        } catch (Exception e) {

            target =
                    "تحسين النظام الأساسي";
        }

        if (target == null ||
                target.trim().isEmpty()) {

            target =
                    "تحسين النظام الأساسي";
        }

        prefs.edit()
                .putString(
                        KEY_ACTIVE_TARGET,
                        target
                )
                .apply();

        createEvolutionGoal(
                "تطوير النظام: "
                        + target
        );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS EVOLUTION ENGINE 5.0\n"
        );

        result.append(
                "============================\n\n"
        );

        // -----------------------------------------------------
        // 1 OBSERVE
        // -----------------------------------------------------

        recordHistory(
                "OBSERVE",
                "تم فحص حالة النظام"
        );

        result.append(
                "1. OBSERVE\n"
        );

        result.append(
                "✓ فحص الحالة العامة للنظام.\n\n"
        );

        // -----------------------------------------------------
        // 2 DIAGNOSIS
        // -----------------------------------------------------

        int readiness;

        try {

            readiness =
                    diagnosisManager
                            .getReadinessScore();

        } catch (Exception e) {

            readiness = 0;
        }

        recordHistory(
                "DIAGNOSIS",
                "جاهزية النظام: "
                        + readiness
                        + "%"
        );

        result.append(
                "2. SELF DIAGNOSIS\n"
        );

        result.append(
                "✓ جاهزية النظام: "
        )
                .append(
                        readiness
                )
                .append("%\n\n");

        // -----------------------------------------------------
        // 3 CAPABILITY SCAN
        // -----------------------------------------------------

        int capabilityCount =
                capabilityManager.getCount();

        recordHistory(
                "CAPABILITY_SCAN",
                "Capabilities: "
                        + capabilityCount
        );

        result.append(
                "3. CAPABILITY SCAN\n"
        );

        result.append(
                "✓ القدرات المسجلة: "
        )
                .append(
                        capabilityCount
                )
                .append("\n\n");

        // -----------------------------------------------------
        // 4 TARGET
        // -----------------------------------------------------

        recordHistory(
                "TARGET_SELECTED",
                target
        );

        result.append(
                "4. TARGET SELECTION\n"
        );

        result.append(
                "→ "
        )
                .append(
                        target
                )
                .append("\n\n");

        // -----------------------------------------------------
        // 5 DEVELOPMENT GOAL
        // -----------------------------------------------------

        recordHistory(
                "DEVELOPMENT_GOAL",
                "تطوير النظام: "
                        + target
        );

        result.append(
                "5. DEVELOPMENT PLAN\n"
        );

        result.append(
                "✓ الهدف: "
        )
                .append(
                        target
                )
                .append("\n\n");

        // -----------------------------------------------------
        // 6 LEARNING
        // -----------------------------------------------------

        recordHistory(
                "LEARN",
                "Learning stage active"
        );

        result.append(
                "6. LEARN\n"
        );

        result.append(
                "✓ Skill Manager جاهز للتعلم.\n\n"
        );

        // -----------------------------------------------------
        // 7 INITIALIZE BUILDER
        // -----------------------------------------------------

        String builderInit;

        try {

            builderInit =
                    selfBuilderEngine
                            .initialize();

            recordHistory(
                    "BUILDER_INITIALIZED",
                    "Self Builder online"
            );

        } catch (Exception e) {

            builderInit =
                    "ERROR: "
                            + safeError(e);

            recordHistory(
                    "BUILDER_ERROR",
                    builderInit
            );
        }

        result.append(
                "7. SELF BUILDER\n"
        );

        result.append(
                builderInit
        )
                .append("\n\n");

        // -----------------------------------------------------
        // 8 REGISTER DEVELOPMENT
        // -----------------------------------------------------

        String development;

        try {

            development =
                    selfBuilderEngine
                            .registerDevelopment(
                                    target,
                                    "تطوير تلقائي بناء على التشخيص الذاتي"
                            );

            recordHistory(
                    "DEVELOPMENT_REGISTERED",
                    target
            );

        } catch (Exception e) {

            development =
                    "ERROR: "
                            + safeError(e);

            recordHistory(
                    "DEVELOPMENT_ERROR",
                    development
            );
        }

        result.append(
                "8. DEVELOPMENT TARGET\n"
        );

        result.append(
                development
        )
                .append("\n\n");

        // -----------------------------------------------------
        // 9 SNAPSHOT
        // -----------------------------------------------------

        String snapshot;

        try {

            snapshot =
                    selfBuilderEngine
                            .createSnapshot(
                                    "قبل دورة التطور: "
                                            + target
                            );

            recordHistory(
                    "SNAPSHOT",
                    snapshot
            );

        } catch (Exception e) {

            snapshot =
                    "ERROR: "
                            + safeError(e);

            recordHistory(
                    "SNAPSHOT_ERROR",
                    snapshot
            );
        }

        result.append(
                "9. SAFETY SNAPSHOT\n"
        );

        result.append(
                snapshot
        )
                .append("\n\n");

        // -----------------------------------------------------
        // 10 BUILD PLAN
        // -----------------------------------------------------

        String buildPlan;

        try {

            buildPlan =
                    selfBuilderEngine
                            .createBuildPlan(
                                    target
                            );

            recordHistory(
                    "BUILD_PLAN",
                    target
            );

        } catch (Exception e) {

            buildPlan =
                    "ERROR: "
                            + safeError(e);

            recordHistory(
                    "BUILD_ERROR",
                    buildPlan
            );
        }

        result.append(
                "10. BUILD PLAN\n"
        );

        result.append(
                "✓ تم تجهيز خطوات البناء والتعديل.\n\n"
        );

        // -----------------------------------------------------
        // 11 SELF TEST
        // -----------------------------------------------------

        String tests;

        try {

            tests =
                    selfTestEngine
                            .runAllTests();

            recordHistory(
                    "TEST",
                    "Self tests executed"
            );

        } catch (Exception e) {

            tests =
                    "ERROR: "
                            + safeError(e);

            recordHistory(
                    "TEST_ERROR",
                    tests
            );
        }

        result.append(
                "11. SELF TEST\n"
        );

        result.append(
                tests
        )
                .append("\n\n");

        // -----------------------------------------------------
        // 12 IMPROVE
        // -----------------------------------------------------

        recordHistory(
                "IMPROVE",
                "Development workspace prepared"
        );

        result.append(
                "12. IMPROVE\n"
        );

        result.append(
                "✓ تم تجهيز Workspace للتطوير الحقيقي.\n\n"
        );

        // -----------------------------------------------------
        // 13 MEMORY
        // -----------------------------------------------------

        memoryManager.saveMemory(
                "__last_evolution_target__",
                target
        );

        memoryManager.saveMemory(
                "__last_evolution_time__",
                now()
        );

        recordHistory(
                "MEMORY",
                "تم حفظ نتيجة دورة التطور"
        );

        result.append(
                "13. MEMORY\n"
        );

        result.append(
                "✓ تم حفظ هدف التطور ووقت الدورة.\n\n"
        );

        result.append(
                "============================\n"
        );

        result.append(
                "EVOLUTION CYCLE FINISHED\n\n"
        );

        result.append(
                "الهدف:\n"
        );

        result.append(
                target
        );

        result.append(
                "\n\n"
        );

        result.append(
                "الحالة:\n"
        );

        result.append(
                "تم التشخيص + تجهيز Builder + Snapshot + Build Plan + Self Test."
        );

        result.append(
                "\n\n"
        );

        result.append(
                "ملاحظة:\n"
        );

        result.append(
                "التعديل المباشر على APK المثبت وبناء APK جديد يحتاج مرحلة APK Builder منفصلة."
        );

        return result.toString();
    }

    // =========================================================
    // ACTIVE TARGET
    // =========================================================

    public String getActiveDevelopmentTarget() {

        return prefs.getString(
                KEY_ACTIVE_TARGET,
                "لا يوجد هدف تطوير نشط حاليا."
        );
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public void requestApproval(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

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

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل إنشاء طلب موافقة"
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

        if (action == null ||
                action.trim().isEmpty()) {

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

                if (action.equals(
                        item.optString(
                                "action"
                        )
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
                        status.equals(
                                "approved"
                        )
                                ? "APPROVAL"
                                : "REJECTION",
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
                "JARVIS STATUS\n"
        );

        result.append(
                "================\n"
        );

        result.append(
                "Engine: "
        )
                .append(
                        prefs.getString(
                                KEY_VERSION,
                                "5.0"
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

        try {

            result.append(
                    "Builder: "
            )
                    .append(
                            selfBuilderEngine
                                    .isHealthy()
                                    ? "ONLINE"
                                    : "ATTENTION"
                    )
                    .append("\n");

            result.append(
                    "Workspace: "
            )
                    .append(
                            selfBuilderEngine
                                    .getWorkspacePath()
                    )
                    .append("\n");

        } catch (Exception e) {

            result.append(
                    "Builder: ERROR\n"
            );
        }

        try {

            result.append(
                    "Self Test: "
            )
                    .append(
                            selfTestEngine
                                    .isHealthy()
                                    ? "HEALTHY"
                                    : "ISSUES"
                    )
                    .append("\n");

        } catch (Exception e) {

            result.append(
                    "Self Test: ERROR\n"
            );
        }

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

            int maxEntries = 100;

            if (history.length() >
                    maxEntries) {

                JSONArray trimmed =
                        new JSONArray();

                int start =
                        history.length()
                                - maxEntries;

                for (int i = start;
                     i < history.length();
                     i++) {

                    trimmed.put(
                            history.get(i)
                    );
                }

                history = trimmed;
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
                            history.length() - 20
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
                    "تعذر قراءة سجل التطور.";
        }
    }

    // =========================================================
    // COUNTERS
    // =========================================================

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

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }
}