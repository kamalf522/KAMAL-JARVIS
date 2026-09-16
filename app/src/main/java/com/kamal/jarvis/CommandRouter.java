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
    private final MemoryManager memoryManager;
    private final EvolutionEngine evolutionEngine;

    public CommandRouter(Context context) {
        this.context = context.getApplicationContext();

        memoryManager = new MemoryManager(this.context);
        evolutionEngine = new EvolutionEngine(this.context);
    }

    public String execute(String command) {

        if (command == null || command.trim().isEmpty()) {
            return "ما سمعت حتى أمر.";
        }

        String original = command.trim();
        String cmd = original.toLowerCase(Locale.ROOT);

        // =========================
        // تحية
        // =========================

        if (containsAny(cmd,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "أهلا",
                "hello",
                "hi")) {

            return "مرحبا كمال. JARVIS حاضر ومستعد.";
        }

        // =========================
        // مساعدة
        // =========================

        if (containsAny(cmd,
                "مساعدة",
                "ساعدني",
                "شنو نقدر ندير",
                "الأوامر",
                "الاوامر")) {

            return getHelp();
        }

        // =========================
        // حالة النظام
        // =========================

        if (containsAny(cmd,
                "حالة النظام",
                "حالتك",
                "ستاتوس",
                "status",
                "كيف داير")) {

            return evolutionEngine.getEvolutionStatus();
        }

        // =========================
        // القدرات
        // =========================

        if (containsAny(cmd,
                "شنو هي القدرات ديالك",
                "ما هي قدراتك",
                "شنو قدراتك",
                "القدرات ديالك",
                "قدراتك",
                "capabilities")) {

            return evolutionEngine.getCapabilitiesStatus();
        }

        // =========================
        // المهارات
        // =========================

        if (containsAny(cmd,
                "شنو هي المهارات ديالك",
                "شنو المهارات ديالك",
                "ما هي مهاراتك",
                "المهارات ديالك",
                "مهاراتك",
                "skills")) {

            return evolutionEngine.getSkillsStatus();
        }

        // =========================
        // التشخيص الذاتي
        // =========================

        if (containsAny(cmd,
                "شخص نفسك",
                "شخص النظام",
                "التشخيص",
                "التشخيص الذاتي",
                "شنو ناقصك",
                "شنو ناقص",
                "ما الذي ينقصك",
                "self diagnosis",
                "diagnosis")) {

            return evolutionEngine.selfDiagnosis();
        }

        // =========================
        // بدء التطور
        // =========================

        if (containsAny(cmd,
                "طور نفسك",
                "طور ذاتك",
                "ابدأ التطور",
                "بدا التطور",
                "ابدأ دورة التطور",
                "دورة التطور",
                "evolve",
                "evolution")) {

            return evolutionEngine.runEvolutionCycle();
        }

        // =========================
        // حالة التطور
        // =========================

        if (containsAny(cmd,
                "حالة التطور",
                "ستاتوس التطور",
                "فين وصل التطور",
                "تطورك")) {

            return evolutionEngine.getEvolutionStatus();
        }

        // =========================
        // تاريخ التطور
        // =========================

        if (containsAny(cmd,
                "تاريخ التطور",
                "سجل التطور",
                "تاريخك",
                "سجل العمليات",
                "history")) {

            return evolutionEngine.getEvolutionHistory();
        }

        // =========================
        // إضافة مهارة
        // =========================

        if (cmd.startsWith("أضف مهارة")
                || cmd.startsWith("اضف مهارة")) {

            String skill =
                    original
                            .replaceFirst(
                                    "(?i)^أضف مهارة\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^اضف مهارة\\s*",
                                    ""
                            )
                            .trim();

            if (skill.isEmpty()) {
                return "قول ليا اسم المهارة اللي بغيتي نضيف.";
            }

            evolutionEngine.registerSkill(
                    skill,
                    "مهارة أضافها كمال إلى نظام JARVIS."
            );

            return "تم تسجيل المهارة: " + skill;
        }

        // =========================
        // هدف جديد
        // =========================

        if (cmd.startsWith("هدف جديد")
                || cmd.startsWith("اضف هدف")
                || cmd.startsWith("أضف هدف")) {

            String goal =
                    original
                            .replaceFirst(
                                    "(?i)^هدف جديد\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^اضف هدف\\s*",
                                    ""
                            )
                            .replaceFirst(
                                    "(?i)^أضف هدف\\s*",
                                    ""
                            )
                            .trim();

            if (goal.isEmpty()) {
                return "قول ليا شنو هو الهدف.";
            }

            evolutionEngine.createEvolutionGoal(goal);

            return "تم تسجيل الهدف: " + goal;
        }

        // =========================
        // حفظ ذاكرة
        // =========================

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

            String key = "memory_" + System.currentTimeMillis();

            memoryManager.saveMemory(
                    key,
                    memory
            );

            return "حفظتها في الذاكرة.";
        }

        // =========================
        // عدد الذكريات
        // =========================

        if (containsAny(cmd,
                "شنو حافظ",
                "الذاكرة ديالك",
                "ذكرياتك",
                "memory")) {

            return "عندي حاليا "
                    + memoryManager.getMemoryCount()
                    + " ذكريات محفوظة.";
        }

        // =========================
        // مسح الذاكرة
        // =========================

        if (containsAny(cmd,
                "مسح الذاكرة",
                "امسح الذاكرة",
                "حذف الذاكرة",
                "نسى كلشي")) {

            memoryManager.clearAllMemories();

            return "تم مسح الذاكرة.";
        }

        // =========================
        // الوقت
        // =========================

        if (containsAny(cmd,
                "الوقت",
                "شحال فالوقت",
                "كم الساعة",
                "الساعة")) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(new Date());

            return "الوقت دابا هو " + time;
        }

        // =========================
        // التاريخ
        // =========================

        if (containsAny(cmd,
                "التاريخ",
                "نهار شحال",
                "اليوم شحال")) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date());

            return "التاريخ اليوم هو " + date;
        }

        // =========================
        // إيقاف الكلام
        // =========================

        if (containsAny(cmd,
                "سكت",
                "اسكت",
                "وقف الكلام",
                "وقف الصوت",
                "stop speaking")) {

            return "__STOP_SPEAKING__";
        }

        // =========================
        // فتح YouTube
        // =========================

        if (containsAny(cmd,
                "فتح يوتيوب",
                "افتح يوتيوب",
                "youtube")) {

            openUrl(
                    "https://www.youtube.com"
            );

            return "فتحت YouTube.";
        }

        // =========================
        // فتح Google
        // =========================

        if (containsAny(cmd,
                "فتح جوجل",
                "افتح جوجل",
                "google")) {

            openUrl(
                    "https://www.google.com"
            );

            return "فتحت Google.";
        }

        // =========================
        // فتح Facebook
        // =========================

        if (containsAny(cmd,
                "فتح فيسبوك",
                "افتح فيسبوك",
                "facebook")) {

            openUrl(
                    "https://www.facebook.com"
            );

            return "فتحت Facebook.";
        }

        // =========================
        // فتح Instagram
        // =========================

        if (containsAny(cmd,
                "فتح انستغرام",
                "افتح انستغرام",
                "instagram")) {

            openUrl(
                    "https://www.instagram.com"
            );

            return "فتحت Instagram.";
        }

        // =========================
        // فتح WhatsApp
        // =========================

        if (containsAny(cmd,
                "فتح واتساب",
                "افتح واتساب",
                "whatsapp")) {

            openUrl(
                    "https://wa.me/"
            );

            return "فتحت WhatsApp.";
        }

        // =========================
        // إعدادات الهاتف
        // =========================

        if (containsAny(cmd,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "الإعدادات",
                "الاعدادات",
                "settings")) {

            openSettings();

            return "فتحت إعدادات الهاتف.";
        }

        // =========================
        // Wi-Fi
        // =========================

        if (containsAny(cmd,
                "افتح الواي فاي",
                "الواي فاي",
                "wifi")) {

            openWifiSettings();

            return "فتحت إعدادات Wi-Fi.";
        }

        // =========================
        // Bluetooth
        // =========================

        if (containsAny(cmd,
                "افتح البلوتوث",
                "البلوتوث",
                "bluetooth")) {

            openBluetoothSettings();

            return "فتحت إعدادات Bluetooth.";
        }

        // =========================
        // الهاتف
        // =========================

        if (containsAny(cmd,
                "معلومات الهاتف",
                "حول الهاتف",
                "عن الهاتف")) {

            openPhoneSettings();

            return "فتحت معلومات الهاتف.";
        }

        // =========================
        // أمر غير معروف
        // =========================

        evolutionEngine.createEvolutionGoal(
                "تعلم كيفية التعامل مع الأمر: " + original
        );

        return "الأمر مازال ما عنديش له قدرة مباشرة.\n"
                + "سجلتو كهدف تعلم باش نطورو النظام مستقبلا.";
    }

    // ==================================================
    // HELP
    // ==================================================

    private String getHelp() {

        return "أوامر JARVIS المتاحة حاليا:\n\n"

                + "• شنو هي القدرات ديالك\n"
                + "• شنو هي المهارات ديالك\n"
                + "• شنو ناقصك\n"
                + "• طور نفسك\n"
                + "• حالة التطور\n"
                + "• تاريخ التطور\n"
                + "• أضف مهارة ...\n"
                + "• هدف جديد ...\n"
                + "• تذكر ...\n"
                + "• شحال فالوقت\n"
                + "• نهار شحال\n"
                + "• افتح يوتيوب\n"
                + "• افتح جوجل\n"
                + "• افتح فيسبوك\n"
                + "• افتح انستغرام\n"
                + "• افتح واتساب\n"
                + "• افتح الإعدادات\n"
                + "• افتح الواي فاي\n"
                + "• افتح البلوتوث\n"
                + "• اسكت";
    }

    // ==================================================
    // UTILITIES
    // ==================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        for (String value : values) {

            if (text.contains(
                    value.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }

        return false;
    }

    private void openUrl(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    private void openSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    private void openWifiSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_WIFI_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    private void openBluetoothSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_BLUETOOTH_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    private void openPhoneSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_DEVICE_INFO_SETTINGS
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }
}