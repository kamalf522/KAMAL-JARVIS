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
 * Executes CapabilityPlan through the JARVIS runtime.
 *
 * This class:
 * - validates execution conditions
 * - checks permissions
 * - checks owner authorization requirements
 * - resolves tools
 * - synchronizes tools with the runtime when necessary
 * - executes through JarvisRuntime
 *
 * It does not build capabilities and does not modify security.
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

    public JarvisResult<ToolContract.ToolOutput> execute(
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {
        if (plan == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability plan cannot be null."
            );
        }

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:
            case EXECUTE_ALTERNATIVE:
                return executeTool(plan, input);

            case REQUEST_PERMISSION:
                return failure(
                        JarvisError.Type.NOT_AUTHORIZED,
                        buildPermissionMessage(
                                plan.getMissingPermissions()
                        )
                );

            case BUILD_CAPABILITY:
                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "This capability must be handled by EvolutionCore."
                );

            case UNAVAILABLE:
            default:
                return failure(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        plan.getReason()
                );
        }
    }

    private JarvisResult<ToolContract.ToolOutput> executeTool(
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {
        if (input == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool input cannot be null."
            );
        }

        if (plan.requiresOwnerAuthorization()) {
            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Owner authorization is required."
            );
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing != null && !missing.isEmpty()) {
            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    buildPermissionMessage(missing)
            );
        }

        List<String> toolIds = plan.getToolIds();

        if (toolIds == null || toolIds.isEmpty()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No tool is available for this capability."
            );
        }

        JarvisError lastError = null;

        for (String toolId : toolIds) {

            if (toolId == null ||
                    toolId.trim().isEmpty()) {
                continue;
            }

            String normalizedId = toolId.trim();

            ToolContract tool =
                    toolRegistry.get(normalizedId);

            if (tool == null) {
                continue;
            }

            if (!tool.isAvailable()) {
                continue;
            }

            if (tool.requiresOwnerAuthorization(input)) {
                lastError = JarvisError.of(
                        JarvisError.Type.NOT_AUTHORIZED,
                        "Tool requires owner authorization.",
                        "CapabilityExecutor"
                );
                continue;
            }

            /*
             * ToolRegistry and JarvisRuntime are intentionally
             * separate components.
             *
             * Before execution we make sure the selected tool
             * also exists inside the runtime.
             */
            JarvisResult<ToolContract> runtimeTool =
                    runtime.getTool(normalizedId);

            if (!runtimeTool.isSuccess()) {

                JarvisResult<Void> registration =
                        runtime.registerTool(tool);

                if (!registration.isSuccess()) {
                    lastError = registration.getError();
                    continue;
                }
            }

            JarvisResult<ToolContract.ToolOutput> result =
                    runtime.execute(
                            normalizedId,
                            input
                    );

            if (result.isSuccess()) {
                return result;
            }

            lastError = result.getError();
        }

        if (lastError != null) {
            return JarvisResult.failure(lastError);
        }

        return failure(
                JarvisError.Type.TOOL_UNAVAILABLE,
                "No usable tool was found for capability: "
                        + plan.getCapabilityId()
        );
    }

    public Set<CapabilityPermission> getMissingPermissions(
            CapabilityPlan plan
    ) {
        if (plan == null) {
            return Collections.emptySet();
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing == null) {
            return Collections.emptySet();
        }

        return missing;
    }

    public boolean canExecute(
            CapabilityPlan plan
    ) {
        if (plan == null ||
                !plan.shouldExecute()) {
            return false;
        }

        if (plan.requiresOwnerAuthorization()) {
            return false;
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing != null &&
                !missing.isEmpty()) {
            return false;
        }

        List<String> toolIds =
                plan.getToolIds();

        if (toolIds == null ||
                toolIds.isEmpty()) {
            return false;
        }

        for (String toolId : toolIds) {

            if (toolId == null ||
                    toolId.trim().isEmpty()) {
                continue;
            }

            ToolContract tool =
                    toolRegistry.get(
                            toolId.trim()
                    );

            if (tool == null ||
                    !tool.isAvailable()) {
                continue;
            }

            if (tool.requiresOwnerAuthorization(null)) {
                continue;
            }

            return true;
        }

        return false;
    }

    public boolean hasRequiredPermissions(
            CapabilityPlan plan
    ) {
        if (plan == null) {
            return false;
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        return missing == null ||
                missing.isEmpty();
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public JarvisRuntime getRuntime() {
        return runtime;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    private JarvisResult<ToolContract.ToolOutput> failure(
            JarvisError.Type type,
            String message
    ) {
        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "CapabilityExecutor"
                )
        );
    }

    private String buildPermissionMessage(
            Set<CapabilityPermission> permissions
    ) {
        if (permissions == null ||
                permissions.isEmpty()) {
            return "Required permission is missing.";
        }

        StringBuilder builder =
                new StringBuilder(
                        "Required permissions are missing: "
                );

        boolean first = true;

        for (CapabilityPermission permission :
                permissions) {

            if (!first) {
                builder.append(", ");
            }

            builder.append(
                    permission == null
                            ? "UNKNOWN"
                            : permission.getId()
            );

            first = false;
        }

        return builder.toString();
    }
}