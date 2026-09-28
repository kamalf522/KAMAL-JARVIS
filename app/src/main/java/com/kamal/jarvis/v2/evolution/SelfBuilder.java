package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JARVIS V2 - Self Builder
 *
 * مسؤول عن إنشاء Workspace منظم وآمن للقدرات الجديدة.
 *
 * مهم:
 * هذا المكون لا يتجاوز Android Security ولا يعدل APK المثبت
 * مباشرة. هو يبني artifacts داخل Workspace مسموح به،
 * ثم يمكن للطبقات الأعلى استعمالها في دورة البناء والاختبار.
 */
public final class SelfBuilder {

    private static final String BUILDER_ID = "v2.self_builder";

    private final OwnerSecurityBoundary securityBoundary;
    private final File workspaceRoot;

    private final Set<String> protectedPathNames =
            Collections.synchronizedSet(new HashSet<String>());

    private volatile boolean initialized;

    public SelfBuilder(
            OwnerSecurityBoundary securityBoundary,
            File workspaceRoot
    ) {
        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null"
            );
        }

        if (workspaceRoot == null) {
            throw new IllegalArgumentException(
                    "workspaceRoot cannot be null"
            );
        }

        this.securityBoundary = securityBoundary;
        this.workspaceRoot = workspaceRoot.getAbsoluteFile();
        this.initialized = false;

        initializeProtectedPathNames();
    }

    /**
     * Initializes the builder workspace.
     */
    public synchronized JarvisResult<Boolean> initialize() {

        if (initialized) {
            return JarvisResult.success(
                    true,
                    "SelfBuilder is already initialized."
            );
        }

        try {
            if (!workspaceRoot.exists()) {

                if (!workspaceRoot.mkdirs()
                        && !workspaceRoot.exists()) {

                    return failure(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Cannot create builder workspace."
                    );
                }
            }

            if (!workspaceRoot.isDirectory()) {
                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Builder workspace is not a directory."
                );
            }

            File canonicalRoot =
                    workspaceRoot.getCanonicalFile();

            if (!canonicalRoot.canRead()
                    || !canonicalRoot.canWrite()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Builder workspace is not readable and writable."
                );
            }

            initialized = true;

            return JarvisResult.success(
                    true,
                    "SelfBuilder initialized successfully."
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Cannot initialize builder workspace.",
                            "SelfBuilder",
                            e
                    )
            );
        }
    }

    /**
     * Builds the workspace representation of a capability.
     */
    public synchronized JarvisResult<BuildResult> build(
            CapabilitySpec spec
    ) {

        if (spec == null) {
            return failureResult(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (!initialized) {

            JarvisResult<Boolean> initResult =
                    initialize();

            if (!initResult.isSuccess()) {
                return JarvisResult.failure(
                        initResult.getError()
                );
            }
        }

        JarvisResult<Boolean> validation =
                validateSpec(spec);

        if (!validation.isSuccess()) {
            return JarvisResult.failure(
                    validation.getError()
            );
        }

        /*
         * Owner authorization is a security prerequisite.
         * The builder never initializes or changes the owner.
         */
        if (spec.isOwnerAuthorizationRequired()
                && !securityBoundary.isActive()) {

            return failureResult(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Owner security boundary is not active."
            );
        }

        /*
         * Project modification is allowed only outside the
         * protected security areas.
         */
        if (spec.canModifyProjectFiles()) {

            JarvisResult<Void> securityCheck =
                    securityBoundary.authorizeEvolutionChange(
                            "CAPABILITY_PROJECT_FILES"
                    );

            if (!securityCheck.isSuccess()) {
                return JarvisResult.failure(
                        securityCheck.getError()
                );
            }
        }

        try {

            File capabilityDirectory =
                    createCapabilityDirectory(
                            spec.getCapabilityId()
                    );

            List<File> createdFiles =
                    new ArrayList<>();

            File manifestFile =
                    createCapabilityManifest(
                            capabilityDirectory,
                            spec
                    );

            createdFiles.add(manifestFile);

            File specificationFile =
                    createSpecificationFile(
                            capabilityDirectory,
                            spec
                    );

            createdFiles.add(specificationFile);

            BuildResult result =
                    new BuildResult(
                            spec.getCapabilityId(),
                            capabilityDirectory,
                            createdFiles,
                            spec.requiresBuild(),
                            spec.requiresTests()
                    );

            return JarvisResult.success(
                    result,
                    "Capability build workspace created successfully."
            );

        } catch (SecurityException e) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            safeMessage(e),
                            "SelfBuilder"
                    )
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            "Capability workspace build failed.",
                            "SelfBuilder",
                            e
                    )
            );

        } catch (Exception e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            "Unexpected SelfBuilder failure.",
                            "SelfBuilder",
                            e
                    )
            );
        }
    }

    /**
     * Validates the capability before any filesystem mutation.
     */
    private JarvisResult<Boolean> validateSpec(
            CapabilitySpec spec
    ) {

        if (isBlank(spec.getCapabilityId())) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability ID is required."
            );
        }

        if (isBlank(spec.getName())) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability name is required."
            );
        }

        if (isBlank(spec.getGoal())) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability goal is required."
            );
        }

        if (spec.getSuccessCriteria() == null
                || spec.getSuccessCriteria().isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "At least one success criterion is required."
            );
        }

        if (containsProtectedPath(
                spec.getCapabilityId()
        )) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Capability targets a protected path."
            );
        }

        return JarvisResult.success(
                true,
                "Capability specification is valid."
        );
    }

    /**
     * Creates the isolated capability directory.
     */
    private File createCapabilityDirectory(
            String capabilityId
    ) throws IOException {

        String safeId =
                sanitizePathPart(capabilityId);

        File capabilitiesRoot =
                new File(
                        workspaceRoot,
                        "capabilities"
                ).getCanonicalFile();

        ensureInsideWorkspace(
                capabilitiesRoot
        );

        if (!capabilitiesRoot.exists()
                && !capabilitiesRoot.mkdirs()
                && !capabilitiesRoot.exists()) {

            throw new IOException(
                    "Cannot create capabilities directory."
            );
        }

        File directory =
                new File(
                        capabilitiesRoot,
                        safeId
                ).getCanonicalFile();

        ensureInsideWorkspace(directory);
        ensureNotProtected(directory);

        if (!directory.exists()
                && !directory.mkdirs()
                && !directory.exists()) {

            throw new IOException(
                    "Cannot create capability directory."
            );
        }

        if (!directory.isDirectory()) {
            throw new IOException(
                    "Capability path is not a directory."
            );
        }

        return directory;
    }

    /**
     * Creates the capability manifest.
     */
    private File createCapabilityManifest(
            File directory,
            CapabilitySpec spec
    ) throws IOException {

        File file =
                new File(
                        directory,
                        "capability.manifest"
                ).getCanonicalFile();

        ensureInsideWorkspace(file);
        ensureNotProtected(file);

        StringBuilder content =
                new StringBuilder();

        content.append("id=")
                .append(spec.getCapabilityId())
                .append('\n');

        content.append("name=")
                .append(spec.getName())
                .append('\n');

        content.append("goal=")
                .append(spec.getGoal())
                .append('\n');

        content.append("description=")
                .append(nullToEmpty(spec.getDescription()))
                .append('\n');

        content.append("ownerAuthorizationRequired=")
                .append(spec.isOwnerAuthorizationRequired())
                .append('\n');

        content.append("canModifyProjectFiles=")
                .append(spec.canModifyProjectFiles())
                .append('\n');

        content.append("requiresBuild=")
                .append(spec.requiresBuild())
                .append('\n');

        content.append("requiresTests=")
                .append(spec.requiresTests())
                .append('\n');

        writeFile(
                file,
                content.toString()
        );

        return file;
    }

    /**
     * Creates a human-readable capability specification.
     */
    private File createSpecificationFile(
            File directory,
            CapabilitySpec spec
    ) throws IOException {

        File file =
                new File(
                        directory,
                        "specification.txt"
                ).getCanonicalFile();

        ensureInsideWorkspace(file);
        ensureNotProtected(file);

        StringBuilder content =
                new StringBuilder();

        content.append(
                "JARVIS V2 CAPABILITY SPECIFICATION\n"
        );

        content.append(
                "----------------------------------\n"
        );

        content.append("Capability ID: ")
                .append(spec.getCapabilityId())
                .append('\n');

        content.append("Name: ")
                .append(spec.getName())
                .append('\n');

        content.append("Goal: ")
                .append(spec.getGoal())
                .append('\n');

        content.append("Description: ")
                .append(nullToEmpty(spec.getDescription()))
                .append('\n');

        content.append('\n');

        content.append(
                "Required permissions:\n"
        );

        for (CapabilityPermission permission :
                spec.getRequiredPermissions()) {

            content.append("- ")
                    .append(permission.name())
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Required tools:\n"
        );

        for (String tool :
                spec.getRequiredTools()) {

            content.append("- ")
                    .append(tool)
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Preferred tools:\n"
        );

        for (String tool :
                spec.getPreferredTools()) {

            content.append("- ")
                    .append(tool)
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Alternative tools:\n"
        );

        for (String tool :
                spec.getAlternativeTools()) {

            content.append("- ")
                    .append(tool)
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Required files:\n"
        );

        for (String path :
                spec.getRequiredFiles()) {

            content.append("- ")
                    .append(path)
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Allowed files:\n"
        );

        for (String path :
                spec.getAllowedFiles()) {

            content.append("- ")
                    .append(path)
                    .append('\n');
        }

        content.append('\n');

        content.append(
                "Success criteria:\n"
        );

        for (String criterion :
                spec.getSuccessCriteria()) {

            content.append("- ")
                    .append(criterion)
                    .append('\n');
        }

        writeFile(
                file,
                content.toString()
        );

        return file;
    }

    /**
     * Ensures that a path cannot escape the workspace.
     */
    private void ensureInsideWorkspace(
            File file
    ) throws IOException {

        File canonicalWorkspace =
                workspaceRoot.getCanonicalFile();

        File canonicalFile =
                file.getCanonicalFile();

        String workspacePath =
                canonicalWorkspace.getPath();

        String filePath =
                canonicalFile.getPath();

        if (!filePath.equals(workspacePath)
                && !filePath.startsWith(
                workspacePath + File.separator
        )) {

            throw new SecurityException(
                    "Path escapes JARVIS builder workspace."
            );
        }
    }

    /**
     * Blocks protected filesystem areas.
     */
    private void ensureNotProtected(
            File file
    ) {

        String path =
                file.getAbsolutePath();

        String normalizedPath =
                path.replace('\\', '/')
                        .toLowerCase();

        for (String protectedName :
                protectedPathNames) {

            if (normalizedPath.contains(
                    "/" + protectedName + "/"
            )
                    || normalizedPath.endsWith(
                    "/" + protectedName
            )) {

                throw new SecurityException(
                        "Attempt to modify protected path: "
                                + protectedName
                );
            }
        }
    }

    /**
     * Prevents capability IDs from targeting protected areas.
     */
    private boolean containsProtectedPath(
            String value
    ) {

        if (value == null) {
            return false;
        }

        String normalized =
                value.replace('\\', '/')
                        .toLowerCase();

        return normalized.contains(
                "ownersecurityboundary"
        )
                || normalized.contains(
                "owner_security"
        )
                || normalized.contains(
                "securityboundary"
        )
                || normalized.contains(
                "authorization"
        )
                || normalized.contains(
                "owneridentity"
        )
                || normalized.contains(
                "../"
        )
                || normalized.contains(
                "..\\"
        );
    }

    private void initializeProtectedPathNames() {

        protectedPathNames.add(
                "security"
        );

        protectedPathNames.add(
                "owner"
        );

        protectedPathNames.add(
                "authorization"
        );
    }

    /**
     * Writes UTF-8 content safely.
     */
    private void writeFile(
            File file,
            String content
    ) throws IOException {

        ensureInsideWorkspace(file);
        ensureNotProtected(file);

        File parent =
                file.getParentFile();

        if (parent != null
                && !parent.exists()
                && !parent.mkdirs()
                && !parent.exists()) {

            throw new IOException(
                    "Cannot create parent directory."
            );
        }

        try (FileOutputStream output =
                     new FileOutputStream(
                             file,
                             false
                     )) {

            output.write(
                    content.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            output.flush();
        }
    }

    private String sanitizePathPart(
            String value
    ) {

        String safe =
                value.trim()
                        .replace('\\', '_')
                        .replace('/', '_')
                        .replace(':', '_')
                        .replace('*', '_')
                        .replace('?', '_')
                        .replace('"', '_')
                        .replace('<', '_')
                        .replace('>', '_')
                        .replace('|', '_')
                        .replace("..", "_");

        if (safe.isEmpty()) {
            return "unnamed_capability";
        }

        return safe;
    }

    private String nullToEmpty(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }

    private String safeMessage(
            Exception exception
    ) {

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return exception.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private JarvisResult<Boolean> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "SelfBuilder"
                )
        );
    }

    private <T> JarvisResult<T> failureResult(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "SelfBuilder"
                )
        );
    }

    public boolean isInitialized() {
        return initialized;
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
    }

    public static String getBuilderId() {
        return BUILDER_ID;
    }

    /**
     * Complete result of a capability workspace build.
     */
    public static final class BuildResult {

        private final String capabilityId;
        private final File capabilityDirectory;
        private final List<File> createdFiles;
        private final boolean requiresExternalBuild;
        private final boolean requiresTests;

        private BuildResult(
                String capabilityId,
                File capabilityDirectory,
                List<File> createdFiles,
                boolean requiresExternalBuild,
                boolean requiresTests
        ) {

            this.capabilityId =
                    capabilityId;

            this.capabilityDirectory =
                    capabilityDirectory;

            this.createdFiles =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    createdFiles
                            )
                    );

            this.requiresExternalBuild =
                    requiresExternalBuild;

            this.requiresTests =
                    requiresTests;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public File getCapabilityDirectory() {
            return capabilityDirectory;
        }

        public List<File> getCreatedFiles() {
            return createdFiles;
        }

        public boolean requiresExternalBuild() {
            return requiresExternalBuild;
        }

        public boolean requiresTests() {
            return requiresTests;
        }

        public int getCreatedFileCount() {
            return createdFiles.size();
        }

        public boolean isReadyForTesting() {
            return !createdFiles.isEmpty();
        }

        @Override
        public String toString() {
            return "BuildResult{" +
                    "capabilityId='" +
                    capabilityId +
                    '\'' +
                    ", capabilityDirectory=" +
                    capabilityDirectory +
                    ", createdFiles=" +
                    createdFiles.size() +
                    ", requiresExternalBuild=" +
                    requiresExternalBuild +
                    ", requiresTests=" +
                    requiresTests +
                    '}';
        }
    }
}