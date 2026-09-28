package com.kamal.jarvis.v2.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes what JARVIS needs in order to accomplish a capability/goal.
 *
 * This class does NOT grant Android permissions.
 * It only describes requirements and checks the internal permission state.
 *
 * The decision flow can use this information to:
 * 1. Check the preferred capability/tool.
 * 2. Detect missing permissions.
 * 3. Look for an alternative route.
 * 4. Request legitimate Android permissions when needed.
 * 5. Build a new capability when an alternative is possible.
 */
public final class CapabilityRequirement {

    private final String capabilityId;
    private final String description;

    private final Set<CapabilityPermission> requiredPermissions;
    private final List<Set<CapabilityPermission>> alternativePermissionSets;

    private final boolean ownerAuthorizationRequired;
    private final boolean canBuildAlternative;

    private final List<String> preferredToolIds;
    private final List<String> alternativeToolIds;

    private CapabilityRequirement(
            String capabilityId,
            String description,
            Set<CapabilityPermission> requiredPermissions,
            List<Set<CapabilityPermission>> alternativePermissionSets,
            boolean ownerAuthorizationRequired,
            boolean canBuildAlternative,
            List<String> preferredToolIds,
            List<String> alternativeToolIds
    ) {
        this.capabilityId = capabilityId;
        this.description = description;

        this.requiredPermissions = Collections.unmodifiableSet(
                EnumSet.copyOf(requiredPermissions.isEmpty()
                        ? EnumSet.noneOf(CapabilityPermission.class)
                        : requiredPermissions)
        );

        List<Set<CapabilityPermission>> alternatives = new ArrayList<>();

        for (Set<CapabilityPermission> set : alternativePermissionSets) {
            Set<CapabilityPermission> copy = set.isEmpty()
                    ? EnumSet.noneOf(CapabilityPermission.class)
                    : EnumSet.copyOf(set);

            alternatives.add(Collections.unmodifiableSet(copy));
        }

        this.alternativePermissionSets =
                Collections.unmodifiableList(alternatives);

        this.ownerAuthorizationRequired = ownerAuthorizationRequired;
        this.canBuildAlternative = canBuildAlternative;

        this.preferredToolIds = Collections.unmodifiableList(
                new ArrayList<>(preferredToolIds)
        );

        this.alternativeToolIds = Collections.unmodifiableList(
                new ArrayList<>(alternativeToolIds)
        );
    }

