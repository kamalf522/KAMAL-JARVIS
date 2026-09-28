package com.kamal.jarvis.v2.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bridge between JARVIS permission system and Android OS permissions.
 *
 * This class only READS the real Android permission state.
 * It does not bypass Android security and does not secretly grant permissions.
 *
 * Permission requests themselves will be handled later by the Android/UI layer.
 */
public final class AndroidPermissionBridge {

    private final Context context;

    public AndroidPermissionBridge(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Context cannot be null"
            );
        }

        this.context = context.getApplicationContext();
    }

    /**
     * Checks whether a specific JARVIS capability permission
     * is actually available from Android.
     */
    public boolean isGranted(CapabilityPermission permission) {
        if (permission == null) {
            return false;
        }

        String androidPermission = mapToAndroidPermission(permission);

        // Some capabilities do not correspond to one normal
        // dangerous Android permission.
        if (androidPermission == null) {
            return isSpecialCapabilityAvailable(permission);
        }

        return ContextCompat.checkSelfPermission(
                context,
                androidPermission
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Synchronizes the real Android permission state into
     * the internal PermissionManager.
     */
    public void synchronize(PermissionManager permissionManager) {
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
     * Returns all capability permissions currently available
     * according to Android.
     */
    public Set<CapabilityPermission> getGrantedPermissions() {

        EnumSet<CapabilityPermission> granted =
                EnumSet.noneOf(CapabilityPermission.class);

        for (CapabilityPermission permission
                : CapabilityPermission.values()) {

            if (isGranted(permission)) {
                granted.add(permission);
            }
        }

        return granted;
    }

    /**
     * Returns all permissions from the requested set that
     * are not currently available on Android.
     */
    public Set<CapabilityPermission> getMissingPermissions(
            Set<CapabilityPermission> required
    ) {
        EnumSet<CapabilityPermission> missing =
                EnumSet.noneOf(CapabilityPermission.class);

        if (required == null) {
            return missing;
        }

        for (CapabilityPermission permission : required) {
            if (!isGranted(permission)) {
                missing.add(permission);
            }
        }

        return missing;
    }

    /**
     * Maps JARVIS capabilities to real Android permissions.
     *
     * Only permissions that have a direct Android equivalent
     * are mapped here.
     */
    private String mapToAndroidPermission(
            CapabilityPermission permission
    ) {

        switch (permission) {

            case MICROPHONE:
                return Manifest.permission.RECORD_AUDIO;

            case NOTIFICATIONS:
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    return Manifest.permission.POST_NOTIFICATIONS;
                }
                return null;

            case NETWORK:
                return Manifest.permission.INTERNET;

            case FILE_READ:
                /*
                 * Modern Android storage access is not represented
                 * by one universal permission.
                 *
                 * JARVIS will later use the appropriate Storage
                 * Access Framework / MediaStore route.
                 */
                return null;

            case FILE_WRITE:
                return null;

            case FILE_DELETE:
                return null;

            case FILE_MOVE:
                return null;

            case NOTIFICATION_ACCESS:
                return null;

            case ACCESSIBILITY:
                return null;

            case BACKGROUND_EXECUTION:
                return null;

            case PROJECT_READ:
                return null;

            case PROJECT_WRITE:
                return null;

            case BUILD_PROJECT:
                return null;

            case RUN_TESTS:
                return null;

            case EVOLUTION:
                return null;

            case OWNER_AUTHORIZATION:
                return null;

            default:
                return null;
        }
    }

    /**
     * Checks capabilities that require a special Android mechanism
     * rather than a normal runtime permission.
     *
     * For now these return false until their dedicated Android
     * bridges are implemented.
     */
    private boolean isSpecialCapabilityAvailable(
            CapabilityPermission permission
    ) {

        switch (permission) {

            case NOTIFICATION_ACCESS:
                return false;

            case ACCESSIBILITY:
                return false;

            case BACKGROUND_EXECUTION:
                /*
                 * Background execution is controlled by Android
                 * service/lifecycle rules, not a single permission.
                 */
                return true;

            case FILE_READ:
            case FILE_WRITE:
            case FILE_DELETE:
            case FILE_MOVE:
                /*
                 * These will be handled through Android's
                 * Storage Access Framework / app-private storage
                 * instead of pretending a normal permission exists.
                 */
                return true;

            case PROJECT_READ:
            case PROJECT_WRITE:
            case BUILD_PROJECT:
            case RUN_TESTS:
            case EVOLUTION:
                /*
                 * These are JARVIS internal capabilities.
                 * They will be controlled by the tool/evolution
                 * system rather than Android runtime permissions.
                 */
                return true;

            case OWNER_AUTHORIZATION:
                /*
                 * Owner authorization is handled by the
                 * OwnerSecurityBoundary and later Android
                 * authentication mechanisms.
                 */
                return false;

            default:
                return false;
        }
    }

    /**
     * Gives a human-readable explanation of why a capability
     * may not be directly represented by a runtime permission.
     */
    public String explain(CapabilityPermission permission) {

        if (permission == null) {
            return "Unknown permission.";
        }

        if (isGranted(permission)) {
            return "Capability is currently available.";
        }

        switch (permission) {

            case NOTIFICATION_ACCESS:
                return "Requires Android Notification Listener access.";

            case ACCESSIBILITY:
                return "Requires Android Accessibility Service access.";

            case FILE_READ:
            case FILE_WRITE:
            case FILE_DELETE:
            case FILE_MOVE:
                return "Requires an appropriate Android storage route.";

            case BACKGROUND_EXECUTION:
                return "Controlled by Android service and lifecycle rules.";

            case PROJECT_READ:
            case PROJECT_WRITE:
                return "Controlled by JARVIS project tools.";

            case BUILD_PROJECT:
                return "Requires a valid project build environment.";

            case RUN_TESTS:
                return "Requires a test execution environment.";

            case EVOLUTION:
                return "Controlled by the JARVIS evolution system.";

            case OWNER_AUTHORIZATION:
                return "Requires successful owner authorization.";

            case MICROPHONE:
                return "Android microphone permission is not currently granted.";

            case NOTIFICATIONS:
                return "Android notification permission is not currently granted.";

            case NETWORK:
                return "Android network permission is not currently available.";

            default:
                return "Required capability is not currently available.";
        }
    }
}