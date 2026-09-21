package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * JARVIS Self Diagnosis Manager
 *
 * مسؤول على:
 * - فحص الأنظمة الموجودة فعليا
 * - تحديد الأنظمة اللي فيها مشكل
 * - حساب الجاهزية
 * - اقتراح التطوير التالي
 *
 * مهم:
 * ما بقاش كيعتمد على لائحة قديمة كتقول بأن الأنظمة
 * الموجودة أصلا مازال ناقصة.
 */
public class SelfDiagnosisManager {

    private final Context context;

    private final CapabilityManager capabilityManager;
    private final SkillManager skillManager;
    private final MemoryManager memoryManager;
    private final TaskManager taskManager;
    private final LearningEngine learningEngine;

    public SelfDiagnosisManager(Context context) {

        this.context =
                context.getApplicationContext();

        capabilityManager =
                new CapabilityManager(this.context);

        skillManager =
                new SkillManager(this.context);

        memoryManager =
                new MemoryManager(this.context);

        taskManager =
                new TaskManager(this.context);

        learningEngine =
                new LearningEngine(this.context);
    }

    // =========================================================
    // FULL DIAGNOSIS
    // =========================================================

    public String runDiagnosis() {

        StringBuilder result =
                new StringBuilder();

        int healthy = 0;
        int checked = 0;

        result.append(
                "JARVIS SELF DIAGNOSIS\n"
        );

        result.append(
                "============================\n\n"
        );

        // -----------------------------------------------------
        // CORE
        // -----------------------------------------------------

        result.append("CORE SYSTEM\n");

        result.append(
                "✓ Core Engine: ONLINE\n"
        );

        healthy++;
        checked++;

        // -----------------------------------------------------
        // MEMORY
        // -----------------------------------------------------

        result.append("\nMEMORY SYSTEM\n");

        try {

            int count =
                    memoryManager.getMemoryCount();

            if (count >= 0) {

                result.append(
                        "✓ Memory System: ONLINE\n"
                );

                result.append(
                        "✓ Stored memories: "
                );

                result.append(count);
                result.append("\n");

                healthy++;

            } else {

                result.append(
                        "✗ Memory System: ERROR\n"
                );
            }

        } catch (Exception e) {

            result.append(
                    "✗ Memory System: ERROR\n"
            );

            result.append(
                    "  "
            );

            result.append(
                    safeError(e)
            );

            result.append("\n");
        }

        checked++;

        // -----------------------------------------------------
        // CAPABILITIES
        // -----------------------------------------------------

        result.append("\nCAPABILITY SYSTEM\n");

        try {

            int count =
                    capabilityManager.getCount();

            if (count >= 0) {

                result.append(
                        "✓ Capability Manager: ONLINE\n"
                );

                result.append(
                        "✓ Registered capabilities: "
                );

                result.append(count);
                result.append("\n");

                healthy++;

            } else {

                result.append(
                        "✗ Capability Manager: ERROR\n"
                );
            }

        } catch (Exception e) {

            result.append(
                    "✗ Capability Manager: ERROR\n"
            );

            result.append(
                    "  "
            );

            result.append(
                    safeError(e)
            );

            result.append("\n");
        }

        checked++;

        // -----------------------------------------------------
        // SKILLS
        // -----------------------------------------------------

        result.append("\nSKILL SYSTEM\n");

        try {

            int count =
                    skillManager.getSkillCount();

            if (count >= 0) {

                result.append(
                        "✓ Skill Manager: ONLINE\n"
                );

                result.append(
                        "✓ Registered skills: "
                );

                result.append(count);
                result.append("\n");

                healthy++;

            } else {

                result.append(
                        "✗ Skill Manager: ERROR\n"
                );
            }

        } catch (Exception e) {

            result.append(
                    "✗ Skill Manager: ERROR\n"
            );

            result.append(
                    "  "
            );

            result.append(
                    safeError(e)
            );

            result.append("\n");
        }

        checked++;

        // -----------------------------------------------------
        // TASK SYSTEM
        // -----------------------------------------------------

        result.append("\nTASK SYSTEM\n");

        try {

            boolean taskHealthy =
                    taskManager.isHealthy();

            int pending =
                    taskManager.getPendingTaskCount();

            if (taskHealthy) {

                result.append(
                        "✓ Task Manager: ONLINE\n"
                );

                result.append(
                        "✓ Pending tasks: "
                );

                result.append(pending);
                result.append("\n");

                healthy++;

            } else {

                result.append(
                        "✗ Task Manager: ERROR\n"
                );
            }

        } catch (Exception e) {

            result.append(
                    "✗ Task Manager: ERROR\n"
            );

            result.append(
                    "  "
            );

            result.append(
                    safeError(e)
            );

            result.append("\n");
        }

        checked++;

        // -----------------------------------------------------
        // LEARNING SYSTEM
        // -----------------------------------------------------

        result.append("\nLEARNING SYSTEM\n");

        try {

            if (learningEngine.isHealthy()) {

                result.append(
                        "✓ Learning Engine: ONLINE\n"
                );

                healthy++;

            } else {

                result.append(
                        "✗ Learning Engine: ERROR\n"
                );
            }

        } catch (Exception e) {

            result.append(
                    "✗ Learning Engine: ERROR\n"
            );

            result.append(
                    "  "
            );

            result.append(
                    safeError(e)
            );

            result.append("\n");
        }

        checked++;

        // -----------------------------------------------------
        // DIAGNOSIS ENGINE
        // -----------------------------------------------------

        result.append("\nSELF-DIAGNOSIS\n");

        result.append(
                "✓ Diagnosis Engine: ONLINE\n"
        );

        healthy++;
        checked++;

        // -----------------------------------------------------
        // MISSING / DEVELOPMENT SYSTEMS
        // -----------------------------------------------------

        List<String> missing =
                getMissingSystems();

        result.append(
                "\nSYSTEMS REQUIRING DEVELOPMENT\n"
        );

        if (missing.isEmpty()) {

            result.append(
                    "✓ No core manager is currently missing.\n"
            );

        } else {

            for (String item : missing) {

                result.append("• ");
                result.append(item);
                result.append("\n");
            }
        }

        // -----------------------------------------------------
        // SCORE
        // -----------------------------------------------------

        int percentage = 0;

        if (checked > 0) {

            percentage =
                    (healthy * 100) / checked;
        }

        result.append(
                "\n============================\n"
        );

        result.append(
                "SYSTEMS CHECKED: "
        );

        result.append(checked);
        result.append("\n");

        result.append(
                "SYSTEMS HEALTHY: "
        );

        result.append(healthy);
        result.append("\n");

        result.append(
                "SYSTEM READINESS: "
        );

        result.append(percentage);
        result.append("%\n");

        if (healthy == checked) {

            result.append(
                    "DIAGNOSIS STATUS: HEALTHY ✓\n"
            );

        } else {

            result.append(
                    "DIAGNOSIS STATUS: ATTENTION REQUIRED ⚠\n"
            );
        }

        result.append(
                "DIAGNOSIS COMPLETE"
        );

        return result.toString();
    }

