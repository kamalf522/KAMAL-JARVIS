package com.kamal.jarvis.v2.tools;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolRegistry {

    private final Map<String, ToolContract> tools =
            new LinkedHashMap<>();

    public synchronized JarvisResult<Void> register(
            ToolContract tool
    ) {
        if (tool == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool cannot be null",
                            "ToolRegistry"
                    )
            );
        }

        String id = tool.getId();

        if (id == null || id.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool id cannot be empty",
                            "ToolRegistry"
                    )
            );
        }

        id = id.trim();

        if (tools.containsKey(id)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Tool already registered: " + id,
                            "ToolRegistry"
                    )
            );
        }

        tools.put(id, tool);

        return JarvisResult.success(
                null,
                "Tool registered: " + id
        );
    }

    public synchronized JarvisResult<Void> replace(
            ToolContract tool
    ) {
        if (tool == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool cannot be null",
                            "ToolRegistry"
                    )
            );
        }

        String id = tool.getId();

        if (id == null || id.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool id cannot be empty",
                            "ToolRegistry"
                    )
            );
        }

        id = id.trim();
        tools.put(id, tool);

        return JarvisResult.success(
                null,
                "Tool registered or replaced: " + id
        );
    }

    public synchronized JarvisResult<ToolContract> get(
            String id
    ) {
        if (id == null || id.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool id cannot be empty",
                            "ToolRegistry"
                    )
            );
        }

        String normalizedId = id.trim();
        ToolContract tool = tools.get(normalizedId);

        if (tool == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_FOUND,
                            "Tool not found: " + normalizedId,
                            "ToolRegistry"
                    )
            );
        }

        return JarvisResult.success(tool);
    }

    public synchronized JarvisResult<Void> remove(
            String id
    ) {
        if (id == null || id.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool id cannot be empty",
                            "ToolRegistry"
                    )
            );
        }

        String normalizedId = id.trim();

        if (!tools.containsKey(normalizedId)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_FOUND,
                            "Tool not found: " + normalizedId,
                            "ToolRegistry"
                    )
            );
        }

        tools.remove(normalizedId);

        return JarvisResult.success(
                null,
                "Tool removed: " + normalizedId
        );
    }

    public synchronized boolean contains(
            String id
    ) {
        if (id == null) {
            return false;
        }

        return tools.containsKey(id.trim());
    }

    public synchronized boolean isAvailable(
            String id
    ) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        ToolContract tool = tools.get(id.trim());

        return tool != null && tool.isAvailable();
    }

    public synchronized int size() {
        return tools.size();
    }

    public synchronized int getToolCount() {
        return tools.size();
    }

    public synchronized boolean isEmpty() {
        return tools.isEmpty();
    }

    public synchronized List<String> getToolIds() {
        return Collections.unmodifiableList(
                new ArrayList<>(tools.keySet())
        );
    }

    public synchronized List<ToolContract> getTools() {
        return Collections.unmodifiableList(
                new ArrayList<>(tools.values())
        );
    }

    public synchronized List<String> getAvailableToolIds() {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, ToolContract> entry : tools.entrySet()) {
            ToolContract tool = entry.getValue();

            if (tool != null && tool.isAvailable()) {
                result.add(entry.getKey());
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized void clear() {
        tools.clear();
    }
}