    public static Builder builder(
            String capabilityId,
            String description
    ) {
        return new Builder(capabilityId, description);
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getDescription() {
        return description;
    }

    public Set<CapabilityPermission> getRequiredPermissions() {
        return requiredPermissions;
    }

    public List<Set<CapabilityPermission>> getAlternativePermissionSets() {
        return alternativePermissionSets;
    }

    public boolean isOwnerAuthorizationRequired() {
        return ownerAuthorizationRequired;
    }

    public boolean canBuildAlternative() {
        return canBuildAlternative;
    }

    public List<String> getPreferredToolIds() {
        return preferredToolIds;
    }

    public List<String> getAlternativeToolIds() {
        return alternativeToolIds;
    }

    /**
     * Returns the permissions missing from the preferred route.
     */
    public Set<CapabilityPermission> getMissingPermissions(
            PermissionManager permissionManager
    ) {
        if (permissionManager == null) {
            return Collections.unmodifiableSet(
                    EnumSet.copyOf(requiredPermissions)
            );
        }

        EnumSet<CapabilityPermission> missing =
                EnumSet.noneOf(CapabilityPermission.class);

        for (CapabilityPermission permission : requiredPermissions) {
            if (!permissionManager.isGranted(permission)) {
                missing.add(permission);
            }
        }

        return Collections.unmodifiableSet(missing);
    }

    /**
     * Checks whether the preferred route has all required permissions.
     */
    public boolean hasRequiredPermissions(
            PermissionManager permissionManager
    ) {
        if (permissionManager == null) {
            return requiredPermissions.isEmpty();
        }

        return permissionManager.hasAll(requiredPermissions);
    }

    /**
     * Checks whether at least one alternative permission route
     * is currently available.
     */
    public boolean hasAvailableAlternative(
            PermissionManager permissionManager
    ) {
        if (permissionManager == null) {
            return false;
        }

        for (Set<CapabilityPermission> alternative
                : alternativePermissionSets) {

            if (permissionManager.hasAll(alternative)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first currently available alternative permission set.
     *
     * Returns an empty set when no alternative is currently available.
     */
    public Set<CapabilityPermission> getAvailableAlternative(
            PermissionManager permissionManager
    ) {
        if (permissionManager == null) {
            return Collections.emptySet();
        }

        for (Set<CapabilityPermission> alternative
                : alternativePermissionSets) {

            if (permissionManager.hasAll(alternative)) {
                return Collections.unmodifiableSet(
                        EnumSet.copyOf(alternative.isEmpty()
                                ? EnumSet.noneOf(CapabilityPermission.class)
                                : alternative)
                );
            }
        }

        return Collections.emptySet();
    }

    /**
     * Checks whether the requirement can currently be satisfied
     * through the preferred route or an alternative permission route.
     */
    public boolean isSatisfied(
            PermissionManager permissionManager
    ) {
        if (hasRequiredPermissions(permissionManager)) {
            return true;
        }

        return hasAvailableAlternative(permissionManager);
    }

    /**
     * Returns all permissions needed to attempt the preferred route.
     */
    public Set<CapabilityPermission> getRequiredPermissionsCopy() {
        if (requiredPermissions.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(
                EnumSet.copyOf(requiredPermissions)
        );
    }

    @Override
    public String toString() {
        return "CapabilityRequirement{" +
                "capabilityId='" + capabilityId + '\'' +
                ", description='" + description + '\'' +
                ", requiredPermissions=" + requiredPermissions +
                ", alternativePermissionSets=" + alternativePermissionSets +
                ", ownerAuthorizationRequired=" +
                ownerAuthorizationRequired +
                ", canBuildAlternative=" +
                canBuildAlternative +
                ", preferredToolIds=" + preferredToolIds +
                ", alternativeToolIds=" + alternativeToolIds +
                '}';
    }

    public static final class Builder {

        private final String capabilityId;
        private final String description;

        private final Set<CapabilityPermission> requiredPermissions =
                EnumSet.noneOf(CapabilityPermission.class);

        private final List<Set<CapabilityPermission>>
                alternativePermissionSets = new ArrayList<>();

        private final List<String> preferredToolIds =
                new ArrayList<>();

        private final List<String> alternativeToolIds =
                new ArrayList<>();

        private boolean ownerAuthorizationRequired = false;
        private boolean canBuildAlternative = false;

        private Builder(
                String capabilityId,
                String description
        ) {
            if (capabilityId == null || capabilityId.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "capabilityId cannot be empty"
                );
            }

            this.capabilityId = capabilityId.trim();

            this.description =
                    description == null ? "" : description.trim();
        }

        public Builder requirePermission(
                CapabilityPermission permission
        ) {
            if (permission != null) {
                requiredPermissions.add(permission);
            }

            return this;
        }

        public Builder requirePermissions(
                CapabilityPermission... permissions
        ) {
            if (permissions != null) {
                for (CapabilityPermission permission : permissions) {
                    requirePermission(permission);
                }
            }

            return this;
        }

        /**
         * Adds one complete alternative permission route.
         *
         * Example:
         *
         * Route A:
         * FILE_READ + FILE_WRITE
         *
         * Route B:
         * NETWORK
         *
         * JARVIS can then try B when A is unavailable.
         */
        public Builder addAlternativePermissionSet(
                CapabilityPermission... permissions
        ) {
            EnumSet<CapabilityPermission> set =
                    EnumSet.noneOf(CapabilityPermission.class);

            if (permissions != null) {
                for (CapabilityPermission permission : permissions) {
                    if (permission != null) {
                        set.add(permission);
                    }
                }
            }

            alternativePermissionSets.add(set);

            return this;
        }

        public Builder requireOwnerAuthorization() {
            ownerAuthorizationRequired = true;
            return this;
        }

        public Builder allowAlternativeBuilding() {
            canBuildAlternative = true;
            return this;
        }

        public Builder preferTool(String toolId) {
            if (toolId != null && !toolId.trim().isEmpty()) {
                preferredToolIds.add(toolId.trim());
            }

            return this;
        }

        public Builder alternativeTool(String toolId) {
            if (toolId != null && !toolId.trim().isEmpty()) {
                alternativeToolIds.add(toolId.trim());
            }

            return this;
        }

        public CapabilityRequirement build() {
            return new CapabilityRequirement(
                    capabilityId,
                    description,
                    requiredPermissions,
                    alternativePermissionSets,
                    ownerAuthorizationRequired,
                    canBuildAlternative,
                    preferredToolIds,
                    alternativeToolIds
            );
        }
    }
}