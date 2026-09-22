package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SkillManager {

    private static final String PREFS =
            "jarvis_skill_manager";

    private static final String KEY_SKILLS =
            "skills";

    private final SharedPreferences preferences;

    public SkillManager(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "SkillManager context cannot be null"
            );
        }

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );

        initialize();
    }

    private synchronized void initialize() {

        if (!preferences.contains(KEY_SKILLS)) {

            preferences.edit()
                    .putString(
                            KEY_SKILLS,
                            "[]"
                    )
                    .apply();
        }
    }

    // =========================================================
    // ADD SKILL
    // =========================================================

    public synchronized boolean addSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null
                        ? ""
                        : description.trim();

        try {

            JSONArray skills =
                    getSkills();

            if (findSkillIndex(
                    skills,
                    cleanName
            ) >= 0) {

                return false;
            }

            JSONObject skill =
                    new JSONObject();

            skill.put(
                    "name",
                    cleanName
            );

            skill.put(
                    "description",
                    cleanDescription
            );

            skill.put(
                    "enabled",
                    true
            );

            skill.put(
                    "success",
                    0
            );

            skill.put(
                    "failure",
                    0
            );

            skill.put(
                    "created",
                    now()
            );

            skill.put(
                    "updated",
                    now()
            );

            skills.put(skill);

            return saveSkills(skills);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // REMOVE SKILL
    // =========================================================

    public synchronized boolean removeSkill(
            String name
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray skills =
                    getSkills();

            int index =
                    findSkillIndex(
                            skills,
                            name.trim()
                    );

            if (index < 0) {
                return false;
            }

            JSONArray updated =
                    new JSONArray();

            for (int i = 0;
                 i < skills.length();
                 i++) {

                if (i != index) {

                    updated.put(
                            skills.getJSONObject(i)
                    );
                }
            }

            return saveSkills(updated);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    public synchronized boolean setEnabled(
            String name,
            boolean enabled
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray skills =
                    getSkills();

            int index =
                    findSkillIndex(
                            skills,
                            name.trim()
                    );

            if (index < 0) {
                return false;
            }

            JSONObject skill =
                    skills.getJSONObject(index);

            skill.put(
                    "enabled",
                    enabled
            );

            skill.put(
                    "updated",
                    now()
            );

            return saveSkills(skills);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // RECORD SUCCESS
    // =========================================================

    public synchronized void recordSuccess(
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

    public synchronized void recordFailure(
            String name
    ) {

        updateResult(
                name,
                false
        );
    }

    private boolean updateResult(
            String name,
            boolean success
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray skills =
                    getSkills();

            int index =
                    findSkillIndex(
                            skills,
                            name.trim()
                    );

            if (index < 0) {
                return false;
            }

            JSONObject skill =
                    skills.getJSONObject(index);

            if (success) {

                int value =
                        skill.optInt(
                                "success",
                                0
                        );

                skill.put(
                        "success",
                        value + 1
                );

            } else {

                int value =
                        skill.optInt(
                                "failure",
                                0
                        );

                skill.put(
                        "failure",
                        value + 1
                );
            }

            skill.put(
                    "updated",
                    now()
            );

            return saveSkills(skills);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // FIND SKILL
    // =========================================================

    public synchronized JSONObject getSkill(
            String name
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return null;
        }

        try {

            JSONArray skills =
                    getSkills();

            int index =
                    findSkillIndex(
                            skills,
                            name.trim()
                    );

            if (index < 0) {
                return null;
            }

            return skills.getJSONObject(index);

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // CHECK SKILL
    // =========================================================

    public synchronized boolean hasSkill(
            String name
    ) {

        return getSkill(name) != null;
    }

    public synchronized boolean isEnabled(
            String name
    ) {

        JSONObject skill =
                getSkill(name);

        return skill != null &&
                skill.optBoolean(
                        "enabled",
                        false
                );
    }

    // =========================================================
    // SKILL NAMES
    // =========================================================

    public synchronized List<String>
    getSkillNames() {

        List<String> result =
                new ArrayList<>();

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                 i < skills.length();
                 i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                String name =
                        skill.optString(
                                "name",
                                ""
                        ).trim();

                if (!name.isEmpty()) {

                    result.add(name);
                }
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    // =========================================================
    // ENABLED SKILLS
    // =========================================================

    public synchronized List<String>
    getEnabledSkillNames() {

        List<String> result =
                new ArrayList<>();

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                 i < skills.length();
                 i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.optBoolean(
                        "enabled",
                        false
                )) {

                    String name =
                            skill.optString(
                                    "name",
                                    ""
                            ).trim();

                    if (!name.isEmpty()) {
                        result.add(name);
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    // =========================================================
    // SKILL COUNT
    // =========================================================

    public synchronized int getSkillCount() {

        return getSkillNames().size();
    }

    public synchronized int getEnabledSkillCount() {

        return getEnabledSkillNames().size();
    }

    // =========================================================
    // SUCCESS / FAILURE
    // =========================================================

    public synchronized int getSuccessCount(
            String name
    ) {

        JSONObject skill =
                getSkill(name);

        if (skill == null) {
            return 0;
        }

        return skill.optInt(
                "success",
                0
        );
    }

    public synchronized int getFailureCount(
            String name
    ) {

        JSONObject skill =
                getSkill(name);

        if (skill == null) {
            return 0;
        }

        return skill.optInt(
                "failure",
                0
        );
    }

    public synchronized double
    getSuccessRate(
            String name
    ) {

        JSONObject skill =
                getSkill(name);

        if (skill == null) {
            return 0.0;
        }

        int success =
                skill.optInt(
                        "success",
                        0
                );

        int failure =
                skill.optInt(
                        "failure",
                        0
                );

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
    // SKILL REPORT
    // =========================================================

    public synchronized String
    getSkillReport() {

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS SKILL SYSTEM\n\n"
        );

        try {

            JSONArray skills =
                    getSkills();

            if (skills.length() == 0) {

                report.append(
                        "لا توجد مهارات مسجلة بعد."
                );

                return report.toString();
            }

            report.append(
                    "إجمالي المهارات: "
            )
                    .append(
                            skills.length()
                    )
                    .append("\n");

            report.append(
                    "المفعلة: "
            )
                    .append(
                            getEnabledSkillCount()
                    )
                    .append("\n\n");

            for (int i = 0;
                 i < skills.length();
                 i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                String name =
                        skill.optString(
                                "name",
                                "Unknown"
                        );

                String description =
                        skill.optString(
                                "description",
                                ""
                        );

                boolean enabled =
                        skill.optBoolean(
                                "enabled",
                                false
                        );

                int success =
                        skill.optInt(
                                "success",
                                0
                        );

                int failure =
                        skill.optInt(
                                "failure",
                                0
                        );

                report.append(
                        "━━━━━━━━━━━━━━\n"
                );

                report.append(
                        "المهارة: "
                )
                        .append(name)
                        .append("\n");

                report.append(
                        "الحالة: "
                )
                        .append(
                                enabled
                                        ? "مفعلة"
                                        : "متوقفة"
                        )
                        .append("\n");

                report.append(
                        "الوصف: "
                )
                        .append(description)
                        .append("\n");

                report.append(
                        "نجاح: "
                )
                        .append(success)
                        .append("\n");

                report.append(
                        "فشل: "
                )
                        .append(failure)
                        .append("\n");

                report.append(
                        "نسبة النجاح: "
                )
                        .append(
                                String.format(
                                        Locale.US,
                                        "%.1f%%",
                                        getSuccessRate(
                                                name
                                        )
                                )
                        )
                        .append("\n");
            }

        } catch (Exception e) {

            report.append(
                    "حدث خطأ أثناء قراءة المهارات."
            );
        }

        return report.toString();
    }

    // =========================================================
    // COMPATIBILITY REPORT
    // =========================================================

    public String getReport() {

        return getSkillReport();
    }

    // =========================================================
    // EXPORT
    // =========================================================

    public synchronized String exportSkills() {

        return getSkills().toString();
    }

    // =========================================================
    // IMPORT
    // =========================================================

    public synchronized boolean importSkills(
            String json
    ) {

        if (json == null ||
                json.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray data =
                    new JSONArray(json);

            JSONArray cleanData =
                    new JSONArray();

            for (int i = 0;
                 i < data.length();
                 i++) {

                JSONObject original =
                        data.optJSONObject(i);

                if (original == null) {
                    continue;
                }

                String name =
                        original.optString(
                                "name",
                                ""
                        ).trim();

                if (name.isEmpty()) {
                    continue;
                }

                JSONObject skill =
                        new JSONObject();

                skill.put(
                        "name",
                        name
                );

                skill.put(
                        "description",
                        original.optString(
                                "description",
                                ""
                        )
                );

                skill.put(
                        "enabled",
                        original.optBoolean(
                                "enabled",
                                true
                        )
                );

                skill.put(
                        "success",
                        Math.max(
                                0,
                                original.optInt(
                                        "success",
                                        0
                                )
                        )
                );

                skill.put(
                        "failure",
                        Math.max(
                                0,
                                original.optInt(
                                        "failure",
                                        0
                                )
                        )
                );

                skill.put(
                        "created",
                        original.optString(
                                "created",
                                now()
                        )
                );

                skill.put(
                        "updated",
                        now()
                );

                cleanData.put(skill);
            }

            return saveSkills(cleanData);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // CLEAR
    // =========================================================

    public synchronized void clearSkills() {

        preferences.edit()
                .putString(
                        KEY_SKILLS,
                        "[]"
                )
                .apply();
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            JSONArray skills =
                    getSkills();

            return skills != null;

        } catch (Exception e) {

            return false;
        }
    }

    public synchronized String getStatus() {

        if (!isHealthy()) {

            return
                    "Skill Manager: ERROR ⚠";
        }

        return
                "Skill Manager: ONLINE ✓\n"
                + "Skills: "
                + getSkillCount()
                + "\n"
                + "Enabled: "
                + getEnabledSkillCount();
    }

    // =========================================================
    // STORAGE
    // =========================================================

    private JSONArray getSkills() {

        try {

            String data =
                    preferences.getString(
                            KEY_SKILLS,
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

    private boolean saveSkills(
            JSONArray skills
    ) {

        if (skills == null) {
            return false;
        }

        return preferences.edit()
                .putString(
                        KEY_SKILLS,
                        skills.toString()
                )
                .commit();
    }

    // =========================================================
    // FIND INDEX
    // =========================================================

    private int findSkillIndex(
            JSONArray skills,
            String name
    ) {

        if (skills == null ||
                name == null) {

            return -1;
        }

        String wanted =
                name.trim();

        if (wanted.isEmpty()) {
            return -1;
        }

        for (int i = 0;
             i < skills.length();
             i++) {

            try {

                JSONObject skill =
                        skills.getJSONObject(i);

                String current =
                        skill.optString(
                                "name",
                                ""
                        ).trim();

                if (current.equalsIgnoreCase(
                        wanted
                )) {

                    return i;
                }

            } catch (Exception ignored) {
            }
        }

        return -1;
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