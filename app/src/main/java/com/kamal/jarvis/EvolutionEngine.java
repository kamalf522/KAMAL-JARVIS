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

public class EvolutionEngine {

    private static final String PREF_NAME =
            "jarvis_evolution";

    private static final String KEY_GOALS =
            "goals";

    private static final String KEY_HISTORY =
            "history";

    private static final String KEY_APPROVALS =
            "pending_approvals";

    private static final String KEY_VERSION =
            "engine_version";

    private static final String ENGINE_VERSION =
            "2.0";

    private final Context context;
    private final SharedPreferences preferences;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;

    public EvolutionEngine(
            Context context,
            MemoryManager memoryManager
    ) {

        this.context =
                context.getApplicationContext();

        this.memoryManager =
                memoryManager;

        this.skillManager =
                new SkillManager(
                        this.context
                );

        preferences =
                this.context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        initialize();
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    private void initialize() {

        if (!preferences.contains(KEY_GOALS)) {

            preferences.edit()
                    .putString(
                            KEY_GOALS,
                            "[]"
                    )
                    .apply();
        }

        if (!preferences.contains(KEY_HISTORY)) {

            preferences.edit()
                    .putString(
                            KEY_HISTORY,
                            "[]"
                    )
                    .apply();
        }

        if (!preferences.contains(KEY_APPROVALS)) {

            preferences.edit()
                    .putString(
                            KEY_APPROVALS,
                            "[]"
                    )
                    .apply();
        }

        preferences.edit()
                .putString(
                        KEY_VERSION,
                        ENGINE_VERSION
                )
                .apply();
    }

    // =========================================================
    // SKILLS
    // =========================================================

    public boolean registerSkill(
            String name,
            String description
    ) {

        boolean added =
                skillManager.addSkill(
                        name,
                        description
                );

        if (added) {

            addHistory(
                    "SKILL_CREATED",
                    name
            );
        }

        return added;
    }

    public boolean removeSkill(
            String name
    ) {

        boolean removed =
                skillManager.removeSkill(
                        name
                );

        if (removed) {

            addHistory(
                    "SKILL_REMOVED",
                    name
            );
        }

        return removed;
    }

    public void recordSkillSuccess(
            String name
    ) {

        skillManager.recordSuccess(
                name
        );

        addHistory(
                "SKILL_SUCCESS",
                name
        );
    }

    public void recordSkillFailure(
            String name
    ) {

        skillManager.recordFailure(
                name
        );

        addHistory(
                "SKILL_FAILURE",
                name
        );
    }

    public List<String> getSkillNames() {

        return skillManager
                .getSkillNames();
    }

    public String getSkillReport() {

        return skillManager
                .getSkillReport();
    }

    // =========================================================
    // GOALS
    // =========================================================

    public boolean createEvolutionGoal(
            String goal
    ) {

        if (goal == null ||
                goal.trim().isEmpty()) {

            return false;
        }

        try {

            JSONArray goals =
                    getGoals();

            JSONObject object =
                    new JSONObject();

            object.put(
                    "goal",
                    goal.trim()
            );

            object.put(
                    "status",
                    "pending"
            );

            object.put(
                    "created_at",
                    now()
            );

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
                    e.getMessage()
            );

            return false;
        }
    }

    // =========================================================
    // EVOLUTION CYCLE
    // =========================================================

    public String runEvolutionCycle() {

        addHistory(
                "EVOLUTION_STARTED",
                "بدأت دورة تطور جديدة."
        );

        StringBuilder report =
                new StringBuilder();

        report.append(
                "JARVIS EVOLUTION CYCLE\n\n"
        );

        // -----------------------------------------------------
        // 1. OBSERVE
        // -----------------------------------------------------

        report.append(
                "1. OBSERVE: OK\n"
        );

        int skillCount =
                skillManager
                        .getSkillNames()
                        .size();

        report.append(
                "   Skills detected: "
        );

        report.append(
                skillCount
        );

        report.append("\n");

        // -----------------------------------------------------
        // 2. LEARN
        // -----------------------------------------------------

        report.append(
                "2. LEARN: READY\n"
        );

        // -----------------------------------------------------
        // 3. PLAN
        // -----------------------------------------------------

        report.append(
                "3. PLAN: READY\n"
        );

        // -----------------------------------------------------
        // 4. BUILD
        // -----------------------------------------------------

        report.append(
                "4. BUILD: MODULE PENDING\n"
        );

        // -----------------------------------------------------
        // 5. TEST
        // -----------------------------------------------------

        report.append(
                "5. TEST: MODULE PENDING\n"
        );

        // -----------------------------------------------------
        // 6. IMPROVE
        // -----------------------------------------------------

        report.append(
                "6. IMPROVE: READY\n"
        );

        // -----------------------------------------------------
        // 7. REMEMBER
        // -----------------------------------------------------

        report.append(
                "7. REMEMBER: ACTIVE\n"
        );

        addHistory(
                "EVOLUTION_CYCLE",
                report.toString()
        );

        return report.toString();
    }

