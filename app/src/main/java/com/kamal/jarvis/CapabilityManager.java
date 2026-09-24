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

    private static final String KEY_VERSION =
            "version";

    private final SharedPreferences preferences;

    public CapabilityManager(Context context) {

        Context safeContext =
                context != null
                        ? context.getApplicationContext()
                        : null;

        if (safeContext == null) {

            preferences = null;

        } else {

            preferences =
                    safeContext.getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                    );

            initialize();
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initialize() {

        if (preferences == null) {
            return;
        }

        try {

            if (!preferences.contains(KEY_CAPABILITIES)) {

                preferences.edit()
                        .putString(
                                KEY_CAPABILITIES,
                                "[]"
                        )
                        .putString(
                                KEY_VERSION,
                                "3.0"
                        )
                        .commit();

                registerDefaultCapabilities();

            } else if (!preferences.contains(KEY_VERSION)) {

                preferences.edit()
                        .putString(
                                KEY_VERSION,
                                "3.0"
                        )
                        .commit();
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // DEFAULT CAPABILITIES
    // =========================================================

    private void registerDefaultCapabilities() {

        addCapability(
                "voice",
                "التعرف على الصوت وتحويل الكلام إلى أوامر"
        );

        addCapability(
                "text_commands",
                "فهم وتنفيذ الأوامر النصية"
        );

        addCapability(
                "text_to_speech",
                "التحدث بالصوت"
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
                "الوصول إلى خدمات الإنترنت"
        );

        addCapability(
                "android_settings",
                "الوصول إلى إعدادات Android"
        );

        addCapability(
                "application_launcher",
                "فتح التطبيقات والخدمات"
        );

        addCapability(
                "self_diagnosis",
                "تشخيص حالة JARVIS"
        );

        addCapability(
                "planning",
                "إنشاء الأهداف والخطط"
        );

        addCapability(
                "approval",
                "إدارة العمليات التي تحتاج موافقة"
        );

        addCapability(
                "history",
                "تسجيل العمليات"
        );

        addCapability(
                "tasks",
                "إدارة المهام"
        );

        addCapability(
                "reminders",
                "إنشاء وإدارة التذكيرات"
        );

        addCapability(
                "notifications",
                "قراءة وتحليل الإشعارات"
        );

        addCapability(
                "screen_intelligence",
                "تحليل محتوى الشاشة"
        );

        addCapability(
                "android_control",
                "التحكم في وظائف Android"
        );

        addCapability(
                "automation",
                "تنفيذ سلاسل أوامر متعددة الخطوات"
        );

        addCapability(
                "learning",
                "تعلم معلومات وأوامر جديدة"
        );

        addCapability(
                "knowledge",
                "إدارة المعرفة والمعلومات"
        );

        addCapability(
                "self_builder",
                "إدارة مساحة التطوير والبناء الذاتي"
        );
    }

    // =========================================================
    // ADD CAPABILITY
    // =========================================================

    public synchronized boolean addCapability(
            String name,
            String description
    ) {

        if (preferences == null ||
                name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        String cleanName =
                normalizeName(name);

        if (cleanName.isEmpty()) {
            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            int existingIndex =
                    findCapabilityIndex(
                            capabilities,
                            cleanName
                    );

            if (existingIndex >= 0) {
                return false;
            }

            JSONObject capability =
                    new JSONObject();

            String timestamp =
                    now();

            capability.put(
                    "name",
                    cleanName
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
                    "usage",
                    0
            );

            capability.put(
                    "created_at",
                    timestamp
            );

            capability.put(
                    "updated_at",
                    timestamp
            );

            capability.put(
                    "last_success",
                    ""
            );

            capability.put(
                    "last_failure",
                    ""
            );

            capabilities.put(
                    capability
            );

            return saveCapabilities(
                    capabilities
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // REMOVE CAPABILITY
    // =========================================================

    public synchronized boolean removeCapability(
            String name
    ) {

        if (preferences == null ||
                name == null) {

            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            JSONArray updated =
                    new JSONArray();

            String cleanName =
                    normalizeName(name);

            boolean removed = false;

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities.optJSONObject(i);

                if (capability == null) {
                    continue;
                }

                String currentName =
                        normalizeName(
                                capability.optString(
                                        "name",
                                        ""
                                )
                        );

                if (currentName.equals(cleanName)) {

                    removed = true;
                    continue;
                }

                updated.put(
                        capability
                );
            }

            if (!removed) {
                return false;
            }

            return saveCapabilities(
                    updated
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // HAS CAPABILITY
    // =========================================================

    public synchronized boolean hasCapability(
            String name
    ) {

        if (name == null) {
            return false;
        }

        return findCapabilityIndex(
                getCapabilities(),
                normalizeName(name)
        ) >= 0;
    }

    // =========================================================
    // FIND CAPABILITY
    // =========================================================

    private int findCapabilityIndex(
            JSONArray capabilities,
            String name
    ) {

        if (capabilities == null ||
                name == null ||
                name.trim().isEmpty()) {

            return -1;
        }

        try {

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                JSONObject capability =
                        capabilities.optJSONObject(i);

                if (capability == null) {
                    continue;
                }

                String currentName =
                        normalizeName(
                                capability.optString(
                                        "name",
                                        ""
                                )
                        );

                if (currentName.equals(name)) {
                    return i;
                }
            }

        } catch (Exception ignored) {
        }

        return -1;
    }

    // =========================================================
    // GET CAPABILITY
    // =========================================================

    public synchronized JSONObject getCapability(
            String name
    ) {

        if (name == null) {
            return null;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            int index =
                    findCapabilityIndex(
                            capabilities,
                            normalizeName(name)
                    );

            if (index >= 0) {

                return capabilities.optJSONObject(
                        index
                );
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =========================================================
    // SET STATUS
    // =========================================================

    public synchronized boolean setStatus(
            String name,
            String status
    ) {

        if (preferences == null ||
                name == null ||
                status == null) {

            return false;
        }

        String cleanStatus =
                status.trim();

        if (cleanStatus.isEmpty()) {
            return false;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            int index =
                    findCapabilityIndex(
                            capabilities,
                            normalizeName(name)
                    );

            if (index < 0) {
                return false;
            }

            JSONObject capability =
                    capabilities.optJSONObject(index);

            if (capability == null) {
                return false;
            }

            capability.put(
                    "status",
                    cleanStatus
            );

            capability.put(
                    "updated_at",
                    now()
            );

            return saveCapabilities(
                    capabilities
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // ENABLE
    // =========================================================

    public boolean enableCapability(
            String name
    ) {

        return setStatus(
                name,
                "available"
        );
    }

    // =========================================================
    // DISABLE
    // =========================================================

    public boolean disableCapability(
            String name
    ) {

        return setStatus(
                name,
                "disabled"
        );
    }

    // =========================================================
    // RECORD SUCCESS
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
    // RECORD FAILURE
    // =========================================================

    public void recordFailure(
            String name
    ) {

        updateResult(
                name,
                false
        );
    }

    // =========================================================
    // RECORD USAGE
    // =========================================================

    public synchronized void recordUsage(
            String name
    ) {

        if (preferences == null ||
                name == null) {

            return;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            int index =
                    findCapabilityIndex(
                            capabilities,
                            normalizeName(name)
                    );

            if (index < 0) {
                return;
            }

            JSONObject capability =
                    capabilities.optJSONObject(index);

            if (capability == null) {
                return;
            }

            int usage =
                    capability.optInt(
                            "usage",
                            0
                    );

            capability.put(
                    "usage",
                    usage + 1
            );

            capability.put(
                    "updated_at",
                    now()
            );

            saveCapabilities(
                    capabilities
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // UPDATE RESULT
    // =========================================================

    private synchronized void updateResult(
            String name,
            boolean success
    ) {

        if (preferences == null ||
                name == null) {

            return;
        }

        try {

            JSONArray capabilities =
                    getCapabilities();

            int index =
                    findCapabilityIndex(
                            capabilities,
                            normalizeName(name)
                    );

            if (index < 0) {
                return;
            }

            JSONObject capability =
                    capabilities.optJSONObject(index);

            if (capability == null) {
                return;
            }

            String timestamp =
                    now();

            int usage =
                    capability.optInt(
                            "usage",
                            0
                    );

            int successCount =
                    capability.optInt(
                            "success",
                            0
                    );

            int failureCount =
                    capability.optInt(
                            "failure",
                            0
                    );

            capability.put(
                    "usage",
                    usage + 1
            );

            if (success) {

                capability.put(
                        "success",
                        successCount + 1
                );

                capability.put(
                        "last_success",
                        timestamp
                );

            } else {

                capability.put(
                        "failure",
                        failureCount + 1
                );

                capability.put(
                        "last_failure",
                        timestamp
                );
            }

            capability.put(
                    "updated_at",
                    timestamp
            );

            saveCapabilities(
                    capabilities
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SUCCESS COUNT
    // =========================================================

    public synchronized int getSuccessCount(
            String name
    ) {

        JSONObject capability =
                getCapability(name);

        if (capability == null) {
            return 0;
        }

        return capability.optInt(
                "success",
                0
        );
    }

    // =========================================================
    // FAILURE COUNT
    // =========================================================

    public synchronized int getFailureCount(
            String name
    ) {

        JSONObject capability =
                getCapability(name);

        if (capability == null) {
            return 0;
        }

        return capability.optInt(
                "failure",
                0
        );
    }

    // =========================================================
    // USAGE COUNT
    // =========================================================

    public synchronized int getUsageCount(
            String name
    ) {

        JSONObject capability =
                getCapability(name);

        if (capability == null) {
            return 0;
        }

        return capability.optInt(
                "usage",
                0
        );
    }

    // =========================================================
    // SUCCESS RATE
    // =========================================================

    public synchronized double getSuccessRate(
            String name
    ) {

        int success =
                getSuccessCount(name);

        int failure =
                getFailureCount(name);

        int total =
                success + failure;

        if (total <= 0) {
            return 0.0;
        }

        return
                ((double) success / total)
                        * 100.0;
    }

    // =========================================================
    // CAPABILITY COUNT
    // =========================================================

    public synchronized int getCapabilityCount() {

        return getCapabilities().length();
    }

    // =========================================================
    // COMPATIBILITY
    // =========================================================

    public int getCount() {

        return getCapabilityCount();
    }

    // =========================================================
    // CAPABILITY NAMES
    // =========================================================

    public synchronized String getCapabilityNames() {

        JSONArray capabilities =
                getCapabilities();

        if (capabilities.length() == 0) {

            return "لا توجد قدرات مسجلة.";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
                i < capabilities.length();
                i++) {

            JSONObject capability =
                    capabilities.optJSONObject(i);

            if (capability == null) {
                continue;
            }

            result.append(i + 1)
                    .append(". ")
                    .append(
                            capability.optString(
                                    "name",
                                    "Unknown"
                            )
                    )
                    .append(" — ")
                    .append(
                            capability.optString(
                                    "status",
                                    "unknown"
                            )
                    )
                    .append("\n");
        }

        return result.toString().trim();
    }

    // =========================================================
    // REPORT
    // =========================================================

    public synchronized String getReport() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS CAPABILITY REGISTRY\n"
        );

        report.append(
                "============================\n\n"
        );

        report.append(
                "Version: "
        );

        report.append(
                getVersion()
        );

        report.append("\n");

        report.append(
                "Total: "
        );

        report.append(
                getCapabilityCount()
        );

        report.append("\n\n");

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
                    capabilities.optJSONObject(i);

            if (capability == null) {
                continue;
            }

            String name =
                    capability.optString(
                            "name",
                            "Unknown"
                    );

            report.append(
                    "━━━━━━━━━━━━━━\n"
            );

            report.append(
                    "القدرة: "
            );

            report.append(name);

            report.append("\nالوصف: ");

            report.append(
                    capability.optString(
                            "description",
                            ""
                    )
            );

            report.append("\nالحالة: ");

            report.append(
                    capability.optString(
                            "status",
                            "unknown"
                    )
            );

            report.append("\nالاستخدام: ");

            report.append(
                    capability.optInt(
                            "usage",
                            0
                    )
            );

            report.append("\nالنجاح: ");

            report.append(
                    capability.optInt(
                            "success",
                            0
                    )
            );

            report.append("\nالفشل: ");

            report.append(
                    capability.optInt(
                            "failure",
                            0
                    )
            );

            report.append("\nمعدل النجاح: ");

            report.append(
                    formatRate(
                            getSuccessRate(name)
                    )
            );

            report.append("\n");
        }

        return report.toString();
    }

    // =========================================================
    // EXPORT
    // =========================================================

    public synchronized String exportCapabilities() {

        return getCapabilities().toString();
    }

    // =========================================================
    // IMPORT
    // =========================================================

    public synchronized boolean importCapabilities(
            String data
    ) {

        if (preferences == null ||
                data == null ||
                data.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray imported =
                    new JSONArray(data);

            JSONArray validated =
                    new JSONArray();

            for (int i = 0;
                    i < imported.length();
                    i++) {

                JSONObject source =
                        imported.optJSONObject(i);

                if (source == null) {
                    continue;
                }

                String name =
                        normalizeName(
                                source.optString(
                                        "name",
                                        ""
                                )
                        );

                if (name.isEmpty()) {
                    continue;
                }

                if (findCapabilityIndex(
                        validated,
                        name
                ) >= 0) {

                    continue;
                }

                JSONObject capability =
                        new JSONObject();

                capability.put(
                        "name",
                        name
                );

                capability.put(
                        "description",
                        source.optString(
                                "description",
                                ""
                        )
                );

                capability.put(
                        "status",
                        source.optString(
                                "status",
                                "available"
                        )
                );

                capability.put(
                        "success",
                        Math.max(
                                0,
                                source.optInt(
                                        "success",
                                        0
                                )
                        )
                );

                capability.put(
                        "failure",
                        Math.max(
                                0,
                                source.optInt(
                                        "failure",
                                        0
                                )
                        )
                );

                capability.put(
                        "usage",
                        Math.max(
                                0,
                                source.optInt(
                                        "usage",
                                        0
                                )
                        )
                );

                capability.put(
                        "created_at",
                        source.optString(
                                "created_at",
                                now()
                        )
                );

                capability.put(
                        "updated_at",
                        now()
                );

                capability.put(
                        "last_success",
                        source.optString(
                                "last_success",
                                ""
                        )
                );

                capability.put(
                        "last_failure",
                        source.optString(
                                "last_failure",
                                ""
                        )
                );

                validated.put(
                        capability
                );
            }

            return saveCapabilities(
                    validated
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // VERSION
    // =========================================================

    public synchronized String getVersion() {

        if (preferences == null) {
            return "0.0";
        }

        return preferences.getString(
                KEY_VERSION,
                "3.0"
        );
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            if (preferences == null) {
                return false;
            }

            JSONArray capabilities =
                    getCapabilities();

            if (capabilities == null) {
                return false;
            }

            for (int i = 0;
                    i < capabilities.length();
                    i++) {

                if (capabilities.optJSONObject(i)
                        == null) {

                    return false;
                }
            }

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        if (!isHealthy()) {

            return
                    "Capability Manager: ERROR ⚠";
        }

        return
                "Capability Manager: ONLINE ✓\n"
                        + "Version: "
                        + getVersion()
                        + "\nCapabilities: "
                        + getCapabilityCount();
    }

    // =========================================================
    // INTERNAL GET
    // =========================================================

    private JSONArray getCapabilities() {

        if (preferences == null) {

            return new JSONArray();
        }

        try {

            String data =
                    preferences.getString(
                            KEY_CAPABILITIES,
                            "[]"
                    );

            if (data == null ||
                    data.trim().isEmpty()) {

                return new JSONArray();
            }

            return new JSONArray(data);

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    // =========================================================
    // INTERNAL SAVE
    // =========================================================

    private boolean saveCapabilities(
            JSONArray capabilities
    ) {

        if (preferences == null ||
                capabilities == null) {

            return false;
        }

        try {

            return preferences.edit()
                    .putString(
                            KEY_CAPABILITIES,
                            capabilities.toString()
                    )
                    .commit();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // NORMALIZE NAME
    // =========================================================

    private String normalizeName(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replace("ؤ", "و")
                .replace("ئ", "ي");
    }

    // =========================================================
    // CURRENT TIME
    // =========================================================

    private String now() {

        try {

            return new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.US
            ).format(
                    new Date()
            );

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // FORMAT RATE
    // =========================================================

    private String formatRate(
            double rate
    ) {

        return String.format(
                Locale.US,
                "%.1f%%",
                rate
        );
    }
}