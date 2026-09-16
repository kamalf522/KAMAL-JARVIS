package com.kamal.jarvis;

import android.content.Context;

public class JarvisCore {

    private final Context context;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final EvolutionEngine evolutionEngine;
    private final SelfDiagnosisManager selfDiagnosisManager;
    private final SelfTestEngine selfTestEngine;
    private final RecoverySystem recoverySystem;

    private final TaskManager taskManager;
    private final ReminderEngine reminderEngine;
    private final LearningEngine learningEngine;
    private final PlanningEngine planningEngine;

    private final DecisionEngine decisionEngine;
    private final ApprovalEngine approvalEngine;
    private final ActionHistoryManager actionHistoryManager;
    private final ContextEngine contextEngine;
    private final AutomationEngine automationEngine;
    private final KnowledgeEngine knowledgeEngine;
    private final CommandLearningEngine commandLearningEngine;

    private final AndroidControlEngine androidControlEngine;
    private final ScreenIntelligence screenIntelligence;
    private final NotificationIntelligence notificationIntelligence;

    private final SelfBuilderEngine selfBuilderEngine;
    private final SystemMonitor systemMonitor;

    public JarvisCore(Context context) {

        this.context = context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        evolutionEngine =
                new EvolutionEngine(this.context);

        selfDiagnosisManager =
                new SelfDiagnosisManager(this.context);

        selfTestEngine =
                new SelfTestEngine(this.context);

        recoverySystem =
                new RecoverySystem(this.context);

        taskManager =
                new TaskManager(this.context);

        reminderEngine =
                new ReminderEngine(this.context);

        learningEngine =
                new LearningEngine(this.context);

        planningEngine =
                new PlanningEngine(this.context);

        decisionEngine =
                new DecisionEngine(this.context);

        approvalEngine =
                new ApprovalEngine(this.context);

        actionHistoryManager =
                new ActionHistoryManager(this.context);

        contextEngine =
                new ContextEngine(this.context);

        automationEngine =
                new AutomationEngine(this.context);

        knowledgeEngine =
                new KnowledgeEngine(this.context);

        commandLearningEngine =
                new CommandLearningEngine(this.context);

        androidControlEngine =
                new AndroidControlEngine(this.context);

        screenIntelligence =
                new ScreenIntelligence(this.context);

        notificationIntelligence =
                new NotificationIntelligence(this.context);

        selfBuilderEngine =
                new SelfBuilderEngine(this.context);

        systemMonitor =
                new SystemMonitor(this.context);
    }

    // =========================================================
    // COMMAND PROCESSING
    // =========================================================

    public String processCommand(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        try {

            actionHistoryManager.recordAction(
                    cleanCommand
            );

            String decision =
                    decisionEngine.decide(
                            cleanCommand
                    );

            contextEngine.updateCommand(
                    cleanCommand,
                    decision
            );

            contextEngine.updateDecision(
                    decision
            );

            String learned =
                    commandLearningEngine.findLearnedCommand(
                            cleanCommand
                    );

            if (learned != null &&
                    !learned.trim().isEmpty()) {

                return learned;
            }

            return routeCommand(
                    cleanCommand,
                    decision
            );

        } catch (Exception e) {

            return recoverFromError(
                    "processCommand",
                    e
            );
        }
    }

