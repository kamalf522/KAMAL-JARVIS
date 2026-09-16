package com.kamal.jarvis;

import android.content.Context;

public class LearningEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final SkillManager skillManager;

    public LearningEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        skillManager =
                new SkillManager(this.context);
    }

    public String learn(
            String subject,
            String information
    ) {

        if (subject == null ||
                subject.trim().isEmpty()) {

            return "خاصك تحدد شنو بغيتي JARVIS يتعلم.";
        }

        if (information == null ||
                information.trim().isEmpty()) {

            return "خاصك تعطيني المعلومة اللي بغيتي نتعلمها.";
        }

        String cleanSubject =
                subject.trim();

        String cleanInformation =
                information.trim();

        String key =
                "__learning__"
                        + cleanSubject
                        .toLowerCase();

        memoryManager.saveMemory(
                key,
                cleanInformation
        );

        return
                "تم التعلم ✓\n\n"
                + "الموضوع: "
                + cleanSubject
                + "\n"
                + "المعلومة محفوظة في ذاكرة JARVIS.";
    }

    public String rememberLearning(
            String subject
    ) {

        if (subject == null ||
                subject.trim().isEmpty()) {

            return "حدد شنو بغيتي نرجع نتذكر.";
        }

        String key =
                "__learning__"
                        + subject.trim()
                        .toLowerCase();

        String information =
                memoryManager.getMemory(key);

        if (information == null ||
                information.trim().isEmpty()) {

            return
                    "مازال ما تعلمتش هاد المعلومة.";
        }

        return
                "المعلومة اللي تعلمتها:\n\n"
                + information;
    }

    public String createSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return "خاصك تحدد اسم المهارة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null
                        ? "Skill learned by JARVIS"
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

    public String getLearningStatus() {

        try {

            int memories =
                    memoryManager.getMemoryCount();

            int skills =
                    skillManager.getSkillCount();

            return
                    "LEARNING ENGINE\n"
                    + "============================\n\n"
                    + "Memory entries: "
                    + memories
                    + "\n"
                    + "Known skills: "
                    + skills
                    + "\n\n"
                    + "Learning Engine: ONLINE ✓";

        } catch (Exception e) {

            return
                    "Learning Engine: ERROR ⚠\n"
                    + safeError(e);
        }
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            skillManager.getSkillCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Learning Engine: ONLINE ✓";

        }

        return
                "Learning Engine: NEEDS ATTENTION ⚠";
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