package com.kamal.jarvis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommandRouter {

    private final Context context;
    private final JarvisCore core;
    private final ScreenIntelligence screenIntelligence;

    public CommandRouter(Context context) {

        this.context =
                context.getApplicationContext();

        core =
                new JarvisCore(this.context);

        screenIntelligence =
                new ScreenIntelligence(this.context);
    }

    public String execute(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String original =
                command.trim();

        String cmd =
                original.toLowerCase(Locale.ROOT);

        // =========================
        // VOICE CONTROL
        // =========================

        if (containsAny(
                cmd,
                "سكت",
                "اسكت",
                "وقف الكلام",
                "وقف الصوت",
                "stop speaking")) {

            return "__STOP_SPEAKING__";
        }

        // =========================
        // GREETING
        // =========================

        if (containsAny(
                cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "أهلا",
                "hello",
                "hi")) {

            return
                    "مرحبا كمال. JARVIS CORE حاضر ومستعد.";
        }

        // =========================
        // HELP
        // =========================

        if (containsAny(
                cmd,
                "مساعدة",
                "ساعدني",
                "شنو نقدر ندير",
                "الأوامر",
                "الاوامر",
                "help")) {

            return getHelp();
        }

        // =========================
        // TIME
        // =========================

        if (containsAny(
                cmd,
                "الوقت",
                "شحال فالوقت",
                "كم الساعة",
                "الساعة")) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(new Date());

            return
                    "الوقت دابا هو " + time;
        }

        // =========================
        // DATE
        // =========================

        if (containsAny(
                cmd,
                "التاريخ",
                "نهار شحال",
                "اليوم شحال")) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date());

            return
                    "التاريخ اليوم هو " + date;
        }

        // =========================
        // WEBSITES
        // =========================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "youtube")) {

            openUrl(
                    "https://www.youtube.com"
            );

            return "فتحت YouTube.";
        }

        if (containsAny(
                cmd,
                "فتح جوجل",
                "افتح جوجل",
                "google")) {

            openUrl(
                    "https://www.google.com"
            );

            return "فتحت Google.";
        }

        if (containsAny(
                cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "facebook")) {

            openUrl(
                    "https://www.facebook.com"
            );

            return "فتحت Facebook.";
        }

        if (containsAny(
                cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "instagram")) {

            openUrl(
                    "https://www.instagram.com"
            );

            return "فتحت Instagram.";
        }

        if (containsAny(
                cmd,
                "فتح واتساب",
                "افتح واتساب",
                "whatsapp")) {

            openUrl(
                    "https://wa.me/"
            );

            return "فتحت WhatsApp.";
        }

        // =========================
        // PHONE SETTINGS
        // =========================

        if (containsAny(
                cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "الإعدادات",
                "الاعدادات",
                "settings")) {

            openSettings();

            return
                    "فتحت إعدادات الهاتف.";
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi")) {

            return core.openWifi();
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth")) {

            return core.openBluetooth();
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "حول الهاتف",
                "عن الهاتف")) {

            return core.openDeviceInfo();
        }

        if (containsAny(
                cmd,
                "إعدادات الإشعارات",
                "اعدادات الاشعارات",
                "notification settings")) {

            return core.openNotificationSettings();
        }

        if (containsAny(
                cmd,
                "إعدادات الشاشة",
                "اعدادات الشاشة",
                "display settings")) {

            return core.openDisplaySettings();
        }

        if (containsAny(
                cmd,
                "إعدادات الصوت",
                "اعدادات الصوت",
                "sound settings")) {

            return core.openSoundSettings();
        }

        if (containsAny(
                cmd,
                "إعدادات البطارية",
                "اعدادات البطارية",
                "battery settings")) {

            return core.openBatterySettings();
        }

        // =========================
        // REAL SCREEN INTELLIGENCE
        // =========================

        if (containsAny(
                cmd,
                "حلل الشاشة",
                "حلل لي الشاشة",
                "اقرأ الشاشة",
                "قرا الشاشة",
                "شوف الشاشة",
                "شنو كاين فالشاشة",
                "محتوى الشاشة",
                "analyze screen",
                "read screen")) {

            return screenIntelligence
                    .analyzeCurrentScreen();
        }

        if (containsAny(
                cmd,
                "حالة الشاشة",
                "screen status",
                "هل الشاشة متصلة",
                "هل تقدر تتحكم فالشاشة")) {

            return screenIntelligence
                    .getStatus();
        }

        if (containsAny(
                cmd,
                "شجرة الشاشة",
                "عناصر الشاشة",
                "screen tree",
                "screen elements")) {

            JarvisAccessibilityService service =
                    JarvisAccessibilityService
                            .getInstance();

            if (service == null) {

                return
                        "JARVIS: Accessibility Service غير مفعلة.";
            }

            return service.getScreenTree();
        }

        // =========================
        // CLICK SCREEN ELEMENT
        // =========================

        if (cmd.startsWith(
                "اضغط على "
        ) || cmd.startsWith(
                "ضغط على "
        ) || cmd.startsWith(
                "كليك على "
        ) || cmd.startsWith(
                "انقر على "
        ) || cmd.startsWith(
                "click "
        )) {

            String target =
                    original
                            .replaceFirst(
                                    "(?i)^اضغط\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^ضغط\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^كليك\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^انقر\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^click\\s*",
                                    ""
                            )
                            .trim();

            if (target.isEmpty()) {

                return
                        "شنو هو العنصر اللي بغيتي نضغط عليه؟";
            }

            return screenIntelligence
                    .clickElement(target);
        }

        // =========================
        // FIND SCREEN ELEMENT
        // =========================

        if (cmd.startsWith(
                "قلب على "
        ) || cmd.startsWith(
                "ابحث على "
        ) || cmd.startsWith(
                "لقى "
        ) || cmd.startsWith(
                "find "
        )) {

            String target =
                    original
                            .replaceFirst(
                                    "(?i)^قلب\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^ابحث\\s+على\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^لقى\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^find\\s*",
                                    ""
                            )
                            .trim();

            if (target.isEmpty()) {

                return
                        "شنو هو العنصر اللي بغيتي نقلب عليه؟";
            }

            return screenIntelligence
                    .findElement(target);
        }

        // =========================
        // TYPE TEXT
        // =========================

        if (cmd.startsWith(
                "كتب "
        )) {

            String text =
                    original
                            .replaceFirst(
                                    "(?i)^كتب\\s*",
                                    ""
                            )
                            .trim();

            if (text.isEmpty()) {

                return
                        "شنو بغيتي نكتب؟";
            }

            return screenIntelligence
                    .typeText(
                            "",
                            text
                    );
        }

        if (cmd.startsWith(
                "اكتب "
        )) {

            String text =
                    original
                            .replaceFirst(
                                    "(?i)^اكتب\\s*",
                                    ""
                            )
                            .trim();

            if (text.isEmpty()) {

                return
                        "شنو بغيتي نكتب؟";
            }

            return screenIntelligence
                    .typeText(
                            "",
                            text
                    );
        }

        // =========================
        // SCROLL
        // =========================

        if (containsAny(
                cmd,
                "سكرول لتحت",
                "سكرول للاسفل",
                "مرر لتحت",
                "مرر للأسفل",
                "انزل",
                "scroll down")) {

            return screenIntelligence
                    .scrollForward();
        }

        if (containsAny(
                cmd,
                "سكرول لفوق",
                "سكرول للاعلى",
                "مرر لفوق",
                "مرر للأعلى",
                "طلع",
                "scroll up")) {

            return screenIntelligence
                    .scrollBackward();
        }

        // =========================
        // MEMORY
        // =========================

        if (cmd.startsWith("تذكر ")
                || cmd.startsWith("احفظ ")
                || cmd.startsWith("حفظ ")) {

            String memory =
                    original
                            .replaceFirst(
                                    "(?i)^تذكر\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^احفظ\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^حفظ\\s*",
                                    ""
                            )
                            .trim();

            if (memory.isEmpty()) {

                return "شنو بغيتي نحفظ؟";
            }

            String key =
                    "memory_"
                            + System.currentTimeMillis();

            return core.remember(
                    key,
                    memory
            );
        }

        // =========================
        // SYSTEM STATUS
        // =========================

        if (containsAny(
                cmd,
                "حالة النظام",
                "حالتك",
                "ستاتوس",
                "status",
                "كيف داير")) {

            return core.getFullStatus();
        }

        // =========================
        // SELF DIAGNOSIS
        // =========================

        if (containsAny(
                cmd,
                "شخص نفسك",
                "شخص النظام",
                "التشخيص",
                "التشخيص الذاتي",
                "شنو ناقصك",
                "شنو ناقص",
                "ما الذي ينقصك",
                "self diagnosis",
                "diagnosis")) {

            return core.processCommand(
                    "التشخيص الذاتي"
            );
        }

        // =========================
        // SELF TEST
        // =========================

        if (containsAny(
                cmd,
                "اختبر نفسك",
                "اختبار النظام",
                "self test")) {

            return core.processCommand(
                    "اختبر نفسك"
            );
        }

        // =========================
        // RECOVERY
        // =========================

        if (containsAny(
                cmd,
                "صلح نفسك",
                "إصلاح النظام",
                "recovery")) {

            return core.processCommand(
                    "إصلاح النظام"
            );
        }

        // =========================
        // EVOLUTION
        // =========================

        if (containsAny(
                cmd,
                "طور نفسك",
                "طور ذاتك",
                "ابدأ التطور",
                "بدا التطور",
                "دورة التطور",
                "evolve",
                "evolution")) {

            return core.processCommand(
                    "طور نفسك"
            );
        }

        // =========================
        // CAPABILITIES
        // =========================

        if (containsAny(
                cmd,
                "شنو هي القدرات ديالك",
                "شنو عندك",
                "القدرات",
                "capabilities")) {

            return core.processCommand(
                    "القدرات"
            );
        }

        // =========================
        // SKILLS
        // =========================

        if (containsAny(
                cmd,
                "شنو هي المهارات ديالك",
                "المهارات",
                "skills")) {

            return core.processCommand(
                    "المهارات"
            );
        }

        // =========================
        // MEMORY STATUS
        // =========================

        if (containsAny(
                cmd,
                "شنو حافظ",
                "الذاكرة",
                "ذكرياتك",
                "memory")) {

            return core.processCommand(
                    "الذاكرة"
            );
        }

        // =========================
        // TASKS
        // =========================

        if (containsAny(
                cmd,
                "المهام",
                "شنو عندي من مهام",
                "tasks")) {

            return core.processCommand(
                    "المهام"
            );
        }

        // =========================
        // PLAN
        // =========================

        if (containsAny(
                cmd,
                "الخطة",
                "الخطة الحالية",
                "شنو هي الخطة",
                "plan")) {

            return core.processCommand(
                    "الخطة"
            );
        }

        // =========================
        // KNOWLEDGE
        // =========================

        if (containsAny(
                cmd,
                "المعرفة",
                "شنو كتعرف",
                "knowledge")) {

            return core.processCommand(
                    "المعرفة"
            );
        }

        // =========================
        // NOTIFICATIONS
        // =========================

        if (containsAny(
                cmd,
                "الإشعارات",
                "الاشعارات",
                "notifications")) {

            return core.processCommand(
                    "الإشعارات"
            );
        }

        // =========================
        // SCREEN
        // =========================

        if (containsAny(
                cmd,
                "الشاشة",
                "screen")) {

            return core.processCommand(
                    "الشاشة"
            );
        }

        // =========================
        // AUTOMATION
        // =========================

        if (containsAny(
                cmd,
                "الأتمتة",
                "الاتماتة",
                "automation")) {

            return core.processCommand(
                    "الأتمتة"
            );
        }

        // =========================
        // MONITOR
        // =========================

        if (containsAny(
                cmd,
                "المراقبة",
                "مراقبة النظام",
                "monitor")) {

            return core.processCommand(
                    "المراقبة"
            );
        }

        // =========================
        // DEVELOPMENT TARGET
        // =========================

        if (containsAny(
                cmd,
                "شنو الهدف الحالي",
                "الهدف الحالي")) {

            return core.getDevelopmentTarget();
        }

        // =========================
        // LEARNING
        // =========================

        if (cmd.startsWith("تعلم ")
                || cmd.startsWith("علمني ")) {

            String information =
                    original
                            .replaceFirst(
                                    "(?i)^تعلم\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^علمني\\s*",
                                    ""
                            )
                            .trim();

            if (information.isEmpty()) {

                return
                        "شنو بغيتي نتعلم؟";
            }

            return core.learn(
                    information,
                    information
            );
        }

        // =========================
        // TASK CREATION
        // =========================

        if (cmd.startsWith("مهمة ")
                || cmd.startsWith("دير مهمة ")) {

            String task =
                    original
                            .replaceFirst(
                                    "(?i)^دير\\s+مهمة\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^مهمة\\s*",
                                    ""
                            )
                            .trim();

            if (task.isEmpty()) {

                return "شنو هي المهمة؟";
            }

            return core.addTask(task);
        }

        // =========================
        // PLANNING
        // =========================

        if (cmd.startsWith("خطط ل")
                || cmd.startsWith("خطط ")) {

            String goal =
                    original
                            .replaceFirst(
                                    "(?i)^خطط\\s+ل\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^خطط\\s*",
                                    ""
                            )
                            .trim();

            if (goal.isEmpty()) {

                return
                        "شنو الهدف اللي بغيتي نخطط ليه؟";
            }

            return core.createPlan(goal);
        }

        // =========================
        // COMMAND LEARNING
        // =========================

        if (cmd.startsWith(
                "علم جارفيس "
        ) || cmd.startsWith(
                "علم جارفيس"
        )) {

            return
                    "تعليم الأوامر المتقدمة غادي يتربط مع Command Learning Engine.";
        }

        // =========================
        // CORE FALLBACK
        // =========================

        return core.processCommand(
                original
        );
    }

    // =========================
    // OPEN URL
    // =========================

    private void openUrl(
            String url
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(
                    intent
            );

        } catch (Exception e) {
            // Ignore
        }
    }

    // =========================
    // OPEN SETTINGS
    // =========================

    private void openSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(
                    intent
            );

        } catch (Exception e) {
            // Ignore
        }
    }

    // =========================
    // HELP
    // =========================

    private String getHelp() {

        StringBuilder help =
                new StringBuilder();

        help.append(
                "===== JARVIS COMMANDS =====\n\n"
        );

        help.append(
                "النظام:\n"
        );

        help.append(
                "- حالة النظام\n"
        );

        help.append(
                "- شنو عندك\n"
        );

        help.append(
                "- شنو هي المهارات ديالك\n"
        );

        help.append(
                "- شنو حافظ\n"
        );

        help.append(
                "- شنو ناقصك\n"
        );

        help.append(
                "- شخص نفسك\n"
        );

        help.append(
                "- اختبر نفسك\n"
        );

        help.append(
                "- صلح نفسك\n"
        );

        help.append(
                "- طور نفسك\n\n"
        );

        help.append(
                "التحكم في الشاشة:\n"
        );

        help.append(
                "- حلل الشاشة\n"
        );

        help.append(
                "- شنو كاين فالشاشة\n"
        );

        help.append(
                "- شجرة الشاشة\n"
        );

        help.append(
                "- اضغط على [العنصر]\n"
        );

        help.append(
                "- قلب على [العنصر]\n"
        );

        help.append(
                "- اكتب [النص]\n"
        );

        help.append(
                "- سكرول لتحت\n"
        );

        help.append(
                "- سكرول لفوق\n"
        );

        help.append(
                "- حالة الشاشة\n\n"
        );

        help.append(
                "الهاتف:\n"
        );

        help.append(
                "- افتح الإعدادات\n"
        );

        help.append(
                "- افتح الواي فاي\n"
        );

        help.append(
                "- افتح البلوتوث\n"
        );

        help.append(
                "- معلومات الهاتف\n"
        );

        help.append(
                "- إعدادات الإشعارات\n"
        );

        help.append(
                "- إعدادات الشاشة\n"
        );

        help.append(
                "- إعدادات الصوت\n"
        );

        help.append(
                "- إعدادات البطارية\n\n"
        );

        help.append(
                "الذاكرة والتعلم:\n"
        );

        help.append(
                "- تذكر [المعلومة]\n"
        );

        help.append(
                "- تعلم [المعلومة]\n"
        );

        help.append(
                "- شنو حافظ\n"
        );

        help.append(
                "- المعرفة\n\n"
        );

        help.append(
                "التنظيم:\n"
        );

        help.append(
                "- مهمة [المهمة]\n"
        );

        help.append(
                "- المهام\n"
        );

        help.append(
                "- خطط [الهدف]\n"
        );

        help.append(
                "- الخطة\n\n"
        );

        help.append(
                "التطبيقات والمواقع:\n"
        );

        help.append(
                "- افتح يوتيوب\n"
        );

        help.append(
                "- افتح جوجل\n"
        );

        help.append(
                "- افتح فيسبوك\n"
        );

        help.append(
                "- افتح انستغرام\n"
        );

        help.append(
                "- افتح واتساب\n\n"
        );

        help.append(
                "أوامر عامة:\n"
        );

        help.append(
                "- الوقت\n"
        );

        help.append(
                "- التاريخ\n"
        );

        help.append(
                "- سكت\n"
        );

        return help.toString();
    }

    // =========================
    // MATCH COMMAND
    // =========================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null) {
            return false;
        }

        String normalized =
                text.toLowerCase(
                        Locale.ROOT
                );

        for (String value : values) {

            if (value != null &&
                    normalized.contains(
                            value.toLowerCase(
                                    Locale.ROOT
                            )
                    )) {

                return true;
            }
        }

        return false;
    }
}