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

    private String lastCommand = "";
    private String lastResult = "";
    private long lastExecutionTime = 0L;

    public AutomationEngine(Context context) {

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

    public String analyzeCommand(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

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

        lastCommand = cleanCommand;

        return
                "تم تحليل الأمر ✓\n\n"
                + "الأمر:\n"
                + cleanCommand
                + "\n\n"
                + "نوع العملية:\n"
                + decision;
    }

    public String execute(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String cleanCommand =
                command.trim();

        lastCommand =
                cleanCommand;

        lastExecutionTime =
                System.currentTimeMillis();

        try {

            /*
             * الأتمتة متعددة الخطوات خاصها تتفحص
             * قبل الأمر المفرد.
             *
             * مثال:
             * افتح يوتيوب ثم انتظر 2 ثانية ثم رجع
             *
             * ما خاصش JARVIS ينفذ غير "افتح يوتيوب".
             */

            List<String> steps =
                    splitIntoSteps(cleanCommand);

            if (steps.size() > 1) {

                String result =
                        executeSequence(cleanCommand);

                lastResult =
                        result;

                contextEngine.updateCommand(
                        cleanCommand,
                        "AUTOMATION"
                );

                contextEngine.updateDecision(
                        "AUTOMATION"
                );

                return result;
            }

            String singleResult =
                    executeSingleCommand(
                            cleanCommand
                    );

            if (singleResult != null &&
                    !singleResult.trim().isEmpty()) {

                lastResult =
                        singleResult;

                contextEngine.updateCommand(
                        cleanCommand,
                        "EXECUTED"
                );

                contextEngine.updateDecision(
                        "EXECUTED"
                );

                return singleResult;
            }

            String result =
                    executeSequence(
                            cleanCommand
                    );

            lastResult =
                    result;

            contextEngine.updateCommand(
                    cleanCommand,
                    "AUTOMATION"
            );

            contextEngine.updateDecision(
                    "AUTOMATION"
            );

            return result;

        } catch (Exception e) {

            lastResult =
                    "وقع خطأ أثناء التنفيذ.";

            return
                    "JARVIS Automation: وقع خطأ أثناء التنفيذ ⚠";
        }
    }

    private String executeSingleCommand(
            String command
    ) {

        String cmd =
                normalize(command);

        if (cmd.isEmpty()) {

            return "الأمر فارغ.";
        }

        if (containsAny(
                cmd,
                "رجع للرئيسية",
                "الصفحة الرئيسية",
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
                "recents"
        )) {

            return androidControlEngine.openRecents();
        }

        if (containsAny(
                cmd,
                "افتح الإشعارات",
                "افتح الاشعارات",
                "notification panel"
        )) {

            return androidControlEngine.openNotifications();
        }

        if (containsAny(
                cmd,
                "لوحة الاختصارات",
                "افتح لوحة الاختصارات",
                "quick settings"
        )) {

            return androidControlEngine.openQuickSettings();
        }

        if (containsAny(
                cmd,
                "قفل الشاشة",
                "قفل الهاتف",
                "lock screen"
        )) {

            return androidControlEngine.lockScreen();
        }

        if (containsAny(
                cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "فتح الإعدادات",
                "settings"
        )) {

            return androidControlEngine.openSettings();
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi"
        )) {

            return androidControlEngine.openWifiSettings();
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {

            return androidControlEngine
                    .openBluetoothSettings();
        }

        if (containsAny(
                cmd,
                "افتح إمكانية الوصول",
                "افتح الاكسيسيبيليتي",
                "accessibility"
        )) {

            return androidControlEngine
                    .openAccessibilitySettings();
        }

        if (containsAny(
                cmd,
                "افتح يوتيوب",
                "فتح يوتيوب",
                "youtube"
        )) {

            return androidControlEngine.openYouTube();
        }

        if (containsAny(
                cmd,
                "افتح واتساب",
                "فتح واتساب",
                "whatsapp"
        )) {

            return androidControlEngine.openWhatsApp();
        }

        if (containsAny(
                cmd,
                "افتح انستغرام",
                "فتح انستغرام",
                "instagram"
        )) {

            return androidControlEngine.openInstagram();
        }

        if (containsAny(
                cmd,
                "افتح فيسبوك",
                "فتح فيسبوك",
                "facebook"
        )) {

            return androidControlEngine.openFacebook();
        }

        if (containsAny(
                cmd,
                "افتح كروم",
                "فتح كروم",
                "chrome"
        )) {

            return androidControlEngine.openChrome();
        }

        if (cmd.startsWith(
                "افتح تطبيق "
        )) {

            String app =
                    removePrefix(
                            command,
                            "افتح تطبيق"
                    );

            if (!app.isEmpty()) {

                return androidControlEngine
                        .openApplicationByName(app);
            }
        }

        if (cmd.startsWith(
                "فتح تطبيق "
        )) {

            String app =
                    removePrefix(
                            command,
                            "فتح تطبيق"
                    );

            if (!app.isEmpty()) {

                return androidControlEngine
                        .openApplicationByName(app);
            }
        }

        if (cmd.startsWith(
                "اضغط على "
        ) ||
                cmd.startsWith(
                        "ضغط على "
                ) ||
                cmd.startsWith(
                        "كليك على "
                ) ||
                cmd.startsWith(
                        "انقر على "
                ) ||
                cmd.startsWith(
                        "click "
                )) {

            String target =
                    extractTarget(
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

        if (cmd.startsWith(
                "اكتب "
        )) {

            String text =
                    removePrefix(
                            command,
                            "اكتب"
                    );

            if (!text.isEmpty()) {

                return androidControlEngine
                        .typeIntoScreen(
                                "",
                                text
                        );
            }
        }

        if (cmd.startsWith(
                "كتب "
        )) {

            String text =
                    removePrefix(
                            command,
                            "كتب"
                    );

            if (!text.isEmpty()) {

                return androidControlEngine
                        .typeIntoScreen(
                                "",
                                text
                        );
            }
        }

        if (containsAny(
                cmd,
                "سكرول لتحت",
                "سكرول للاسفل",
                "سكرول للأسفل",
                "مرر لتحت",
                "مرر للأسفل",
                "انزل",
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
                "scroll up"
        )) {

            return androidControlEngine
                    .scrollUp();
        }

        if (containsAny(
                cmd,
                "شوف الشاشة",
                "اقرأ الشاشة",
                "حلل الشاشة",
                "analyze screen"
        )) {

            JarvisAccessibilityService service =
                    JarvisAccessibilityService
                            .getInstance();

            if (service == null) {

                return
                        "Accessibility Service غير مفعلة.";
            }

            return service.getScreenText();
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "معلومات الجهاز",
                "حول الهاتف"
        )) {

            return androidControlEngine
                    .openDeviceInformation();
        }

        return null;
    }

    private String executeSequence(
            String command
    ) {

        List<String> steps =
                splitIntoSteps(command);

        if (steps.isEmpty()) {

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

        int totalCount =
                steps.size();

        for (
                int i = 0;
                i < totalCount;
                i++
        ) {

            String step =
                    steps.get(i);

            report.append(
                    "STEP "
            )
                    .append(
                            i + 1
                    )
                    .append(
                            ":\n"
                    );

            report.append(step);
            report.append("\n");

            String result =
                    executeStep(step);

            if (result == null) {

                result =
                        "الأمر ما تعالجش.";
            }

            report.append(
                    "RESULT:\n"
            );

            report.append(result);
            report.append("\n\n");

            if (isSuccessResult(result)) {

                successCount++;
            }

            if (isHardFailure(result)) {

                report.append(
                        "AUTOMATION STOPPED ⚠\n"
                );

                report.append(
                        "سبب التوقف: فشل تنفيذ الخطوة الحالية."
                );

                return report.toString();
            }
        }

        report.append(
                "SUMMARY:\n"
        );

        report.append(successCount);
        report.append("/");
        report.append(totalCount);

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

        return report.toString();
    }

    private String executeStep(
            String step
    ) {

        if (step == null ||
                step.trim().isEmpty()) {

            return "الخطوة فارغة.";
        }

        String clean =
                step.trim();

        long delay =
                extractDelay(clean);

        if (delay >= 0L) {

            try {

                Thread.sleep(
                        Math.min(
                                delay,
                                10000L
                        )
                );

                return
                        "انتظرت "
                                + delay
                                + "ms ✓";

            } catch (
                    InterruptedException e
            ) {

                Thread.currentThread()
                        .interrupt();

                return
                        "التنفيذ توقف أثناء الانتظار ⚠";
            }
        }

        String result =
                executeSingleCommand(clean);

        if (result != null) {

            return result;
        }

        return
                "JARVIS: ما فهمتش هاد الخطوة:\n"
                        + clean;
    }

    private List<String> splitIntoSteps(
            String command
    ) {

        List<String> steps =
                new ArrayList<>();

        if (command == null) {

            return steps;
        }

        String normalized =
                command
                        .trim()
                        .replace(
                                " ثم ",
                                "||"
                        )
                        .replace(
                                " ومن بعد ",
                                "||"
                        )
                        .replace(
                                " وبعدها ",
                                "||"
                        )
                        .replace(
                                " بعد ذلك ",
                                "||"
                        )
                        .replace(
                                " وبعد ",
                                "||"
                        )
                        .replace(
                                " و من بعد ",
                                "||"
                        )
                        .replace(
                                " ; ",
                                "||"
                        )
                        .replace(
                                ";",
                                "||"
                        );

        String[] parts =
                normalized.split(
                        "\\|\\|"
                );

        for (String part : parts) {

            String clean =
                    part.trim();

            if (!clean.isEmpty()) {

                steps.add(clean);
            }
        }

        if (steps.isEmpty() &&
                !command.trim().isEmpty()) {

            steps.add(
                    command.trim()
            );
        }

        return steps;
    }

    public String createTask(
            String task
    ) {

        if (task == null ||
                task.trim().isEmpty()) {

            return
                    "حدد المهمة اللي بغيتي نضيف.";
        }

        String result =
                taskManager.addTask(
                        task.trim()
                );

        contextEngine.updateGoal(
                task.trim()
        );

        return result;
    }

    public String completeTask(
            int index
    ) {

        return taskManager.completeTask(index);
    }

    public String removeTask(
            int index
    ) {

        return taskManager.removeTask(index);
    }

    public String getTasks() {

        return taskManager.getTasks();
    }

    public String clearCompletedTasks() {

        return taskManager.clearCompleted();
    }

    public String clearAllTasks() {

        return taskManager.clearAll();
    }

    public String createPlan(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return "حدد الهدف الأول.";
        }

        contextEngine.updateGoal(
                goal.trim()
        );

        return contextEngine
                .createContextPlan();
    }

    public String createReminder(
            String title,
            long triggerTime
    ) {

        if (title == null ||
                title.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم التذكير.";
        }

        return reminderEngine
                .createReminder(
                        title.trim(),
                        triggerTime
                );
    }

    public String getLastReminder() {

        return reminderEngine
                .getLastReminder();
    }

    public String getAutomationStatus() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "=== AUTOMATION ENGINE ===\n\n"
        );

        report.append(
                "Engine: ONLINE ✓\n"
        );

        report.append(
                "Decision Engine: "
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
                        androidControlEngine
                                .getStatus()
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

        return report.toString();
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Automation Engine: ONLINE ✓";
        }

        return
                "Automation Engine: ERROR ⚠";
    }

    public boolean isHealthy() {

        try {

            decisionEngine.getStatus();
            taskManager.getStatus();
            reminderEngine.getStatus();
            androidControlEngine.getStatus();
            contextEngine.getStatus();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getLastCommand() {

        return lastCommand;
    }

    public String getLastResult() {

        return lastResult;
    }

    public long getLastExecutionTime() {

        return lastExecutionTime;
    }

    private long extractDelay(
            String command
    ) {

        String lower =
                normalize(command);

        if (!lower.startsWith(
                "انتظر "
        ) &&
                !lower.startsWith(
                        "استنى "
                ) &&
                !lower.startsWith(
                        "wait "
                )) {

            return -1L;
        }

        String value =
                lower
                        .replace(
                                "انتظر",
                                ""
                        )
                        .replace(
                                "استنى",
                                ""
                        )
                        .replace(
                                "wait",
                                ""
                        )
                        .trim();

        if (value.isEmpty()) {

            return 1000L;
        }

        try {

            String digits =
                    value.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (digits.isEmpty()) {

                return 1000L;
            }

            long number =
                    Long.parseLong(digits);

            if (value.contains(
                    "ثانيه"
            ) ||
                    value.contains(
                            "ثانية"
                    ) ||
                    value.contains(
                            "second"
                    )) {

                return number * 1000L;
            }

            if (value.contains(
                    "دقيقه"
            ) ||
                    value.contains(
                            "دقيقة"
                    ) ||
                    value.contains(
                            "minute"
                    )) {

                return number * 60000L;
            }

            return number;

        } catch (Exception e) {

            return 1000L;
        }
    }

    private boolean isSuccessResult(
            String result
    ) {

        if (result == null) {

            return false;
        }

        String value =
                normalize(result);

        return
                !value.contains(
                        "ما قدرتش"
                )
                && !value.contains(
                        "ما لقيتش"
                )
                && !value.contains(
                        "غير مفعله"
                )
                && !value.contains(
                        "غير مفعلة"
                )
                && !value.contains(
                        "فشل"
                )
                && !value.contains(
                        "error"
                )
                && !value.contains(
                        "وقع خطا"
                )
                && !value.contains(
                        "وقع خطأ"
                );
    }

    private boolean isHardFailure(
            String result
    ) {

        if (result == null) {

            return true;
        }

        String value =
                normalize(result);

        return
                value.contains(
                        "وقع خطا"
                )
                || value.contains(
                        "وقع خطأ"
                )
                || value.contains(
                        "error"
                )
                || value.contains(
                        "فشل التنفيذ"
                );
    }

    private String removePrefix(
            String original,
            String prefix
    ) {

        if (original == null ||
                prefix == null) {

            return "";
        }

        String value =
                original.trim();

        String lowerValue =
                normalize(value);

        String lowerPrefix =
                normalize(prefix);

        if (lowerValue.startsWith(
                lowerPrefix
        )) {

            return value
                    .substring(
                            prefix.length()
                    )
                    .trim();
        }

        return "";
    }

    private String extractTarget(
            String command,
            String... prefixes
    ) {

        if (command == null) {

            return "";
        }

        for (String prefix : prefixes) {

            String result =
                    removePrefix(
                            command,
                            prefix
                    );

            if (!result.isEmpty()) {

                return result;
            }
        }

        return "";
    }

    private String normalize(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null) {

            return false;
        }

        String normalized =
                normalize(text);

        for (String value : values) {

            if (value == null) {

                continue;
            }

            String target =
                    normalize(value);

            if (!target.isEmpty() &&
                    normalized.contains(target)) {

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