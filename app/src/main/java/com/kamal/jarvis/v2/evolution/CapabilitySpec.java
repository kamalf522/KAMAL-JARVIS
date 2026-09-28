package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.permissions.CapabilityPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Complete specification of a capability that JARVIS wants
 * to create, improve, test, or activate.
 *
 * CapabilitySpec is a description of WHAT must exist.
 * It does not itself create code or modify files.
 *
 * The SelfBuilder will use this specification later to
 * construct the capability.
 */
public final class CapabilitySpec {

    private final String capabilityId;
    private final String name;
    private final String goal;
    private final String description;

    private final Set<CapabilityPermission> requiredPermissions;

    private final List<String> requiredTools;
    private final List<String> preferredTools;
    private final List<String> alternativeTools;

    private final List<String> requiredFiles;
    private final List<String> allowedFiles;

    private final List<String> successCriteria;

    private final boolean ownerAuthorizationRequired;
    private final boolean canModifyProjectFiles;
    private final boolean requiresBuild;
    private final boolean requiresTests;

    private CapabilitySpec(
            Builder builder
    ) {
        this.capabilityId = builder.capabilityId;
        this.name = builder.name;
        this.goal = builder.goal;
        this.description = builder.description;

        EnumSet<CapabilityPermission> permissions =
                EnumSet.noneOf(CapabilityPermission.class);

        permissions.addAll(builder.requiredPermissions);

        this.requiredPermissions =
                Collections.unmodifiableSet(permissions);

        this.requiredTools =
                immutableCopy(builder.requiredTools);

        this.preferredTools =
                immutableCopy(builder.preferredTools);

        this.alternativeTools =
                immutableCopy(builder.alternativeTools);

        this.requiredFiles =
                immutableCopy(builder.requiredFiles);

        this.allowedFiles =
                immutableCopy(builder.allowedFiles);

        this.successCriteria =
                immutableCopy(builder.successCriteria);

        this.ownerAuthorizationRequired =
                builder.ownerAuthorizationRequired;

        this.canModifyProjectFiles =
                builder.canModifyProjectFiles;

        this.requiresBuild =
                builder.requiresBuild;

        this.requiresTests =
                builder.requiresTests;
    }

    private static List<String> immutableCopy(
            List<String> source
    ) {
        return Collections.unmodifiableList(
                new ArrayList<>(source)
        );
    }

