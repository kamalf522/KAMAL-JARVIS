package com.kamal.jarvis.v2.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class JarvisEvent {

    public enum Type {
        COMMAND_RECEIVED,
        COMMAND_STARTED,
        COMMAND_COMPLETED,
        COMMAND_FAILED,

        TOOL_STARTED,
        TOOL_COMPLETED,
        TOOL_FAILED,

        EVOLUTION_STARTED,
        EVOLUTION_COMPLETED,
        EVOLUTION_FAILED,

        BUILD_STARTED,
        BUILD_COMPLETED,
        BUILD_FAILED,

        TEST_STARTED,
        TEST_COMPLETED,
        TEST_FAILED,

        RECOVERY_STARTED,
        RECOVERY_COMPLETED,
        RECOVERY_FAILED,

        MEMORY_SAVED,
        MEMORY_UPDATED,

        SECURITY_CHECK,
        SECURITY_BLOCKED,

        SYSTEM_STARTED,
        SYSTEM_STOPPED,

        CUSTOM
    }

    private final String id;
    private final Type type;
    private final long timestamp;
    private final String source;
    private final String message;
    private final Map<String, Object> data;

    public JarvisEvent(
            Type type,
            String source,
            String message
    ) {
        this(
                UUID.randomUUID().toString(),
                type,
                System.currentTimeMillis(),
                source,
                message,
                null
        );
    }

    public JarvisEvent(
            String id,
            Type type,
            long timestamp,
            String source,
            String message,
            Map<String, Object> data
    ) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.type = type == null ? Type.CUSTOM : type;
        this.timestamp = timestamp;
        this.source = source == null ? "unknown" : source;
        this.message = message == null ? "" : message;

        if (data == null) {
            this.data = Collections.emptyMap();
        } else {
            this.data = Collections.unmodifiableMap(
                    new LinkedHashMap<>(data)
            );
        }
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public JarvisEvent withData(String key, Object value) {
        Map<String, Object> updated =
                new LinkedHashMap<>(data);

        updated.put(key, value);

        return new JarvisEvent(
                id,
                type,
                timestamp,
                source,
                message,
                updated
        );
    }

    @Override
    public String toString() {
        return "JarvisEvent{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}