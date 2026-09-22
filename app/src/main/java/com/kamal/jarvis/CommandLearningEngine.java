package com.kamal.jarvis;

import android.content.Context;

import java.util.Locale;

public class CommandLearningEngine {

    private static final String PREFIX =
            "__command_learning__";

    private static final String META_PREFIX =
            "__command_learning_meta__";

    private static final String KEY_INDEX =
            "__command_learning_index__";

    private final Context context;

    private final MemoryManager memoryManager;

    private final LearningEngine learningEngine;

    private final SkillManager skillManager;

    public CommandLearningEngine(
            Context context
    ) {

        if (context == null) {

            throw new IllegalArgumentException(
                    "CommandLearningEngine context cannot be null"
            );
        }

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(
                        this.context
                );

        learningEngine =
                new LearningEngine(
                        this.context
                );

        skillManager =
                new SkillManager(
                        this.context
                );

        initialize();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initialize() {

        try {

            if (memoryManager.getMemory(
                    KEY_INDEX
            ) == null) {

                memoryManager.saveMemory(
                        KEY_INDEX,
                        ""
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // LEARN COMMAND
    // =========================================================

    public synchronized String learnCommand(
            String command,
            String action
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "خاصك تعطيني الأمر اللي بغيتي JARVIS يتعلم.";
        }

        if (action == null ||
                action.trim().isEmpty()) {

            return
                    "خاصك تحدد شنو خاص JARVIS يدير ملي يسمع الأمر.";
        }

        String cleanCommand =
                command.trim();

        String cleanAction =
                action.trim();

        String normalized =
                normalize(cleanCommand);

        String key =
                buildKey(normalized);

        memoryManager.saveMemory(
                key,
                cleanAction
        );

        initializeMeta(
                normalized
        );

        addToIndex(
                normalized
        );

        try {

            learningEngine.learn(
                    "command:" + cleanCommand,
                    "action:" + cleanAction
            );

        } catch (Exception ignored) {
        }

        return
                "تم تعلم الأمر ✓\n\n"
                + "الأمر: "
                + cleanCommand
                + "\n"
                + "الإجراء: "
                + cleanAction;
    }

    // =========================================================
    // FIND LEARNED COMMAND
    // =========================================================

    public synchronized String findLearnedCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return null;
        }

        String cleanCommand =
                command.trim();

        String normalized =
                normalize(cleanCommand);

        String exact =
                memoryManager.getMemory(
                        buildKey(normalized)
                );

        if (exact != null &&
                !exact.trim().isEmpty()) {

            recordUsage(
                    normalized
            );

            return exact.trim();
        }

        /*
         * محاولة البحث عن أمر قريب
         * باستعمال الكلمات الأساسية.
         */
        String index =
                memoryManager.getMemory(
                        KEY_INDEX
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return null;
        }

        String[] commands =
                index.split("\\|");

        for (String storedCommand :
                commands) {

            if (storedCommand == null ||
                    storedCommand.trim().isEmpty()) {

                continue;
            }

            String candidate =
                    storedCommand.trim();

            if (isSimilarCommand(
                    normalized,
                    candidate
            )) {

                String action =
                        memoryManager.getMemory(
                                buildKey(candidate)
                        );

                if (action != null &&
                        !action.trim().isEmpty()) {

                    recordUsage(
                            candidate
                    );

                    return action.trim();
                }
            }
        }

        return null;
    }

    // =========================================================
    // CHECK IF COMMAND IS LEARNED
    // =========================================================

    public boolean isCommandLearned(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return false;
        }

        String normalized =
                normalize(command);

        String action =
                memoryManager.getMemory(
                        buildKey(normalized)
                );

        return action != null &&
                !action.trim().isEmpty();
    }

    // =========================================================
    // FORGET COMMAND
    // =========================================================

    public synchronized String forgetCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "حدد الأمر اللي بغيتي نحيد.";
        }

        String cleanCommand =
                command.trim();

        String normalized =
                normalize(cleanCommand);

        memoryManager.removeMemory(
                buildKey(normalized)
        );

        memoryManager.removeMemory(
                buildMetaKey(normalized)
        );

        removeFromIndex(
                normalized
        );