    private String routeCommand(
            String command,
            String decision
    ) {

        String cmd =
                command.toLowerCase();

        if (containsAny(
                cmd,
                "تشخيص",
                "شخص نفسك",
                "التشخيص الذاتي",
                "شنو ناقصك"
        )) {

            return selfDiagnosisManager.runDiagnosis();
        }

        if (containsAny(
                cmd,
                "اختبر نفسك",
                "اختبار النظام",
                "self test"
        )) {

            return selfTestEngine.runAllTests();
        }

        if (containsAny(
                cmd,
                "صلح نفسك",
                "إصلاح النظام",
                "recovery"
        )) {

            return recoverySystem.runRecovery();
        }

        if (containsAny(
                cmd,
                "حالة النظام",
                "status",
                "كيف داير"
        )) {

            return getFullStatus();
        }

        if (containsAny(
                cmd,
                "طور نفسك",
                "بدا التطور",
                "دورة التطور",
                "evolution"
        )) {

            return evolutionEngine.runEvolutionCycle();
        }

        if (containsAny(
                cmd,
                "شنو عندك",
                "القدرات",
                "capabilities"
        )) {

            return capabilityManager.getReport();
        }

        if (containsAny(
                cmd,
                "المهارات",
                "skills"
        )) {

            return skillManager.getReport();
        }

        if (containsAny(
                cmd,
                "الذاكرة",
                "ذكرياتك",
                "شنو حافظ"
        )) {

            return memoryManager.getAllMemories();
        }

        if (containsAny(
                cmd,
                "المهام",
                "tasks"
        )) {

            return taskManager.getTasks();
        }

        if (containsAny(
                cmd,
                "الخطة",
                "plan",
                "خطط"
        )) {

            return planningEngine.getActivePlan();
        }

        if (containsAny(
                cmd,
                "شنو كتعرف",
                "المعرفة",
                "knowledge"
        )) {

            return knowledgeEngine.getReport();
        }

        if (containsAny(
                cmd,
                "الاشعارات",
                "الإشعارات",
                "notifications"
        )) {

            return notificationIntelligence.getStatus();
        }

        if (containsAny(
                cmd,
                "الشاشة",
                "screen"
        )) {

            return screenIntelligence.getStatus();
        }

        if (containsAny(
                cmd,
                "الأتمتة",
                "automation"
        )) {

            return automationEngine.getAutomationStatus();
        }

        if (containsAny(
                cmd,
                "المراقبة",
                "monitor",
                "مراقبة النظام"
        )) {

            return systemMonitor.getFullStatus();
        }

        if (containsAny(
                cmd,
                "شنو الهدف الحالي",
                "الهدف الحالي"
        )) {

            return evolutionEngine
                    .getActiveDevelopmentTarget();
        }

        return automationEngine.execute(
                command
        );
    }

    // =========================================================
    // MEMORY
    // =========================================================

    public String remember(
            String key,
            String value
    ) {

        if (key == null ||
                key.trim().isEmpty()) {

            return "خاصني اسم الذاكرة.";
        }

        if (value == null ||
                value.trim().isEmpty()) {

            return "خاصني المعلومة اللي بغيتي نحفظ.";
        }

        memoryManager.saveMemory(
                key.trim(),
                value.trim()
        );

        return "تم حفظ المعلومة في ذاكرة JARVIS.";
    }

    public String rememberCommand(
            String command,
            String action
    ) {

        return commandLearningEngine.learnCommand(
                command,
                action
        );
    }

    // =========================================================
    // LEARNING / KNOWLEDGE
    // =========================================================

    public String learn(
            String subject,
            String information
    ) {

        return knowledgeEngine.learn(
                subject,
                information
        );
    }

    public String rememberKnowledge(
            String subject
    ) {

        return knowledgeEngine.remember(
                subject
        );
    }

    // =========================================================
    // TASKS
    // =========================================================

    public String addTask(
            String task
    ) {

        return taskManager.addTask(
                task
        );
    }

    public String completeTask(
            int index
    ) {

        return taskManager.completeTask(
                index
        );
    }

    public String removeTask(
            int index
    ) {

        return taskManager.removeTask(
                index
        );
    }

    // =========================================================
    // PLANNING
    // =========================================================

    public String createPlan(
            String goal
    ) {

        return planningEngine.createPlan(
                goal
        );
    }

    public String getPlan() {

        return planningEngine.getActivePlan();
    }

    // =========================================================
    // REMINDERS
    // =========================================================

    public String createReminder(
            String title,
            long triggerTime
    ) {

        return reminderEngine.createReminder(
                title,
                triggerTime
        );
    }

    // =========================================================
    // ANDROID CONTROL
    // =========================================================

    public String openWifi() {

        return androidControlEngine
                .openWifiSettings();
    }

    public String openBluetooth() {

        return androidControlEngine
                .openBluetoothSettings();
    }

    public String openDeviceInfo() {

        return androidControlEngine
                .openDeviceInfo();
    }

