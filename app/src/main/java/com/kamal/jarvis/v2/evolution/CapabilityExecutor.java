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
 * Executes a CapabilityPlan through the JARVIS runtime.
 *
 * Responsibilities:
 * - Validate execution conditions.
 * - Check required permissions.
 * - Resolve usable tools.
 * - Synchronize selected tools with JarvisRuntime.
 * - Execute tools through JarvisRuntime.
 *
 * This class does not:
 * - Build capabilities.
 * - Modify source code.
 * - Modify owner security.
 * - Bypass Android permissions.
 */
public final class CapabilityExecutor {

    private final JarvisRuntime runtime;
    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;

    public CapabilityExecutor(
            JarvisRuntime runtime,
            ToolRegistry toolRegistry,
            PermissionManager permissionManager) {

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
     * Executes a capability plan.
     */
    public JarvisResult<ToolContract.ToolOutput> execute(
            CapabilityPlan plan,
            ToolContract.ToolInput input) {

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
                        "Capability building must be handled by EvolutionCore."
                );

            case UNAVAILABLE:
            default:
                return failure(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        plan.getReason()
                );
        }
    }

    /**
     * Executes the first usable tool from the plan.
     */
    private JarvisResult<ToolContract.ToolOutput> executeTool(
            CapabilityPlan plan,
            ToolContract.ToolInput input) {

        if (input == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool input cannot be null."
            );
        }

        /*
         * Owner authorization is intentionally not silently
         * bypassed here.
         *
         * A future higher-level controller must provide the
         * authorization decision before executing owner-protected
         * capabilities.
         */
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

            /*
             * ToolRegistry is the capability catalogue.
             */
            ToolContract tool =
                    findTool(normalizedId);

            if (tool == null) {
                lastError = JarvisError.of(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        "Tool not found: " + normalizedId,
                        "CapabilityExecutor"
                );
                continue;
            }

            if (!tool.isAvailable()) {
                lastError = JarvisError.of(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        "Tool is unavailable: " + normalizedId,
                        "CapabilityExecutor"
                );
                continue;
            }

            /*
             * A tool may independently require owner authorization.
             * The executor does not bypass that requirement.
             */
            if (tool.requiresOwnerAuthorization(input)) {
                lastError = JarvisError.of(
                        JarvisError.Type.NOT_AUTHORIZED,
                        "Tool requires owner authorization: "
                                + normalizedId,
                        "CapabilityExecutor"
                );
                continue;
            }

            /*
             * JarvisRuntime stores the actual executable tool.
             *
             * IMPORTANT:
             * JarvisRuntime.getTool() returns ToolContract directly,
             * not JarvisResult.
             */
            ToolContract runtimeTool =
                    runtime.getTool(normalizedId);

            if (runtimeTool == null) {

                JarvisResult<Boolean> registration =
                        runtime.registerTool(tool);

                if (!registration.isSuccess()) {
                    lastError = registration.getError();
                    continue;
                }

            } else if (runtimeTool != tool) {

                /*
                 * Registry and runtime may contain different
                 * instances under the same ID.
                 *
                 * Replace the runtime instance so execution uses
                 * the current registry definition.
                 */
                JarvisResult<Boolean> replacement =
                        runtime.replaceTool(tool);

                if (!replacement.isSuccess()) {
                    lastError = replacement.getError();
                    continue;
                }
            }

            /*
             * Runtime must be running before execution.
             *
             * Starting it here is intentional: the executor is
             * responsible for making the execution route usable,
             * but it does not bypass any Android security boundary.
             */
            if (!runtime.isRunning()) {

                JarvisResult<Boolean> startResult =
                        runtime.start();

                if (!startResult.isSuccess()) {
                    lastError = startResult.getError();
                    continue;
                }
            }

            JarvisResult<ToolContract.ToolOutput> result =
                    runtime.execute(
                            normalizedId,
                            input
                    );

            if (result != null && result.isSuccess()) {
                return result;
            }

            if (result != null) {
                lastError = result.getError();
            }
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

    /**
     * Finds a tool in the registry.
     */
    private ToolContract findTool(String toolId) {

        if (toolId == null ||
                toolId.trim().isEmpty()) {
            return null;
        }

        JarvisResult<ToolContract> result =
                toolRegistry.get(toolId.trim());

        if (result == null ||
                !result.isSuccess()) {
            return null;
        }

        return result.getData();
    }

    /**
     * Returns missing permissions from a plan.
     */
    public Set<CapabilityPermission> getMissingPermissions(
            CapabilityPlan plan) {

        if (plan == null) {
            return Collections.emptySet();
        }

        Set<CapabilityPermission> missing =
                plan.getMissingPermissions();

        if (missing == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(missing);
    }

    /**
     * Checks whether a plan can currently execute.
     */
    public boolean canExecute(
            CapabilityPlan plan) {

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
                    findTool(toolId.trim());

            if (tool == null ||
                    !tool.isAvailable()) {
                continue;
            }

            /*
             * We deliberately do not call
             * requiresOwnerAuthorization(null) here because
             * individual tools may legitimately expect real input.
             *
             * The definitive authorization check occurs during
             * executeTool(), with the actual ToolInput.
             */
            return true;
        }

        return false;
    }

    /**
     * Checks whether all permissions required by a plan
     * are currently represented as granted.
     */
    public boolean hasRequiredPermissions(
            CapabilityPlan plan) {

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
            String message) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "CapabilityExecutor"
                )
        );
    }

    private String buildPermissionMessage(
            Set<CapabilityPermission> permissions) {

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

            if (permission != null) {
                builder.append(permission.getId());
            } else {
                builder.append("UNKNOWN");
            }

            first = false;
        }

        return builder.toString();
    }
}