        return
                "تم نسيان الأمر ✓\n\n"
                + cleanCommand;
    }

    // =========================================================
    // GET LEARNED ACTION
    // =========================================================

    public String getLearnedAction(
            String command
    ) {

        String action =
                findLearnedCommand(
                        command
                );

        if (action == null ||
                action.trim().isEmpty()) {

            return
                    "هاد الأمر مازال ما تعلموش JARVIS.";
        }

        return
                "الإجراء المتعلم:\n"
                + action;
    }

    // =========================================================
    // RECORD SUCCESS
    // =========================================================

    public synchronized void recordSuccess(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String normalized =
                normalize(command);

        CommandStats stats =
                getStats(normalized);

        stats.success++;

        saveStats(
                normalized,
                stats
        );
    }

    // =========================================================
    // RECORD FAILURE
    // =========================================================

    public synchronized void recordFailure(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String normalized =
                normalize(command);

        CommandStats stats =
                getStats(normalized);

        stats.failure++;

        saveStats(
                normalized,
                stats
        );
    }

    // =========================================================
    // RECORD RESULT
    // =========================================================

    public synchronized void recordResult(
            String command,
            boolean success
    ) {

        if (success) {

            recordSuccess(
                    command
            );

        } else {

            recordFailure(
                    command
            );
        }
    }

    // =========================================================
    // GET USAGE COUNT
    // =========================================================

    public int getUsageCount(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return 0;
        }

        return getStats(
                normalize(command)
        ).usage;
    }

    // =========================================================
    // GET SUCCESS COUNT
    // =========================================================

    public int getSuccessCount(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return 0;
        }

        return getStats(
                normalize(command)
        ).success;
    }

    // =========================================================
    // GET FAILURE COUNT
    // =========================================================

    public int getFailureCount(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return 0;
        }

        return getStats(
                normalize(command)
        ).failure;
    }

    // =========================================================
    // SUCCESS RATE
    // =========================================================

    public float getSuccessRate(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return 0f;
        }

        CommandStats stats =
                getStats(
                        normalize(command)
                );

        int total =
                stats.success +
                stats.failure;

        if (total <= 0) {

            return 0f;
        }

        return
                ((float) stats.success /
                        (float) total)
                        * 100f;
    }

    // =========================================================
    // GET LEARNED COMMAND COUNT
    // =========================================================

    public int getLearnedCommandCount() {

        String index =
                memoryManager.getMemory(
                        KEY_INDEX
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return 0;
        }

        int count = 0;

        String[] items =
                index.split("\\|");

        for (String item :
                items) {

            if (item != null &&
                    !item.trim().isEmpty()) {

                count++;
            }
        }

        return count;
    }

    // =========================================================
    // GET LEARNED COMMANDS
    // =========================================================

    public String getLearnedCommands() {

        String index =
                memoryManager.getMemory(
                        KEY_INDEX
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return
                    "مازال ما تعلم حتى أمر.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "=== LEARNED COMMANDS ===\n\n"
        );

        String[] items =
                index.split("\\|");

        int number = 1;

        for (String item :
                items) {

            if (item == null ||
                    item.trim().isEmpty()) {

                continue;
            }

            String command =
                    item.trim();

            String action =
                    memoryManager.getMemory(
                            buildKey(command)
                    );

            result.append(
                    number++
            );

            result.append(
                    ". "
            );

            result.append(
                    command
            );

            result.append(
                    "\n   → "
            );

            result.append(
                    action == null
                            ? "غير معروف"
                            : action
            );

            result.append(
                    "\n"
            );
        }

        return result.toString();
    }

    // =========================================================
    // LEARNING REPORT
    // =========================================================

    public String getLearningReport(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "حدد الأمر اللي بغيتي التقرير ديالو.";
        }

        String normalized =
                normalize(command);

        String action =
                memoryManager.getMemory(
                        buildKey(normalized)
                );

        CommandStats stats =
                getStats(normalized);

        StringBuilder report =
                new StringBuilder();

        report.append(
                "=== COMMAND LEARNING ===\n\n"
        );

        report.append(
                "الأمر: "
        );

        report.append(
                command.trim()
        );

        report.append(
                "\n\nالإجراء: "
        );

        report.append(
                action == null
                        ? "مازال ما متعلمش"
                        : action
        );

        report.append(
                "\n\nمرات الاستعمال: "
        );

        report.append(
                stats.usage
        );

        report.append(
                "\nالنجاحات: "
        );

        report.append(
                stats.success
        );

        report.append(
                "\nالفشل: "
        );

        report.append(
                stats.failure
        );

        report.append(
                "\nنسبة النجاح: "
        );

        report.append(
                String.format(
                        Locale.ROOT,
                        "%.1f%%",
                        getSuccessRate(command)
                )
        );

        return report.toString();
    }

    // =========================================================
    // BUILD MEMORY KEY
    // =========================================================

    private String buildKey(
            String command
    ) {

        return
                PREFIX
                + normalize(command);
    }

    // =========================================================
    // BUILD META KEY
    // =========================================================

    private String buildMetaKey(
            String command
    ) {

        return
                META_PREFIX
                + normalize(command);
    }

    // =========================================================
    // NORMALIZE COMMAND
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {

            return "";
        }

        String clean =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        clean =
                clean
                        .replace(
                                "أ",
                                "ا"
                        )
                        .replace(
                                "إ",
                                "ا"
                        )
                        .replace(
                                "آ",
                                "ا"
                        )
                        .replace(
                                "ة",
                                "ه"
                        )
                        .replace(
                                "ى",
                                "ي"
                        );

        clean =
                clean
                        .replace(
                                "؟",
                                " "
                        )
                        .replace(
                                "?",
                                " "
                        )
                        .replace(
                                "!",
                                " "
                        )
                        .replace(
                                "،",
                                " "
                        )
                        .replace(
                                ",",
                                " "
                        );

        while (clean.contains("  ")) {

            clean =
                    clean.replace(
                            "  ",
                            " "
                    );
        }

        return clean.trim();
    }

    // =========================================================
    // SIMILAR COMMAND
    // =========================================================

    private boolean isSimilarCommand(
            String first,
            String second
    ) {

        if (first == null ||
                second == null) {

            return false;
        }

        if (first.equals(second)) {

            return true;
        }

        String[] firstWords =
                first.split("\\s+");

        String[] secondWords =
                second.split("\\s+");

        if (firstWords.length == 0 ||
                secondWords.length == 0) {

            return false;
        }

        int matches = 0;

        for (String word :
                firstWords) {

            if (word.length() < 2) {

                continue;
            }

            for (String other :
                    secondWords) {

                if (word.equals(other)) {

                    matches++;

                    break;
                }
            }
        }

        int usefulWords =
                Math.min(
                        firstWords.length,
                        secondWords.length
                );

        if (usefulWords <= 1) {

            return matches >= 1;
        }

        return
                matches >= 2 ||
                ((float) matches /
                        (float) usefulWords) >= 0.60f;
    }

    // =========================================================
    // ADD TO INDEX
    // =========================================================

    private void addToIndex(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String normalized =
                normalize(command);

        String index =
                memoryManager.getMemory(
                        KEY_INDEX
                );

        if (index == null) {

            index = "";
        }

        String[] items =
                index.split("\\|");

        for (String item :
                items) {

            if (normalized.equals(
                    item.trim()
            )) {

                return;
            }
        }

        if (index.trim().isEmpty()) {

            index =
                    normalized;

        } else {

            index =
                    index
                    + "|"
                    + normalized;
        }

        memoryManager.saveMemory(
                KEY_INDEX,
                index
        );
    }

    // =========================================================
    // REMOVE FROM INDEX
    // =========================================================

    private void removeFromIndex(
            String command
    ) {

        String index =
                memoryManager.getMemory(
                        KEY_INDEX
                );

        if (index == null ||
                index.trim().isEmpty()) {

            return;
        }

        StringBuilder result =
                new StringBuilder();

        String[] items =
                index.split("\\|");

        for (String item :
                items) {

            if (item == null ||
                    item.trim().isEmpty()) {

                continue;
            }

            String current =
                    item.trim();

            if (current.equals(
                    command
            )) {

                continue;
            }

            if (result.length() > 0) {

                result.append("|");
            }

            result.append(
                    current
            );
        }

        memoryManager.saveMemory(
                KEY_INDEX,
                result.toString()
        );
    }

    // =========================================================
    // INITIALIZE META
    // =========================================================

    private void initializeMeta(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        String key =
                buildMetaKey(command);

        if (memoryManager.getMemory(
                key
        ) == null) {

            memoryManager.saveMemory(
                    key,
                    "0|0|0"
            );
        }
    }

    // =========================================================
    // RECORD USAGE
    // =========================================================

    private void recordUsage(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        CommandStats stats =
                getStats(command);

        stats.usage++;

        saveStats(
                command,
                stats
        );
    }

    // =========================================================
    // GET STATS
    // =========================================================

    private CommandStats getStats(
            String command
    ) {

        CommandStats stats =
                new CommandStats();

        if (command == null ||
                command.trim().isEmpty()) {

            return stats;
        }

        try {

            String stored =
                    memoryManager.getMemory(
                            buildMetaKey(
                                    command
                            )
                    );

            if (stored == null ||
                    stored.trim().isEmpty()) {

                return stats;
            }

            String[] values =
                    stored.split("\\|");

            if (values.length > 0) {

                stats.usage =
                        parseInt(
                                values[0]
                        );
            }

            if (values.length > 1) {

                stats.success =
                        parseInt(
                                values[1]
                        );
            }

            if (values.length > 2) {

                stats.failure =
                        parseInt(
                                values[2]
                        );
            }

        } catch (Exception ignored) {
        }

        return stats;
    }

    // =========================================================
    // SAVE STATS
    // =========================================================

    private void saveStats(
            String command,
            CommandStats stats
    ) {

        if (command == null ||
                command.trim().isEmpty() ||
                stats == null) {

            return;
        }

        memoryManager.saveMemory(
                buildMetaKey(command),
                stats.usage
                        + "|"
                        + stats.success
                        + "|"
                        + stats.failure
        );
    }

    // =========================================================
    // PARSE INT
    // =========================================================

    private int parseInt(
            String value
    ) {

        try {

            return Math.max(
                    0,
                    Integer.parseInt(
                            value.trim()
                    )
            );

        } catch (Exception e) {

            return 0;
        }
   