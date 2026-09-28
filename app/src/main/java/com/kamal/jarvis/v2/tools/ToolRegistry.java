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
                            "Tool cannot be null.",
                            "ToolRegistry"
                    )
            );
        }

        String id = normalize(tool.getId());

        if (id.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Tool ID cannot be empty.",
                            "ToolRegistry"
                    )
            );
        }

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
                            "Tool cannot be null.",
                            "ToolRegistry"
                    )
            );
        }

        String id = normalize(tool.getId());

        if (id.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Tool ID cannot be empty.",
                            "ToolRegistry"
                    )
            );
        }

        tools.put(id, tool);

        return JarvisResult.success(
                null,
                "Tool registered or replaced: " + id
        );
    }

    public synchronized JarvisResult<ToolContract> get(
            String toolId
    ) {
        String id = normalize(toolId);

        if (id.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool ID cannot be empty.",
                            "ToolRegistry"
                    )
            );
        }

        ToolContract tool = tools.get(id);

        if (tool == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_FOUND,
                            "Tool not found: " + id,
                            "ToolRegistry"
                    )
            );
        }

        return JarvisResult.success(
                tool,
                "Tool found: " + id
        );
    }

    public synchronized JarvisResult<Void> remove(
            String toolId
    ) {
        String id = normalize(toolId);

        if (id.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Tool ID cannot be empty.",
                            "ToolRegistry"
                    )
            );
        }

        if (!tools.containsKey(id)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_FOUND,
                            "Tool not found: " + id,
                            "ToolRegistry"
                    )
            );
        }

        tools.remove(id);

        return JarvisResult.success(
                null,
                "Tool removed: " + id
        );
    }

    public synchronized boolean contains(
            String toolId
    ) {
        String id = normalize(toolId);

        return !id.isEmpty() && tools.containsKey(id);
    }

    public synchronized boolean isAvailable(
            String toolId
    ) {
        String id = normalize(toolId);

        ToolContract tool = tools.get(id);

        return tool != null && tool.isAvailable();
    }

    public synchronized List<String> getToolIds() {
        return Collections.unmodifiableList(
                new ArrayList<>(tools.keySet())
        );
    }

    public synchronized List<String> getAvailableToolIds() {
        List<String> available = new ArrayList<>();

        for (Map.Entry<String, ToolContract> entry
                : tools.entrySet()) {

            ToolContract tool = entry.getValue();

            if (tool != null && tool.isAvailable()) {
                available.add(entry.getKey());
            }
        }

        return Collections.unmodifiableList(available);
    }

    public synchronized int size() {
        return tools.size();
    }

    public synchronized void clear() {
        tools.clear();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }
}