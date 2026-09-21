package com.kamal.jarvis;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS CODE EVOLUTION ENGINE
 *
 * محرك فهم وتطوير الكود.
 *
 * المسؤوليات:
 * 1. تحليل ملفات المشروع
 * 2. البحث عن الكود
 * 3. اكتشاف Java/XML/Gradle
 * 4. إنشاء ملفات جديدة
 * 5. تعديل الملفات عن طريق Patch آمن
 * 6. إنشاء Snapshot قبل التطوير
 * 7. حفظ سجل التطورات
 * 8. تحليل dependencies
 * 9. إنشاء خطة تطوير
 * 10. ربط التطوير مع SelfBuilderEngine
 *
 * مهم:
 * هذا المحرك يعمل داخل Workspace الخاص بـ JARVIS.
 * لا يدعي أنه يستطيع تعديل APK المثبت مباشرة.
 */
public class CodeEvolutionEngine {

    private final Context context;
    private final SelfBuilderEngine selfBuilder;

    private final MemoryManager memoryManager;

    private final List<String> evolutionHistory =
            new ArrayList<>();

    public CodeEvolutionEngine(Context context) {

        this.context =
                context.getApplicationContext();

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

        if (selfBuilder.isHealthy()) {

            return
                    "CODE EVOLUTION ENGINE: ONLINE ✓\n"
                    + "Code Analysis: READY ✓\n"
                    + "Code Generation: READY ✓\n"
                    + "Code Modification: READY ✓\n"
                    + "Snapshot: READY ✓\n"
                    + "Rollback: READY ✓\n"
                    + "Dependency Analysis: READY ✓";

        }

        return
                "CODE EVOLUTION ENGINE: ATTENTION ⚠";
    }

    public boolean isHealthy() {

        return selfBuilder.isHealthy();
    }

    // =========================================================
    // PROJECT ANALYSIS
    // =========================================================

    public String analyzeProject() {

        try {

            String files =
                    selfBuilder.listProjectFiles();

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS PROJECT ANALYSIS\n"
            );

            result.append(
                    "============================\n\n"
            );

            result.append(
                    "FILES\n"
            );

            result.append(
                    files
            );

            result.append(
                    "\n\nARCHITECTURE\n"
            );

            result.append(
                    detectArchitecture()
            );

            result.append(
                    "\n\nDEPENDENCIES\n"
            );

            result.append(
                    analyzeDependencies()
            );

            memoryManager.saveMemory(
                    "__last_code_analysis__",
                    result.toString()
            );

            record(
                    "PROJECT_ANALYSIS",
                    "تم تحليل Workspace"
            );

            return result.toString();

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

        boolean java =
                selfBuilder
                        .searchSource(".java")
                        .contains(".java");

        boolean xml =
                selfBuilder
                        .searchSource(".xml")
                        .contains(".xml");

        boolean gradle =
                selfBuilder
                        .searchSource("gradle")
                        .toLowerCase(
                                Locale.getDefault()
                        )
                        .contains("gradle");

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
    }

    // =========================================================
    // DEPENDENCY ANALYSIS
    // =========================================================

    public String analyzeDependencies() {

        StringBuilder result =
                new StringBuilder();

        String gradleFiles =
                selfBuilder.searchSource(
                        "implementation"
                );

        result.append(
                "Gradle dependency scan:\n\n"
        );

        if (gradleFiles.contains(
                "ما لقيتش"
        )) {

            result.append(
                    "ما بان حتى dependency من نوع implementation.\n"
            );

        } else {

            result.append(
                    gradleFiles
            )
                    .append("\n");
        }

        result.append(
                "\nJARVIS dependency policy:\n"
        );

        result.append(
                "• تجنب dependency غير ضرورية\n"
        );

        result.append(
                "• فضل Android APIs الموجودة\n"
        );

        result.append(
                "• تحقق من compileSdk قبل إضافة مكتبة\n"
        );

        result.append(
                "• لا تضف خدمة مدفوعة تلقائيا"
        );

        return result.toString();
    }

    // =========================================================
    // SEARCH CODE
    // =========================================================

    public String searchCode(
            String query
    ) {

        return selfBuilder.searchSource(
                query
        );
    }

    // =========================================================
    // READ CODE
    // =========================================================

    public String readCode(
            String path
    ) {

        return selfBuilder.readSourceFile(
                path
        );
    }

    // =========================================================
    // CREATE FILE
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

        String snapshot =
                selfBuilder.createSnapshot(
                        "قبل إنشاء ملف: "
                                + safeText(reason)
                );

        String result =
                selfBuilder.writeSourceFile(
                        path,
                        content
                );

        boolean success =
                isSuccess(result);

        if (success) {

            record(
                    "CREATE_FILE",
                    path
            );

            memoryManager.saveMemory(
                    "__last_created_source__",
                    path
            );

            return
                    "CODE FILE CREATED ✓\n\n"
                    + "File: "
                    + path
                    + "\n\n"
                    + "Snapshot:\n"
                    + snapshot
                    + "\n\n"
                    + result;
        }

        return
                "إنشاء الملف فشل ⚠\n\n"
                + result;
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

