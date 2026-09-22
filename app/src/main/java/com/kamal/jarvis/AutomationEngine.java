package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutomationEngine {

    private final Context context;

    private final DecisionEngine decisionEngine;
    private final TaskManager taskManager;
    private final ReminderEngine reminderEngine;
    private final AndroidControlEngine androidControlEngine;
    private final ContextEngine contextEngine;

    private volatile String lastCommand = "";
    private volatile String lastResult = "";
    private volatile long lastExecutionTime = 0L;
    private volatile int lastStepCount = 0;
    private volatile int lastSuccessfulSteps = 0;
    private volatile int lastFailedStep = 0;

    public AutomationEngine(Context context) {

        this.context =
                context == null
                        ? null
                        : context.getApplicationContext();

        decisionEngine =
                new DecisionEngine(this.context);

        taskManager =
                new TaskManager(this.context);

        reminderEngine =
                new ReminderEngine(this.context);

        androidControlEngine =
                new AndroidControlEngine(this.context);

        contextEngine =
                new ContextEngine(this.context);
    }

    // =========================================================
    // COMMAND ANALYSIS
    // =========================================================

    public String analyzeCommand(String command) {

        if (isEmpty(command)) {
            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        lastCommand =
                cleanCommand;

        try {

            String decision =
                    safe(
                            decisionEngine.decide(
                                    cleanCommand
                            )
                    );

            contextEngine.updateCommand(
                    cleanCommand,
                    decision
            );

            contextEngine.updateDecision(
                    decision
            );

            return
                    "تم تحليل الأمر ✓\n\n"
                    + "الأمر:\n"
                    + cleanCommand
                    + "\n\n"
                    + "نوع العملية:\n"
                    + decision;

        } catch (Exception e) {

            return
                    "ما قدرتش نحلل الأمر دابا ⚠";
        }
    }

    // =========================================================
    // MAIN EXECUTION
    // =========================================================

    public synchronized String execute(
            String command
    ) {

        if (isEmpty(command)) {
            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        lastCommand =
                cleanCommand;

        lastExecutionTime =
                System.currentTimeMillis();

        lastStepCount = 0;
        lastSuccessfulSteps = 0;
        lastFailedStep = 0;

        try {

            List<String> steps =
                    splitIntoSteps(
                            cleanCommand
                    );

            lastStepCount =
                    steps.size();

            String result;

            if (steps.size() > 1) {

                result =
                        executeSequence(
                                cleanCommand,
                                steps
                        );

            } else {

                result =
                        executeSingleCommand(
                                cleanCommand
                        );

                if (isEmpty(result)) {

                    result =
                            executeSequence(
                                    cleanCommand,
                                    steps
                            );
                }
            }

            lastResult =
                    safe(result);

            contextEngine.updateCommand(
                    cleanCommand,
                    lastResult
            );

            contextEngine.updateDecision(
                    steps.size() > 1
                            ? "AUTOMATION"
                            : "EXECUTED"
            );

            return lastResult;

        } catch (Exception e) {

            lastResult =
                    "JARVIS Automation: وقع خطأ أثناء التنفيذ ⚠";

            contextEngine.updateCommand(
                    cleanCommand,
                    lastResult
            );

            contextEngine.updateDecision(
                    "ERROR"
            );

            return lastResult;
        }
    }

    // =========================================================
    // SINGLE COMMAND ENGINE
    // =========================================================

    private String executeSingleCommand(
            String command
    ) {

        String cmd =
                normalize(command);

        if (cmd.isEmpty()) {
            return "الأمر فارغ.";
        }

        // -----------------------------------------------------
        // SYSTEM NAVIGATION
        // -----------------------------------------------------

        if (containsAny(
                cmd,
                "رجع للرئيسية",
                "الصفحة الرئيسية",
                "الرئيسية",
                "home"
        )) {

            return androidControlEngine.goHome();
        }

        if (containsAny(
                cmd,
                "رجع للور",
                "رجوع",
                "رجع خطوة",
                "back"
        )) {

            return androidControlEngine.goBack();
        }

        if (containsAny(
                cmd,
                "التطبيقات الأخيرة",
                "التطبيقات الاخيرة",
                "افتح التطبيقات الأخيرة",
                "افتح التطبيقات الاخيره",
                "recents"
        )) {

            return androidControlEngine.openRecents();
        }

        if (containsAny(
                cmd,
                "افتح الإشعارات",
                "افتح الاشعارات",
                "الإشعارات",
                "الاشعارات",
                "notification panel"
        )) {

            return androidControlEngine.openNotifications();
        }

        if (containsAny(
                cmd,
                "لوحة الاختصارات",
                "افتح لوحة الاختصارات",
                "الاختصارات",
                "quick settings"
        )) {

            return androidControlEngine.openQuickSettings();
        }

        if (containsAny(
                cmd,
                "قفل الشاشة",
                "قفل الهاتف",
                "سكر الشاشة",
                "lock screen"
        )) {

            return androidControlEngine.lockScreen();
        }

        // -----------------------------------------------------
        // SETTINGS
        // -----------------------------------------------------

        if (containsAny(
                cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "فتح الإعدادات",
                "فتح الاعدادات",
                "settings"
        )) {

            return androidControlEngine.openSettings();
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi",
                "wi fi"
        )) {

            return androidControlEngine.openWifiSettings();
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {

            return androidControlEngine.openBluetoothSettings();
        }

        if (containsAny(
                cmd,
                "افتح إمكانية الوصول",
                "افتح امكانية الوصول",
                "افتح الاكسيسيبيليتي",
                "accessibility"
        )) {

            return androidControlEngine
                    .openAccessibilitySettings();
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "معلومات الجهاز",
                "حول الهاتف",
                "حول الجهاز",
                "device information"
        )) {

            return androidControlEngine
                    .openDeviceInformation();
        }

        // -----------------------------------------------------
        // COMMON APPS
        // -----------------------------------------------------

        if (containsAny(
                cmd,
                "افتح يوتيوب",
                "فتح يوتيوب",
                "youtube"
        )) {

            return androidControlEngine
                    .openYouTube();
        }

        if (containsAny(
                cmd,
                "افتح واتساب",
                "فتح واتساب",
                "whatsapp"
        )) {

            return androidControlEngine
                    .openWhatsApp();
        }

        if (containsAny(
                cmd,
                "افتح انستغرام",
                "فتح انستغرام",
                "instagram"
        )) {

            return androidControlEngine
                    .openInstagram();
        }

        if (containsAny(
                cmd,
                "افتح فيسبوك",
                "فتح فيسبوك",
                "facebook"
        )) {

            return androidControlEngine
                    .openFacebook();
        }

        if (containsAny(
                cmd,
                "افتح كروم",
                "فتح كروم",
                "chrome"
        )) {

            return androidControlEngine
                    .openChrome();
        }

        // -----------------------------------------------------
        // OPEN APPLICATION BY NAME
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "افتح تطبيق ",
                "فتح تطبيق ",
                "شغل تطبيق ",
                "شغل "
        )) {

            String app =
                    extractAfterPrefix(
                            command,
                            "افتح تطبيق",
                            "فتح تطبيق",
                            "شغل تطبيق",
                            "شغل"
                    );

            if (!app.isEmpty()) {

                return androidControlEngine
                        .openApplicationByName(
                                app
                        );
            }
        }

        // -----------------------------------------------------
        // WEB SEARCH
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "قلب ليا على ",
                "قلب لي على ",
                "قلب على ",
                "ابحث عن ",
                "بحث عن ",
                "بحث على ",
                "search for "
        )) {

            String query =
                    extractAfterPrefix(
                            command,
                            "قلب ليا على",
                            "قلب لي على",
                            "قلب على",
                            "ابحث عن",
                            "بحث عن",
                            "بحث على",
                            "search for"
                    );

            if (!query.isEmpty()) {

                return androidControlEngine
                        .searchWeb(query);
            }
        }

        // -----------------------------------------------------
        // OPEN WEBSITE
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "افتح الموقع ",
                "فتح الموقع ",
                "افتح رابط ",
                "فتح رابط ",
                "open website ",
                "open site "
        )) {

            String url =
                    extractAfterPrefix(
                            command,
                            "افتح الموقع",
                            "فتح الموقع",
                            "افتح رابط",
                            "فتح رابط",
                            "open website",
                            "open site"
                    );

            if (!url.isEmpty()) {

                return androidControlEngine
                        .openWebPage(url);
            }
        }

        // -----------------------------------------------------
        // PHONE
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "اتصل ب ",
                "اتصل ب",
                "عيط ل ",
                "عيط ل",
                "عيط على ",
                "عيط على",
                "call "
        )) {

            String number =
                    extractAfterPrefix(
                            command,
                            "اتصل ب",
                            "اتصل ب ",
                            "عيط ل",
                            "عيط ل ",
                            "عيط على",
                            "عيط على ",
                            "call"
                    );

            if (!number.isEmpty()) {

                return androidControlEngine
                        .dialNumber(number);
            }
        }

        // -----------------------------------------------------
        // SCREEN CLICK
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "اضغط على ",
                "ضغط على ",
                "كليك على ",
                "انقر على ",
                "click "
        )) {

            String target =
                    extractAfterPrefix(
                            command,
                            "اضغط على",
                            "ضغط على",
                            "كليك على",
                            "انقر على",
                            "click"
                    );

            if (!target.isEmpty()) {

                return androidControlEngine
                        .clickScreenElement(
                                target
                        );
            }
        }

        // -----------------------------------------------------
        // SCREEN TEXT INPUT
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "اكتب ",
                "كتب ",
                "write "
        )) {

            String text =
                    extractAfterPrefix(
                            command,
                            "اكتب",
                            "كتب",
                            "write"
                    );

            if (!text.isEmpty()) {

                return androidControlEngine
                        .typeIntoScreen(
                                "",
                                text
                        );
            }
        }

        // -----------------------------------------------------
        // SCROLL
        // -----------------------------------------------------

        if (containsAny(
                cmd,
                "سكرول لتحت",
                "سكرول للاسفل",
                "سكرول للأسفل",
                "مرر لتحت",
                "مرر للأسفل",
                "انزل",
                "هبط",
                "scroll down"
        )) {

            return androidControlEngine
                    .scrollDown();
        }

        if (containsAny(
                cmd,
                "سكرول لفوق",
                "سكرول للاعلى",
                "سكرول للأعلى",
                "مرر لفوق",
                "مرر للأعلى",
                "طلع",
                "اصعد",
                "scroll up"
        )) {

            return androidControlEngine
                    .scrollUp();
        }

        // -----------------------------------------------------
        // SCREEN INTELLIGENCE
        // -----------------------------------------------------

        if (containsAny(
                cmd,
                "شوف الشاشة",
                "اقرا الشاشة",
                "اقرأ الشاشة",
                "حلل الشاشة",
                "شنو كاين فالشاشة",
                "شنو فالشاشة",
                "analyze screen",
                "read screen"
        )) {

            return androidControlEngine
                    .getCurrentScreenText();
        }

        if (containsAny(
                cmd,
                "شوف شجرة الشاشة",
                "شجرة الشاشة",
                "screen tree"
        )) {

            return androidControlEngine
                    .getCurrentScreenTree();
        }

        // -----------------------------------------------------
        // TASK COMMANDS
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "زيد مهمة ",
                "اضف مهمة ",
                "أضف مهمة ",
                "ضيف مهمة ",
                "add task "
        )) {

            String task =
                    extractAfterPrefix(
                            command,
                            "زيد مهمة",
                            "اضف مهمة",
                            "أضف مهمة",
                            "ضيف مهمة",
                            "add task"
                    );

            if (!task.isEmpty()) {

                return createTask(task);
            }
        }

        if (containsAny(
                cmd,
                "شوف المهام",
                "المهام ديالي",
                "المهام",
                "show tasks"
        )) {

            return getTasks();
        }

        if (containsAny(
                cmd,
                "حيد المهام المكتملة",
                "مسح المهام المكتملة",
                "clear completed tasks"
        )) {

            return clearCompletedTasks();
        }

        if (containsAny(
                cmd,
                "حيد جميع المهام",
                "مسح جميع المهام",
                "clear all tasks"
        )) {

            return clearAllTasks();
        }

        // -----------------------------------------------------
        // UNKNOWN
        // -----------------------------------------------------

        return null;
    }

    // =========================================================
    // MULTI STEP AUTOMATION
    // =========================================================

    private String executeSequence(
            String command,
            List<String> steps
    ) {

        if (steps == null ||
                steps.isEmpty()) {

            return
                    "JARVIS: ما قدرتش نفهم خطوات الأتمتة.";
        }

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS AUTOMATION\n"
        );

        report.append(
                "=================\n\n"
        );

        int successCount = 0;
        int totalCount = steps.size();

        for (int i = 0;
             i < totalCount;
             i++) {

            String step =
                    steps.get(i);

            if (step == null ||
                    step.trim().isEmpty()) {

                continue;
            }

            report.append(
                    "STEP "
            );

            report.append(
                    i + 1
            );

            report.append(
                    ":\n"
            );

            report.append(
                    step
            );

            report.append(
                    "\n"
            );

            String result =
                    executeStep(
                            step
                    );

            result =
                    safe(result);

            report.append(
                    "RESULT:\n"
            );

            report.append(
                    result
            );

            report.append(
                    "\n\n"
            );

            if (isSuccessResult(result)) {

                successCount++;

                lastSuccessfulSteps =
                        successCount;

            } else {

                lastFailedStep =
                        i + 1;
            }

            if (isHardFailure(result)) {

                report.append(
                        "AUTOMATION STOPPED ⚠\n"
                );

                report.append(
                        "سبب التوقف: فشل تنفيذ الخطوة الحالية."
                );

                lastSuccessfulSteps =
                        successCount;

                return report.toString();
            }
        }

        report.append(
                "SUMMARY:\n"
        );

        report.append(
                successCount
        );

        report.append(
                "/"
        );

        report.append(
                totalCount
        );

        report.append(
                " خطوات منفذة بنجاح."
        );

        if (successCount == totalCount) {

            report.append(
                    "\n\nAUTOMATION COMPLETE ✓"
            );

        } else {

            report.append(
                    "\n\nAUTOMATION PARTIAL ⚠"
            );
        }

        lastSuccessfulSteps =
                successCount;

        return report.toString();
    }

    // =========================================================
    // EXECUTE STEP
    // =========================================================

    private String executeStep(
            String step
    ) {

        if (isEmpty(step)) {

            return "الخطوة فارغة.";
        }

        String clean =
                step.trim();

        long delay =
                extractDelay(clean);

        if (delay >= 0L) {

            try {

                /*
                 * الحد الأقصى 10 ثواني
                 * باش الأتمتة ما تبقاش معلقة.
                 */
                long safeDelay =
                        Math.min(
                                delay,
                                10000L
                        );

                Thread.sleep(
                        safeDelay
                );

                return
                        "انتظرت "
                        + safeDelay
                        + "ms ✓";

            } catch (InterruptedException e) {

                Thread.currentThread()
                        .interrupt();

                return
                        "التنفيذ توقف أثناء الانتظار ⚠";
            }
        }

        String result =
                executeSingleCommand(
                        clean
                );

        if (!isEmpty(result)) {

            return result;
        }

        return
                "JARVIS: ما فهمتش هاد الخطوة:\n"
                + clean;
    }

    // =========================================================
    // STEP SPLITTER
    // =========================================================

    private List<String> splitIntoSteps(
            String command
    ) {

        List<String> steps =
                new ArrayList<>();

        if (isEmpty(command)) {
            return steps;
        }

        String normalized =
                command.trim();

        String[] separators = {
                " ثم ",
                " ومن بعد ",
                " و من بعد ",
                " وبعدها ",
                " و بعدها ",
                " بعد ذلك ",
                " وبعد ",
                " ومن بعد",
                "؛",
                ";"
        };

        for (String separator :
                separators) {

            normalized =
                    normalized.replace(
                            separator,
                            "||"
                    );
        }

        String[] parts =
                normalized.split(
                        "\\|\\|"
                );

        for (String part :
                parts) {

            if (part == null) {
                continue;
            }

            String clean =
                    part.trim();

            if (!clean.isEmpty()) {

                steps.add(
                        clean
                );
            }
        }

        if (steps.isEmpty()) {

            steps.add(
                    command.trim()
            );
        }

        return steps;
    }

    // =========================================================
    // TASK MANAGEMENT
    // =========================================================

    public String createTask(
            String task
    ) {

        if (isEmpty(task)) {

            return
                    "حدد المهمة اللي بغيتي نضيف.";
        }

        String cleanTask =
                task.trim();

        String result =
                taskManager.addTask(
                        cleanTask
                );

        contextEngine.updateGoal(
                cleanTask
        );

        return safe(result);
    }

    public String completeTask(
            int index
    ) {

        return safe(
                taskManager.completeTask(
                        index
                )
        );
    }

    public String removeTask(
            int index
    ) {

        return safe(
                taskManager.removeTask(
                        index
                )
        );
    }

    public String getTasks() {

        return safe(
                taskManager.getTasks()
        );
    }

    public String clearCompletedTasks() {

        return safe(
                taskManager.clearCompleted()
        );
    }

    public String clearAllTasks() {

        return safe(
                taskManager.clearAll()
        );
    }

    // =========================================================
    // PLANNING
    // =========================================================

    public String createPlan(
            String goal
    ) {

        if (isEmpty(goal)) {

            return
                    "حدد الهدف الأول.";
        }

        String cleanGoal =
                goal.trim();

        contextEngine.updateGoal(
                cleanGoal
        );

        return safe(
                contextEngine.createContextPlan()
        );
    }

    // =========================================================
    // REMINDERS
    // =========================================================

    public String createReminder(
            String title,
            long triggerTime
    ) {

        if (isEmpty(title)) {

            return
                    "خاصك تحدد اسم التذكير.";
        }

        return safe(
                reminderEngine.createReminder(
                        title.trim(),
                        triggerTime
                )
        );
    }

    public String getLastReminder() {

        return safe(
                reminderEngine.getLastReminder()
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getAutomationStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "=== AUTOMATION ENGINE ===\n\n"
        );

        report.append(
                "Engine: "
        );

        report.append(
                isHealthy()
                        ? "ONLINE ✓"
                        : "ERROR ⚠"
        );

        report.append(
                "\nDecision Engine: "
        );

        report.append(
                safe(
                        decisionEngine.getStatus()
                )
        );

        report.append(
                "\nTask Manager: "
        );

        report.append(
                safe(
                        taskManager.getStatus()
                )
        );

        report.append(
                "\nReminder Engine: "
        );

        report.append(
                safe(
                        reminderEngine.getStatus()
                )
        );

        report.append(
                "\nAndroid Control: "
        );

        report.append(
                safe(
                        androidControlEngine.getStatus()
                )
        );

        report.append(
                "\nContext Engine: "
        );

        report.append(
                safe(
                        contextEngine.getStatus()
                )
        );

        report.append(
                "\n\nLast Command: "
        );

        report.append(
                lastCommand.isEmpty()
                        ? "NONE"
                        : lastCommand
        );

        report.append(
                "\nLast Result: "
        );

        report.append(
                lastResult.isEmpty()
                        ? "NONE"
                        : lastResult
        );

        report.append(
                "\n\nLast Steps: "
        );

        report.append(
                lastStepCount
        );

        report.append(
                "\nSuccessful Steps: "
        );

        report.append(
                lastSuccessfulSteps
        );

        report.append(
                "\nFailed Step: "
        );

        report.append(
                lastFailedStep == 0
                        ? "NONE"
                        : String.valueOf(
                                lastFailedStep
                        )
        );

        return report.toString();
    }

    public String getStatus() {

        return isHealthy()
                ? "Automation Engine: ONLINE ✓"
                : "Automation Engine: ERROR ⚠";
    }

    public boolean isHealthy() {

        try {

            return context != null
                    && decisionEngine != null
                    && taskManager != null
                    && reminderEngine != null
                    && androidControlEngine != null
                    && contextEngine != null
                    && androidControlEngine
                            .isHealthy()
                    && reminderEngine
                            .isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // LAST EXECUTION
    // =========================================================

    public String getLastCommand() {

        return lastCommand;
    }

    public String getLastResult() {

        return lastResult;
    }

    public long getLastExecutionTime() {

        return lastExecutionTime;
    }

    public int getLastStepCount() {

        return lastStepCount;
    }

    public int getLastSuccessfulSteps() {

        return lastSuccessfulSteps;
    }

    public int getLastFailedStep() {

        return lastFailedStep;
    }

    // =========================================================
    // DELAY ENGINE
    // =========================================================

    private long extractDelay(
            String command
    ) {

        String value =
                normalize(command);

        if (!startsWithAny(
                value,
                "انتظر ",
                "