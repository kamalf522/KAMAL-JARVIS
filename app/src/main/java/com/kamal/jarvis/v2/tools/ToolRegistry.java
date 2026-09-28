package com.kamal.jarvis.v2.tools;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry of JARVIS capabilities/tools.
 *
 * Responsibilities:
 * - Register tools
 * - Replace tools safely
 * - Remove tools
 * - Find tools
 * - Check availability
 * - Provide stable snapshots
 *
 * This class does not execute tools.
 * Execution is handled by JarvisRuntime.
 */
public final class ToolRegistry {

    private final Map<String, ToolContract> tools =
            new LinkedHashMap<>();

    public synchronized JarvisResult<ToolContract> register(
            ToolContract tool) {

        if (tool == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null"
            );
        }

        String id = normalizeId(tool.getId());

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool id cannot be empty"
            );
        }

        if (tools.containsKey(id)) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool already exists: " + id
            );
        }

        tools.put(id, tool);

        return JarvisResult.success(
                tool,
                "Tool registered successfully"
        );
    }

    public synchronized JarvisResult<ToolContract> replace(
            ToolContract tool) {

        if (tool == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null"
            );
        }

        String id = normalizeId(tool.getId());

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool id cannot be empty"
            );
        }

        tools.put(id, tool);

        return JarvisResult.success(
                tool,
                "Tool registered or replaced successfully"
        );
    }

    public synchronized JarvisResult<ToolContract> get(
            String toolId) {

        String id = normalizeId(toolId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool id cannot be empty"
            );
        }

        ToolContract tool = tools.get(id);

        if (tool == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Tool not found: " + id
            );
        }

        return JarvisResult.success(
                tool,
                "Tool found"
        );
    }

    public synchronized JarvisResult<ToolContract> remove(
            String toolId) {

        String id = normalizeId(toolId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool id cannot be empty"
            );
        }

        ToolContract removed = tools.remove(id);

        if (removed == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Tool not found: " + id
            );
        }

        return JarvisResult.success(
                removed,
                "Tool removed successfully"
        );
    }

    public synchronized boolean contains(String toolId) {
        String id = normalizeId(toolId);
        return !id.isEmpty() && tools.containsKey(id);
    }

    public synchronized boolean isAvailable(String toolId) {
        String id = normalizeId(toolId);

        if (id.isEmpty()) {
            return false;
        }

        ToolContract tool = tools.get(id);

        return tool != null && tool.isAvailable();
    }

    public synchronized int size() {
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

    private static String normalizeId(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "ToolRegistry"
                )
        );
    }
}