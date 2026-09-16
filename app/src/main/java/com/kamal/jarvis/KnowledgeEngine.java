package com.kamal.jarvis;

import android.content.Context;

public class KnowledgeEngine {

    private static final String PREFIX = "__knowledge__";

    private final Context context;
    private final MemoryManager memoryManager;
    private final LearningEngine learningEngine;
    private final SkillManager skillManager;

    public KnowledgeEngine(Context context) {

        this.context = context.getApplicationContext();

        memoryManager = new MemoryManager(this.context);
        learningEngine = new LearningEngine(this.context);
        skillManager = new SkillManager(this.context);
    }

    // ==========================================
    // LEARN
    // ==========================================

    public String learn(
            String topic,
            String information
    ) {

        if (topic == null || topic.trim().isEmpty()) {
            return "حدد الموضوع اللي بغيتي JARVIS يتعلمو.";
        }

        if (information == null || information.trim().isEmpty()) {
            return "عطيني المعلومة اللي بغيتي نخزن.";
        }

        String cleanTopic = topic.trim();
        String cleanInformation = information.trim();

        String key =
                PREFIX
                        + cleanTopic.toLowerCase();

        memoryManager.saveMemory(
                key,
                cleanInformation
        );

        return
                "تم حفظ المعرفة ✓\n\n"
                + "الموضوع: "
                + cleanTopic
                + "\n"
                + "المعلومة: "
                + cleanInformation;
    }

    // ==========================================
    // REMEMBER
    // ==========================================

    public String remember(String topic) {

        if (topic == null || topic.trim().isEmpty()) {
            return "حدد الموضوع اللي بغيتي نبحث عليه.";
        }

        String cleanTopic = topic.trim();

        String key =
                PREFIX
                        + cleanTopic.toLowerCase();

        String information =
                memoryManager.getMemory(key);

        if (information == null
                || information.trim().isEmpty()) {

            return
                    "ما عنديش معرفة محفوظة على: "
                    + cleanTopic;
        }

        return
                "المعرفة المحفوظة ✓\n\n"
                + "الموضوع: "
                + cleanTopic
                + "\n"
                + information;
    }

    // ==========================================
    // HAS KNOWLEDGE
    // ==========================================

    public boolean hasKnowledge(String topic) {

        if (topic == null || topic.trim().isEmpty()) {
            return false;
        }

        String key =
                PREFIX
                        + topic.trim().toLowerCase();

        return memoryManager.hasMemory(key);
    }

    // ==========================================
    // FORGET
    // ==========================================

    public String forget(String topic) {

        if (topic == null || topic.trim().isEmpty()) {
            return "حدد المعرفة اللي بغيتي نحيد.";
        }

        String cleanTopic = topic.trim();

        String key =
                PREFIX
                        + cleanTopic.toLowerCase();

        memoryManager.removeMemory(key);

        return
                "تم حذف المعرفة ✓\n\n"
                + cleanTopic;
    }

    // ==========================================
    // CREATE KNOWLEDGE SKILL
    // ==========================================

    public String createKnowledgeSkill(
            String name,
            String description
    ) {

        if (name == null || name.trim().isEmpty()) {
            return "حدد اسم المهارة.";
        }

        if (description == null
                || description.trim().isEmpty()) {

            return "حدد وصف المهارة.";
        }

        skillManager.addSkill(
                name.trim(),
                description.trim()
        );

        return
                "تم إنشاء المهارة من المعرفة ✓\n\n"
                + "المهارة: "
                + name.trim()
                + "\n"
                + "الوصف: "
                + description.trim();
    }

    // ==========================================
    // IMPORT LEARNING
    // ==========================================

    public String importLearning(String topic) {

        if (topic == null || topic.trim().isEmpty()) {
            return "حدد موضوع التعلم.";
        }

        String learning =
                learningEngine.rememberLearning(
                        topic.trim()
                );

        if (learning == null
                || learning.trim().isEmpty()) {

            return
                    "ما لقيتش تعلم محفوظ على هاد الموضوع.";
        }

        return learning;
    }

    // ==========================================
    // KNOWLEDGE REPORT
    // ==========================================

    public String getKnowledgeReport() {

        int count = 0;

        String memories =
                memoryManager.getAllMemories();

        if (memories != null
                && !memories.trim().isEmpty()
                && !memories.equals(
                        "ما عنديش معلومات محفوظة حاليا."
                )) {

            String[] lines =
                    memories.split("\\n");

            for (String line : lines) {

                if (line.contains(PREFIX)) {
                    count++;
                }
            }
        }

        return
                "=== KNOWLEDGE ENGINE ===\n\n"
                + "المعارف المحفوظة: "
                + count
                + "\n"
                + "Learning Engine: ONLINE\n"
                + "Memory System: ONLINE\n"
                + "Skill System: ONLINE";
    }

    // ==========================================
    // STATUS
    // ==========================================

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Knowledge Engine: ONLINE ✓";
        }

        return
                "Knowledge Engine: ERROR ⚠";
    }

    // ==========================================
    // HEALTH
    // ==========================================

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            learningEngine.getLearningStatus();

            skillManager.getSkillCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}