package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Executes a CapabilityPlan through registered JARVIS tools.
 *
 * This class is intentionally focused on execution.
 * It does not build capabilities, grant Android permissions,
 * or modify the security boundary.
 */
public final class CapabilityExecutor {

    private final JarvisRuntime runtime;
    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;

    public CapabilityExecutor(
            JarvisRuntime runtime,
            ToolRegistry toolRegistry,
            PermissionManager permissionManager
    ) {
        if (runtime == null) {
            throw new IllegalArgumentException(
                    "JarvisRuntime cannot be null."
            );
        }

        if (toolRegistry == null) {
            throw new IllegalArgumentException(
                    "ToolRegistry cannot be null."
            );
        }

        if (permissionManager == null) {
            throw new IllegalArgumentException(
                    "PermissionManager cannot be null."
            );
        }

        this.runtime = runtime;
        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
    }

    /**
     * Executes the supplied plan.
     */
    public JarvisResult<ToolContract.ToolOutput> execute(
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {
        if (plan == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Capability plan cannot be null."
                    )
            );
        }

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:
            case EXECUTE_ALTERNATIVE:
                return executeTool(plan, input);

            case REQUEST_PERMISSION:
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.NOT_AUTHORIZED,
                                buildPermissionMessage(
                                        plan.getMissingPermissions()
                                )
                        )
                );

            case BUILD_CAPABILITY:
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Capability must be handled by the evolution system."
                        )
                );

            case UNAVAILABLE:
            default:
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.TOOL_UNAVAILABLE,
                                plan.getReason()
                        )
                );
        }
    }

    /**
     * Executes the first usable tool from the plan.
     */
    private JarvisResult<ToolContract.ToolOutput> executeTool(
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {
        if (plan.requiresOwnerAuthorization()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner authorization is required."
                    )
            );
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing != null && !missing.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            buildPermissionMessage(missing)
                    )
            );
        }

        List<String> toolIds = plan.getToolIds();

        if (toolIds == null || toolIds.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TOOL_UNAVAILABLE,
                            "No tool is available for this capability."
                    )
            );
        }

        for (String toolId : toolIds) {

            if (toolId == null || toolId.trim().isEmpty()) {
                continue;
            }

            ToolContract tool = toolRegistry.get(toolId.trim());

            if (tool == null || !tool.isAvailable()) {
                continue;
            }

            if (tool.requiresOwnerAuthorization(input)) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.NOT_AUTHORIZED,
                                "Tool requires owner authorization."
                        )
                );
            }

            try {
                return runtime.executeTool(
                        tool.getId(),
                        input
                );

            } catch (Exception exception) {
                return JarvisResult.failure(
                        JarvisError.fromException(
                                JarvisError.Type.EXECUTION_FAILED,
                                exception
                        )
                );
            }
        }

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        "No usable tool was found."
                )
        );
    }

    /**
     * Returns permissions that the current plan still needs.
     */
    public Set<CapabilityPermission> getMissingPermissions(
            CapabilityPlan plan
    ) {
        if (plan == null) {
            return Collections.emptySet();
        }

        return plan.getMissingPermissions();
    }

    /**
     * Checks whether the plan has the basic conditions
     * required for immediate execution.
     */
    public boolean canExecute(
            CapabilityPlan plan
    ) {
        if (plan == null || !plan.shouldExecute()) {
            return false;
        }

        if (plan.requiresOwnerAuthorization()) {
            return false;
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing != null && !missing.isEmpty()) {
            return false;
        }

        List<String> toolIds = plan.getToolIds();

        if (toolIds == null || toolIds.isEmpty()) {
            return false;
        }

        for (String toolId : toolIds) {

            if (toolId == null || toolId.trim().isEmpty()) {
                continue;
            }

            ToolContract tool =
                    toolRegistry.get(toolId.trim());

            if (tool != null && tool.isAvailable()) {

                if (!tool.requiresOwnerAuthorization(null)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether at least one required permission
     * is actually granted in the internal permission state.
     */
    public boolean hasRequiredPermissions(
            CapabilityPlan plan
    ) {
        if (plan == null) {
            return false;
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        return missing == null || missing.isEmpty();
    }

    private String buildPermissionMessage(
            Set<CapabilityPermission> permissions
    ) {
        if (permissions == null || permissions.isEmpty()) {
            return "Additional authorization or capability is required.";
        }

        StringBuilder message = new StringBuilder(
                "Missing capabilities: "
        );

        boolean first = true;

        for (CapabilityPermission permission : permissions) {

            if (permission == null) {
                continue;
            }

            if (!first) {
                message.append(", ");
            }

            message.append(permission.getId());
            first = false;
        }

        return message.toString();
    }
}