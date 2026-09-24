package com.kamal.jarvis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

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

        this.commandLearningEngine =
                new CommandLearningEngine(this.context);
    }

    // =========================================================
    // MAIN
    // =========================================================

    public synchronized String execute(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String original =
                intelligenceEngine.prepareCommand(
                        command.trim()
                );

        if (original == null ||
                original.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String intent = "unknown";

        try {
            intent =
                    intelligenceEngine.detectIntent(
                            original
                    );
        } catch (Exception ignored) {
        }

        try {

            String intelligent =
                    intelligenceEngine.intercept(
                            original
                    );

            if (intelligent != null &&
                    !intelligent.trim().isEmpty()) {

                return finish(
                        original,
                        intelligent,
                        intent,
                        true
                );
            }

        } catch (Exception ignored) {
        }

        try {
            decisionEngine.decide(original);
        } catch (Exception ignored) {
        }

        String cmd =
                normalize(original);

        // =====================================================
        // GREETING
        // =====================================================

        if (containsAny(
                cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "اهلا جارفيس",
                "hello",
                "hi"
        )) {

            return finish(
                    original,
                    "مرحبا كمال. JARVIS CORE حاضر ومستعد.",
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

            return finish(
                    original,
                    getHelp(),
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
                    ).format(new Date());

            return finish(
                    original,
                    "الوقت دابا هو " + time,
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
                    ).format(new Date());

            return finish(
                    original,
                    "التاريخ اليوم هو " + date,
                    intent,
                    true
            );
        }

        // =====================================================
        // STOP SPEAKING
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
        // APPS
        // =====================================================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "حل يوتيوب",
                "youtube"
        )) {

            return finish(
                    original,
                    androidControl.openYouTube(),
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

            return finish(
                    original,
                    androidControl.openWhatsApp(),
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

            return finish(
                    original,
                    androidControl.openInstagram(),
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

            return finish(
                    original,
                    androidControl.openFacebook(),
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

            return finish(
                    original,
                    androidControl.openChrome(),
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

            return finish(
                    original,
                    androidControl.openWebPage(
                            "https://www.google.com"
                    ),
                    intent,
                    true
            );
        }

        // =====================================================
        // SEARCH
        // =====================================================

        if (cmd.startsWith("ابحث في جوجل ")) {

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

            return finish(
                    original,
                    androidControl.searchWeb(query),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("قلب في جوجل ")) {

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

            return finish(
                    original,
                    androidControl.searchWeb(query),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("ابحث عن ")) {

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

            return finish(
                    original,
                    androidControl.searchWeb(query),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("ابحث على ")) {

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

            return finish(
                    original,
                    androidControl.searchWeb(query),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("قلب ")) {

            String query =
                    removePrefix(
                            original,
                            "قلب"
                    );

            if (!query.isEmpty()) {

                return finish(
                        original,
                        androidControl.searchWeb(query),
                        intent,
                        true
                );
            }
        }

        // =====================================================
        // URL
        // =====================================================

        if (cmd.startsWith("افتح الرابط ")) {

            String url =
                    removePrefix(
                            original,
                            "افتح الرابط"
                    );

            return finish(
                    original,
                    safeOpenUrl(url),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("https://") ||
                cmd.startsWith("http://")) {

            return finish(
                    original,
                    safeOpenUrl(original),
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

            return finish(
                    original,
                    androidControl.openPhoneDialer(),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("اتصل ب ")) {

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

            return finish(
                    original,
                    androidControl.dialNumber(number),
                    intent,
                    true
            );
        }

        if (cmd.startsWith("اتصل ب")) {

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

            return finish(
                    original,
                    androidControl.dialNumber(number),
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

            return finish(
                    original,
                    androidControl.openSettings(),
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

            return finish(
                    original,
                    androidControl.openWifiSettings(),
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

            return finish(
                    original,
                    androidControl.openBluetoothSettings(),
                    intent,
                    true
            );
        }

        // =====================================================
        // ANDROID NAVIGATION
        // =====================================================

        if (containsAny(
                cmd,
                "رجع للخلف",
                "رجع",
                "back"
        )) {

            return finish(
                    original,
                    androidControl.goBack(),
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

            return finish(
                    original,
                    androidControl.goHome(),
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

            return finish(
                    original,
                    androidControl.openRecents(),
                    intent,
                    true
            );
        }

        // =====================================================
        // OPEN APPLICATION
        // =====================================================

        if (cmd.startsWith("افتح تطبيق ") ||
                cmd.startsWith("فتح تطبيق ")) {

            String appName;

            if (cmd.startsWith("افتح تطبيق ")) {

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

            return finish(
                    original,
                    androidControl.openApplicationByName(
                            appName
                    ),
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

            return finish(
                    original,
                    launchIntent(
                            new Intent(
                                    "android.media.action.IMAGE_CAPTURE"
                            ),
                            "الكاميرا"
                    ),
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

            return finish(
                    original,
                    launchIntent(
                            new Intent(
                                    "android.intent.action.SHOW_ALARMS"
                            ),
                            "الساعة"
                    ),
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

            return finish(
                    original,
                    androidControl.openApplicationByName(
                            "Calculator"
                    ),
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

            return finish(
                    original,
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "content://contacts/people/"
                                    )
                            ),
                            "جهات الاتصال"
                    ),
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

            Intent messageIntent =
                    new Intent(Intent.ACTION_MAIN);

            messageIntent.addCategory(
                    Intent.CATEGORY_APP_MESSAGING
            );

            return finish(
                    original,
                    launchIntent(
                            messageIntent,
                            "الرسائل"
                    ),
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

            return finish(
                    original,
                    launchIntent(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                            "content://media/internal/images/media"
                                    )
                            ),
                            "المعرض"
                    ),
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

            Intent fileIntent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            fileIntent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            fileIntent.setType("*/*");

            return finish(
                    original,
                    launchIntent(
                            fileIntent,
                            "الملفات"
                    ),
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

            Intent musicIntent =
                    new Intent(
                            Intent.ACTION_MAIN
                    );

            musicIntent.addCategory(
                    Intent.CATEGORY_APP_MUSIC
            );

            return finish(
                    original,
                    launchIntent(
                            musicIntent,
                            "الموسيقى"
                    ),
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

            Intent mapIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:0,0")
                    );

            return finish(
                    original,
                    launchIntent(
                            mapIntent,
                            "الخرائط"
                    ),
                    intent,
                    true
            );
        }

        // =====================================================
        // SCREEN
        // =====================================================

        if (containsAny(
                cmd,
                "حلل الشاشة",
                "حلل الشاشة ديالي",
                "شوف الشاشة",
                "تحليل الشاشة",
                "screen analysis"
        )) {

            return finish(
                    original,
                    safeScreenAnalysis(),
                    intent,
                    true
            );
        }

        // =====================================================
        // NOTIFICATIONS
        // =====================================================

        if (containsAny(
                cmd,
                "الإشعارات",
                "الاشعارات",
                "شوف الإشعارات",
                "شوف الاشعارات",
                "notifications"
        )) {

            return finish(
                    original,
                    safeNotificationStatus(),
                    intent,
                    true
            );
        }

        // =====================================================
        // CORE / SELF DIAGNOSIS / EVOLUTION
        // =====================================================

        if (containsAny(
                cmd,
                "حالة جارفيس",
                "حالة النظام",
                "status",
                "diagnose",
                "self diagnosis",
                "شخص راسك",
                "شخص نفسك",
                "طور راسك",
                "طور نفسك",
                "طور جارفيس",
                "evolve",
                "evolution",
                "شنو القدرات ديالك",
                "القدرات",
                "capabilities",
                "المهارات ديالك",
                "skills",
                "المعرفة",
                "knowledge",
                "السياق",
                "context"
        )) {

            return finish(
                    original,
                    processWithCore(original),
                    intent,
                    true
            );
        }

        // =====================================================
        // EXPLICIT CORE
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
                    processWithCore(original),
                    intent,
                    true
            );
        }

        // =====================================================
        // GENERIC OPEN
        // =====================================================

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

                if (result != null &&
                        !result.trim().isEmpty()) {

                    return finish(
                            original,
                            result,
                            intent,
                            true
                    );
                }
            }
        }

        // =====================================================
        // FINAL CORE
        // =====================================================

        String result =
                processWithCore(original);

        return finish(
                original,
                result,
                intent,
                result != null &&
                        !result.trim().isEmpty()
        );
    }

    // =========================================================
    // CORE
    // =========================================================

    private String processWithCore(String command) {

        try {

            String result =
                    core.processCommand(command);

            if (result != null &&
                    !result.trim().isEmpty()) {

                return result;
            }

        } catch (Exception ignored) {
        }

        return "الأمر وصل، ولكن ما قدرتش نخرج نتيجة واضحة.";
    }

    // =========================================================
    // SCREEN
    // =========================================================

    private String safeScreenAnalysis() {

        try {

            String result =
                    screenIntelligence.analyzeCurrentScreen();

            if (result != null &&
                    !result.trim().isEmpty()) {

                return result;
            }

        } catch (Exception ignored) {
        }

        return "تحليل الشاشة ما متاحش حاليا.";
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private String safeNotificationStatus() {

        try {

            String result =
                    notificationIntelligence.getStatus();

            if (result != null &&
                    !result.trim().isEmpty()) {

                return result;
            }

        } catch (Exception ignored) {
        }

        return "نظام الإشعارات ما متاحش حاليا.";
    }

    // =========================================================
    // URL
    // =========================================================

    private String safeOpenUrl(String url) {

        if (url == null ||
                url.trim().isEmpty()) {

            return "عطيني الرابط.";
        }

        try {

            String clean =
                    url.trim();

            if (!clean.startsWith("http://") &&
                    !clean.startsWith("https://")) {

                clean =
                        "https://" + clean;
            }

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(clean)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت ليك الرابط.";

        } catch (Exception e) {

            return "ما قدرتش نفتح الرابط.";
        }
    }

    // =========================================================
    // LAUNCH
    // =========================================================

    private String launchIntent(
            Intent intent,
            String name
    ) {

        try {

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت ليك " + name + ".";

        } catch (Exception e) {

            return "ما قدرتش نفتح " + name + ".";
        }
    }

    // =========================================================
    // FINISH
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

        try {

            intelligenceEngine.recordTurn(
                    command,
                    safeResponse
            );

        } catch (Exception ignored) {
        }

        try {

            if (success) {

                commandLearningEngine.recordSuccess(
                        command
                );

            } else {

                commandLearningEngine.recordFailure(
                        command
                );
            }

        } catch (Exception ignored) {
        }

        if (safeResponse.isEmpty()) {

            return "الأمر وصل، ولكن ما لقيتش نتيجة واضحة.";
        }

        return safeResponse;
    }

    // =========================================================
    // HELP
    // =========================================================

    private String getHelp() {

        return
                "نقدر ندير حاليا:\n" +
                "• فتح التطبيقات\n" +
                "• يوتيوب وواتساب وإنستغرام وفيسبوك\n" +
                "• البحث في Google\n" +
                "• فتح الروابط\n" +
                "• الاتصال\n" +
                "• الإعدادات و Wi-Fi و Bluetooth\n" +
                "• Home / Back / Recent Apps\n" +
                "• الكاميرا والساعة والحاسبة\n" +
                "• الملفات والصور والرسائل والموسيقى\n" +
                "• الخرائط\n" +
                "• تحليل الشاشة\n" +
                "• الإشعارات\n" +
                "• الذاكرة والتعلم والمهارات\n" +
                "• التشخيص والتطور\n" +
                "• أوامر JARVIS CORE.";
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

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
                .replace("ى", "ي")
                .replace("ؤ", "و")
                .replace("ئ", "ي")
                .replace("ـ", "")
                .replaceAll("\\s+", " ");
    }

    private String removePrefix(
            String value,
            String prefix
    ) {

        if (value == null ||
                prefix == null) {

            return "";
        }

        String normalizedValue =
                normalize(value);

        String normalizedPrefix =
                normalize(prefix);

        if (normalizedValue.startsWith(
                normalizedPrefix
        )) {

            return value
                    .substring(
                            Math.min(
                                    value.length(),
                                    prefix.length()
                            )
                    )
                    .trim();
        }

        return "";
    }

    private boolean containsAny(
            String value,
            String... words
    ) {

        if (value == null ||
                words == null) {

            return false;
        }

        String normalized =
                normalize(value);

        for (String word : words) {

            if (word == null ||
                    word.trim().isEmpty()) {

                continue;
            }

            if (normalized.contains(
                    normalize(word)
            )) {

                return true;
            }
        }

        return false;
    }
}