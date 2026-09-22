package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ScreenIntelligence {

    private final Context context;

    private boolean enabled = false;

    private String lastAnalysis = "";

    private String lastPackage = "";

    private long lastAnalysisTime = 0L;

    private int analysisCount = 0;

    private int detectedElementCount = 0;

    private boolean hasClickableElements = false;

    private boolean hasEditableElements = false;

    private boolean hasScrollableElements = false;

    private boolean hasTextElements = false;

    public ScreenIntelligence(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "ScreenIntelligence requires a valid Context"
            );
        }

        this.context =
                context.getApplicationContext();
    }

    // =========================================================
    // MAIN SCREEN ANALYSIS
    // =========================================================

    public synchronized String analyzeCurrentScreen() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            enabled = false;

            lastAnalysis =
                    "JARVIS SCREEN INTELLIGENCE\n\n"
                    + "الحالة: OFFLINE ⚠\n\n"
                    + "خاصك تفعل Accessibility Service "
                    + "باش JARVIS يقدر يشوف ويتحكم فالشاشة.";

            return lastAnalysis;
        }

        enabled = true;

        String screenText =
                safe(service.getScreenText());

        String packageName =
                safe(service.getCurrentPackage());

        String tree =
                safe(service.getScreenTree());

        lastPackage = packageName;

        lastAnalysisTime =
                System.currentTimeMillis();

        analysisCount++;

        detectedElementCount = 0;
        hasClickableElements = false;
        hasEditableElements = false;
        hasScrollableElements = false;
        hasTextElements = !screenText.isEmpty();

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

        result.append("\n\n");

        result.append(
                "SCREEN TEXT:\n"
        );

        result.append(
                screenText.isEmpty()
                        ? "لا يوجد نص واضح."
                        : limit(
                                screenText,
                                12000
                        )
        );

        result.append("\n\n");

        result.append(
                "SCREEN STRUCTURE:\n"
        );

        result.append(
                tree.isEmpty()
                        ? "لا توجد بنية شاشة متاحة."
                        : limit(
                                tree,
                                16000
                        )
        );

        result.append("\n\n");

        result.append(
                analyzeCapabilities(tree)
        );

        result.append("\n");

        result.append(
                buildActionHints(
                        screenText,
                        tree
                )
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    // =========================================================
    // ANALYZE PROVIDED DATA
    // =========================================================

    public synchronized String analyzeScreen(
            String screenDescription
    ) {

        if (isEmpty(screenDescription)) {
            return analyzeCurrentScreen();
        }

        String description =
                screenDescription.trim();

        enabled = true;

        lastAnalysisTime =
                System.currentTimeMillis();

        analysisCount++;

        detectedElementCount = 0;
        hasClickableElements = false;
        hasEditableElements = false;
        hasScrollableElements = false;
        hasTextElements = !description.isEmpty();

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
                limit(
                        description,
                        16000
                )
        );

        result.append("\n\n");

        result.append(
                detectElements(description)
        );

        result.append("\n");

        result.append(
                buildActionHints(
                        description,
                        description
                )
        );

        lastAnalysis =
                result.toString();

        return lastAnalysis;
    }

    // =========================================================
    // ELEMENT DETECTION
    // =========================================================

    private String detectElements(
            String description
    ) {

        String lower =
                normalize(description);

        StringBuilder result =
                new StringBuilder();

        result.append(
                "العناصر المكتشفة:\n"
        );

        boolean detected = false;

        if (containsAny(
                lower,
                "button",
                "زر",
                "click",
                "اضغط",
                "كليك",
                "tap"
        )) {

            result.append(
                    "• عنصر قابل للضغط ✓\n"
            );

            hasClickableElements = true;
            detectedElementCount++;
            detected = true;
        }

        if (containsAny(
                lower,
                "text",
                "نص",
                "رسالة",
                "title",
                "عنوان",
                "label"
        )) {

            result.append(
                    "• نص / معلومات ✓\n"
            );

            hasTextElements = true;
            detectedElementCount++;
            detected = true;
        }

        if (containsAny(
                lower,
                "input",
                "خانة",
                "حقل",
                "edittext",
                "editable",
                "اكتب",
                "كتابة"
        )) {

            result.append(
                    "• حقل إدخال ✓\n"
            );

            hasEditableElements = true;
            detectedElementCount++;
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

            detectedElementCount++;
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

            detectedElementCount++;
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

            hasScrollableElements = true;
            detectedElementCount++;
            detected = true;
        }

        if (containsAny(
                lower,
                "checkbox",
                "check box",
                "خانة اختيار"
        )) {

            result.append(
                    "• خانة اختيار ✓\n"
            );

            detectedElementCount++;
            detected = true;
        }

        if (containsAny(
                lower,
                "switch",
                "toggle",
                "مفتاح"
        )) {

            result.append(
                    "• مفتاح تشغيل/إيقاف ✓\n"
            );

            detectedElementCount++;
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
    // ACCESSIBILITY TREE ANALYSIS
    // =========================================================

    private String analyzeCapabilities(
            String tree
    ) {

        if (isEmpty(tree)) {

            return
                    "DETECTED CAPABILITIES:\n"
                    + "ما كايناش معلومات كافية.";
        }

        String lower =
                normalize(tree);

        StringBuilder result =
                new StringBuilder();

        result.append(
                "DETECTED CAPABILITIES:\n"
        );

        boolean found = false;

        if (containsAny(
                lower,
                "[clickable]",
                "clickable"
        )) {

            hasClickableElements = true;

            result.append(
                    "• الضغط على العناصر: YES ✓\n"
            );

            found = true;
        }

        if (containsAny(
                lower,
                "[editable]",
                "editable"
        )) {

            hasEditableElements = true;

            result.append(
                    "• إدخال النص: YES ✓\n"
            );

            found = true;
        }

        if (containsAny(
                lower,
                "[scrollable]",
                "scrollable"
        )) {

            hasScrollableElements = true;

            result.append(
                    "• التمرير: YES ✓\n"
            );

            found = true;
        }

        if (containsAny(
                lower,
                "[enabled]",
                "enabled"
        )) {

            result.append(
                    "• عناصر مفعلة: YES ✓\n"
            );

            found = true;
        }

        if (!found) {

            result.append(
                    "• قدرات تفاعلية واضحة: غير مكتشفة.\n"
            );
        }

        result.append(
                "• عدد العناصر المكتشفة: "
        );

        result.append(
                detectedElementCount
        );

        result.append("\n");

        return result.toString();
    }

    // =========================================================
    // ACTION HINTS
    // =========================================================

    private String buildActionHints(
            String screenText,
            String tree
    ) {

        String combined =
                normalize(
                        safe(screenText)
                                + " "
                                + safe(tree)
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS ACTION HINTS:\n"
        );

        List<String> hints =
                new ArrayList<>();

        if (hasClickableElements ||
                containsAny(
                        combined,
                        "button",
                        "زر",
                        "clickable",
                        "اضغط"
                )) {

            hints.add(
                    "يمكن محاولة الضغط على عنصر مناسب."
            );
        }

        if (hasEditableElements ||
                containsAny(
                        combined,
                        "input",
                        "edittext",
                        "editable",
                        "حقل",
                        "خانة"
                )) {

            hints.add(
                    "يمكن محاولة إدخال النص."
            );
        }

        if (hasScrollableElements ||
                containsAny(
                        combined,
                        "scrollable",
                        "scroll",
                        "تمرير"
                )) {

            hints.add(
                    "يمكن محاولة التمرير."
            );
        }

        if (containsAny(
                combined,
                "search",
                "بحث",
                "google"
        )) {

            hints.add(
                    "كاين مؤشر على واجهة بحث."
            );
        }

        if (containsAny(
                combined,
                "login",
                "sign in",
                "تسجيل الدخول",
                "دخول"
        )) {

            hints.add(
                    "كاين مؤشر على واجهة تسجيل الدخول."
            );
        }

        if (containsAny(
                combined,
                "password",
                "كلمة السر",
                "كلمه السر"
        )) {

            hints.add(
                    "كاين حقل متعلق بكلمة السر."
            );
        }

        if (hints.isEmpty()) {

            result.append(
                    "• ما كايناش إشارة واضحة لإجراء محدد.\n"
            );

        } else {

            for (String hint : hints) {

                result.append(
                        "• "
                );

                result.append(
                        hint
                );

                result.append(
                        "\n"
                );
            }
        }

        return result.toString();
    }

    // =========================================================
    // CLICK
    // =========================================================

    public String clickElement(
            String target
    ) {

        if (isEmpty(target)) {

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
                            + target.trim()
                            + "\" ✓";
        }

        return
                "JARVIS: ما لقيتش العنصر \""
                        + target.trim()
                        + "\" أو ما قدرتش نضغط عليه.";
    }

    // =========================================================
    // FIND ELEMENT
    // =========================================================

    public String findElement(
            String target
    ) {

        if (isEmpty(target)) {

            return
                    "JARVIS: خاصني اسم العنصر.";
        }

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "JARVIS: Accessibility Service غير مفعلة.";
        }

        return safe(
                service.getNodeInfoByText(
                        target.trim()
                )
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

    public synchronized String getLastAnalysis() {

        if (isEmpty(lastAnalysis)) {

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

            return lastPackage;
        }

        String packageName =
                safe(
                        service.getCurrentPackage()
                );

        if (!packageName.isEmpty()) {
            lastPackage = packageName;
        }

        return lastPackage;
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

        return safe(
                service.getScreenText()
        );
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

        return safe(
                service.getScreenTree()
        );
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    public synchronized String enable() {

        enabled = true;

        return
                "Screen Intelligence: ENABLED ✓";
    }

    public synchronized String disable() {

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

    public synchronized String getStatus() {

        JarvisAccessibilityService service =
                JarvisAccessibilityService.getInstance();

        if (service == null) {

            return
                    "Screen Intelligence: STANDBY\n"
                    + "Accessibility Service: OFFLINE ⚠\n"
                    + "Analyses: "
                    + analysisCount;
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
        )
                + "\n"
                + "Analyses: "
                + analysisCount
                + "\n"
                + "Detected Elements: "
                + detectedElementCount
                + "\n"
                + "Clickable: "
                + (hasClickableElements ? "YES" : "NO")
                + "\n"
                + "Editable: "
                + (hasEditableElements ? "YES" : "NO")
                + "\n"
                + "Scrollable: "
                + (hasScrollableElements ? "YES" : "NO");
    }

    // =========================================================
    // ANALYSIS DATA
    // =========================================================

    public long getLastAnalysisTime() {

        return lastAnalysisTime;
    }

    public int getAnalysisCount() {

        return analysisCount;
    }

    public int getDetectedElementCount() {

        return detectedElementCount;
    }

    public boolean hasClickableElements() {

        return hasClickableElements;
    }

    public boolean hasEditableElements() {

        return hasEditableElements;
    }

    public boolean hasScrollableElements() {

        return hasScrollableElements;
    }

    public boolean hasTextElements() {

        return hasTextElements;
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {

        return context;
    }

    // =========================================================
    // NORMALIZATION
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
                )
                .replace(
                        "ى",
                        "ي"
                );
    }

    // =========================================================
    // MATCHING
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                text.isEmpty()) {

            return false;
        }

        for (String value : values) {

            if (value == null ||
                    value.isEmpty()) {

                continue;
            }

            String normalized =
                    normalize(value);

            if (text.contains(
                    normalized
            )) {

                return true;
            }
        }

        return false;
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
    // LIMIT OUTPUT
    // =========================================================

    private String limit(
            String value,
            int max
    ) {

        if (value == null) {
            return "";
        }

        if (max <= 0) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return
                value.substring(
                        0,
                        max
                )
                        + "\n...[TRUNCATED]";
    }

    // =========================================================
    // EMPTY CHECK
    // =========================================================

    private boolean isEmpty(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}