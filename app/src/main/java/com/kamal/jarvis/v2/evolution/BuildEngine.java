package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JARVIS V2 - Real Build Engine
 *
 * مسؤول عن تنفيذ Build حقيقي للمشروع من داخل Workspace.
 *
 * Build flow:
 *
 * Project
 *   -> Gradle Wrapper
 *   -> Gradle
 *   -> Android Gradle Plugin
 *   -> APK
 *
 * هذا المحرك لا يعتبر العملية ناجحة لمجرد أن
 * Gradle process توقف بدون exception.
 *
 * النجاح الحقيقي يتطلب:
 * 1. Process exit code = 0
 * 2. وجود APK الناتج
 *
 * ملاحظة تقنية:
 * هذا المحرك يحتاج إلى بيئة Build حقيقية:
 * - gradlew أو gradlew.bat
 * - Gradle distribution التي يستعملها wrapper
 * - JDK
 * - Android SDK
 * - Build tools
 *
 * إذا لم تكن البيئة موجودة، يرجع سبب الفشل الحقيقي.
 */
public final class BuildEngine {

    private static final String ENGINE_ID =
            "v2.build_engine";

    private static final long DEFAULT_TIMEOUT_SECONDS =
            600L;

    private static final String GRADLE_WRAPPER =
            "gradlew";

    private static final String GRADLE_WRAPPER_WINDOWS =
            "gradlew.bat";

    private final File workspaceRoot;
    private final OwnerSecurityBoundary securityBoundary;

    private volatile boolean building;
    private volatile BuildRecord lastBuild;

