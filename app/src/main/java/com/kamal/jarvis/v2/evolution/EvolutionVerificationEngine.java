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
 * مسؤول على التحقق من نتيجة التطور.
 *
 * الفرق بين:
 *
 * "JARVIS كتب الكود"
 *
 * و:
 *
 * "JARVIS عندو دليل أن التغيير صالح."
 *
 * التحقق يتم على عدة مستويات:
 *
 * 1. Workspace
 * 2. الملفات المطلوبة
 * 3. محتوى الملفات
 * 4. ملفات Gradle الأساسية
 * 5. نتيجة Build
 * 6. وجود APK إذا كان Build مطلوباً
 * 7. Success Criteria الخاصة بالقدرة
 *
 * هذا المحرك لا يعلن SUCCESS بدون أدلة.
 */
public final class EvolutionVerificationEngine {

    private static final String ENGINE_ID =
            "v2.evolution_verification_engine";

    private final File workspaceRoot;
    private final OwnerSecurityBoundary securityBoundary;

    private volatile VerificationReport
            lastReport;

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

        this.workspaceRoot =
                workspaceRoot.getAbsoluteFile();

        this.securityBoundary =
                securityBoundary;
    }

    /**
     * التحقق من Capability بعد SelfBuilder.
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
         * 1. Workspace
         */
        checkWorkspace(
                checks
        );

        /*
         * 2. Capability directory
         */
        checkCapabilityDirectory(
                checks,
                buildResult
        );

        /*
         * 3. Created files
         */
        checkCreatedFiles(
                checks,
                buildResult
        );

        /*
         * 4. File contents
         */
        checkFileContents(
                checks,
                buildResult
        );

        /*
         * 5. Gradle project structure
         *
         * هذا لا يعني أن Build نجح.
         * فقط نتأكد أن المشروع قابل للفحص.
         */
        if (spec.requiresBuild()) {

            checkGradleStructure(
                    checks
            );
        }

        /*
         * 6. Success criteria
         */
        checkSuccessCriteria(
                checks,
                spec
        );

        /*
         * BuildResult نفسه لا يعتبر Build.
         * لذلك إذا spec.requiresBuild() = true
         * وBuild لم يتم تمريره لهذه الدالة،
         * يبقى التحقق النهائي ناقصاً.
         */
        if (spec.requiresBuild()) {

            checks.add(
                    VerificationCheck.warning(
                            "BUILD_EVIDENCE",
                            "Build evidence must be supplied by BuildEngine."
                    )
            );
        }

        long duration =
                System.currentTimeMillis()
                        - startedAt;

        VerificationReport report =
                VerificationReport.from(
                        spec.getCapabilityId(),
                        checks,
                        duration
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
     * تحقق نهائي عندما يكون BuildEngine قد نفذ Build فعلياً.
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

        checkWorkspace(
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

        if (spec.requiresBuild()) {

            checkGradleStructure(
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

        long duration =
                System.currentTimeMillis()
                        - startedAt;

        VerificationReport report =
                VerificationReport.from(
                        spec.getCapabilityId(),
                        checks,
                        duration
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

    private void checkWorkspace(
            List<VerificationCheck> checks
    ) {

        if (!workspaceRoot.exists()) {

            checks.add(
                    VerificationCheck.failed(
                            "WORKSPACE",
                            "Workspace does not exist."
                    )
            );

            return;
        }

        if (!workspaceRoot.isDirectory()) {

            checks.add(
                    VerificationCheck.failed(
                            "WORKSPACE",
                            "Workspace is not a directory."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "WORKSPACE",
                        "Workspace exists."
                )
        );
    }

    private void checkCapabilityDirectory(
            List<VerificationCheck> checks,
            SelfBuilder.BuildResult buildResult
    ) {

        File directory =
                buildResult
                        .getCapabilityDirectory();

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

        if (!isInsideWorkspace(
                directory
        )) {

            checks.add(
                    VerificationCheck.failed(
                            "CAPABILITY_DIRECTORY",
                            "Capability directory is outside workspace."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "CAPABILITY_DIRECTORY",
                        "Capability directory is valid."
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
                            "No files were created."
                    )
            );

            return;
        }

        int valid =
                0;

        for (File file :
                files) {

            if (file == null) {

                continue;
            }

            if (file.exists() &&
                    file.isFile() &&
                    isInsideWorkspace(
                            file
                    )) {

                valid++;

            } else {

                checks.add(
                        VerificationCheck.failed(
                                "FILE:" + file,
                                "Created file is missing or unsafe."
                        )
                );
            }
        }

        if (valid == files.size()) {

            checks.add(
                    VerificationCheck.passed(
                            "CREATED_FILES",
                            "All created files exist."
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

        for (File file :
                files) {

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
                                    "File is empty."
                            )
                    );

                } else {

                    checks.add(
                            VerificationCheck.passed(
                                    "CONTENT:" + file.getName(),
                                    "File contains data."
                            )
                    );
                }

            } catch (IOException e) {

                checks.add(
                        VerificationCheck.failed(
                                "CONTENT:" + file.getName(),
                                "Could not read file."
                        )
                );
            }
        }
    }

    private void checkGradleStructure(
            List<VerificationCheck> checks
    ) {

        File settingsGradle =
                new File(
                        workspaceRoot,
                        "settings.gradle"
                );

        File settingsKts =
                new File(
                        workspaceRoot,
                        "settings.gradle.kts"
                );

        boolean hasSettings =
                settingsGradle.isFile()
                        || settingsKts.isFile();

        if (!hasSettings) {

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

        File app =
                new File(
                        workspaceRoot,
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

        File buildGradle =
                new File(
                        app,
                        "build.gradle"
                );

        File buildKts =
                new File(
                        app,
                        "build.gradle.kts"
                );

        if (!buildGradle.isFile() &&
                !buildKts.isFile()) {

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
    }

    private void checkBuildResult(
            List<VerificationCheck> checks,
            BuildEngine.BuildRecord buildRecord
    ) {

        if (!buildRecord.isSuccess()) {

            checks.add(
                    VerificationCheck.failed(
                            "BUILD",
                            "BuildEngine reported build failure."
                    )
            );

            return;
        }

        if (buildRecord.getExitCode() != 0) {

            checks.add(
                    VerificationCheck.failed(
                            "BUILD_EXIT_CODE",
                            "Build exit code is not zero."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "BUILD_EXIT_CODE",
                        "Gradle exited successfully."
                )
        );

        File apk =
                buildRecord.getApkFile();

        if (apk == null ||
                !apk.isFile() ||
                apk.length() <= 0) {

            checks.add(
                    VerificationCheck.failed(
                            "APK",
                            "No valid APK was produced."
                    )
            );

            return;
        }

        checks.add(
                VerificationCheck.passed(
                        "APK",
                        "A valid APK was produced."
                )
        );
    }

    /**
     * Success Criteria الحالية في CapabilitySpec
     * هي شروط نصية.
     *
     * المحرك لا يكذب ويعتبر مجرد وجود النص
     * دليلاً على أن الوظيفة اشتغلت.
     *
     * لذلك يتم تسجيلها كمعلومات تحتاج
     * Runtime/Tool evidence إذا كانت تتطلب تشغيل فعلي.
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
                            "No success criteria defined."
                    )
            );

            return;
        }

        for (String criterion :
                criteria) {

            if (criterion == null ||
                    criterion.trim().isEmpty()) {

                checks.add(
                        VerificationCheck.failed(
                                "SUCCESS_CRITERION",
                                "Empty success criterion."
                        )
                );

            } else {

                checks.add(
                        VerificationCheck.passed(
                                "SUCCESS_CRITERION",
                                "Defined: "
                                        + criterion
                        )
                );
            }
        }
    }

    private boolean isInsideWorkspace(
            File file
    ) {

        try {

            java.nio.file.Path root =
                    workspaceRoot
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

    public VerificationReport
    getLastReport() {

        return lastReport;
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
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
     * نتيجة فحص واحد.
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
        private final List<VerificationCheck> checks;
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

                if (check.isPassed()) {
                    passed++;
                } else if (check.isFailed()) {
                    failed++;
                } else {
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

            return new VerificationReport(
                    capabilityId,
                    checks,
                    durationMillis
            );
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public List<VerificationCheck> getChecks() {
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

                if (check.isFailed()) {

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
    }
}