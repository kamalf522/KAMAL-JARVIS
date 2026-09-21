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

        memoryManager = new MemoryManager(this.context);
        skillManager = new SkillManager(this.context);
        capabilityManager = new CapabilityManager(this.context);

        evolutionEngine = new EvolutionEngine(this.context);

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
    // MAIN COMMAND PROCESSOR
    // =========================================================

    public String processCommand(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand = command.trim();

        try {

            actionHistoryManager.recordAction(
                    cleanCommand
            );

            String decision =
                    decisionEngine.decide(cleanCommand);

            contextEngine.updateCommand(
                    cleanCommand,
                    decision
            );

            contextEngine.updateDecision(
                    decision
            );

            String learnedAction =
                    commandLearningEngine.findLearnedCommand(
                            cleanCommand
                    );

            if (learnedAction != null &&
                    !learnedAction.trim().isEmpty()) {

                String cleanLearnedAction =
                        learnedAction.trim();

                if (!cleanLearnedAction.equalsIgnoreCase(
                        cleanCommand
                )) {

                    return executeLearnedCommand(
                            cleanCommand,
                            cleanLearnedAction
                    );
                }
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

    // =========================================================
    // LEARNED COMMAND
    // =========================================================

    private String executeLearnedCommand(
            String originalCommand,
            String learnedAction
    ) {

        try {

            actionHistoryManager.recordAction(
                    "LEARNED: " + originalCommand
            );

            String result =
                    automationEngine.execute(
                            learnedAction
                    );

            if (result == null ||
                    result.trim().isEmpty()) {

                return
                        "JARVIS تعلم الأمر، ولكن ما قدرش ينفذ "
                                + "الإجراء حالياً.";
            }

            return result;

        } catch (Exception e) {

            return
                    "JARVIS عرف الأمر المتعلم ولكن وقع "
                            + "خطأ أثناء التنفيذ.\n\n"
                            + "الأمر: "
                            + originalCommand
                            + "\n"
                            + "الإجراء: "
                            + learnedAction;
        }
    }

    // =========================================================
    // ROUTER
    // =========================================================

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
                "شنو ناقصك",
                "diagnose"
        )) {

            return selfDiagnosisManager.runDiagnosis();
        }

        if (containsAny(
                cmd,
                "اختبر نفسك",
                "اختبار النظام",
                "اختبر النظام",
                "self test",
                "self-test"
        )) {

            return selfTestEngine.runAllTests();
        }

        if (containsAny(
                cmd,
                "صلح نفسك",
                "إصلاح النظام",
                "اصلح نفسك",
                "recovery"
        )) {

            return recoverySystem.runRecovery();
        }

        if (containsAny(
                cmd,
                "حالة النظام",
                "status",
                "كيف داير",
                "الوضع ديالك",
                "system status"
        )) {

            return getFullStatus();
        }

        if (containsAny(
                cmd,
                "طور نفسك",
                "طور راسك",
                "بدا التطور",
                "دورة التطور",
                "دور التطور",
                "evolution",
                "evolve"
        )) {

            return evolutionEngine.runEvolutionCycle();
        }

        if (containsAny(
                cmd,
                "حالة التطور",
                "وضع التطور",
                "evolution status"
        )) {

            return evolutionEngine.getEvolutionStatus();
        }

        if (containsAny(
                cmd,
                "سجل التطور",
                "تاريخ التطور",
                "evolution history"
        )) {

            return evolutionEngine.getEvolutionHistory();
        }

        if (containsAny(
                cmd,
                "شنو الهدف الحالي",
                "الهدف الحالي",
                "هدف التطور",
                "development target"
        )) {

            return evolutionEngine.getActiveDevelopmentTarget();
        }

        if (containsAny(
                cmd,
                "حالة builder",
                "حالة self builder",
                "self builder",
                "selfbuilder",
                "حالة البناء الذاتي"
        )) {

            return evolutionEngine.getSelfBuilderStatus();
        }

        if (containsAny(
                cmd,
                "فحص مشروع apk",
                "افحص مشروع apk",
                "تحقق من مشروع apk",
                "apk project check",
                "validate apk"
        )) {

            return evolutionEngine.validateApkProject();
        }

        if (containsAny(
                cmd,
                "جهز apk",
                "جهز البناء",
                "جهز build",
                "جهز apk build",
                "prepare apk",
                "prepare build"
        )) {

            return evolutionEngine.prepareApkBuild(
                    "طلب المستخدم بناء APK"
            );
        }

        if (containsAny(
                cmd,
                "بني apk",
                "ابني apk",
                "بناء apk",
                "صايب apk",
                "صنع apk",
                "build apk",
                "build debug",
                "assemble debug"
        )) {

            return evolutionEngine.buildDebugApk();
        }

        if (containsAny(
                cmd,
                "حالة apk",
                "حالة البناء",
                "حالة build",
                "build status",
                "apk status"
        )) {

            return evolutionEngine.getApkBuildStatus();
        }

        if (containsAny(
                cmd,
                "فين apk",
                "أين apk",
                "اين apk",
                "لقا apk",
                "ابحث عن apk",
                "find apk",
                "latest apk"
        )) {

            return evolutionEngine.getLatestApk();
        }

        if (containsAny(
                cmd,
                "سجل apk",
                "تاريخ apk",
                "سجل البناء",
                "build history"
        )) {

            return evolutionEngine.getApkBuildHistory();
        }

        if (containsAny(
                cmd,
                "شنو عندك",
                "القدرات",
                "قدراتك",
                "capabilities"
        )) {

            return capabilityManager.getReport();
        }

        if (containsAny(
                cmd,
                "المهارات",
                "مهاراتك",
                "skills"
        )) {

            return skillManager.getReport();
        }

        if (containsAny(
                cmd,
                "الذاكرة",
                "ذكرياتك",
                "شنو حافظ",
                "شنو كتعقل",
                "memory"
        )) {

            return memoryManager.getAllMemories();
        }

        if (containsAny(
                cmd,
                "المهام",
                "مهامي",
                "tasks"
        )) {

            return taskManager.getTasks();
        }

        if (containsAny(
                cmd,
                "الخطة",
                "plan",
                "خطط",
                "خطتي"
        )) {

            return planningEngine.getActivePlan();
        }

        if (containsAny(
                cmd,
                "شنو كتعرف",
                "المعرفة",
                "knowledge"
        )) {

            return knowledgeEngine.getKnowledgeReport();
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
                "الاتمتة",
                "automation"
        )) {

            return automationEngine.getAutomationStatus();
        }

        if (containsAny(
                cmd,
                "المراقبة",
                "monitor",
                "مراقبة النظام",
                "system monitor"
        )) {

            return systemMonitor.getFullStatus();
        }

        return automationEngine.execute(command);
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

    // =========================================================
    // COMMAND LEARNING
    // =========================================================

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
    // KNOWLEDGE
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

        return knowledgeEngine.remember(subject);
    }

    // =========================================================
    // TASKS
    // =========================================================

    public String addTask(String task) {

        return taskManager.addTask(task);
    }

    public String completeTask(int index) {

        return taskManager.completeTask(index);
    }

    public String removeTask(int index) {

        return taskManager.removeTask(index);
    }

    // =========================================================
    // PLANNING
    // =========================================================

    public String createPlan(String goal) {

        return planningEngine.createPlan(goal);
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

        return androidControlEngine.openWifiSettings();
    }

    public String openBluetooth() {

        return androidControlEngine.openBluetoothSettings();
    }

    public String openDeviceInfo() {

        return androidControlEngine.openDeviceInfo();
    }

    public String openNotificationSettings() {

        return androidControlEngine.openNotificationSettings();
    }

    public String openDisplaySettings() {

        return androidControlEngine.openDisplaySettings();
    }

    public String openSoundSettings() {

        return androidControlEngine.openSoundSettings();
    }

    public String openBatterySettings() {

        return androidControlEngine.openBatterySettings();
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public String requestApproval(String action) {

        if (action == null ||
                action.trim().isEmpty()) {

            return "خاصني الأمر اللي بغيتي نطلب عليه الموافقة.";
        }

        return approvalEngine.requestApproval(
                action.trim()
        );
    }

    public String approveAction(String action) {

        if (action == null ||
                action.trim().isEmpty()) {

            return "خاصني الأمر اللي بغيتي توافق عليه.";
        }

        boolean approved =
                approvalEngine.approve(
                        action.trim()
                );

        if (approved) {

            return "تمت الموافقة على الأمر: "
                    + action.trim();
        }

        return "ما تمت الموافقة على الأمر.";
    }

    // =========================================================
    // DEVELOPMENT
    // =========================================================

    public String getDevelopmentTarget() {

        return evolutionEngine.getActiveDevelopmentTarget();
    }

    public String createBuildPlan(
            String systemName
    ) {

        return selfBuilderEngine.createBuildPlan(
                systemName
        );
    }

    // =========================================================
    // SCREEN INTELLIGENCE
    // =========================================================

    public String analyzeScreen(
            String description
    ) {

        return screenIntelligence.analyzeScreen(
                description
        );
    }

    // =========================================================
    // NOTIFICATION INTELLIGENCE
    // =========================================================

    public String processNotification(
            String appName,
            String title,
            String message
    ) {

        return notificationIntelligence.processNotification(
                appName,
                title,
                message
        );
    }

    // =========================================================
    // FULL STATUS
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
                        evolutionEngine.getEvolutionStatus()
                )
        );

        report.append("\n\n");

        report.append("APK Builder:\n");

        report.append(
                safe(
                        evolutionEngine.getApkBuildStatus()
                )
        );

        report.append("\n\n");

        report.append("Self Diagnosis:\n");

        report.append(
                safe(
                        selfDiagnosisManager.runDiagnosis()
                )
        );

        report.append("\n\n");

        report.append("Skills:\n");

        report.append(
                safe(
                        skillManager.getReport()
                )
        );

        report.append("\n\n");

        report.append("Capabilities:\n");

        report.append(
                safe(
                        capabilityManager.getReport()
                )
        );

        report.append("\n\n");

        report.append("Tasks:\n");

        report.append(
                safe(
                        taskManager.getTasks()
                )
        );

        report.append("\n\n");

        report.append("Notifications:\n");

        report.append(
                safe(
                        notificationIntelligence.getStatus()
                )
        );

        report.append("\n\n");

        report.append("Screen:\n");

        report.append(
                safe(
                        screenIntelligence.getStatus()
                )
        );

        report.append("\n\n");

        report.append("Automation:\n");

        report.append(
                safe(
                        automationEngine.getAutomationStatus()
                )
        );

        report.append("\n\n");

        report.append("System Monitor:\n");

        report.append(
                safe(
                        systemMonitor.getFullStatus()
                )
        );

        return report.toString();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                values == null) {

            return false;
        }

        String clean =
                text.toLowerCase();

        for (String value : values) {

            if (value != null &&
                    clean.contains(
                            value.toLowerCase()
                    )) {

                return true;
            }
        }

        return false;
    }

    private String safe(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "غير متوفر حالياً.";
        }

        return value;
    }

    private String recoverFromError(
            String source,
            Exception error
    ) {

        String message =
                error.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            message =
                    error.getClass()
                            .getSimpleName();
        }

        try {

            recoverySystem.recordFailure(
                    source,
                    message
            );

        } catch (Exception ignored) {
        }

        return
                "JARVIS واجه خطأ في "
                        + source
                        + ".\n"
                        + "تم تسجيل الخطأ ومحاولة الاسترجاع.\n\n"
                        + message;
    }

    public Context getContext() {

        return context;
    }

    public MemoryManager getMemoryManager() {

        return memoryManager;
    }

    public SkillManager getSkillManager() {

        return skillManager;
    }

    public CapabilityManager getCapabilityManager() {

        return capabilityManager;
    }

    public EvolutionEngine getEvolutionEngine() {

        return evolutionEngine;
    }

    public SelfDiagnosisManager getSelfDiagnosisManager() {

        return selfDiagnosisManager;
    }

    public SelfTestEngine getSelfTestEngine() {

        return selfTestEngine;
    }

    public RecoverySystem getRecoverySystem() {

        return recoverySystem;
    }

    public TaskManager getTaskManager() {

        return taskManager;
    }

    public ReminderEngine getReminderEngine() {

        return reminderEngine;
    }

    public LearningEngine getLearningEngine() {

        return learningEngine;
    }

    public PlanningEngine getPlanningEngine() {

        return planningEngine;
    }

    public DecisionEngine getDecisionEngine() {

        return decisionEngine;
    }

    public ApprovalEngine getApprovalEngine() {

        return approvalEngine;
    }

    public ActionHistoryManager getActionHistoryManager() {

        return actionHistoryManager;
    }

    public ContextEngine getContextEngine() {

        return contextEngine;
    }

    public AutomationEngine getAutomationEngine() {

        return automationEngine;
    }

    public KnowledgeEngine getKnowledgeEngine() {

        return knowledgeEngine;
    }

    public CommandLearningEngine getCommandLearningEngine() {

        return commandLearningEngine;
    }

    public AndroidControlEngine getAndroidControlEngine() {

        return androidControlEngine;
    }

    public ScreenIntelligence getScreenIntelligence() {

        return screenIntelligence;
    }

    public NotificationIntelligence getNotificationIntelligence() {

        return notificationIntelligence;
    }

    public SelfBuilderEngine getSelfBuilderEngine() {

        return selfBuilderEngine;
    }

    public SystemMonitor getSystemMonitor() {

        return systemMonitor;
    }
}