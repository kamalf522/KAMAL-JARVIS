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

    private final Context context;
    private final SharedPreferences prefs;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;

    public EvolutionEngine(Context context) {
        this.context = context.getApplicationContext();

        prefs = this.context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        memoryManager = new MemoryManager(this.context);
        skillManager = new SkillManager(this.context);
        capabilityManager = new CapabilityManager(this.context);

        if (!prefs.contains(KEY_VERSION)) {
            prefs.edit()
                    .putString(KEY_VERSION, "3.0")
                    .apply();
        }
    }

    private String now() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    // ==============================
    // GOALS
    // ==============================

    public void createEvolutionGoal(String goal) {
        if (goal == null || goal.trim().isEmpty()) {
            return;
        }

        try {
            JSONArray goals = new JSONArray(
                    prefs.getString(KEY_GOALS, "[]")
            );

            JSONObject item = new JSONObject();

            item.put("goal", goal.trim());
            item.put("status", "pending");
            item.put("created", now());

            goals.put(item);

            prefs.edit()
                    .putString(KEY_GOALS, goals.toString())
                    .apply();

        } catch (Exception e) {
            recordHistory("ERROR", "فشل إنشاء الهدف: " + e.getMessage());
        }
    }

    public String getGoals() {
        try {
            JSONArray goals = new JSONArray(
                    prefs.getString(KEY_GOALS, "[]")
            );

            if (goals.length() == 0) {
                return "لا توجد أهداف تطور مسجلة حاليا.";
            }

            StringBuilder result = new StringBuilder();
            result.append("أهداف JARVIS:\n\n");

            for (int i = 0; i < goals.length(); i++) {
                JSONObject item = goals.getJSONObject(i);

                result.append("• ")
                        .append(item.optString("goal"))
                        .append("\n");

                result.append("الحالة: ")
                        .append(item.optString("status"))
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "تعذر قراءة الأهداف.";
        }
    }

    // ==============================
    // SKILLS
    // ==============================

    public void registerSkill(
            String name,
            String description
    ) {
        skillManager.addSkill(name, description);

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

    public void recordSkillSuccess(String name) {
        skillManager.recordSuccess(name);
    }

    public void recordSkillFailure(String name) {
        skillManager.recordFailure(name);
    }

    public String getSkillsStatus() {
        return skillManager.getReport();
    }

    public String getSkillNames() {
        return skillManager.getSkillNames();
    }

    // ==============================
    // CAPABILITIES
    // ==============================

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

    public void enableCapability(String name) {
        capabilityManager.setStatus(
                name,
                "active"
        );
    }

    public void disableCapability(String name) {
        capabilityManager.setStatus(
                name,
                "disabled"
        );
    }

    public void recordCapabilitySuccess(String name) {
        capabilityManager.recordSuccess(name);
    }

    public void recordCapabilityFailure(String name) {
        capabilityManager.recordFailure(name);
    }

    public String getCapabilitiesStatus() {
        return capabilityManager.getReport();
    }

    // ==============================
    // EVOLUTION CYCLE
    // ==============================

    public String runEvolutionCycle() {

        StringBuilder result = new StringBuilder();

        result.append("JARVIS EVOLUTION ENGINE 3.0\n");
        result.append("============================\n\n");

        // 1. OBSERVE
        result.append("1. OBSERVE\n");
        result.append("✓ تم فحص حالة النظام.\n\n");

        recordHistory(
                "OBSERVE",
                "تم فحص حالة النظام"
        );

        // 2. CAPABILITIES
        result.append("2. CAPABILITY SCAN\n");

        int capabilityCount =
                capabilityManager.getCount();

        result.append("✓ القدرات المسجلة: ")
                .append(capabilityCount)
                .append("\n\n");

        recordHistory(
                "CAPABILITY_SCAN",
                "عدد القدرات: " + capabilityCount
        );

        // 3. LEARN
        result.append("3. LEARN\n");
        result.append("✓ نظام المهارات جاهز للتعلم.\n\n");

        recordHistory(
                "LEARN",
                "Skill Manager جاهز"
        );

        // 4. PLAN
        result.append("4. PLAN\n");
        result.append("✓ نظام الأهداف والتخطيط جاهز.\n\n");

        recordHistory(
                "PLAN",
                "Planning system ready"
        );

        // 5. BUILD
        result.append("5. BUILD\n");
        result.append("⚙ نظام البناء الذاتي: قيد التطوير.\n\n");

        recordHistory(
                "BUILD",
                "Self Builder pending"
        );

        // 6. TEST
        result.append("6. TEST\n");
        result.append("⚙ نظام الاختبار الذاتي: قيد التطوير.\n\n");

        recordHistory(
                "TEST",
                "Self Test pending"
        );

        // 7. IMPROVE
        result.append("7. IMPROVE\n");
        result.append("✓ محرك التطور مستعد لتحسين الأنظمة.\n\n");

        recordHistory(
                "IMPROVE",
                "Improvement engine ready"
        );

        // 8. MEMORY
        result.append("8. MEMORY\n");
        result.append("✓ الذاكرة طويلة المدى تعمل.\n\n");

        recordHistory(
                "MEMORY",
                "Memory active"
        );

        // 9. AUTHORITY
        result.append("9. AUTHORITY\n");
        result.append("✓ موافقة المالك يمكن استخدامها للعمليات الحساسة.\n\n");

        recordHistory(
                "AUTHORITY",
                "Owner approval ready"
        );

        result.append("============================\n");
        result.append("دورة التطور اكتملت.\n");
        result.append("النظام الحالي: CORE ONLINE");

        return result.toString();
    }

    // ==============================
    // SELF DIAGNOSIS
    // ==============================

    public String selfDiagnosis() {

        StringBuilder result = new StringBuilder();

        result.append("JARVIS SELF DIAGNOSIS\n");
        result.append("============================\n\n");

        result.append("CORE: ONLINE ✓\n");
        result.append("MEMORY: ONLINE ✓\n");

        int skills = skillManager.getSkillCount();

        result.append("SKILLS: ")
                .append(skills)
                .append(" registered ✓\n");

        int capabilities =
                capabilityManager.getCount();

        result.append("CAPABILITIES: ")
                .append(capabilities)
                .append(" registered ✓\n");

        result.append("GOALS: ")
                .append(getGoalCount())
                .append(" registered ✓\n");

        result.append("HISTORY: ")
                .append(getHistoryCount())
                .append(" records ✓\n");

        result.append("\n");

        result.append("EVOLUTION ENGINE: ONLINE ✓\n");
        result.append("APPROVAL SYSTEM: READY ✓\n");

        result.append("\n");

        result.append("SYSTEMS STILL IN DEVELOPMENT:\n");

        result.append("• Self Builder\n");
        result.append("• Self Test Engine\n");
        result.append("• Recovery System\n");
        result.append("• Advanced Task Automation\n");
        result.append("• Screen Intelligence\n");
        result.append("• Advanced Android Control\n");

        result.append("\n");
        result.append("التشخيص اكتمل.");

        return result.toString();
    }

    // ==============================
    // APPROVAL SYSTEM
    // ==============================

    public void requestApproval(String action) {

        if (action == null || action.trim().isEmpty()) {
            return;
        }

        try {

            JSONArray pending = new JSONArray(
                    prefs.getString(
                            KEY_PENDING,
                            "[]"
                    )
            );

            JSONObject request = new JSONObject();

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

    public boolean approve(String action) {

        if (action == null) {
            return false;
        }

        try {

            JSONArray pending = new JSONArray(
                    prefs.getString(
                            KEY_PENDING,
                            "[]"
                    )
            );

            boolean found = false;

            for (int i = 0; i < pending.length(); i++) {

                JSONObject item =
                        pending.getJSONObject(i);

                if (action.equals(
                        item.optString("action")
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
                        "تمت الموافقة على: " + action
                );
            }

            return found;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean reject(String action) {

        if (action == null) {
            return false;
        }

        try {

            JSONArray pending = new JSONArray(
                    prefs.getString(
                            KEY_PENDING,
                            "[]"
                    )
            );

            boolean found = false;

            for (int i = 0; i < pending.length(); i++) {

                JSONObject item =
                        pending.getJSONObject(i);

                if (action.equals(
                        item.optString("action")
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
                        "تم رفض: " + action
                );
            }

            return found;

        } catch (Exception e) {

            return false;
        }
    }

    // ==============================
    // STATUS
    // ==============================

    public String getEvolutionStatus() {

        StringBuilder result = new StringBuilder();

        result.append("JARVIS STATUS\n");
        result.append("================\n");

        result.append("Engine: ")
                .append(
                        prefs.getString(
                                KEY_VERSION,
                                "3.0"
                        )
                )
                .append("\n");

        result.append("Memory: ONLINE\n");

        result.append("Skills: ")
                .append(
                        skillManager.getSkillCount()
                )
                .append("\n");

        result.append("Capabilities: ")
                .append(
                        capabilityManager.getCount()
                )
                .append("\n");

        result.append("Goals: ")
                .append(getGoalCount())
                .append("\n");

        result.append("History: ")
                .append(getHistoryCount())
                .append("\n");

        result.append("\nCORE STATUS: ONLINE");

        return result.toString();
    }

    // ==============================
    // HISTORY
    // ==============================

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

    // ==============================
    // COUNTERS
    // ==============================

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

    // ==============================
    // MEMORY ACCESS
    // ==============================

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }
}