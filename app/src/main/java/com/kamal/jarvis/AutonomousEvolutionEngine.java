package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * JARVIS Autonomous Evolution Engine
 *
 * المسؤول عن دورة التطوير الآمنة:
 *
 * Analyze
 * -> Baseline Test
 * -> Snapshot
 * -> Modify
 * -> Verify
 * -> Self Test
 * -> Rollback عند الفشل
 * -> Save Result
 * -> Prepare Build
 *
 * ملاحظة:
 * هذا النظام يعدل Workspace الخاص بـ JARVIS.
 * لا يدعي أنه يعدل APK المثبت مباشرة.
 */
public class AutonomousEvolutionEngine {

    private static final String PREFS =
            "JARVIS_AUTONOMOUS_EVOLUTION";

    private static final String HISTORY_KEY =
            "history";

    private final Context context;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SelfTestEngine selfTestEngine;
    private final CodeEvolutionEngine codeEvolutionEngine;
    private final SelfDiagnosisManager diagnosisManager;
    private final ApkBuilderEngine apkBuilderEngine;
    private final MemoryManager memoryManager;

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
                && diagnosisManager != null;
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

            // -------------------------------------------------
            // 1. Verify file
            // -------------------------------------------------

            if (!selfBuilderEngine.fileExists(
                    path
            )) {

                return
                        "Evolution مرفوض:\n"
                                + "الملف غير موجود في Workspace:\n"
                                + path;
            }

            // -------------------------------------------------
            // 2. Baseline test
            // -------------------------------------------------

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
                                + baseline
                                + "\n\n"
                                + "ما غاديش نعدل الكود حتى يكون النظام مستقر.";
            }

            // -------------------------------------------------
            // 3. Create snapshot
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 4. Apply real modification
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 5. Verify file changed
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 6. Run system tests
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 7. Prepare APK build
            // -------------------------------------------------

            String buildRequest =
                    apkBuilderEngine.prepareBuildRequest(
                            "Successful autonomous evolution: "
                                    + reason
                    );

            // -------------------------------------------------
            // 8. Save learning memory
            // -------------------------------------------------

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

            // -------------------------------------------------
            // 9. History
            // -------------------------------------------------

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

            // Baseline
            String baseline =
                    selfTestEngine.runAllTests();

            if (!testPassed(baseline)) {

                return
                        "إنشاء الملف توقف.\n\n"
                                + "Baseline tests failed.\n\n"
                                + baseline;
            }

            // Do not overwrite existing files
            if (selfBuilderEngine.fileExists(
                    path
            )) {

                return
                        "إنشاء الملف مرفوض.\n\n"
                                + "الملف موجود أصلا:\n"
                                + path;
            }

            // Snapshot
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

            // Create
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

            // Test
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

            // Build request
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

            String analysis =
                    analyzeBeforeEvolution(
                            goal
                    );

            String snapshot =
                    selfBuilderEngine.createSnapshot(
                            "Full autonomous cycle: "
                                    + goal
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
                            "Autonomous cycle: "
                                    + goal
                    );

            recordHistory(
                    "CYCLE_PREPARED",
                    goal
                            + " | snapshot="
                            + snapshotId
            );

            return
                    "AUTONOMOUS EVOLUTION CYCLE READY ✓\n\n"
                            + "GOAL:\n"
                            + goal
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
                            + "خاص الكود الجديد يتحدد قبل تطبيقه؛ "
                            + "ما غاديش JARVIS يخترع patch عشوائي ويدخلو مباشرة.";

        } catch (Exception e) {

            return
                    "Evolution Cycle Failed:\n"
                            + safeError(e);
        }
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

            // Keep last 100 operations
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