package com.kamal.jarvis.v2.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JarvisResult<T> {

    private final boolean success;
    private final T data;
    private final JarvisError error;
    private final String message;
    private final Map<String, Object> metadata;

    private JarvisResult(
            boolean success,
            T data,
            JarvisError error,
            String message,
            Map<String, Object> metadata
    ) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.message = message;
        this.metadata = metadata == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public static <T> JarvisResult<T> success(T data) {
        return new JarvisResult<>(
                true,
                data,
                null,
                "Operation completed successfully.",
                null
        );
    }

    public static <T> JarvisResult<T> success(T data, String message) {
        return new JarvisResult<>(
                true,
                data,
                null,
                message,
                null
        );
    }

    public static <T> JarvisResult<T> failure(JarvisError error) {
        return new JarvisResult<>(
                false,
                null,
                error,
                error != null ? error.getMessage() : "Unknown error.",
                null
        );
    }

    public static <T> JarvisResult<T> failure(
            JarvisError error,
            String message
    ) {
        return new JarvisResult<>(
                false,
                null,
                error,
                message,
                null
        );
    }

    public JarvisResult<T> withMetadata(String key, Object value) {
        Map<String, Object> updated = new LinkedHashMap<>(metadata);
        updated.put(key, value);

        return new JarvisResult<>(
                success,
                data,
                error,
                message,
                updated
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getData() {
        return data;
    }

    public JarvisError getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "JarvisResult{" +
                "success=" + success +
                ", data=" + data +
                ", error=" + error +
                ", message='" + message + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}