package com.kamal.jarvis;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import java.util.List;
import java.util.Locale;

public class AndroidControlEngine {

    private final Context context;
    private final PackageManager packageManager;

    public AndroidControlEngine(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = this.context.getPackageManager();
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {
        if (!isHealthy()) {
            return "Android Control Engine: ERROR ⚠";
        }

        return "Android Control Engine: ONLINE ✓";
    }

    public boolean isHealthy() {
        return context != null && packageManager != null;
    }

    // =========================================================
    // SYSTEM SETTINGS
    // =========================================================

    public String openSettings() {
        return launch(
                new Intent(Settings.ACTION_SETTINGS),
                "الإعدادات"
        );
    }

    public String openWifiSettings() {
        return launch(
                new Intent(Settings.ACTION_WIFI_SETTINGS),
                "إعدادات Wi-Fi"
        );
    }

    public String openBluetoothSettings() {
        return launch(
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                "إعدادات Bluetooth"
        );
    }

    public String openApplicationSettings() {
        return launch(
                new Intent(Settings.ACTION_APPLICATION_SETTINGS),
                "إعدادات التطبيقات"
        );
    }

    public String openAppSettings() {
        return openApplicationSettings();
    }

    public String openDeviceInformation() {
        return launch(
                new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
                "معلومات الجهاز"
        );
    }

    public String openDeviceInfo() {
        return openDeviceInformation();
    }

    public String openNotificationSettings() {
        return launch(
                new Intent("android.settings.NOTIFICATION_SETTINGS"),
                "إعدادات الإشعارات"
        );
    }

    public String openDisplaySettings() {
        return launch(
                new Intent(Settings.ACTION_DISPLAY_SETTINGS),
                "إعدادات الشاشة"
        );
    }

    public String openSoundSettings() {
        return launch(
                new Intent(Settings.ACTION_SOUND_SETTINGS),
                "إعدادات الصوت"
        );
    }

    public String openBatterySettings() {
        return launch(
                new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                "إعدادات البطارية"
        );
    }

    public String openAccessibilitySettings() {
        return launch(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                "إعدادات إمكانية الوصول"
        );
    }

    public String openAccessibilitySettingsForJarvis() {
        return openAccessibilitySettings();
    }

    // =========================================================
    // ACCESSIBILITY
    // =========================================================

    public boolean isAccessibilityConnected() {
        return JarvisAccessibilityService.getInstance() != null;
    }

    public String getAccessibilityStatus() {
        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {
            return "JARVIS Accessibility Service: OFFLINE ⚠";
        }

        return service.getStatus();
    }

    private JarvisAccessibilityService getAccessibilityService() {
        return JarvisAccessibilityService.getInstance();
    }

    // =========================================================
    // GLOBAL PHONE CONTROLS
    // =========================================================

    public String goHome() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش نرجع للصفحة الرئيسية حتى تفعل Accessibility Service.";
        }

