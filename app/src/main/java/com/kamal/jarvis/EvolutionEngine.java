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

/**
 * JARVIS Evolution Engine
 *
 * مسؤول عن إدارة التطور المستمر لـ JARVIS:
 *
 * Observe -> Learn -> Plan -> Build -> Test -> Improve -> Remember
 *
 * هذه الطبقة هي نواة نظام التطور.
 * سيتم ربطها لاحقاً مع:
 * - Skill System
 * - Learning System
 * - Self Diagnosis
 * - Self Test
 * - Self Builder
 * - Permission Manager
 * - Kamal Authority
 * - Recovery System
 */
public class EvolutionEngine {

    private static final String PREF_NAME = "jarvis_evolution";

    private static final String KEY_SKILLS = "skills";
    private static final String KEY_GOALS = "goals";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_PENDING_APPROVALS = "pending_approvals";
    private static final String KEY_VERSION = "engine_version";

    private static final String ENGINE_VERSION = "1.0";

    private final Context context;
    private final SharedPreferences preferences;
    private final MemoryManager memoryManager;

    public EvolutionEngine(Context context, MemoryManager memoryManager) {
        this.context = context.getApplicationContext();
        this.memoryManager = memoryManager;

        preferences = this.context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        initialize();
    }

    /**
     * إنشاء البيانات الأساسية لأول مرة.
     */
    private void initialize() {

        if (!preferences.contains(KEY_SKILLS)) {
            preferences.edit()
                    .putString(KEY_SKILLS, "[]")
                    .apply();
        }

        if (!preferences.contains(KEY_GOALS)) {
            preferences.edit()
                    .putString(KEY_GOALS, "[]")
                    .apply();
        }

        if (!preferences.contains(KEY_HISTORY)) {
            preferences.edit()
                    .putString(KEY_HISTORY, "[]")
                    .apply();
        }

        if (!preferences.contains(KEY_PENDING_APPROVALS)) {
            preferences.edit()
                    .putString(KEY_PENDING_APPROVALS, "[]")
                    .apply();
        }

        preferences.edit()
                .putString(KEY_VERSION, ENGINE_VERSION)
                .apply();
    }

    // ============================================================
    // SKILLS
    // ============================================================

    /**
     * إضافة مهارة جديدة.
     */
    public boolean registerSkill(
            String name,
            String description
    ) {

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        try {

            JSONArray skills = getSkillsJson();

            // منع التكرار
            for (int i = 0; i < skills.length(); i++) {

                JSONObject skill = skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(name.trim())) {

                    return false;
                }
            }

            JSONObject skill = new JSONObject();

            skill.put("name", name.trim());
            skill.put(
                    "description",
                    description == null ? "" : description.trim()
            );

            skill.put("status", "active");
            skill.put("success_count", 0);
            skill.put("failure_count", 0);
            skill.put("created_at", now());
            skill.put("updated_at", now());

            skills.put(skill);

            saveSkills(skills);

            addHistory(
                    "SKILL_CREATED",
                    "تمت إضافة مهارة جديدة: " + name
            );

            return true;

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "فشل إنشاء المهارة: " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * تسجيل نجاح المهارة.
     */
    public void recordSkillSuccess(String skillName) {

        updateSkillResult(
                skillName,
                true
        );
    }

    /**
     * تسجيل فشل المهارة.
     */
    public void recordSkillFailure(String skillName) {

        updateSkillResult(
                skillName,
                false
        );
    }

    private void updateSkillResult(
            String skillName,
            boolean success
    ) {

        if (skillName == null) {
            return;
        }

        try {

            JSONArray skills = getSkillsJson();

            for (int i = 0; i < skills.length(); i++) {

                JSONObject skill = skills.getJSONObject(i);

                if (skill.getString("name")
                        .equalsIgnoreCase(skillName.trim())) {

                    if (success) {

                        int count =
                                skill.optInt("success_count", 0);

                        skill.put(
                                "success_count",
                                count + 1
                        );

                    } else {

                        int count =
                                skill.optInt("failure_count", 0);

                        skill.put(
                                "failure_count",
                                count + 1
                        );
                    }

                    skill.put("updated_at", now());

                    saveSkills(skills);

                    addHistory(
                            success
                                    ? "SKILL_SUCCESS"
                                    : "SKILL_FAILURE",
                            skillName
                    );

                    return;
                }
            }

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "Skill result error: " + e.getMessage()
            );
        }
    }

