package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CapabilityManager {

    private static final String PREFS =
            "jarvis_capabilities";

    private static final String KEY_CAPABILITIES =
            "capabilities";

    private final SharedPreferences preferences;

    public CapabilityManager(Context context) {

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );

        initialize();
    }

    private void initialize() {

        if (!preferences.contains(
                KEY_CAPABILITIES
        )) {

            preferences.edit()
                    .putString(
                            KEY_CAPABILITIES,
                            "[]"
                    )
                    .apply();

            registerDefaultCapabilities();
        }
    }

    // =========================================================
    // DEFAULT CAPABILITIES
    // =========================================================

    private void registerDefaultCapabilities() {

        addCapability(
                "voice",
                "التعرف على صوت كمال وتحويل الكلام إلى أوامر"
        );

        addCapability(
                "text_commands",
                "فهم وتنفيذ الأوامر النصية"
        );

        addCapability(
                "text_to_speech",
                "التحدث مع كمال بالصوت"
        );

        addCapability(
                "memory",
                "حفظ واسترجاع المعلومات"
        );

        addCapability(
                "skills",
                "إدارة مهارات JARVIS"
        );

        addCapability(
                "evolution",
                "إدارة عملية تطور JARVIS"
        );

        addCapability(
                "web",
                "فتح والوصول إلى خدمات الإنترنت"
        );

        addCapability(
                "android_settings",
                "الوصول إلى إعدادات Android المتاحة"
        );

        addCapability(
                "application_launcher",
                "فتح التطبيقات والخدمات"
        );

        addCapability(
                "self_diagnosis",
                "فحص قدرات JARVIS وتحديد النواقص"
        );

        addCapability(
                "planning",
                "إنشاء أهداف وخطط للتطوير"
        );

        addCapability(
                "approval",
                "إدارة العمليات التي تحتاج موافقة كمال"
        );

        addCapability(
                "history",
                "تسجيل عمليات وتطور JARVIS"
        );
    }

    // =========================================================
    // ADD
    // =========================================================

    public boolean addCapability(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            if (hasCapability(
                    capabilities,
                    name
            )) {

                return false;
            }

            JSONObject capability =
                    new JSONObject();

            capability.put(
                    "name",
                    name.trim()
            );

            capability.put(
                    "description",
                    description == null
                            ? ""
                            : description.trim()
            );

            capability.put(
                    "status",
                    "available"
            );

            capability.put(
                    "success",
                    0
            );

            capability.put(
                    "failure",
                    0
            );

            capability.put(
                    "created_at",
                    now()
            );

            capability.put(
                    "updated_at",
                    now()
            );

            capabilities.put(
                    capability
            );

            saveCapabilities(
                    capabilities
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // REMOVE
    // =========================================================

    public boolean removeCapability(
            String name
    ) {

        if (name == null) {
            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            JSONArray updated =
                    new JSONArray();

            boolean removed = false;

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                if (capability
                        .getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    removed = true;
                    continue;
                }

                updated.put(
                        capability
                );
            }

            if (removed) {

                saveCapabilities(
                        updated
                );
            }

            return removed;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public boolean hasCapability(
            String name
    ) {

        if (name == null) {
            return false;
        }

        try {

            return hasCapability(
                    getCapabilities(),
                    name
            );

        } catch (Exception e) {

            return false;
        }
    }

    private boolean hasCapability(
            JSONArray capabilities,
            String name
    ) {

        try {

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                if (capability
                        .getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    // =========================================================
    // SET STATUS
    // =========================================================

    public boolean setStatus(
            String name,
            String status
    ) {

        if (name == null ||
                status == null) {

            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                if (capability
                        .getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    capability.put(
                            "status",
                            status
                    );

                    capability.put(
                            "updated_at",
                            now()
                    );

                    saveCapabilities(
                            capabilities
                    );

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    public void recordSuccess(
            String name
    ) {

        updateResult(
                name,
                true
        );
    }

    // =========================================================
    // FAILURE
    // =========================================================

    public void recordFailure(
            String name
    ) {

        updateResult(
                name,
                false
        );
    }

    private void updateResult(
            String name,
            boolean success
    ) {

        if (name == null) {
            return;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                if (capability
                        .getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    String key =
                            success
                                    ? "success"
                                    : "failure";

                    int value =
                            capability.optInt(
                                    key,
                                    0
                            );

                    capability.put(
                            key,
                            value + 1
                    );

                    capability.put(
                            "updated_at",
                            now()
                    );

                    saveCapabilities(
                            capabilities
                    );

                    return;
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // GET CAPABILITY
    // =========================================================

    public JSONObject getCapability(
            String name
    ) {

        if (name == null) {
            return null;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                if (capability
                        .getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    return capability;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =========================================================
    // REPORT
    // =========================================================

    public String getReport() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS CAPABILITY REGISTRY\n\n"
        );

        try {

            JSONArray capabilities =
                    getCapabilities();

            if (capabilities.length() == 0) {

                report.append(
                        "لا توجد قدرات مسجلة."
                );

                return report.toString();
            }

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities
                                .getJSONObject(i);

                report.append(
                        "━━━━━━━━━━━━━━\n"
                );

                report.append(
                        "القدرة: "
                );

                report.append(
                        capability.optString(
                                "name",
                                "Unknown"
                        )
                );

                report.append("\n");

                report.append(
                        "الحالة: "
                );

                report.append(
                        capability.optString(
                                "status",
                                "unknown"
                        )
                );

                report.append("\n");

                report.append(
                        "النجاح: "
                );

                report.append(
                        capability.optInt(
                                "success",
                                0
                        )
                );

                report.append("\n");

                report.append(
                        "الفشل: "
                );

                report.append(
                        capability.optInt(
                                "failure",
                                0
                        )
                );

                report.append("\n");
            }

        } catch (Exception e) {

            report.append(
                    "تعذر قراءة سجل القدرات."
            );
        }

        return report.toString();
    }

    // =========================================================
    // COUNT
    // =========================================================

    public int getCapabilityCount() {

        return getCapabilities()
                .length();
    }

    // =========================================================
    // EXPORT
    // =========================================================

    public String exportCapabilities() {

        return getCapabilities()
                .toString();
    }

    // =========================================================
    // STORAGE
    // =========================================================

    private JSONArray getCapabilities() {

        try {

            String data =
                    preferences.getString(
                            KEY_CAPABILITIES,
                            "[]"
                    );

            return new JSONArray(data);

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    private void saveCapabilities(
            JSONArray capabilities
    ) {

        preferences.edit()
                .putString(
                        KEY_CAPABILITIES,
                        capabilities.toString()
                )
                .apply();
    }

    // =========================================================
    // TIME
    // =========================================================

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }
}