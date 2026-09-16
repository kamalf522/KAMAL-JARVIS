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

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );

        initialize();
    }

    private void initialize() {

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

    public boolean addSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                    i < skills.length();
                    i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    return false;
                }
            }

            JSONObject skill =
                    new JSONObject();

            skill.put(
                    "name",
                    name.trim()
            );

            skill.put(
                    "description",
                    description == null
                            ? ""
                            : description.trim()
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

            saveSkills(skills);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // REMOVE SKILL
    // =========================================================

    public boolean removeSkill(
            String name
    ) {

        if (name == null) {
            return false;
        }

        try {

            JSONArray skills =
                    getSkills();

            JSONArray updated =
                    new JSONArray();

            boolean removed = false;

            for (int i = 0;
                    i < skills.length();
                    i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    removed = true;
                    continue;
                }

                updated.put(skill);
            }

            if (removed) {

                saveSkills(updated);
            }

            return removed;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    public boolean setEnabled(
            String name,
            boolean enabled
    ) {

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                    i < skills.length();
                    i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    skill.put(
                            "enabled",
                            enabled
                    );

                    skill.put(
                            "updated",
                            now()
                    );

                    saveSkills(skills);

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
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

    private void updateResult(
            String name,
            boolean success
    ) {

        if (name == null) {
            return;
        }

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                    i < skills.length();
                    i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

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

                    saveSkills(skills);

                    return;
                }
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // FIND SKILL
    // =========================================================

    public JSONObject getSkill(
            String name
    ) {

        if (name == null) {
            return null;
        }

        try {

            JSONArray skills =
                    getSkills();

            for (int i = 0;
                    i < skills.length();
                    i++) {

                JSONObject skill =
                        skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(
                                name.trim()
                        )) {

                    return skill;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =========================================================
    // CHECK SKILL
    // =========================================================

    public boolean hasSkill(
            String name
    ) {

        return getSkill(name) != null;
    }

    public boolean isEnabled(
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
    // LIST SKILLS
    // =========================================================

    public List<String> getSkillNames() {

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

                result.add(
                        skill.getString("name")
                );
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    // =========================================================
    // SKILL REPORT
    // =========================================================

    public String getSkillReport() {

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
                );

                report.append(name);

                report.append("\n");

                report.append(
                        "الحالة: "
                );

                report.append(
                        enabled
                                ? "مفعلة"
                                : "متوقفة"
                );

                report.append("\n");

                report.append(
                        "الوصف: "
                );

                report.append(description);

                report.append("\n");

                report.append(
                        "نجاح: "
                );

                report.append(success);

                report.append("\n");

                report.append(
                        "فشل: "
                );

                report.append(failure);

                report.append("\n");
            }

        } catch (Exception e) {

            report.append(
                    "حدث خطأ أثناء قراءة المهارات."
            );
        }

        return report.toString();
    }

    // =========================================================
    // EXPORT
    // =========================================================

    public String exportSkills() {

        return getSkills().toString();
    }

    // =========================================================
    // IMPORT
    // =========================================================

    public boolean importSkills(
            String json
    ) {

        if (json == null ||
                json.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray data =
                    new JSONArray(json);

            saveSkills(data);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // CLEAR
    // =========================================================

    public void clearSkills() {

        preferences.edit()
                .putString(
                        KEY_SKILLS,
                        "[]"
                )
                .apply();
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

            return new JSONArray(data);

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    private void saveSkills(
            JSONArray skills
    ) {

        preferences.edit()
                .putString(
                        KEY_SKILLS,
                        skills.toString()
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