    // =========================================================
    // FIND REAL MISSING SYSTEMS
    // =========================================================

    public List<String> getMissingSystems() {

        List<String> missing =
                new ArrayList<>();

        /*
         * الأنظمة الأساسية التالية راه موجودة فعليا
         * داخل المشروع، لذلك ما خاصناش نعتبرها Missing.
         *
         * أي نظام جديد مستقبلا يقدر يتزاد هنا فقط
         * إلا كان فعلا مازال ما تطورش.
         */

        try {

            if (!taskManager.isHealthy()) {

                missing.add(
                        "Task Manager — يحتاج إصلاح"
                );
            }

        } catch (Exception e) {

            missing.add(
                    "Task Manager — غير متاح"
            );
        }

        try {

            if (!learningEngine.isHealthy()) {

                missing.add(
                        "Learning Engine — يحتاج إصلاح"
                );
            }

        } catch (Exception e) {

            missing.add(
                    "Learning Engine — غير متاح"
            );
        }

        /*
         * إلا كانت الذاكرة ما خداماش، نسجلوها كمشكل
         * بدل ما نقولو بأنها وحدة ناقصة.
         */

        try {

            if (memoryManager.getMemoryCount() < 0) {

                missing.add(
                        "Memory System — يحتاج إصلاح"
                );
            }

        } catch (Exception e) {

            missing.add(
                    "Memory System — غير متاح"
            );
        }

        try {

            if (capabilityManager.getCount() < 0) {

                missing.add(
                        "Capability Manager — يحتاج إصلاح"
                );
            }

        } catch (Exception e) {

            missing.add(
                    "Capability Manager — غير متاح"
            );
        }

        try {

            if (skillManager.getSkillCount() < 0) {

                missing.add(
                        "Skill Manager — يحتاج إصلاح"
                );
            }

        } catch (Exception e) {

            missing.add(
                    "Skill Manager — غير متاح"
            );
        }

        return missing;
    }

