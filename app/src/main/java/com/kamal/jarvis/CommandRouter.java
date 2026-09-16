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
        this.context = context;
        this.memoryManager = new MemoryManager(context);
        this.evolutionEngine =
                new EvolutionEngine(
                        context,
                        memoryManager
                );
    }

    public String execute(String command) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما سمعت حتى أمر.";
        }

        String input =
                command.trim();

        String lower =
                input.toLowerCase(
                        Locale.getDefault()
                );

        // =====================================================
        // GREETINGS
        // =====================================================

        if (containsAny(
                lower,
                "سلام",
                "السلام عليكم",
                "مرحبا",
                "اهلا",
                "أهلا",
                "hello",
                "hi"
        )) {

            return "مرحبا كمال. JARVIS حاضر.";
        }

        // =====================================================
        // HELP
        // =====================================================

        if (containsAny(
                lower,
                "مساعدة",
                "ساعدني",
                "شنو تقدر",
                "ماذا تستطيع",
                "الأوامر"
        )) {

            return getHelp();
        }

        // =====================================================
        // EVOLUTION
        // =====================================================

        if (containsAny(
                lower,
                "طور نفسك",
                "طوّر نفسك",
                "ابدأ التطور",
                "ابدأ تطوير نفسك",
                "تطور",
                "التطور"
        )) {

            return evolutionEngine
                    .runEvolutionCycle();
        }

        // =====================================================
        // SELF DIAGNOSIS
        // =====================================================

        if (containsAny(
                lower,
                "شخص نفسك",
                "شخص ذاتك",
                "فحص نفسك",
                "افحص نفسك",
                "شنو ناقصك",
                "ما الذي ينقصك"
        )) {

            return evolutionEngine
                    .selfDiagnosis();
        }

        // =====================================================
        // EVOLUTION STATUS
        // =====================================================

        if (containsAny(
                lower,
                "حالة التطور",
                "حالة النظام",
                "وضع التطور",
                "تقدمك",
                "تقدم النظام"
        )) {

            return evolutionEngine
                    .getEvolutionStatus();
        }

        // =====================================================
        // EVOLUTION HISTORY
        // =====================================================

        if (containsAny(
                lower,
                "سجل التطور",
                "تاريخ التطور",
                "ماذا طورت",
                "شنو طورت"
        )) {

            return evolutionEngine
                    .getEvolutionHistory();
        }

        // =====================================================
        // ADD SKILL
        // =====================================================

        if (lower.startsWith("أضف مهارة")) {

            String skill =
                    input.substring(
                            "أضف مهارة".length()
                    ).trim();

            if (skill.isEmpty()) {

                return "قول لي اسم المهارة التي تريد إضافتها.";
            }

            boolean added =
                    evolutionEngine.registerSkill(
                            skill,
                            "مهارة أضافها كمال إلى JARVIS."
                    );

            if (added) {

                return "تمت إضافة المهارة: "
                        + skill;
            }

            return "هذه المهارة موجودة بالفعل.";
        }

        if (lower.startsWith("اضف مهارة")) {

            String skill =
                    input.substring(
                            "اضف مهارة".length()
                    ).trim();

            if (skill.isEmpty()) {

                return "قول لي اسم المهارة التي تريد إضافتها.";
            }

            boolean added =
                    evolutionEngine.registerSkill(
                            skill,
                            "مهارة أضافها كمال إلى JARVIS."
                    );

            if (added) {

                return "تمت إضافة المهارة: "
                        + skill;
            }

            return "هذه المهارة موجودة بالفعل.";
        }

        // =====================================================
        // ADD GOAL
        // =====================================================

        if (lower.startsWith("هدف جديد")) {

            String goal =
                    input.substring(
                            "هدف جديد".length()
                    ).trim();

            if (goal.isEmpty()) {

                return "قول لي الهدف الذي تريد أن أعمل عليه.";
            }

            boolean created =
                    evolutionEngine
                            .createEvolutionGoal(
                                    goal
                            );

            if (created) {

                return "تم تسجيل الهدف: "
                        + goal;
            }

            return "تعذر تسجيل الهدف.";
        }

        // =====================================================
        // TIME
        // =====================================================

        if (containsAny(
                lower,
                "الساعة",
                "الوقت",
                "شحال فالساعة",
                "كم الساعة"
        )) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            return "الساعة الآن هي "
                    + time;
        }

        // =====================================================
        // DATE
        // =====================================================

        if (containsAny(
                lower,
                "التاريخ",
                "اليوم",
                "شنو نهار اليوم"
        )) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            return "تاريخ اليوم هو "
                    + date;
        }

        // =====================================================
        // MEMORY - SAVE
        // =====================================================

        if (lower.startsWith("تذكر أن")) {

            String value =
                    input.substring(
                            "تذكر أن".length()
                    ).trim();

            if (!value.isEmpty()) {

                memoryManager.saveMemory(
                        "custom_" +
                                System.currentTimeMillis(),
                        value
                );

                return "تم حفظ المعلومة في ذاكرتي.";
            }
        }

        if (lower.startsWith("تذكر")) {

            String value =
                    input.substring(
                            "تذكر".length()
                    ).trim();

            if (!value.isEmpty()) {

                memoryManager.saveMemory(
                        "custom_" +
                                System.currentTimeMillis(),
                        value
                );

                return "تم حفظ المعلومة.";
            }
        }

        // =====================================================
        // MEMORY - CLEAR
        // =====================================================

        if (containsAny(
                lower,
                "امسح الذاكرة",
                "احذف الذاكرة",
                "انس كل شيء",
                "انسى كل شيء"
        )) {

            memoryManager.clearAllMemories();

            return "تم مسح الذاكرة.";
        }

        // =====================================================
        // OPEN YOUTUBE
        // =====================================================

        if (containsAny(
                lower,
                "افتح يوتيوب",
                "فتح يوتيوب",
                "youtube"
        )) {

            openUrl(
                    "https://www.youtube.com"
            );

            return "تم فتح يوتيوب.";
        }

        // =====================================================
        // OPEN GOOGLE
        // =====================================================

        if (containsAny(
                lower,
                "افتح جوجل",
                "فتح جوجل",
                "google"
        )) {

            openUrl(
                    "https://www.google.com"
            );

            return "تم فتح جوجل.";
        }

        // =====================================================
        // OPEN FACEBOOK
        // =====================================================

        if (containsAny(
                lower,
                "افتح فيسبوك",
                "فتح فيسبوك",
                "facebook"
        )) {

            openUrl(
                    "https://www.facebook.com"
            );

            return "تم فتح فيسبوك.";
        }

        // =====================================================
        // OPEN INSTAGRAM
        // =====================================================

        if (containsAny(
                lower,
                "افتح انستغرام",
                "فتح انستغرام",
                "instagram"
        )) {

            openUrl(
                    "https://www.instagram.com"
            );

            return "تم فتح انستغرام.";
        }

        // =====================================================
        // OPEN WHATSAPP
        // =====================================================

        if (containsAny(
                lower,
                "افتح واتساب",
                "فتح واتساب",
                "whatsapp"
        )) {

            try {

                Intent intent =
                        context.getPackageManager()
                                .getLaunchIntentForPackage(
                                        "com.whatsapp"
                                );

                if (intent != null) {

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    context.startActivity(intent);

                    return "تم فتح واتساب.";
                }

            } catch (Exception ignored) {
            }

            return "واتساب غير متوفر.";
        }

        // =====================================================
        // SETTINGS
        // =====================================================

        if (containsAny(
                lower,
                "افتح الإعدادات",
                "افتح الاعدادات",
                "الإعدادات",
                "الاعدادات"
        )) {

            openSettings();

            return "تم فتح الإعدادات.";
        }

        // =====================================================
        // WIFI
        // =====================================================

        if (containsAny(
                lower,
                "افتح الواي فاي",
                "افتح wifi",
                "wifi"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_WIFI_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return "تم فتح إعدادات Wi-Fi.";

            } catch (Exception e) {

                return "تعذر فتح إعدادات Wi-Fi.";
            }
        }

        // =====================================================
        // BLUETOOTH
        // =====================================================

        if (containsAny(
                lower,
                "افتح البلوتوث",
                "افتح bluetooth",
                "bluetooth"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings
                                        .ACTION_BLUETOOTH_SETTINGS
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return "تم فتح إعدادات Bluetooth.";

            } catch (Exception e) {

                return "تعذر فتح إعدادات Bluetooth.";
            }
        }

        // =====================================================
        // PHONE
        // =====================================================

        if (containsAny(
                lower,
                "افتح الهاتف",
                "افتح الاتصال",
                "الهاتف"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_DIAL
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                context.startActivity(intent);

                return "تم فتح الهاتف.";

            } catch (Exception e) {

                return "تعذر فتح الهاتف.";
            }
        }

        // =====================================================
        // STOP
        // =====================================================

        if (containsAny(
                lower,
                "توقف عن الكلام",
                "اسكت",
                "توقف"
        )) {

            return "__STOP_SPEAKING__";
        }

        // =====================================================
        // UNKNOWN COMMAND
        // =====================================================

        evolutionEngine.createEvolutionGoal(
                "تعلم كيفية التعامل مع الأمر: "
                        + input
        );

        return
                "لم أفهم الأمر بالكامل بعد.\n\n" +
                "سجلته كقدرة أحتاج إلى تعلمها:\n" +
                input;
    }

    // =========================================================
    // HELP
    // =========================================================

    private String getHelp() {

        return
                "أوامر JARVIS الحالية:\n\n" +

                "• طور نفسك\n" +
                "• شخّص نفسك\n" +
                "• حالة التطور\n" +
                "• سجل التطور\n" +
                "• أضف مهارة [اسم المهارة]\n" +
                "• هدف جديد [الهدف]\n" +
                "• تذكر [المعلومة]\n" +
                "• امسح الذاكرة\n" +
                "• شحال فالساعة\n" +
                "• شنو نهار اليوم\n" +
                "• افتح يوتيوب\n" +
                "• افتح جوجل\n" +
                "• افتح فيسبوك\n" +
                "• افتح انستغرام\n" +
                "• افتح واتساب\n" +
                "• افتح الإعدادات\n" +
                "• افتح الواي فاي\n" +
                "• افتح البلوتوث\n" +
                "• افتح الهاتف";
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private boolean containsAny(
            String text,
            String... values
    ) {

        for (String value : values) {

            if (text.contains(
                    value.toLowerCase(
                            Locale.getDefault()
                    )
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
}