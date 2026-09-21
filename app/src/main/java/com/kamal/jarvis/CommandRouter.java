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

        // =====================================================
        // VOICE
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
                "اهلا",
                "hello",
                "hi"
        )) {
            return "مرحبا كمال. JARVIS CORE حاضر ومستعد.";
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
                    ).format(new Date());

            return "الوقت دابا هو " + time;
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

            return "التاريخ اليوم هو " + date;
        }

        // =====================================================
        // COMMON APPS
        // =====================================================

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

        // =====================================================
        // WEB SEARCH
        // =====================================================

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

        // =====================================================
        // URL
        // =====================================================

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
            return androidControl.openSettings();
        }

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi",
                "wi fi"
        )) {