        if (oldCode == null ||
                oldCode.isEmpty()) {

            return
                    "التعديل مرفوض:\n"
                    + "oldCode فارغ.";
        }

        String snapshot =
                selfBuilder.createSnapshot(
                        "قبل تعديل الكود: "
                                + safeText(reason)
                );

        String result =
                selfBuilder.evolveSourceFile(
                        path,
                        oldCode,
                        newCode,
                        reason
                );

        if (isSuccess(result)) {

            record(
                    "MODIFY_CODE",
                    path
            );

            memoryManager.saveMemory(
                    "__last_code_change__",
                    path
            );

            memoryManager.saveMemory(
                    "__last_code_change_reason__",
                    safeText(reason)
            );

            return
                    "CODE EVOLUTION SUCCESS ✓\n\n"
                    + result
                    + "\n\n"
                    + "Safety Snapshot:\n"
                    + snapshot;
        }

        return
                "CODE EVOLUTION FAILED ⚠\n\n"
                + result
                + "\n\n"
                + "Snapshot محفوظ:\n"
                + snapshot;
    }

    // =========================================================
    // GENERATE SIMPLE CLASS
    // =========================================================

    public String generateJavaClass(
            String packageName,
            String className,
            String description
    ) {

        if (!validIdentifier(
                className
        )) {

            return
                    "اسم Class غير صالح.";
        }

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            packageName =
                    "com.kamal.jarvis";
        }

        String cleanPackage =
                packageName.trim();

        StringBuilder code =
                new StringBuilder();

        code.append(
                "package "
        )
                .append(
                        cleanPackage
                )
                .append(
                        ";\n\n"
                );

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
                        safeText(description)
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
                .append(
                        className.trim()
                )
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
                        className.trim()
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

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            packageName =
                    "com.kamal.jarvis";
        }

        return
                "package "
                + packageName.trim()
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

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            packageName =
                    "com.kamal.jarvis";
        }

        StringBuilder code =
                new StringBuilder();

        code.append(
                "package "
        )
                .append(
                        packageName.trim()
                )
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

        if (values != null) {

            for (int i = 0;
                 i < values.length;
                 i++) {

                if (!validIdentifier(
                        values[i]
                )) {
                    continue;
                }

                code.append(
                        "    "
                )
                        .append(
                                values[i]
                                        .trim()
                        );

                if (i <
                        values.length - 1) {

                    code.append(",");
                }

                code.append("\n");
            }
        }

        code.append(
                "}\n"
        );

        return code.toString();
    }

    // =========================================================
    // GENERATE DEVELOPMENT PLAN
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
                "التحقق من الملف والـ Hash"
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
                "تجهيز APK جديد إذا كان Build Environment متاحا"
        );

        addPlanStep(
                steps,
                9,
                "Learn",
                "حفظ نتيجة التطوير"
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

        memoryManager.saveMemory(
                "__last_code_plan__",
                plan.toString()
        );

        record(
                "PLAN",
                cleanGoal
        );

        return
                plan.toString();
    }

    // =========================================================
    // SNAPSHOT
    // =========================================================

    public String snapshot(
            String reason
    ) {

        return selfBuilder.createSnapshot(
                reason
        );
    }

    // =========================================================
    // ROLLBACK
    // =========================================================

    public String rollback(
            String snapshotId
    ) {

        String result =
                selfBuilder.rollback(
                        snapshotId
                );

        record(
                "ROLLBACK",
                snapshotId
        );

        return result;
    }

    // =========================================================
    // INVENTORY
    // =========================================================

    public String getProjectInventory() {

        return
                selfBuilder.listProjectFiles();
    }

    // =========================================================
    // LAST PLAN
    // =========================================================

    public String getLastPlan() {

        String plan =
                memoryManager.getMemory(
                        "__last_code_plan__"
                );

        if (plan == null ||
                plan.trim().isEmpty()) {

            return
                    "ما كايناش خطة كود محفوظة.";
        }

        return plan;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    public String getHistory() {

        if (evolutionHistory.isEmpty()) {

            return
                    "ما كاين حتى Evolution Engine history حاليا.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "CODE EVOLUTION HISTORY\n\n"
        );

        for (String item :
                evolutionHistory) {

            result.append(
                    "• "
            )
                    .append(
                            item
                    )
                    .append(
                            "\n"
                    );
        }

        return result.toString();
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
                                Locale.getDefault()
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
    // IDENTIFIER VALIDATION
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
    // SUCCESS
    // =========================================================

    private boolean isSuccess(
            String result
    ) {

        if (result == null ||
                result.trim().isEmpty()) {

            return false;
        }

        String lower =
                result.toLowerCase(
                        Locale.getDefault()
                );

        return
                !lower.contains("فشل")
                && !lower.contains("failed")
                && !lower.contains("error");
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void record(
            String type,
            String message
    ) {

        String value =
                type
                        + " | "
                        + safeText(message);

        evolutionHistory.add(
                value
        );

        while (
                evolutionHistory.size()
                        > 100
        ) {

            evolutionHistory.remove(0);
        }

        memoryManager.saveMemory(
                "__code_evolution_last_action__",
                value
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
}