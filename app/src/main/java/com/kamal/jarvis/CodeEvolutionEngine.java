package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS CODE EVOLUTION ENGINE
 *
 * محرك فهم وتطوير كود JARVIS.
 *
 * المسؤوليات:
 * - تحليل المشروع
 * - البحث وقراءة الملفات
 * - تحليل البنية والـ dependencies
 * - إنشاء ملفات Java
 * - تعديل الكود بطريقة آمنة
 * - Snapshot / Rollback
 * - خطط التطوير
 * - حفظ تاريخ التطور
 * - ربط التطوير مع SelfBuilderEngine
 *
 * ملاحظة:
 * هذا المحرك يطور Workspace الداخلي.
 * بناء APK حقيقي يتم عبر ApkBuilderEngine عندما تتوفر بيئة Build.
 */
public class CodeEvolutionEngine {

    private static final String PREFS_NAME =
            "jarvis_code_evolution";

    private static final String KEY_HISTORY =
            "history";

    private static final String KEY_LAST_ACTION =
            "last_action";

    private static final String KEY_LAST_PLAN =
            "last_plan";

    private final Context context;

    private final SelfBuilderEngine selfBuilder;

    private final MemoryManager memoryManager;

    private final SharedPreferences prefs;

    private final List<String> sessionHistory =
            new ArrayList<>();

    public CodeEvolutionEngine(Context context) {

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

        selfBuilder =
                new SelfBuilderEngine(
                        this.context
                );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        if (!isHealthy()) {

            return
                    "CODE EVOLUTION ENGINE: ATTENTION ⚠";
        }

        return
                "CODE EVOLUTION ENGINE: ONLINE ✓\n"
                + "Project Analysis: READY ✓\n"
                + "Code Search: READY ✓\n"
                + "Code Generation: READY ✓\n"
                + "Code Modification: READY ✓\n"
                + "Snapshot: READY ✓\n"
                + "Rollback: READY ✓\n"
                + "Dependency Analysis: READY ✓\n"
                + "History: READY ✓";
    }

