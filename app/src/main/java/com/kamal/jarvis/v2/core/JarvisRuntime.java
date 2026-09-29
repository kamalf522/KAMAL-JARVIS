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
 * Runtime المركزي لتنفيذ الأدوات وتسجيل الأحداث.
 *
 * المسؤوليات:
 * - تشغيل وإيقاف Runtime.
 * - تسجيل الأدوات.
 * - تنفيذ الأدوات.
 * - تسجيل أحداث النظام.
 * - الحفاظ على تاريخ محدود للأحداث.
 *
 * لا يحتوي على منطق Evolution أو Security Policy.
 */
public final class JarvisRuntime {

    private static final int MAX_EVENT_HISTORY = 500;

    private final Map<String, ToolContract> tools =
            new LinkedHashMap<>();

    private final List<JarvisEvent> eventHistory =
            new ArrayList<>();

    private final List<RuntimeEventListener> listeners =
            new CopyOnWriteArrayList<>();

    private RuntimeState state =
            RuntimeState.CREATED;

    /**
     * تشغيل Runtime.
     */
    public synchronized JarvisResult<Boolean> start() {

        if (state == RuntimeState.RUNNING) {

            return JarvisResult.success(
                    true,
                    "Runtime already running."
            );
        }

        if (state == RuntimeState.STOPPED) {

            state = RuntimeState.CREATED;
        }

        state = RuntimeState.RUNNING;

        publishEvent(
                JarvisEvent.Type.SYSTEM_STARTED,
                "JarvisRuntime",
                "Runtime started.",
                null
        );

        return JarvisResult.success(
                true,
                "Runtime started."
        );
    }

    /**
     * إيقاف Runtime.
     *
     * الأدوات المسجلة تبقى موجودة.
     */
    public synchronized JarvisResult<Boolean> stop() {

        if (state == RuntimeState.STOPPED) {

            return JarvisResult.success(
                    true,
                    "Runtime already stopped."
            );
        }

        state = RuntimeState.STOPPED;

        publishEvent(
                JarvisEvent.Type.SYSTEM_STOPPED,
                "JarvisRuntime",
                "Runtime stopped.",
                null
        );

        return JarvisResult.success(
                true,
                "Runtime stopped."
        );
    }

    /**
     * إعادة Runtime إلى الحالة الابتدائية
     * مع حذف الأدوات والأحداث.
     */
    public synchronized JarvisResult<Boolean> reset() {

        tools.clear();
        eventHistory.clear();

        state = RuntimeState.CREATED;

        publishEvent(
                JarvisEvent.Type.SYSTEM_STARTED,
                "JarvisRuntime",
                "Runtime reset.",
                null
        );

        return JarvisResult.success(
                true,
                "Runtime reset."
        );
    }

    /**
     * تسجيل أداة جديدة.
     */
    public synchronized JarvisResult<Boolean> registerTool(
            ToolContract tool
    ) {

        if (tool == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null."
            );
        }

        String id =
                normalizeId(tool.getId());

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

        tools.put(
                id,
                tool
        );

        publishEvent(
                JarvisEvent.Type.TOOL_STARTED,
                "JarvisRuntime",
                "Tool registered: " + id,
                id
        );

