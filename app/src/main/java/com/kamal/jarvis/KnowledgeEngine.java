package com.kamal.jarvis;

import android.content.Context;

import java.util.Map;

public class KnowledgeEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final LearningEngine learningEngine;
    private final SkillManager skillManager;

    private static final String PREFIX =
            "__knowledge__";

    public KnowledgeEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        learningEngine =
                new LearningEngine(this.context);

        skillManager =
                new SkillManager(this.context);
    }

    public String learn(
            String topic,
            String information
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return "حدد الموضوع اللي بغيتي JARVIS يتعلمو.";
        }

        if (information == null ||
                information.trim().isEmpty()) {

            return "عطيني المعلومة اللي بغيتي نخزن.";
        }

        String cleanTopic =
                topic.trim();

        String cleanInformation =
                information.trim();

        String key =
                PREFIX
                        + cleanTopic
                        .toLowerCase();

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

    public String remember(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return "حدد الموضوع اللي بغيتي نبحث عليه.";
        }

        String key =
                PREFIX
                        + topic.trim()
                        .toLowerCase();

        String information =
                memoryManager.getMemory(key);

        if (information == null ||
                information.trim().isEmpty()) {

            return
                    "ما عنديش معرفة محفوظة على: "
                    + topic.trim();
        }

        return
                "المعرفة المحفوظة ✓\n\n"
                + "الموضوع: "
                + topic.trim()
                + "\n"
                + information;
    }

    public boolean hasKnowledge(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return false;
        }

        String key =
                PREFIX
                        + topic.trim()
                        .toLowerCase();

        return memoryManager.hasMemory(key);
    }

    public String forget(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return "حدد المعرفة اللي بغيتي نحيد.";
        }

        String key =
                PREFIX
                        + topic.trim()
                        .toLowerCase();

        memoryManager.removeMemory(key);

        return
                "تم حذف المعرفة ✓\n\n"
                + topic.trim();
    }

    public String createKnowledgeSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return "حدد اسم المهارة.";
        }

        if (description == null ||
                description.trim().isEmpty()) {

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

    public String importLearning(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return "حدد موضوع التعلم.";
        }

        String learning =
                learningEngine
                        .rememberLearning(
                                topic.trim()
                        );

        if (learning == null ||
                learning.trim().isEmpty()) {

            return
                    "ما لقيتش تعلم محفوظ على هاد الموضوع.";
        }

        return learning;
    }

    public String getKnowledgeReport() {

        Map<String, String> memories =
                memoryManager.getAllMemories();

        int count = 0;

        if (memories != null) {

            for (String key :
                    memories.keySet()) {

                if (key.startsWith(PREFIX)) {

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

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Knowledge Engine: ONLINE ✓";
        }

        return
                "Knowledge Engine: ERROR ⚠";
    }

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