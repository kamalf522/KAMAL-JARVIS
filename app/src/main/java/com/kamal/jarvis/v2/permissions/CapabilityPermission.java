package com.kamal.jarvis.v2.permissions;

public enum CapabilityPermission {

    FILE_READ(
            "FILE_READ",
            "Read files that Android has granted JARVIS access to."
    ),

    FILE_WRITE(
            "FILE_WRITE",
            "Create or modify files that Android has granted JARVIS access to."
    ),

    FILE_DELETE(
            "FILE_DELETE",
            "Delete files that Android has granted JARVIS access to."
    ),

    FILE_MOVE(
            "FILE_MOVE",
            "Move or rename files that Android has granted JARVIS access to."
    ),

    MICROPHONE(
            "MICROPHONE",
            "Use the device microphone."
    ),

    NOTIFICATIONS(
            "NOTIFICATIONS",
            "Send notifications to the user."
    ),

    NOTIFICATION_ACCESS(
            "NOTIFICATION_ACCESS",
            "Access notifications through Android's notification listener permission."
    ),

    ACCESSIBILITY(
            "ACCESSIBILITY",
            "Use Android accessibility capabilities after the user explicitly enables the service."
    ),

    NETWORK(
            "NETWORK",
            "Use network connectivity."
    ),

    BACKGROUND_EXECUTION(
            "BACKGROUND_EXECUTION",
            "Run supported long-running tasks through Android background mechanisms."
    ),

    PROJECT_READ(
            "PROJECT_READ",
            "Read files belonging to an authorized development project."
    ),

    PROJECT_WRITE(
            "PROJECT_WRITE",
            "Modify files belonging to an authorized development project."
    ),

    BUILD_PROJECT(
            "BUILD_PROJECT",
            "Run an authorized project build operation."
    ),

    RUN_TESTS(
            "RUN_TESTS",
            "Run authorized tests."
    ),

    EVOLUTION(
            "EVOLUTION",
            "Use the JARVIS evolution system."
    ),

    OWNER_AUTHORIZATION(
            "OWNER_AUTHORIZATION",
            "Perform an operation that requires verified owner authorization."
    );

    private final String id;
    private final String description;

    CapabilityPermission(
            String id,
            String description
    ) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return id;
    }
}