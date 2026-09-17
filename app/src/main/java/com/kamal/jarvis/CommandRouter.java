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
    private final AndroidControlEngine androidControl;

    public CommandRouter(Context context) {

        this.context =
                context.getApplicationContext();

        core =
                new JarvisCore(
                        this.context
                );

        screenIntelligence =
                new ScreenIntelligence(
                        this.context
                );

        androidControl =
                new AndroidControlEngine(
                        this.context
                );
    }

    public String execute(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String original =
                command.trim();

        String cmd =
                normalize(original);

        // =====================================================
        // VOICE CONTROL
        // =====================================================

        if (containsAny(
                cmd,
                "سكت",
                "اسكت",
                "وقف الكلام",
                "وقف الصوت",
                "سكت جارفيس",
                "stop speaking"
        )) {

            return "__STOP_SPEAKING__";
        }

        // =====================================================
        // GREETING
        // =====================================================

        if (containsAny(
                cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "مرحبا جارفيس",
                "اهلا",
                "أهلا",
                "hello",
                "hi"
        )) {

            return
                    "مرحبا كمال. JARVIS CORE حاضر ومستعد.";
        }

        // =====================================================
        // HELP
        // =====================================================

        if (containsAny(
                cmd,
                "مساعدة",
                "ساعدني",
                "شنو نقدر ندير",
                "شنو تقدر تدير",
                "الأوامر",
                "الاوامر",
                "help"
        )) {

            return getHelp();
        }

        // =====================================================
        // TIME
        // =====================================================

        if (containsAny(
                cmd,
                "الوقت",
                "شحال فالوقت",
                "كم الساعة",
                "الساعة"
        )) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            return
                    "الوقت دابا هو "
                            + time;
        }

        // =====================================================
        // DATE
        // =====================================================

        if (containsAny(
                cmd,
                "التاريخ",
                "نهار شحال",
                "اليوم شحال",
                "شنو هو التاريخ"
        )) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            return
                    "التاريخ اليوم هو "
                            + date;
        }

        // =====================================================
        // OPEN COMMON APPS
        // =====================================================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "حل يوتيوب",
                "youtube"
        )) {

            return
                    androidControl
                            .openYouTube();
        }

        if (containsAny(
                cmd,
                "فتح واتساب",
                "افتح واتساب",
                "حل واتساب",
                "whatsapp"
        )) {

            return
                    androidControl
                            .openWhatsApp();
        }

        if (containsAny(
                cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "حل انستغرام",
                "instagram"
        )) {

            return
                    androidControl
                            .openInstagram();
        }

        if (containsAny(
                cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "حل فيسبوك",
                "facebook"
        )) {

            return
                    androidControl
                            .openFacebook();
        }

        if (containsAny(
                cmd,
                "فتح كروم",
                "افتح كروم",
                "حل كروم",
                "chrome"
        )) {

            return
                    androidControl
                            .openChrome();
        }

        // =====================================================
        // OPEN GOOGLE
        // =====================================================

        if (containsAny(
                cmd,
                "فتح جوجل",
                "افتح جوجل",
                "حل جوجل",
                "google"
        )) {

            return
                    androidControl.openWebPage(
                            "https://www.google.com"
                    );
        }

        // =====================================================
        // WEB SEARCH
        // =====================================================

        if (cmd.startsWith(
                "ابحث في جوجل "
        )) {

            String query =
                    removePrefix(
                            original,
                            "ابحث في جوجل"
                    );

            if (query.isEmpty()) {

                return
                        "شنو بغيتي نقلب عليه؟";
            }

            return
                    androidControl
                            .searchWeb(query);
        }

        if (cmd.startsWith(
                "قلب في جوجل "
        )) {

            String query =
                    removePrefix(
                            original,
                            "قلب في جوجل"
                    );

            if (query.isEmpty()) {

                return
                        "شنو بغيتي نقلب عليه؟";
            }

            return
                    androidControl
                            .searchWeb(query);
        }

        if (cmd.startsWith(
                "ابحث عن "
        )) {

            String query =
                    removePrefix(
                            original,
                            "ابحث عن"
                    );

            if (query.isEmpty()) {

                return
                        "شنو بغيتي نقلب عليه؟";
            }

            return
                    androidControl
                            .searchWeb(query);
        }

        if (cmd.startsWith(
                "ابحث على "
        )) {

            String query =
                    removePrefix(
                            original,
                            "ابحث على"
                    );

            if (query.isEmpty()) {

                return
                        "شنو بغيتي نقلب عليه؟";
            }

            return
                    androidControl
                            .searchWeb(query);
        }

        // =====================================================
        // OPEN URL
        // =====================================================

        if (cmd.startsWith(
                "افتح الرابط "
        )) {

            String url =
                    removePrefix(
                            original,
                            "افتح الرابط"
                    );

            if (url.isEmpty()) {

                return
                        "عطيني الرابط.";
            }

            return
                    androidControl
                            .openWebPage(url);
        }

        // =====================================================
        // PHONE
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الهاتف",
                "افتح الاتصال",
                "فتح الهاتف",
                "dialer"
        )) {

            return
                    androidControl
                            .openPhoneDialer();
        }

        if (cmd.startsWith(
                "اتصل ب "
        )) {

            String number =
                    removePrefix(
                            original,
                            "اتصل ب"
                    );

            if (number.isEmpty()) {

                return
                        "عطيني رقم الهاتف.";
            }

            return
                    androidControl
                            .dialNumber(number);
        }

        if (cmd.startsWith(
                "اتصل ب"
        )) {

            String number =
                    removePrefix(
                            original,
                            "اتصل ب"
                    );

            if (number.isEmpty()) {

                return
                        "عطيني رقم الهاتف.";
            }

            return
                    androidControl
                            .dialNumber(number);
        }

        // =====================================================
        // SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "فتح الإعدادات",
                "الإعدادات",
                "الاعدادات",
                "settings"
        )) {

            return
                    androidControl
                            .openSettings();
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi",
                "wi fi"
        )) {

            return
                    androidControl
                            .openWifiSettings();
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {

            return
                    androidControl
                            .openBluetoothSettings();
        }

        if (containsAny(
                cmd,
                "إعدادات التطبيقات",
                "اعدادات التطبيقات",
                "application settings"
        )) {

            return
                    androidControl
                            .openApplicationSettings();
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "معلومات الجهاز",
                "حول الهاتف",
                "عن الهاتف"
        )) {

            return
                    androidControl
                            .openDeviceInformation();
        }

        if (containsAny(
                cmd,
                "إعدادات الإشعارات",
                "اعدادات الاشعارات",
                "notification settings"
        )) {

            return
                    androidControl
                            .openNotificationSettings();
        }

        if (containsAny(
                cmd,
                "إعدادات الشاشة",
                "اعدادات الشاشة",
                "display settings"
        )) {

            return
                    androidControl
                            .openDisplaySettings();
        }

        if (containsAny(
                cmd,
                "إعدادات الصوت",
                "اعدادات الصوت",
                "sound settings"
        )) {

            return
                    androidControl
                            .openSoundSettings();
        }

        if (containsAny(
                cmd,
                "إعدادات البطارية",
                "اعدادات البطارية",
                "battery settings"
        )) {

            return
                    androidControl
                            .openBatterySettings();
        }

        if (containsAny(
                cmd,
                "إعدادات إمكانية الوصول",
                "اعدادات إمكانية الوصول",
                "accessibility settings"
        )) {

            return
                    androidControl
                            .openAccessibilitySettings();
        }

        // =====================================================
        // ACCESSIBILITY STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "حالة إمكانية الوصول",
                "حالة الاكسيسيبيليتي",
                "حالة accessibility",
                "هل التحكم فالشاشة خدام"
        )) {

            return
                    androidControl
                            .getAccessibilityStatus();
        }

        // =====================================================
        // GLOBAL PHONE CONTROL
        // =====================================================

        if (containsAny(
                cmd,
                "رجع للرئيسية",
                "رجع للصفحة الرئيسية",
                "الصفحة الرئيسية",
                "home"
        )) {

            return
                    androidControl
                            .goHome();
        }

        if (containsAny(
                cmd,
                "رجع للور",
                "رجوع",
                "رجع خطوة",
                "back"
        )) {

            return
                    androidControl
                            .goBack();
        }

        if (containsAny(
                cmd,
                "افتح التطبيقات الأخيرة",
                "افتح التطبيقات الاخيرة",
                "التطبيقات الأخيرة",
                "recents"
        )) {

            return
                    androidControl
                            .openRecents();
        }

        if (containsAny(
                cmd,
                "افتح الإشعارات",
                "افتح الاشعارات",
                "الإشعارات",
                "الاشعارات"
        )) {

            return
                    androidControl
                            .openNotifications();
        }

        if (containsAny(
                cmd,
                "افتح لوحة الاختصارات",
                "لوحة الاختصارات",
                "quick settings"
        )) {

            return
                    androidControl
                            .openQuickSettings();
        }

        if (containsAny(
                cmd,
                "قفل الشاشة",
                "قفل الهاتف",
                "lock screen"
        )) {

            return
                    androidControl
                            .lockScreen();
        }

        // =====================================================
        // OPEN APPLICATION BY NAME
        // =====================================================

        if (cmd.startsWith(
                "افتح تطبيق "
        )) {

            String app =
                    removePrefix(
                            original,
                            "افتح تطبيق"
                    );

            if (app.isEmpty()) {

                return
                        "شنو هو اسم التطبيق؟";
            }

            return
                    androidControl
                            .openApplicationByName(app);
        }

        if (cmd.startsWith(
                "فتح تطبيق "
        )) {

            String app =
                    removePrefix(
                            original,
                            "فتح تطبيق"
                    );

            if (app.isEmpty()) {

                return
                        "شنو هو اسم التطبيق؟";
            }

            return
                    androidControl
                            .openApplicationByName(app);
        }

        if (cmd.startsWith(
                "حل تطبيق "
        )) {

            String app =
                    removePrefix(
                            original,
                            "حل تطبيق"
                    );

            if (app.isEmpty()) {

                return
                        "شنو هو اسم التطبيق؟";
            }

            return
                    androidControl
                            .openApplicationByName(app);
        }

        // =====================================================
        // SCREEN INTELLIGENCE
        // =====================================================

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
                "read screen"
        )) {

            return
                    screenIntelligence
                            .analyzeCurrentScreen();
        }

        if (containsAny(
                cmd,
                "حالة الشاشة",
                "screen status",
                "هل الشاشة متصلة",
                "هل تقدر تتحكم فالشاشة"
        )) {

            return
                    screenIntelligence
                            .getStatus();
        }

        if (containsAny(
                cmd,
                "شجرة الشاشة",
                "عناصر الشاشة",
                "screen tree",
                "screen elements"
        )) {

            return
                    screenIntelligence
                            .getCurrentScreenTree();
        }

        if (containsAny(
                cmd,
                "نص الشاشة",
                "اقرأ نص الشاشة",
                "ماذا يوجد مكتوب",
                "شنو مكتوب فالشاشة"
        )) {

            return
                    screenIntelligence
                            .getCurrentScreenText();
        }

        // =====================================================
        // CLICK SCREEN ELEMENT
        // =====================================================

        if (cmd.startsWith(
                "اضغط على "
        )
                || cmd.startsWith(
                "ضغط على "
        )
                || cmd.startsWith(
                "كليك على "
        )
                || cmd.startsWith(
                "انقر على "
        )
                || cmd.startsWith(
                "click "
        )) {

            String target =
                    extractTarget(
                            original,
                            "اضغط على",
                            "ضغط على",
                            "كليك على",
                            "انقر على",
                            "click"
                    );

            if (target.isEmpty()) {

                return
                        "شنو هو العنصر اللي بغيتي نضغط عليه؟";
            }

            return
                    screenIntelligence
                            .clickElement(target);
        }

        // =====================================================
        // FIND SCREEN ELEMENT
        // =====================================================

        if (cmd.startsWith(
                "قلب على "
        )
                || cmd.startsWith(
                "ابحث على "
        )
                || cmd.startsWith(
                "لقى "
        )
                || cmd.startsWith(
                "find "
        )) {

            String target =
                    extractTarget(
                            original,
                            "قلب على",
                            "ابحث على",
                            "لقى",
                            "find"
                    );

            if (target.isEmpty()) {

                return
                        "شنو هو العنصر اللي بغيتي نقلب عليه؟";
            }

            return
                    screenIntelligence
                            .findElement(target);
        }

        // =====================================================
        // TYPE TEXT
        // =====================================================

        if (cmd.startsWith(
                "كتب "
        )) {

            String text =
                    removePrefix(
                            original,
                            "كتب"
                    );

            if (text.isEmpty()) {

                return
                        "شنو بغيتي نكتب؟";
            }

            return
                    screenIntelligence
                            .typeText(
                                    "",
                                    text
                            );
        }

        if (cmd.startsWith(
                "اكتب "
        )) {

            String text =
                    removePrefix(
                            original,
                            "اكتب"
                    );

            if (text.isEmpty()) {

                return
                        "شنو بغيتي نكتب؟";
            }

            return
                    screenIntelligence
                            .typeText(
                                    "",
                                    text
                            );
        }

        // =====================================================
        // SCROLL
        // =====================================================

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

            return
                    screenIntelligence
                            .scrollForward();
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

            return
                    screenIntelligence
                            .scrollBackward();
        }

        // =====================================================
        // MEMORY
        // =====================================================

        if (cmd.startsWith(
                "تذكر "
        )
                || cmd.startsWith(
                "احفظ "
        )
                || cmd.startsWith(
                "حفظ "
        )) {

            String memory =
                    extractTarget(
                            original,
                            "تذكر",
                            "احفظ",
                            "حفظ"
                    );

            if (memory.isEmpty()) {

                return
                        "شنو بغيتي نحفظ؟";
            }

            String key =
                    "memory_"
                            + System.currentTimeMillis();

            return
                    core.remember(
                            key,
                            memory
                    );
        }

        // =====================================================
        // SYSTEM STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "حالة النظام",
                "حالتك",
                "ستاتوس",
                "status",
                "كيف داير"
        )) {

            return
                    core.getFullStatus()
                            + "\n\n"
                            + androidControl.getStatus()
                            + "\n"
                            + screenIntelligence.getStatus();
        }

        // =====================================================
        // SELF DIAGNOSIS
        // =====================================================

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
                "diagnosis"
        )) {

            return
                    core.processCommand(
                            "التشخيص الذاتي"
                    );
        }

        // =====================================================
        // SELF TEST
        // =====================================================

        if (containsAny(
                cmd,
                "اختبر نفسك",
                "اختبار النظام",
                "self test"
        )) {

            return
                    core.processCommand(
                            "اختبر نفسك"
                    );
        }

        // =====================================================
        // RECOVERY
        // =====================================================

        if (containsAny(
                cmd,
                "صلح نفسك",
                "إصلاح النظام",
                "اصلح النظام",
                "recovery"
        )) {

            return
                    core.processCommand(
                            "إصلاح النظام"
                    );
        }

        // =====================================================
        // EVOLUTION
        // =====================================================

        if (containsAny(
                cmd,
                "طور نفسك",
                "طور ذاتك",
                "ابدأ التطور",
                "بدا التطور",
                "دورة التطور",
                "evolve",
                "evolution"
        )) {

            return
                    core.processCommand(
                            "طور نفسك"
                    );
        }

        // =====================================================
        // CAPABILITIES
        // =====================================================

        if (containsAny(
                cmd,
                "شنو هي القدرات ديالك",
                "شنو عندك",
                "القدرات",
                "capabilities"
        )) {

            return
                    core.processCommand(
                            "القدرات"
                    );
        }

        // =====================================================
        // SKILLS
        // =====================================================

        if (containsAny(
                cmd,
                "شنو هي المهارات ديالك",
                "المهارات",
                "skills"
        )) {

            return
                    core.processCommand(
                            "المهارات"
                    );
        }

        // =====================================================
        // MEMORY STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "شنو حافظ",
                "الذاكرة",
                "ذكرياتك",
                "memory"
        )) {

            return
                    core.processCommand(
                            "الذاكرة"
                    );
        }

        // =====================================================
        // TASKS
        // =====================================================

        if (containsAny(
                cmd,
                "المهام",
                "شنو عندي من مهام",
                "tasks"
        )) {

            return
                    core.processCommand(
                            "المهام"
                    );
        }

        // =====================================================
        // PLAN
        // =====================================================

        if (containsAny(
                cmd,
                "الخطة",
                "الخطة الحالية",
                "شنو هي الخطة",
                "plan"
        )) {

            return
                    core.processCommand(
                            "الخطة"
                    );
        }

        // =====================================================
        // KNOWLEDGE
        // =====================================================

        if (containsAny(
                cmd,
                "المعرفة",
                "شنو كتعرف",
                "knowledge"
        )) {

            return
                    core.processCommand(
                            "المعرفة"
                    );
        }

        // =====================================================
        // NOTIFICATIONS
        // =====================================================

        if (containsAny(
                cmd,
                "الإشعارات",
                "الاشعارات",
                "notifications"
        )) {

            return
                    core.processCommand(
                            "الإشعارات"
                    );
        }

        // =====================================================
        // SCREEN
        // =====================================================

        if (containsAny(
                cmd,
                "الشاشة",
                "screen"
        )) {

            return
                    core.processCommand(
                            "الشاشة"
                    );
        }

        // =====================================================
        // AUTOMATION
        // =====================================================

        if (containsAny(
                cmd,
                "الأتمتة",
                "الاتماتة",
                "automation"
        )) {

            return
                    core.processCommand(
                            "الأتمتة"
                    );
        }

        // =====================================================
        // MONITOR
        // =====================================================

        if (containsAny(
                cmd,
                "المراقبة",
                "مراقبة النظام",
                "monitor"
        )) {

            return
                    core.processCommand(
                            "المراقبة"
                    );
        }

        // =====================================================
        // DEVELOPMENT TARGET
        // =====================================================

        if (containsAny(
                cmd,
                "شنو الهدف الحالي",
                "الهدف الحالي"
        )) {

            return
                    core.getDevelopmentTarget();
        }

        // =====================================================
        // LEARNING
        // =====================================================

        if (cmd.startsWith(
                "تعلم "
        )
                || cmd.startsWith(
                "علمني "
        )) {

            String information =
                    extractTarget(
                            original,
                            "تعلم",
                            "علمني"
                    );

            if (information.isEmpty()) {

                return
                        "شنو بغيتي نتعلم؟";
            }

            return
                    core.learn(
                            information,
                            information
                    );
        }

        // =====================================================
        // TASK CREATION
        // =====================================================

        if (cmd.startsWith(
                "مهمة "
        )
                || cmd.startsWith(
                "دير مهمة "
        )) {

            String task =
                    extractTarget(
                            original,
                            "دير مهمة",
                            "مهمة"
                    );

            if (task.isEmpty()) {

                return
                        "شنو هي المهمة؟";
            }

            return
                    core.addTask(task);
        }

        // =====================================================
        // PLANNING
        // =====================================================

        if (cmd.startsWith(
                "خطط ل"
        )
                || cmd.startsWith(
                "خطط "
        )) {

            String goal =
                    extractTarget(
                            original,
                            "خطط ل",
                            "خطط"
                    );

            if (goal.isEmpty()) {

                return
                        "شنو الهدف اللي بغيتي نخطط ليه؟";
            }

            return
                    core.createPlan(goal);
        }

        // =====================================================
        // COMMAND LEARNING
        // =====================================================

        if (cmd.startsWith(
                "علم جارفيس "
        )
                || cmd.equals(
                "علم جارفيس"
        )) {

            String learning =
                    extractTarget(
                            original,
                            "علم جارفيس"
                    );

            if (learning.isEmpty()) {

                return
                        "قول ليا شنو بغيتي نعلم جارفيس يدير.";
            }

            /*
             * الصيغ المدعومة:
             *
             * علم جارفيس افتح يوتيوب = افتح يوتيوب
             * علم جارفيس شغل الموسيقى -> افتح يوتيوب
             *
             * كنستعمل "=" أو "->" أو "ثم نفذ" للفصل
             * بين الأمر والإجراء.
             */

            String learnedCommand = "";
            String learnedAction = "";

            String[] separators = {
                    "=",
                    "->",
                    "=>",
                    " ثم ",
                    " ومن بعد ",
                    " وبعدها "
            };

            for (String separator : separators) {

                int index =
                        learning.indexOf(
                                separator
                        );

                if (index > 0) {

                    learnedCommand =
                            learning
                                    .substring(
                                            0,
                                            index
                                    )
                                    .trim();

                    learnedAction =
                            learning
                                    .substring(
                                            index
                                                    + separator.length()
                                    )
                                    .trim();

                    break;
                }
            }

            /*
             * إلا ما كانش separator،
             * كنقلب على الصيغة:
             *
             * "علم جارفيس أمر هو إجراء"
             */

            if (learnedCommand.isEmpty() &&
                    learnedAction.isEmpty()) {

                String normalizedLearning =
                        normalize(
                                learning
                        );

                String marker =
                        " هو ";

                int index =
                        normalizedLearning.indexOf(
                                marker
                        );

                if (index > 0) {

                    learnedCommand =
                            learning
                                    .substring(
                                            0,
                                            index
                                    )
                                    .trim();

                    learnedAction =
                            learning
                                    .substring(
                                            index
                                                    + marker.length()
                                    )
                                    .trim();
                }
            }

            if (learnedCommand.isEmpty() ||
                    learnedAction.isEmpty()) {

                return
                        "باش نتعلم مزيان خاصك تعطيني الأمر والإجراء.\n\n"
                                + "مثال:\n"
                                + "علم جارفيس شغل يوتيوب = افتح يوتيوب";
            }

            if (normalize(
                    learnedCommand
            ).equals(
                    normalize(
                            learnedAction
                    )
            )) {

                return
                        "الأمر والإجراء ما خاصهمش يكونو نفس الحاجة.";
            }

            return
                    core.rememberCommand(
                            learnedCommand,
                            learnedAction
                    );
        }

        // =====================================================
        // COMMAND LEARNING STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "تعلم الأوامر",
                "حالة التعلم",
                "حالة تعلم الأوامر",
                "command learning"
        )) {

            return
                    "Command Learning Engine: ONLINE ✓";
        }

        // =====================================================
        // ANDROID ENGINE STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "حالة التحكم",
                "حالة android control",
                "android control"
        )) {

            return
                    androidControl
                            .getStatus();
        }

        // =====================================================
        // CORE FALLBACK
        // =====================================================

        return
                core.processCommand(
                        original
                );
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "أ",
                        "ا"
                )
                .replace(
                        "إ",
                        "ا"
                )
                .replace(
                        "آ",
                        "ا"
                )
                .replace(
                        "ة",
                        "ه"
                );
    }

    // =========================================================
    // CONTAINS ANY
    // =========================================================

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
                    normalized.contains(
                            target
                    )) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // REMOVE PREFIX
    // =========================================================

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
                value.toLowerCase(
                        Locale.ROOT
                );

        String lowerPrefix =
                prefix.toLowerCase(
                        Locale.ROOT
                );

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

    // =========================================================
    // EXTRACT TARGET
    // =========================================================

    private String extractTarget(
            String original,
            String... prefixes
    ) {

        if (original == null) {

            return "";
        }

        String value =
                original.trim();

        for (String prefix : prefixes) {

            String result =
                    removePrefix(
                            value,
                            prefix
                    );

            if (!result.isEmpty()) {

                return result;
            }

            if (normalize(value)
                    .equals(
                            normalize(prefix)
                    )) {

                return "";
            }
        }

        return "";
    }

    // =========================================================
    // HELP
    // =========================================================

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
                "التحكم فالهاتف:\n"
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
                "- افتح التطبيقات الأخيرة\n"
        );

        help.append(
                "- رجع للور\n"
        );

        help.append(
                "- رجع للصفحة الرئيسية\n"
        );

        help.append(
                "- افتح الإشعارات\n"
        );

        help.append(
                "- قفل الشاشة\n\n"
        );

        help.append(
                "الشاشة:\n"
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
                "التطبيقات:\n"
        );

        help.append(
                "- افتح يوتيوب\n"
        );

        help.append(
                "- افتح واتساب\n"
        );

        help.append(
                "- افتح انستغرام\n"
        );

        help.append(
                "- افتح فيسبوك\n"
        );

        help.append(
                "- افتح كروم\n"
        );

        help.append(
                "- افتح تطبيق [الاسم]\n\n"
        );

        help.append(
                "الإنترنت:\n"
        );

        help.append(
                "- افتح جوجل\n"
        );

        help.append(
                "- ابحث على [شيء]\n"
        );

        help.append(
                "- ابحث في جوجل [شيء]\n"
        );

        help.append(
                "- افتح الرابط [الرابط]\n\n"
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
                "- علم جارفيس [الأمر] = [الإجراء]\n\n"
        );

        help.append(
                "مثال:\n"
        );

        help.append(
                "علم جارفيس شغل يوتيوب = افتح يوتيوب\n\n"
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
                "الاتصال:\n"
        );

        help.append(
                "- افتح الهاتف\n"
        );

        help.append(
                "- اتصل ب [الرقم]\n\n"
        );

        help.append(
                "عام:\n"
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
}