package com.kamal.jarvis;

import android.content.Context;

import java.util.Locale;

public class CommandLearningEngine {

    private final Context context;

    private final MemoryManager memoryManager;

    private final LearningEngine learningEngine;

    public CommandLearningEngine(
            Context context
    ) {

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
    }

    // =========================================================
    // LEARN COMMAND
    // =========================================================

    public String learnCommand(
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

        String key =
                buildKey(
                        cleanCommand
                );

        memoryManager.saveMemory(
                key,
                cleanAction
        );

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

    public String findLearnedCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return null;
        }

        String cleanCommand =
                command.trim();

        String key =
                buildKey(
                        cleanCommand
                );

        String action =
                memoryManager.getMemory(
                        key
                );

        if (action == null ||
                action.trim().isEmpty()) {

            return null;
        }

        return action.trim();
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

        String action =
                findLearnedCommand(
                        command
                );

        return action != null &&
                !action.trim().isEmpty();
    }

    // =========================================================
    // FORGET COMMAND
    // =========================================================

    public String forgetCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return
                    "حدد الأمر اللي بغيتي نحيد.";
        }

        String cleanCommand =
                command.trim();

        String key =
                buildKey(
                        cleanCommand
                );

        memoryManager.removeMemory(
                key
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
    // BUILD MEMORY KEY
    // =========================================================

    private String buildKey(
            String command
    ) {

        return
                "__command_learning__"
                + normalize(
                        command
                );
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

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
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
                );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Command Learning Engine: ONLINE ✓";
        }

        return
                "Command Learning Engine: ERROR ⚠";
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            learningEngine
                    .getLearningStatus();

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
}