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
    private final JarvisIntelligenceEngine intelligenceEngine;

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

        intelligenceEngine =
                new JarvisIntelligenceEngine(
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
                intelligenceEngine.prepareCommand(
                        command.trim()
                );

        String intelligenceResponse =
                intelligenceEngine.intercept(original);

        if (intelligenceResponse != null &&
                !intelligenceResponse.trim().isEmpty()) {

            intelligenceEngine.recordTurn(
                    original,
                    intelligenceResponse
            );

            return intelligenceResponse;
        }

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
        }        // =====================================================
        // SCREEN INTELLIGENCE
        // =====================================================

        if (containsAny(
                cmd,
                "شوف الشاشة",
                "اقرا الشاشة",
                "اقرأ الشاشة",
                "شنو كاين فالشاشة",
                "شنو باين فالشاشة",
                "حلل الشاشة",
                "حلل ليا الشاشة",
                "screen"
        )) {

            String result =
                    screenIntelligence
                            .analyzeCurrentScreen();

            if (result == null ||
                    result.trim().isEmpty()) {

                return
                        "ما قدرتش نقرا الشاشة دابا. "
                                + "تأكد أن خدمة إمكانية الوصول "
                                + "مفعلة.";
            }

            return result;
        }

        // =====================================================
        // ACCESSIBILITY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "فعل الوصول",
                "فعل إمكانية الوصول",
                "اعدادات الوصول",
                "إعدادات الوصول",
                "accessibility"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت ليك إعدادات إمكانية الوصول. "
                                + "قلب على JARVIS وفعل الخدمة.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الوصول.";
            }
        }

        // =====================================================
        // NOTIFICATION INTELLIGENCE
        // =====================================================

        if (containsAny(
                cmd,
                "آخر إشعار",
                "اخر اشعار",
                "آخر إشعارات",
                "اخر اشعارات",
                "شنو وصلني",
                "شنو وصلني دابا",
                "الإشعارات",
                "الاشعارات",
                "notifications"
        )) {

            String summary =
                    NotificationIntelligence
                            .getLastNotificationSummary();

            if (summary == null ||
                    summary.trim().isEmpty()) {

                return
                        "ما عنديش إشعار محفوظ دابا.";
            }

            return summary;
        }

        if (containsAny(
                cmd,
                "فعل الإشعارات",
                "فعل الاشعارات",
                "خدمة الإشعارات",
                "خدمة الاشعارات",
                "notification access"
        )) {

            try {

                Intent intent =
                        new Intent(
                                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت ليك إعدادات الوصول للإشعارات. "
                                + "فعل JARVIS باش يقدر يفهم الإشعارات.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الإشعارات.";
            }
        }

        // =====================================================
        // MEMORY
        // =====================================================

        if (containsAny(
                cmd,
                "شنو حافظ",
                "اش حافظ",
                "شنو فذاكرتك",
                "الذاكرة",
                "الذاكره",
                "memory"
        )) {

            return
                    core.processCommand(
                            "شنو حافظ"
                    );
        }

        if (cmd.startsWith(
                "حفظ "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "سجل "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "نسى "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "انسى "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // TASKS
        // =====================================================

        if (containsAny(
                cmd,
                "المهام",
                "المهام ديالي",
                "شنو عندي من مهام",
                "شنو عندي اليوم",
                "task",
                "tasks"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "مهمة "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "دير ليا مهمة "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "زيد مهمة "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "حيد المهمة "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // REMINDERS
        // =====================================================

        if (containsAny(
                cmd,
                "التذكيرات",
                "التذكير",
                "شنو عندي من تذكيرات",
                "ذكرني",
                "reminder",
                "reminders"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "ذكرني "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "فكرني "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "دير ليا تذكير "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // PLANNING
        // =====================================================

        if (containsAny(
                cmd,
                "خطط ليا",
                "صاوب ليا خطة",
                "دير ليا خطة",
                "الخطة ديالي",
                "التخطيط",
                "plan",
                "planning"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // LEARNING
        // =====================================================

        if (containsAny(
                cmd,
                "تعلم",
                "علمني",
                "بغيت نتعلم",
                "تعلم هادشي",
                "learning",
                "learn"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // COMMAND LEARNING
        // =====================================================

        if (containsAny(
                cmd,
                "تعلم الأمر",
                "تعلم الامر",
                "حفظ الأمر",
                "حفظ الامر",
                "تعلم هاد الأمر",
                "تعلم هاد الامر"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // AUTOMATION
        // =====================================================

        if (containsAny(
                cmd,
                "أتمتة",
                "اتمته",
                "اوتوماتيك",
                "أوتوماتيك",
                "automation"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // SYSTEM STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "حالة جارفيس",
                "حالة النظام",
                "واش جارفيس خدام",
                "واش النظام خدام",
                "status",
                "system status"
        )) {

            return
                    core.processCommand(
                            "حالة النظام"
                    );
        }

        // =====================================================
        // SELF TEST
        // =====================================================

        if (containsAny(
                cmd,
                "اختبر راسك",
                "اختبر نفسك",
                "دير اختبار",
                "فحص النظام",
                "فحص جارفيس",
                "self test",
                "test system"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // SELF DIAGNOSIS
        // =====================================================

        if (containsAny(
                cmd,
                "شخص راسك",
                "شخص نفسك",
                "شخص النظام",
                "شخص جارفيس",
                "self diagnosis",
                "diagnose"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // RECOVERY
        // =====================================================

        if (containsAny(
                cmd,
                "صلح راسك",
                "صلح نفسك",
                "صلح النظام",
                "اصلح النظام",
                "recovery",
                "repair"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // EVOLUTION
        // =====================================================

        if (containsAny(
                cmd,
                "طور راسك",
                "طور نفسك",
                "طور جارفيس",
                "طور النظام",
                "تطور",
                "evolve",
                "evolution"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // CAPABILITIES
        // =====================================================

        if (containsAny(
                cmd,
                "شنو القدرات ديالك",
                "اش هي القدرات ديالك",
                "شنو كتقدر",
                "شنو عندك من قدرات",
                "القدرات",
                "capabilities"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // SKILLS
        // =====================================================

        if (containsAny(
                cmd,
                "المهارات ديالك",
                "شنو المهارات ديالك",
                "اش كتقدر تدير",
                "skills",
                "skill"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // KNOWLEDGE
        // =====================================================

        if (containsAny(
                cmd,
                "المعرفة",
                "المعلومات",
                "شنو عارف",
                "شنو كتعرف",
                "knowledge"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // CONTEXT
        // =====================================================

        if (containsAny(
                cmd,
                "السياق",
                "context",
                "شنو وقع قبل"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // ACTION HISTORY
        // =====================================================

        if (containsAny(
                cmd,
                "شنو درتي",
                "شنو درت",
                "الأفعال الأخيرة",
                "الافعال الاخيرة",
                "سجل الأفعال",
                "action history"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // OPEN APP BY PACKAGE / URI
        // =====================================================

        if (cmd.startsWith(
                "افتح تطبيق "
        )) {

            String appName =
                    removePrefix(
                            original,
                            "افتح تطبيق"
                    );

            if (appName.isEmpty()) {

                return
                        "قول ليا شنو هو التطبيق.";
            }

            return
                    androidControl
                            .openApplication(appName);
        }

        if (cmd.startsWith(
                "فتح تطبيق "
        )) {

            String appName =
                    removePrefix(
                            original,
                            "فتح تطبيق"
                    );

            if (appName.isEmpty()) {

                return
                        "قول ليا شنو هو التطبيق.";
            }

            return
                    androidControl
                            .openApplication(appName);
        }

        // =====================================================
        // OPEN BROWSER URL DIRECTLY
        // =====================================================

        if (cmd.startsWith(
                "http://"
        ) ||
                cmd.startsWith(
                        "https://"
                )) {

            return
                    androidControl
                            .openWebPage(
                                    original
                            );
        }

        // =====================================================
        // GOOGLE SEARCH SHORT FORM
        // =====================================================

        if (cmd.startsWith(
                "قلب "
        )) {

            String query =
                    removePrefix(
                            original,
                            "قلب"
                    );

            if (!query.isEmpty()) {

                return
                        androidControl
                                .searchWeb(query);
            }
        }

        // =====================================================
        // OPEN MAPS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الخرائط",
                "فتح الخرائط",
                "google maps",
                "خرائط جوجل"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                        "geo:0,0"
                                )
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت ليك الخرائط.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح الخرائط.";
            }
        }

        // =====================================================
        // CAMERA
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الكاميرا",
                "فتح الكاميرا",
                "حل الكاميرا",
                "camera"
        )) {

            return
                    androidControl
                            .openCamera();
        }

        // =====================================================
        // CLOCK
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الساعة",
                "فتح الساعة",
                "clock"
        )) {

            return
                    androidControl
                            .openClock();
        }

        // =====================================================
        // CALCULATOR
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الحاسبة",
                "افتح الالة الحاسبة",
                "فتح الحاسبة",
                "calculator"
        )) {

            return
                    androidControl
                            .openCalculator();
        }

        // =====================================================
        // CONTACTS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح جهات الاتصال",
                "فتح جهات الاتصال",
                "contacts"
        )) {

            return
                    androidControl
                            .openContacts();
        }

        // =====================================================
        // MESSAGES
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الرسائل",
                "فتح الرسائل",
                "messages"
        )) {

            return
                    androidControl
                            .openMessages();
        }

        // =====================================================
        // GALLERY
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الصور",
                "فتح الصور",
                "المعرض",
                "gallery",
                "photos"
        )) {

            return
                    androidControl
                            .openGallery();
        }

        // =====================================================
        // FILES
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الملفات",
                "فتح الملفات",
                "file manager",
                "files"
        )) {

            return
                    androidControl
                            .openFileManager();
        }

        // =====================================================
        // MUSIC
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الموسيقى",
                "فتح الموسيقى",
                "music"
        )) {

            return
                    androidControl
                            .openMusic();
        }

        // =====================================================
        // CORE PROCESSING
        // =====================================================

        if (cmd.startsWith(
                "جارفيس "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        if (cmd.startsWith(
                "jarvis "
        )) {

            return
                    core.processCommand(
                            original
                    );
        }        // =====================================================
        // DIRECT CORE COMMANDS
        // =====================================================

        if (containsAny(
                cmd,
                "نفذ الأمر",
                "نفذ الامر",
                "نفذ هاد الأمر",
                "نفذ هاد الامر",
                "execute command"
        )) {

            return
                    core.processCommand(
                            original
                    );
        }

        // =====================================================
        // ANDROID CONTROL
        // =====================================================

        if (containsAny(
                cmd,
                "رجع للخلف",
                "رجع",
                "back"
        )) {

            return
                    androidControl
                            .goBack();
        }

        if (containsAny(
                cmd,
                "دير الرئيسية",
                "رجع للرئيسية",
                "الصفحة الرئيسية",
                "home"
        )) {

            return
                    androidControl
                            .goHome();
        }

        if (containsAny(
                cmd,
                "شوف التطبيقات المفتوحة",
                "التطبيقات المفتوحة",
                "recent apps",
                "التطبيقات الأخيرة"
        )) {

            return
                    androidControl
                            .openRecentApps();
        }

        // =====================================================
        // SCREEN / ACCESSIBILITY STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "واش الوصول خدام",
                "هل الوصول خدام",
                "حالة الوصول",
                "accessibility status"
        )) {

            if (JarvisAccessibilityService
                    .isServiceConnected()) {

                return
                        "خدمة التحكم فالشاشة خدامة.";
            }

            return
                    "خدمة التحكم فالشاشة مازال ما تفعلاتش.";
        }

        // =====================================================
        // NOTIFICATION STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "واش الإشعارات خدامة",
                "هل الاشعارات خدامة",
                "حالة الإشعارات",
                "notification status"
        )) {

            if (JarvisNotificationListenerService
                    .isServiceConnected()) {

                return
                        "خدمة الإشعارات خدامة.";
            }

            return
                    "خدمة الإشعارات مازال ما تفعلاتش.";
        }

        // =====================================================
        // OPEN ACCESSIBILITY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "فتح إعدادات إمكانية الوصول",
                "فتح اعدادات امكانية الوصول",
                "افتح إمكانية الوصول",
                "افتح امكانية الوصول"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات إمكانية الوصول.";

            } catch (Exception e) {

                return
                        "وقع مشكل وأنا كنفتح إعدادات الوصول.";
            }
        }

        // =====================================================
        // OPEN NOTIFICATION ACCESS SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "فتح إعدادات وصول الإشعارات",
                "فتح اعدادات وصول الاشعارات",
                "افتح وصول الإشعارات",
                "افتح وصول الاشعارات"
        )) {

            try {

                Intent intent =
                        new Intent(
                                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات وصول الإشعارات.";

            } catch (Exception e) {

                return
                        "وقع مشكل وأنا كنفتح إعدادات الإشعارات.";
            }
        }

        // =====================================================
        // OPEN APP SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح معلومات التطبيق",
                "معلومات تطبيق جارفيس",
                "إعدادات تطبيق جارفيس",
                "اعدادات تطبيق جارفيس"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        );

                intent.setData(
                        Uri.parse(
                                "package:"
                                        + context.getPackageName()
                        )
                );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات تطبيق JARVIS.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات التطبيق.";
            }
        }

        // =====================================================
        // INTERNET / CONNECTION SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح إعدادات الانترنت",
                "افتح اعدادات الانترنت",
                "إعدادات الشبكة",
                "اعدادات الشبكة",
                "network settings"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_WIRELESS_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات الشبكة.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الشبكة.";
            }
        }

        // =====================================================
        // BATTERY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح البطارية",
                "إعدادات البطارية",
                "اعدادات البطارية",
                "battery"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_BATTERY_SAVER_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات البطارية.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات البطارية.";
            }
        }

        // =====================================================
        // DISPLAY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الشاشة",
                "اعدادات الشاشة",
                "افتح الشاشة",
                "display settings"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_DISPLAY_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات الشاشة.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الشاشة.";
            }
        }

        // =====================================================
        // SOUND SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الصوت",
                "اعدادات الصوت",
                "افتح الصوت",
                "sound settings"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_SOUND_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات الصوت.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الصوت.";
            }
        }

        // =====================================================
        // LOCATION SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الموقع",
                "اعدادات الموقع",
                "افتح الموقع",
                "location settings"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات الموقع.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات الموقع.";
            }
        }

        // =====================================================
        // LANGUAGE SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات اللغة",
                "اعدادات اللغة",
                "اللغة",
                "language settings"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_LOCALE_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return
                        "فتحت إعدادات اللغة.";

            } catch (Exception e) {

                return
                        "ما قدرتش نفتح إعدادات اللغة.";
            }
        }

        // =====================================================
        // WIFI / BLUETOOTH / SYSTEM SHORTCUTS
        // =====================================================

        if (cmd.startsWith(
                "افتح "
        )) {

            String target =
                    removePrefix(
                            original,
                            "افتح"
                    );

            if (!target.isEmpty()) {

                String result =
                        android    // =========================================================
    // EXTRA COMMAND HELPERS
    // =========================================================

    private boolean isEmpty(
            String value
    ) {

        return value == null ||
                value.trim().isEmpty();
    }

    // =========================================================
    // OPEN SYSTEM PAGE
    // =========================================================

    private String openSystemPage(
            String action
    ) {

        if (isEmpty(action)) {

            return
                    "ما عرفتاش شنو الصفحة اللي بغيتي.";
        }

        try {

            Intent intent =
                    new Intent(action);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return
                    "فتحت ليك الصفحة المطلوبة.";

        } catch (Exception e) {

            return
                    "ما قدرتش نفتح الصفحة المطلوبة.";
        }
    }

    // =========================================================
    // SAFE WEB OPEN
    // =========================================================

    private String safeOpenUrl(
            String url
    ) {

        if (isEmpty(url)) {

            return
                    "عطيني الرابط.";
        }

        try {

            String cleanUrl =
                    url.trim();

            if (!cleanUrl.startsWith(
                    "http://"
            ) &&
                    !cleanUrl.startsWith(
                            "https://"
                    )) {

                cleanUrl =
                        "https://"
                                + cleanUrl;
            }

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(cleanUrl)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return
                    "فتحت الرابط.";

        } catch (Exception e) {

            return
                    "الرابط ما قدرش يتحل.";
        }
    }

    // =========================================================
    // FINAL FALLBACK
    // =========================================================

    private String processWithCore(
            String command
    ) {

        if (isEmpty(command)) {

            return
                    "ما عطيتيني حتى أمر.";
        }

        try {

            String response =
                    core.processCommand(
                            command.trim()
                    );

            intelligenceEngine.recordTurn(
                    command.trim(),
                    response
            );

            if (response == null ||
                    response.trim().isEmpty()) {

                return
                        "الأمر وصل، ولكن ما لقيتش "
                                + "طريقة مناسبة لتنفيذه.";
            }

            return response;

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء تنفيذ الأمر، "
                            + "ولكن JARVIS باقي خدام.";
        }
    }
}