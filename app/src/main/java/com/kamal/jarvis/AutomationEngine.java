package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutomationEngine {

    private static final long MAX_DELAY_MS = 15000L;
    private static final int MAX_STEPS = 30;
    private static final int MAX_RETRIES = 2;

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
    private volatile int lastRetryCount = 0;
    private volatile boolean running = false;

    public AutomationEngine(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "AutomationEngine requires a valid Context"
            );
        }

        this.context =
                context.getApplicationContext();

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

    public synchronized String analyzeCommand(
            String command
    ) {

        if (isEmpty(command)) {
            return "ما عطيتيني حتى أمر.";
        }

        String clean =
                normalize(command);

        lastCommand = clean;

        try {

            String decision =
                    safe(
                            decisionEngine.decide(
                                    clean
                            )
                    );

            contextEngine.updateCommand(
                    clean,
                    decision
            );

            contextEngine.updateDecision(
                    decision
            );

            return
                    "تم تحليل الأمر ✓\n\n"
                            + "الأمر:\n"
                            + clean
                            + "\n\n"
                            + "القرار:\n"
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

        String clean =
                normalize(command);

        lastCommand = clean;
        lastExecutionTime =
                System.currentTimeMillis();

        lastStepCount = 0;
        lastSuccessfulSteps = 0;
        lastFailedStep = 0;
        lastRetryCount = 0;
        running = true;

        try {

            List<String> steps =
                    splitIntoSteps(clean);

            if (steps.isEmpty()) {
                return finish(
                        "ما قدرتش نفهم الأمر."
                );
            }

            if (steps.size() > MAX_STEPS) {

                return finish(
                        "الأتمتة فيها بزاف ديال الخطوات. الحد الأقصى هو "
                                + MAX_STEPS
                                + "."
                );
            }

            lastStepCount =
                    steps.size();

            String result;

            if (steps.size() == 1) {

                result =
                        executeSingleCommand(
                                steps.get(0)
                        );

                if (isEmpty(result)) {

                    result =
                            executeSequence(
                                    steps
                            );
                }

            } else {

                result =
                        executeSequence(
                                steps
                        );
            }

            return finish(
                    safe(result)
            );

        } catch (Exception e) {

            String error =
                    "وقع خطأ أثناء الأتمتة ⚠";

            try {

                contextEngine.updateResult(
                        error
                );

            } catch (Exception ignored) {
            }

            return finish(error);

        } finally {

            running = false;
        }
    }

    // =========================================================
    // SINGLE COMMAND
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

            return
                    androidControlEngine
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

            return
                    androidControlEngine
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

            return
                    androidControlEngine
                            .openYouTube();
        }

        if (containsAny(
                cmd,
                "افتح واتساب",
                "فتح واتساب",
                "whatsapp"
        )) {

            return
                    androidControlEngine
                            .openWhatsApp();
        }

        if (containsAny(
                cmd,
                "افتح انستغرام",
                "فتح انستغرام",
                "instagram"
        )) {

            return
                    androidControlEngine
                            .openInstagram();
        }

        if (containsAny(
                cmd,
                "افتح فيسبوك",
                "فتح فيسبوك",
                "facebook"
        )) {

            return
                    androidControlEngine
                            .openFacebook();
        }

        if (containsAny(
                cmd,
                "افتح كروم",
                "فتح كروم",
                "chrome"
        )) {

            return
                    androidControlEngine
                            .openChrome();
        }

        if (containsAny(
                cmd,
                "افتح الخرائط",
                "فتح الخرائط",
                "maps",
                "google maps"
        )) {

            return
                    androidControlEngine
                            .openMaps();
        }

        if (containsAny(
                cmd,
                "افتح الكاميرا",
                "فتح الكاميرا",
                "camera"
        )) {

            return
                    androidControlEngine
                            .openCamera();
        }

        if (containsAny(
                cmd,
                "افتح الساعة",
                "افتح المنبه",
                "المنبه",
                "clock",
                "alarm"
        )) {

            return
                    androidControlEngine
                            .openClock();
        }

        if (containsAny(
                cmd,
                "افتح الحاسبة",
                "افتح الآلة الحاسبة",
                "الحاسبة",
                "calculator"
        )) {

            return
                    androidControlEngine
                            .openCalculator();
        }

        // -----------------------------------------------------
        // APPLICATION BY NAME
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

                return
                        androidControlEngine
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

                return
                        androidControlEngine
                                .searchWeb(query);
            }
        }

        // -----------------------------------------------------
        // MAP SEARCH
        // -----------------------------------------------------

        if (startsWithAny(
                cmd,
                "قلب فالخريطة على ",
                "قلب في الخريطة على ",
                "ابحث فالخريطة عن ",
                "ابحث في الخريطة عن ",
                "search maps "
        )) {

            String query =
                    extractAfterPrefix(
                            command,
                            "قلب فالخريطة على",
                            "قلب في الخريطة على",
                            "ابحث فالخريطة عن",
                            "ابحث في الخريطة عن",
                            "search maps"
                    );

            if (!query.isEmpty()) {

                return
                        androidControlEngine
                                .searchMaps(query);
            }
        }

        // -----------------------------------------------------
        // WEBSITE
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

                return
                        androidControlEngine
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

                return
                        androidControlEngine
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

                return
                        androidControlEngine
                                .clickScreenElement(
                                        target
                                );
            }
        }

        // -----------------------------------------------------
        // TEXT INPUT
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

                return
                        androidControlEngine
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

            return
                    androidControlEngine
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

            return
                    androidControlEngine
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

            return
                    androidControlEngine
                            .getCurrentScreenText();
        }

        if (containsAny(
                cmd,
                "شوف شجرة الشاشة",
                "شجرة الشاشة",
                "screen tree"
        )) {

            return
                    androidControlEngine
                            .getCurrentScreenTree();
        }

        if (startsWithAny(
                cmd,
                "معلومات العنصر ",
                "node info "
        )) {

            String target =
                    extractAfterPrefix(
                            command,
                            "معلومات العنصر",
                            "node info"
                    );

            if (!target.isEmpty()) {

                return
                        androidControlEngine
                                .getNodeInfoByText(
                                        target
                                );
            }
        }

        // -----------------------------------------------------
        // TASKS
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
    // MULTI-STEP AUTOMATION
    // =========================================================

    private String executeSequence(
            List<String> steps
    ) {

        if (steps == null ||
                steps.isEmpty()) {

            return
                    "ما كاين حتى خطوة للتنفيذ.";
        }

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS AUTOMATION\n"
        );

        report.append(
                "=================\n\n"
        );

        int successful = 0;
        int failed = 0;

        for (int i = 0;
             i < steps.size();
             i++) {

            String step =
                    steps.get(i);

            if (isEmpty(step)) {
                continue;
            }

            report.append("STEP ")
                    .append(i + 1)
                    .append(":\n")
                    .append(step)
                    .append("\n");

            String result =
                    executeStepWithRetry(
                            step
                    );

            report.append("RESULT:\n")
                    .append(safe(result))
                    .append("\n\n");

            if (isSuccessResult(result)) {

                successful++;

                lastSuccessfulSteps =
                        successful;

            } else {

                failed++;

                lastFailedStep =
                        i + 1;
            }

            if (isHardFailure(result)) {

                report.append(
                        "AUTOMATION STOPPED ⚠\n"
                );

                report.append(
                        "توقف التنفيذ بسبب فشل الخطوة "
                );

                report.append(
                        i + 1
                );

                break;
            }
        }

        report.append(
                "SUMMARY:\n"
        );

        report.append(
                successful
        );

        report.append("/")
                .append(
                        steps.size()
                )
                .append(
                        " خطوات ناجحة."
                );

        if (failed == 0) {

            report.append(
                    "\n\nAUTOMATION COMPLETE ✓"
            );

        } else if (successful > 0) {

            report.append(
                    "\n\nAUTOMATION PARTIAL ⚠"
            );

        } else {

            report.append(
                    "\n\nAUTOMATION FAILED ✗"
            );
        }

        return report.toString();
    }

    // =========================================================
    // RETRY ENGINE
    // =========================================================

    private String executeStepWithRetry(
            String step
    ) {

        String last =
                "";

        for (int attempt = 0;
             attempt <= MAX_RETRIES;
             attempt++) {

            if (attempt > 0) {

                lastRetryCount++;

                try {

                    Thread.sleep(
                            350L * attempt
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread()
                            .interrupt();

                    return
                            "توقف التنفيذ أثناء إعادة المحاولة ⚠";
                }
            }

            last =
                    executeStep(step);

            if (isSuccessResult(last)) {
                return last;
            }

            if (isHardFailure(last)) {
                return last;
            }
        }

        return safe(last);
    }

    // =========================================================
    // STEP EXECUTION
    // =========================================================

    private String executeStep(
            String step
    ) {

        if (isEmpty(step)) {
            return "الخطوة فارغة.";
        }

        String clean =
                normalize(step);

        long delay =
                extractDelay(clean);

        if (delay >= 0L) {

            try {

                Thread.sleep(delay);

                return
                        "انتظرت "
                                + delay
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
    // DELAY PARSER
    // =========================================================

    private long extractDelay(
            String command
    ) {

        String value =
                normalize(command);

        String[] prefixes = {
                "انتظر ",
                "انتضر ",
                "تسنى ",
                "تسنا ",
                "wait "
        };

        for (String prefix :
                prefixes) {

            if (!value.startsWith(prefix)) {
                continue;
            }

            String rest =
                    value.substring(
                            prefix.length()
                    ).trim();

            if (rest.isEmpty()) {
                return -1L;
            }

            String[] parts =
                    rest.split(
                            "\\s+",
                            2
                    );

            if (parts.length == 0) {
                return -1L;
            }

            String numberText =
                    parts[0]
                            .replace(
                                    ",",
                                    "."
                            );

            try {

                double number =
                        Double.parseDouble(
                                numberText
                        );

                if (number < 0) {
                    return -1L;
                }

                long milliseconds;

                if (containsAny(
                        rest,
                        "ثانية",
                        "ثواني",
                        "second",
                        "seconds",
                        "sec"
                )) {

                    milliseconds =
                            (long)
                                    (number * 1000L);

                } else if (containsAny(
                        rest,
                        "دقيقة",
                        "دقائق",
                        "minute",
                        "minutes",
                        "min"
                )) {

                    milliseconds =
                            (long)
                                    (number * 60000L);

                } else {

                    milliseconds =
                            (long) number;
                }

                if (milliseconds >
                        MAX_DELAY_MS) {

                    milliseconds =
                            MAX_DELAY_MS;
                }

                return milliseconds;

            } catch (Exception ignored) {
                return -1L;
            }
        }

        return -1L;
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

        String value =
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

            value =
                    value.replace(
                            separator,
                            "||"
                    );
        }

        String[] parts =
                value.split(
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
                steps.add(clean);
            }
        }

        if (steps.isEmpty()) {
            steps.add(command.trim());
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

        String clean =
                task.trim();

        String result =
                taskManager.addTask(
                        clean
                );

        contextEngine.updateGoal(
                clean
        );

        contextEngine.updateAction(
                "ADD_TASK"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
    }

    public String completeTask(
            int index
    ) {

        String result =
                taskManager.completeTask(
                        index
                );

        contextEngine.updateAction(
                "COMPLETE_TASK"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
    }

    public String removeTask(
            int index
    ) {

        String result =
                taskManager.removeTask(
                        index
                );

        contextEngine.updateAction(
                "REMOVE_TASK"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
    }

    public String getTasks() {

        return safe(
                taskManager.getTasks()
        );
    }

    public String clearCompletedTasks() {

        String result =
                taskManager
                        .clearCompletedTasks();

        contextEngine.updateAction(
                "CLEAR_COMPLETED_TASKS"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
    }

    public String clearAllTasks() {

        String result =
                taskManager.clearTasks();

        contextEngine.updateAction(
                "CLEAR_ALL_TASKS"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
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

        try {

            String result =
                    contextEngine
                            .createContextPlan();

            contextEngine.updatePlan(
                    safe(result)
            );

            return safe(result);

        } catch (Exception e) {

            return
                    "ما قدرتش نبني الخطة دابا ⚠";
        }
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

        String result =
                reminderEngine.createReminder(
                        title.trim(),
                        triggerTime
                );

        contextEngine.updateAction(
                "CREATE_REMINDER"
        );

        contextEngine.updateResult(
                safe(result)
        );

        return safe(result);
    }

    public String getLastReminder() {

        return safe(
                reminderEngine.getLastReminder()
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getAutomationStatus() {

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
                "\nRunning: "
        );

        report.append(
                running
                        ? "YES"
                        : "NO"
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
                "\nLast Steps: "
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

        report.append(
                "\nRetries: "
        );

        report.append(
                lastRetryCount
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
                    && taskManager.isHealthy()
                    && reminderEngine.isHealthy()
                    && androidControlEngine.isHealthy();

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

    public int getLastRetryCount() {
        return lastRetryCount;
    }

    public boolean isRunning() {
        return running;
    }

    // =========================================================
    // RESULT PROCESSING
    // =========================================================

    private String finish(
            String result
    ) {

        String clean =
                safe(result);

        lastResult =
                clean;

        try {

            contextEngine.updateResult(
                    clean
            );

        } catch (Exception ignored) {
        }

        return clean;
    }

    private boolean isSuccessResult(
            String result
    ) {

        if (isEmpty(result)) {
            return false;
        }

        String value =
                normalize(result);

        if (isHardFailure(value)) {
            return false;
        }

        return containsAny(
                value,
                "✓",
                "تم ",
                "فتحت ",
                "رجعت ",
                "رجع",
                "قلبت ",
                "تم الضغط",
                "انتظرت ",
                "مهمة",
                "online"
        );
    }

    private boolean isHardFailure(
            String result
    ) {

        if (isEmpty(result)) {
            return true;
        }

        String value =
                normalize(result)
                        .toLowerCase(
                                Locale.ROOT
                        );

        return containsAny(
                value,
                "وقع خطأ",
                "خطأ أثناء",
                "فشل",
                "ما قدرتش",
                "ما قدرت",
                "ما لقيتش",
                "غير مفعلة",
                "غير متوفر",
                "غير جاهز",
                "توقف التنفيذ",
                "stopped",
                "failed",
                "error",
                "exception"
        );
    }

    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    private boolean isEmpty(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                values == null) {

            return false;
        }

        String source =
                text.toLowerCase(
                        Locale.ROOT
                );

        for (String value :
                values) {

            if (value == null) {
                continue;
            }

            String target =
                    value.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (!target.isEmpty()
                    && source.contains(target)) {

                return true;
            }
        }

        return false;
    }

    private boolean startsWithAny(
            String text,
            String... values
    ) {

        if (text == null ||
                values == null) {

            return false;
        }

        String source =
                text.toLowerCase(
                        Locale.ROOT
                );

        for (String value :
                values) {

            if (value == null) {
                continue;
            }

            String target =
                    value.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (!target.isEmpty()
                    && source.startsWith(target)) {

                return true;
            }
        }

        return false;
    }

    private String extractAfterPrefix(
            String command,
            String... prefixes
    ) {

        if (isEmpty(command) ||
                prefixes == null) {

            return "";
        }

        String original =
                command.trim();

        String lower =
                original.toLowerCase(
                        Locale.ROOT
                );

        for (String prefix :
                prefixes) {

            if (prefix == null) {
                continue;
            }

            String cleanPrefix =
                    prefix.trim();

            if (cleanPrefix.isEmpty()) {
                continue;
            }

            String lowerPrefix =
                    cleanPrefix.toLowerCase(
                            Locale.ROOT
                    );

            if (lower.startsWith(
                    lowerPrefix
            )) {

                return original
                        .substring(
                                cleanPrefix.length()
                        )
                        .trim();
            }
        }

        return "";
    }

    private String safe(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}