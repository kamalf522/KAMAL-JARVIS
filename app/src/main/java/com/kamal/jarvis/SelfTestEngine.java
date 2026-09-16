package com.kamal.jarvis;

import android.content.Context;

public class SelfTestEngine {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;

    public SelfTestEngine(Context context) {

        this.context = context.getApplicationContext();

        memoryManager = new MemoryManager(this.context);
        skillManager = new SkillManager(this.context);
        capabilityManager = new CapabilityManager(this.context);
    }

    // ==========================================
    // RUN ALL TESTS
    // ==========================================

    public String runAllTests() {

        StringBuilder report = new StringBuilder();

        int passed = 0;
        int failed = 0;

        report.append("JARVIS SELF TEST ENGINE\n");
        report.append("============================\n\n");

        TestResult memory = testMemory();

        report.append(format(memory));

        if (memory.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult skills = testSkills();

        report.append(format(skills));

        if (skills.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult capabilities = testCapabilities();

        report.append(format(capabilities));

        if (capabilities.passed) {
            passed++;
        } else {
            failed++;
        }

        TestResult evolution = testEvolution();

        report.append(format(evolution));

        if (evolution.passed) {
            passed++;
        } else {
            failed++;
        }

        report.append("============================\n");
        report.append("PASSED: ");
        report.append(passed);
        report.append("\n");

        report.append("FAILED: ");
        report.append(failed);
        report.append("\n\n");

        if (failed == 0) {

            report.append("SYSTEM STATUS: HEALTHY ✓");

        } else {

            report.append("SYSTEM STATUS: ISSUES DETECTED ⚠");
        }

        return report.toString();
    }

    // ==========================================
    // COMPATIBILITY METHOD
    // ==========================================

    public String testSystem() {

        return runAllTests();
    }

    // ==========================================
    // MEMORY TEST
    // ==========================================

    private TestResult testMemory() {

        try {

            String key = "__jarvis_self_test__";
            String value = "JARVIS_TEST_OK";

            memoryManager.saveMemory(
                    key,
                    value
            );

            String result =
                    memoryManager.getMemory(key);

            memoryManager.removeMemory(key);

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

            if (count > 0) {

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
                    "Capability registry is empty"
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
    // EVOLUTION TEST
    // ==========================================

    private TestResult testEvolution() {

        try {

            EvolutionEngine engine =
                    new EvolutionEngine(context);

            String status =
                    engine.getEvolutionStatus();

            if (status != null
                    && !status.trim().isEmpty()) {

                return new TestResult(
                        "EVOLUTION",
                        true,
                        "Evolution Engine ONLINE"
                );
            }

            return new TestResult(
                    "EVOLUTION",
                    false,
                    "Evolution Engine returned empty status"
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

        TestResult memory = testMemory();
        TestResult skills = testSkills();
        TestResult capabilities = testCapabilities();
        TestResult evolution = testEvolution();

        return memory.passed
                && skills.passed
                && capabilities.passed
                && evolution.passed;
    }

    // ==========================================
    // TEST SINGLE SYSTEM
    // ==========================================

    public String testSystem(String system) {

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

        if (name.contains("evolution")
                || name.contains("تطور")) {

            return format(
                    testEvolution()
            );
        }

        return "ما عنديش اختبار لهذا النظام حاليا.";
    }

    // ==========================================
    // FORMAT
    // ==========================================

    private String format(TestResult result) {

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

    private String safeError(Exception e) {

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