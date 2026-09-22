package com.kamal.jarvis;

import android.content.Context;

public class SelfTestEngine {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final TaskManager taskManager;
    private final LearningEngine learningEngine;

    private final SelfBuilderEngine selfBuilderEngine;
    private final CodeEvolutionEngine codeEvolutionEngine;
    private final ApkBuilderEngine apkBuilderEngine;

    private final RecoverySystem recoverySystem;
    private final SystemMonitor systemMonitor;

    public SelfTestEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        taskManager =
                new TaskManager(this.context);

        learningEngine =
                new LearningEngine(this.context);

        selfBuilderEngine =
                new SelfBuilderEngine(this.context);

        codeEvolutionEngine =
                new CodeEvolutionEngine(this.context);

        apkBuilderEngine =
                new ApkBuilderEngine(this.context);

        recoverySystem =
                new RecoverySystem(this.context);

        systemMonitor =
                new SystemMonitor(this.context);
    }

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

        TestResult[] tests = {

                testMemory(),

                testSkills(),

                testCapabilities(),

                testTasks(),

                testLearning(),

                testSelfBuilder(),

                testCodeEvolution(),

                testApkBuilder(),

                testRecovery(),

                testSystemMonitor(),

                testEvolutionInfrastructure()
        };

        for (TestResult test : tests) {

            report.append(
                    format(test)
            );

            if (test.passed) {

                passed++;

            } else {

                failed++;
            }
        }

        report.append(
                "============================\n"
        );

        report.append(
                "TESTS RUN: "
        );

        report.append(
                tests.length
        );

        report.append(
                "\nPASSED: "
        );

        report.append(
                passed
        );

        report.append(
                "\nFAILED: "
        );

        report.append(
                failed
        );

        report.append("\n\n");

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

    public String testSystem() {

        return runAllTests();
    }

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

    private TestResult testTasks() {

        try {

            int total =
                    taskManager.getTaskCount();

            int pending =
                    taskManager.getPendingTaskCount();

            int completed =
                    taskManager.getCompletedTaskCount();

            if (taskManager.isHealthy()) {

                return new TestResult(
                        "TASKS",
                        true,
                        "Task Manager ONLINE"
                                + " | Total: "
                                + total
                                + " | Pending: "
                                + pending
                                + " | Completed: "
                                + completed
                );
            }

            return new TestResult(
                    "TASKS",
                    false,
                    "Task Manager is not healthy"
            );

        } catch (Exception e) {

            return new TestResult(
                    "TASKS",
                    false,
                    safeError(e)
            );
        }
    }

    private TestResult testLearning() {

        try {

            if (learningEngine.isHealthy()) {

                return new TestResult(
                        "LEARNING",
                        true,
                        "Learning Engine ONLINE"
                );
            }

            return new TestResult(
                    "LEARNING",
                    false,
                    "Learning Engine is not healthy"
            );

        } catch (Exception e) {

            return new TestResult(
                    "LEARNING",
                    false,
                    safeError(e)
            );
        }
    }

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
                        "Workspace / Snapshot / Rollback ONLINE"
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
                        "Analysis / Generation / Modification ONLINE"
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

    private TestResult testRecovery() {

        try {

            String status =
                    recoverySystem.getStatus();

            if (status != null
                    && !status.trim().isEmpty()
                    && recoverySystem.isHealthy()) {

                return new TestResult(
                        "RECOVERY",
                        true,
                        "Recovery System ONLINE"
                );
            }

            return new TestResult(
                    "RECOVERY",
                    false,
                    "Recovery System needs attention"
            );

        } catch (Exception e) {

            return new TestResult(
                    "RECOVERY",
                    false,
                    safeError(e)
            );
        }
    }

    private TestResult testSystemMonitor() {

        try {

            String status =
                    systemMonitor.getStatus();

            int score =
                    systemMonitor.getHealthScore();

            if (status != null
                    && !status.trim().isEmpty()
                    && systemMonitor.isHealthy()) {

                return new TestResult(
                        "SYSTEM MONITOR",
                        true,
                        "System Monitor ONLINE"
                                + " | Health: "
                                + score
                                + "/100"
                );
            }

            return new TestResult(
                    "SYSTEM MONITOR",
                    false,
                    "System Monitor needs attention"
                            + " | Health: "
                            + score
                            + "/100"
            );

        } catch (Exception e) {

            return new TestResult(
                    "SYSTEM MONITOR",
                    false,
                    safeError(e)
            );
        }
    }

    private TestResult testEvolutionInfrastructure() {

        try {

            SelfDiagnosisManager diagnosis =
                    new SelfDiagnosisManager(
                            context
                    );

            String target =
                    diagnosis.getNextDevelopmentTarget();

            int readiness =
                    diagnosis.getReadinessScore();

            if (target != null
                    && !target.trim().isEmpty()) {

                return new TestResult(
                        "EVOLUTION",
                        true,
                        "Self Diagnosis ONLINE"
                                + " | Readiness: "
                                + readiness
                                + "%"
                );
            }

            return new TestResult(
                    "EVOLUTION",
                    false,
                    "Evolution infrastructure returned empty target"
            );

        } catch (Exception e) {

            return new TestResult(
                    "EVOLUTION",
                    false,
                    safeError(e)
            );
        }
    }

    public boolean isHealthy() {

        TestResult[] tests = {

                testMemory(),
                testSkills(),
                testCapabilities(),
                testTasks(),
                testLearning(),
                testSelfBuilder(),
                testCodeEvolution(),
                testApkBuilder(),
                testRecovery(),
                testSystemMonitor(),
                testEvolutionInfrastructure()
        };

        for (TestResult test : tests) {

            if (!test.passed) {

                return false;
            }
        }

        return true;
    }

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

            return format(testMemory());
        }

        if (name.contains("skill")
                || name.contains("مهارة")) {

            return format(testSkills());
        }

        if (name.contains("capability")
                || name.contains("قدرة")) {

            return format(testCapabilities());
        }

        if (name.contains("task")
                || name.contains("مهمة")) {

            return format(testTasks());
        }

        if (name.contains("learning")
                || name.contains("تعلم")) {

            return format(testLearning());
        }

        if (name.contains("builder")
                || name.contains("بناء")) {

            return format(testSelfBuilder());
        }

        if (name.contains("code")
                || name.contains("كود")
                || name.contains("تطوير")) {

            return format(testCodeEvolution());
        }

        if (name.contains("apk")
                || name.contains("build")) {

            return format(testApkBuilder());
        }

        if (name.contains("recovery")
                || name.contains("استرجاع")) {

            return format(testRecovery());
        }

        if (name.contains("monitor")
                || name.contains("مراقبة")) {

            return format(testSystemMonitor());
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