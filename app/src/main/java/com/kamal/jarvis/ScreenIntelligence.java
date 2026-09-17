package com.kamal.jarvis;

import android.content.Context;

public class ScreenIntelligence {

    private final Context context;

    private boolean enabled = false;

    private String lastAnalysis = "";

    public ScreenIntelligence(Context context) {

        this.context =
                context.getApplicationContext();
    }

    public String analyzeCurrentScreen() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            enabled = false;

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.\n\n"
                    + "خاصك تفعّل JARVIS Accessibility من إعدادات الهاتف.";
        }

        enabled = true;

        String screenText =
                service.getScreenText();

        String packageName =
                service.getCurrentPackage();

        String tree =
                service.getScreenTree();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SCREEN INTELLIGENCE\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "SCREEN ACCESS: ONLINE ✓\n"
        );

        result.append(
                "PACKAGE: "
        );

        result.append(
                packageName == null
                        ? "UNKNOWN"
                        : packageName
        );

        result.append("\n\n");

        result.append(
                "SCREEN TEXT:\n"
        );

        result.append(
                screenText
        );

        result.append("\n\n");

        result.append(
                "SCREEN TREE:\n"
        );

        result.append(
                tree
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    public String analyzeScreen(
            String screenDescription
    ) {

        if (screenDescription == null ||
                screenDescription.trim().isEmpty()) {

            return analyzeCurrentScreen();
        }

        String description =
                screenDescription.trim();

        lastAnalysis =
                description;

        enabled = true;

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SCREEN INTELLIGENCE\n"
        );

        result.append(
                "============================\n\n"
        );

        result.append(
                "SCREEN DATA RECEIVED ✓\n\n"
        );

        result.append(
                "المحتوى:\n"
        );

        result.append(
                description
        );

        result.append("\n\n");

        result.append(
                detectElements(description)
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    private String detectElements(
            String description
    ) {

        String lower =
                description.toLowerCase();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "العناصر المحتملة:\n"
        );

        boolean detected =
                false;

        if (lower.contains("button")
                || lower.contains("زر")
                || lower.contains("click")
                || lower.contains("اضغط")) {

            result.append(
                    "• زر / عنصر قابل للضغط\n"
            );

            detected = true;
        }

        if (lower.contains("text")
                || lower.contains("نص")
                || lower.contains("رسالة")) {

            result.append(
                    "• نص / معلومات\n"
            );

            detected = true;
        }

        if (lower.contains("image")
                || lower.contains("صورة")) {

            result.append(
                    "• صورة\n"
            );

            detected = true;
        }

        if (lower.contains("input")
                || lower.contains("خانة")
                || lower.contains("حقل")) {

            result.append(
                    "• حقل إدخال\n"
            );

            detected = true;
        }

        if (lower.contains("menu")
                || lower.contains("قائمة")) {

            result.append(
                    "• قائمة / Menu\n"
            );

            detected = true;
        }

        if (lower.contains("scroll")
                || lower.contains("تمرير")
                || lower.contains("سكرول")) {

            result.append(
                    "• منطقة قابلة للتمرير\n"
            );

            detected = true;
        }

        if (!detected) {

            result.append(
                    "• لم يتم التعرف على عناصر محددة بعد.\n"
            );
        }

        return result.toString();
    }

    public String clickElement(
            String target
    ) {

        if (target == null ||
                target.trim().isEmpty()) {

            return
                    "JARVIS: عطيني اسم العنصر اللي بغيتي نضغط عليه.";
        }

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.";
        }

        boolean clicked =
                service.clickByText(
                        target.trim()
                );

        if (clicked) {

            return
                    "JARVIS: تم الضغط على \""
                    + target
                    + "\" ✓";
        }

        return
                "JARVIS: ما لقيتش العنصر \""
                + target
                + "\" أو ما قدرتش نضغط عليه.";
    }

    public String typeText(
            String target,
            String text
    ) {

        if (text == null) {

            return
                    "JARVIS: النص فارغ.";
        }

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.";
        }

        boolean typed =
                service.typeText(
                        target == null
                                ? ""
                                : target.trim(),
                        text
                );

        if (typed) {

            return
                    "JARVIS: تم إدخال النص ✓";
        }

        return
                "JARVIS: ما قدرتش ندخل النص.";
    }

    public String scrollForward() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.";
        }

        boolean result =
                service.scrollForward();

        if (result) {

            return
                    "JARVIS: تم التمرير للأسفل ✓";
        }

        return
                "JARVIS: ما قدرتش نمرر الشاشة.";
    }

    public String scrollBackward() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.";
        }

        boolean result =
                service.scrollBackward();

        if (result) {

            return
                    "JARVIS: تم التمرير للأعلى ✓";
        }

        return
                "JARVIS: ما قدرتش نمرر الشاشة.";
    }

    public String findElement(
            String target
    ) {

        if (target == null ||
                target.trim().isEmpty()) {

            return
                    "JARVIS: خاصني اسم العنصر.";
        }

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: خدمة التحكم في الشاشة غير مفعلة.";
        }

        return service.getNodeInfoByText(
                target.trim()
        );
    }

    public String getLastAnalysis() {

        if (lastAnalysis == null ||
                lastAnalysis.trim().isEmpty()) {

            return
                    "مازال ما تحللات حتى شاشة.";
        }

        return
                "آخر تحليل للشاشة:\n\n"
                + lastAnalysis;
    }

    public String enable() {

        enabled = true;

        return
                "Screen Intelligence: ENABLED ✓";
    }

    public String disable() {

        enabled = false;

        return
                "Screen Intelligence: DISABLED";
    }

    public boolean isEnabled() {

        return enabled;
    }

    public boolean isConnected() {

        return
                JarvisAccessibilityService
                        .getInstance() != null;
    }

    public boolean isHealthy() {

        return context != null;
    }

    public String getStatus() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "Screen Intelligence: STANDBY\n"
                    + "Accessibility Service: OFF";
        }

        return
                "Screen Intelligence: ONLINE ✓\n"
                + "Accessibility Service: CONNECTED ✓\n"
                + "Package: "
                + service.getCurrentPackage();
    }
}