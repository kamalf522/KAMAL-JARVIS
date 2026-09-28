package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
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
 * مسؤول عن:
 * 1. التحقق من CapabilitySpec.
 * 2. التحقق من حدود Owner/Security.
 * 3. إنشاء Workspace آمن للبناء.
 * 4. إنشاء ملفات capability المطلوبة.
 * 5. منع تعديل الملفات المحمية.
 * 6. إخراج BuildResult يمكن استعماله من Evolution Orchestrator.
 *
 * ملاحظة:
 * هذا النظام لا يحاول تجاوز Android Security ولا يعدل APK المثبت
 * مباشرة. البناء النهائي للتطبيق يحتاج Build Environment خارجي
 * أو Workspace مخصص له صلاحية الكتابة.
 */
public final class SelfBuilder {

    private static final String BUILDER_ID = "v2.self_builder";

    private final OwnerSecurityBoundary securityBoundary;
    private final File workspaceRoot;

    private final Set<String> protectedPaths =
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

        initializeProtectedPaths();
    }

    /**
     * يجهز Builder.
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
                if (!workspaceRoot.mkdirs()) {
                    return JarvisResult.failure(
                            JarvisError.of(
                                    JarvisError.Type.FILE_OPERATION_FAILED,
                                    "Cannot create builder workspace."
                            )
                    );
                }
            }

            if (!workspaceRoot.isDirectory()) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.FILE_OPERATION_FAILED,
                                "Builder workspace is not a directory."
                        )
                );
            }

            initialized = true;

            return JarvisResult.success(
                    true,
                    "SelfBuilder initialized successfully."
            );

        } catch (Exception e) {
            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            e
                    )
            );
        }
    }

    /**
     * يبني capability من CapabilitySpec.
     */
    public JarvisResult<BuildResult> build(
            CapabilitySpec spec
    ) {
        if (spec == null) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "CapabilitySpec cannot be null."
                    )
            );
        }

        if (!initialized) {
            JarvisResult<Boolean> initResult = initialize();

            if (!initResult.isSuccess()) {
                return JarvisResult.failure(
                        initResult.getError()
                );
            }
        }

        JarvisResult<Boolean> validation = validateSpec(spec);

        if (!validation.isSuccess()) {
            return JarvisResult.failure(
                    validation.getError()
            );
        }

        if (spec.isOwnerAuthorizationRequired()
                && !securityBoundary.isActive()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner security boundary is not active."
                    )
            );
        }

        if (spec.canModifyProjectFiles()) {
            JarvisResult<Boolean> securityCheck =
                    securityBoundary.validateEvolutionChange(
                            OwnerSecurityBoundary.ProtectedArea.EVOLUTION_SECURITY
                    );

            if (!securityCheck.isSuccess()) {
                return JarvisResult.failure(
                        securityCheck.getError()
                );
            }
        }

        try {
            File capabilityDirectory =
                    createCapabilityDirectory(spec.getCapabilityId());

            if (capabilityDirectory == null) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                "Cannot create capability workspace."
                        )
                );
            }

            List<File> createdFiles = new ArrayList<>();

            File manifestFile =
                    createCapabilityManifest(
                            capabilityDirectory,
                            spec
                    );

            if (manifestFile == null) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                "Cannot create capability manifest."
                        )
                );
            }

            createdFiles.add(manifestFile);

            File specificationFile =
                    createSpecificationFile(
                            capabilityDirectory,
                            spec
                    );

            if (specificationFile == null) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                "Cannot create capability specification."
                        )
                );
            }

            createdFiles.add(specificationFile);

            BuildResult result = new BuildResult(
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
                            e.getMessage()
                    )
            );

        } catch (Exception e) {
            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            e
                    )
            );
        }
    }

    /**
     * يتحقق من Specification قبل أي كتابة.
     */
    private JarvisResult<Boolean> validateSpec(
            CapabilitySpec spec
    ) {
        if (isBlank(spec.getCapabilityId())) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Capability ID is required."
                    )
            );
        }

        if (isBlank(spec.getName())) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Capability name is required."
                    )
            );
        }

        if (isBlank(spec.getGoal())) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Capability goal is required."
                    )
            );
        }

        if (spec.getSuccessCriteria() == null
                || spec.getSuccessCriteria().isEmpty()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "At least one success criterion is required."
                    )
            );
        }

        if (containsProtectedPath(spec.getCapabilityId())) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Capability targets a protected path."
                    )
            );
        }

        return JarvisResult.success(
                true,
                "Capability specification is valid."
        );
    }

    /**
     * ينشئ مجلد capability.
     */
    private File createCapabilityDirectory(
            String capabilityId
    ) throws IOException {

        String safeId = sanitizePathPart(capabilityId);

        File directory =
                new File(
                        workspaceRoot,
                        "capabilities" + File.separator + safeId
                ).getCanonicalFile();

        ensureInsideWorkspace(directory);

        if (!directory.exists()
                && !directory.mkdirs()) {

            throw new IOException(
                    "Cannot create capability directory: "
                            + directory.getAbsolutePath()
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
     * ينشئ manifest داخلي للـcapability.
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

        StringBuilder content = new StringBuilder();

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

        writeFile(file, content.toString());

        return file;
    }

    /**
     * ينشئ ملف specification قابل للقراءة.
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

        StringBuilder content = new StringBuilder();

        content.append("JARVIS V2 CAPABILITY SPECIFICATION\n");
        content.append("----------------------------------\n");

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

        content.append("Required permissions:\n");

        for (String permission : spec.getRequiredPermissions()) {
            content.append("- ")
                    .append(permission)
                    .append('\n');
        }

        content.append('\n');

        content.append("Preferred tools:\n");

        for (String tool : spec.getPreferredToolIds()) {
            content.append("- ")
                    .append(tool)
                    .append('\n');
        }

        content.append('\n');

        content.append("Alternative tools:\n");

        for (String tool : spec.getAlternativeToolIds()) {
            content.append("- ")
                    .append(tool)
                    .append('\n');
        }

        content.append('\n');

        content.append("Required files:\n");

        for (String path : spec.getRequiredFiles()) {
            content.append("- ")
                    .append(path)
                    .append('\n');
        }

        content.append('\n');

        content.append("Allowed files:\n");

        for (String path : spec.getAllowedFiles()) {
            content.append("- ")
                    .append(path)
                    .append('\n');
        }

        content.append('\n');

        content.append("Success criteria:\n");

        for (String criterion : spec.getSuccessCriteria()) {
            content.append("- ")
                    .append(criterion)
                    .append('\n');
        }

        writeFile(file, content.toString());

        return file;
    }

    /**
     * يمنع الوصول خارج Workspace.
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
                workspacePath + File.separator)) {

            throw new SecurityException(
                    "Path escapes JARVIS builder workspace."
            );
        }
    }

    /**
     * يمنع الكتابة إلى المناطق المحمية.
     */
    private void ensureNotProtected(
            File file
    ) {

        String path =
                file.getAbsolutePath();

        for (String protectedPath : protectedPaths) {

            if (path.equals(protectedPath)
                    || path.startsWith(
                    protectedPath + File.separator)) {

                throw new SecurityException(
                        "Attempt to modify protected path."
                );
            }
        }
    }

    private boolean containsProtectedPath(
            String value
    ) {

        if (value == null) {
            return false;
        }

        String normalized =
                value.replace('\\', '/')
                        .toLowerCase();

        return normalized.contains("ownersecurityboundary")
                || normalized.contains("owner_security")
                || normalized.contains("securityboundary")
                || normalized.contains("authorization")
                || normalized.contains("owneridentity");
    }

    private void initializeProtectedPaths() {

        protectedPaths.add(
                new File(
                        workspaceRoot,
                        "security"
                ).getAbsolutePath()
        );

        protectedPaths.add(
                new File(
                        workspaceRoot,
                        "owner"
                ).getAbsolutePath()
        );

        protectedPaths.add(
                new File(
                        workspaceRoot,
                        "authorization"
                ).getAbsolutePath()
        );
    }

    private void writeFile(
            File file,
            String content
    ) throws IOException {

        ensureNotProtected(file);

        File parent =
                file.getParentFile();

        if (parent != null && !parent.exists()) {

            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException(
                        "Cannot create parent directory."
                );
            }
        }

        try (FileOutputStream output =
                     new FileOutputStream(file, false)) {

            output.write(
                    content.getBytes(StandardCharsets.UTF_8)
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
                        .replace('|', '_');

        if (safe.isEmpty()) {
            return "unnamed_capability";
        }

        return safe;
    }

    private String nullToEmpty(
            String value
    ) {
        return value == null ? "" : value;
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
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
     * نتيجة عملية البناء.
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
            this.capabilityId = capabilityId;
            this.capabilityDirectory =
                    capabilityDirectory;

            this.createdFiles =
                    Collections.unmodifiableList(
                            new ArrayList<>(createdFiles)
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
                    capabilityId + '\'' +
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