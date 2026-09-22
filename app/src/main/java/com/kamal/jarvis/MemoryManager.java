package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MemoryManager {

    private static final String PREFS_NAME =
            "JARVIS_MEMORY";

    private final SharedPreferences preferences;

    public MemoryManager(Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "MemoryManager context cannot be null"
            );
        }

        Context appContext =
                context.getApplicationContext();

        preferences =
                appContext.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // =========================================================
    // SAVE
    // =========================================================

    public synchronized void saveMemory(
            String key,
            String value
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return;
        }

        String cleanValue =
                value == null
                        ? ""
                        : value.trim();

        preferences.edit()
                .putString(
                        cleanKey,
                        cleanValue
                )
                .apply();
    }

    // =========================================================
    // GET
    // =========================================================

    public synchronized String getMemory(
            String key
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return "";
        }

        return preferences.getString(
                cleanKey,
                ""
        );
    }

    // =========================================================
    // HAS
    // =========================================================

    public synchronized boolean hasMemory(
            String key
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return false;
        }

        return preferences.contains(
                cleanKey
        );
    }

    // =========================================================
    // REMOVE
    // =========================================================

    public synchronized void removeMemory(
            String key
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return;
        }

        preferences.edit()
                .remove(cleanKey)
                .apply();
    }

    // =========================================================
    // CLEAR
    // =========================================================

    public synchronized void clearAllMemories() {

        preferences.edit()
                .clear()
                .apply();
    }

    // =========================================================
    // COUNT
    // =========================================================

    public synchronized int getMemoryCount() {

        return preferences
                .getAll()
                .size();
    }

    // =========================================================
    // GET ALL
    // =========================================================

    public synchronized String getAllMemories() {

        Map<String, ?> memories =
                preferences.getAll();

        if (memories.isEmpty()) {

            return
                    "ما عنديش معلومات محفوظة حاليا.";
        }

        List<String> keys =
                new ArrayList<>(
                        memories.keySet()
                );

        Collections.sort(
                keys,
                String.CASE_INSENSITIVE_ORDER
        );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "المعلومات المحفوظة:\n\n"
        );

        for (String key : keys) {

            Object value =
                    memories.get(key);

            result.append("• ")
                    .append(key)
                    .append(" = ")
                    .append(
                            value == null
                                    ? ""
                                    : String.valueOf(value)
                    )
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // SEARCH BY KEY
    // =========================================================

    public synchronized String searchMemory(
            String query
    ) {

        String cleanQuery =
                normalizeSearch(query);

        if (cleanQuery.isEmpty()) {

            return
                    "خاصني كلمة أو معلومة نقلب عليها.";
        }

        Map<String, ?> memories =
                preferences.getAll();

        if (memories.isEmpty()) {

            return
                    "ما عنديش معلومات محفوظة حاليا.";
        }

        List<String> matches =
                new ArrayList<>();

        for (Map.Entry<String, ?> entry :
                memories.entrySet()) {

            String key =
                    entry.getKey();

            Object rawValue =
                    entry.getValue();

            String value =
                    rawValue == null
                            ? ""
                            : String.valueOf(rawValue);

            String searchable =
                    (
                            key
                                    + " "
                                    + value
                    ).toLowerCase(
                            Locale.ROOT
                    );

            if (searchable.contains(
                    cleanQuery
            )) {

                matches.add(
                        key
                                + " = "
                                + value
                );
            }
        }

        if (matches.isEmpty()) {

            return
                    "ما لقيتش ذاكرة مرتبطة بـ: "
                            + query;
        }

        Collections.sort(
                matches,
                String.CASE_INSENSITIVE_ORDER
        );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "لقيت هاد الذكريات:\n\n"
        );

        for (String match : matches) {

            result.append("• ")
                    .append(match)
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // SEARCH KEYS
    // =========================================================

    public synchronized List<String> searchKeys(
            String query
    ) {

        String cleanQuery =
                normalizeSearch(query);

        List<String> result =
                new ArrayList<>();

        if (cleanQuery.isEmpty()) {
            return result;
        }

        for (String key :
                preferences.getAll().keySet()) {

            if (key.toLowerCase(
                    Locale.ROOT
            ).contains(cleanQuery)) {

                result.add(key);
            }
        }

        Collections.sort(
                result,
                String.CASE_INSENSITIVE_ORDER
        );

        return result;
    }

    // =========================================================
    // GET ALL KEYS
    // =========================================================

    public synchronized List<String> getMemoryKeys() {

        List<String> keys =
                new ArrayList<>(
                        preferences
                                .getAll()
                                .keySet()
                );

        Collections.sort(
                keys,
                String.CASE_INSENSITIVE_ORDER
        );

        return keys;
    }

    // =========================================================
    // GET ALL VALUES
    // =========================================================

    public synchronized List<String> getMemoryValues() {

        List<String> values =
                new ArrayList<>();

        for (Object value :
                preferences
                        .getAll()
                        .values()) {

            if (value != null) {

                values.add(
                        String.valueOf(value)
                );
            }
        }

        return values;
    }

    // =========================================================
    // MEMORY SNAPSHOT
    // =========================================================

    public synchronized Map<String, ?> getAllMemoryMap() {

        return preferences.getAll();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public synchronized boolean updateMemory(
            String key,
            String value
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return false;
        }

        if (!preferences.contains(
                cleanKey
        )) {

            return false;
        }

        String cleanValue =
                value == null
                        ? ""
                        : value.trim();

        preferences.edit()
                .putString(
                        cleanKey,
                        cleanValue
                )
                .apply();

        return true;
    }

    // =========================================================
    // APPEND
    // =========================================================

    public synchronized boolean appendMemory(
            String key,
            String value
    ) {

        String cleanKey =
                normalizeKey(key);

        if (cleanKey.isEmpty()) {
            return false;
        }

        String cleanValue =
                value == null
                        ? ""
                        : value.trim();

        if (cleanValue.isEmpty()) {
            return false;
        }

        String oldValue =
                preferences.getString(
                        cleanKey,
                        ""
                );

        String newValue;

        if (oldValue == null ||
                oldValue.trim().isEmpty()) {

            newValue =
                    cleanValue;

        } else {

            newValue =
                    oldValue.trim()
                            + "\n"
                            + cleanValue;
        }

        preferences.edit()
                .putString(
                        cleanKey,
                        newValue
                )
                .apply();

        return true;
    }

    // =========================================================
    // COPY
    // =========================================================

    public synchronized boolean copyMemory(
            String sourceKey,
            String targetKey
    ) {

        String source =
                normalizeKey(sourceKey);

        String target =
                normalizeKey(targetKey);

        if (source.isEmpty() ||
                target.isEmpty()) {

            return false;
        }

        if (!preferences.contains(
                source
        )) {

            return false;
        }

        String value =
                preferences.getString(
                        source,
                        ""
                );

        preferences.edit()
                .putString(
                        target,
                        value
                )
                .apply();

        return true;
    }

    // =========================================================
    // RENAME
    // =========================================================

    public synchronized boolean renameMemory(
            String oldKey,
            String newKey
    ) {

        String source =
                normalizeKey(oldKey);

        String target =
                normalizeKey(newKey);

        if (source.isEmpty() ||
                target.isEmpty()) {

            return false;
        }

        if (!preferences.contains(
                source
        )) {

            return false;
        }

        String value =
                preferences.getString(
                        source,
                        ""
                );

        preferences.edit()
                .putString(
                        target,
                        value
                )
                .remove(source)
                .apply();

        return true;
    }

    // =========================================================
    // EXPORT
    // =========================================================

    public synchronized String exportMemory() {

        Map<String, ?> memories =
                preferences.getAll();

        if (memories.isEmpty()) {

            return "";
        }

        List<String> keys =
                new ArrayList<>(
                        memories.keySet()
                );

        Collections.sort(
                keys,
                String.CASE_INSENSITIVE_ORDER
        );

        StringBuilder result =
                new StringBuilder();

        for (String key : keys) {

            Object value =
                    memories.get(key);

            if (result.length() > 0) {
                result.append("\n");
            }

            result.append(key)
                    .append("=")
                    .append(
                            value == null
                                    ? ""
                                    : String.valueOf(value)
                    );
        }

        return result.toString();
    }

    // =========================================================
    // MEMORY STATUS
    // =========================================================

    public synchronized String getStatus() {

        int count =
                getMemoryCount();

        if (count == 0) {

            return
                    "Memory Manager: ONLINE ✓\n"
                            + "Memories: 0";
        }

        return
                "Memory Manager: ONLINE ✓\n"
                        + "Memories: "
                        + count;
    }

    // =========================================================
    // HEALTH
    // =========================================================

    public synchronized boolean isHealthy() {

        try {

            preferences.getAll();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // NORMALIZE KEY
    // =========================================================

    private String normalizeKey(
            String key
    ) {

        if (key == null) {
            return "";
        }

        return key
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    // =========================================================
    // NORMALIZE SEARCH
    // =========================================================

    private String normalizeSearch(
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
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}