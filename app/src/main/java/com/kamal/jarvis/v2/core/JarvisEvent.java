package com.kamal.jarvis.v2.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JARVIS V2 - Event
 *
 * الحدث الموحد داخل النظام.
 *
 * يستعمله Runtime وباقي المكونات لتسجيل:
 *
 * - Commands
 * - Tools
 * - Evolution
 * - Build
 * - Tests
 * - Recovery
 * - Security
 * - System
 */
public final class JarvisEvent {

    public enum Type {

        COMMAND_RECEIVED,
        COMMAND_STARTED,
        COMMAND_COMPLETED,
        COMMAND_FAILED,

        TOOL_STARTED,
        TOOL_COMPLETED,
        TOOL_FAILED,

        /*
         * Alias عام يستعمله Runtime الحالي.
         */
        TOOL,

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

        /*
         * Alias عام يستعمله Runtime الحالي.
         */
        SYSTEM,

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

        this.id =
                id == null || id.trim().isEmpty()
                        ? UUID.randomUUID().toString()
                        : id;

        this.type =
                type == null
                        ? Type.CUSTOM
                        : type;

        this.timestamp =
                timestamp;

        this.source =
                source == null
                        ? "unknown"
                        : source;

        this.message =
                message == null
                        ? ""
                        : message;

        if (data == null || data.isEmpty()) {

            this.data =
                    Collections.emptyMap();

        } else {

            this.data =
                    Collections.unmodifiableMap(
                            new LinkedHashMap<>(
                                    data
                            )
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

    /**
     * إنشاء نسخة من الحدث مع إضافة معلومة.
     */
    public JarvisEvent withData(
            String key,
            Object value
    ) {

        Map<String, Object> updated =
                new LinkedHashMap<>(
                        data
                );

        if (key != null &&
                !key.trim().isEmpty()) {

            updated.put(
                    key,
                    value
            );
        }

        return new JarvisEvent(
                id,
                type,
                timestamp,
                source,
                message,
                updated
        );
    }

    /**
     * إنشاء نسخة مع Map كامل.
     */
    public JarvisEvent withData(
            Map<String, Object> additionalData
    ) {

        Map<String, Object> updated =
                new LinkedHashMap<>(
                        data
                );

        if (additionalData != null) {

            updated.putAll(
                    additionalData
            );
        }

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