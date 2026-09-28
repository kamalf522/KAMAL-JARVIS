package com.kamal.jarvis.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JARVIS V2 Runtime
 *
 * Central execution runtime for registered tools and system events.
 *
 * Design rules:
 * - No Android-specific dependencies.
 * - Tools are registered through ToolContract.
 * - Execution always produces a JarvisResult.
 * - Runtime keeps a bounded event history.
 * - Runtime does not control security policy or evolution policy.
 * - Higher-level systems such as Brain/EvolutionOrchestrator
 *   may use this runtime, but the runtime does not depend on them.
 */
public final class JarvisRuntime {

    private static final int MAX_EVENT_HISTORY = 500;

    private final Map<String, ToolContract> tools =
            new LinkedHashMap<>();

    private final List<JarvisEvent> eventHistory =
            new ArrayList<>();

    private final List<RuntimeEventListener> listeners =
            new CopyOnWriteArrayList<>();

    private RuntimeState state = RuntimeState.CREATED;

    /**
     * Starts the runtime.
     */
    public synchronized JarvisResult<Boolean> start() {
        if (state == RuntimeState.RUNNING) {
            return JarvisResult.success(true, "Runtime already running.");
        }

        if (state == RuntimeState.STOPPED) {
            state = RuntimeState.CREATED;
        }

        state = RuntimeState.RUNNING;

        publishEvent(
                JarvisEvent.Type.SYSTEM,
                "JarvisRuntime",
                "Runtime started.",
                null
        );

        return JarvisResult.success(true, "Runtime started.");
    }

    /**
     * Stops the runtime without deleting registered tools.
     */
    public synchronized JarvisResult<Boolean> stop() {
        if (state == RuntimeState.STOPPED) {
            return JarvisResult.success(true, "Runtime already stopped.");
        }

        state = RuntimeState.STOPPED;

        publishEvent(
                JarvisEvent.Type.SYSTEM,
                "JarvisRuntime",
                "Runtime stopped.",
                null
        );

        return JarvisResult.success(true, "Runtime stopped.");
    }

    /**
     * Resets runtime state and removes registered tools.
     */
    public synchronized JarvisResult<Boolean> reset() {
        tools.clear();
        eventHistory.clear();
        state = RuntimeState.CREATED;

        publishEvent(
                JarvisEvent.Type.SYSTEM,
                "JarvisRuntime",
                "Runtime reset.",
                null
        );

        return JarvisResult.success(true, "Runtime reset.");
    }

    /**
     * Registers a new tool.
     */
    public synchronized JarvisResult<Boolean> registerTool(
            ToolContract tool) {

        if (tool == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null."
            );
        }

