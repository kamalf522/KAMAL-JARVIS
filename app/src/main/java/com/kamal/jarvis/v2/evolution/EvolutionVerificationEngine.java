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
 *
 * يوجد هنا نوعان من المساحات:
 *
 * 1. artifactWorkspaceRoot
 *    مساحة JARVIS الداخلية التي ينشئ فيها SelfBuilder
 *    ملفات capability الخاصة به.
 *
 * 2. ProjectWorkspaceManager
 *    المشروع Android الحقيقي الذي يتم عليه:
 *      - Source Evolution
 *      - Gradle Build
 *      - APK verification
 *
 * هذا الفصل مهم حتى لا نخلط بين:
 *
 * "JARVIS أنشأ ملفات capability"
 *
 * و:
 *
 * "مشروع Android الحقيقي تم بناؤه بنجاح."
 *
 * لا يتم إعلان النجاح النهائي بدون الأدلة المطلوبة.
 */
public final class EvolutionVerificationEngine {

    private static final String ENGINE_ID =
            "v2.evolution_verification_engine";

    /*
     * Workspace الداخلي الخاص بـSelfBuilder.
     */
    private final File artifactWorkspaceRoot;

    /*
     * المشروع Android الحقيقي.
     *
     * يمكن أن يكون null في constructor القديم
     * من أجل المحافظة على compatibility.
     */
    private final ProjectWorkspaceManager projectWorkspaceManager;

    private final OwnerSecurityBoundary securityBoundary;

    private volatile VerificationReport lastReport;

    /**
     * Constructor قديم.
     *
     * يبقى موجوداً حتى لا نكسر الملفات القديمة.
     */
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

    /**
     * Constructor الجديد.
     *
     * artifactWorkspaceRoot:
     * workspace الداخلي الذي يستعمله SelfBuilder.
     *
     * projectWorkspaceManager:
     * المشروع Android الحقيقي.
     */
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

    /**
     * التحقق من artifacts بعد SelfBuilder.
     *
     * هذه المرحلة لا تدعي أن Android project تم بناؤه.
     */
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

        /*
         * 1. Artifact workspace.
         */
        checkArtifactWorkspace(
                checks
        );

        /*
         * 2. Capability directory.
         */
        checkCapabilityDirectory(
                checks,
                buildResult
        );

        /*
         * 3. Files created by SelfBuilder.
         */
        checkCreatedFiles(
                checks,
                buildResult
        );

        /*
         * 4. Contents.
         */
        checkFileContents(
                checks,
                buildResult
        );

        /*
         * 5. If build is required,
         *    verify the REAL Android project structure.
         */
        if (spec.requiresBuild()) {

            checkRealProjectStructure(
                    checks
            );
        }

        /*
         * Success criteria هنا يتم التحقق من تعريفها،
         * وليس الادعاء أن الوظيفة نفسها اشتغلت.
         */
        checkSuccessCriteria(
                checks,
                spec
        );

        /*
         * Build evidence غير موجود في هذه المرحلة.
         *
         * هذا Warning وليس Success نهائي.
         */
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

    /**
     * التحقق النهائي بعد BuildEngine.
     *
     * هنا يمكن اعتبار Build دليلاً فقط إذا:
     *
     * - BuildRecord success
     * - exit code = 0
     * - APK موجود
     * - APK حجمه > 0
     */
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

        /*
         * Artifact verification.
         */
        checkArtifactWorkspace(
                checks
        );

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

        /*
         * Real Android project.
         */
        if (spec.requiresBuild()) {

            checkRealProjectStructure(
                    checks
            );

            checkBuildResult(
                    checks,
                    buildRecord
            );
        }

        /*
         * Success criteria.
         */
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

    /**
     * إنشاء VerificationReport.
     */
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

    /**
     * فحص workspace الداخلي.
     */
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

    /**
     * فحص capability directory.
     */
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

    /**
     * فحص جميع الملفات التي أنشأها SelfBuilder.
     */
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

        int valid =
                0;

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

    /**
     * فحص محتوى الملفات.
     */
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

                String content =
                        Files.readString(
                                file.toPath(),
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

    /**
     * الحصول على المشروع Android الحقيقي.
     *
     * إذا كان ProjectWorkspaceManager موجوداً:
     * نستعمله فقط.
     *
     * لا يوجد fallback صامت إلى jarvis_workspace.
     */
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

    /**
     * فحص بنية Android/Gradle الحقيقية.
     */
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

        /*
         * settings.gradle / settings.gradle.kts
         */
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

        /*
         * Root build.gradle.
         */
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

        /*
         * Android app module.
         */
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

        /*
         * app/build.gradle
         */
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

        /*
         * Gradle Wrapper.
         */
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

    /**
     * التحقق من BuildRecord الحقيقي.
     */
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

        /*
         * APK خاصو يكون داخل المشروع الحقيقي
         * أو على الأقل BuildEngine هو الذي أنتجه.
         *
         * لا نرفضه فقط لأنه قد يكون copy destination
         * خارج المشروع.
         */
    }

    /**
     * Success Criteria.
     *
     * ملاحظة مهمة:
     *
     * وجود criterion لا يعني أن الوظيفة اشتغلت.
     *
     * لذلك هذا الفحص يتحقق من أن الشروط:
     * - موجودة
     * - غير فارغة
     *
     * أما إثبات التنفيذ الحقيقي فيحتاج Runtime evidence.
     */
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

        int valid =
                0;

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

    /**
     * هل الملف داخل artifact workspace؟
     */
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

    /**
     * آخر تقرير.
     */
    public VerificationReport getLastReport() {
        return lastReport;
    }

    /**
     * Workspace الداخلي.
     */
    public File getWorkspaceRoot() {
        return artifactWorkspaceRoot;
    }

    /**
     * Workspace المشروع الحقيقي.
     */
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

    /**
     * نتيجة فحص واحدة.
     */
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

    /**
     * التقرير الكامل.
     */
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

            int passed =
                    0;

            int failed =
                    0;

            int warning =
                    0;

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

        /**
         * Warning لا يفشل التقرير.
         *
         * Failed فقط هو الذي يجعل التقرير failed.
         */
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