        return JarvisResult.success(
                true,
                "Tool registered."
        );
    }

    /**
     * استبدال أداة أو تسجيلها إذا لم تكن موجودة.
     */
    public synchronized JarvisResult<Boolean> replaceTool(
            ToolContract tool
    ) {

        if (tool == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null."
            );
        }

        String id =
                normalizeId(tool.getId());

        if (id.isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool ID cannot be empty."
            );
        }

        tools.put(
                id,
                tool
        );

        publishEvent(
                JarvisEvent.Type.TOOL_STARTED,
                "JarvisRuntime",
                "Tool replaced/registered: " + id,
                id
        );

        return JarvisResult.success(
                true,
                "Tool registered."
        );
    }

    /**
     * حذف أداة.
     */
    public synchronized JarvisResult<Boolean> unregisterTool(
            String toolId
    ) {

        String id =
                normalizeId(toolId);

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
                JarvisEvent.Type.TOOL_COMPLETED,
                "JarvisRuntime",
                "Tool unregistered: " + id,
                id
        );

        return JarvisResult.success(
                true,
                "Tool unregistered."
        );
    }

    /**
     * الحصول على أداة.
     */
    public synchronized ToolContract getTool(
            String toolId
    ) {

        return tools.get(
                normalizeId(toolId)
        );
    }

    /**
     * هل الأداة موجودة؟
     */
    public synchronized boolean containsTool(
            String toolId
    ) {

        String id =
                normalizeId(toolId);

        return !id.isEmpty()
                && tools.containsKey(id);
    }

    /**
     * هل الأداة موجودة ومتاحة؟
     */
    public synchronized boolean isToolAvailable(
            String toolId
    ) {

        ToolContract tool =
                getTool(toolId);

        return tool != null
                && tool.isAvailable();
    }

    /**
     * تنفيذ أداة.
     */
    public JarvisResult<ToolContract.ToolOutput> execute(
            String toolId,
            ToolContract.ToolInput input
    ) {

        String id =
                normalizeId(toolId);

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

            tool =
                    tools.get(id);
        }

        if (tool == null) {

            publishEvent(
                    JarvisEvent.Type.TOOL_FAILED,
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
                    JarvisEvent.Type.TOOL_FAILED,
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
                    JarvisEvent.Type.TOOL_STARTED,
                    "JarvisRuntime",
                    "Executing tool: " + id,
                    id
            );

            JarvisResult<ToolContract.ToolOutput> result =
                    tool.execute(input);

            if (result == null) {

                JarvisResult<ToolContract.ToolOutput>
                        failureResult =
                        failure(
                                JarvisError.Type.EXECUTION_FAILED,
                                "Tool returned null result: " + id
                        );

                publishEvent(
                        JarvisEvent.Type.TOOL_FAILED,
                        "JarvisRuntime",
                        "Tool returned null result: " + id,
                        id
                );

                return failureResult;
            }

            /*
             * JarvisResult.withMetadata() يعمل
             * بمفتاح وقيمة، لذلك نضيف البيانات واحدة واحدة.
             */
            JarvisResult<ToolContract.ToolOutput> enriched =
                    result
                            .withMetadata(
                                    "tool_id",
                                    id
                            )
                            .withMetadata(
                                    "runtime_state",
                                    getState().name()
                            );

            publishEvent(
                    result.isSuccess()
                            ? JarvisEvent.Type.TOOL_COMPLETED
                            : JarvisEvent.Type.TOOL_FAILED,
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
                    JarvisEvent.Type.TOOL_FAILED,
                    "JarvisRuntime",
                    "Tool execution exception: " + id,
                    id
            );

            return JarvisResult.failure(
                    error
            );
        }
    }

    /**
     * Snapshot للأدوات.
     */
    public synchronized List<ToolContract> getTools() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        tools.values()
                )
        );
    }

    /**
     * عدد الأدوات.
     */
    public synchronized int getToolCount() {

        return tools.size();
    }

    /**
     * Snapshot للأحداث.
     */
    public synchronized List<JarvisEvent> getEventHistory() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        eventHistory
                )
        );
    }

    /**
     * حذف تاريخ الأحداث.
     */
    public synchronized void clearEventHistory() {

        eventHistory.clear();
    }

    /**
     * إضافة Listener.
     */
    public void addEventListener(
            RuntimeEventListener listener
    ) {

        if (listener != null
                && !listeners.contains(listener)) {

            listeners.add(listener);
        }
    }

    /**
     * حذف Listener.
     */
    public void removeEventListener(
            RuntimeEventListener listener
    ) {

        if (listener != null) {

            listeners.remove(listener);
        }
    }

    /**
     * حالة Runtime.
     */
    public synchronized RuntimeStatus getStatus() {

        return new RuntimeStatus(
                state,
                tools.size(),
                eventHistory.size()
        );
    }

    /**
     * الحالة الحالية.
     */
    public synchronized RuntimeState getState() {

        return state;
    }

    /**
     * هل Runtime شغال؟
     */
    public synchronized boolean isRunning() {

        return state == RuntimeState.RUNNING;
    }

    /**
     * تسجيل Event.
     *
     * JarvisEvent يحتاج Map<String,Object>.
     * لذلك Object المفرد يتم تحويله إلى Map آمنة.
     */
    public void publishEvent(
            JarvisEvent.Type type,
            String source,
            String message,
            Object data
    ) {

        Map<String, Object> eventData =
                null;

        if (data != null) {

            eventData =
                    new LinkedHashMap<>();

            eventData.put(
                    "value",
                    data
            );
        }

        JarvisEvent event =
                new JarvisEvent(
                        UUID.randomUUID().toString(),
                        type,
                        System.currentTimeMillis(),
                        source,
                        message,
                        eventData
                );

        synchronized (this) {

            eventHistory.add(
                    event
            );

            if (eventHistory.size()
                    > MAX_EVENT_HISTORY) {

                int removeCount =
                        eventHistory.size()
                                - MAX_EVENT_HISTORY;

                for (int i = 0;
                     i < removeCount;
                     i++) {

                    eventHistory.remove(0);
                }
            }
        }

        for (RuntimeEventListener listener
                : listeners) {

            try {

                listener.onEvent(
                        event
                );

            } catch (Exception ignored) {

                /*
                 * Listener لا يمكنه إسقاط Runtime.
                 */
            }
        }
    }

    /**
     * Failure موحد.
     */
    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "JarvisRuntime"
                )
        );
    }

    private String normalizeId(
            String id
    ) {

        return id == null
                ? ""
                : id.trim();
    }

    public enum RuntimeState {

        CREATED,
        RUNNING,
        STOPPED
    }

    public interface RuntimeEventListener {

        void onEvent(
                JarvisEvent event
        );
    }

    public static final class RuntimeStatus {

        private final RuntimeState state;

        private final int toolCount;

        private final int eventCount;

        public RuntimeStatus(
                RuntimeState state,
                int toolCount,
                int eventCount
        ) {

            this.state =
                    state;

            this.toolCount =
                    toolCount;

            this.eventCount =
                    eventCount;
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

            return state ==
                    RuntimeState.RUNNING;
        }

        @Override
        public String toString() {

            return "RuntimeStatus{" +
                    "state=" + state +
                    ", toolCount=" +
                    toolCount +
                    ", eventCount=" + eventCount +
                    '}';
        }
    }
}