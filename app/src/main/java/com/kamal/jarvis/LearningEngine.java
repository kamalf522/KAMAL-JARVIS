package com.kamal.jarvis;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LearningEngine {

    private static final String LEARNING_PREFIX =
            "__learning__";

    private static final String LEARNING_INDEX =
            "__learning_index__";

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

    // =========================================================
    // LEARN
    // =========================================================

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
                normalizeSubject(subject);

        String cleanInformation =
                information.trim();

        if (cleanSubject.isEmpty()) {
            return "الموضوع غير صالح.";
        }

        String timestamp =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.ROOT
                ).format(new Date());

        String key =
                LEARNING_PREFIX
                        + cleanSubject;

        String previous =
                memoryManager.getMemory(key);

        StringBuilder stored =
                new StringBuilder();

        if (previous != null &&
                !previous.trim().isEmpty()) {

            stored.append(previous.trim())
                    .append("\n");
        }

        stored.append("[")
                .append(timestamp)
                .append("] ")
                .append(cleanInformation);

        memoryManager.saveMemory(
                key,
                stored.toString()
        );

        addToLearningIndex(
                cleanSubject
        );

        return
                "تم التعلم ✓\n\n"
                + "الموضوع: "
                + cleanSubject
                + "\n"
                + "المعلومة تسجلات فذاكرة JARVIS."
                + "\n"
                + "عدد المعلومات المحفوظة: "
                + countFacts(stored.toString());
    }

    // =========================================================
    // REMEMBER LEARNING
    // =========================================================

    public String rememberLearning(
            String subject
    ) {

        if (subject == null ||
                subject.trim().isEmpty()) {

            return "حدد شنو بغيتي نرجع نتذكر.";
        }

        String cleanSubject =
                normalizeSubject(subject);

        String key =
                LEARNING_PREFIX
                        + cleanSubject;

        String information =
                memoryManager.getMemory(key);

        if (information == null ||
                information.trim().isEmpty()) {

            return
                    "مازال ما تعلمتش هاد المعلومة.";
        }

        return
                "المعلومات اللي تعلمتها على: "
                + cleanSubject
                + "\n\n"
                + information;
    }

    // =========================================================
    // SEARCH LEARNING
    // =========================================================

    public String searchLearning(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return "حدد شنو بغيتي نقلب عليه فالتعلم.";
        }

        String cleanQuery =
                normalizeSubject(query);

        List<String> subjects =
                getLearningSubjects();

        if (subjects.isEmpty()) {

            return
                    "مازال ما عنديش معلومات متعلمة.";
        }

        StringBuilder result =
                new StringBuilder();

        int matches = 0;

        for (String subject : subjects) {

            if (subject.contains(cleanQuery) ||
                    cleanQuery.contains(subject)) {

                String information =
                        memoryManager.getMemory(
                                LEARNING_PREFIX + subject
                        );

                if (information == null ||
                        information.trim().isEmpty()) {
                    continue;
                }

                if (matches == 0) {

                    result.append(
                            "لقيت هاد المعلومات:\n\n"
                    );
                }

                result.append("• ")
                        .append(subject)
                        .append("\n")
                        .append(information)
                        .append("\n\n");

                matches++;
            }
        }

        if (matches == 0) {

            return
                    "ما لقيتش تعلم مرتبط بـ: "
                    + query;
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // CREATE SKILL
    // =========================================================

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
                description == null ||
                        description.trim().isEmpty()
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

    // =========================================================
    // LEARNING SUBJECTS
    // =========================================================

    public String getLearningSubjectsText() {

        List<String> subjects =
                getLearningSubjects();

        if (subjects.isEmpty()) {

            return
                    "مازال ما تسجل حتى موضوع للتعلم.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "المواضيع اللي تعلمها JARVIS:\n\n"
        );

        for (int i = 0;
             i < subjects.size();
             i++) {

            result.append(i + 1)
                    .append(". ")
                    .append(subjects.get(i))
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // LEARNING STATUS
    // =========================================================

    public String getLearningStatus() {

        try {

            int memories =
                    memoryManager.getMemoryCount();

            int skills =
                    skillManager.getSkillCount();

            int learnedSubjects =
                    getLearningSubjects().size();

            int learnedFacts =
                    countAllFacts();

            return
                    "LEARNING ENGINE\n"
                    + "============================\n\n"
                    + "Memory entries: "
                    + memories
                    + "\n"
                    + "Known skills: "
                    + skills
                    + "\n"
                    + "Learned subjects: "
                    + learnedSubjects
                    + "\n"
                    + "Learned facts: "
                    + learnedFacts
                    + "\n\n"
                    + "Learning Engine: ONLINE ✓";

        } catch (Exception e) {

            return
                    "Learning Engine: ERROR ⚠\n"
                    + safeError(e);
        }
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            skillManager.getSkillCount();

            getLearningSubjects();

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

    // =========================================================
    // INDEX MANAGEMENT
    // =========================================================

    private void addToLearningIndex(
            String subject
    ) {

        String raw =
                memoryManager.getMemory(
                        LEARNING_INDEX
                );

        List<String> subjects =
                parseIndex(raw);

        if (!subjects.contains(subject)) {

            subjects.add(subject);
        }

        memoryManager.saveMemory(
                LEARNING_INDEX,
                joinIndex(subjects)
        );
    }

    private List<String> getLearningSubjects() {

        String raw =
                memoryManager.getMemory(
                        LEARNING_INDEX
                );

        return parseIndex(raw);
    }

    private List<String> parseIndex(
            String raw
    ) {

        List<String> result =
                new ArrayList<>();

        if (raw == null ||
                raw.trim().isEmpty()) {

            return result;
        }

        String[] parts =
                raw.split("\\|");

        for (String part : parts) {

            String value =
                    part.trim();

            if (!value.isEmpty() &&
                    !result.contains(value)) {

                result.add(value);
            }
        }

        return result;
    }

    private String joinIndex(
            List<String> subjects
    ) {

        StringBuilder result =
                new StringBuilder();

        for (String subject : subjects) {

            if (result.length() > 0) {
                result.append("|");
            }

            result.append(
                    subject.replace("|", " ")
            );
        }

        return result.toString();
    }

    // =========================================================
    // COUNT FACTS
    // =========================================================

    private int countFacts(
            String information
    ) {

        if (information == null ||
                information.trim().isEmpty()) {

            return 0;
        }

        String[] lines =
                information.split("\\n");

        int count = 0;

        for (String line : lines) {

            if (!line.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private int countAllFacts() {

        int total = 0;

        List<String> subjects =
                getLearningSubjects();

        for (String subject : subjects) {

            String information =
                    memoryManager.getMemory(
                            LEARNING_PREFIX + subject
                    );

            total += countFacts(information);
        }

        return total;
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeSubject(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replaceAll("\\s+", " ");
    }

    // =========================================================
    // ERROR
    // =========================================================

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