    public static Builder builder(
            String capabilityId,
            String name
    ) {
        return new Builder(
                capabilityId,
                name
        );
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public String getDescription() {
        return description;
    }

    public Set<CapabilityPermission>
    getRequiredPermissions() {
        return requiredPermissions;
    }

    public List<String> getRequiredTools() {
        return requiredTools;
    }

    public List<String> getPreferredTools() {
        return preferredTools;
    }

    public List<String> getAlternativeTools() {
        return alternativeTools;
    }

    public List<String> getRequiredFiles() {
        return requiredFiles;
    }

    public List<String> getAllowedFiles() {
        return allowedFiles;
    }

    public List<String> getSuccessCriteria() {
        return successCriteria;
    }

    public boolean isOwnerAuthorizationRequired() {
        return ownerAuthorizationRequired;
    }

    public boolean canModifyProjectFiles() {
        return canModifyProjectFiles;
    }

    public boolean requiresBuild() {
        return requiresBuild;
    }

    public boolean requiresTests() {
        return requiresTests;
    }

    /**
     * A valid capability must have:
     *
     * - an ID
     * - a name
     * - a goal
     * - at least one success criterion
     */
    public boolean isValid() {

        return !capabilityId.isEmpty()
                && !name.isEmpty()
                && !goal.isEmpty()
                && !successCriteria.isEmpty();
    }

    @Override
    public String toString() {
        return "CapabilitySpec{" +
                "capabilityId='" + capabilityId + '\'' +
                ", name='" + name + '\'' +
                ", goal='" + goal + '\'' +
                ", requiredPermissions=" +
                requiredPermissions +
                ", requiredTools=" +
                requiredTools +
                ", requiredFiles=" +
                requiredFiles +
                ", successCriteria=" +
                successCriteria +
                ", ownerAuthorizationRequired=" +
                ownerAuthorizationRequired +
                ", canModifyProjectFiles=" +
                canModifyProjectFiles +
                ", requiresBuild=" +
                requiresBuild +
                ", requiresTests=" +
                requiresTests +
                '}';
    }

    public static final class Builder {

        private final String capabilityId;
        private final String name;

        private String goal = "";
        private String description = "";

        private final Set<CapabilityPermission>
                requiredPermissions =
                EnumSet.noneOf(
                        CapabilityPermission.class
                );

        private final List<String> requiredTools =
                new ArrayList<>();

        private final List<String> preferredTools =
                new ArrayList<>();

        private final List<String> alternativeTools =
                new ArrayList<>();

        private final List<String> requiredFiles =
                new ArrayList<>();

        private final List<String> allowedFiles =
                new ArrayList<>();

        private final List<String> successCriteria =
                new ArrayList<>();

        private boolean ownerAuthorizationRequired;
        private boolean canModifyProjectFiles;
        private boolean requiresBuild;
        private boolean requiresTests;

        private Builder(
                String capabilityId,
                String name
        ) {
            if (capabilityId == null ||
                    capabilityId.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Capability ID cannot be empty."
                );
            }

            if (name == null ||
                    name.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Capability name cannot be empty."
                );
            }

            this.capabilityId =
                    capabilityId.trim();

            this.name =
                    name.trim();
        }

        public Builder goal(
                String goal
        ) {
            this.goal =
                    goal == null
                            ? ""
                            : goal.trim();

            return this;
        }

        public Builder description(
                String description
        ) {
            this.description =
                    description == null
                            ? ""
                            : description.trim();

            return this;
        }

        public Builder requirePermission(
                CapabilityPermission permission
        ) {
            if (permission != null) {
                requiredPermissions.add(
                        permission
                );
            }

            return this;
        }

        public Builder requireTool(
                String toolId
        ) {
            addNonEmpty(
                    requiredTools,
                    toolId
            );

            return this;
        }

        public Builder preferTool(
                String toolId
        ) {
            addNonEmpty(
                    preferredTools,
                    toolId
            );

            return this;
        }

        public Builder alternativeTool(
                String toolId
        ) {
            addNonEmpty(
                    alternativeTools,
                    toolId
            );

            return this;
        }

        public Builder requireFile(
                String path
        ) {
            addNonEmpty(
                    requiredFiles,
                    path
            );

            return this;
        }

        public Builder allowFile(
                String path
        ) {
            addNonEmpty(
                    allowedFiles,
                    path
            );

            return this;
        }

        public Builder successCriterion(
                String criterion
        ) {
            addNonEmpty(
                    successCriteria,
                    criterion
            );

            return this;
        }

        public Builder requireOwnerAuthorization() {
            ownerAuthorizationRequired = true;
            return this;
        }

        public Builder allowProjectModification() {
            canModifyProjectFiles = true;
            return this;
        }

        public Builder requireBuild() {
            requiresBuild = true;
            return this;
        }

        public Builder requireTests() {
            requiresTests = true;
            return this;
        }

        public CapabilitySpec build() {

            CapabilitySpec spec =
                    new CapabilitySpec(this);

            if (!spec.isValid()) {
                throw new IllegalStateException(
                        "CapabilitySpec is incomplete. " +
                        "A goal and at least one success " +
                        "criterion are required."
                );
            }

            return spec;
        }

        private static void addNonEmpty(
                List<String> list,
                String value
        ) {
            if (value == null) {
                return;
            }

            String cleaned =
                    value.trim();

            if (!cleaned.isEmpty() &&
                    !list.contains(cleaned)) {

                list.add(cleaned);
            }
        }
    }
}