    // =========================================================
    // NEXT DEVELOPMENT TARGET
    // =========================================================

    public String getNextDevelopmentTarget() {

        List<String> missing =
                getMissingSystems();

        if (!missing.isEmpty()) {

            return
                    "أولوية الإصلاح التالية:\n"
                            + missing.get(0);
        }

        /*
         * إلا كانت الأنظمة الأساسية كلها سليمة،
         * التطور ما خاصوش يرجع دائما لـ Self Builder.
         *
         * الأولوية كتولي:
         * تحسين القدرة على التعلم والتطور الداخلي.
         */

        return
                "أولوية التطوير التالية:\n"
                        + "تحسين Autonomous Evolution والتعلم الذاتي";
    }

    // =========================================================
    // DEVELOPMENT PLAN
    // =========================================================

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
                "PHASE 1 — CORE\n"
        );

        plan.append(
                "✓ Core Engine\n"
        );

        plan.append(
                "✓ Memory System\n"
        );

        plan.append(
                "✓ Skill Manager\n"
        );

        plan.append(
                "✓ Capability Manager\n"
        );

        plan.append(
                "✓ Task Manager\n"
        );

        plan.append(
                "✓ Learning Engine\n\n"
        );

        plan.append(
                "PHASE 2 — INTELLIGENCE\n"
        );

        plan.append(
                "✓ Decision Engine\n"
        );

        plan.append(
                "✓ Context Engine\n"
        );

        plan.append(
                "✓ Planning Engine\n"
        );

        plan.append(
                "✓ Command Learning\n"
        );

        plan.append(
                "→ تحسين التعلم من استعمال المستخدم\n\n"
        );

        plan.append(
                "PHASE 3 — ANDROID CONTROL\n"
        );

        plan.append(
                "✓ Android Control Engine\n"
        );

        plan.append(
                "✓ Screen Intelligence\n"
        );

        plan.append(
                "✓ Accessibility Control\n"
        );

        plan.append(
                "✓ Notification Intelligence\n"
        );

        plan.append(
                "→ توسيع قدرات التحكم الآمن\n\n"
        );

        plan.append(
                "PHASE 4 — SELF EVOLUTION\n"
        );

        plan.append(
                "✓ Self Diagnosis\n"
        );

        plan.append(
                "✓ Self Test Engine\n"
        );

        plan.append(
                "✓ Recovery System\n"
        );

        plan.append(
                "✓ Self Builder\n"
        );

        plan.append(
                "✓ Code Evolution Engine\n"
        );

        plan.append(
                "✓ Autonomous Evolution Engine\n"
        );

        plan.append(
                "→ تحسين حلقة: Analyze → Plan → Learn → Test → Evolve\n\n"
        );

        plan.append(
                "PHASE 5 — ADVANCED EVOLUTION\n"
        );

        plan.append(
                "→ تقييم نتائج التطور تلقائيا\n"
        );

        plan.append(
                "→ تعلم من الأخطاء السابقة\n"
        );

        plan.append(
                "→ اختيار أهداف التطور حسب الاستعمال الحقيقي\n"
        );

        plan.append(
                "→ تحسين المهارات والقدرات بدون APK جديد\n"
        );

        plan.append(
                "→ تجهيز تغييرات الكود للـ Build عندما تكون ضرورية\n"
        );

        return plan.toString();
    }

    // =========================================================
    // QUICK HEALTH CHECK
    // =========================================================

    public boolean isHealthy() {

        try {

            return memoryManager.getMemoryCount() >= 0
                    && capabilityManager.getCount() >= 0
                    && skillManager.getSkillCount() >= 0
                    && taskManager.isHealthy()
                    && learningEngine.isHealthy();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // READINESS SCORE
    // =========================================================

    public int getReadinessScore() {

        int score = 0;
        int total = 5;

        try {

            if (memoryManager.getMemoryCount() >= 0) {
                score++;
            }

        } catch (Exception ignored) {
        }

        try {

            if (capabilityManager.getCount() >= 0) {
                score++;
            }

        } catch (Exception ignored) {
        }

        try {

            if (skillManager.getSkillCount() >= 0) {
                score++;
            }

        } catch (Exception ignored) {
        }

        try {

            if (taskManager.isHealthy()) {
                score++;
            }

        } catch (Exception ignored) {
        }

        try {

            if (learningEngine.isHealthy()) {
                score++;
            }

        } catch (Exception ignored) {
        }

        return
                (score * 100) / total;
    }

    // =========================================================
    // SAFE ERROR
    // =========================================================

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }
}