    public boolean isHealthy() {

        try {

            return selfBuilder != null
                    && selfBuilder.isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // PROJECT ANALYSIS
    // =========================================================

    public String analyzeProject() {

        try {

            String files =
                    selfBuilder.listProjectFiles();

            String architecture =
                    detectArchitecture();

            String dependencies =
                    analyzeDependencies();

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS PROJECT ANALYSIS\n"
            );

            result.append(
                    "============================\n\n"
            );

            result.append(
                    "PROJECT FILES\n"
            );

            result.append(
                    files
            );

            result.append(
                    "\n\nARCHITECTURE\n"
            );

            result.append(
                    architecture
            );

            result.append(
                    "\n\nDEPENDENCIES\n"
            );

            result.append(
                    dependencies
            );

            result.append(
                    "\n\nEVOLUTION STATUS\n"
            );

            result.append(
                    "Workspace: "
            )
                    .append(
                            selfBuilder.isHealthy()
                                    ? "ONLINE"
                                    : "ATTENTION"
                    )
                    .append("\n");

            result.append(
                    "Safe modification: READY\n"
            );

            result.append(
                    "Snapshot/Rollback: READY\n"
            );

            result.append(
                    "APK Build: delegated to ApkBuilderEngine"
            );

            String finalResult =
                    result.toString();

            memoryManager.saveMemory(
                    "__last_code_analysis__",
                    finalResult
            );

            record(
                    "PROJECT_ANALYSIS",
                    "تم تحليل Workspace"
            );

            return finalResult;

        } catch (Exception e) {

            return
                    "فشل تحليل المشروع:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // ARCHITECTURE
    // =========================================================

    public String detectArchitecture() {

        StringBuilder result =
                new StringBuilder();

        try {

            String javaSearch =
                    selfBuilder.searchSource(
                            ".java"
                    );

            boolean java =
                    containsIgnoreCase(
                            javaSearch,
                            ".java"
                    );

            String xmlSearch =
                    selfBuilder.searchSource(
                            ".xml"
                    );

            boolean xml =
                    containsIgnoreCase(
                            xmlSearch,
                            ".xml"
                    );

            String gradleSearch =
                    selfBuilder.searchSource(
                            "gradle"
                    );

            boolean gradle =
                    containsIgnoreCase(
                            gradleSearch,
                            "gradle"
                    );

            if (java) {

                result.append(
                        "✓ Java Android source\n"
                );

            } else {

                result.append(
                        "⚠ Java source غير مكتشف\n"
                );
            }

            if (xml) {

                result.append(
                        "✓ Android XML resources\n"
                );

            } else {

                result.append(
                        "⚠ XML resources غير مكتشفة\n"
                );
            }

            if (gradle) {

                result.append(
                        "✓ Gradle configuration\n"
                );

            } else {

                result.append(
                        "⚠ Gradle configuration غير مكتشفة\n"
                );
            }

            result.append(
                    "✓ Workspace architecture scan complete"
            );

            return result.toString();

        } catch (Exception e) {

            return
                    "Architecture scan failed:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    public String analyzeDependencies() {

        try {

            String result =
                    selfBuilder.searchSource(
                            "implementation"
                    );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "Gradle dependency scan:\n\n"
            );

            if (isSearchEmpty(result)) {

                output.append(
                        "ما بان حتى dependency من نوع implementation.\n"
                );

            } else {

                output.append(
                        result
                )
                        .append("\n");
            }

            output.append(
                    "\nJARVIS dependency policy:\n"
            );

            output.append(
                    "• تجنب dependencies غير الضرورية\n"
            );

            output.append(
                    "• فضل Android APIs الموجودة\n"
            );

            output.append(
                    "• تحقق من compileSdk قبل إضافة مكتبة\n"
            );

            output.append(
                    "• لا تضف خدمات مدفوعة تلقائيا\n"
            );

            output.append(
                    "• أي dependency جديدة خاصها تحقق قبل اعتمادها"
            );

            return output.toString();

        } catch (Exception e) {

            return
                    "Dependency analysis failed:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // SEARCH CODE
    // =========================================================

    public String searchCode(
            String query
    ) {

        if (isBlank(query)) {

            return
                    "خاصني كلمة أو جملة للبحث.";
        }

        return selfBuilder.searchSource(
                query.trim()
        );
    }

    // =========================================================
    // READ CODE
    // =========================================================

    public String readCode(
            String path
    ) {

        if (isBlank(path)) {

            return
                    "حدد مسار الملف.";
        }

        return selfBuilder.readSourceFile(
                path.trim()
        );
    }

    // =========================================================
    // CREATE CODE FILE
    // =========================================================

    public String createCodeFile(
            String path,
            String content,
            String reason
    ) {

        if (!validSourcePath(path)) {

            return
                    "إنشاء الملف مرفوض:\n"
                    + "المسار غير صالح.";
        }

        if (content == null) {

            return
                    "إنشاء الملف مرفوض:\n"
                    + "المحتوى فارغ.";
        }

        try {

            String snapshot =
                    selfBuilder.createSnapshot(
                            "قبل إنشاء ملف: "
                                    + safeText(reason)
                    );

            String result =
                    selfBuilder.writeSourceFile(
                            path.trim(),
                            content
                    );

            if (!isOperationSuccessful(
                    result
            )) {

                return
                        "إنشاء الملف فشل ⚠\n\n"
                        + result
                        + "\n\nSnapshot:\n"
                        + snapshot;
            }

            memoryManager.saveMemory(
                    "__last_created_source__",
                    path.trim()
            );

            memoryManager.saveMemory(
                    "__last_code_change__",
                    path.trim()
            );

            record(
                    "CREATE_FILE",
                    path.trim()
            );

            return
                    "CODE FILE CREATED ✓\n\n"
                    + "File:\n"
                    + path.trim()
                    + "\n\n"
                    + "Snapshot:\n"
                    + snapshot
                    + "\n\n"
                    + result;

        } catch (Exception e) {

            return
                    "فشل إنشاء الملف:\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // MODIFY CODE
    // =========================================================

    public String modifyCode(
            String path,
            String oldCode,
            String newCode,
            String reason
    ) {

        if (!validSourcePath(path)) {

            return
                    "تعديل الكود مرفوض:\n"
                    + "المسار غير صالح.";
        }

        if (isBlank(oldCode)) {

            return
                    "التعديل مرفوض:\n"
                    + "oldCode فارغ.";
        }

        if (newCode == null) {

            newCode = "";
        }

        try {

            String snapshot =
                    selfBuilder.createSnapshot(
                            "قبل تعديل الكود: "
                                    + safeText(reason)
                    );

            String result =
                    selfBuilder.evolveSourceFile(
                            path.trim(),
                            oldCode,
                            newCode,
                            reason
                    );

            if (!isEvolutionSuccess(
                    result
            )) {

                record(
                        "MODIFY_FAILED",
                        path.trim()
                );

                return
                        "CODE EVOLUTION FAILED ⚠\n\n"
                        + result
                        + "\n\n"
                        + "Snapshot محفوظ:\n"
                        + snapshot;
            }

            memoryManager.saveMemory(
                    "__last_code_change__",
                    path.trim()
            );

            memoryManager.saveMemory(
                    "__last_code_change_reason__",
                    safeText(reason)
            );

            memoryManager.saveMemory(
                    "__last_successful_code_evolution__",
                    result
            );

            record(
                    "MODIFY_CODE",
                    path.trim()
            );

            return
                    "CODE EVOLUTION SUCCESS ✓\n\n"
                    + result
                    + "\n\n"
                    + "Safety Snapshot:\n"
                    + snapshot;

        } catch (Exception e) {

            return
                    "CODE EVOLUTION FAILED ⚠\n\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // GENERATE JAVA CLASS
    // =========================================================

    public String generateJavaClass(
            String packageName,
            String className,
            String description
    ) {

        if (!validIdentifier(className)) {

            return
                    "اسم Class غير صالح.";
        }

        String cleanPackage =
                normalizePackage(packageName);

        String cleanClass =
                className.trim();

        String cleanDescription =
                safeText(description);

        StringBuilder code =
                new StringBuilder();

        code.append(
                "package "
        )
                .append(cleanPackage)
                .append(";\n\n");

        code.append(
                "/**\n"
        );

        code.append(
                " * JARVIS generated component.\n"
        );

        code.append(
                " * "
        )
                .append(
                        cleanDescription
                )
                .append(
                        "\n"
                );

        code.append(
                " */\n"
        );

        code.append(
                "public class "
        )
                .append(cleanClass)
                .append(
                        " {\n\n"
                );

        code.append(
                "    public String getName() {\n"
        );

        code.append(
                "        return \""
        )
                .append(
                        escapeJavaString(
                                cleanClass
                        )
                )
                .append(
                        "\";\n"
                );

        code.append(
                "    }\n"
        );

        code.append(
                "}\n"
        );

        return code.toString();
    }

    // =========================================================
    // GENERATE INTERFACE
    // =========================================================

    public String generateJavaInterface(
            String packageName,
            String interfaceName,
            String description
    ) {

        if (!validIdentifier(
                interfaceName
        )) {

            return
                    "اسم Interface غير صالح.";
        }

        String cleanPackage =
                normalizePackage(packageName);

        return
                "package "
                + cleanPackage
                + ";\n\n"
                + "/**\n"
                + " * "
                + safeText(description)
                + "\n"
                + " */\n"
                + "public interface "
                + interfaceName.trim()
                + " {\n\n"
                + "}\n";
    }

    // =========================================================
    // GENERATE ENUM
    // =========================================================

    public String generateJavaEnum(
            String packageName,
            String enumName,
            String[] values
    ) {

        if (!validIdentifier(
                enumName
        )) {

            return
                    "اسم Enum غير صالح.";
        }

        String cleanPackage =
                normalizePackage(packageName);

        StringBuilder code =
                new StringBuilder();

        code.append(
                "package "
        )
                .append(cleanPackage)
                .append(
                        ";\n\n"
                );

        code.append(
                "public enum "
        )
                .append(
                        enumName.trim()
                )
                .append(
                        " {\n"
                );

        List<String> validValues =
                new ArrayList<>();

        if (values != null) {

            for (String value :
                    values) {

                if (validIdentifier(
                        value
                )) {

                    validValues.add(
                            value.trim()
                    );
                }
            }
        }

        for (int i = 0;
             i < validValues.size();
             i++) {

            code.append(
                    "    "
            )
                    .append(
                            validValues.get(i)
                    );

            if (i <
                    validValues.size() - 1) {

                code.append(",");
            }

            code.append("\n");
        }

        code.append(
                "}\n"
        );

        return code.toString();
    }

    // =========================================================
    // DEVELOPMENT PLAN
    // =========================================================

    public String generateDevelopmentPlan(
            String goal
    ) {

        String cleanGoal =
                safeText(goal);

        JSONArray steps =
                new JSONArray();

        addPlanStep(
                steps,
                1,
                "Analyze",
                "تحليل الهدف والمشروع"
        );

        addPlanStep(
                steps,
                2,
                "Locate",
                "تحديد الملفات والأنظمة المرتبطة"
        );

        addPlanStep(
                steps,
                3,
                "Snapshot",
                "حفظ نسخة قبل التعديل"
        );

        addPlanStep(
                steps,
                4,
                "Generate",
                "إنشاء أو توليد الكود المطلوب"
        );

        addPlanStep(
                steps,
                5,
                "Modify",
                "تطبيق التعديل بشكل آمن"
        );

        addPlanStep(
                steps,
                6,
                "Verify",
                "التحقق من المحتوى والـHash"
        );

        addPlanStep(
                steps,
                7,
                "Test",
                "تشغيل الاختبارات"
        );

        addPlanStep(
                steps,
                8,
                "Build",
                "طلب Build للـAPK إذا كانت البيئة متوفرة"
        );

        addPlanStep(
                steps,
                9,
                "Learn",
                "حفظ النتيجة والتعلم منها"
        );

        JSONObject plan =
                new JSONObject();

        try {

            plan.put(
                    "goal",
                    cleanGoal
            );

            plan.put(
                    "created",
                    System.currentTimeMillis()
            );

            plan.put(
                    "steps",
                    steps
            );

        } catch (Exception ignored) {
        }

        String result =
                plan.toString();

        prefs.edit()
                .putString(
                        KEY_LAST_PLAN,
                        result
                )
                .apply();

        memoryManager.saveMemory(
                "__last_code_plan__",
                result
        );

        record(
                "PLAN",
                cleanGoal
        );

        return result;
    }

    // =========================================================
    // SNAPSHOT
    // =========================================================

    public String snapshot(
            String reason
    ) {

        String result =
                selfBuilder.createSnapshot(
                        reason
                );

        record(
                "SNAPSHOT",
                safeText(reason)
        );

        return result;
    }

    // =========================================================
    // ROLLBACK
    // =========================================================

    public String rollback(
            String snapshotId
    ) {

        if (isBlank(snapshotId)) {

            return
                    "خاصك تحدد Snapshot.";
        }

        String result =
                selfBuilder.rollback(
                        snapshotId.trim()
                );

        record(
                "ROLLBACK",
                snapshotId.trim()
        );

        return result;
    }

    // =========================================================
    // PROJECT INVENTORY
    // =========================================================

    public String getProjectInventory() {

        return selfBuilder.listProjectFiles();
    }

    // =========================================================
    // LAST PLAN
    // =========================================================

    public String getLastPlan() {

        String plan =
                prefs.getString(
                        KEY_LAST_PLAN,
                        ""
                );

        if (!isBlank(plan)) {

            return plan;
        }

        plan =
                memoryManager.getMemory(
                        "__last_code_plan__"
                );

        if (isBlank(plan)) {

            return
                    "ما كايناش خطة كود محفوظة.";
        }

        return plan;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public String getHistory() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            if (history.length() == 0
                    && sessionHistory.isEmpty()) {

                return
                        "ما كاين حتى Evolution Engine history حاليا.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "CODE EVOLUTION HISTORY\n\n"
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
                )
                        .append(": ");

                result.append(
                        item.optString(
                                "message"
                        )
                )
                        .append("\n");
            }

            for (String item :
                    sessionHistory) {

                result.append(
                        "• "
                )
                        .append(item)
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
    // LAST ACTION
    // =========================================================

    public String getLastAction() {

        String action =
                prefs.getString(
                        KEY_LAST_ACTION,
                        ""
                );

        if (isBlank(action)) {

            return
                    "ما كاين حتى إجراء مسجل.";
        }

        return action;
    }

    // =========================================================
    // PATH SAFETY
    // =========================================================

    private boolean validSourcePath(
            String path
    ) {

        if (path == null ||
                path.trim().isEmpty()) {

            return false;
        }

        String clean =
                path.trim()
                        .replace(
                                '\\',
                                '/'
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (clean.startsWith("/")
                || clean.contains("..")
                || clean.contains("\0")) {

            return false;
        }

        return
                clean.endsWith(".java")
                || clean.endsWith(".xml")
                || clean.endsWith(".gradle")
                || clean.endsWith(".properties")
                || clean.endsWith(".json")
                || clean.endsWith(".kt")
                || clean.endsWith(".txt")
                || clean.endsWith(".md");
    }

    // =========================================================
    // JAVA IDENTIFIER
    // =========================================================

    private boolean validIdentifier(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return false;
        }

        String clean =
                value.trim();

        if (!Character.isJavaIdentifierStart(
                clean.charAt(0)
        )) {

            return false;
        }

        for (int i = 1;
             i < clean.length();
             i++) {

            if (!Character.isJavaIdentifierPart(
                    clean.charAt(i)
            )) {

                return false;
            }
        }

        return true;
    }

    // =========================================================
    // PACKAGE
    // =========================================================

    private String normalizePackage(
            String packageName
    ) {

        if (isBlank(packageName)) {

            return
                    "com.kamal.jarvis";
        }

        String clean =
                packageName.trim()
                        .replace(
                                '/',
                                '.'
                        );

        String[] parts =
                clean.split("\\.");

        StringBuilder result =
                new StringBuilder();

        for (String part :
                parts) {

            if (!validIdentifier(part)) {
                continue;
            }

            if (result.length() > 0) {
                result.append(".");
            }

            result.append(part);
        }

        if (result.length() == 0) {

            return
                    "com.kamal.jarvis";
        }

        return result.toString();
    }

    // =========================================================
    // PLAN HELPER
    // =========================================================

    private void addPlanStep(
            JSONArray array,
            int number,
            String name,
            String description
    ) {

        try {

            JSONObject item =
                    new JSONObject();

            item.put(
                    "step",
                    number
            );

            item.put(
                    "name",
                    name
            );

            item.put(
                    "description",
                    description
            );

            array.put(item);

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private synchronized void record(
            String type,
            String message
    ) {

        String value =
                type
                        + " | "
                        + safeText(message);

        sessionHistory.add(
                value
        );

        while (
                sessionHistory.size() > 50
        ) {

            sessionHistory.remove(0);
        }

        prefs.edit()
                .putString(
                        KEY_LAST_ACTION,
                        value
                )
                .apply();

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
                    safeText(message)
            );

            item.put(
                    "time",
                    System.currentTimeMillis()
            );

            history.put(item);

            while (
                    history.length() > 100
            ) {

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

        memoryManager.saveMemory(
                "__code_evolution_last_action__",
                value
        );
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    private boolean isOperationSuccessful(
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

        return
                !lower.contains("فشل")
                && !lower.contains("failed")
                && !lower.contains("error")
                && !lower.contains("مرفوض");
    }

    private boolean isEvolutionSuccess(
            String result
    ) {

        if (result == null) {

            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.ROOT
                );

        return
                lower.contains(
                        "evolution success"
                )
                || lower.contains(
                        "تعديل الكود ناجح"
                )
                || lower.contains(
                        "code evolution success"
                );
    }

    // =========================================================
    // SEARCH HELPERS
    // =========================================================

    private boolean containsIgnoreCase(
            String source,
            String query
    ) {

        if (source == null ||
                query == null) {

            return false;
        }

        return source.toLowerCase(
                Locale.ROOT
        ).contains(
                query.toLowerCase(
                        Locale.ROOT
                )
        );
    }

    private boolean isSearchEmpty(
            String value
    ) {

        if (isBlank(value)) {

            return true;
        }

        String lower =
                value.toLowerCase(
                        Locale.ROOT
                );

        return
                lower.contains(
                        "ما لقيتش"
                )
                || lower.contains(
                        "not found"
                )
                || lower.contains(
                        "no results"
                );
    }

    // =========================================================
    // JAVA ESCAPE
    // =========================================================

    private String escapeJavaString(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    // =========================================================
    // SAFE TEXT
    // =========================================================

    private String safeText(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "unspecified";
        }

        return value.trim();
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

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return context;
    }
}