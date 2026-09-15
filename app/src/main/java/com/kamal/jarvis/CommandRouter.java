package com.kamal.jarvis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommandRouter {

    public interface ResponseListener {
        void onResponse(String message);
    }

    private final Context context;
    private final MemoryManager memory;
    private final ResponseListener listener;

    public CommandRouter(
            Context context,
            MemoryManager memory,
            ResponseListener listener
    ) {
        this.context = context;
        this.memory = memory;
        this.listener = listener;
    }

    public void execute(String command) {

        if (command == null) {
            respond("ما فهمتش الأمر.");
            return;
        }

        String original =
                command.trim();

        if (original.isEmpty()) {
            respond("كتب ليا شي أمر.");
            return;
        }

        String text =
                original.toLowerCase(
                        Locale.ROOT
                );

        // =========================
        // التحية
        // =========================

        if (containsAny(
                text,
                "سلام",
                "مرحبا",
                "السلام عليكم",
                "hello",
                "hi"
        )) {

            respond(
                    "مرحبا كمال. JARVIS حاضر."
            );

            return;
        }

        // =========================
        // المساعدة
        // =========================

        if (containsAny(
                text,
                "ساعدني",
                "المساعدة",
                "شنو تقدر",
                "ماذا تستطيع",
                "help"
        )) {

            respond(
                    "نقدر حاليا ننفذ أوامر الهاتف الأساسية، " +
                    "نفتح التطبيقات والمواقع، نعرف الوقت والتاريخ، " +
                    "ونحفظ معلومات في ذاكرتي."
            );

            return;
        }

        // =========================
        // الوقت
        // =========================

        if (containsAny(
                text,
                "الوقت",
                "الساعة",
                "شحال فالوقت"
        )) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            respond(
                    "الوقت دابا هو " + time
            );

            return;
        }

        // =========================
        // التاريخ
        // =========================

        if (containsAny(
                text,
                "التاريخ",
                "نهار شحال",
                "اليوم"
        )) {

            String date =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            respond(
                    "اليوم هو " + date
            );

            return;
        }

        // =========================
        // حفظ معلومة
        // =========================

        if (text.startsWith("حفظ ")
                || text.startsWith("سجل ")
                || text.startsWith("تذكر ")) {

            String data =
                    original.substring(
                            original.indexOf(" ") + 1
                    ).trim();

            saveInformation(data);

            return;
        }

        // =========================
        // استرجاع الذاكرة
        // =========================

        if (containsAny(
                text,
                "شنو كتذكر",
                "شنو حافظ",
                "ماذا تتذكر",
                "ذاكرتك",
                "عرض الذاكرة"
        )) {

            respond(
                    memory.getAllMemories()
            );

            return;
        }

        // =========================
        // مسح الذاكرة
        // =========================

        if (containsAny(
                text,
                "مسح الذاكرة",
                "امسح الذاكرة",
                "نسى كلشي",
                "انسى كل شيء"
        )) {

            memory.clearAllMemories();

            respond(
                    "تم مسح الذاكرة."
            );

            return;
        }

        // =========================
        // YouTube
        // =========================

        if (containsAny(
                text,
                "يوتيوب",
                "youtube"
        )) {

            openUrl(
                    "https://www.youtube.com"
            );

            respond(
                    "فتحت ليك YouTube."
            );

            return;
        }

        // =========================
        // Google
        // =========================

        if (containsAny(
                text,
                "غوغل",
                "جوجل",
                "google"
        )) {

            openUrl(
                    "https://www.google.com"
            );

            respond(
                    "فتحت ليك Google."
            );

            return;
        }

        // =========================
        // Facebook
        // =========================

        if (containsAny(
                text,
                "فيسبوك",
                "فايسبوك",
                "facebook"
        )) {

            openUrl(
                    "https://www.facebook.com"
            );

            respond(
                    "فتحت ليك Facebook."
            );

            return;
        }

        // =========================
        // Instagram
        // =========================

        if (containsAny(
                text,
                "انستغرام",
                "انستجرام",
                "instagram"
        )) {

            openUrl(
                    "https://www.instagram.com"
            );

            respond(
                    "فتحت ليك Instagram."
            );

            return;
        }

        // =========================
        // WhatsApp
        // =========================

        if (containsAny(
                text,
                "واتساب",
                "whatsapp"
        )) {

            openUrl(
                    "https://web.whatsapp.com"
            );

            respond(
                    "فتحت ليك WhatsApp."
            );

            return;
        }

        // =========================
        // الإعدادات
        // =========================

        if (containsAny(
                text,
                "الإعدادات",
                "اعدادات الهاتف",
                "settings"
        )) {

            openSettings();

            respond(
                    "فتحت إعدادات الهاتف."
            );

            return;
        }

        // =========================
        // Wi-Fi
        // =========================

        if (containsAny(
                text,
                "الواي فاي",
                "wifi",
                "wi-fi"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_WIFI_SETTINGS
                        );

                context.startActivity(intent);

                respond(
                        "فتحت إعدادات Wi-Fi."
                );

            } catch (Exception e) {

                respond(
                        "ما قدرتش نفتح إعدادات Wi-Fi."
                );
            }

            return;
        }

        // =========================
        // Bluetooth
        // =========================

        if (containsAny(
                text,
                "البلوتوث",
                "bluetooth"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Settings.ACTION_BLUETOOTH_SETTINGS
                        );

                context.startActivity(intent);

                respond(
                        "فتحت إعدادات Bluetooth."
                );

            } catch (Exception e) {

                respond(
                        "ما قدرتش نفتح إعدادات Bluetooth."
                );
            }

            return;
        }

        // =========================
        // الهاتف
        // =========================

        if (containsAny(
                text,
                "الهاتف",
                "اتصل",
                "phone"
        )) {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_DIAL
                        );

                context.startActivity(intent);

                respond(
                        "فتحت واجهة الاتصال."
                );

            } catch (Exception e) {

                respond(
                        "ما قدرتش نفتح تطبيق الهاتف."
                );
            }

            return;
        }

        // =========================
        // إيقاف الكلام
        // =========================

        if (containsAny(
                text,
                "اسكت",
                "توقف عن الكلام",
                "وقف الصوت",
                "stop"
        )) {

            respond(
                    "حاضر."
            );

            return;
        }

        // =========================
        // أمر غير معروف
        // =========================

        respond(
                "مازال ما تعلمتش هاد الأمر: " +
                original +
                "\n\n" +
                "تقدر تقول ليا مثلا: " +
                "\"حفظ اسمي كمال\"."
        );
    }

    private void saveInformation(
            String data
    ) {

        if (data.isEmpty()) {

            respond(
                    "شنو بغيتي نحفظ؟"
            );

            return;
        }

        String key =
                createMemoryKey(data);

        String value =
                createMemoryValue(data);

        memory.saveMemory(
                key,
                value
        );

        respond(
                "حفظتها في الذاكرة:\n" +
                value
        );
    }

    private String createMemoryKey(
            String data
    ) {

        String text =
                data.toLowerCase(
                        Locale.ROOT
                );

        if (text.startsWith("اسمي ")) {
            return "الاسم";
        }

        if (text.startsWith("عمري ")) {
            return "العمر";
        }

        if (text.startsWith("خدمتي ")) {
            return "العمل";
        }

        if (text.startsWith("هدفي ")) {
            return "الهدف";
        }

        if (text.startsWith("حلمي ")) {
            return "الحلم";
        }

        if (text.startsWith("كنحب ")) {
            return "الهواية";
        }

        return "معلومة_" +
                memory.getMemoryCount();
    }

    private String createMemoryValue(
            String data
    ) {

        return data.trim();
    }

    private void openUrl(
            String url
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            context.startActivity(intent);

        } catch (Exception e) {

            respond(
                    "ما قدرتش نفتح الرابط."
            );
        }
    }

    private void openSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_SETTINGS
                    );

            context.startActivity(intent);

        } catch (Exception e) {

            respond(
                    "ما قدرتش نفتح الإعدادات."
            );
        }
    }

    private boolean containsAny(
            String text,
            String... words
    ) {

        for (String word : words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private void respond(
            String message
    ) {

        if (listener != null) {

            listener.onResponse(
                    message
            );
        }
    }
}