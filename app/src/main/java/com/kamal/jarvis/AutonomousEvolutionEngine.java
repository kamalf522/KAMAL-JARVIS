package com.kamal.jarvis;

import android.content.Context;

public class ActionHistoryManager {

    private static final String HISTORY_KEY =
            "__jarvis_action_history__";

    private static final int MAX_ENTRIES = 200;

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

        if (isBlank(command)) {
            return;
        }

        String cleanCommand =
                clean(command);

        String cleanResult =
                clean(result);

        String entry =
                System.currentTimeMillis()
                        + " | COMMAND: "
                        + cleanCommand
                        + " | RESULT: "
                        + cleanResult;

        appendEntry(entry);
    }

    public synchronized void recordAction(
            String action
    ) {

        if (isBlank(action)) {
            return;
        }

        record(
                action,
                ""
        );
    }

    public synchronized void recordSuccess(
            String command,
            String result
    ) {

        if (isBlank(command)) {
            return;
        }

        record(
                command,
                "SUCCESS: " + clean(result)
        );
    }

    public synchronized void recordFailure(
            String command,
            String error
    ) {

        if (isBlank(command)) {
            return;
        }

        record(
                command,
                "FAILURE: " + clean(error)
        );
    }

    public synchronized String getHistory() {

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (isBlank(history)) {

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

        if (isBlank(history)) {

            return
                    "ما كاين حتى عملية مسجلة.";
        }

        String[] entries =
                history.split("\n");

        for (int i = entries.length - 1;
             i >= 0;
             i--) {

            if (!isBlank(entries[i])) {

                return
                        "آخر عملية:\n\n"
                        + entries[i];
            }
        }

        return
                "ما كاين حتى عملية مسجلة.";
    }

    public synchronized String getRecentActions(
            int count
    ) {

        if (count <= 0) {
            count = 1;
        }

        if (count > MAX_ENTRIES) {
            count = MAX_ENTRIES;
        }

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (isBlank(history)) {

            return
                    "ما كاين حتى سجل للعمليات.";
        }

        String[] entries =
                history.split("\n");

        StringBuilder result =
                new StringBuilder();

        int start =
                Math.max(
                        0,
                        entries.length - count
                );

        for (int i = start;
             i < entries.length;
             i++) {

            if (isBlank(entries[i])) {
                continue;
            }

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append(entries[i]);
        }

        return result.toString();
    }

    public synchronized String search(
            String query
    ) {

        if (isBlank(query)) {

            return
                    "خاصني كلمة أو أمر للبحث.";
        }

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (isBlank(history)) {

            return
                    "ما كاين حتى سجل للبحث فيه.";
        }

        String normalizedQuery =
                normalize(query);

        String[] entries =
                history.split("\n");

        StringBuilder result =
                new StringBuilder();

        int matches = 0;

        for (String entry : entries) {

            if (isBlank(entry)) {
                continue;
            }

            if (normalize(entry)
                    .contains(normalizedQuery)) {

                if (result.length() > 0) {
                    result.append("\n");
                }

                result.append(entry);
                matches++;
            }
        }

        if (matches == 0) {

            return
                    "ما لقيت حتى عملية مطابقة.";
        }

        return
                "نتائج البحث: "
                + matches
                + "\n\n"
                + result;
    }

    public synchronized int getHistoryCount() {

        String history =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        if (isBlank(history)) {
            return 0;
        }

        int count = 0;

        String[] entries =
                history.split("\n");

        for (String entry : entries) {

            if (!isBlank(entry)) {
                count++;
            }
        }

        return count;
    }

    public synchronized void clearHistory() {

        memoryManager.removeMemory(
                HISTORY_KEY
        );
    }

    public boolean isHealthy() {

        try {

            if (context == null ||
                    memoryManager == null) {

                return false;
            }

            memoryManager.getMemoryCount();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getStatus() {

        if (!isHealthy()) {

            return
                    "Action History Manager: ERROR ⚠";
        }

        return
                "Action History Manager: ONLINE ✓\n"
                + "Records: "
                + getHistoryCount();
    }

    private void appendEntry(
            String entry
    ) {

        String oldHistory =
                memoryManager.getMemory(
                        HISTORY_KEY
                );

        String newHistory;

        if (isBlank(oldHistory)) {

            newHistory = entry;

        } else {

            newHistory =
                    oldHistory
                            + "\n"
                            + entry;
        }

        newHistory =
                limitHistory(
                        newHistory
                );

        memoryManager.saveMemory(
                HISTORY_KEY,
                newHistory
        );
    }

    private String limitHistory(
            String history
    ) {

        if (isBlank(history)) {
            return "";
        }

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

            if (isBlank(entries[i])) {
                continue;
            }

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append(entries[i]);
        }

        return result.toString();
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("|", "/")
                .trim();
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي");
    }
}