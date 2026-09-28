package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.CapabilityDiscovery;
import com.kamal.jarvis.v2.evolution.CapabilityPlan;
import com.kamal.jarvis.v2.evolution.CapabilityRequirement;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JARVIS V2 - Planning Engine
 *
 * محرك التخطيط.
 *
 * المسؤوليات:
 * 1. استقبال نتيجة فهم الأمر.
 * 2. تحويل الهدف إلى CapabilityRequirement.
 * 3. اكتشاف الأدوات المتوفرة.
 * 4. معرفة الصلاحيات الناقصة.
 * 5. اختيار المسار المباشر أو البديل.
 * 6. تحديد هل نحتاج Evolution / Build.
 * 7. إنتاج خطة تنفيذ واضحة للطبقات الأعلى.
 *
 * PlanningEngine لا ينفذ الأدوات بنفسه.
 * التنفيذ يبقى في Execution/CapabilityExecutor.
 *
 * Security ليست قابلة للتجاوز من هذا الملف.
 */
public final class PlanningEngine {

    private static final String ENGINE_ID =
            "v2.planning_engine";

    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;
    private final CapabilityDiscovery capabilityDiscovery;

    private volatile PlanningState state =
            PlanningState.IDLE;

    private volatile Plan lastPlan;

    public PlanningEngine(
            ToolRegistry toolRegistry,
            PermissionManager permissionManager
    ) {
        if (toolRegistry == null) {
            throw new IllegalArgumentException(
                    "toolRegistry cannot be null."
            );
        }

        if (permissionManager == null) {
            throw new IllegalArgumentException(
                    "permissionManager cannot be null."
            );
        }

        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;

        this.capabilityDiscovery =
                new CapabilityDiscovery(
                        toolRegistry,
                        permissionManager
                );
    }

    /**
     * يبني خطة انطلاقاً من نتيجة فهم الأمر.
     */
    public synchronized JarvisResult<Plan> createPlan(
            CommandUnderstanding.Result understanding
    ) {

        if (understanding == null ||
                !understanding.isValid()) {

            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Cannot create a plan from an invalid command."
            );
        }

        state = PlanningState.ANALYZING;

        CapabilityRequirement requirement =
                createRequirement(
                        understanding
                );

