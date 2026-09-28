package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.permissions.CapabilityPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * CapabilityPlan
 *
 * يمثل الخطة التي سيستعملها JARVIS بعد اكتشاف
 * الطرق الممكنة لتنفيذ هدف معين.
 *
 * Discovery = اكتشاف شنو ممكن.
 * Plan      = تحديد شنو خاصنا نديرو.
 *
 * هذا الكلاس لا ينفذ الخطة.
 * التنفيذ غادي يكون من مسؤولية Orchestrator لاحقاً.
 */
public final class CapabilityPlan {

    public enum Action {
        EXECUTE_DIRECT,
        EXECUTE_ALTERNATIVE,
        REQUEST_PERMISSION,
        BUILD_CAPABILITY,
        UNAVAILABLE
    }

    private final String capabilityId;
    private final Action action;

    private final List<String> toolIds;
    private final Set<CapabilityPermission> missingPermissions;

    private final boolean requiresEvolution;
    private final boolean requiresOwnerAuthorization;

    private final String reason;

    private CapabilityPlan(
            String capabilityId,
            Action action,
            List<String> toolIds,
            Set<CapabilityPermission> missingPermissions,
            boolean requiresEvolution,
            boolean requiresOwnerAuthorization,
            String reason
    ) {
        this.capabilityId = capabilityId;
        this.action = action;

        this.toolIds = Collections.unmodifiableList(
                new ArrayList<>(toolIds)
        );

        EnumSet<CapabilityPermission> missing =
                EnumSet.noneOf(CapabilityPermission.class);

        if (missingPermissions != null) {
            missing.addAll(missingPermissions);
        }

        this.missingPermissions =
                Collections.unmodifiableSet(missing);

        this.requiresEvolution = requiresEvolution;
        this.requiresOwnerAuthorization =
                requiresOwnerAuthorization;

        this.reason = reason == null ? "" : reason;
    }

    public static CapabilityPlan fromDiscovery(
            CapabilityDiscovery.DiscoveryResult discovery
    ) {
        if (discovery == null ||
                !discovery.isValid()) {

            return new CapabilityPlan(
                    "",
                    Action.UNAVAILABLE,
                    Collections.emptyList(),
                    Collections.emptySet(),
                    false,
                    false,
                    "Invalid capability discovery result."
            );
        }

        if (discovery.canExecuteDirectly()) {

            return createExecutionPlan(
                    discovery,
                    Action.EXECUTE_DIRECT,
                    "Direct execution path is available."
            );
        }

        if (discovery.hasAlternative()) {

            return createExecutionPlan(
                    discovery,
                    Action.EXECUTE_ALTERNATIVE,
                    "An alternative execution path is available."
            );
        }

        if (discovery.needsPermission()) {

            return new CapabilityPlan(
                    discovery.getRequirement().getCapabilityId(),
                    Action.REQUEST_PERMISSION,
                    Collections.emptyList(),
                    discovery.getMissingPermissions(),
                    false,
                    discovery.getRequirement()
                            .isOwnerAuthorizationRequired(),
                    "Required permission or capability must be obtained."
            );
        }

        if (discovery.needsEvolution()) {

            return new CapabilityPlan(
                    discovery.getRequirement().getCapabilityId(),
                    Action.BUILD_CAPABILITY,
                    Collections.emptyList(),
                    discovery.getMissingPermissions(),
                    true,
                    discovery.getRequirement()
                            .isOwnerAuthorizationRequired(),
                    "A new capability or execution route must be built."
            );
        }

        return new CapabilityPlan(
                discovery.getRequirement().getCapabilityId(),
                Action.UNAVAILABLE,
                Collections.emptyList(),
                discovery.getMissingPermissions(),
                false,
                discovery.getRequirement()
                        .isOwnerAuthorizationRequired(),
                discovery.getMessage()
        );
    }

    private static CapabilityPlan createExecutionPlan(
            CapabilityDiscovery.DiscoveryResult discovery,
            Action action,
            String reason
    ) {
        List<String> toolIds = new ArrayList<>();

        if (discovery.getAvailableTools() != null) {

            for (com.kamal.jarvis.v2.core.ToolContract tool
                    : discovery.getAvailableTools()) {

                if (tool != null &&
                        tool.getId() != null) {

                    toolIds.add(tool.getId());
                }
            }
        }

        return new CapabilityPlan(
                discovery.getRequirement().getCapabilityId(),
                action,
                toolIds,
                Collections.emptySet(),
                false,
                discovery.getRequirement()
                        .isOwnerAuthorizationRequired(),
                reason
        );
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public Action getAction() {
        return action;
    }

    public List<String> getToolIds() {
        return toolIds;
    }

    public Set<CapabilityPermission> getMissingPermissions() {
        return missingPermissions;
    }

    public boolean requiresEvolution() {
        return requiresEvolution;
    }

    public boolean requiresOwnerAuthorization() {
        return requiresOwnerAuthorization;
    }

    public String getReason() {
        return reason;
    }

    public boolean shouldExecute() {
        return action == Action.EXECUTE_DIRECT ||
                action == Action.EXECUTE_ALTERNATIVE;
    }

    public boolean shouldRequestPermission() {
        return action == Action.REQUEST_PERMISSION;
    }

    public boolean shouldBuildCapability() {
        return action == Action.BUILD_CAPABILITY;
    }

    public boolean isUnavailable() {
        return action == Action.UNAVAILABLE;
    }

    @Override
    public String toString() {
        return "CapabilityPlan{" +
                "capabilityId='" + capabilityId + '\'' +
                ", action=" + action +
                ", toolIds=" + toolIds +
                ", missingPermissions=" + missingPermissions +
                ", requiresEvolution=" + requiresEvolution +
                ", requiresOwnerAuthorization=" +
                requiresOwnerAuthorization +
                ", reason='" + reason + '\'' +
                '}';
    }
}