package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * JARVIS V2 - Capability Discovery
 *
 * يكتشف شنو يقدر JARVIS ينفذ حالياً،
 * شنو ناقصو من صلاحيات،
 * واش كاين بديل،
 * وواش خاص Evolution تبني قدرة جديدة.
 *
 * هذا الكلاس لا ينفذ الأدوات.
 */
public final class CapabilityDiscovery {

    private final ToolRegistry toolRegistry;
    private final PermissionManager permissionManager;

    public CapabilityDiscovery(
            ToolRegistry toolRegistry,
            PermissionManager permissionManager
    ) {

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

        this.toolRegistry = toolRegistry;
        this.permissionManager = permissionManager;
    }

    /**
     * تحليل CapabilityRequirement.
     */
    public DiscoveryResult discover(
            CapabilityRequirement requirement
    ) {

        if (requirement == null) {

            return DiscoveryResult.invalid(
                    "Capability requirement is null."
            );
        }

        List<ToolContract> preferredTools =
                findTools(
                        requirement.getPreferredToolIds()
                );

        List<ToolContract> alternativeTools =
                findTools(
                        requirement.getAlternativeToolIds()
                );

        Set<CapabilityPermission> missing =
                requirement.getMissingPermissions(
                        permissionManager
                );

        boolean preferredPermissionsAvailable =
                requirement.hasRequiredPermissions(
                        permissionManager
                );

        boolean alternativePermissionsAvailable =
                requirement.hasAvailableAlternative(
                        permissionManager
                );

        boolean preferredToolAvailable =
                hasAvailableTool(
                        preferredTools
                );

        boolean alternativeToolAvailable =
                hasAvailableTool(
                        alternativeTools
                );

        /*
         * المسار المباشر:
         * Tool موجود + الصلاحيات موجودة.
         */
        if (preferredToolAvailable &&
                preferredPermissionsAvailable) {

            return DiscoveryResult.direct(
                    requirement,
                    preferredTools
            );
        }

        /*
         * المسار البديل:
         * Tool بديل موجود + المتطلبات البديلة متوفرة.
         */
        if (alternativeToolAvailable &&
                alternativePermissionsAvailable) {

            return DiscoveryResult.alternative(
                    requirement,
                    alternativeTools
            );
        }

        /*
         * في بعض الحالات قد يكون Tool المفضل
         * موجوداً ولكن المتطلبات الأصلية لا تنطبق،
         * بينما الصلاحيات العامة المطلوبة متوفرة.
         */
        if (preferredToolAvailable &&
                alternativePermissionsAvailable) {

            return DiscoveryResult.direct(
                    requirement,
                    preferredTools
            );
        }

        /*
         * إذا كانت هناك صلاحيات ناقصة،
         * لا نبني قدرة جديدة لمجرد تجاوز Permission.
         *
         * Evolution هنا يبقى لمسار capability،
         * وليس لتجاوز Android/User security.
         */
        if (!missing.isEmpty()) {

            if (requirement.canBuildAlternative()) {

                return DiscoveryResult.needsEvolution(
                        requirement,
                        missing
                );
            }

            return DiscoveryResult.needsPermission(
                    requirement,
                    missing
            );
        }

        /*
         * لا توجد صلاحيات ناقصة ولكن لا توجد أداة.
         * هنا يمكن لـEvolution بناء قدرة جديدة.
         */
        if (!preferredToolAvailable &&
                !alternativeToolAvailable &&
                requirement.canBuildAlternative()) {

            return DiscoveryResult.needsEvolution(
                    requirement,
                    Collections.emptySet()
            );
        }

        return DiscoveryResult.unavailable(
                requirement,
                missing
        );
    }

    /**
     * إيجاد الأدوات المسجلة.
     *
     * مهم:
     * ToolRegistry.get() يرجع JarvisResult<ToolContract>
     * وليس ToolContract مباشرة.
     */
    private List<ToolContract> findTools(
            List<String> toolIds
    ) {

        List<ToolContract> result =
                new ArrayList<>();

        if (toolIds == null ||
                toolIds.isEmpty()) {

            return result;
        }

        for (String id : toolIds) {

            if (id == null ||
                    id.trim().isEmpty()) {

                continue;
            }

            JarvisResult<ToolContract> lookup =
                    toolRegistry.get(
                            id.trim()
                    );

            if (lookup == null ||
                    !lookup.isSuccess()) {

                continue;
            }

            ToolContract tool =
                    lookup.getData();

            if (tool != null) {

                result.add(tool);
            }
        }

        return result;
    }

