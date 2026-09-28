package com.kamal.jarvis.v2.permissions;

/**
 * Permissions/capabilities that JARVIS may need in order to complete goals.
 *
 * These are JARVIS-level capabilities, not all of them map directly
 * to Android runtime permissions.
 */
public enum CapabilityPermission {

    FILE_READ(
            "file_read",
            "Read files and project data"
    ),

    FILE_WRITE(
            "file_write",
            "Create or modify files"
    ),

    FILE_DELETE(
            "file_delete",
            "Delete files when explicitly allowed"
    ),

    FILE_MOVE(
            "file_move",
            "Move or reorganize files"
    ),

    MICROPHONE(
            "microphone",
            "Use the device microphone"
    ),

    NOTIFICATIONS(
            "notifications",
            "Post notifications"
    ),

    NOTIFICATION_ACCESS(
            "notification_access",
            "Read permitted notification data"
    ),

    ACCESSIBILITY(
            "accessibility",
            "Use Android accessibility capabilities"
    ),

    NETWORK(
            "network",
            "Access network resources"
    ),

    BACKGROUND_EXECUTION(
            "background_execution",
            "Execute permitted background work"
    ),

    PROJECT_READ(
            "project_read",
            "Read JARVIS project/workspace files"
    ),

    PROJECT_WRITE(
            "project_write",
            "Modify permitted JARVIS project files"
    ),

    BUILD_PROJECT(
            "build_project",
            "Build a project or generated capability"
    ),

    RUN_TESTS(
            "run_tests",
            "Run automated tests"
    ),

    EVOLUTION(
            "evolution",
            "Use JARVIS evolution mechanisms"
    ),

    OWNER_AUTHORIZATION(
            "owner_authorization",
            "Require verified owner authorization"
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

    public static CapabilityPermission fromId(String id) {

        if (id == null) {
            return null;
        }

        String normalized = id.trim();

        for (CapabilityPermission permission : values()) {

            if (permission.id.equalsIgnoreCase(normalized)) {
                return permission;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}