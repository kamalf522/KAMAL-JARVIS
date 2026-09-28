package com.kamal.jarvis.v2.permissions;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class PermissionManager {

    private final Set<CapabilityPermission> grantedPermissions;
    private final Set<CapabilityPermission> requestedPermissions;

    public PermissionManager() {
        grantedPermissions =
                EnumSet.noneOf(CapabilityPermission.class);

        requestedPermissions =
                EnumSet.noneOf(CapabilityPermission.class);
    }

    public synchronized JarvisResult<Void> request(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Permission cannot be null.",
                            "PermissionManager"
                    )
            );
        }

        requestedPermissions.add(permission);

        return JarvisResult.success(
                null,
                "Permission registered: "
                        + permission.getId()
        );
    }

    public synchronized JarvisResult<Void> grant(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Permission cannot be null.",
                            "PermissionManager"
                    )
            );
        }

        grantedPermissions.add(permission);

        return JarvisResult.success(
                null,
                "Permission granted: "
                        + permission.getId()
        );
    }

    public synchronized JarvisResult<Void> revoke(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Permission cannot be null.",
                            "PermissionManager"
                    )
            );
        }

        grantedPermissions.remove(permission);

        return JarvisResult.success(
                null,
                "Permission revoked: "
                        + permission.getId()
        );
    }

    public synchronized boolean isGranted(
            CapabilityPermission permission
    ) {
        return permission != null
                && grantedPermissions.contains(permission);
    }

    public synchronized boolean isRequested(
            CapabilityPermission permission
    ) {
        return permission != null
                && requestedPermissions.contains(permission);
    }

    public synchronized JarvisResult<Void> require(
            CapabilityPermission permission
    ) {
        if (permission == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Permission cannot be null.",
                            "PermissionManager"
                    )
            );
        }

        if (!isGranted(permission)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Required permission is not granted: "
                                    + permission.getId(),
                            "PermissionManager"
                    )
            );
        }

        return JarvisResult.success(
                null,
                "Permission verified: "
                        + permission.getId()
        );
    }

    public synchronized JarvisResult<Void> requireAll(
            Set<CapabilityPermission> permissions
    ) {
        if (permissions == null || permissions.isEmpty()) {
            return JarvisResult.success(
                    null,
                    "No permissions required."
            );
        }

        for (CapabilityPermission permission : permissions) {
            if (permission == null) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.INVALID_REQUEST,
                                "Permission set contains null.",
                                "PermissionManager"
                        )
                );
            }

            if (!grantedPermissions.contains(permission)) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.NOT_AUTHORIZED,
                                "Required permission is not granted: "
                                        + permission.getId(),
                                "PermissionManager"
                        )
                );
            }
        }

        return JarvisResult.success(
                null,
                "All required permissions are granted."
        );
    }

    public synchronized List<CapabilityPermission>
    getGrantedPermissions() {

        return Collections.unmodifiableList(
                new ArrayList<>(grantedPermissions)
        );
    }

    public synchronized List<CapabilityPermission>
    getRequestedPermissions() {

        return Collections.unmodifiableList(
                new ArrayList<>(requestedPermissions)
        );
    }

    public synchronized List<CapabilityPermission>
    getMissingPermissions(
            Set<CapabilityPermission> required
    ) {
        List<CapabilityPermission> missing =
                new ArrayList<>();

        if (required == null) {
            return Collections.unmodifiableList(missing);
        }

        for (CapabilityPermission permission : required) {
            if (permission != null
                    && !grantedPermissions.contains(permission)) {

                missing.add(permission);
            }
        }

        return Collections.unmodifiableList(missing);
    }

    public synchronized boolean
    hasAll(
            Set<CapabilityPermission> required
    ) {
        return getMissingPermissions(required).isEmpty();
    }

    public synchronized void clear() {
        grantedPermissions.clear();
        requestedPermissions.clear();
    }

    public synchronized int grantedCount() {
        return grantedPermissions.size();
    }

    public synchronized int requestedCount() {
        return requestedPermissions.size();
    }
}