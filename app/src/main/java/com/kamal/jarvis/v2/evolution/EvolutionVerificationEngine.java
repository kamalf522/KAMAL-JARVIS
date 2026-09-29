package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Evolution Verification Engine
 *
 * مسؤول عن التحقق الحقيقي من نتائج Evolution.
 */
public final class EvolutionVerificationEngine {

    private static final String ENGINE_ID =
            "v2.evolution_verification_engine";

    private final File artifactWorkspaceRoot;

    private final ProjectWorkspaceManager projectWorkspaceManager;

    private final OwnerSecurityBoundary securityBoundary;

    private volatile VerificationReport lastReport;

    public EvolutionVerificationEngine(
            File workspaceRoot,
            OwnerSecurityBoundary securityBoundary
    ) {

        if (workspaceRoot == null) {
            throw new IllegalArgumentException(
                    "workspaceRoot cannot be null."
            );
        }

        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null."
            );
        }

        this.artifactWorkspaceRoot =
                workspaceRoot.getAbsoluteFile();

        this.projectWorkspaceManager =
                null;

        this.securityBoundary =
                securityBoundary;
    }

    public EvolutionVerificationEngine(
            File artifactWorkspaceRoot,
            ProjectWorkspaceManager projectWorkspaceManager,
            OwnerSecurityBoundary securityBoundary
    ) {

        if (artifactWorkspaceRoot == null) {
            throw new IllegalArgumentException(
                    "artifactWorkspaceRoot cannot be null."
            );
        }

        if (projectWorkspaceManager == null) {
            throw new IllegalArgumentException(
                    "projectWorkspaceManager cannot be null."
            );
        }

        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null."
            );
        }

        this.artifactWorkspaceRoot =
                artifactWorkspaceRoot.getAbsoluteFile();

        this.projectWorkspaceManager =
                projectWorkspaceManager;

        this.securityBoundary =
                securityBoundary;
    }

    public synchronized JarvisResult<VerificationReport>
    verify(
            CapabilitySpec spec,
            SelfBuilder.BuildResult buildResult
    ) {

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (!spec.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability specification is invalid."
            );
        }

        if (buildResult == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "BuildResult cannot be null."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Security boundary is not active."
            );
        }

        List<VerificationCheck> checks =
                new ArrayList<>();

        long startedAt =
                System.currentTimeMillis();

        checkArtifactWorkspace(checks);

        checkCapabilityDirectory(
                checks,
                buildResult
        );

        checkCreatedFiles(
                checks,
                buildResult
        );

        checkFileContents(
                checks,
                buildResult
        );

        if (spec.requiresBuild()) {

            checkRealProjectStructure(
                    checks
            );
        }

        checkSuccessCriteria(
                checks,
                spec
        );

        if (spec.requiresBuild()) {

            checks.add(
                    VerificationCheck.warning(
                            "BUILD_EVIDENCE",
                            "Real build evidence must be supplied by BuildEngine."
                    )
            );
        }

        VerificationReport report =
                createReport(
                        spec.getCapabilityId(),
                        checks,
                        startedAt
                );

        lastReport =
                report;

        if (!report.isPassed()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TEST_FAILED,
                            report.getSummary(),
                            ENGINE_ID
                    ),
                    report.getSummary()
            );
        }

        return JarvisResult.success(
                report,
                report.getSummary()
        );
    }

    public synchronized JarvisResult<VerificationReport>
    verifyWithBuild(
            CapabilitySpec spec,
            SelfBuilder.BuildResult buildResult,
            BuildEngine.BuildRecord buildRecord
    ) {

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (!spec.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability specification is invalid."
            );
        }

        if (buildResult == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "BuildResult cannot be null."
            );
        }

        if (buildRecord == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "BuildRecord cannot be null."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Security boundary is not active."
            );
        }

        List<VerificationCheck> checks =
                new ArrayList<>();

        long startedAt =
                System.currentTimeMillis();

        checkArtifactWorkspace(checks);

        checkCapabilityDirectory(
                checks,
                buildResult
        );

        checkCreatedFiles(
                checks,
                buildResult
        );

        checkFileContents(
                checks,
                buildResult
        );

        if (spec.requiresBuild()) {

            checkRealProjectStructure(
                    checks
            );

            checkBuildResult(
                    checks,
                    buildRecord
            );
        }

        checkSuccessCriteria(
                checks,
                spec
        );

        VerificationReport report =
                createReport(
                        spec.getCapabilityId(),
                        checks,
                        startedAt
                );

        lastReport =
                report;

        if (!report.isPassed()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TEST_FAILED,
                            report.getSummary(),
                            ENGINE_ID
                    ),
                    report.getSummary()
            );
        }

        return JarvisResult.success(
                report,
                "Evolution passed final verification."
        );
    }

    private VerificationReport createReport(
            String capabilityId,
            List<VerificationCheck> checks,
            long startedAt
    ) {

        long duration =
                System.currentTimeMillis()
                        - startedAt;

        return VerificationReport.from(
                capabilityId,
                checks,
                duration
        );
    }

    private void checkArtifactWorkspace(
            List<VerificationCheck> checks
    ) {

        if (!artifactWorkspaceRoot.exists()) {

            checks.add(
                    VerificationCheck.failed(
                            "ARTIFACT_WORKSPACE",
                            "JARVIS artifact workspace does not exist."
                    )
            );

            return;
        }

        if (!artifactWorkspaceRoot.isDirectory()) {

            checks.add(
                    VerificationCheck.failed(
                            "ARTIFACT_WORKSPACE",
                            "JARVIS artifact workspace is not a directory."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "ARTIFACT_WORKSPACE",
                        "JARVIS artifact workspace exists."
                )
        );
    }

    private void checkCapabilityDirectory(
            List<VerificationCheck> checks,
            SelfBuilder.BuildResult buildResult
    ) {

        File directory =
                buildResult.getCapabilityDirectory();

        if (directory == null) {

            checks.add(
                    VerificationCheck.failed(
                            "CAPABILITY_DIRECTORY",
                            "Capability directory is null."
                    )
            );

            return;
        }

        if (!directory.exists() ||
                !directory.isDirectory()) {

            checks.add(
                    VerificationCheck.failed(
                            "CAPABILITY_DIRECTORY",
                            "Capability directory does not exist."
                    )
            );

            return;
        }

        if (!isInsideArtifactWorkspace(directory)) {

            checks.add(
                    VerificationCheck.failed(
                            "CAPABILITY_DIRECTORY",
                            "Capability directory is outside JARVIS artifact workspace."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "CAPABILITY_DIRECTORY",
                        "Capability directory is inside the JARVIS artifact workspace."
                )
        );
    }

    private void checkCreatedFiles(
            List<VerificationCheck> checks,
            SelfBuilder.BuildResult buildResult
    ) {

        List<File> files =
                buildResult.getCreatedFiles();

        if (files == null ||
                files.isEmpty()) {

            checks.add(
                    VerificationCheck.failed(
                            "CREATED_FILES",
                            "SelfBuilder did not create any files."
                    )
            );

            return;
        }

        int valid = 0;

        for (File file : files) {

            if (file == null) {
                continue;
            }

            if (file.exists() &&
                    file.isFile() &&
                    isInsideArtifactWorkspace(file)) {

                valid++;

            } else {

                checks.add(
                        VerificationCheck.failed(
                                "FILE:" + file,
                                "Created file is missing or outside the artifact workspace."
                        )
                );
            }
        }

        if (valid == files.size()) {

            checks.add(
                    VerificationCheck.passed(
                            "CREATED_FILES",
                            "All created files exist and are inside the artifact workspace."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.failed(
                            "CREATED_FILES",
                            valid
                                    + "/"
                                    + files.size()
                                    + " created files are valid."
                    )
            );
        }
    }

    private void checkFileContents(
            List<VerificationCheck> checks,
            SelfBuilder.BuildResult buildResult
    ) {

        List<File> files =
                buildResult.getCreatedFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file == null ||
                    !file.isFile()) {

                continue;
            }

            try {

                /*
                 * Android-compatible replacement for
                 * Files.readString(...).
                 */
                String content =
                        new String(
                                Files.readAllBytes(
                                        file.toPath()
                                ),
                                StandardCharsets.UTF_8
                        );

                if (content.trim().isEmpty()) {

                    checks.add(
                            VerificationCheck.failed(
                                    "CONTENT:" + file.getName(),
                                    "Created file is empty."
                            )
                    );

                } else {

                    checks.add(
                            VerificationCheck.passed(
                                    "CONTENT:" + file.getName(),
                                    "Created file contains data."
                            )
                    );
                }

            } catch (IOException e) {

                checks.add(
                        VerificationCheck.failed(
                                "CONTENT:" + file.getName(),
                                "Could not read created file."
                        )
                );
            }
        }
    }

    private File resolveProjectWorkspace() {

        if (projectWorkspaceManager == null) {
            return null;
        }

        if (!projectWorkspaceManager.isReady()) {
            return null;
        }

        File root =
                projectWorkspaceManager.getProjectRoot();

        if (root == null) {
            return null;
        }

        return root.getAbsoluteFile();
    }

    private void checkRealProjectStructure(
            List<VerificationCheck> checks
    ) {

        File projectRoot =
                resolveProjectWorkspace();

        if (projectRoot == null) {

            checks.add(
                    VerificationCheck.failed(
                            "REAL_PROJECT",
                            "Real Android project workspace is not configured."
                    )
            );

            return;
        }

        if (!projectRoot.exists() ||
                !projectRoot.isDirectory()) {

            checks.add(
                    VerificationCheck.failed(
                            "REAL_PROJECT",
                            "Real Android project root does not exist."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "REAL_PROJECT",
                        "Real Android project root is available."
                )
        );

        File settingsGradle =
                new File(
                        projectRoot,
                        "settings.gradle"
                );

        File settingsKts =
                new File(
                        projectRoot,
                        "settings.gradle.kts"
                );

        if (!settingsGradle.isFile() &&
                !settingsKts.isFile()) {

            checks.add(
                    VerificationCheck.failed(
                            "GRADLE_SETTINGS",
                            "Gradle settings file is missing."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.passed(
                            "GRADLE_SETTINGS",
                            "Gradle settings file exists."
                    )
            );
        }

        File rootBuildGradle =
                new File(
                        projectRoot,
                        "build.gradle"
                );

        File rootBuildKts =
                new File(
                        projectRoot,
                        "build.gradle.kts"
                );

        if (!rootBuildGradle.isFile() &&
                !rootBuildKts.isFile()) {

            checks.add(
                    VerificationCheck.failed(
                            "ROOT_BUILD_FILE",
                            "Root Gradle build file is missing."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.passed(
                            "ROOT_BUILD_FILE",
                            "Root Gradle build file exists."
                    )
            );
        }

        File app =
                new File(
                        projectRoot,
                        "app"
                );

        if (!app.isDirectory()) {

            checks.add(
                    VerificationCheck.failed(
                            "ANDROID_MODULE",
                            "Android app module is missing."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "ANDROID_MODULE",
                        "Android app module exists."
                )
        );

        File appBuildGradle =
                new File(
                        app,
                        "build.gradle"
                );

        File appBuildKts =
                new File(
                        app,
                        "build.gradle.kts"
                );

        if (!appBuildGradle.isFile() &&
                !appBuildKts.isFile()) {

            checks.add(
                    VerificationCheck.failed(
                            "APP_BUILD_FILE",
                            "App Gradle build file is missing."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.passed(
                            "APP_BUILD_FILE",
                            "App Gradle build file exists."
                    )
            );
        }

        File gradlew =
                new File(
                        projectRoot,
                        "gradlew"
                );

        File gradlewBat =
                new File(
                        projectRoot,
                        "gradlew.bat"
                );

        if (!gradlew.isFile() &&
                !gradlewBat.isFile()) {

            checks.add(
                    VerificationCheck.failed(
                            "GRADLE_WRAPPER",
                            "Gradle Wrapper is missing."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.passed(
                            "GRADLE_WRAPPER",
                            "Gradle Wrapper exists."
                    )
            );
        }
    }

    private void checkBuildResult(
            List<VerificationCheck> checks,
            BuildEngine.BuildRecord buildRecord
    ) {

        if (!buildRecord.isSuccess()) {

            checks.add(
                    VerificationCheck.failed(
                            "BUILD",
                            "BuildEngine reported that the build failed."
                    )
            );

            return;
        }

        if (buildRecord.getExitCode() != 0) {

            checks.add(
                    VerificationCheck.failed(
                            "BUILD_EXIT_CODE",
                            "Gradle exit code is not zero."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "BUILD_EXIT_CODE",
                        "Gradle exited with code 0."
                )
        );

        File apk =
                buildRecord.getApkFile();

        if (apk == null) {

            checks.add(
                    VerificationCheck.failed(
                            "APK",
                            "BuildRecord contains no APK file."
                    )
            );

            return;
        }

        if (!apk.isFile()) {

            checks.add(
                    VerificationCheck.failed(
                            "APK",
                            "APK file does not exist."
                    )
            );

            return;
        }

        if (apk.length() <= 0) {

            checks.add(
                    VerificationCheck.failed(
                            "APK",
                            "APK file exists but is empty."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "APK",
                        "A non-empty APK was produced by the build."
                )
        );
    }

    private void checkSuccessCriteria(
            List<VerificationCheck> checks,
            CapabilitySpec spec
    ) {

        List<String> criteria =
                spec.getSuccessCriteria();

        if (criteria == null ||
                criteria.isEmpty()) {

            checks.add(
                    VerificationCheck.failed(
                            "SUCCESS_CRITERIA",
                            "No success criteria are defined."
                    )
            );

            return;
        }

        int valid = 0;

        for (String criterion : criteria) {

            if (criterion == null ||
                    criterion.trim().isEmpty()) {

                checks.add(
                        VerificationCheck.failed(
                                "SUCCESS_CRITERION",
                                "An empty success criterion was found."
                        )
                );

            } else {

                valid++;

                checks.add(
                        VerificationCheck.passed(
                                "SUCCESS_CRITERION",
                                "Defined criterion: "
                                        + criterion
                        )
                );
            }
        }

        if (valid != criteria.size()) {

            checks.add(
                    VerificationCheck.failed(
                            "SUCCESS_CRITERIA_VALIDITY",
                            valid
                                    + "/"
                                    + criteria.size()
                                    + " success criteria are valid."
                    )
            );
        }
    }

    private boolean isInsideArtifactWorkspace(
            File file
    ) {

        if (file == null) {
            return false;
        }

        try {

            java.nio.file.Path root =
                    artifactWorkspaceRoot
                            .getCanonicalFile()
                            .toPath();

            java.nio.file.Path target =
                    file.getCanonicalFile()
                            .toPath();

            return target.startsWith(
                    root
            );

        } catch (IOException e) {

            return false;
        }
    }

    public VerificationReport getLastReport() {
        return lastReport;
    }

    public File getWorkspaceRoot() {
        return artifactWorkspaceRoot;
    }

    public File getProjectWorkspaceRoot() {
        return resolveProjectWorkspace();
    }

    public ProjectWorkspaceManager
    getProjectWorkspaceManager() {

        return projectWorkspaceManager;
    }

    public OwnerSecurityBoundary
    getSecurityBoundary() {

        return securityBoundary;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    private <T>
    JarvisResult<T> failure(
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

    public static final class VerificationCheck {

        public enum Status {
            PASSED,
            FAILED,
            WARNING
        }

        private final String id;
        private final Status status;
        private final String message;

        private VerificationCheck(
                String id,
                Status status,
                String message
        ) {

            this.id =
                    id;

            this.status =
                    status;

            this.message =
                    message;
        }

        public static VerificationCheck passed(
                String id,
                String message
        ) {

            return new VerificationCheck(
                    id,
                    Status.PASSED,
                    message
            );
        }

        public static VerificationCheck failed(
                String id,
                String message
        ) {

            return new VerificationCheck(
                    id,
                    Status.FAILED,
                    message
            );
        }

        public static VerificationCheck warning(
                String id,
                String message
        ) {

            return new VerificationCheck(
                    id,
                    Status.WARNING,
                    message
            );
        }

        public String getId() {
            return id;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public boolean isPassed() {
            return status == Status.PASSED;
        }

        public boolean isFailed() {
            return status == Status.FAILED;
        }

        public boolean isWarning() {
            return status == Status.WARNING;
        }
    }

    public static final class VerificationReport {

        private final String capabilityId;

        private final List<VerificationCheck>
                checks;

        private final long durationMillis;

        private final int passedCount;

        private final int failedCount;

        private final int warningCount;

        private VerificationReport(
                String capabilityId,
                List<VerificationCheck> checks,
                long durationMillis
        ) {

            this.capabilityId =
                    capabilityId;

            this.checks =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    checks
                            )
                    );

            this.durationMillis =
                    durationMillis;

            int passed = 0;
            int failed = 0;
            int warning = 0;

            for (VerificationCheck check :
                    checks) {

                if (check == null) {
                    continue;
                }

                if (check.isPassed()) {

                    passed++;

                } else if (check.isFailed()) {

                    failed++;

                } else if (check.isWarning()) {

                    warning++;
                }
            }

            this.passedCount =
                    passed;

            this.failedCount =
                    failed;

            this.warningCount =
                    warning;
        }

        public static VerificationReport from(
                String capabilityId,
                List<VerificationCheck> checks,
                long durationMillis
        ) {

            if (checks == null) {

                checks =
                        Collections.emptyList();
            }

            return new VerificationReport(
                    capabilityId,
                    checks,
                    durationMillis
            );
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public List<VerificationCheck>
        getChecks() {

            return checks;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public int getPassedCount() {
            return passedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public boolean isPassed() {
            return failedCount == 0;
        }

        public boolean isFailed() {
            return failedCount > 0;
        }

        public String getSummary() {

            return "Verification{" +
                    "capability=" +
                    capabilityId +
                    ", passed=" +
                    passedCount +
                    ", failed=" +
                    failedCount +
                    ", warnings=" +
                    warningCount +
                    ", durationMs=" +
                    durationMillis +
                    '}';
        }

        public List<String>
        getFailures() {

            List<String> failures =
                    new ArrayList<>();

            for (VerificationCheck check :
                    checks) {

                if (check != null &&
                        check.isFailed()) {

                    failures.add(
                            check.getId()
                                    + ": "
                                    + check.getMessage()
                    );
                }
            }

            return Collections.unmodifiableList(
                    failures
            );
        }

        public List<String>
        getWarnings() {

            List<String> warnings =
                    new ArrayList<>();

            for (VerificationCheck check :
                    checks) {

                if (check != null &&
                        check.isWarning()) {

                    warnings.add(
                            check.getId()
                                    + ": "
                                    + check.getMessage()
                    );
                }
            }

            return Collections.unmodifiableList(
                    warnings
            );
        }
    }
}