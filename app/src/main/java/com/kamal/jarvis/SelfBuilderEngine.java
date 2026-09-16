package com.kamal.jarvis;

import android.content.Context;

public class SelfBuilderEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;
    private final CapabilityManager capabilityManager;
    private final TaskManager taskManager;

    public SelfBuilderEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);

        capabilityManager =
                new CapabilityManager(this.context);

        taskManager =
                new TaskManager(this.context);
    }

    public String registerDevelopment(
            String systemName,
            String description
    ) {

        if (systemName == null ||
                systemName.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم النظام اللي بغيتي نطورو.";
        }

        String name =
                systemName.trim();

        String details =
                description == null ||
                        description.trim().isEmpty()
                        ? "System development target"
                        : description.trim();

        memoryManager.saveMemory(
                "__builder_target__",
                name
        );

        memoryManager.saveMemory(
                "__builder_description__",
                details
        );

        return
                "تم تسجيل هدف التطوير ✓\n\n"
                + "النظام: "
                + name
                + "\n"
                + "الوصف: "
                + details;
    }

    public String createBuildPlan(
            String systemName
    ) {

        if (systemName == null ||
                systemName.trim().isEmpty()) {

            return
                    "اسم النظام غير موجود.";
        }

        String name =
                systemName.trim();

        taskManager.addTask(
                "تحليل متطلبات " + name
        );

        taskManager.addTask(
                "تصميم بنية " + name
        );

        taskManager.addTask(
                "إنشاء مكونات " + name
        );

        taskManager.addTask(
                "اختبار " + name
        );

        taskManager.addTask(
                "إصلاح الأخطاء وتحسين " + name
        );

        taskManager.addTask(
                "تسجيل " + name + " كنظام جاهز"
        );

        memoryManager.saveMemory(
                "__builder_plan__",
                name
        );

        return
                "تم إنشاء خطة التطوير ✓\n\n"
                + "النظام: "
                + name
                + "\n\n"
                + taskManager.getTasks();
    }

    public String registerCapability(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم القدرة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null
                        ? "Capability created by Self Builder"
                        : description.trim();

        try {

            capabilityManager.addCapability(
                    cleanName,
                    cleanDescription
            );

            return
                    "تم تسجيل القدرة ✓\n\n"
                    + cleanName;

        } catch (Exception e) {

            return
                    "فشل تسجيل القدرة: "
                    + safeError(e);
        }
    }

    public String registerSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return
                    "خاصك تحدد اسم المهارة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null
                        ? "Skill created by Self Builder"
                        : description.trim();

        try {

            skillManager.addSkill(
                    cleanName,
                    cleanDescription
            );

            return
                    "تم تسجيل المهارة ✓\n\n"
                    + cleanName;

        } catch (Exception e) {

            return
                    "فشل تسجيل المهارة: "
                    + safeError(e);
        }
    }

    public String getCurrentTarget() {

        String target =
                memoryManager.getMemory(
                        "__builder_target__"
                );

        if (target == null ||
                target.trim().isEmpty()) {

            return
                    "ما كاين حتى هدف تطوير حالي.";
        }

        return
                "هدف التطوير الحالي:\n"
                + target;
    }

    public String getBuildStatus() {

        try {

            String target =
                    memoryManager.getMemory(
                            "__builder_target__"
                    );

            if (target == null ||
                    target.trim().isEmpty()) {

                return
                        "Self Builder: STANDBY\n"
                        + "ما كاين حتى هدف حاليا.";
            }

            return
                    "Self Builder: ONLINE ✓\n"
                    + "Current Target: "
                    + target
                    + "\n"
                    + "Tasks: "
                    + taskManager.getTaskCount()
                    + "\n"
                    + "Pending: "
                    + taskManager.getPendingTaskCount()
                    + "\n"
                    + "Completed: "
                    + taskManager.getCompletedTaskCount();

        } catch (Exception e) {

            return
                    "Self Builder: ERROR ⚠\n"
                    + safeError(e);
        }
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            skillManager.getSkillCount();

            capabilityManager.getCount();

            taskManager.getTaskCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Self Builder Engine: ONLINE ✓";

        }

        return
                "Self Builder Engine: NEEDS ATTENTION ⚠";
    }

    private String safeError(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }
}