    // =========================================================
    // SELF DIAGNOSIS
    // =========================================================

    public String selfDiagnosis() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS SELF DIAGNOSIS\n\n"
        );

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
                "Skill System: ONLINE\n"
        );

        result.append(
                "Goal System: ONLINE\n"
        );

        result.append(
                "History System: ONLINE\n"
        );

        result.append(
                "Approval System: READY\n"
        );

        result.append(
                "Self Builder: PENDING\n"
        );

        result.append(
                "Self Test: PENDING\n"
        );

        result.append(
                "Recovery System: PENDING\n"
        );

        return result.toString();
    }

    // =========================================================
    // APPROVAL
    // =========================================================

    public String requestApproval(
            String action,
            String reason
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

            return null;
        }

        try {

            JSONArray approvals =
                    getApprovals();

            JSONObject request =
                    new JSONObject();

            String id =
                    "REQ-" +
                    System.currentTimeMillis();

            request.put(
                    "id",
                    id
            );

            request.put(
                    "action",
                    action
            );

            request.put(
                    "reason",
                    reason == null
                            ? ""
                            : reason
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

            saveApprovals(
                    approvals
            );

            addHistory(
                    "APPROVAL_REQUESTED",
                    action
            );

            return id;

        } catch (Exception e) {

            return null;
        }
    }

    public boolean approve(
            String requestId
    ) {

        return updateApproval(
                requestId,
                "approved"
        );
    }

    public boolean reject(
            String requestId
    ) {

        return updateApproval(
                requestId,
                "rejected"
        );
    }

    private boolean updateApproval(
            String requestId,
            String status
    ) {

        try {

            JSONArray approvals =
                    getApprovals();

            for (int i = 0;
                    i < approvals.length();
                    i++) {

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

                    saveApprovals(
                            approvals
                    );

                    addHistory(
                            "APPROVAL_" +
                                    status.toUpperCase(),
                            requestId
                    );

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getEvolutionStatus() {

        int skills =
                skillManager
                        .getSkillNames()
                        .size();

        int goals =
                getGoals()
                        .length();

        int approvals =
                getApprovals()
                        .length();

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

                "Self Builder: PENDING\n" +

                "Self Test: PENDING\n" +

                "Recovery: PENDING";
    }

    // =========================================================
    // SKILL REPORT
    // =========================================================

    public String getSkillsStatus() {

        return skillManager
                .getSkillReport();
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void addHistory(
            String type,
            String message
    ) {

        try {

            JSONArray history =
                    getHistory();

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

            if (history.length() > 1000) {

                JSONArray newHistory =
                        new JSONArray();

                int start =
                        history.length()
                                - 1000;

                for (int i = start;
                        i < history.length();
                        i++) {

                    newHistory.put(
                            history.get(i)
                    );
                }

                history =
                        newHistory;
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

    public String getEvolutionHistory() {

        return getHistory()
                .toString();
    }

    // =========================================================
    // STORAGE
    // =========================================================

    private JSONArray getGoals() {

        return readArray(
                KEY_GOALS
        );
    }

    private JSONArray getHistory() {

        return readArray(
                KEY_HISTORY
        );
    }

    private JSONArray getApprovals() {

        return readArray(
                KEY_APPROVALS
        );
    }

    private JSONArray readArray(
            String key
    ) {

        try {

            String data =
                    preferences.getString(
                            key,
                            "[]"
                    );

            return new JSONArray(data);

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    private void saveGoals(
            JSONArray data
    ) {

        preferences.edit()
                .putString(
                        KEY_GOALS,
                        data.toString()
                )
                .apply();
    }

    private void saveApprovals(
            JSONArray data
    ) {

        preferences.edit()
                .putString(
                        KEY_APPROVALS,
                        data.toString()
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