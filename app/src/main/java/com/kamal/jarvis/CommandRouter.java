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

    public CommandRouter(Context context) {
        this.context = context.getApplicationContext();

        this.core = new JarvisCore(this.context);
        this.screenIntelligence =
                new ScreenIntelligence(this.context);
        this.androidControl =
                new AndroidControlEngine(this.context);
        this.intelligenceEngine =
                new JarvisIntelligenceEngine(this.context);
        this.notificationIntelligence =
                new NotificationIntelligence(this.context);
        this.decisionEngine =
                new DecisionEngine(this.context);
    }

    public String execute(String command) {

        if (command == null || command.trim().isEmpty()) {
            return "ما سمعت حتى أمر.";
        }

        String original =
                intelligenceEngine.prepareCommand(
                        command.trim()
                );

        if (original.isEmpty()) {
            return "ما سمعت حتى أمر.";
        }

        /*
         * DecisionEngine كيحلل الأمر ويسجل القرار قبل التنفيذ.
         * إذا وقع مشكل فالتحليل، ما خاصوش يوقف تنفيذ الأمر.
         */
        try {
            decisionEngine.decide(original);
        } catch (Exception ignored) {
            // القرار مجرد طبقة ذكاء إضافية ولا يجب أن يمنع التنفيذ.
        }

        String intelligenceResponse =
                intelligenceEngine.intercept(original);

        if (intelligenceResponse != null
                && !intelligenceResponse.trim().isEmpty()) {

            intelligenceEngine.recordTurn(
                    original,
                    intelligenceResponse
            );

            return intelligenceResponse;
        }

        String cmd = normalize(original);

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

        if (containsAny(
                cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "hello",
                "hi"
        )) {
            return "مرحبا كمال. JARVIS CORE حاضر ومستعد.";
        }

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
            return getHelp();
        }

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
                    ).format(new Date());

            return "الوقت دابا هو " + time;
        }

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
                    ).format(new Date());

            return "التاريخ اليوم هو " + date;
        }

        // ==============================
        // APPS
        // ==============================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "حل يوتيوب",
                "youtube"
        )) {
            return androidControl.openYouTube();
        }

        if (containsAny(
                cmd,
                "فتح واتساب",
                "افتح واتساب",
                "حل واتساب",
                "whatsapp"
        )) {
            return androidControl.openWhatsApp();
        }

        if (containsAny(
                cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "حل انستغرام",
                "instagram"
        )) {
            return androidControl.openInstagram();
        }

        if (containsAny(
                cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "حل فيسبوك",
                "facebook"
        )) {
            return androidControl.openFacebook();
        }

        if (containsAny(
                cmd,
                "فتح كروم",
                "افتح كروم",
                "حل كروم",
                "chrome"
        )) {
            return androidControl.openChrome();
        }

        if (containsAny(
                cmd,
                "فتح جوجل",
                "افتح جوجل",
                "حل جوجل",
                "google"
        )) {
            return androidControl.openWebPage(
                    "https://www.google.com"
            );
        }

        // ==============================
        // WEB SEARCH
        // ==============================

        if (cmd.startsWith("ابحث في جوجل ")) {

            String query =
                    removePrefix(
                            original,
                            "ابحث في جوجل"
                    );

            if (query.isEmpty()) {
                return "شنو بغيتي نقلب عليه؟";
            }

            return androidControl.searchWeb(query);
        }

        if (cmd.startsWith("قلب في جوجل ")) {

            String query =
                    removePrefix(
                            original,
                            "قلب في جوجل"
                    );

            if (query.isEmpty()) {
                return "شنو بغيتي نقلب عليه؟";
            }

            return androidControl.searchWeb(query);
        }

        if (cmd.startsWith("ابحث عن ")) {

            String query =
                    removePrefix(
                            original,
                            "ابحث عن"
                    );

            if (query.isEmpty()) {
                return "شنو بغيتي نقلب عليه؟";
            }

            return androidControl.searchWeb(query);
        }

        if (cmd.startsWith("ابحث على ")) {

            String query =
                    removePrefix(
                            original,
                            "ابحث على"
                    );

            if (query.isEmpty()) {
                return "شنو بغيتي نقلب عليه؟";
            }

            return androidControl.searchWeb(query);
        }

        if (cmd.startsWith("قلب ")) {

            String query =
                    removePrefix(
                            original,
                            "قلب"
                    );

            if (!query.isEmpty()) {
                return androidControl.searchWeb(query);
            }
        }

        // ==============================
        // URL
        // ==============================

        if (cmd.startsWith("افتح الرابط ")) {

            String url =
                    removePrefix(
                            original,
                            "افتح الرابط"
                    );

            return safeOpenUrl(url);
        }

        if (cmd.startsWith("http://")
                || cmd.startsWith("https://")) {

            return safeOpenUrl(original);
        }

        // ==============================
        // PHONE
        // ==============================

        if (containsAny(
                cmd,
                "افتح الهاتف",
                "افتح الاتصال",
                "فتح الهاتف",
                "dialer"
        )) {
            return androidControl.openPhoneDialer();
        }

        if (cmd.startsWith("اتصل ب ")) {

            String number =
                    removePrefix(
                            original,
                            "اتصل ب"
                    );

            if (number.isEmpty()) {
                return "عطيني رقم الهاتف.";
            }

            return androidControl.dialNumber(number);
        }

        if (cmd.startsWith("اتصل ب")) {

            String number =
                    removePrefix(
                            original,
                            "اتصل ب"
                    );

            if (number.isEmpty()) {
                return "عطيني رقم الهاتف.";
            }

            return androidControl.dialNumber(number);
        }

        // ==============================
        // SETTINGS
        // ==============================

        if (containsAny(
                cmd,
                "افتح الاعدادات",
                "افتح الإعدادات",
                "فتح الاعدادات",
                "الاعدادات",
                "الإعدادات",
                "settings"
        )) {
            return androidControl.openSettings();
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi",
                "wi fi"
        )) {
            return androidControl.openWifiSettings();
        }

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {
            return androidControl.openBluetoothSettings();
        }

        if (containsAny(
                cmd,
                "اعدادات التطبيقات",
                "إعدادات التطبيقات",
                "application settings"
        )) {
            return androidControl.openApplicationSettings();
        }

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "معلومات الجهاز",
                "حول الهاتف",
                "عن الهاتف"
        )) {
            return androidControl.openDeviceInformation();
        }

        if (containsAny(
                cmd,
                "اعدادات الاشعارات",
                "إعدادات الإشعارات",
                "notification settings"
        )) {
            return androidControl.openNotificationSettings();
        }

        // ==============================
        // SCREEN
        // ==============================

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

            if (result == null
                    || result.trim().isEmpty()) {

                return "ما قدرتش نقرا الشاشة دابا. تأكد أن خدمة إمكانية الوصول مفعلة.";
            }

            return result;
        }

        // ==============================
        // ACCESSIBILITY
        // ==============================

        if (containsAny(
                cmd,
                "فعل الوصول",
                "فعل إمكانية الوصول",
                "اعدادات الوصول",
                "إعدادات الوصول",
                "accessibility"
        )) {

            return openSystemPage(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    "فتحت ليك إعدادات إمكانية الوصول. قلب على JARVIS وفعل الخدمة."
            );
        }

        if (containsAny(
                cmd,
                "واش الوصول خدام",
                "هل الوصول خدام",
                "حالة الوصول",
                "accessibility status"
        )) {

            if (JarvisAccessibilityService.getInstance()
                    != null) {

                return "خدمة التحكم فالشاشة خدامة.";
            }

            return "خدمة التحكم فالشاشة مازال ما تفعلاتش.";
        }

        // ==============================
        // NOTIFICATIONS
        // ==============================

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

            return notificationIntelligence
                    .getLastNotification();
        }

        if (containsAny(
                cmd,
                "فعل الإشعارات",
                "فعل الاشعارات",
                "خدمة الإشعارات",
                "خدمة الاشعارات",
                "notification access"
        )) {

            return openSystemPage(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                    "فتحت ليك إعدادات الوصول للإشعارات. فعل JARVIS باش يقدر يفهم الإشعارات."
            );
        }

        if (containsAny(
                cmd,
                "واش الإشعارات خدامة",
                "هل الاشعارات خدامة",
                "حالة الإشعارات",
                "notification status"
        )) {

            if (NotificationIntelligence.isServiceConnected()) {
                return "خدمة الإشعارات خدامة.";
            }

            return "خدمة الإشعارات مازال ما تفعلاتش.";
        }

        // ==============================
        // MEMORY / TASKS / CORE
        // ==============================

        if (containsAny(
                cmd,
                "شنو حافظ",
                "اش حافظ",
                "شنو فذاكرتك",
                "الذاكرة",
                "الذاكره",
                "memory"
        )) {
            return processWithCore("شنو حافظ");
        }

        if (cmd.startsWith("حفظ ")
                || cmd.startsWith("سجل ")
                || cmd.startsWith("نسى ")
                || cmd.startsWith("انسى ")) {

            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "المهام",
                "المهام ديالي",
                "شنو عندي من مهام",
                "شنو عندي اليوم",
                "task",
                "tasks"
        )) {
            return processWithCore(original);
        }

        if (cmd.startsWith("مهمة ")
                || cmd.startsWith("دير ليا مهمة ")
                || cmd.startsWith("زيد مهمة ")
                || cmd.startsWith("حيد المهمة ")) {

            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "التذكيرات",
                "التذكير",
                "شنو عندي من تذكيرات",
                "ذكرني",
                "reminder",
                "reminders"
        )) {
            return processWithCore(original);
        }

        if (cmd.startsWith("ذكرني ")
                || cmd.startsWith("فكرني ")
                || cmd.startsWith("دير ليا تذكير ")) {

            return processWithCore(original);
        }

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
            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "تعلم",
                "علمني",
                "بغيت نتعلم",
                "تعلم هادشي",
                "learning",
                "learn"
        )) {
            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "أتمتة",
                "اتمته",
                "اوتوماتيك",
                "أوتوماتيك",
                "automation"
        )) {
            return processWithCore(original);
        }

        // ==============================
        // SYSTEM INTELLIGENCE
        // ==============================

        if (containsAny(
                cmd,
                "حالة جارفيس",
                "حالة النظام",
                "واش جارفيس خدام",
                "واش النظام خدام",
                "status",
                "system status"
        )) {
            return processWithCore("حالة النظام");
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
            return processWithCore(original);
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
            return processWithCore(original);
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
            return processWithCore(original);
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
            return processWithCore(original);
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
            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "المهارات ديالك",
                "شنو المهارات ديالك",
                "اش كتقدر تدير",
                "skills",
                "skill"
        )) {
            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "المعرفة",
                "المعلومات",
                "شنو عارف",
                "شنو كتعرف",
                "knowledge"
        )) {
            return processWithCore(original);
        }

        if (containsAny(
                cmd,
                "السياق",
                "context",
                "شنو وقع قبل"
        )) {
            return processWithCore(original);
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
            return processWithCore(original);
        }

        // ==============================
        // OPEN APP BY NAME
        // ==============================

        if (cmd.startsWith("افتح تطبيق ")
                || cmd.startsWith("فتح تطبيق ")) {

            String appName =
                    cmd.startsWith("افتح تطبيق ")
                            ? removePrefix(
                                    original,
                                    "افتح تطبيق"
                            )
                            : removePrefix(
                                    original,
                                    "فتح تطبيق"
                            );

            if (appName.isEmpty()) {
                return "قول ليا شنو هو التطبيق.";
            }

            return androidControl
                    .openApplicationByName(appName);
        }

        // ==============================
        // MAPS
        // ==============================

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
                                Uri.parse("geo:0,0")
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return "فتحت ليك الخرائط.";

            } catch (Exception e) {

                return "ما قدرتش نفتح الخرائط.";
            }
        }

        // ==============================
        // CAMERA
        // ==============================

        if (containsAny(
                cmd,
                "افتح الكاميرا",
                "فتح الكاميرا",
                "حل الكاميرا",
                "camera"
        )) {

            return launchIntent(
                    new Intent(
                            "android.media.action.IMAGE_CAPTURE"
                    ),
                    "الكاميرا"
            );
        }

        // ==============================
        // CLOCK
        // ==============================

        if (containsAny(
                cmd,
                "افتح الساعة",
                "فتح الساعة",
                "clock"
        )) {

            return launchIntent(
                    new Intent(
                            "android.intent.action.SHOW_ALARMS"
                    ),
                    "الساعة"
            );
        }

        // ==============================
        // CALCULATOR
        // ==============================

        if (containsAny(
                cmd,
                "افتح الحاسبة",
                "افتح الالة الحاسبة",
                "فتح الحاسبة",
                "calculator"
        )) {

            return androidControl
                    .openApplicationByName(
                            "Calculator"
                    );
        }

        // ==============================
        // CONTACTS
        // ==============================

        if (containsAny(
                cmd,
                "افتح جهات الاتصال",
                "فتح جهات الاتصال",
                "contacts"
        )) {

            return launchIntent(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "content://contacts/people/"
                            )
                    ),
                    "جهات الاتصال"
            );
        }

        // ==============================
        // MESSAGES
        // ==============================

        if (containsAny(
                cmd,
                "افتح الرسائل",
                "فتح الرسائل",
                "messages"
        )) {

            return launchIntent(
                    new Intent(
                            Intent.ACTION_MAIN
                    ).addCategory(
                            Intent.CATEGORY_APP_MESSAGING
                    ),
                    "الرسائل"
            );
        }

        // ==============================
        // GALLERY
        // ==============================

        if (containsAny(
                cmd,
                "افتح الصور",
                "فتح الصور",
                "المعرض",
                "gallery",
                "photos"
        )) {

            return launchIntent(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "content://media/internal/images/media"
                            )
                    ),
                    "المعرض"
            );
        }

        // ==============================
        // FILES
        // ==============================

        if (containsAny(
                cmd,
                "افتح الملفات",
                "فتح الملفات",
                "file manager",
                "files"
        )) {

            return launchIntent(
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    )
                            .addCategory(
                                    Intent.CATEGORY_OPENABLE
                            )
                            .setType("*/*"),
                    "الملفات"
            );
        }

        // ==============================
        // MUSIC
        // ==============================

        if (containsAny(
                cmd,
                "افتح الموسيقى",
                "فتح الموسيقى",
                "music"
        )) {

            return launchIntent(
                    new Intent(
                            Intent.ACTION_MAIN
                    ).addCategory(
                            Intent.CATEGORY_APP_MUSIC
                    ),
                    "الموسيقى"
            );
        }

        // ==============================
        // ANDROID CONTROL
        // ==============================

        if (containsAny(
                cmd,
                "رجع للخلف",
                "رجع",
                "back"
        )) {
            return androidControl.goBack();
        }

        if (containsAny(
                cmd,
                "دير الرئيسية",
                "رجع للرئيسية",
                "الصفحة الرئيسية",
                "home"
        )) {
            return androidControl.goHome();
        }

        if (containsAny(
                cmd,
                "شوف التطبيقات المفتوحة",
                "التطبيقات المفتوحة",
                "recent apps",
                "التطبيقات الأخيرة"
        )) {
            return androidControl.openRecents();
        }

        // ==============================
        // JARVIS CORE
        // ==============================

        if (cmd.startsWith("جارفيس ")
                || cmd.startsWith("jarvis ")
                || containsAny(
                        cmd,
                        "نفذ الأمر",
                        "نفذ الامر",
                        "نفذ هاد الأمر",
                        "نفذ هاد الامر",
                        "execute command"
                )) {

            return processWithCore(original);
        }

        // ==============================
        // GENERIC OPEN
        // ==============================

        if (cmd.startsWith("افتح ")) {

            String target =
                    removePrefix(
                            original,
                            "افتح"
                    );

            if (!target.isEmpty()) {

                String result =
                        androidControl
                                .openApplicationByName(
                                        target
                                );

                if (result != null
                        && !result.trim().isEmpty()) {

                    return result;
                }
            }
        }

        // ==============================
        // FINAL CORE
        // ==============================

        return processWithCore(original);
    }

    private String normalize(String value) {

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
                .replace("؟", "")
                .replace("!", "")
                .replace("،", " ")
                .replaceAll("\\s+", " ");
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null) {
            return false;
        }

        for (String value : values) {

            if (value == null) {
                continue;
            }

            String target =
                    normalize(value);

            if (!target.isEmpty()
                    && text.contains(target)) {

                return true;
            }
        }

        return false;
    }

    private String removePrefix(
            String value,
            String prefix
    ) {

        if (value == null
                || prefix == null) {
            return "";
        }

        String result =
                value.trim();

        String normalizedResult =
                normalize(result);

        String normalizedPrefix =
                normalize(prefix);

        if (normalizedPrefix.isEmpty()) {
            return result;
        }

        if (!normalizedResult.startsWith(
                normalizedPrefix
        )) {
            return result;
        }

        int originalIndex = 0;
        int normalizedIndex = 0;

        while (originalIndex < result.length()
                && normalizedIndex < normalizedPrefix.length()) {

            String current =
                    normalize(
                            String.valueOf(
                                    result.charAt(
                                            originalIndex
                                    )
                            )
                    );

            if (current.isEmpty()) {
                originalIndex++;
                continue;
            }

            int currentLength =
                    current.length();

            if (normalizedIndex + currentLength
                    > normalizedPrefix.length()) {
                break;
            }

            String expected =
                    normalizedPrefix.substring(
                            normalizedIndex,
                            normalizedIndex + currentLength
                    );

            if (!current.equals(expected)) {
                return result;
            }

            normalizedIndex += currentLength;
            originalIndex++;
        }

        if (normalizedIndex
                == normalizedPrefix.length()) {

            return result
                    .substring(originalIndex)
                    .trim();
        }

        return result;
    }

    private String safeOpenUrl(String url) {

        if (url == null
                || url.trim().isEmpty()) {

            return "عطيني الرابط.";
        }

        try {

            String cleanUrl =
                    url.trim();

            if (!cleanUrl.startsWith("http://")
                    && !cleanUrl.startsWith("https://")) {

                cleanUrl =
                        "https://" + cleanUrl;
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

            return "فتحت الرابط.";

        } catch (Exception e) {

            return "الرابط ما قدرش يتحل.";
        }
    }

    private String openSystemPage(
            String action,
            String successMessage
    ) {

        try {

            Intent intent =
                    new Intent(action);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return successMessage;

        } catch (Exception e) {

            return "ما قدرتش نفتح الصفحة المطلوبة.";
        }
    }

    private String launchIntent(
            Intent intent,
            String name
    ) {

        if (intent == null) {
            return "الأمر غير صالح.";
        }

        try {

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت " + name + " ✓";

        } catch (Exception e) {

            return "ما قدرتش نفتح " + name + " ⚠";
        }
    }

    private String processWithCore(
            String command
    ) {

        if (command == null
                || command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
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

            if (response == null
                    || response.trim().isEmpty()) {

                return "الأمر وصل، ولكن ما لقيتش طريقة مناسبة لتنفيذه.";
            }

            return response;

        } catch (Exception e) {

            return "وقع خطأ أثناء تنفيذ الأمر، ولكن JARVIS باقي خدام.";
        }
    }

    private String getHelp() {

        return "نقدر نفتح التطبيقات، نقلب فالويب، "
                + "نتعامل مع الهاتف، نقرا الشاشة، "
                + "نفهم الإشعارات، نحفظ المعلومات، "
                + "ندير المهام والتذكيرات، "
                + "ونستعمل JARVIS CORE "
                + "للتعلم والتخطيط والتشخيص.";
    }
}