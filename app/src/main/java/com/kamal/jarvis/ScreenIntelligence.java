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

    public String analyzeScreen(
            String screenDescription
    ) {

        if (screenDescription == null ||
                screenDescription.trim().isEmpty()) {

            return
                    "ما وصلني حتى وصف للشاشة.";
        }

        String description =
                screenDescription.trim();

        lastAnalysis =
                description;

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

        result.append(description);

        result.append("\n\n");

        result.append(
                detectElements(description)
        );

        enabled = true;

        return result.toString();
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

        if (!detected) {

            result.append(
                    "• لم يتم التعرف على عناصر محددة بعد.\n"
            );
        }

        return result.toString();
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

    public boolean isHealthy() {

        return context != null;
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Screen Intelligence: ONLINE ✓\n"
                    + "Mode: "
                    + (enabled
                    ? "ACTIVE"
                    : "STANDBY");

        }

        return
                "Screen Intelligence: ERROR ⚠";
    }
}