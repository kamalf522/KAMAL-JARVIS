package com.kamal.jarvis;

import android.content.Context;

public class ActionHistoryManager {

    private static final String HISTORY_KEY =
            "__jarvis_action_history__";

    private static final int MAX_ENTRIES = 100;

    private final Context context;
    private final MemoryManager memoryManager;

    public ActionHistoryManager(Context context) {

        this.context =
                context.getApplicationContext();

        memoryManager =
                new MemoryManager(this.context);
    }

    public synchronized void record(
            String command,
            String result
    ) {

        if (command == null ||
                command.trim().isEmpty()) {

            return;
        }

        if (result == null) {
            result = "";
        }

        String oldHistory =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        String entry =
                System.currentTimeMillis()
                        + " | COMMAND: "
                        + clean(command)
                        + " | RESULT: "
                        + clean(result);

        String newHistory;

        if (oldHistory == null ||
                oldHistory.trim().isEmpty()) {

            newHistory = entry;

        } else {

            newHistory =
                    oldHistory
                            + "\n"
                            + entry;
        }

        newHistory =
                limitHistory(newHistory);

        memoryManager.saveMemory(
                HISTORY_KEY,
                newHistory
        );
    }

    // Compatibility method
    public synchronized void recordAction(
            String action
    ) {

        if (action == null ||
                action.trim().isEmpty()) {

            return;
        }

        record(
                action,
                ""
        );
    }

    public synchronized String getHistory() {

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (history == null ||
                history.trim().isEmpty()) {

            return
                    "مازال ما كاين حتى سجل للعمليات.";
        }

        return
                "JARVIS ACTION HISTORY\n"
                + "============================\n\n"
                + history;
    }

    public synchronized String getLastAction() {

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (history == null ||
                history.trim().isEmpty()) {

            return
                    "ما كاين حتى عملية مسجلة.";
        }

        String[] entries =
                history.split("\n");

        return
                "آخر عملية:\n\n"
                + entries[
                        entries.length - 1
                ];
    }

    public synchronized void clearHistory() {

        memoryManager.removeMemory(
                HISTORY_KEY
        );
    }

    public synchronized int getHistoryCount() {

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (history == null ||
                history.trim().isEmpty()) {

            return 0;
        }

        return history.split("\n").length;
    }

    public boolean isHealthy() {

        try {

            memoryManager.getMemoryCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (isHealthy()) {

            return
                    "Action History Manager: ONLINE ✓\n"
                    + "Records: "
                    + getHistoryCount();
        }

        return
                "Action History Manager: ERROR ⚠";
    }

    private String clean(
            String value
    ) {

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private String limitHistory(
            String history
    ) {

        String[] entries =
                history.split("\n");

        if (entries.length <= MAX_ENTRIES) {

            return history;
        }

        StringBuilder result =
                new StringBuilder();

        int start =
                entries.length
                        - MAX_ENTRIES;

        for (int i = start;
             i < entries.length;
             i++) {

            if (result.length() > 0) {

                result.append("\n");
            }

            result.append(
                    entries[i]
            );
        }

        return result.toString();
    }
}