        return service.goHome()
                ? "رجعت للصفحة الرئيسية ✓"
                : "ما قدرتش نرجع للصفحة الرئيسية ⚠";
    }

    public String goBack() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش ندير رجوع حتى تفعل Accessibility Service.";
        }

        return service.goBack()
                ? "رجعت خطوة للور ✓"
                : "ما قدرتش ندير رجوع ⚠";
    }

    public String openRecents() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش نفتح التطبيقات الأخيرة حتى تفعل Accessibility Service.";
        }

        return service.openRecents()
                ? "فتحت التطبيقات الأخيرة ✓"
                : "ما قدرتش نفتح التطبيقات الأخيرة ⚠";
    }

    public String openNotifications() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش نفتح الإشعارات حتى تفعل Accessibility Service.";
        }

        return service.openNotifications()
                ? "فتحت الإشعارات ✓"
                : "ما قدرتش نفتح الإشعارات ⚠";
    }

    public String openQuickSettings() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش نفتح لوحة الاختصارات حتى تفعل Accessibility Service.";
        }

        return service.openQuickSettings()
                ? "فتحت لوحة الاختصارات ✓"
                : "ما قدرتش نفتح لوحة الاختصارات ⚠";
    }

    public String lockScreen() {
        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "ما نقدرش نقفل الشاشة حتى تفعل Accessibility Service.";
        }

        return service.lockScreen()
                ? "تم قفل الشاشة ✓"
                : "ما قدرتش نقفل الشاشة ⚠";
    }

    // =========================================================
    // APPLICATIONS
    // =========================================================

    public String openApplication(String packageName) {

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            return "خاصني اسم التطبيق أو package.";
        }

        String packageId = packageName.trim();

        try {
            Intent launchIntent =
                    packageManager.getLaunchIntentForPackage(packageId);

            if (launchIntent == null) {
                return "ما لقيتش تطبيق بالحزمة: " + packageId;
            }

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(launchIntent);

            return "فتحت التطبيق ✓";

        } catch (Exception e) {
            return "ما قدرتش نفتح التطبيق ⚠";
        }
    }

    public String openApplicationByName(String applicationName) {

        if (applicationName == null ||
                applicationName.trim().isEmpty()) {

            return "خاصني اسم التطبيق.";
        }

        String target =
                normalize(applicationName);

        try {

            List<ApplicationInfo> applications =
                    packageManager.getInstalledApplications(
                            PackageManager.GET_META_DATA
                    );

            for (ApplicationInfo app : applications) {

                if (app == null) {
                    continue;
                }

                CharSequence label =
                        packageManager.getApplicationLabel(app);

                if (label == null) {
                    continue;
                }

                String name =
                        normalize(label.toString());

                if (name.equals(target)
                        || name.contains(target)
                        || target.contains(name)) {

                    Intent launchIntent =
                            packageManager.getLaunchIntentForPackage(
                                    app.packageName
                            );

                    if (launchIntent == null) {
                        continue;
                    }

                    launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    context.startActivity(launchIntent);

                    return "فتحت " + label + " ✓";
                }
            }

        } catch (Exception e) {
            return "وقع خطأ وأنا كنقلب على التطبيق ⚠";
        }

        return "ما لقيتش التطبيق: " + applicationName;
    }

    public boolean isApplicationInstalled(String packageName) {

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            return false;
        }

        try {

            packageManager.getApplicationInfo(
                    packageName.trim(),
                    0
            );

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // COMMON APPLICATIONS
    // =========================================================

    public String openYouTube() {
        return openApplicationOrWeb(
                "com.google.android.youtube",
                "https://www.youtube.com",
                "YouTube"
        );
    }

    public String openWhatsApp() {
        return openApplicationOrWeb(
                "com.whatsapp",
                "https://wa.me/",
                "WhatsApp"
        );
    }

    public String openInstagram() {
        return openApplicationOrWeb(
                "com.instagram.android",
                "https://www.instagram.com",
                "Instagram"
        );
    }

    public String openFacebook() {
        return openApplicationOrWeb(
                "com.facebook.katana",
                "https://www.facebook.com",
                "Facebook"
        );
    }

    public String openChrome() {
        return openApplicationOrWeb(
                "com.android.chrome",
                "https://www.google.com",
                "Chrome"
        );
    }

    // =========================================================
    // MAPS
    // =========================================================

    public String openMaps() {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0")
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت الخرائط ✓";

        } catch (Exception e) {

            return openWebPage(
                    "https://maps.google.com"
            );
        }
    }

    public String searchMaps(String query) {

        if (query == null ||
                query.trim().isEmpty()) {

            return "شنو بغيتي نقلب عليه فالخريطة؟";
        }

        try {

            String encoded =
                    Uri.encode(query.trim());

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                            "geo:0,0?q=" + encoded
                    )
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "قلبت فـالخريطة على: "
                    + query.trim()
                    + " ✓";

        } catch (Exception e) {

            return searchWeb(
                    query + " Google Maps"
            );
        }
    }

    // =========================================================
    // CAMERA
    // =========================================================

    public String openCamera() {

        try {

            Intent intent =
                    new Intent(
                            "android.media.action.IMAGE_CAPTURE"
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            if (intent.resolveActivity(packageManager) == null) {
                return "ما لقيتش تطبيق الكاميرا.";
            }

            context.startActivity(intent);

            return "فتحت الكاميرا ✓";

        } catch (Exception e) {

            return "ما قدرتش نفتح الكاميرا ⚠";
        }
    }

    // =========================================================
    // CLOCK / ALARM
    // =========================================================

    public String openClock() {

        Intent intent =
                new Intent(
                        "android.intent.action.SHOW_ALARMS"
                );

        return launch(
                intent,
                "المنبه والساعة"
        );
    }

    // =========================================================
    // CALCULATOR
    // =========================================================

    public String openCalculator() {

        String[] packages = {
                "com.google.android.calculator",
                "com.sec.android.app.popupcalculator",
                "com.android.calculator2"
        };

        for (String packageName : packages) {

            if (isApplicationInstalled(packageName)) {

                String result =
                        openApplication(packageName);

                if (result.contains("✓")) {
                    return result;
                }
            }
        }

        return searchWeb("calculator");
    }

    // =========================================================
    // WEB
    // =========================================================

    public String openWebPage(String url) {

        if (url == null ||
                url.trim().isEmpty()) {

            return "خاصني رابط الصفحة.";
        }

        String finalUrl =
                url.trim();

        if (!finalUrl.startsWith("http://")
                && !finalUrl.startsWith("https://")) {

            finalUrl =
                    "https://" + finalUrl;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(finalUrl)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت الصفحة ✓";

        } catch (Exception e) {

            return "ما قدرتش نفتح الصفحة ⚠";
        }
    }

    public String searchWeb(String query) {

        if (query == null ||
                query.trim().isEmpty()) {

            return "شنو بغيتي نقلب عليه؟";
        }

        String encoded =
                Uri.encode(query.trim());

        return openWebPage(
                "https://www.google.com/search?q="
                        + encoded
        );
    }

    // =========================================================
    // PHONE
    // =========================================================

    public String openPhoneDialer() {

        return launch(
                new Intent(Intent.ACTION_DIAL),
                "الهاتف"
        );
    }

    public String dialNumber(String number) {

        if (number == null ||
                number.trim().isEmpty()) {

            return "خاصني رقم الهاتف.";
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(
                                    "tel:"
                                            + Uri.encode(
                                            number.trim()
                                    )
                            )
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

            return "فتحت الاتصال بالرقم ✓";

        } catch (Exception e) {

            return "ما قدرتش نفتح الاتصال ⚠";
        }
    }

    // =========================================================
    // SCREEN INFORMATION
    // =========================================================

    public String getCurrentScreenText() {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "Accessibility Service غير مفعلة.";
        }

        return service.getScreenText();
    }

    public String getCurrentScreenTree() {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "Accessibility Service غير مفعلة.";
        }

        return service.getScreenTree();
    }

    public String getCurrentPackage() {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "";
        }

        return service.getCurrentPackage();
    }

    public String getNodeInfoByText(String target) {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "Accessibility Service غير مفعلة.";
        }

        return service.getNodeInfoByText(target);
    }

    // =========================================================
    // SCREEN INTERACTION
    // =========================================================

    public String clickScreenElement(String target) {

        if (target == null ||
                target.trim().isEmpty()) {

            return "خاصني اسم العنصر.";
        }

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "خاصك تفعل Accessibility Service أولا.";
        }

        boolean result =
                service.clickByText(
                        target.trim()
                );

        return result
                ? "تم الضغط على "
                + target.trim()
                + " ✓"
                : "ما لقيتش العنصر: "
                + target.trim();
    }

    public String typeIntoScreen(
            String target,
            String text
    ) {

        if (text == null ||
                text.trim().isEmpty()) {

            return "خاصني النص اللي نكتب.";
        }

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "خاصك تفعل Accessibility Service أولا.";
        }

        boolean result =
                service.typeText(
                        target,
                        text
                );

        return result
                ? "تمت الكتابة ✓"
                : "ما قدرتش نكتب فالعنصر المطلوب ⚠";
    }

    public String scrollDown() {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "Accessibility Service غير مفعلة.";
        }

        return service.scrollForward()
                ? "تم النزول ✓"
                : "ما قدرتش ندير Scroll لتحت ⚠";
    }

    public String scrollUp() {

        JarvisAccessibilityService service =
                getAccessibilityService();

        if (service == null) {
            return "Accessibility Service غير مفعلة.";
        }

        return service.scrollBackward()
                ? "تم الطلوع ✓"
                : "ما قدرتش ندير Scroll لفوق ⚠";
    }

    // =========================================================
    // GENERIC APPLICATION / WEB LAUNCHER
    // =========================================================

    private String openApplicationOrWeb(
            String packageName,
            String fallbackUrl,
            String displayName
    ) {

        try {

            Intent launchIntent =
                    packageManager.getLaunchIntentForPackage(
                            packageName
                    );

            if (launchIntent != null) {

                launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(
                        launchIntent
                );

                return "فتحت "
                        + displayName
                        + " ✓";
            }

        } catch (Exception ignored) {
        }

        return openWebPage(fallbackUrl);
    }

    // =========================================================
    // GENERIC INTENT LAUNCHER
    // =========================================================

    private String launch(
            Intent intent,
            String name
    ) {

        if (intent == null) {
            return "الأمر غير صالح ⚠";
        }

        try {

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            if (intent.resolveActivity(
                    packageManager
            ) == null) {

                return "ما لقيتش الخدمة المطلوبة: "
                        + name;
            }

            context.startActivity(intent);

            return "تم فتح "
                    + name
                    + " ✓";

        } catch (Exception e) {

            return "ما قدرتش نفتح "
                    + name
                    + " ⚠";
        }
    }

    // =========================================================
    // TEXT NORMALIZATION
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
                .replace("ى", "ي");
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {
        return context;
    }
}