    public BuildEngine(
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
     * يتحقق من أن Workspace صالح للبناء.
     */
    public synchronized JarvisResult<BuildEnvironment>
    inspectEnvironment() {

        try {

            if (!workspaceRoot.exists()) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "Build workspace does not exist."
                );
            }

            if (!workspaceRoot.isDirectory()) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "Build workspace is not a directory."
                );
            }

            File unixWrapper =
                    new File(
                            workspaceRoot,
                            GRADLE_WRAPPER
                    );

            File windowsWrapper =
                    new File(
                            workspaceRoot,
                            GRADLE_WRAPPER_WINDOWS
                    );

            boolean hasUnixWrapper =
                    unixWrapper.isFile();

            boolean hasWindowsWrapper =
                    windowsWrapper.isFile();

            if (!hasUnixWrapper &&
                    !hasWindowsWrapper) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "No Gradle Wrapper found in project root."
                );
            }

            File gradleProject =
                    new File(
                            workspaceRoot,
                            "settings.gradle"
                    );

            File gradleProjectKts =
                    new File(
                            workspaceRoot,
                            "settings.gradle.kts"
                    );

            if (!gradleProject.isFile() &&
                    !gradleProjectKts.isFile()) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "No Gradle settings file found."
                );
            }

            File appDirectory =
                    new File(
                            workspaceRoot,
                            "app"
                    );

            boolean hasAppModule =
                    appDirectory.isDirectory();

            File buildFile =
                    new File(
                            appDirectory,
                            "build.gradle"
                    );

            File buildFileKts =
                    new File(
                            appDirectory,
                            "build.gradle.kts"
                    );

            boolean hasBuildFile =
                    buildFile.isFile()
                            || buildFileKts.isFile();

            BuildEnvironment environment =
                    new BuildEnvironment(
                            hasUnixWrapper,
                            hasWindowsWrapper,
                            hasAppModule,
                            hasBuildFile
                    );

            if (!environment.isReady()) {

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                environment.getProblem(),
                                ENGINE_ID
                        )
                );
            }

            return JarvisResult.success(
                    environment,
                    "Real Gradle build environment detected."
            );

        } catch (Exception e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            "Build environment inspection failed.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * Build Debug APK.
     */
    public JarvisResult<BuildRecord>
    buildDebug() {

        return build(
                "assembleDebug",
                DEFAULT_TIMEOUT_SECONDS
        );
    }

    /**
     * Build باستعمال Gradle task محددة.
     */
    public synchronized JarvisResult<BuildRecord>
    build(
            String gradleTask,
            long timeoutSeconds
    ) {

        if (gradleTask == null ||
                gradleTask.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Gradle task cannot be empty."
            );
        }

        if (timeoutSeconds <= 0) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Build timeout must be greater than zero."
            );
        }

        if (building) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    "Another build is already running."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Security boundary is not active."
            );
        }

        JarvisResult<Boolean>
                authorization =
                securityBoundary
                        .authorizeEvolutionChange(
                                "PROJECT_BUILD"
                        );

        if (authorization == null ||
                !authorization.isSuccess()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Project build was not authorized."
            );
        }

        JarvisResult<BuildEnvironment>
                environmentResult =
                inspectEnvironment();

        if (environmentResult == null ||
                !environmentResult.isSuccess()) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    environmentResult == null
                            ? "Could not inspect build environment."
                            : environmentResult.getMessage()
            );
        }

        BuildEnvironment environment =
                environmentResult.getData();

        File wrapper =
                chooseWrapper(
                        environment
                );

        if (wrapper == null) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    "No executable Gradle Wrapper is available."
            );
        }

        List<String> command =
                createGradleCommand(
                        wrapper,
                        gradleTask
                );

        long startedAt =
                System.currentTimeMillis();

        building = true;

        Process process = null;

        List<String> output =
                new ArrayList<>();

        int exitCode = -1;

        boolean timedOut = false;

        try {

            ProcessBuilder builder =
                    new ProcessBuilder(
                            command
                    );

            builder.directory(
                    workspaceRoot
            );

            builder.redirectErrorStream(
                    true
            );

            process =
                    builder.start();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream(),
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    output.add(
                            line
                    );
                }
            }

            boolean finished =
                    process.waitFor(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );

            if (!finished) {

                timedOut = true;

                process.destroy();

                if (!process.waitFor(
                        5,
                        TimeUnit.SECONDS
                )) {

                    process.destroyForcibly();
                }

                long duration =
                        System.currentTimeMillis()
                                - startedAt;

                BuildRecord record =
                        BuildRecord.failed(
                                gradleTask,
                                command,
                                -1,
                                output,
                                duration,
                                true,
                                null,
                                "Build timed out."
                        );

                lastBuild = record;

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                "Gradle build timed out.",
                                ENGINE_ID
                        ),
                        record.getSummary()
                );
            }

            exitCode =
                    process.exitValue();

            long duration =
                    System.currentTimeMillis()
                            - startedAt;

            File apk =
                    findDebugApk();

            boolean processSucceeded =
                    exitCode == 0;

            boolean apkExists =
                    apk != null &&
                            apk.isFile() &&
                            apk.length() > 0;

            if (!processSucceeded) {

                BuildRecord record =
                        BuildRecord.failed(
                                gradleTask,
                                command,
                                exitCode,
                                output,
                                duration,
                                false,
                                apk,
                                "Gradle process failed."
                        );

                lastBuild = record;

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                createFailureMessage(
                                        exitCode,
                                        output
                                ),
                                ENGINE_ID
                        ),
                        record.getSummary()
                );
            }

            if (!apkExists) {

                BuildRecord record =
                        BuildRecord.failed(
                                gradleTask,
                                command,
                                exitCode,
                                output,
                                duration,
                                false,
                                null,
                                "Gradle returned success but no APK was found."
                        );

                lastBuild = record;

                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.BUILD_FAILED,
                                "Build did not produce a valid APK.",
                                ENGINE_ID
                        ),
                        record.getSummary()
                );
            }

            BuildRecord record =
                    BuildRecord.success(
                            gradleTask,
                            command,
                            exitCode,
                            output,
                            duration,
                            apk
                    );

            lastBuild = record;

            return JarvisResult.success(
                    record,
                    "Debug APK built successfully."
            );

        } catch (IOException e) {

            long duration =
                    System.currentTimeMillis()
                            - startedAt;

            BuildRecord record =
                    BuildRecord.failed(
                            gradleTask,
                            command,
                            exitCode,
                            output,
                            duration,
                            timedOut,
                            null,
                            "Could not start Gradle process."
                    );

            lastBuild = record;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            "Could not start the Gradle build process.",
                            ENGINE_ID,
                            e
                    )
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            if (process != null) {
                process.destroy();
            }

            long duration =
                    System.currentTimeMillis()
                            - startedAt;

            BuildRecord record =
                    BuildRecord.failed(
                            gradleTask,
                            command,
                            exitCode,
                            output,
                            duration,
                            false,
                            null,
                            "Build thread was interrupted."
                    );

            lastBuild = record;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.BUILD_FAILED,
                            "Gradle build was interrupted.",
                            ENGINE_ID,
                            e
                    )
            );

        } finally {

            building = false;
        }
    }

    /**
     * البحث عن APK Debug الناتج.
     */
    public synchronized File findDebugApk() {

        File direct =
                new File(
                        workspaceRoot,
                        "app/build/outputs/apk/debug/app-debug.apk"
                );

        if (isValidApk(
                direct
        )) {
            return direct;
        }

        File outputDirectory =
                new File(
                        workspaceRoot,
                        "app/build/outputs/apk"
                );

        if (!outputDirectory.isDirectory()) {
            return null;
        }

        return findApkRecursively(
                outputDirectory
        );
    }

    /**
     * ينسخ APK إلى مكان آمن داخل Workspace.
     */
    public synchronized JarvisResult<File>
    copyApkTo(
            String relativeDestination
    ) {

        if (relativeDestination == null ||
                relativeDestination.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "APK destination cannot be empty."
            );
        }

        File apk =
                findDebugApk();

        if (apk == null) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "No valid debug APK exists."
            );
        }

        File destination =
                resolveSafeDestination(
                        relativeDestination
                );

        if (destination == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "APK destination is outside the workspace."
            );
        }

        try {

            File parent =
                    destination.getParentFile();

            if (parent != null &&
                    !parent.exists() &&
                    !parent.mkdirs()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Could not create APK destination directory."
                );
            }

            Files.copy(
                    apk.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            if (!isValidApk(
                    destination
            )) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Copied APK failed validation."
                );
            }

            return JarvisResult.success(
                    destination,
                    "APK copied successfully."
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Could not copy APK.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    public boolean isBuilding() {
        return building;
    }

    public BuildRecord getLastBuild() {
        return lastBuild;
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

    private File chooseWrapper(
            BuildEnvironment environment
    ) {

        /*
         * على Android/Linux نستعمل gradlew.
         */
        File unix =
                new File(
                        workspaceRoot,
                        GRADLE_WRAPPER
                );

        if (unix.isFile()) {
            return unix;
        }

        /*
         * على Windows نستعمل gradlew.bat.
         */
        File windows =
                new File(
                        workspaceRoot,
                        GRADLE_WRAPPER_WINDOWS
                );

        if (windows.isFile()) {
            return windows;
        }

        return null;
    }

    private List<String>
    createGradleCommand(
            File wrapper,
            String task
    ) {

        List<String> command =
                new ArrayList<>();

        String name =
                wrapper.getName()
                        .toLowerCase();

        if (name.endsWith(
                ".bat"
        )) {

            command.add(
                    "cmd"
            );

            command.add(
                    "/c"
            );

            command.add(
                    wrapper.getAbsolutePath()
            );

        } else {

            command.add(
                    wrapper.getAbsolutePath()
            );
        }

        command.add(
                task
        );

        /*
         * يمنع daemon من البقاء بعد انتهاء Build.
         */
        command.add(
                "--no-daemon"
        );

        /*
         * يجعل output أوضح لمحرك التشخيص.
         */
        command.add(
                "--stacktrace"
        );

        return command;
    }

    private File findApkRecursively(
            File directory
    ) {

        File[] children =
                directory.listFiles();

        if (children == null) {
            return null;
        }

        for (File child :
                children) {

            if (child == null) {
                continue;
            }

            if (child.isFile() &&
                    child.getName()
                            .equalsIgnoreCase(
                                    "app-debug.apk"
                            ) &&
                    isValidApk(
                            child
                    )) {

                return child;
            }

            if (child.isDirectory()) {

                File result =
                        findApkRecursively(
                                child
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private boolean isValidApk(
            File apk
    ) {

        return apk != null &&
                apk.isFile() &&
                apk.length() > 0;
    }

    private File resolveSafeDestination(
            String relativePath
    ) {

        String normalized =
                relativePath
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        while (
                normalized.startsWith("/")
        ) {

            normalized =
                    normalized.substring(
                            1
                    );
        }

        if (normalized.isEmpty() ||
                normalized.equals(".") ||
                normalized.contains(
                        "../"
                ) ||
                normalized.startsWith(
                        ".."
                )) {

            return null;
        }

        File destination =
                new File(
                        workspaceRoot,
                        normalized
                );

        try {

            java.nio.file.Path root =
                    workspaceRoot
                            .getCanonicalFile()
                            .toPath();

            java.nio.file.Path target =
                    destination
                            .getCanonicalFile()
                            .toPath();

            if (!target.startsWith(
                    root
            )) {
                return null;
            }

            return destination;

        } catch (IOException e) {

            return null;
        }
    }

    private String createFailureMessage(
            int exitCode,
            List<String> output
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Gradle failed with exit code "
        );

        builder.append(
                exitCode
        );

        builder.append(
                "."
        );

        if (output != null &&
                !output.isEmpty()) {

            builder.append(
                    " Last output: "
            );

            int start =
                    Math.max(
                            0,
                            output.size() - 8
                    );

            for (
                    int i = start;
                    i < output.size();
                    i++
            ) {

                builder.append(
                        output.get(i)
                );

                if (i < output.size() - 1) {
                    builder.append(
                            " | "
                    );
                }
            }
        }

        return builder.toString();
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
     * معلومات بيئة البناء.
     */
    public static final class BuildEnvironment {

        private final boolean hasUnixWrapper;
        private final boolean hasWindowsWrapper;
        private final boolean hasAppModule;
        private final boolean hasBuildFile;

        private BuildEnvironment(
                boolean hasUnixWrapper,
                boolean hasWindowsWrapper,
                boolean hasAppModule,
                boolean hasBuildFile
        ) {

            this.hasUnixWrapper =
                    hasUnixWrapper;

            this.hasWindowsWrapper =
                    hasWindowsWrapper;

            this.hasAppModule =
                    hasAppModule;

            this.hasBuildFile =
                    hasBuildFile;
        }

        public boolean hasUnixWrapper() {
            return hasUnixWrapper;
        }

        public boolean hasWindowsWrapper() {
            return hasWindowsWrapper;
        }

        public boolean hasAppModule() {
            return hasAppModule;
        }

        public boolean hasBuildFile() {
            return hasBuildFile;
        }

        public boolean isReady() {

            return (
                    hasUnixWrapper ||
                    hasWindowsWrapper
            )
                    && hasAppModule
                    && hasBuildFile;
        }

        public String getProblem() {

            if (!(
                    hasUnixWrapper ||
                    hasWindowsWrapper
            )) {

                return "Gradle Wrapper is missing.";
            }

            if (!hasAppModule) {

                return "Android app module is missing.";
            }

            if (!hasBuildFile) {

                return "App Gradle build file is missing.";
            }

            return "";
        }
    }

    /**
     * النتيجة الكاملة لعملية Build.
     */
    public static final class BuildRecord {

        private final String task;
        private final List<String> command;
        private final int exitCode;
        private final List<String> output;
        private final long durationMillis;
        private final boolean timedOut;
        private final File apkFile;
        private final boolean success;
        private final String summary;

        private BuildRecord(
                String task,
                List<String> command,
                int exitCode,
                List<String> output,
                long durationMillis,
                boolean timedOut,
                File apkFile,
                boolean success,
                String summary
        ) {

            this.task =
                    task;

            this.command =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    command
                            )
                    );

            this.exitCode =
                    exitCode;

            this.output =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    output
                            )
                    );

            this.durationMillis =
                    durationMillis;

            this.timedOut =
                    timedOut;

            this.apkFile =
                    apkFile;

            this.success =
                    success;

            this.summary =
                    summary;
        }

        public static BuildRecord success(
                String task,
                List<String> command,
                int exitCode,
                List<String> output,
                long durationMillis,
                File apkFile
        ) {

            return new BuildRecord(
                    task,
                    command,
                    exitCode,
                    output,
                    durationMillis,
                    false,
                    apkFile,
                    true,
                    "Build succeeded and APK was produced."
            );
        }

        public static BuildRecord failed(
                String task,
                List<String> command,
                int exitCode,
                List<String> output,
                long durationMillis,
                boolean timedOut,
                File apkFile,
                String summary
        ) {

            return new BuildRecord(
                    task,
                    command,
                    exitCode,
                    output,
                    durationMillis,
                    timedOut,
                    apkFile,
                    false,
                    summary
            );
        }

        public String getTask() {
            return task;
        }

        public List<String> getCommand() {
            return command;
        }

        public int getExitCode() {
            return exitCode;
        }

        public List<String> getOutput() {
            return output;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public File getApkFile() {
            return apkFile;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSummary() {
            return summary;
        }
    }
}