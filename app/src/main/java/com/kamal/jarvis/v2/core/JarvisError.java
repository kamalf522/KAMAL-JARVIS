package com.kamal.jarvis.v2.core;

public final class JarvisError {

    public enum Type {
        INVALID_REQUEST,
        NOT_AUTHORIZED,
        NOT_FOUND,
        VALIDATION_FAILED,
        TOOL_UNAVAILABLE,
        BUILD_FAILED,
        TEST_FAILED,
        EXECUTION_FAILED,
        FILE_OPERATION_FAILED,
        MEMORY_ERROR,
        EVOLUTION_FAILED,
        RECOVERY_FAILED,
        INTERNAL_ERROR,
        UNKNOWN
    }

    private final Type type;
    private final String message;
    private final String source;
    private final Throwable cause;

    private JarvisError(
            Type type,
            String message,
            String source,
            Throwable cause
    ) {
        this.type = type == null ? Type.UNKNOWN : type;
        this.message = message == null ? "" : message;
        this.source = source == null ? "" : source;
        this.cause = cause;
    }

    public static JarvisError of(
            Type type,
            String message,
            String source
    ) {
        return new JarvisError(type, message, source, null);
    }

    public static JarvisError of(
            Type type,
            String message
    ) {
        return new JarvisError(type, message, "", null);
    }

    public static JarvisError fromException(
            Type type,
            String message,
            String source,
            Throwable cause
    ) {
        return new JarvisError(type, message, source, cause);
    }

    public static JarvisError fromException(
            Type type,
            Throwable cause
    ) {
        String message = "";

        if (cause != null) {
            message = cause.getMessage();

            if (message == null || message.trim().isEmpty()) {
                message = cause.getClass().getSimpleName();
            }
        }

        return new JarvisError(
                type,
                message,
                "",
                cause
        );
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean is(Type expectedType) {
        return expectedType != null && type == expectedType;
    }

    @Override
    public String toString() {
        return "JarvisError{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}