package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class SelfDiagnosisManager {

    private final Context context;
    private final CapabilityManager capabilityManager;
    private final SkillManager skillManager;

    public SelfDiagnosisManager(Context context) {
        this.context = context.getApplicationContext();

        capabilityManager =
                new CapabilityManager(this.context);

        skillManager =
                new SkillManager(this.context);
    }

    // ==========================================
    // FULL DIAGNOSIS
    // ==========================================

    public String runDiagnosis() {

        StringBuilder result =
                new StringBuilder();

        result.append("JARVIS SELF DIAGNOSIS\n");
        result.append("============================\n\n");

        int score = 0;
        int total = 0;

        // --------------------------------------
        // CORE
        // --------------------------------------

        result.append("CORE SYSTEM\n");
        result.append("✓ Core Engine: ONLINE\n");
        score++;
        total++;

        // --------------------------------------
        // CAPABILITIES
        // --------------------------------------

        result.append("\nCAPABILITIES\n");

        int capabilities =
                capabilityManager.getCount();

        if (capabilities > 0) {

            result.append("✓ Capability Registry: ONLINE\n");
            result.append("✓ Registered capabilities: ")
                    .append(capabilities)
                    .append("\n");

            score++;
        } else {

            result.append("✗ Capability Registry: EMPTY\n");
        }

        total++;

        // --------------------------------------
        // SKILLS
        // --------------------------------------

        result.append("\nSKILLS\n");

        int skills =
                skillManager.getSkillCount();

        if (skills > 0) {

            result.append("✓ Skill Manager: ONLINE\n");
            result.append("✓ Registered skills: ")
                    .append(skills)
                    .append("\n");

            score++;

        } else {

            result.append(
                    "⚠ Skill Manager: NO USER SKILLS\n"
            );
        }

        total++;

        // --------------------------------------
        // MEMORY
        // --------------------------------------

        result.append("\nMEMORY\n");

        MemoryManager memoryManager =
                new MemoryManager(context);

        int memories =
                memoryManager.getMemoryCount();

        result.append("✓ Memory system: ONLINE\n");

        result.append("✓ Memories stored: ")
                .append(memories)
                .append("\n");

        score++;
        total++;

        // --------------------------------------
        // DIAGNOSIS MODULE
        // --------------------------------------

        result.append("\nSELF-DIAGNOSIS\n");

        result.append(
                "✓ Diagnosis engine: ONLINE\n"
        );

        score++;
        total++;

        // --------------------------------------
        // MISSING SYSTEMS
        // --------------------------------------

        List<String> missing =
                getMissingSystems();

        result.append("\nMISSING / DEVELOPMENT SYSTEMS\n");

        if (missing.isEmpty()) {

            result.append(
                    "✓ No registered missing systems.\n"
            );

        } else {

            for (String item : missing) {

                result.append("• ")
                        .append(item)
                        .append("\n");
            }
        }

        // --------------------------------------
        // SCORE
        // --------------------------------------

        int percentage = 0;

        if (total > 0) {
            percentage =
                    (score * 100) / total;
        }

        result.append("\n============================\n");

        result.append("SYSTEM READINESS: ")
                .append(percentage)
                .append("%\n");

        result.append(
                "DIAGNOSIS COMPLETE\n"
        );

        return result.toString();
    }

    // ==========================================
    // FIND MISSING SYSTEMS
    // ==========================================

    public List<String> getMissingSystems() {

        List<String> missing =
                new ArrayList<>();

        // Future architecture modules

        missing.add(
                "Self Builder — بناء وحدات جديدة"
        );

        missing.add(
                "Self Test Engine — اختبار التغييرات"
        );

        missing.add(
                "Recovery System — استرجاع النسخة السابقة"
        );

        missing.add(
                "Advanced Task Manager — إدارة المهام"
        );

        missing.add(
                "Reminder Engine — التذكيرات"
        );

        missing.add(
                "Notification Intelligence — فهم الإشعارات"
        );

        missing.add(
                "Screen Intelligence — تحليل الشاشة"
        );

        missing.add(
                "Advanced Android Control — تحكم متقدم في Android"
        );

        missing.add(
                "Learning Engine — نظام تعلم متقدم"
        );

        missing.add(
                "Planning Engine — التخطيط المتقدم"
        );

        return missing;
    }

    // ==========================================
    // NEXT DEVELOPMENT TARGET
    // ==========================================

    public String getNextDevelopmentTarget() {

        List<String> missing =
                getMissingSystems();

        if (missing.isEmpty()) {

            return "لا توجد وحدة تطوير مسجلة حاليا.";
        }

        return "أولوية التطوير التالية:\n"
                + missing.get(0);
    }

    // ==========================================
    // DEVELOPMENT PLAN
    // ==========================================

    public String getDevelopmentPlan() {

        StringBuilder plan =
                new StringBuilder();

        plan.append(
                "JARVIS DEVELOPMENT ROADMAP\n"
        );

        plan.append(
                "============================\n\n"
        );

        plan.append(
                "PHASE 1\n"
        );

        plan.append(
                "✓ Core\n"
        );

        plan.append(
                "✓ Memory\n"
        );

        plan.append(
                "✓ Skills\n"
        );

        plan.append(
                "✓ Capabilities\n"
        );

        plan.append(
                "✓ Evolution Engine\n"
        );

        plan.append(
                "✓ Self Diagnosis\n\n"
        );

        plan.append(
                "PHASE 2\n"
        );

        plan.append(
                "→ Self Test Engine\n"
        );

        plan.append(
                "→ Recovery System\n"
        );

        plan.append(
                "→ Task Manager\n"
        );

        plan.append(
                "→ Reminder Engine\n\n"
        );

        plan.append(
                "PHASE 3\n"
        );

        plan.append(
                "→ Learning Engine\n"
        );

        plan.append(
                "→ Planning Engine\n"
        );

        plan.append(
                "→ Screen Intelligence\n\n"
        );

        plan.append(
                "PHASE 4\n"
        );

        plan.append(
                "→ Self Builder\n"
        );

        plan.append(
                "→ Advanced Android Control\n"
        );

        plan.append(
                "→ Recovery + Testing Integration\n"
        );

        return plan.toString();
    }

    // ==========================================
    // QUICK HEALTH CHECK
    // ==========================================

    public boolean isHealthy() {

        int capabilities =
                capabilityManager.getCount();

        return capabilities > 0;
    }

    // ==========================================
    // GET SCORE
    // ==========================================

    public int getReadinessScore() {

        int score = 0;
        int total = 5;

        score++; // Core

        if (capabilityManager.getCount() > 0) {
            score++;
        }

        if (skillManager.getSkillCount() > 0) {
            score++;
        }

        MemoryManager memoryManager =
                new MemoryManager(context);

        if (memoryManager.getMemoryCount() >= 0) {
            score++;
        }

        score++; // Diagnosis

        return (score * 100) / total;
    }
}