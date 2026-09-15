package com.kamal.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class MemoryManager {

    private static final String PREFS_NAME = "JARVIS_MEMORY";

    private final SharedPreferences preferences;

    public MemoryManager(Context context) {

        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveMemory(
            String key,
            String value
    ) {

        if (key == null || key.trim().isEmpty()) {
            return;
        }

        if (value == null) {
            value = "";
        }

        preferences.edit()
                .putString(
                        normalizeKey(key),
                        value.trim()
                )
                .apply();
    }

    public String getMemory(
            String key
    ) {

        if (key == null || key.trim().isEmpty()) {
            return "";
        }

        return preferences.getString(
                normalizeKey(key),
                ""
        );
    }

    public boolean hasMemory(
            String key
    ) {

        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        return preferences.contains(
                normalizeKey(key)
        );
    }

    public void removeMemory(
            String key
    ) {

        if (key == null || key.trim().isEmpty()) {
            return;
        }

        preferences.edit()
                .remove(
                        normalizeKey(key)
                )
                .apply();
    }

    public void clearAllMemories() {

        preferences.edit()
                .clear()
                .apply();
    }

    public int getMemoryCount() {

        return preferences.getAll().size();
    }

    public String getAllMemories() {

        Map<String, ?> memories =
                preferences.getAll();

        if (memories.isEmpty()) {

            return "ما عنديش معلومات محفوظة حاليا.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "المعلومات المحفوظة:\n\n"
        );

        for (Map.Entry<String, ?> entry :
                memories.entrySet()) {

            result.append("• ")
                    .append(entry.getKey())
                    .append(" = ")
                    .append(entry.getValue())
                    .append("\n");
        }

        return result.toString().trim();
    }

    private String normalizeKey(
            String key
    ) {

        return key
                .trim()
                .toLowerCase();
    }
}