        if (requirement == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability requirement could not be created."
            );
        }

        CapabilityDiscovery.DiscoveryResult discovery =
                capabilityDiscovery.discover(
                        requirement
                );

        if (discovery == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability discovery returned no result."
            );
        }

        CapabilityPlan capabilityPlan =
                CapabilityPlan.fromDiscovery(
                        discovery
                );

        if (capabilityPlan == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability plan could not be created."
            );
        }

        Plan plan =
                buildPlan(
                        understanding,
                        requirement,
                        discovery,
                        capabilityPlan
                );

        if (plan == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Planning failed."
            );
        }

        lastPlan = plan;

        if (plan.requiresEvolution()) {
            state = PlanningState.EVOLUTION_REQUIRED;
        } else if (plan.requiresPermission()) {
            state = PlanningState.PERMISSION_REQUIRED;
        } else if (plan.canExecute()) {
            state = PlanningState.READY;
        } else {
            state = PlanningState.UNAVAILABLE;
        }

        return JarvisResult.success(
                plan,
                plan.getSummary()
        );
    }

    /**
     * إنشاء Requirement من فهم الأمر.
     */
    private CapabilityRequirement createRequirement(
            CommandUnderstanding.Result understanding
    ) {

        CapabilityRequirement.Builder builder =
                CapabilityRequirement.builder(
                        understanding.getCapabilityId(),
                        understanding.getGoal()
                );

        for (
                CommandUnderstanding.Requirement requirement
                : understanding.getRequirements()
        ) {

            switch (requirement) {

                case NOTIFICATIONS:
                    builder.requirePermission(
                            CapabilityPermission.NOTIFICATIONS
                    );
                    break;

                case BACKGROUND_EXECUTION:
                    builder.requirePermission(
                            CapabilityPermission.BACKGROUND_EXECUTION
                    );
                    break;

                case FILE_ACCESS:
                    builder.requirePermission(
                            CapabilityPermission.FILE_READ
                    );
                    builder.requirePermission(
                            CapabilityPermission.FILE_WRITE
                    );
                    break;

                case NETWORK:
                    builder.requirePermission(
                            CapabilityPermission.NETWORK
                    );
                    break;

                case MICROPHONE:
                    builder.requirePermission(
                            CapabilityPermission.MICROPHONE
                    );
                    break;

                case PROJECT_ACCESS:
                    builder.requirePermission(
                            CapabilityPermission.PROJECT_READ
                    );
                    builder.requirePermission(
                            CapabilityPermission.PROJECT_WRITE
                    );
                    break;

                case BUILD_ACCESS:
                    builder.requirePermission(
                            CapabilityPermission.BUILD_PROJECT
                    );
                    break;

                case TEST_ACCESS:
                    builder.requirePermission(
                            CapabilityPermission.RUN_TESTS
                    );
                    break;

                case OWNER_AUTHORIZATION:
                    builder.requireOwnerAuthorization();
                    break;

                case DESTRUCTIVE_OPERATION:
                    /*
                     * العملية التخريبية لا تتحول إلى Android
                     * permission تلقائياً.
                     *
                     * هي security-sensitive ويتم الاحتفاظ بها
                     * داخل الخطة حتى تتعامل معها طبقة التنفيذ.
                     */
                    builder.requireOwnerAuthorization();
                    break;

                default:
                    break;
            }
        }

        addTools(
                builder,
                understanding
        );

        /*
         * السماح بالبناء البديل عندما لا توجد أداة مناسبة.
         */
        builder.allowAlternativeBuilding();

        return builder.build();
    }

    /**
     * ربط نوع الأمر بالأدوات المعروفة.
     */
    private void addTools(
            CapabilityRequirement.Builder builder,
            CommandUnderstanding.Result understanding
    ) {

        switch (understanding.getIntentType()) {

            case REMINDER:

                builder.preferTool(
                        "android.reminders"
                );

                builder.alternativeTool(
                        "android.notifications"
                );

                break;

            case FILE_OPERATION:

                builder.preferTool(
                        "android.files"
                );

                builder.alternativeTool(
                        "workspace.files"
                );

                break;

            case APP_ACTION:

                builder.preferTool(
                        "android.app_launcher"
                );

                builder.alternativeTool(
                        "android.intent"
                );

                break;

            case SEARCH:

                builder.preferTool(
                        "network.search"
                );

                builder.alternativeTool(
                        "network.http"
                );

                break;

            case VOICE:

                builder.preferTool(
                        "android.microphone"
                );

                break;

            case DEVELOPMENT:

                builder.preferTool(
                        "project.inspector"
                );

                builder.preferTool(
                        "project.builder"
                );

                builder.alternativeTool(
                        "project.workspace"
                );

                break;

            case SETTINGS:

                builder.preferTool(
                        "android.settings"
                );

                builder.alternativeTool(
                        "android.intent"
                );

                break;

            case INFORMATION:

                builder.preferTool(
                        "jarvis.information"
                );

                builder.alternativeTool(
                        "network.search"
                );

                break;

            case NETWORK:

                builder.preferTool(
                        "network.http"
                );

                builder.alternativeTool(
                        "network.search"
                );

                break;

            case GENERAL:

            default:

                builder.preferTool(
                        "jarvis.general"
                );

                builder.alternativeTool(
                        "jarvis.capability"
                );

                break;
        }
    }

    /**
     * تحويل Discovery + CapabilityPlan
     * إلى خطة أعلى مستوى يفهمها JARVIS.
     */
    private Plan buildPlan(
            CommandUnderstanding.Result understanding,
            CapabilityRequirement requirement,
            CapabilityDiscovery.DiscoveryResult discovery,
            CapabilityPlan capabilityPlan
    ) {

        List<String> tools =
                new ArrayList<>(
                        capabilityPlan.getToolIds()
                );

        Set<CapabilityPermission> missing =
                new LinkedHashSet<>(
                        capabilityPlan.getMissingPermissions()
                );

        PlanAction action =
                mapAction(
                        capabilityPlan.getAction()
                );

        boolean evolution =
                capabilityPlan.shouldBuildCapability();

        boolean permission =
                capabilityPlan.shouldRequestPermission();

        boolean executable =
                capabilityPlan.shouldExecute();

        String reason =
                capabilityPlan.getReason();

        if (reason == null ||
                reason.trim().isEmpty()) {

            reason =
                    discovery.getMessage();
        }

        return new Plan(
                understanding,
                requirement,
                capabilityPlan,
                action,
                tools,
                missing,
                evolution,
                permission,
                executable,
                requirement.isOwnerAuthorizationRequired(),
                reason
        );
    }

    private PlanAction mapAction(
            CapabilityPlan.Action action
    ) {

        if (action == null) {
            return PlanAction.UNAVAILABLE;
        }

        switch (action) {

            case EXECUTE_DIRECT:
                return PlanAction.EXECUTE_DIRECT;

            case EXECUTE_ALTERNATIVE:
                return PlanAction.EXECUTE_ALTERNATIVE;

            case REQUEST_PERMISSION:
                return PlanAction.REQUEST_PERMISSION;

            case BUILD_CAPABILITY:
                return PlanAction.BUILD_CAPABILITY;

            case UNAVAILABLE:
            default:
                return PlanAction.UNAVAILABLE;
        }
    }

    /**
     * يسمح بفحص سريع للخطة بدون تنفيذ.
     */
    public synchronized boolean canPlan(
            CommandUnderstanding.Result understanding
    ) {

        if (understanding == null ||
                !understanding.isValid()) {
            return false;
        }

        CapabilityRequirement requirement =
                createRequirement(
                        understanding
                );

        if (requirement == null) {
            return false;
        }

        CapabilityDiscovery.DiscoveryResult discovery =
                capabilityDiscovery.discover(
                        requirement
                );

        return discovery != null;
    }

    /**
     * فحص الصلاحيات الناقصة مباشرة.
     */
    public Set<CapabilityPermission>
    getMissingPermissions(
            CommandUnderstanding.Result understanding
    ) {

        if (understanding == null ||
                !understanding.isValid()) {

            return Collections.emptySet();
        }

        CapabilityRequirement requirement =
                createRequirement(
                        understanding
                );

        if (requirement == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(
                new LinkedHashSet<>(
                        requirement.getMissingPermissions(
                                permissionManager
                        )
                )
        );
    }

    public PlanningState getState() {
        return state;
    }

    public Plan getLastPlan() {
        return lastPlan;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public CapabilityDiscovery getCapabilityDiscovery() {
        return capabilityDiscovery;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    public boolean isReady() {
        return state == PlanningState.READY;
    }

    public boolean requiresEvolution() {
        return state ==
                PlanningState.EVOLUTION_REQUIRED;
    }

    public boolean requiresPermission() {
        return state ==
                PlanningState.PERMISSION_REQUIRED;
    }

    public boolean isUnavailable() {
        return state ==
                PlanningState.UNAVAILABLE;
    }

    public String getStatus() {
        return state.name();
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        ENGINE_ID
                )
        );
    }

    public enum PlanningState {

        IDLE,

        ANALYZING,

        READY,

        PERMISSION_REQUIRED,

        EVOLUTION_REQUIRED,

        UNAVAILABLE,

        FAILED
    }

    public enum PlanAction {

        EXECUTE_DIRECT,

        EXECUTE_ALTERNATIVE,

        REQUEST_PERMISSION,

        BUILD_CAPABILITY,

        UNAVAILABLE
    }

    /**
     * الخطة النهائية التي تنتقل إلى طبقة التنفيذ.
     */
    public static final class Plan {

        private final CommandUnderstanding.Result understanding;
        private final CapabilityRequirement requirement;
        private final CapabilityPlan capabilityPlan;
        private final PlanAction action;
        private final List<String> toolIds;
        private final Set<CapabilityPermission>
                missingPermissions;
        private final boolean requiresEvolution;
        private final boolean requiresPermission;
        private final boolean executable;
        private final boolean ownerAuthorizationRequired;
        private final String reason;

        private Plan(
                CommandUnderstanding.Result understanding,
                CapabilityRequirement requirement,
                CapabilityPlan capabilityPlan,
                PlanAction action,
                List<String> toolIds,
                Set<CapabilityPermission> missingPermissions,
                boolean requiresEvolution,
                boolean requiresPermission,
                boolean executable,
                boolean ownerAuthorizationRequired,
                String reason
        ) {

            this.understanding =
                    understanding;

            this.requirement =
                    requirement;

            this.capabilityPlan =
                    capabilityPlan;

            this.action =
                    action;

            this.toolIds =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    toolIds
                            )
                    );

            this.missingPermissions =
                    Collections.unmodifiableSet(
                            new LinkedHashSet<>(
                                    missingPermissions
                            )
                    );

            this.requiresEvolution =
                    requiresEvolution;

            this.requiresPermission =
                    requiresPermission;

            this.executable =
                    executable;

            this.ownerAuthorizationRequired =
                    ownerAuthorizationRequired;

            this.reason =
                    reason == null
                            ? ""
                            : reason;
        }

        public CommandUnderstanding.Result
        getUnderstanding() {
            return understanding;
        }

        public CapabilityRequirement
        getRequirement() {
            return requirement;
        }

        public CapabilityPlan
        getCapabilityPlan() {
            return capabilityPlan;
        }

        public PlanAction getAction() {
            return action;
        }

        public List<String> getToolIds() {
            return toolIds;
        }

        public Set<CapabilityPermission>
        getMissingPermissions() {
            return missingPermissions;
        }

        public boolean requiresEvolution() {
            return requiresEvolution;
        }

        public boolean requiresPermission() {
            return requiresPermission;
        }

        public boolean canExecute() {
            return executable;
        }

        public boolean requiresOwnerAuthorization() {
            return ownerAuthorizationRequired;
        }

        public boolean isDirectExecution() {
            return action ==
                    PlanAction.EXECUTE_DIRECT;
        }

        public boolean isAlternativeExecution() {
            return action ==
                    PlanAction.EXECUTE_ALTERNATIVE;
        }

        public boolean isBuildRequired() {
            return action ==
                    PlanAction.BUILD_CAPABILITY;
        }

        public boolean isUnavailable() {
            return action ==
                    PlanAction.UNAVAILABLE;
        }

        public String getReason() {
            return reason;
        }

        public String getCapabilityId() {

            return requirement == null
                    ? ""
                    : requirement
                    .getCapabilityId();
        }

        public String getGoal() {

            return requirement == null
                    ? ""
                    : requirement
                    .getDescription();
        }

        public String getSummary() {

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    "Plan="
            );

            result.append(
                    action.name()
            );

            result.append(
                    ", capability="
            );

            result.append(
                    getCapabilityId()
            );

            result.append(
                    ", tools="
            );

            result.append(
                    toolIds
            );

            if (!missingPermissions.isEmpty()) {

                result.append(
                        ", missingPermissions="
                );

                result.append(
                        missingPermissions
                );
            }

            if (requiresEvolution) {

                result.append(
                        ", evolutionRequired=true"
                );
            }

            return result.toString();
        }

        @Override
        public String toString() {

            return "Plan{" +
                    "action=" +
                    action +
                    ", capabilityId='" +
                    getCapabilityId() +
                    '\'' +
                    ", toolIds=" +
                    toolIds +
                    ", missingPermissions=" +
                    missingPermissions +
                    ", requiresEvolution=" +
                    requiresEvolution +
                    ", requiresPermission=" +
                    requiresPermission +
                    ", executable=" +
                    executable +
                    '}';
        }
    }
}