        String id = normalizeId(tool.getId());

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool ID cannot be empty."
            );
        }

        if (tools.containsKey(id)) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool already registered: " + id
            );
        }

        tools.put(id, tool);

        publishEvent(
                JarvisEvent.Type.TOOL,
                "JarvisRuntime",
                "Tool registered: " + id,
                id
        );

        return JarvisResult.success(true, "Tool registered.");
    }

    /**
     * Replaces an existing tool or registers it if absent.
     */
    public synchronized JarvisResult<Boolean> replaceTool(
            ToolContract tool) {

        if (tool == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null."
            );
        }

        String id = normalizeId(tool.getId());

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool ID cannot be empty."
            );
        }

        tools.put(id, tool);

        publishEvent(
                JarvisEvent.Type.TOOL,
                "JarvisRuntime",
                "Tool replaced/registered: " + id,
                id
        );

        return JarvisResult.success(true, "Tool registered.");
    }

    /**
     * Removes a registered tool.
     */
    public synchronized JarvisResult<Boolean> unregisterTool(
            String toolId) {

        String id = normalizeId(toolId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool ID cannot be empty."
            );
        }

        if (!tools.containsKey(id)) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Tool not found: " + id
            );
        }

        tools.remove(id);

        publishEvent(
                JarvisEvent.Type.TOOL,
                "JarvisRuntime",
                "Tool unregistered: " + id,
                id
        );

        return JarvisResult.success(true, "Tool unregistered.");
    }

    /**
     * Returns a registered tool.
     */
    public synchronized ToolContract getTool(String toolId) {
        return tools.get(normalizeId(toolId));
    }

    /**
     * Checks whether a tool exists.
     */
    public synchronized boolean containsTool(String toolId) {
        return tools.containsKey(normalizeId(toolId));
    }

    /**
     * Checks whether a tool exists and reports itself available.
     */
    public synchronized boolean isToolAvailable(String toolId) {
        ToolContract tool = getTool(toolId);
        return tool != null && tool.isAvailable();
    }

    /**
     * Executes a registered tool.
     */
    public JarvisResult<ToolContract.ToolOutput> execute(
            String toolId,
            ToolContract.ToolInput input) {

        String id = normalizeId(toolId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool ID cannot be empty."
            );
        }

        if (input == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool input cannot be null."
            );
        }

        synchronized (this) {
            if (state != RuntimeState.RUNNING) {
                return failure(
                        JarvisError.Type.EXECUTION_FAILED,
                        "Runtime is not running."
                );
            }
        }

        ToolContract tool;

        synchronized (this) {
            tool = tools.get(id);
        }

        if (tool == null) {
            publishEvent(
                    JarvisEvent.Type.TOOL,
                    "JarvisRuntime",
                    "Tool not found: " + id,
                    id
            );

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool not found: " + id
            );
        }

        if (!tool.isAvailable()) {
            publishEvent(
                    JarvisEvent.Type.TOOL,
                    "JarvisRuntime",
                    "Tool unavailable: " + id,
                    id
            );

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool unavailable: " + id
            );
        }

        try {
            publishEvent(
                    JarvisEvent.Type.TOOL,
                    "JarvisRuntime",
                    "Executing tool: " + id,
                    id
            );

            JarvisResult<ToolContract.ToolOutput> result =
                    tool.execute(input);

            if (result == null) {
                JarvisResult<ToolContract.ToolOutput> failure =
                        failure(
                                JarvisError.Type.EXECUTION_FAILED,
                                "Tool returned null result: " + id
                        );

                publishEvent(
                        JarvisEvent.Type.TOOL,
                        "JarvisRuntime",
                        "Tool returned null result: " + id,
                        id
                );

                return failure;
            }

            Map<String, String> metadata =
                    new LinkedHashMap<>();

            metadata.put("tool_id", id);
            metadata.put("runtime_state", state.name());

            JarvisResult<ToolContract.ToolOutput> enriched =
                    result.withMetadata(metadata);

            publishEvent(
                    JarvisEvent.Type.TOOL,
                    "JarvisRuntime",
                    result.isSuccess()
                            ? "Tool execution succeeded: " + id
                            : "Tool execution failed: " + id,
                    id
            );

            return enriched;

        } catch (Exception e) {

            JarvisError error =
                    JarvisError.fromException(
                            JarvisError.Type.EXECUTION_FAILED,
                            "Tool execution exception: " + id,
                            "JarvisRuntime",
                            e
                    );

            publishEvent(
                    JarvisEvent.Type.TOOL,
                    "JarvisRuntime",
                    "Tool execution exception: " + id,
                    id
            );

            return JarvisResult.failure(error);
        }
    }

    /**
     * Returns an immutable snapshot of registered tools.
     */
    public synchronized List<ToolContract> getTools() {
        return Collections.unmodifiableList(
                new ArrayList<>(tools.values())
        );
    }

    /**
     * Returns the number of registered tools.
     */
    public synchronized int getToolCount() {
        return tools.size();
    }

    /**
     * Returns a snapshot of event history.
     */
    public synchronized List<JarvisEvent> getEventHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(eventHistory)
        );
    }

    /**
     * Clears event history.
     */
    public synchronized void clearEventHistory() {
        eventHistory.clear();
    }

    /**
     * Adds a runtime event listener.
     */
    public void addEventListener(RuntimeEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a runtime event listener.
     */
    public void removeEventListener(RuntimeEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns runtime status.
     */
    public synchronized RuntimeStatus getStatus() {
        return new RuntimeStatus(
                state,
                tools.size(),
                eventHistory.size()
        );
    }

    /**
     * Returns current state.
     */
    public synchronized RuntimeState getState() {
        return state;
    }

    /**
     * Returns true when runtime is running.
     */
    public synchronized boolean isRunning() {
        return state == RuntimeState.RUNNING;
    }

    /**
     * Publishes a system event.
     */
    public void publishEvent(
            JarvisEvent.Type type,
            String source,
            String message,
            Object data) {

        JarvisEvent event = new JarvisEvent(
                UUID.randomUUID().toString(),
                type,
                System.currentTimeMillis(),
                source,
                message,
                data
        );

        synchronized (this) {
            eventHistory.add(event);

            if (eventHistory.size() > MAX_EVENT_HISTORY) {
                int removeCount =
                        eventHistory.size() - MAX_EVENT_HISTORY;

                for (int i = 0; i < removeCount; i++) {
                    eventHistory.remove(0);
                }
            }
        }

        for (RuntimeEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
                // Listener failure must never break runtime execution.
            }
        }
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "JarvisRuntime"
                )
        );
    }

    private String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }

    public enum RuntimeState {
        CREATED,
        RUNNING,
        STOPPED
    }

    public interface RuntimeEventListener {
        void onEvent(JarvisEvent event);
    }

    public static final class RuntimeStatus {

        private final RuntimeState state;
        private final int toolCount;
        private final int eventCount;

        public RuntimeStatus(
                RuntimeState state,
                int toolCount,
                int eventCount) {

            this.state = state;
            this.toolCount = toolCount;
            this.eventCount = eventCount;
        }

        public RuntimeState getState() {
            return state;
        }

        public int getToolCount() {
            return toolCount;
        }

        public int getEventCount() {
            return eventCount;
        }

        public boolean isRunning() {
            return state == RuntimeState.RUNNING;
        }

        @Override
        public String toString() {
            return "RuntimeStatus{" +
                    "state=" + state +
                    ", toolCount=" + toolCount +
                    ", eventCount=" + eventCount +
                    '}';
        }
    }
}