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

    private final NotificationIntelligence notificationIntelligence;

    private final DecisionEngine decisionEngine;

    private final CommandLearningEngine commandLearningEngine;

    public CommandRouter(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "CommandRouter context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        this.core =
                new JarvisCore(
                        this.context
                );

        this.screenIntelligence =
                new ScreenIntelligence(
                        this.context
                );

        this.androidControl =
                new AndroidControlEngine(
                        this.context
                );

        this.intelligenceEngine =
                new JarvisIntelligenceEngine(
                        this.context
                );

        this.notificationIntelligence =
                new NotificationIntelligence(
                        this.context
                );

        this.decisionEngine =
                new DecisionEngine(
                        this.context
                );

        this.commandLearningEngine =
                new CommandLearningEngine(
                        this.context
                );
    }

    // =========================================================
    // MAIN EXECUTION
    // =========================================================

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

        if (original.isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String decision =
                "";

        String intent =
                "unknown";

        String response =
                null;

        boolean executed =
                false;

        /*
         * 1. فهم الأمر
         */
        try {

            intent =
                    intelligenceEngine.detectIntent(
                            original
                    );

        } catch (Exception ignored) {
        }

        /*
         * 2. تحليل القرار
         */
        try {

            decision =
                    decisionEngine.decide(
                            original
                    );

        } catch (Exception ignored) {
        }

        /*
         * 3. أوامر الذكاء المباشرة
         */
        try {

            response =
                    intelligenceEngine.intercept(
                            original
                    );

            if (response != null &&
                    !response.trim().isEmpty()) {

                return finish(
                        original,
                        response,
                        intent,
                        true
                );
            }

        } catch (Exception ignored) {
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

            return finish(
                    original,
                    "__STOP_SPEAKING__",
                    intent,
                    true
            );
        }

        // =====================================================
        // GREETING
        // =====================================================

        if (containsAny(
                cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "hello",
                "hi"
        )) {

            response =
                    "مرحبا كمال. JARVIS CORE حاضر ومستعد.";

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // HELP
        // =====================================================

        if (containsAny(
                cmd,
                "مساعدة",
                "ساعدني",
                "شنو تقدر تدير",
                "شنو نقدر ندير",
                "الاوامر",
                "الأوامر",
                "help"
        )) {

            response =
                    getHelp();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // TIME
        // =====================================================

        if (containsAny(
                cmd,
                "شحال فالوقت",
                "كم الساعة",
                "الوقت",
                "الساعة"
        )) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            response =
                    "الوقت دابا هو "
                            + time;

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    "التاريخ اليوم هو "
                            + date;

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // APPS
        // =====================================================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "حل يوتيوب",
                "youtube"
        )) {

            response =
                    androidControl.openYouTube();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فتح واتساب",
                "افتح واتساب",
                "حل واتساب",
                "whatsapp"
        )) {

            response =
                    androidControl.openWhatsApp();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "حل انستغرام",
                "instagram"
        )) {

            response =
                    androidControl.openInstagram();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "حل فيسبوك",
                "facebook"
        )) {

            response =
                    androidControl.openFacebook();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فتح كروم",
                "افتح كروم",
                "حل كروم",
                "chrome"
        )) {

            response =
                    androidControl.openChrome();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فتح جوجل",
                "افتح جوجل",
                "حل جوجل",
                "google"
        )) {

            response =
                    androidControl.openWebPage(
                            "https://www.google.com"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
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

                return finish(
                        original,
                        "شنو بغيتي نقلب عليه؟",
                        intent,
                        false
                );
            }

            response =
                    androidControl.searchWeb(
                            query
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

                return finish(
                        original,
                        "شنو بغيتي نقلب عليه؟",
                        intent,
                        false
                );
            }

            response =
                    androidControl.searchWeb(
                            query
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

                return finish(
                        original,
                        "شنو بغيتي نقلب عليه؟",
                        intent,
                        false
                );
            }

            response =
                    androidControl.searchWeb(
                            query
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

                return finish(
                        original,
                        "شنو بغيتي نقلب عليه؟",
                        intent,
                        false
                );
            }

            response =
                    androidControl.searchWeb(
                            query
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (cmd.startsWith(
                "قلب "
        )) {

            String query =
                    removePrefix(
                            original,
                            "قلب"
                    );

            if (!query.isEmpty()) {

                response =
                        androidControl.searchWeb(
                                query
                        );

                return finish(
                        original,
                        response,
                        intent,
                        true
                );
            }
        }

        // =====================================================
        // URL
        // =====================================================

        if (cmd.startsWith(
                "افتح الرابط "
        )) {

            String url =
                    removePrefix(
                            original,
                            "افتح الرابط"
                    );

            response =
                    safeOpenUrl(
                            url
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (cmd.startsWith("http://")
                || cmd.startsWith("https://")) {

            response =
                    safeOpenUrl(
                            original
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    androidControl
                            .openPhoneDialer();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

                return finish(
                        original,
                        "عطيني رقم الهاتف.",
                        intent,
                        false
                );
            }

            response =
                    androidControl.dialNumber(
                            number
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

                return finish(
                        original,
                        "عطيني رقم الهاتف.",
                        intent,
                        false
                );
            }

            response =
                    androidControl.dialNumber(
                            number
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الاعدادات",
                "افتح الإعدادات",
                "فتح الاعدادات",
                "الاعدادات",
                "الإعدادات",
                "settings"
        )) {

            response =
                    androidControl.openSettings();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi",
                "wi fi"
        )) {

            response =
                    androidControl
                            .openWifiSettings();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {

            response =
                    androidControl
                            .openBluetoothSettings();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "اعدادات التطبيقات",
                "إعدادات التطبيقات",
                "application settings"
        )) {

            response =
                    androidControl
                            .openApplicationSettings();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "معلومات الجهاز",
                "حول الهاتف",
                "عن الهاتف"
        )) {

            response =
                    androidControl
                            .openDeviceInformation();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "اعدادات الاشعارات",
                "إعدادات الإشعارات",
                "notification settings"
        )) {

            response =
                    androidControl
                            .openNotificationSettings();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // SCREEN
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

            response =
                    screenIntelligence
                            .analyzeCurrentScreen();

            if (response == null ||
                    response.trim().isEmpty()) {

                response =
                        "ما قدرتش نقرا الشاشة دابا. "
                                + "تأكد أن خدمة إمكانية الوصول مفعلة.";

                return finish(
                        original,
                        response,
                        intent,
                        false
                );
            }

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // ACCESSIBILITY
        // =====================================================

        if (containsAny(
                cmd,
                "فعل الوصول",
                "فعل إمكانية الوصول",
                "اعدادات الوصول",
                "إعدادات الوصول",
                "accessibility"
        )) {

            response =
                    openSystemPage(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS,
                            "فتحت ليك إعدادات إمكانية الوصول. "
                                    + "قلب على JARVIS وفعل الخدمة."
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "واش الوصول خدام",
                "هل الوصول خدام",
                "حالة الوصول",
                "accessibility status"
        )) {

            if (JarvisAccessibilityService
                    .getInstance() != null) {

                response =
                        "خدمة التحكم فالشاشة خدامة.";

            } else {

                response =
                        "خدمة التحكم فالشاشة مازال ما تفعلاتش.";
            }

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // NOTIFICATIONS
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

            response =
                    notificationIntelligence
                            .getLastNotification();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "فعل الإشعارات",
                "فعل الاشعارات",
                "خدمة الإشعارات",
                "خدمة الاشعارات",
                "notification access"
        )) {

            response =
                    openSystemPage(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                            "فتحت ليك إعدادات الوصول للإشعارات. "
                                    + "فعل JARVIS باش يقدر يفهم الإشعارات."
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "واش الإشعارات خدامة",
                "هل الاشعارات خدامة",
                "حالة الإشعارات",
                "notification status"
        )) {

            if (NotificationIntelligence
                    .isServiceConnected()) {

                response =
                        "خدمة الإشعارات خدامة.";

            } else {

                response =
                        "خدمة الإشعارات مازال ما تفعلاتش.";
            }

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            return finish(
                    original,
                    processWithCore(
                            "شنو حافظ"
                    ),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("حفظ ")
                || cmd.startsWith("سجل ")
                || cmd.startsWith("نسى ")
                || cmd.startsWith("انسى ")) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("مهمة ")
                || cmd.startsWith("دير ليا مهمة ")
                || cmd.startsWith("زيد مهمة ")
                || cmd.startsWith("حيد المهمة ")) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("ذكرني ")
                || cmd.startsWith("فكرني ")
                || cmd.startsWith("دير ليا تذكير ")) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        // =====================================================
        // SYSTEM INTELLIGENCE
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

            return finish(
                    original,
                    processWithCore(
                            "حالة النظام"
                    ),
                    intent,
                    true
            );
        }

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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "شخص راسك",
                "شخص نفسك",
                "شخص النظام",
                "شخص جارفيس",
                "self diagnosis",
                "diagnose"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "صلح راسك",
                "صلح نفسك",
                "صلح النظام",
                "اصلح النظام",
                "recovery",
                "repair"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

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

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "شنو القدرات ديالك",
                "اش هي القدرات ديالك",
                "شنو كتقدر",
                "شنو عندك من قدرات",
                "القدرات",
                "capabilities"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "المهارات ديالك",
                "شنو المهارات ديالك",
                "اش كتقدر تدير",
                "skills",
                "skill"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "المعرفة",
                "المعلومات",
                "شنو عارف",
                "شنو كتعرف",
                "knowledge"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "السياق",
                "context",
                "شنو وقع قبل"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "شنو درتي",
                "شنو درت",
                "الأفعال الأخيرة",
                "الافعال الاخيرة",
                "سجل الأفعال",
                "action history"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        // =====================================================
        // OPEN APP BY NAME
        // =====================================================

        if (cmd.startsWith(
                "افتح تطبيق "
        )
                || cmd.startsWith(
                        "فتح تطبيق "
                )) {

            String appName;

            if (cmd.startsWith(
                    "افتح تطبيق "
            )) {

                appName =
                        removePrefix(
                                original,
                                "افتح تطبيق"
                        );

            } else {

                appName =
                        removePrefix(
                                original,
                                "فتح تطبيق"
                        );
            }

            if (appName.isEmpty()) {

                return finish(
                        original,
                        "قول ليا شنو هو التطبيق.",
                        intent,
                        false
                );
            }

            response =
                    androidControl
                            .openApplicationByName(
                                    appName
                            );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // MAPS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الخرائط",
                "فتح الخرائط",
                "google maps",
                "خرائط جوجل"
        )) {

            try {

                Intent intentObject =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                        "geo:0,0"
                                )
                        );

                intentObject.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(
                        intentObject
                );

                response =
                        "فتحت ليك الخرائط.";

            } catch (Exception e) {

                response =
                        "ما قدرتش نفتح الخرائط.";
            }

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    "android.media.action.IMAGE_CAPTURE"
                            ),
                            "الكاميرا"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    "android.intent.action.SHOW_ALARMS"
                            ),
                            "الساعة"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    androidControl
                            .openApplicationByName(
                                    "Calculator"
                            );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "content://contacts/people/"
                                    )
                            ),
                            "جهات الاتصال"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_MAIN
                            ).addCategory(
                                    Intent.CATEGORY_APP_MESSAGING
                            ),
                            "الرسائل"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "content://media/internal/images/media"
                                    )
                            ),
                            "المعرض"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_OPEN_DOCUMENT
                            )
                                    .addCategory(
                                            Intent.CATEGORY_OPENABLE
                                    )
                                    .setType("*/*"),
                            "الملفات"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
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

            response =
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_MAIN
                            ).addCategory(
                                    Intent.CATEGORY_APP_MUSIC
                            ),
                            "الموسيقى"
                    );

            return finish(
                    original,
                    response,
                    intent,
                    true
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

            response =
                    androidControl.goBack();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "دير الرئيسية",
                "رجع للرئيسية",
                "الصفحة الرئيسية",
                "home"
        )) {

            response =
                    androidControl.goHome();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        if (containsAny(
                cmd,
                "شوف التطبيقات المفتوحة",
                "التطبيقات المفتوحة",
                "recent apps",
                "التطبيقات الأخيرة"
        )) {

            response =
                    androidControl.openRecents();

            return finish(
                    original,
                    response,
                    intent,
                    true
            );
        }

        // =====================================================
        // EXPLICIT CORE COMMAND
        // =====================================================

        if (containsAny(
                cmd,
                "نفذ الأمر",
                "نفذ الامر",
                "نفذ هاد الأمر",
                "نفذ هاد الامر",
                "execute command"
        )) {

            return finish(
                    original,
                    processWithCore(
                            original
                    ),
                    intent,
                    true
            );
        }

        // =====================================================
        // GENERIC OPEN
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

                response =
                        androidControl
                                .openApplicationByName(
                                        target
                                );

                if (response != null &&
                        !response.trim().isEmpty()) {

                    return finish(
                            original,
                            response,
                            intent,
                            true
                    );
                }
            }
        }

        // =====================================================
        // FINAL CORE
        // =====================================================

        response =
                processWithCore(
                        original
                );

        return finish(
                original,
                response,
                intent,
                response != null
                        && !response.trim().isEmpty()
        );
    }

    // =========================================================
    // FINISH EXECUTION
    // =========================================================

    private String finish(
            String command,
            String response,
            String intent,
            boolean success
    ) {

        String safeResponse =
                response == null
                        ? ""
                        : response.trim();

        /*
         * تسجيل الدور في الذاكرة والسياق.
         */
        try {

            intelligenceEngine.recordTurn(
                    command,
                    safeResponse
            );

        } catch (Exception ignored) {
        }

        /*
         * تعليم النظام من نتيجة التنفيذ.
         */
        try {

            if (success) {

                commandLearningEngine
                        .recordSuccess(
                                command
                        );

            } else {

                commandLearningEngine
                        .recordFailure(
                                command
                        );
            }

        } catch (Exception ignored) {
        }

        if (safeResponse.isEmpty()) {

            return
                    "الأمر وصل، ولكن ما لقيتش نتيجة واضحة.";
        }

        return safeResponse;
    }

    // =========================================================