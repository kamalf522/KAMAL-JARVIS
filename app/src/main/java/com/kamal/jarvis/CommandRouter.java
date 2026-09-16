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

    public CommandRouter(Context context) {

        this.context = context.getApplicationContext();

        core = new JarvisCore(this.context);
    }

    // =========================================================
    // MAIN ENTRY
    // =========================================================

    public String execute(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String original = command.trim();
        String cmd = original.toLowerCase(Locale.ROOT);

        // =====================================================
        // STOP SPEAKING
        // =====================================================

        if (containsAny(
                cmd,
                "سكت",
                "اسكت",
                "وقف الكلام",
                "وقف الصوت",
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
                "أهلا",
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
                "شنو نقدر ندير",
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
                "اليوم شحال"
        )) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date());

            return "التاريخ اليوم هو " + date;
        }

        // =====================================================
        // OPEN YOUTUBE
        // =====================================================

        if (containsAny(
                cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "youtube"
        )) {

            openUrl("https://www.youtube.com");

            return "فتحت YouTube.";
        }

        // =====================================================
        // OPEN GOOGLE
        // =====================================================

        if (containsAny(
                cmd,
                "فتح جوجل",
                "افتح جوجل",
                "google"
        )) {

            openUrl("https://www.google.com");

            return "فتحت Google.";
        }

        // =====================================================
        // OPEN FACEBOOK
        // =====================================================

        if (containsAny(
                cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "facebook"
        )) {

            openUrl("https://www.facebook.com");

            return "فتحت Facebook.";
        }

        // =====================================================
        // OPEN INSTAGRAM
        // =====================================================

        if (containsAny(
                cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "instagram"
        )) {

            openUrl("https://www.instagram.com");

            return "فتحت Instagram.";
        }

        // =====================================================
        // OPEN WHATSAPP
        // =====================================================

        if (containsAny(
                cmd,
                "فتح واتساب",
                "افتح واتساب",
                "whatsapp"
        )) {

            openUrl("https://wa.me/");

            return "فتحت WhatsApp.";
        }

        // =====================================================
        // ANDROID SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "الإعدادات",
                "الاعدادات",
                "settings"
        )) {

            openSettings();

            return "فتحت إعدادات الهاتف.";
        }

        // =====================================================
        // WIFI
        // =====================================================

        if (containsAny(
                cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi"
        )) {

            return core.openWifi();
        }

        // =====================================================
        // BLUETOOTH
        // =====================================================

        if (containsAny(
                cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth"
        )) {

            return core.openBluetooth();
        }

        // =====================================================
        // DEVICE INFO
        // =====================================================

        if (containsAny(
                cmd,
                "معلومات الهاتف",
                "حول الهاتف",
                "عن الهاتف"
        )) {

            return core.openDeviceInfo();
        }

        // =====================================================
        // NOTIFICATION SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الإشعارات",
                "اعدادات الاشعارات",
                "notification settings"
        )) {

            return core.openNotificationSettings();
        }

        // =====================================================
        // DISPLAY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الشاشة",
                "اعدادات الشاشة",
                "display settings"
        )) {

            return core.openDisplaySettings();
        }

        // =====================================================
        // SOUND SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات الصوت",
                "اعدادات الصوت",
                "sound settings"
        )) {

            return core.openSoundSettings();
        }

        // =====================================================
        // BATTERY SETTINGS
        // =====================================================

        if (containsAny(
                cmd,
                "إعدادات البطارية",
                "اعدادات البطارية",
                "battery settings"
        )) {

            return core.openBatterySettings();
        }

        // =====================================================
        // REMEMBER
        // =====================================================

        if (cmd.startsWith("تذكر ")
                || cmd.startsWith("احفظ ")
                || cmd.startsWith("حفظ ")) {

            String memory =
                    original
                            .replaceFirst(
                                    "(?i)^تذكر\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^احفظ\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^حفظ\\s*",
                                    ""
                            )
                            .trim();

            if (memory.isEmpty()) {

                return "شنو بغيتي نحفظ؟";
            }

            String key =
                    "memory_" +
                            System.currentTimeMillis();

            return core.remember(
                    key,
                    memory
            );
        }

        // =====================================================
        // SYSTEM STATUS
        // =====================================================

        if (containsAny(
                cmd,
                "حالة النظام",
                "حالتك",
                "ستاتوس",
                "status",
                "كيف داير"
        )) {

            return core.getFullStatus();
        }

        // =====================================================
        // SELF DIAGNOSIS
        // =====================================================

        if (containsAny(
                cmd,
                "شخص نفسك",
                "شخص النظام",
                "التشخيص",
                "التشخيص الذاتي",
                "شنو ناقصك",
                "شنو ناقص",
                "ما الذي ينقصك",
                "self diagnosis",
                "diagnosis"
        )) {

            return core.processCommand(
                    "التشخيص الذاتي"
            );
        }

        // =====================================================
        // SELF TEST
        // =====================================================

        if (containsAny(
                cmd,
                "اختبر نفسك",
                "اختبار النظام",
                "self test"
        )) {

            return core.processCommand(
                    "اختبر نفسك"
            );
        }

        // =====================================================
        // RECOVERY
        // =====================================================

        if (containsAny(
                cmd,
                "صلح نفسك",
                "إصلاح النظام",
                "recovery"
        )) {

            return core.processCommand(
                    "إصلاح النظام"
            );
        }

        // =====================================================
        // EVOLUTION
        // =====================================================

        if (containsAny(
                cmd,
                "طور نفسك",
                "طور ذاتك",
                "ابدأ التطور",
                "بدا التطور",
                "دورة التطور",
                "evolve",
                "evolution"
        )) {

            return core.processCommand(
                    "طور نفسك"
            );
        }

        // =====================================================
        // CAPABILITIES
        // =====================================================

        if (containsAny(
                cmd,
                "شنو هي القدرات ديالك",
                "