    /**
     * واش كاين Tool متاح.
     */
    private boolean hasAvailableTool(
            List<ToolContract> tools
    ) {

        if (tools == null ||
                tools.isEmpty()) {

            return false;
        }

        for (ToolContract tool : tools) {

            if (tool != null &&
                    tool.isAvailable()) {

                return true;
            }
        }

        return false;
    }

    /**
     * نتيجة Discovery.
     */
    public static final class DiscoveryResult {

        public enum Status {

            DIRECT,

            ALTERNATIVE,

            NEEDS_PERMISSION,

            NEEDS_EVOLUTION,

            UNAVAILABLE,

            INVALID
        }

        private final Status status;

        private final CapabilityRequirement requirement;

        private final List<ToolContract> availableTools;

        private final Set<CapabilityPermission>
                missingPermissions;

        private final String message;

        private DiscoveryResult(
                Status status,
                CapabilityRequirement requirement,
                List<ToolContract> availableTools,
                Set<CapabilityPermission> missingPermissions,
                String message
        ) {

            this.status = status;

            this.requirement =
                    requirement;

            this.availableTools =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    availableTools == null
                                            ? Collections.emptyList()
                                            : availableTools
                            )
                    );

            EnumSet<CapabilityPermission> missing =
                    EnumSet.noneOf(
                            CapabilityPermission.class
                    );

            if (missingPermissions != null) {

                missing.addAll(
                        missingPermissions
                );
            }

            this.missingPermissions =
                    Collections.unmodifiableSet(
                            missing
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public static DiscoveryResult direct(
                CapabilityRequirement requirement,
                List<ToolContract> tools
        ) {

            return new DiscoveryResult(
                    Status.DIRECT,
                    requirement,
                    tools,
                    Collections.emptySet(),
                    "A direct execution path is available."
            );
        }

        public static DiscoveryResult alternative(
                CapabilityRequirement requirement,
                List<ToolContract> tools
        ) {

            return new DiscoveryResult(
                    Status.ALTERNATIVE,
                    requirement,
                    tools,
                    Collections.emptySet(),
                    "An alternative execution path is available."
            );
        }

        public static DiscoveryResult needsPermission(
                CapabilityRequirement requirement,
                Set<CapabilityPermission> missing
        ) {

            return new DiscoveryResult(
                    Status.NEEDS_PERMISSION,
                    requirement,
                    Collections.emptyList(),
                    missing,
                    "Required permission or capability is missing."
            );
        }

        public static DiscoveryResult needsEvolution(
                CapabilityRequirement requirement,
                Set<CapabilityPermission> missing
        ) {

            return new DiscoveryResult(
                    Status.NEEDS_EVOLUTION,
                    requirement,
                    Collections.emptyList(),
                    missing,
                    "No suitable current path exists. Evolution may be required."
            );
        }

        public static DiscoveryResult unavailable(
                CapabilityRequirement requirement,
                Set<CapabilityPermission> missing
        ) {

            return new DiscoveryResult(
                    Status.UNAVAILABLE,
                    requirement,
                    Collections.emptyList(),
                    missing,
                    "No currently usable execution path was discovered."
            );
        }

        public static DiscoveryResult invalid(
                String message
        ) {

            return new DiscoveryResult(
                    Status.INVALID,
                    null,
                    Collections.emptyList(),
                    Collections.emptySet(),
                    message
            );
        }

        public Status getStatus() {
            return status;
        }

        public CapabilityRequirement
        getRequirement() {
            return requirement;
        }

        public List<ToolContract>
        getAvailableTools() {
            return availableTools;
        }

        public Set<CapabilityPermission>
        getMissingPermissions() {
            return missingPermissions;
        }

        public String getMessage() {
            return message;
        }

        public boolean canExecuteDirectly() {
            return status ==
                    Status.DIRECT;
        }

        public boolean hasAlternative() {
            return status ==
                    Status.ALTERNATIVE;
        }

        public boolean needsPermission() {
            return status ==
                    Status.NEEDS_PERMISSION;
        }

        public boolean needsEvolution() {
            return status ==
                    Status.NEEDS_EVOLUTION;
        }

        public boolean isUnavailable() {
            return status ==
                    Status.UNAVAILABLE;
        }

        public boolean isValid() {
            return status !=
                    Status.INVALID;
        }

        @Override
        public String toString() {

            return "DiscoveryResult{" +
                    "status=" +
                    status +
                    ", requirement=" +
                    requirement +
                    ", availableTools=" +
                    availableTools.size() +
                    ", missingPermissions=" +
                    missingPermissions +
                    ", message='" +
                    message +
                    '\'' +
                    '}';
        }
    }
}