package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EvolutionEngine {

    private static final String PREFS_NAME = "jarvis_evolution";
    private static final String KEY_GOALS = "goals";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_PENDING = "pending_approvals";
    private static final String KEY_VERSION = "engine_version";
    private static final String KEY_ACTIVE_TARGET = "active_development_target";

    private final Context context;
    private final SharedPreferences prefs;

    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final SelfDiagnosisManager diagnosisManager;

    public EvolutionEngine(Context context) {

        this.context = context.getApplicationContext();

        prefs = this.context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        diagnosisManager =
                new SelfDiagnosisManager(this.context);

        if (!prefs.contains(KEY_VERSION)) {

            prefs.edit()
                    .putString(KEY_VERSION, "4.0")
                    .apply();
        }
    }

    // =====================================================
    // TIME
    // =====================================================

    private String now() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    // =====================================================
    // GOALS
    // =====================================================

    public void createEvolutionGoal(String goal) {

        if (goal == null ||
                goal.trim().isEmpty()) {
            return;
        }

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "goal",
                    goal.trim()
            );

            item.put(
                    "status",
                    "pending"
            );

            item.put(
                    "created",
                    now()
            );

            goals.put(item);

            prefs.edit()
                    .putString(
                            KEY_GOALS,
                            goals.toString()
                    )
                    .apply();

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل إنشاء الهدف: "
                            + e.getMessage()
            );
        }
    }

    public String getGoals() {

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            if (goals.length() == 0) {

                return "لا توجد أهداف تطور مسجلة حاليا.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "أهداف JARVIS:\n\n"
            );

            for (int i = 0;
                 i < goals.length();
                 i++) {

                JSONObject item =
                        goals.getJSONObject(i);

                result.append("• ")
                        .append(
                                item.optString(
                                        "goal"
                                )
                        )
                        .append("\n");

                result.append("الحالة: ")
                        .append(
                                item.optString(
                                        "status"
                                )
                        )
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return "تعذر قراءة الأهداف.";
        }
    }

    // =====================================================
    // SKILLS
    // =====================================================

    public void registerSkill(
            String name,
            String description
    ) {

        skillManager.addSkill(
                name,
                description
        );

        recordHistory(
                "SKILL_ADDED",
                "تم تسجيل مهارة: " + name
        );
    }

    public void removeSkill(String name) {

        skillManager.removeSkill(name);

        recordHistory(
                "SKILL_REMOVED",
                "تم حذف مهارة: " + name
        );
    }

    public void recordSkillSuccess(
            String name
    ) {

        skillManager.recordSuccess(name);
    }

    public void recordSkillFailure(
            String name
    ) {

        skillManager.recordFailure(name);
    }

    public String getSkillsStatus() {

        return skillManager.getReport();
    }

    public String getSkillNames() {

        return skillManager.getSkillNames();
    }

    // =====================================================
    // CAPABILITIES
    // =====================================================

    public CapabilityManager getCapabilityManager() {

        return capabilityManager;
    }

    public void registerCapability(
            String name,
            String description
    ) {

        capabilityManager.addCapability(
                name,
                description
        );

        recordHistory(
                "CAPABILITY_ADDED",
                "تم تسجيل قدرة: " + name
        );
    }

    public void enableCapability(
            String name
    ) {

        capabilityManager.setStatus(
                name,
                "active"
        );
    }

    public void disableCapability(
            String name
    ) {

        capabilityManager.setStatus(
                name,
                "disabled"
        );
    }

    public void recordCapabilitySuccess(
            String name
    ) {

        capabilityManager.recordSuccess(name);
    }

    public void recordCapabilityFailure(
            String name
    ) {

        capabilityManager.recordFailure(name);
    }

    public String getCapabilitiesStatus() {

        return capabilityManager.getReport();
    }

    // =====================================================
    // SELF DIAGNOSIS
    // =====================================================

    public String runFullDiagnosis() {

        return diagnosisManager.runDiagnosis();
    }

    public String getNextDevelopmentTarget() {

        return diagnosisManager
                .getNextDevelopmentTarget();
    }

    public String getDevelopmentPlan() {

        return diagnosisManager
                .getDevelopmentPlan();
    }

    // =====================================================
    // EVOLUTION CYCLE
    // =====================================================

    public String runEvolutionCycle() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS EVOLUTION ENGINE 4.0\n"
        );

        result.append(
                "============================\n\n"
        );

        // -------------------------------------------------
        // STEP 1 — OBSERVE
        // -------------------------------------------------

        result.append(
                "1. OBSERVE\n"
        );

        result.append(
                "✓ فحص الحالة العامة للنظام.\n\n"
        );

        recordHistory(
                "OBSERVE",
                "تم فحص حالة النظام"
        );

        // -------------------------------------------------
        // STEP 2 — DIAGNOSIS
        // -------------------------------------------------

        result.append(
                "2. SELF DIAGNOSIS\n"
        );

        int readiness =
                diagnosisManager
                        .getReadinessScore();

        result.append(
                "✓ جاهزية النظام: "
        )
                .append(readiness)
                .append("%\n\n");

        recordHistory(
                "DIAGNOSIS",
                "System readiness: "
                        + readiness
                        + "%"
        );

        // -------------------------------------------------
        // STEP 3 — CAPABILITIES
        // -------------------------------------------------

        result.append(
                "3. CAPABILITY SCAN\n"
        );

        int capabilityCount =
                capabilityManager.getCount();

        result.append(
                "✓ القدرات المسجلة: "
        )
                .append(capabilityCount)
                .append("\n\n");

        recordHistory(
                "CAPABILITY_SCAN",
                "عدد القدرات: "
                        + capabilityCount
        );

        // -------------------------------------------------
        // STEP 4 — SELECT TARGET
        // -------------------------------------------------

        String target =
                diagnosisManager
                        .getNextDevelopmentTarget();

        prefs.edit()
                .putString(
                        KEY_ACTIVE_TARGET,
                        target
                )
                .apply();

        result.append(
                "4. TARGET SELECTION\n"
        );

        result.append(
                "→ "
        )
                .append(target)
                .append("\n\n");

        recordHistory(
                "TARGET_SELECTED",
                target
        );

        // -------------------------------------------------
        // STEP 5 — CREATE DEVELOPMENT GOAL
        // -------------------------------------------------

        String developmentGoal =
                "تطوير النظام: "
                        + target;

        createEvolutionGoal(
                developmentGoal
        );

        result.append(
                "5. DEVELOPMENT PLAN\n"
        );

        result.append(
                "✓ تم إنشاء هدف التطوير.\n\n"
        );

        recordHistory(
                "DEVELOPMENT_GOAL",
                developmentGoal
        );

        // -------------------------------------------------
        // STEP 6 — LEARN
        // -------------------------------------------------

        result.append(
                "6. LEARN\n"
        );

        result.append(
                "✓ Skill Manager جاهز للتعلم.\n\n"
        );

        recordHistory(
                "LEARN",
                "Learning stage ready"
        );

        // -------------------------------------------------
        // STEP 7 — BUILD
        // -------------------------------------------------

        result.append(
                "7. BUILD\n"
        );

        result.append(
                "⚙ Self Builder لم يتم تفعيله بعد.\n\n"
        );

        recordHistory(
                "BUILD",
                "Self Builder pending"
        );

        // -------------------------------------------------
        // STEP 8 — TEST
        // -------------------------------------------------

        result.append(
                "8. TEST\n"
        );

        result.append(
                "⚙ Self Test Engine لم يتم تفعيله بعد.\n\n"
        );

        recordHistory(
                "TEST",
                "Self Test pending"
        );

        // -------------------------------------------------
        // STEP 9 — IMPROVE
        // -------------------------------------------------

        result.append(
                "9. IMPROVE\n"
        );

        result.append(
                "✓ تم تحديد أول هدف تطوير.\n\n"
        );

        recordHistory(
                "IMPROVE",
                "Development target selected"
        );

        // -------------------------------------------------
        // STEP 10 — MEMORY
        // -------------------------------------------------

        result.append(
                "10. MEMORY\n"
        );

        result.append(
                "✓ الذاكرة تعمل.\n\n"
        );

        recordHistory(
                "MEMORY",
                "Memory active"
        );

        // -------------------------------------------------
        // RESULT
        // -------------------------------------------------

        result.append(
                "============================\n"
        );

        result.append(
                "EVOLUTION CYCLE COMPLETE\n\n"
        );

        result.append(
                "الهدف التالي:\n"
        );

        result.append(
                target
        );

        return result.toString();
    }

    // =====================================================
    // SELF DIAGNOSIS
    // =====================================================

    public String selfDiagnosis() {

        return diagnosisManager.runDiagnosis();
    }

    // =====================================================
    // ACTIVE TARGET
    // =====================================================

    public String getActiveDevelopmentTarget() {

        return prefs.getString(
                KEY_ACTIVE_TARGET,
                "لا يوجد هدف تطوير نشط حاليا."
        );
    }

    // =====================================================
    // APPROVAL
    // =====================================================

    public void requestApproval(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {
            return;
        }

        try {

            JSONArray pending =
                    new JSONArray(
                            prefs.getString(
                                    KEY_PENDING,
                                    "[]"
                            )
                    );

            JSONObject request =
                    new JSONObject();

            request.put(
                    "action",
                    action.trim()
            );

            request.put(
                    "status",
                    "pending"
            );

            request.put(
                    "created",
                    now()
            );

            pending.put(request);

            prefs.edit()
                    .putString(
                            KEY_PENDING,
                            pending.toString()
                    )
                    .apply();

        } catch (Exception e) {

            recordHistory(
                    "ERROR",
                    "فشل إنشاء طلب موافقة"
            );
        }
    }

    public boolean approve(
            String action
    ) {

        if (action == null) {
            return false;
        }

        try {

            JSONArray pending =
                    new JSONArray(
                            prefs.getString(
                                    KEY_PENDING,
                                    "[]"
                            )
                    );

            boolean found = false;

            for (int i = 0;
                 i < pending.length();
                 i++) {

                JSONObject item =
                        pending.getJSONObject(i);

                if (action.equals(
                        item.optString(
                                "action"
                        )
                )) {

                    item.put(
                            "status",
                            "approved"
                    );

                    item.put(
                            "approved",
                            now()
                    );

                    found = true;
                }
            }

            prefs.edit()
                    .putString(
                            KEY_PENDING,
                            pending.toString()
                    )
                    .apply();

            if (found) {

                recordHistory(
                        "APPROVAL",
                        "تمت الموافقة على: "
                                + action
                );
            }

            return found;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean reject(
            String action
    ) {

        if (action == null) {
            return false;
        }

        try {

            JSONArray pending =
                    new JSONArray(
                            prefs.getString(
                                    KEY_PENDING,
                                    "[]"
                            )
                    );

            boolean found = false;

            for (int i = 0;
                 i < pending.length();
                 i++) {

                JSONObject item =
                        pending.getJSONObject(i);

                if (action.equals(
                        item.optString(
                                "action"
                        )
                )) {

                    item.put(
                            "status",
                            "rejected"
                    );

                    item.put(
                            "rejected",
                            now()
                    );

                    found = true;
                }
            }

            prefs.edit()
                    .putString(
                            KEY_PENDING,
                            pending.toString()
                    )
                    .apply();

            if (found) {

                recordHistory(
                        "REJECTION",
                        "تم رفض: "
                                + action
                );
            }

            return found;

        } catch (Exception e) {

            return false;
        }
    }

    // =====================================================
    // STATUS
    // =====================================================

    public String getEvolutionStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "JARVIS STATUS\n"
        );

        result.append(
                "================\n"
        );

        result.append(
                "Engine: "
        )
                .append(
                        prefs.getString(
                                KEY_VERSION,
                                "4.0"
                        )
                )
                .append("\n");

        result.append(
                "Memory: ONLINE\n"
        );

        result.append(
                "Skills: "
        )
                .append(
                        skillManager.getSkillCount()
                )
                .append("\n");

        result.append(
                "Capabilities: "
        )
                .append(
                        capabilityManager.getCount()
                )
                .append("\n");

        result.append(
                "Readiness: "
        )
                .append(
                        diagnosisManager
                                .getReadinessScore()
                )
                .append("%\n");

        result.append(
                "Goals: "
        )
                .append(
                        getGoalCount()
                )
                .append("\n");

        result.append(
                "History: "
        )
                .append(
                        getHistoryCount()
                )
                .append("\n");

        result.append(
                "Active Target: "
        )
                .append(
                        getActiveDevelopmentTarget()
                )
                .append("\n");

        result.append(
                "\nCORE STATUS: ONLINE"
        );

        return result.toString();
    }

    // =====================================================
    // HISTORY
    // =====================================================

    private void recordHistory(
            String type,
            String message
    ) {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "type",
                    type
            );

            item.put(
                    "message",
                    message
            );

            item.put(
                    "time",
                    now()
            );

            history.put(item);

            prefs.edit()
                    .putString(
                            KEY_HISTORY,
                            history.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    public String getEvolutionHistory() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            if (history.length() == 0) {

                return "لا توجد سجلات تطور بعد.";
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "JARVIS EVOLUTION HISTORY\n\n"
            );

            int start =
                    Math.max(
                            0,
                            history.length() - 20
                    );

            for (int i = start;
                 i < history.length();
                 i++) {

                JSONObject item =
                        history.getJSONObject(i);

                result.append("[")
                        .append(
                                item.optString(
                                        "time"
                                )
                        )
                        .append("] ");

                result.append(
                        item.optString(
                                "type"
                        )
                );

                result.append(": ");

                result.append(
                        item.optString(
                                "message"
                        )
                );

                result.append("\n");
            }

            return result.toString();

        } catch (Exception e) {

            return "تعذر قراءة سجل التطور.";
        }
    }

    // =====================================================
    // COUNTERS
    // =====================================================

    private int getGoalCount() {

        try {

            JSONArray goals =
                    new JSONArray(
                            prefs.getString(
                                    KEY_GOALS,
                                    "[]"
                            )
                    );

            return goals.length();

        } catch (Exception e) {

            return 0;
        }
    }

    private int getHistoryCount() {

        try {

            JSONArray history =
                    new JSONArray(
                            prefs.getString(
                                    KEY_HISTORY,
                                    "[]"
                            )
                    );

            return history.length();

        } catch (Exception e) {

            return 0;
        }
    }

    // =====================================================
    // ACCESSORS
    // =====================================================

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public SelfDiagnosisManager getDiagnosisManager() {
        return diagnosisManager;
    }
}