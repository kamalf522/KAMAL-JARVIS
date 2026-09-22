package com.kamal.jarvis;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KnowledgeEngine {

    private static final String PREFIX =
            "__knowledge__";

    private final Context context;
    private final MemoryManager memoryManager;
    private final LearningEngine learningEngine;
    private final SkillManager skillManager;

    public KnowledgeEngine(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "KnowledgeEngine context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        learningEngine =
                new LearningEngine(this.context);

        skillManager =
                new SkillManager(this.context);
    }

    // =========================================================
    // LEARN
    // =========================================================

    public synchronized String learn(
            String topic,
            String information
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return
                    "حدد الموضوع اللي بغيتي JARVIS يتعلمو.";
        }

        if (information == null ||
                information.trim().isEmpty()) {

            return
                    "عطيني المعلومة اللي بغيتي نخزن.";
        }

        String cleanTopic =
                normalizeTopic(topic);

        String cleanInformation =
                information.trim();

        if (cleanTopic.isEmpty()) {
            return "الموضوع غير صالح.";
        }

        try {

            String key =
                    createKnowledgeKey(
                            cleanTopic
                    );

            memoryManager.saveMemory(
                    key,
                    cleanInformation
            );

            String learningResult =
                    learningEngine.learn(
                            cleanTopic,
                            cleanInformation
                    );

            boolean learningSuccess =
                    learningResult != null
                            && learningResult
                            .contains("تم التعلم");

            return
                    "تم تعليم JARVIS بنجاح ✓\n\n"
                    + "الموضوع: "
                    + cleanTopic
                    + "\n"
                    + "المعلومة: "
                    + cleanInformation
                    + "\n\n"
                    + "Knowledge System: SAVED ✓\n"
                    + "Learning System: "
                    + (
                            learningSuccess
                                    ? "UPDATED ✓"
                                    : "SAVED"
                    );

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء حفظ المعرفة: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // REMEMBER
    // =========================================================

    public synchronized String remember(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return
                    "حدد الموضوع اللي بغيتي نبحث عليه.";
        }

        String cleanTopic =
                normalizeTopic(topic);

        try {

            String key =
                    createKnowledgeKey(
                            cleanTopic
                    );

            String information =
                    memoryManager.getMemory(key);

            if (information != null &&
                    !information.trim().isEmpty()) {

                return
                        "المعرفة المحفوظة ✓\n\n"
                        + "الموضوع: "
                        + cleanTopic
                        + "\n"
                        + information;
            }

            String learnedInformation =
                    learningEngine.rememberLearning(
                            cleanTopic
                    );

            if (isValidLearningResult(
                    learnedInformation
            )) {

                return
                        "المعرفة المتعلمة ✓\n\n"
                        + "الموضوع: "
                        + cleanTopic
                        + "\n"
                        + learnedInformation;
            }

            return
                    "ما عنديش معرفة محفوظة على: "
                    + cleanTopic;

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء البحث عن المعرفة: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public synchronized String search(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return
                    "حدد شنو بغيتي نقلب عليه.";
        }

        String cleanQuery =
                normalizeTopic(query);

        try {

            String memoryResults =
                    memoryManager.searchMemory(
                            cleanQuery
                    );

            String learningResults =
                    learningEngine.searchLearning(
                            cleanQuery
                    );

            boolean hasMemory =
                    memoryResults != null
                            && !memoryResults
                            .contains(
                                    "ما لقيتش ذاكرة"
                            )
                            && !memoryResults
                            .contains(
                                    "ما عنديش معلومات"
                            );

            boolean hasLearning =
                    learningResults != null
                            && !learningResults
                            .contains(
                                    "ما لقيتش تعلم"
                            )
                            && !learningResults
                            .contains(
                                    "مازال ما عنديش"
                            );

            if (!hasMemory &&
                    !hasLearning) {

                return
                        "ما لقيتش معرفة مرتبطة بـ: "
                        + query;
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "نتائج البحث فمعرفة JARVIS:\n\n"
            );

            if (hasMemory) {

                result.append(
                        "━━ الذاكرة ━━\n"
                );

                result.append(
                        memoryResults
                );

                result.append("\n\n");
            }

            if (hasLearning) {

                result.append(
                        "━━ التعلم ━━\n"
                );

                result.append(
                        learningResults
                );
            }

            return result.toString().trim();

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء البحث: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // HAS KNOWLEDGE
    // =========================================================

    public synchronized boolean hasKnowledge(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return false;
        }

        try {

            String cleanTopic =
                    normalizeTopic(topic);

            String key =
                    createKnowledgeKey(
                            cleanTopic
                    );

            if (memoryManager.hasMemory(key)) {
                return true;
            }

            String learnedInformation =
                    learningEngine.rememberLearning(
                            cleanTopic
                    );

            return isValidLearningResult(
                    learnedInformation
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // FORGET
    // =========================================================

    public synchronized String forget(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return
                    "حدد المعرفة اللي بغيتي نحيد.";
        }

        String cleanTopic =
                normalizeTopic(topic);

        try {

            String key =
                    createKnowledgeKey(
                            cleanTopic
                    );

            boolean existed =
                    memoryManager.hasMemory(key);

            memoryManager.removeMemory(key);

            if (existed) {

                return
                        "تم حذف المعرفة الأساسية ✓\n\n"
                        + cleanTopic
                        + "\n\n"
                        + "ملاحظة: سجل التعلم السابق "
                        + "مازال محفوظ فـ Learning Engine.";
            }

            return
                    "ما لقيتش معرفة أساسية محفوظة على: "
                    + cleanTopic;

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء حذف المعرفة: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // CREATE SKILL
    // =========================================================

    public synchronized String createKnowledgeSkill(
            String name,
            String description
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return "حدد اسم المهارة.";
        }

        String cleanName =
                name.trim();

        String cleanDescription =
                description == null ||
                        description.trim().isEmpty()
                        ? "Skill created from JARVIS knowledge."
                        : description.trim();

        try {

            boolean created =
                    skillManager.addSkill(
                            cleanName,
                            cleanDescription
                    );

            if (!created &&
                    skillManager.hasSkill(
                            cleanName
                    )) {

                return
                        "هاد المهارة موجودة من قبل ✓\n\n"
                        + cleanName;
            }

            if (!created) {

                return
                        "ما قدرتش ننشئ المهارة.";
            }

            return
                    "تم إنشاء المهارة من المعرفة ✓\n\n"
                    + "المهارة: "
                    + cleanName
                    + "\n"
                    + "الوصف: "
                    + cleanDescription;

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء إنشاء المهارة: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // IMPORT LEARNING
    // =========================================================

    public synchronized String importLearning(
            String topic
    ) {

        if (topic == null ||
                topic.trim().isEmpty()) {

            return "حدد موضوع التعلم.";
        }

        String cleanTopic =
                normalizeTopic(topic);

        try {

            String learning =
                    learningEngine.rememberLearning(
                            cleanTopic
                    );

            if (!isValidLearningResult(
                    learning
            )) {

                return
                        "ما لقيتش تعلم محفوظ على هاد الموضوع.";
            }

            return learning;

        } catch (Exception e) {

            return
                    "وقع خطأ أثناء استيراد التعلم: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // KNOWLEDGE REPORT
    // =========================================================

    public synchronized String getKnowledgeReport() {

        try {

            Map<String, ?> memories =
                    memoryManager.getAllMemoryMap();

            int knowledgeCount = 0;

            List<String> topics =
                    new ArrayList<>();

            for (String key :
                    memories.keySet()) {

                if (key.startsWith(PREFIX)) {

                    knowledgeCount++;

                    topics.add(
                            key.substring(
                                    PREFIX.length()
                            )
                    );
                }
            }

            String learningStatus =
                    learningEngine
                            .getLearningStatus();

            StringBuilder report =
                    new StringBuilder();

            report.append(
                    "=== KNOWLEDGE ENGINE ===\n\n"
            );

            report.append(
                    "المعارف الأساسية: "
            )
                    .append(
                            knowledgeCount
                    )
                    .append("\n");

            report.append(
                    "Knowledge Storage: ONLINE ✓\n"
            );

            report.append(
                    "Learning Engine: "
            )
                    .append(
                            learningEngine.isHealthy()
                                    ? "ONLINE ✓"
                                    : "ERROR ⚠"
                    )
                    .append("\n");

            report.append(
                    "Memory System: "
            )
                    .append(
                            memoryManager.isHealthy()
                                    ? "ONLINE ✓"
                                    : "ERROR ⚠"
                    )
                    .append("\n");

            report.append(
                    "Skill System: "
            )
                    .append(
                            skillManager.getSkillCount()
                    )
                    .append(" skills\n\n");

            if (!topics.isEmpty()) {

                report.append(
                        "المواضيع المحفوظة:\n"
                );

                for (int i = 0;
                     i < topics.size();
                     i++) {

                    report.append(
                            i + 1
                    )
                            .append(". ")
                            .append(
                                    topics.get(i)
                            )
                            .append("\n");
                }

                report.append("\n");
            }

            report.append(
                    "Learning Status:\n"
            );

            report.append(
                    learningStatus
            );

            return report.toString().trim();

        } catch (Exception e) {

            return
                    "Knowledge Report Error: "
                    + safeMessage(e);
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public synchronized String getStatus() {

        if (isHealthy()) {

            return
                    "Knowledge Engine: ONLINE ✓";
        }

        return
                "Knowledge Engine: ERROR ⚠";
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            if (!memoryManager.isHealthy()) {
                return false;
            }

            if (!learningEngine.isHealthy()) {
                return false;
            }

            skillManager.getSkillCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    public Context getContext() {
        return context;
    }

    // =========================================================
    // KEY
    // =========================================================

    private String createKnowledgeKey(
            String topic
    ) {

        return PREFIX
                + normalizeTopic(topic);
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalizeTopic(
            String topic
    ) {

        if (topic == null) {
            return "";
        }

        return topic
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    // =========================================================
    // LEARNING RESULT CHECK
    // =========================================================

    private boolean isValidLearningResult(
            String message
    ) {

        if (message == null ||
                message.trim().isEmpty()) {

            return false;
        }

        String cleanMessage =
                message.trim();

        return !isLearningNotFoundMessage(
                cleanMessage
        );
    }

    // =========================================================
    // NOT FOUND CHECK
    // =========================================================

    private boolean isLearningNotFoundMessage(
            String message
    ) {

        if (message == null) {
            return true;
        }

        String cleanMessage =
                message.trim();

        return
                cleanMessage.contains(
                        "مازال ما تعلمتش"
                )
                || cleanMessage.contains(
                        "ما لقيتش تعلم"
                )
                || cleanMessage.contains(
                        "مازال ما عنديش معلومات"
                )
                || cleanMessage.contains(
                        "لا توجد"
                )
                || cleanMessage.contains(
                        "غير موجود"
                );
    }

    // =========================================================
    // SAFE ERROR
    // =========================================================

    private String safeMessage(
            Exception exception
    ) {

        if (exception == null) {
            return "خطأ غير معروف";
        }

        String message =
                exception.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }
}