    public String openNotificationSettings() {

        return androidControlEngine
                .openNotificationSettings();
    }

    public String openDisplaySettings() {

        return androidControlEngine
                .openDisplaySettings();
    }

    public String openSoundSettings() {

        return androidControlEngine
                .openSoundSettings();
    }

    public String openBatterySettings() {

        return androidControlEngine
                .openBatterySettings();
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public String requestApproval(
            String action
    ) {

        return approvalEngine.requestApproval(
                action
        );
    }

    public String approveAction() {

        return approvalEngine.approve();
    }

    // =========================================================
    // SELF DEVELOPMENT
    // =========================================================

    public String getDevelopmentTarget() {

        return evolutionEngine
                .getActiveDevelopmentTarget();
    }

    public String createBuildPlan(
            String systemName
    ) {

        return selfBuilderEngine
                .createBuildPlan(
                        systemName
                );
    }

    // =========================================================
    // SCREEN
    // =========================================================

    public String analyzeScreen(
            String description
    ) {

        return screenIntelligence
                .analyzeScreen(
                        description
                );
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    public String processNotification(
            String appName,
            String title,
            String message
    ) {

        return notificationIntelligence
                .processNotification(
                        appName,
                        title,
                        message
                );
    }

    // =========================================================
    // FULL SYSTEM STATUS
    // =========================================================

    public String getFullStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "===== JARVIS CORE =====\n\n"
        );

        report.append(
                "CORE: ONLINE ✓\n\n"
        );

        report.append(
                "Evolution:\n"
        );

        report.append(
                safe(
                        evolutionEngine
                                .getEvolutionStatus()
                )
        );

        report.append("\n\n");

        report.append(
                "Diagnosis:\n"
        );

        report.append(
                safe(
                        selfDiagnosisManager
                                .runDiagnosis()
                )
        );

        report.append("\n\n");

        report.append(
                "Automation:\n"
        );

        report.append(
                safe(
                        automationEngine
                                .getAutomationStatus()
                )
        );

        report.append("\n\n");

        report.append(
                "System Monitor:\n"
        );

        report.append(
                safe(
                        systemMonitor
                                .getStatus()
                )
        );

        report.append("\n\n");

        report.append(
                "Self Test:\n"
        );

        report.append(
                safe(
                        selfTestEngine
                                .testSystem()
                )
        );

        report.append("\n\n");

        report.append(
                "Recovery:\n"
        );

        report.append(
                safe(
                        recoverySystem
                                .getStatus()
                )
        );

        return report.toString();
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            skillManager.getReport();

            capabilityManager.getReport();

            evolutionEngine.getEvolutionStatus();

            selfDiagnosisManager.runDiagnosis();

            selfTestEngine.testSystem();

            recoverySystem.getStatus();

            taskManager.getTasks();

            reminderEngine.getStatus();

            learningEngine.getLearningStatus();

            planningEngine.getStatus();

            decisionEngine.getStatus();

            approvalEngine.getLastApprovedAction();

            actionHistoryManager.getHistory();

            contextEngine.getStatus();

            automationEngine.getStatus();

            knowledgeEngine.getReport();

            commandLearningEngine.getStatus();

            androidControlEngine.getStatus();

            screenIntelligence.getStatus();

            notificationIntelligence.getStatus();

            selfBuilderEngine.getBuildStatus();

            systemMonitor.getStatus();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return "JARVIS CORE: ONLINE ✓";
        }

        return "JARVIS CORE: DEGRADED ⚠";
    }

    // =========================================================
    // ERROR RECOVERY
    // =========================================================

    private String recoverFromError(
            String operation,
            Exception error
    ) {

        try {

            recoverySystem.runRecovery();

        } catch (Exception ignored) {
        }

        return
                "وقع خطأ أثناء العملية: "
                + operation
                + "\n"
                + "JARVIS حاول استرجاع النظام تلقائياً.";
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null) {
            return false;
        }

        String normalized =
                text.toLowerCase();

        for (String value : values) {

            if (value != null &&
                    normalized.contains(
                            value.toLowerCase()
                    )) {

                return true;
            }
        }

        return false;
    }

    private String safe(
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value;
    }
}