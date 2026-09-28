package com.kamal.jarvis.v2.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Central permission state manager for JARVIS V2.
 *
 * This class tracks the permissions/capabilities that JARVIS
 * currently knows to be requested or granted.
 *
 * Important:
 * grant() only updates JARVIS's internal state.
 * Real Android permissions must be synchronized through
 * AndroidPermissionBridge.
 */
public final class PermissionManager {

    private final Set<CapabilityPermission> requestedPermissions =
            EnumSet.noneOf(CapabilityPermission.class);

    private final Set<CapabilityPermission> grantedPermissions =
            EnumSet.noneOf(CapabilityPermission.class);

    public synchronized void request(
            CapabilityPermission permission) {

        if (permission == null) {
            return;
        }

        requestedPermissions.add(permission);
    }

    public synchronized void requestAll(
            Iterable<CapabilityPermission> permissions) {

        if (permissions == null) {
            return;
        }

        for (CapabilityPermission permission : permissions) {
            request(permission);
        }
    }

    public synchronized void grant(
            CapabilityPermission permission) {

        if (permission == null) {
            return;
        }

        requestedPermissions.add(permission);
        grantedPermissions.add(permission);
    }

    public synchronized void grantAll(
            Iterable<CapabilityPermission> permissions) {

        if (permissions == null) {
            return;
        }

        for (CapabilityPermission permission : permissions) {
            grant(permission);
        }
    }

    public synchronized void revoke(
            CapabilityPermission permission) {

        if (permission == null) {
            return;
        }

        grantedPermissions.remove(permission);
    }

    public synchronized void revokeAll(
            Iterable<CapabilityPermission> permissions) {

        if (permissions == null) {
            return;
        }

        for (CapabilityPermission permission : permissions) {
            revoke(permission);
        }
    }

    public synchronized boolean isRequested(
            CapabilityPermission permission) {

        return permission != null
                && requestedPermissions.contains(permission);
    }

    public synchronized boolean isGranted(
            CapabilityPermission permission) {

        return permission != null
                && grantedPermissions.contains(permission);
    }

    public synchronized boolean require(
            CapabilityPermission permission) {

        return permission != null
                && isGranted(permission);
    }

    public synchronized boolean requireAll(
            Iterable<CapabilityPermission> permissions) {

        if (permissions == null) {
            return true;
        }

        for (CapabilityPermission permission : permissions) {

            if (permission == null) {
                continue;
            }

            if (!grantedPermissions.contains(permission)) {
                return false;
            }
        }

        return true;
    }

    public synchronized Set<CapabilityPermission>
    getGrantedPermissions() {

        return Collections.unmodifiableSet(
                EnumSet.copyOf(grantedPermissions)
        );
    }

    public synchronized Set<CapabilityPermission>
    getRequestedPermissions() {

        return Collections.unmodifiableSet(
                EnumSet.copyOf(requestedPermissions)
        );
    }

    public synchronized Set<CapabilityPermission>
    getMissingPermissions(
            Iterable<CapabilityPermission> requiredPermissions) {

        Set<CapabilityPermission> missing =
                EnumSet.noneOf(CapabilityPermission.class);

        if (requiredPermissions == null) {
            return missing;
        }

        for (CapabilityPermission permission : requiredPermissions) {

            if (permission == null) {
                continue;
            }

            if (!grantedPermissions.contains(permission)) {
                missing.add(permission);
            }
        }

        return missing;
    }

    public synchronized boolean hasAll(
            Iterable<CapabilityPermission> permissions) {

        return requireAll(permissions);
    }

    public synchronized int getGrantedCount() {
        return grantedPermissions.size();
    }

    public synchronized int getRequestedCount() {
        return requestedPermissions.size();
    }

    public synchronized List<CapabilityPermission>
    getMissingPermissionsList(
            Iterable<CapabilityPermission> requiredPermissions) {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        getMissingPermissions(requiredPermissions)
                )
        );
    }

    public synchronized void clear() {
        requestedPermissions.clear();
        grantedPermissions.clear();
    }

    public synchronized void clearGranted() {
        grantedPermissions.clear();
    }

    public synchronized boolean isEmpty() {
        return grantedPermissions.isEmpty();
    }

    @Override
    public synchronized String toString() {

        return "PermissionManager{" +
                "requested=" + requestedPermissions.size() +
                ", granted=" + grantedPermissions.size() +
                '}';
    }
}