package com.kamal.jarvis.v2.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Connects JARVIS capability permissions with the real
 * Android permission state.
 *
 * This class NEVER grants or bypasses permissions.
 * It only checks what Android has actually granted.
 */
public final class AndroidPermissionBridge {

    private final Context context;

    public AndroidPermissionBridge(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Context cannot be null."
            );
        }

        this.context = context.getApplicationContext();
    }

    /**
     * Checks a JARVIS capability against the real Android state.
     */
    public boolean isGranted(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return false;
        }

        String androidPermission =
                mapToAndroidPermission(permission);

        if (androidPermission != null) {
            return context.checkSelfPermission(
                    androidPermission
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return isSpecialCapabilityAvailable(permission);
    }

    /**
     * Synchronizes Android's actual state with JARVIS'
     * internal PermissionManager.
     */
    public void synchronize(
            PermissionManager permissionManager
    ) {
        if (permissionManager == null) {
            return;
        }

        for (CapabilityPermission permission
                : CapabilityPermission.values()) {

            if (isGranted(permission)) {
                permissionManager.grant(permission);
            } else {
                permissionManager.revoke(permission);
            }
        }
    }

    /**
     * Converts JARVIS permissions to normal Android
     * runtime permissions where a direct mapping exists.
     */
    private String mapToAndroidPermission(
            CapabilityPermission permission
    ) {

        switch (permission) {

            case MICROPHONE:
                return Manifest.permission.RECORD_AUDIO;

            case NOTIFICATIONS:
                if (Build.VERSION.SDK_INT >= 33) {
                    return Manifest.permission.POST_NOTIFICATIONS;
                }
                return null;

            case NETWORK:
                /*
                 * INTERNET is a normal manifest permission and
                 * does not require a runtime permission dialog.
                 */
                return Manifest.permission.INTERNET;

            default:
                return null;
        }
    }

    /**
     * Handles capabilities that are controlled through
     * Android services, storage APIs, or JARVIS internal systems.
     */
    private boolean isSpecialCapabilityAvailable(
            CapabilityPermission permission
    ) {

        switch (permission) {

            case BACKGROUND_EXECUTION:
                /*
                 * This is controlled by Android lifecycle/service
                 * rules rather than one runtime permission.
                 */
                return true;

            case FILE_READ:
            case FILE_WRITE:
            case FILE_DELETE:
            case FILE_MOVE:
                /*
                 * These must later use the appropriate Android
                 * storage mechanism instead of pretending that
                 * one universal storage permission exists.
                 */
                return true;

            case PROJECT_READ:
            case PROJECT_WRITE:
            case BUILD_PROJECT:
            case RUN_TESTS:
            case EVOLUTION:
                /*
                 * These are JARVIS capabilities, not Android
                 * runtime permissions.
                 */
                return true;

            case NOTIFICATION_ACCESS:
            case ACCESSIBILITY:
            case OWNER_AUTHORIZATION:
                /*
                 * These require their dedicated systems.
                 */
                return false;

            default:
                return false;
        }
    }

    /**
     * Provides a clear explanation when a capability is unavailable.
     */
    public String explain(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return "Unknown capability.";
        }

        if (isGranted(permission)) {
            return "Capability is currently available.";
        }

        switch (permission) {

            case MICROPHONE:
                return "Microphone permission is not granted.";

            case NOTIFICATIONS:
                return "Notification permission is not granted.";

            case NETWORK:
                return "Network access is not available.";

            case NOTIFICATION_ACCESS:
                return "Notification Listener access is required.";

            case ACCESSIBILITY:
                return "Accessibility Service access is required.";

            case FILE_READ:
            case FILE_WRITE:
            case FILE_DELETE:
            case FILE_MOVE:
                return "An appropriate Android storage route is required.";

            case BUILD_PROJECT:
                return "A valid project build environment is required.";

            case RUN_TESTS:
                return "A test execution environment is required.";

            case EVOLUTION:
                return "The JARVIS evolution system must provide this capability.";

            case OWNER_AUTHORIZATION:
                return "Owner authorization is required.";

            default:
                return "The required capability is not currently available.";
        }
    }
}