package com.kamal.jarvis;

import android.content.Context;

import java.util.Locale;

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

    public String learn(String topic, String information) {
        if (topic == null || topic.trim().isEmpty()) {
            return "حدد الموضوع اللي بغيتي JARVIS يتعلمو.";
        }

        if (information == null || information.trim().isEmpty()) {
            return "عطيني المعلومة اللي بغيتي نخزن.";
        }

        String cleanTopic = topic.trim();
        String cleanInformation = information.trim();

        try {
            String key = createKnowledgeKey(cleanTopic);

            // حفظ المعرفة في النظام القديم لضمان التوافق
            memoryManager.saveMemory(key, cleanInformation);

            // إرسال المعلومة إلى نظام التعلم أيضا
            String learningResult = learningEngine.learn(
                    cleanTopic,
                    cleanInformation
            );

            return "تم تعليم JARVIS بنجاح ✓\n\n"
                    + "الموضوع: " + cleanTopic + "\n"
                    + "المعلومة: " + cleanInformation
                    + "\n\n"
                    + "Knowledge System: SAVED\n"
                    + "Learning System: "
                    + (learningResult == null ? "UPDATED" : "UPDATED");

        } catch (Exception e) {
            return "وقع خطأ أثناء حفظ المعرفة: " + safeMessage(e);
        }
    }

    public String remember(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return "حدد الموضوع اللي بغيتي نبحث عليه.";
        }

        String cleanTopic = topic.trim();

        try {
            String key = createKnowledgeKey(cleanTopic);

            // البحث أولا في قاعدة المعرفة الأساسية
            String information = memoryManager.getMemory(key);

            if (information != null && !information.trim().isEmpty()) {
                return "المعرفة المحفوظة ✓\n\n"
                        + "الموضوع: " + cleanTopic + "\n"
                        + information;
            }

            // إذا لم توجد، البحث في نظام التعلم
            String learnedInformation =
                    learningEngine.rememberLearning(cleanTopic);

            if (learnedInformation != null
                    && !learnedInformation.trim().isEmpty()
                    && !isLearningNotFoundMessage(learnedInformation)) {

                return "المعرفة المتعلمة ✓\n\n"
                        + "الموضوع: " + cleanTopic + "\n"
                        + learnedInformation;
            }

            return "ما عنديش معرفة محفوظة على: " + cleanTopic;

        } catch (Exception e) {
            return "وقع خطأ أثناء البحث عن المعرفة: " + safeMessage(e);
        }
    }

    public boolean hasKnowledge(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return false;
        }

        try {
            String cleanTopic = topic.trim();
            String key = createKnowledgeKey(cleanTopic);

            if (memoryManager.hasMemory(key)) {
                return true;
            }

            String learnedInformation =
                    learningEngine.rememberLearning(cleanTopic);

            return learnedInformation != null
                    && !learnedInformation.trim().isEmpty()
                    && !isLearningNotFoundMessage(learnedInformation);

        } catch (Exception e) {
            return false;
        }
    }

    public String forget(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return "حدد المعرفة اللي بغيتي نحيد.";
        }

        String cleanTopic = topic.trim();

        try {
            String key = createKnowledgeKey(cleanTopic);

            memoryManager.removeMemory(key);

            return "تم حذف المعرفة الأساسية ✓\n\n"
                    + cleanTopic
                    + "\n\n"
                    + "ملاحظة: سجل التعلم السابق يمكن يبقى محفوظا "
                    + "ضمن Learning Engine.";

        } catch (Exception e) {
            return "وقع خطأ أثناء حذف المعرفة: " + safeMessage(e);
        }
    }

    public String createKnowledgeSkill(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return "حدد اسم المهارة.";
        }

        if (description == null || description.trim().isEmpty()) {
            return "حدد وصف المهارة.";
        }

        String cleanName = name.trim();
        String cleanDescription = description.trim();

        try {
            skillManager.addSkill(cleanName, cleanDescription);

            return "تم إنشاء المهارة من المعرفة ✓\n\n"
                    + "المهارة: " + cleanName + "\n"
                    + "الوصف: " + cleanDescription;

        } catch (Exception e) {
            return "وقع خطأ أثناء إنشاء المهارة: " + safeMessage(e);
        }
    }

    public String importLearning(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return "حدد موضوع التعلم.";
        }

        String cleanTopic = topic.trim();

        try {
            String learning =
                    learningEngine.rememberLearning(cleanTopic);

            if (learning == null
                    || learning.trim().isEmpty()
                    || isLearningNotFoundMessage(learning)) {

                return "ما لقيتش تعلم محفوظ على هاد الموضوع.";
            }

            return learning;

        } catch (Exception e) {
            return "وقع خطأ أثناء استيراد التعلم: " + safeMessage(e);
        }
    }

    public String getKnowledgeReport() {
        int count = 0;

        try {
            String memories = memoryManager.getAllMemories();

            if (memories != null
                    && !memories.trim().isEmpty()
                    && !memories.equals("ما عنديش معلومات محفوظة حاليا.")) {

                String[] lines = memories.split("\\n");

                for (String line : lines) {
                    if (line.contains(PREFIX)) {
                        count++;
                    }
                }
            }

            String learningStatus =
                    learningEngine.getLearningStatus();

            return "=== KNOWLEDGE ENGINE ===\n\n"
                    + "المعارف الأساسية: " + count + "\n"
                    + "Knowledge Storage: ONLINE\n"
                    + "Learning Engine: ONLINE\n"
                    + "Memory System: ONLINE\n"
                    + "Skill System: ONLINE\n\n"
                    + "Learning Status:\n"
                    + learningStatus;

        } catch (Exception e) {
            return "Knowledge Report Error: " + safeMessage(e);
        }
    }

    public String getStatus() {
        if (isHealthy()) {
            return "Knowledge Engine: ONLINE ✓";
        }

        return "Knowledge Engine: ERROR ⚠";
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

    public Context getContext() {
        return context;
    }

    private String createKnowledgeKey(String topic) {
        return PREFIX + normalizeTopic(topic);
    }

    private String normalizeTopic(String topic) {
        if (topic == null) {
            return "";
        }

        return topic.trim()
                .toLowerCase(Locale.ROOT);
    }

    private boolean isLearningNotFoundMessage(String message) {
        String cleanMessage = message.trim();

        return cleanMessage.contains("مازال ما تعلمتش")
                || cleanMessage.contains("ما لقيتش تعلم")
                || cleanMessage.contains("لا توجد")
                || cleanMessage.contains("غير موجود");
    }

    private String safeMessage(Exception exception) {
        if (exception == null) {
            return "خطأ غير معروف";
        }

        String message = exception.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}