package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS EVOLUTION ENGINE
 *
 * العقل المنسق للتطور الذاتي.
 *
 * المسؤوليات:
 * 1. التشخيص
 * 2. اختيار هدف التطوير
 * 3. إنشاء خطة التطوير
 * 4. إدارة المهارات والقدرات
 * 5. تشغيل Self Builder
 * 6. تعديل Source داخل Workspace
 * 7. Snapshot / Rollback
 * 8. Self Test
 * 9. APK Build
 * 10. تسجيل تاريخ التطور
 * 11. حفظ نتائج التطور في الذاكرة
 *
 * ملاحظة:
 * Android لا يسمح للتطبيق بتعديل APK المثبت مباشرة.
 * Self Builder يعدل Workspace ثم يمكن بناء APK جديد.
 */
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

    private final Context context;
    private final SharedPreferences prefs;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final SelfDiagnosisManager diagnosisManager;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SelfTestEngine selfTestEngine;
    private final ApkBuilderEngine apkBuilderEngine;

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

        apkBuilderEngine =
                new ApkBuilderEngine(
                        this.context
                );

        if (!prefs.contains(KEY_VERSION)) {

            prefs.edit()
                    .putString(
                            KEY_VERSION,
                            "7.0"
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

        if (name == null ||
                name.trim().isEmpty()) {

            return;
        }

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
    // REAL SOURCE EVOLUTION
    // =========================================================

    /**
     * تعديل حقيقي وآمن لملف داخل Workspace.
     *
     * لا يتم التعديل إلا إذا كان oldText موجودا مرة واحدة.
     */
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
                    reason == null ||
                            reason.trim().isEmpty()
                            ? "Evolution target: " + target
                            : reason;

            recordHistory(
                    "SOURCE_EVOLUTION_START",
                    path + " | " + finalReason
            );

            String result =
                    selfBuilderEngine
                            .evolveSourceFile(
                                    path,
                                    oldText,
                                    newText,
                                    finalReason
                            );

            boolean success =
                    result != null &&
                            !result
                                    .toLowerCase(
                                            Locale.US
                                    )
                                    .contains(
                                            "failed"
                                    ) &&
                            !result
                                    .contains(
                                            "فشل"
                                    );

            if (success) {

                recordHistory(
                        "SOURCE_EVOLUTION_SUCCESS",
                        path
                );

                memoryManager.saveMemory(
                        "__last_source_evolution__",
                        path
                );

                memoryManager.saveMemory(
                        "__last_source_evolution_time__",
                        now()
                );

            } else {

                recordHistory(
                        "SOURCE_EVOLUTION_FAILED",
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

    /**
     * إنشاء/استبدال ملف داخل Workspace.
     */
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

            recordHistory(
                    "SOURCE_WRITE_ERROR",
                    safeError(e)
            );

            return
                    "فشل كتابة Source:\n"
                    + safeError(e);
        }
    }

    /**
     * قراءة Source.
     */
    public String readSourceFile(
            String path
    ) {

        try {

            return selfBuilderEngine
                    .readSourceFile(
                            path
                    );

        } catch (Exception e) {

            return
                    "فشل قراءة Source:\n"
                    + safeError(e);
        }
    }

    /**
     * بحث داخل Source.
     */
    public String searchSource(
            String query
    ) {

        try {

            return selfBuilderEngine
                    .searchSource(
                            query
                    );

        } catch (Exception e) {

            return
                    "فشل البحث داخل Source:\n"
                    + safeError(e);
        }
    }

    /**
     * إنشاء Snapshot قبل التعديل.
     */
    public String createDevelopmentSnapshot(
            String reason
    ) {

        try {

            String result =
                    selfBuilderEngine
                            .createSnapshot(
                                    reason
                            );

            recordHistory(
                    "SNAPSHOT",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "SNAPSHOT_ERROR",
                    safeError(e)
            );

            return
                    "فشل Snapshot:\n"
                    + safeError(e);
        }
    }

    /**
     * Rollback إلى Snapshot.
     */
    public String rollbackDevelopment(
            String snapshotId
    ) {

        try {

            String result =
                    selfBuilderEngine
                            .rollback(
                                    snapshotId
                            );

            recordHistory(
                    "ROLLBACK",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "ROLLBACK_ERROR",
                    safeError(e)
            );

            return
                    "فشل Rollback:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // APK BUILDER
    // =========================================================

    public String validateApkProject() {

        try {

            String result =
                    apkBuilderEngine
                            .validateProject();

            recordHistory(
                    "APK_PROJECT_CHECK",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "APK_PROJECT_ERROR",
                    safeError(e)
            );

            return
                    "فشل فحص مشروع APK:\n"
                    + safeError(e);
        }
    }

    public String prepareApkBuild(
            String reason
    ) {

        try {

            String result =
                    apkBuilderEngine
                            .prepareBuildRequest(
                                    reason
                            );

            recordHistory(
                    "APK_BUILD_REQUEST",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "APK_BUILD_REQUEST_ERROR",
                    safeError(e)
            );

            return
                    "فشل تجهيز APK Build:\n"
                    + safeError(e);
        }
    }

    public String buildDebugApk() {

        try {

            recordHistory(
                    "APK_BUILD_START",
                    "بدأ طلب بناء APK Debug"
            );

            String result =
                    apkBuilderEngine
                            .buildDebugApk();

            recordHistory(
                    "APK_BUILD_RESULT",
                    result
            );

            return result;

        } catch (Exception e) {

            recordHistory(
                    "APK_BUILD_ERROR",
                    safeError(e)
            );

            return
                    "فشل APK Build:\n"
                    + safeError(e);
        }
    }

    public String getApkBuildStatus() {

        try {

            return apkBuilderEngine
                    .getStatus();

        } catch (Exception e) {

            return
                    "APK Builder غير متاح:\n"
                    + safeError(e);
        }
    }

    public String getLatestApk() {

        try {

            return apkBuilderEngine
                    .findLatestApk();

        } catch (Exception e) {

            return
                    "تعذر البحث عن APK:\n"
                    + safeError(e);
        }
    }

    public String getApkBuildHistory() {

        try {

            return apkBuilderEngine
                    .getBuildHistory();

        } catch (Exception e) {

            return
                    "تعذر قراءة Build History.";
        }
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
                "JARVIS EVOLUTION ENGINE 7.0\n"
        );

        result.append(
                "============================\n\n"
        );

        // =====================================================
        // 1 OBSERVE
        // =====================================================

        recordHistory(
                "OBSERVE",
                "تم فحص حالة النظام"
        );

        result.append(
                "1. OBSERVE\n"
        );

        result.append(
                "✓ تم فحص حالة النظام.\n\n"
        );

        // =====================================================
        // 2 DIAGNOSIS
        // =====================================================

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
                "✓ الجاهزية: "
        )
                .append(
                        readiness
                )
                .append("%\n\n");

        // =====================================================
        // 3 CAPABILITIES
        // =====================================================

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

        // =====================================================
        // 4 TARGET
        // =====================================================

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

        // =====================================================
        // 5 PLAN
        // =====================================================

        String developmentPlan;

        try {

            developmentPlan =
                    diagnosisManager
                            .getDevelopmentPlan();

        } catch (Exception e) {

            developmentPlan =
                    "الخطة غير متاحة.";
        }

        result.append(
                "5. DEVELOPMENT PLAN\n"
        );

        result.append(
                developmentPlan
        )
                .append("\n\n");

        recordHistory(
                "DEVELOPMENT_PLAN",
                target
        );

        // =====================================================
        // 6 BUILDER INIT
        // =====================================================

        String builderInit;

        try {

            builderInit =
                    selfBuilderEngine.initialize();

            selfBuilderEngine
                    .registerDevelopment(
                            target,
                            "Automatic evolution target"
                    );

        } catch (Exception e) {

            builderInit =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "6. SELF BUILDER\n"
        );

        result.append(
                builderInit
        )
                .append("\n\n");

        recordHistory(
                "BUILDER_READY",
                builderInit
        );

        // =====================================================
        // 7 SELF TEST BEFORE
        // =====================================================

        String beforeTest;

        try {

            beforeTest =
                    selfTestEngine
                            .runAllTests();

        } catch (Exception e) {

            beforeTest =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "7. PRE-EVOLUTION TEST\n"
        );

        result.append(
                beforeTest
        )
                .append("\n\n");

        recordHistory(
                "PRE_TEST",
                "تم الاختبار قبل التطوير"
        );

        // =====================================================
        // 8 SNAPSHOT
        // =====================================================

        String snapshot;

        try {

            snapshot =
                    selfBuilderEngine
                            .createSnapshot(
                                    "Before evolution: "
                                            + target
                            );

        } catch (Exception e) {

            snapshot =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "8. SAFETY SNAPSHOT\n"
        );

        result.append(
                snapshot
        )
                .append("\n\n");

        recordHistory(
                "SNAPSHOT",
                snapshot
        );

        // =====================================================
        // 9 BUILD PLAN
        // =====================================================

        String buildPlan;

        try {

            buildPlan =
                    selfBuilderEngine
                            .createBuildPlan(
                                    target
                            );

        } catch (Exception e) {

            buildPlan =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "9. CODE BUILD PLAN\n"
        );

        result.append(
                buildPlan
        )
                .append("\n\n");

        recordHistory(
                "BUILD_PLAN",
                target
        );

        // =====================================================
        // 10 APK PROJECT VALIDATION
        // =====================================================

        String projectStatus;

        try {

            projectStatus =
                    apkBuilderEngine
                            .validateProject();

        } catch (Exception e) {

            projectStatus =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "10. APK PROJECT CHECK\n"
        );

        result.append(
                projectStatus
        )
                .append("\n\n");

        recordHistory(
                "APK_PROJECT_VALIDATION",
                projectStatus
        );

        // =====================================================
        // 11 BUILD REQUEST
        // =====================================================

        String buildRequest;

        try {

            buildRequest =
                    apkBuilderEngine
                            .prepareBuildRequest(
                                    "Evolution target: "
                                            + target
                            );

        } catch (Exception e) {

            buildRequest =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "11. APK BUILD REQUEST\n"
        );

        result.append(
                buildRequest
        )
                .append("\n\n");

        recordHistory(
                "APK_BUILD_PREPARED",
                target
        );

        // =====================================================
        // 12 POST TEST
        // =====================================================

        String afterTest;

        try {

            afterTest =
                    selfTestEngine
                            .runAllTests();

        } catch (Exception e) {

            afterTest =
                    "ERROR: "
                            + safeError(e);
        }

        result.append(
                "12. POST-EVOLUTION TEST\n"
        );

        result.append(
                afterTest
        )
                .append("\n\n");

        recordHistory(
                "POST_TEST",
                "تم اختبار النظام بعد تجهيز دورة التطور"
        );

        // =====================================================
        // 13 MEMORY
        // =====================================================

        memoryManager.saveMemory(
                "__last_evolution_target__",
                target
        );

        memoryManager.saveMemory(
                "__last_evolution_time__",
                now()
        );

        memoryManager.saveMemory(
                "__last_apk_build_status__",
                apkBuilderEngine.getStatus()
        );

        memoryManager.saveMemory(
                "__last_evolution_workspace__",
                selfBuilderEngine.getWorkspacePath()
        );

        recordHistory(
                "MEMORY",
                "تم حفظ نتائج دورة التطور"
        );

        result.append(
                "13. LEARNING MEMORY\n"
        );

        result.append(
                "✓ تم حفظ الهدف والنتائج والحالة.\n\n"
        );

        // =====================================================
        // 14 FINAL
        // =====================================================

        String finalStatus;

        if (afterTest != null &&
                afterTest.contains(
                        "SYSTEM STATUS: HEALTHY"
                )) {

            finalStatus =
                    "HEALTHY";

        } else {

            finalStatus =
                    "ATTENTION";
        }

        result.append(
                "14. EVOLUTION RESULT\n"
        );

        result.append(
                "Status: "
        )
                .append(
                        finalStatus
                )
                .append("\n");

        result.append(
                "Target: "
        )
                .append(
                        target
                )
                .append("\n\n");

        result.append(
                "============================\n"
        );

        result.append(
                "EVOLUTION CYCLE FINISHED\n\n"
        );

        result.append(
                "JARVIS يقدر دابا يدير:\n"
        );

        result.append(
                "✓ Diagnosis\n"
        );

        result.append(
                "✓ Goal Selection\n"
        );

        result.append(
                "✓ Development Planning\n"
        );

        result.append(
                "✓ Workspace Evolution\n"
        );

        result.append(
                "✓ Source Modification API\n"
        );

        result.append(
                "✓ Snapshot\n"
        );

        result.append(
                "✓ Rollback\n"
        );

        result.append(
                "✓ Self Testing\n"
        );

        result.append(
                "✓ APK Build Pipeline\n"
        );

        result.append(
                "✓ Evolution Memory\n\n"
        );

        result.append(
                "مهم: ما كيتحسبش APK مبني حتى يكون APK فعلي موجود."
        );

        String finalResult =
                result.toString();

        prefs.edit()
                .putString(
                        KEY_LAST_RESULT,
                        finalResult
                )
                .apply();

        return finalResult;
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
                    "فشل إنشاء طلب الموافقة: "
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
                                "7.0"
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

        try {

            result.append(
                    "Readiness: "
            )
                    .append(
                            diagnosisManager
                                    .getReadinessScore()
                    )
                    .append("%\n");

        } catch (Exception e) {

            result.append(
                    "Readiness: ERROR\n"
            );
        }

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
                    "Workspace: "
            )
                    .append(
                            selfBuilderEngine
                                    .getWorkspacePath()
                    )
                    .append("\n");

        } catch (Exception e) {

            result.append(
                    "Self Builder: ERROR\n"
            );
        }

        try {

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
                    "APK Status: "
            )
                    .append(
                            apkBuilderEngine
                                    .getStatus()
                    )
                    .append("\n");

        } catch (Exception e) {

            result.append(
                    "APK Builder: ERROR\n"
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
                "Last Evolution: "
        )
                .append(
                        memoryManager.getMemory(
                                "__last_evolution_time__"
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

            int maxEntries = 100;

            while (history.length() >
                    maxEntries) {

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