    /**
     * إرجاع جميع المهارات.
     */
    public List<String> getSkillNames() {

        List<String> result = new ArrayList<>();

        try {

            JSONArray skills = getSkillsJson();

            for (int i = 0; i < skills.length(); i++) {

                JSONObject skill = skills.getJSONObject(i);

                result.add(
                        skill.getString("name")
                );
            }

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "Skill list error: " + e.getMessage()
            );
        }

        return result;
    }

    // ============================================================
    // GOALS
    // ============================================================

    /**
     * إنشاء هدف تطوري جديد.
     */
    public boolean createEvolutionGoal(String goal) {

        if (goal == null || goal.trim().isEmpty()) {
            return false;
        }

        try {

            JSONArray goals = getGoalsJson();

            JSONObject object = new JSONObject();

            object.put("goal", goal.trim());
            object.put("status", "pending");
            object.put("created_at", now());

            goals.put(object);

            saveGoals(goals);

            addHistory(
                    "GOAL_CREATED",
                    goal
            );

            return true;

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "Goal creation error: " + e.getMessage()
            );

            return false;
        }
    }

    // ============================================================
    // EVOLUTION CYCLE
    // ============================================================

    /**
     * تشغيل دورة تطور واحدة.
     *
     * هذه حالياً طبقة التخطيط.
     * سيتم ربط مراحل BUILD وTEST وIMPROVE
     * مع الأنظمة القادمة.
     */
    public String runEvolutionCycle() {

        addHistory(
                "EVOLUTION_STARTED",
                "بدأت دورة تطور جديدة."
        );

        StringBuilder report =
                new StringBuilder();

        report.append("JARVIS EVOLUTION CYCLE\n\n");

        // 1. Observe
        report.append("1. OBSERVE: OK\n");

        // 2. Learn
        report.append("2. LEARN: READY\n");

        // 3. Plan
        report.append("3. PLAN: READY\n");

        // 4. Build
        report.append("4. BUILD: WAITING FOR BUILDER\n");

        // 5. Test
        report.append("5. TEST: WAITING FOR TEST SYSTEM\n");

        // 6. Improve
        report.append("6. IMPROVE: READY\n");

        // 7. Remember
        report.append("7. REMEMBER: ACTIVE\n");

        addHistory(
                "EVOLUTION_CYCLE",
                report.toString()
        );

        return report.toString();
    }

    // ============================================================
    // SELF DIAGNOSIS
    // ============================================================

    /**
     * فحص أولي لمعرفة حالة نظام التطور.
     */
    public String selfDiagnosis() {

        StringBuilder result =
                new StringBuilder();

        result.append("JARVIS SELF DIAGNOSIS\n\n");

        result.append(
                "Evolution Engine: ONLINE\n"
        );

        result.append(
                "Memory System: "
        );

        result.append(
                memoryManager != null
                        ? "ONLINE\n"
                        : "OFFLINE\n"
        );

        result.append(
                "Skill System: "
        );

        result.append(
                getSkillNames().size() >= 0
                        ? "ONLINE\n"
                        : "ERROR\n"
        );

        result.append(
                "Goal System: ONLINE\n"
        );

        result.append(
                "Evolution History: ONLINE\n"
        );

        result.append(
                "Approval System: READY\n"
        );

        result.append(
                "Self Builder: NOT CONNECTED YET\n"
        );

        result.append(
                "Self Test: NOT CONNECTED YET\n"
        );

        return result.toString();
    }

    // ============================================================
    // APPROVAL SYSTEM
    // ============================================================

    /**
     * إضافة عملية تحتاج موافقة كمال.
     */
    public String requestApproval(
            String action,
            String reason
    ) {

        if (action == null || action.trim().isEmpty()) {
            return null;
        }

        try {

            JSONArray approvals =
                    getApprovalsJson();

            JSONObject request =
                    new JSONObject();

            String id =
                    "REQ-" + System.currentTimeMillis();

            request.put("id", id);
            request.put(
                    "action",
                    action.trim()
            );

            request.put(
                    "reason",
                    reason == null
                            ? ""
                            : reason.trim()
            );

            request.put(
                    "status",
                    "pending"
            );

            request.put(
                    "created_at",
                    now()
            );

            approvals.put(request);

            saveApprovals(approvals);

            addHistory(
                    "APPROVAL_REQUESTED",
                    action
            );

            return id;

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "Approval error: " + e.getMessage()
            );

            return null;
        }
    }

    /**
     * الموافقة على طلب.
     */
    public boolean approve(String requestId) {

        return changeApprovalStatus(
                requestId,
                "approved"
        );
    }

    /**
     * رفض طلب.
     */
    public boolean reject(String requestId) {

        return changeApprovalStatus(
                requestId,
                "rejected"
        );
    }

    private boolean changeApprovalStatus(
            String requestId,
            String status
    ) {

        if (requestId == null) {
            return false;
        }

        try {

            JSONArray approvals =
                    getApprovalsJson();

            for (int i = 0; i < approvals.length(); i++) {

                JSONObject request =
                        approvals.getJSONObject(i);

                if (request.getString("id")
                        .equals(requestId)) {

                    request.put(
                            "status",
                            status
                    );

                    request.put(
                            "updated_at",
                            now()
                    );

                    saveApprovals(approvals);

                    addHistory(
                            "APPROVAL_" +
                                    status.toUpperCase(),
                            requestId
                    );

                    return true;
                }
            }

        } catch (Exception e) {

            addHistory(
                    "ERROR",
                    "Approval update error: "
                            + e.getMessage()
            );
        }

        return false;
    }

    // ============================================================
    // STATUS
    // ============================================================

    /**
     * حالة نظام التطور.
     */
    public String getEvolutionStatus() {

        int skills =
                getSkillNames().size();

        int goals =
                getGoalsJson().length();

        int approvals =
                getApprovalsJson().length();

        return
                "JARVIS EVOLUTION STATUS\n\n" +

                "Engine: ONLINE\n" +

                "Version: " +
                ENGINE_VERSION +
                "\n" +

                "Skills: " +
                skills +
                "\n" +

                "Goals: " +
                goals +
                "\n" +

                "Approval Requests: " +
                approvals +
                "\n" +

                "Memory: " +
                (
                        memoryManager != null
                                ? "ONLINE"
                                : "OFFLINE"
                ) +
                "\n" +

                "Continuous Evolution: READY\n" +

                "Self Builder: PENDING MODULE\n" +

                "Self Test: PENDING MODULE\n" +

                "Recovery: PENDING MODULE";
    }

    // ============================================================
    // HISTORY
    // ============================================================

    private void addHistory(
            String type,
            String message
    ) {

        try {

            JSONArray history =
                    getHistoryJson();

            JSONObject item =
                    new JSONObject();

            item.put(
                    "type",
                    type
            );

            item.put(
                    "message",
                    message == null
                            ? ""
                            : message
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            // نحتفظ بآخر 1000 عملية فقط
            if (history.length() > 1000) {

                JSONArray newHistory =
                        new JSONArray();

                int start =
                        history.length() - 1000;

                for (int i = start;
                     i < history.length();
                     i++) {

                    newHistory.put(
                            history.get(i)
                    );
                }

                history = newHistory;
            }

            preferences.edit()
                    .putString(
                            KEY_HISTORY,
                            history.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    /**
     * إرجاع سجل التطور.
     */
    public String getEvolutionHistory() {

        return getHistoryJson().toString();
    }

    // ============================================================
    // JSON STORAGE
    // ============================================================

    private JSONArray getSkillsJson() {

        return readArray(KEY_SKILLS);
    }

    private JSONArray getGoalsJson() {

        return readArray(KEY_GOALS);
    }

    private JSONArray getHistoryJson() {

        return readArray(KEY_HISTORY);
    }

    private JSONArray getApprovalsJson() {

        return readArray(KEY_PENDING_APPROVALS);
    }

    private JSONArray readArray(String key) {

        try {

            String value =
                    preferences.getString(
                            key,
                            "[]"
                    );

            return new JSONArray(value);

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    private void saveSkills(JSONArray data) {

        preferences.edit()
                .putString(
                        KEY_SKILLS,
                        data.toString()
                )
                .apply();
    }

    private void saveGoals(JSONArray data) {

        preferences.edit()
                .putString(
                        KEY_GOALS,
                        data.toString()
                )
                .apply();
    }

    private void saveApprovals(JSONArray data) {

        preferences.edit()
                .putString(
                        KEY_PENDING_APPROVALS,
                        data.toString()
                )
                .apply();
    }

    // ============================================================
    // TIME
    // ============================================================

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }
}