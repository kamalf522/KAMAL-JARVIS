package com.kamal.jarvis;

import android.content.Context;

public class SelfTestEngine {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final SelfBuilderEngine selfBuilderEngine;
    private final CodeEvolutionEngine codeEvolutionEngine;
    private final ApkBuilderEngine apkBuilderEngine;

    public SelfTestEngine(Context context) {

        this.context =
                context.getApplicationContext();

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

        selfBuilderEngine =
                new SelfBuilderEngine(
                        this.context
                );

        codeEvolutionEngine =
                new CodeEvolutionEngine(
                        this.context
                );

        apkBuilderEngine =
                new ApkBuilderEngine(
                        this.context
                );
    }

    // ==========================================
    // RUN ALL TESTS
    // ==========================================

    public String runAllTests() {

        StringBuilder report =
                new StringBuilder();

        int passed = 0;
        int failed = 0;

        report.append(
                "JARVIS SELF TEST ENGINE\n"
        );

        report.append(
                "============================\n\n"
        );

        TestResult memory =
                testMemory();

        report.append(
                format(memory)
        );

        if (memory.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult skills =
                testSkills();

        report.append(
                format(skills)
        );

        if (skills.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult capabilities =
                testCapabilities();

        report.append(
                format(capabilities)
        );

        if (capabilities.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult builder =
                testSelfBuilder();

        report.append(
                format(builder)
        );

        if (builder.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult codeEvolution =
                testCodeEvolution();

        report.append(
                format(codeEvolution)
        );

        if (codeEvolution.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult apkBuilder =
                testApkBuilder();

        report.append(
                format(apkBuilder)
        );

        if (apkBuilder.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult evolutionInfrastructure =
                testEvolutionInfrastructure();

        report.append(
                format(evolutionInfrastructure)
        );

        if (evolutionInfrastructure.passed) {
            passed++;
        } else {
            failed++;
        }

        report.append(
                "============================\n"
        );

        report.append(
                "PASSED: "
        );

        report.append(
                passed
        );

        report.append(
                "\n"
        );

        report.append(
                "FAILED: "
        );

        report.append(
                failed
        );

        report.append(
                "\n\n"
        );

        if (failed == 0) {

            report.append(
                    "SYSTEM STATUS: HEALTHY ✓"
            );

        } else {

            report.append(
                    "SYSTEM STATUS: ISSUES DETECTED ⚠"
            );
        }

        return report.toString();
    }

    // ==========================================
    // COMPATIBILITY
    // ==========================================

    public String testSystem() {

        return runAllTests();
    }

    // ==========================================
    // MEMORY TEST
    // ==========================================

    private TestResult testMemory() {

        try {

            String key =
                    "__jarvis_self_test__";

            String value =
                    "JARVIS_TEST_OK";

            memoryManager.saveMemory(
                    key,
                    value
            );

            String result =
                    memoryManager.getMemory(
                            key
                    );

            memoryManager.removeMemory(
                    key
            );

            if (value.equals(result)) {

                return new TestResult(
                        "MEMORY",
                        true,
                        "Save / Load / Delete OK"
                );
            }

            return new TestResult(
                    "MEMORY",
                    false,
                    "Memory verification failed"
            );

        } catch (Exception e) {

            return new TestResult(
                    "MEMORY",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // SKILLS TEST
    // ==========================================

    private TestResult testSkills() {

        try {

            int count =
                    skillManager.getSkillCount();

            if (count >= 0) {

                return new TestResult(
                        "SKILLS",
                        true,
                        "Skill Manager ONLINE | Count: "
                                + count
                );
            }

            return new TestResult(
                    "SKILLS",
                    false,
                    "Invalid skill count"
            );

        } catch (Exception e) {

            return new TestResult(
                    "SKILLS",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // CAPABILITIES TEST
    // ==========================================

    private TestResult testCapabilities() {

        try {

            int count =
                    capabilityManager.getCount();

            if (count >= 0) {

                return new TestResult(
                        "CAPABILITIES",
                        true,
                        "Capability Manager ONLINE | Count: "
                                + count
                );
            }

            return new TestResult(
                    "CAPABILITIES",
                    false,
                    "Invalid capability count"
            );

        } catch (Exception e) {

            return new TestResult(
                    "CAPABILITIES",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // SELF BUILDER TEST
    // ==========================================

    private TestResult testSelfBuilder() {

        try {

            String status =
                    selfBuilderEngine.getStatus();

            if (status != null
                    && !status.trim().isEmpty()
                    && selfBuilderEngine.isHealthy()) {

                return new TestResult(
                        "SELF BUILDER",
                        true,
                        "Workspace / Snapshot / Rollback infrastructure ONLINE"
                );
            }

            return new TestResult(
                    "SELF BUILDER",
                    false,
                    "Self Builder is not healthy"
            );

        } catch (Exception e) {

            return new TestResult(
                    "SELF BUILDER",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // CODE EVOLUTION TEST
    // ==========================================

    private TestResult testCodeEvolution() {

        try {

            String status =
                    codeEvolutionEngine.getStatus();

            if (status != null
                    && !status.trim().isEmpty()
                    && codeEvolutionEngine.isHealthy()) {

                return new TestResult(
                        "CODE EVOLUTION",
                        true,
                        "Analysis / Generation / Modification infrastructure ONLINE"
                );
            }

            return new TestResult(
                    "CODE EVOLUTION",
                    false,
                    "Code Evolution Engine is not healthy"
            );

        } catch (Exception e) {

            return new TestResult(
                    "CODE EVOLUTION",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // APK BUILDER TEST
    // ==========================================

    private TestResult testApkBuilder() {

        try {

            String status =
                    apkBuilderEngine.getStatus();

            if (status != null
                    && !status.trim().isEmpty()
                    && apkBuilderEngine.isHealthy()) {

                return new TestResult(
                        "APK BUILDER",
                        true,
                        "APK build infrastructure ONLINE"
                );
            }

            return new TestResult(
                    "APK BUILDER",
                    false,
                    "APK Builder is not healthy"
            );

        } catch (Exception e) {

            return new TestResult(
                    "APK BUILDER",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // EVOLUTION INFRASTRUCTURE TEST
    // ==========================================

    private TestResult testEvolutionInfrastructure() {

        try {

            SelfDiagnosisManager diagnosis =
                    new SelfDiagnosisManager(
                            context
                    );

            String status =
                    diagnosis.getNextDevelopmentTarget();

            if (status != null
                    && !status.trim().isEmpty()) {

                return new TestResult(
                        "EVOLUTION",
                        true,
                        "Diagnosis / Development Target infrastructure ONLINE"
                );
            }

            return new TestResult(
                    "EVOLUTION",
                    false,
                    "Evolution infrastructure returned empty status"
            );

        } catch (Exception e) {

            return new TestResult(
                    "EVOLUTION",
                    false,
                    safeError(e)
            );
        }
    }

    // ==========================================
    // QUICK HEALTH CHECK
    // ==========================================

    public boolean isHealthy() {

        TestResult memory =
                testMemory();

        TestResult skills =
                testSkills();

        TestResult capabilities =
                testCapabilities();

        TestResult builder =
                testSelfBuilder();

        TestResult codeEvolution =
                testCodeEvolution();

        TestResult apkBuilder =
                testApkBuilder();

        TestResult evolution =
                testEvolutionInfrastructure();

        return memory.passed
                && skills.passed
                && capabilities.passed
                && builder.passed
                && codeEvolution.passed
                && apkBuilder.passed
                && evolution.passed;
    }

    // ==========================================
    // TEST SINGLE SYSTEM
    // ==========================================

    public String testSystem(
            String system
    ) {

        if (system == null) {

            return "اسم النظام غير موجود.";
        }

        String name =
                system.trim().toLowerCase();

        if (name.contains("memory")
                || name.contains("ذاكرة")) {

            return format(
                    testMemory()
            );
        }

        if (name.contains("skill")
                || name.contains("مهارة")) {

            return format(
                    testSkills()
            );
        }

        if (name.contains("capability")
                || name.contains("قدرة")) {

            return format(
                    testCapabilities()
            );
        }

        if (name.contains("builder")
                || name.contains("self builder")
                || name.contains("بناء")) {

            return format(
                    testSelfBuilder()
            );
        }

        if (name.contains("code")
                || name.contains("كود")
                || name.contains("تطوير")) {

            return format(
                    testCodeEvolution()
            );
        }

        if (name.contains("apk")
                || name.contains("build")) {

            return format(
                    testApkBuilder()
            );
        }

        if (name.contains("evolution")
                || name.contains("تطور")) {

            return format(
                    testEvolutionInfrastructure()
            );
        }

        return
                "ما عنديش اختبار لهذا النظام حاليا.";
    }

    // ==========================================
    // FORMAT
    // ==========================================

    private String format(
            TestResult result
    ) {

        if (result.passed) {

            return "✓ "
                    + result.name
                    + "\n  "
                    + result.message
                    + "\n\n";
        }

        return "✗ "
                + result.name
                + "\n  "
                + result.message
                + "\n\n";
    }

    // ==========================================
    // ERROR HANDLER
    // ==========================================

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    // ==========================================
    // TEST RESULT
    // ==========================================

    private static class TestResult {

        final String name;
        final boolean passed;
        final String message;

        TestResult(
                String name,
                boolean passed,
                String message
        ) {

            this.name = name;
            this.passed = passed;
            this.message = message;
        }
    }
}

دير غير هاد الملف:
"app/src/main/java/com/kamal/jarvis/SelfTestEngine.java"

بدّل المحتوى كامل → Commit changes.

من بعد قول ليا غير تم، وأنا نراجع الـBuild والملفات المرتبطة قبل الخطوة اللي بعدها.