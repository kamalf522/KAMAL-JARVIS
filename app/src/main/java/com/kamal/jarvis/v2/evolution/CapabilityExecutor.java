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
 * CapabilityExecutor
 *
 * مسؤول على تنفيذ CapabilityPlan.
 *
 * لا يقوم بتجاوز Android Security.
 * إذا كانت الصلاحية ناقصة، يرجع بالمتطلبات الناقصة
 * حتى يتعامل معها Evolution/Permission layer.
 *
 * المسار:
 *
 * Plan
 *   ↓
 * Check authorization
 *   ↓
 * Check permissions
 *   ↓
 * Find tool
 *   ↓
 * Execute
 *   ↓
 * Return result
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
     * Executes a previously generated capability plan.
     *
     * Tool input is supplied separately so the executor
     * remains independent from command parsing.
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
                return executeTool(
                        plan,
                        input
                );

            case EXECUTE_ALTERNATIVE:
                return executeTool(
                        plan,
                        input
                );

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
                                "This capability requires the evolution system."
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
     * Executes the first available tool from the plan.
     */
    private JarvisResult<ToolContract.ToolOutput> executeTool(
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {

        List<String> toolIds = plan.getToolIds();

        if (toolIds == null || toolIds.isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TOOL_UNAVAILABLE,
                            "No tool is available for this capability."
                    )
            );
        }

        if (plan.requiresOwnerAuthorization()) {
            /*
             * Real owner authentication will be connected here
             * through OwnerSecurityBoundary.
             *
             * We deliberately do not pretend that the request
             * is authenticated just because the flag exists.
             */
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner authorization is required."
                    )
            );
        }

        for (String toolId : toolIds) {

            if (toolId == null || toolId.trim().isEmpty()) {
                continue;
            }

            ToolContract tool =
                    toolRegistry.get(toolId);

            if (tool == null) {
                continue;
            }

            if (!tool.isAvailable()) {
                continue;
            }

            /*
             * Tool-level authorization requirement.
             */
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
                        toolId,
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
     * Returns the permissions currently missing from a plan.
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
     * Checks whether the plan is immediately executable.
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
            return permissionManager.hasAll(
                    Collections.emptySet()
            );
        }

        return plan.getToolIds() != null &&
                !plan.getToolIds().isEmpty();
    }

    private String buildPermissionMessage(
            Set<CapabilityPermission> permissions
    ) {
        if (permissions == null || permissions.isEmpty()) {
            return "Additional authorization or capability is required.";
        }

        StringBuilder message = new StringBuilder();

        message.append(
                "Missing capabilities: "
        );

        boolean first = true;

        for (CapabilityPermission permission
                : permissions) {

            if (!first) {
                message.append(", ");
            }

            message.append(permission.getId());
            first = false;
        }

        return message.toString();
    }
}