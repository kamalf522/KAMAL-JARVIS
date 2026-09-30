package com.kamal.jarvis.v2.intelligence;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.CapabilityDiscovery;
import com.kamal.jarvis.v2.evolution.CapabilityPlan;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JARVIS V2 - Planning Engine
 *
 * التخطيط مبني على الهدف والقدرات،
 * وليس على لائحة أوامر ثابتة.
 *
 * المسار:
 * فهم الطلب
 * -> تحديد المتطلبات
 * -> اكتشاف الأدوات
 * -> إنشاء الخطة
 * -> تنفيذ / صلاحية / Evolution
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
                createRequirement(understanding);

        if (requirement == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability requirement could not be created."
            );
        }

        CapabilityDiscovery.DiscoveryResult discovery =
                capabilityDiscovery.discover(requirement);

        if (discovery == null) {
            state = PlanningState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability discovery returned no result."
            );
        }

        CapabilityPlan capabilityPlan =
                CapabilityPlan.fromDiscovery(discovery);

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
     * يحول فهم الطلب إلى متطلبات حقيقية.
     *
     * لا توجد لائحة ثابتة تربط Intent
     * بأداة معينة.
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
                CommandUnderstanding.Requirement required
                : understanding.getRequirements()
        ) {

            switch (required) {

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
                case DESTRUCTIVE_OPERATION:
                    builder.requireOwnerAuthorization();
                    break;

                default:
                    break;
            }
        }

        /*
         * البحث الديناميكي عن الأدوات الموجودة.
         */
        List<ToolMatch> matches =
                findMatchingTools(understanding);

        for (ToolMatch match : matches) {

            if (match.score >= 0.55d) {

                builder.preferTool(
                        match.tool.getId()
                );
            }
        }

        /*
         * إذا ما كانتش القدرة موجودة،
         * EvolutionCore يقدر يحاول يبنيها.
         */
        builder.allowAlternativeBuilding();

        return builder.build();
    }

    /**
     * البحث الديناميكي داخل ToolRegistry.
     *
     * يعتمد على:
     * ID
     * الاسم
     * الوصف
     * metadata
     * هدف المستخدم
     * الأمر الأصلي
     */
    private List<ToolMatch> findMatchingTools(
            CommandUnderstanding.Result understanding
    ) {
        List<ToolMatch> matches =
                new ArrayList<>();

        String goal =
                safe(understanding.getGoal());

        String command =
                safe(understanding.getOriginalCommand());

        String normalized =
                safe(understanding.getNormalizedCommand());

        List<String> requestTokens =
                tokenize(
                        goal + " " +
                        command + " " +
                        normalized
                );

        for (ToolContract tool :
                toolRegistry.getTools()) {

            if (tool == null ||
                    !tool.isAvailable()) {
                continue;
            }

            double score =
                    scoreTool(
                            tool,
                            requestTokens,
                            goal
                    );

            if (score > 0d) {

                matches.add(
                        new ToolMatch(
                                tool,
                                score
                        )
                );
            }
        }

        Collections.sort(
                matches,
                new Comparator<ToolMatch>() {

                    @Override
                    public int compare(
                            ToolMatch left,
                            ToolMatch right
                    ) {
                        return Double.compare(
                                right.score,
                                left.score
                        );
                    }
                }
        );

        /*
         * ناخدو غير أحسن 5 أدوات
         * باش الخطة تبقى واضحة.
         */
        if (matches.size() > 5) {

            return new ArrayList<>(
                    matches.subList(0, 5)
            );
        }

        return matches;
    }

    private double scoreTool(
            ToolContract tool,
            List<String> requestTokens,
            String goal
    ) {
        String searchable =
                safe(tool.getId()) + " " +
                safe(tool.getName()) + " " +
                safe(tool.getDescription());

        Map<String, Object> metadata =
                tool.getMetadata();

        if (metadata != null &&
                !metadata.isEmpty()) {

            searchable += " " +
                    metadata.toString();
        }

        List<String> toolTokens =
                tokenize(searchable);

        if (toolTokens.isEmpty() ||
                requestTokens.isEmpty()) {

            return 0d;
        }

        int matches = 0;

        for (String token : requestTokens) {

            if (token.length() < 3) {
                continue;
            }

            if (toolTokens.contains(token)) {
                matches++;
                continue;
            }

            for (String toolToken : toolTokens) {

                if (toolToken.contains(token) ||
                        token.contains(toolToken)) {

                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0d;
        }

        double score =
                (double) matches /
                        Math.max(
                                1,
                                Math.min(
                                        requestTokens.size(),
                                        8
                                )
                        );

        String lowerGoal =
                goal.toLowerCase();

        String lowerTool =
                searchable.toLowerCase();

        /*
         * إذا كان الهدف كامل موجود
         * فالوصف ديال الأداة، نزيدو الثقة.
         */
        if (!lowerGoal.isEmpty() &&
                lowerTool.contains(lowerGoal)) {

            score += 0.25d;
        }

        return Math.min(
                1.0d,
                score
        );
    }

    private List<String> tokenize(
            String value
    ) {
        if (value == null ||
                value.trim().isEmpty()) {

            return Collections.emptyList();
        }

        String normalized =
                value.toLowerCase()
                        .replaceAll(
                                "[^\\p{L}\\p{N}_]+",
                                " "
                        );

        String[] parts =
                normalized
                        .trim()
                        .split("\\s+");

        Set<String> unique =
                new LinkedHashSet<>();

        for (String part : parts) {

            if (part.length() >= 2) {
                unique.add(part);
            }
        }

        return new ArrayList<>(unique);
    }

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

    public synchronized boolean canPlan(
            CommandUnderstanding.Result understanding
    ) {
        if (understanding == null ||
                !understanding.isValid()) {

            return false;
        }

        CapabilityRequirement requirement =
                createRequirement(understanding);

        if (requirement == null) {
            return false;
        }

        CapabilityDiscovery.DiscoveryResult discovery =
                capabilityDiscovery.discover(
                        requirement
                );

        return discovery != null &&
                discovery.isValid();
    }

    public Set<CapabilityPermission>
    getMissingPermissions(
            CommandUnderstanding.Result understanding
    ) {
        if (understanding == null ||
                !understanding.isValid()) {

            return Collections.emptySet();
        }

        CapabilityRequirement requirement =
                createRequirement(understanding);

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
        return state ==
                PlanningState.READY;
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

    private String safe(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private static final class ToolMatch {

        private final ToolContract tool;
        private final double score;

        private ToolMatch(
                ToolContract tool,
                double score
        ) {
            this.tool = tool;
            this.score = score;
        }
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

    public static final class Plan {

        private final CommandUnderstanding.Result understanding;
        private final CapabilityRequirement requirement;
        private final CapabilityPlan capabilityPlan;
        private final PlanAction action;
        private final List<String> toolIds;
        private final Set<CapabilityPermission> missingPermissions;
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
            this.understanding = understanding;
            this.requirement = requirement;
            this.capabilityPlan = capabilityPlan;
            this.action = action;

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
                    : requirement.getCapabilityId();
        }

        public String getGoal() {
            return requirement == null
                    ? ""
                    : requirement.getDescription();
        }

        public String getSummary() {
            StringBuilder result =
                    new StringBuilder();

            result.append("Plan=")
                    .append(action.name())
                    .append(", capability=")
                    .append(getCapabilityId())
                    .append(", tools=")
                    .append(toolIds);

            if (!missingPermissions.isEmpty()) {

                result.append(
                        ", missingPermissions="
                ).append(
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
                    "action=" + action +
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