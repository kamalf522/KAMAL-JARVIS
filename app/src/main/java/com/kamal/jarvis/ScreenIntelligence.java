package com.kamal.jarvis;

import android.content.Context;

import java.util.Locale;

public class ScreenIntelligence {

    private final Context context;

    private boolean enabled = false;

    private String lastAnalysis = "";

    private String lastPackage = "";

    private long lastAnalysisTime = 0L;

    public ScreenIntelligence(Context context) {

        this.context =
                context.getApplicationContext();
    }

    // =========================================================
    // ANALYZE CURRENT SCREEN
    // =========================================================

    public String analyzeCurrentScreen() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            enabled = false;

            return
                    "JARVIS SCREEN INTELLIGENCE\n\n"
                    + "الحالة: OFFLINE ⚠\n\n"
                    + "خاصك تفعل Accessibility Service "
                    + "باش JARVIS يقدر يشوف ويتحكم فالشاشة.";
        }

        enabled = true;

        String screenText =
                safe(
                        service.getScreenText()
                );

        String packageName =
                safe(
                        service.getCurrentPackage()
                );

        String tree =
                safe(
                        service.getScreenTree()
                );

        lastPackage =
                packageName;

        lastAnalysisTime =
                System.currentTimeMillis();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SCREEN INTELLIGENCE\n"
        );

        result.append(
                "===========================\n\n"
        );

        result.append(
                "ACCESS: ONLINE ✓\n"
        );

        result.append(
                "PACKAGE: "
        );

        result.append(
                packageName.isEmpty()
                        ? "UNKNOWN"
                        : packageName
        );

        result.append(
                "\n\n"
        );

        result.append(
                "SCREEN TEXT:\n"
        );

        result.append(
                screenText.isEmpty()
                        ? "لا يوجد نص واضح."
                        : screenText
        );

        result.append(
                "\n\n"
        );

        result.append(
                "SCREEN STRUCTURE:\n"
        );

        result.append(
                tree
        );

        result.append(
                "\n\n"
        );

        result.append(
                analyzeCapabilities(
                        tree
                )
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    // =========================================================
    // ANALYZE PROVIDED SCREEN DATA
    // =========================================================

    public String analyzeScreen(
            String screenDescription
    ) {

        if (screenDescription == null ||
                screenDescription.trim().isEmpty()) {

            return analyzeCurrentScreen();
        }

        String description =
                screenDescription.trim();

        enabled = true;

        lastAnalysisTime =
                System.currentTimeMillis();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SCREEN INTELLIGENCE\n"
        );

        result.append(
                "===========================\n\n"
        );

        result.append(
                "SCREEN DATA RECEIVED ✓\n\n"
        );

        result.append(
                "CONTENT:\n"
        );

        result.append(
                description
        );

        result.append(
                "\n\n"
        );

        result.append(
                detectElements(
                        description
                )
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    // =========================================================
    // DETECT SCREEN ELEMENTS
    // =========================================================

    private String detectElements(
            String description
    ) {

        String lower =
                description.toLowerCase(
                        Locale.ROOT
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "العناصر المكتشفة:\n"
        );

        boolean detected =
                false;

        if (containsAny(
                lower,
                "button",
                "زر",
                "click",
                "اضغط",
                "كليك"
        )) {

            result.append(
                    "• عنصر قابل للضغط ✓\n"
            );

            detected = true;
        }

        if (containsAny(
                lower,
                "text",
                "نص",
                "رسالة",
                "title",
                "عنوان"
        )) {

            result.append(
                    "• نص / معلومات ✓\n"
            );

            detected = true;
        }

        if (containsAny(
                lower,
                "input",
                "خانة",
                "حقل",
                "edittext",
                "editable"
        )) {

            result.append(
                    "• حقل إدخال ✓\n"
            );

            detected = true;
        }

        if (containsAny(
                lower,
                "image",
                "صورة",
                "photo",
                "صوره"
        )) {

            result.append(
                    "• صورة ✓\n"
            );

            detected = true;
        }

        if (containsAny(
                lower,
                "menu",
                "قائمة",
                "drawer"
        )) {

            result.append(
                    "• قائمة / Menu ✓\n"
            );

            detected = true;
        }

        if (containsAny(
                lower,
                "scroll",
                "تمرير",
                "سكرول",
                "scrollable"
        )) {

            result.append(
                    "• منطقة قابلة للتمرير ✓\n"
            );

            detected = true;
        }

        if (!detected) {

            result.append(
                    "• ما تحدد حتى عنصر خاص.\n"
            );
        }

        return result.toString();
    }

    // =========================================================
    // ANALYZE ACCESSIBILITY TREE
    // =========================================================

    private String analyzeCapabilities(
            String tree
    ) {

        if (tree == null ||
                tree.trim().isEmpty()) {

            return
                    "CAPABILITIES:\n"
                    + "ما كايناش معلومات كافية.";
        }

        String lower =
                tree.toLowerCase(
                        Locale.ROOT
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "DETECTED CAPABILITIES:\n"
        );

        boolean found =
                false;

        if (lower.contains(
                "[clickable]"
        )) {

            result.append(
                    "• الضغط على العناصر: YES ✓\n"
            );

            found = true;
        }

        if (lower.contains(
                "[editable]"
        )) {

            result.append(
                    "• إدخال النص: YES ✓\n"
            );

            found = true;
        }

        if (lower.contains(
                "[scrollable]"
        )) {

            result.append(
                    "• التمرير: YES ✓\n"
            );

            found = true;
        }

        if (!found) {

            result.append(
                    "• قدرات تفاعلية واضحة: غير مكتشفة.\n"
            );
        }

        return result.toString();
    }

    // =========================================================
    // CLICK ELEMENT
    // =========================================================

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
                    "JARVIS: Accessibility Service غير مفعلة.";
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

    // =========================================================
    // FIND ELEMENT
    // =========================================================

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
                    "JARVIS: Accessibility Service غير مفعلة.";
        }

        return service.getNodeInfoByText(
                target.trim()
        );
    }

    // =========================================================
    // TYPE TEXT
    // =========================================================

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
                    "JARVIS: Accessibility Service غير مفعلة.";
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

    // =========================================================
    // SCROLL
    // =========================================================

    public String scrollForward() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: Accessibility Service غير مفعلة.";
        }

        boolean result =
                service.scrollForward();

        if (result) {

            return
                    "JARVIS: تم التمرير للأسفل ✓";
        }

        return
                "JARVIS: ما قدرتش نمرر الشاشة للأسفل.";
    }

    public String scrollBackward() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: Accessibility Service غير مفعلة.";
        }

        boolean result =
                service.scrollBackward();

        if (result) {

            return
                    "JARVIS: تم التمرير للأعلى ✓";
        }

        return
                "JARVIS: ما قدرتش نمرر الشاشة للأعلى.";
    }

    // =========================================================
    // LAST ANALYSIS
    // =========================================================

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

    // =========================================================
    // CURRENT PACKAGE
    // =========================================================

    public String getCurrentPackage() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return "";
        }

        return safe(
                service.getCurrentPackage()
        );
    }

    // =========================================================
    // CURRENT SCREEN TEXT
    // =========================================================

    public String getCurrentScreenText() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "Accessibility Service غير مفعلة.";
        }

        return service.getScreenText();
    }

    // =========================================================
    // CURRENT SCREEN TREE
    // =========================================================

    public String getCurrentScreenTree() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "Accessibility Service غير مفعلة.";
        }

        return service.getScreenTree();
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

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

    // =========================================================
    // CONNECTION
    // =========================================================

    public boolean isConnected() {

        return
                JarvisAccessibilityService
                        .getInstance() != null;
    }

    public boolean isHealthy() {

        return context != null;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "Screen Intelligence: STANDBY\n"
                    + "Accessibility Service: OFFLINE ⚠";
        }

        String packageName =
                safe(
                        service.getCurrentPackage()
                );

        return
                "Screen Intelligence: ONLINE ✓\n"
                + "Accessibility Service: CONNECTED ✓\n"
                + "Current Package: "
                + (
                packageName.isEmpty()
                        ? "UNKNOWN"
                        : packageName
        );
    }

    // =========================================================
    // ANALYSIS TIME
    // =========================================================

    public long getLastAnalysisTime() {

        return lastAnalysisTime;
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value.trim();
    }

    // =========================================================
    // MATCHING
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null) {

            return false;
        }

        for (String value : values) {

            if (value != null &&
                    text.contains(
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