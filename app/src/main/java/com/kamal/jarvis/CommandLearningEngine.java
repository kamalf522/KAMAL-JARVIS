package com.kamal.jarvis;

import android.content.Context;

public class CommandLearningEngine {

    private final Context context;
    private final MemoryManager memoryManager;
    private final LearningEngine learningEngine;

    public CommandLearningEngine(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);

        learningEngine =
                new LearningEngine(this.context);
    }

    public String learnCommand(
            String command,
            String action
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "خاصك تعطيني الأمر اللي بغيتي JARVIS يتعلم.";
        }

        if (action == null ||
                action.trim().isEmpty()) {

            return "خاصك تحدد شنو خاص JARVIS يدير ملي يسمع الأمر.";
        }

        String cleanCommand =
                command.trim();

        String cleanAction =
                action.trim();

        String key =
                "__command_learning__"
                        + cleanCommand
                        .toLowerCase();

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

    public String findLearnedCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "ما عطيتيني حتى أمر.";
        }

        String key =
                "__command_learning__"
                        + command.trim()
                        .toLowerCase();

        String action =
                memoryManager.getMemory(key);

        if (action == null ||
                action.trim().isEmpty()) {

            return
                    "JARVIS مازال ما تعلمش هاد الأمر.";
        }

        return
                "أمر معروف ✓\n\n"
                + "الإجراء المحفوظ:\n"
                + action;
    }

    public String forgetCommand(
            String command
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return "حدد الأمر اللي بغيتي نحيد.";
        }

        String key =
                "__command_learning__"
                        + command.trim()
                        .toLowerCase();

        memoryManager.removeMemory(key);

        return
                "تم نسيان الأمر ✓";
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Command Learning Engine: ONLINE ✓";
        }

        return
                "Command Learning Engine: ERROR ⚠";
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            learningEngine.getLearningStatus();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}