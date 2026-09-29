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
 * Build حقيقي للمشروع Android.
 *
 * هذا المحرك:
 *
 * 1. يستعمل ProjectWorkspaceManager إذا كان المشروع الحقيقي مهيأ.
 * 2. لا يستعمل jarvis_workspace كبديل صامت للمشروع الحقيقي.
 * 3. يتحقق من Gradle Wrapper و Android app module.
 * 4. يشغل assembleDebug.
 * 5. لا يعتبر Build ناجحاً إلا إذا:
 *      - Gradle exit code = 0
 *      - APK موجود
 *      - APK حجمه أكبر من صفر
 * 6. يسجل output الحقيقي.
 * 7. يدعم timeout.
 * 8. يحافظ على Security Boundary.
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

    private final OwnerSecurityBoundary securityBoundary;

    /*
     * المشروع الحقيقي.
     *
     * إذا كان null نستعمل fixedWorkspaceRoot
     * فقط للتوافق مع constructor القديم.
     */
    private final ProjectWorkspaceManager projectWorkspaceManager;

    private final File fixedWorkspaceRoot;

    private volatile boolean building;

    private volatile BuildRecord lastBuild;

    /**
     * Constructor قديم للتوافق.
     *
     * لا نحذفه حتى لا نكسر الملفات الموجودة.
     */
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

        this.securityBoundary =
                securityBoundary;

        this.fixedWorkspaceRoot =
                workspaceRoot.getAbsoluteFile();

        this.projectWorkspaceManager =
                null;
    }

    /**
     * Constructor الجديد للمشروع الحقيقي.
     */
    public BuildEngine(
            ProjectWorkspaceManager projectWorkspaceManager,
            OwnerSecurityBoundary securityBoundary
    ) {

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

        this.securityBoundary =
                securityBoundary;

        this.projectWorkspaceManager =
                projectWorkspaceManager;

        this.fixedWorkspaceRoot =
                null;
    }

    /**
     * الحصول على Workspace الحقيقي.
     *
     * مهم:
     * إذا استعملنا ProjectWorkspaceManager،
     * فلا نرجع إلى jarvis_workspace تلقائياً.
     */
    private File resolveWorkspace() {

        if (projectWorkspaceManager != null) {

            if (!projectWorkspaceManager.isReady()) {
                return null;
            }

            File root =
                    projectWorkspaceManager
                            .getProjectRoot();

            if (root == null) {
                return null;
            }

            return root.getAbsoluteFile();
        }

        return fixedWorkspaceRoot;
    }

    /**
     * فحص بيئة البناء الحقيقية.
     */
    public synchronized JarvisResult<BuildEnvironment>
    inspectEnvironment() {

        File workspaceRoot =
                resolveWorkspace();

        if (workspaceRoot == null) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    "Real Android project workspace is not configured."
            );
        }

        try {

            if (!workspaceRoot.exists()) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "Build project root does not exist: "
                                + workspaceRoot.getAbsolutePath()
                );
            }

            if (!workspaceRoot.isDirectory()) {

                return failure(
                        JarvisError.Type.BUILD_FAILED,
                        "Build project root is not a directory."
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

            File rootBuildGradle =
                    new File(
                            workspaceRoot,
                            "build.gradle"
                    );

            File rootBuildKts =
                    new File(
                            workspaceRoot,
                            "build.gradle.kts"
                    );

            boolean hasRootBuild =
                    rootBuildGradle.isFile()
                            || rootBuildKts.isFile();

            File appDirectory =
                    new File(
                            workspaceRoot,
                            "app"
                    );

            boolean hasAppModule =
                    appDirectory.isDirectory();

            File appBuildGradle =
                    new File(
                            appDirectory,
                            "build.gradle"
                    );

            File appBuildKts =
                    new File(
                            appDirectory,
                            "build.gradle.kts"
                    );

            boolean hasAppBuild =
                    appBuildGradle.isFile()
                            || appBuildKts.isFile();

            BuildEnvironment environment =
                    new BuildEnvironment(
                            hasUnixWrapper,
                            hasWindowsWrapper,
                            hasSettings,
                            hasRootBuild,
                            hasAppModule,
                            hasAppBuild
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
                    "Real Android Gradle project detected."
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
     * تنفيذ Gradle task حقيقية.
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

        JarvisResult<Boolean> authorization =
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

        File workspaceRoot =
                resolveWorkspace();

        if (workspaceRoot == null) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    "No real project workspace is configured."
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
                        workspaceRoot,
                        environment
                );

        if (wrapper == null) {

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    "No Gradle Wrapper is available."
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

            /*
             * في Android/Linux:
             * gradlew يحتاج execute permission.
             *
             * إذا لم تكن executable نحاول استعمال sh.
             */
            List<String> actualCommand =
                    prepareExecutableCommand(
                            wrapper,
                            command
                    );

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            actualCommand
                    );

            processBuilder.directory(
                    workspaceRoot
            );

            processBuilder.redirectErrorStream(
                    true
            );

            process =
                    processBuilder.start();

            /*
             * نقرأ output أثناء Build.
             */
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                output.add(line);
            }

            reader.close();

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
                                actualCommand,
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
                                actualCommand,
                                exitCode,
                                output,
                                duration,
                                false,
                                apk,
                                createFailureMessage(
                                        exitCode,
                                        output
                                )
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
                                actualCommand,
                                exitCode,
                                output,
                                duration,
                                false,
                                null,
                                "Gradle returned success but no valid APK was produced."
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
                            actualCommand,
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
     * البحث عن APK Debug الحقيقي.
     */
    public synchronized File findDebugApk() {

        File workspaceRoot =
                resolveWorkspace();

        if (workspaceRoot == null) {
            return null;
        }

        File direct =
                new File(
                        workspaceRoot,
                        "app/build/outputs/apk/debug/app-debug.apk"
                );

        if (isValidApk(direct)) {
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
     * نسخ APK الناتج إلى داخل المشروع.
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

        File workspaceRoot =
                resolveWorkspace();

        if (workspaceRoot == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Project workspace is not configured."
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
                        workspaceRoot,
                        relativeDestination
                );

        if (destination == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "APK destination is outside the project."
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

            if (!isValidApk(destination)) {

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

    /**
     * يرجع المشروع الحقيقي إذا كان مربوطاً.
     */
    public File getWorkspaceRoot() {
        return resolveWorkspace();
    }

    public OwnerSecurityBoundary
    getSecurityBoundary() {
        return securityBoundary;
    }

    public ProjectWorkspaceManager
    getProjectWorkspaceManager() {
        return projectWorkspaceManager;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * اختيار Gradle Wrapper.
     */
    private File chooseWrapper(
            File workspaceRoot,
            BuildEnvironment environment
    ) {

        if (workspaceRoot == null ||
                environment == null) {

            return null;
        }

        File unix =
                new File(
                        workspaceRoot,
                        GRADLE_WRAPPER
                );

        if (environment.hasUnixWrapper() &&
                unix.isFile()) {

            return unix;
        }

        File windows =
                new File(
                        workspaceRoot,
                        GRADLE_WRAPPER_WINDOWS
                );

        if (environment.hasWindowsWrapper() &&
                windows.isFile()) {

            return windows;
        }

        return null;
    }

    /**
     * إنشاء أمر Gradle.
     */
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

        if (name.endsWith(".bat")) {

            command.add("cmd");
            command.add("/c");
            command.add(wrapper.getAbsolutePath());

        } else {

            command.add(
                    wrapper.getAbsolutePath()
            );
        }

        command.add(task);

        command.add("--no-daemon");

        command.add("--stacktrace");

        return command;
    }

    /**
     * تجهيز الأمر على Linux/Android.
     *
     * إذا كان gradlew executable:
     *
     * ./gradlew
     *
     * إذا لم يكن executable:
     *
     * sh gradlew
     */
    private List<String>
    prepareExecutableCommand(
            File wrapper,
            List<String> command
    ) {

        if (wrapper == null ||
                command == null) {

            return command;
        }

        String name =
                wrapper.getName()
                        .toLowerCase();

        /*
         * Windows لا يحتاج sh.
         */
        if (name.endsWith(".bat")) {
            return command;
        }

        /*
         * إذا executable نستعمله مباشرة.
         */
        if (wrapper.canExecute()) {
            return command;
        }

        /*
         * محاولة حقيقية لتشغيل Gradle Wrapper
         * حتى إذا لم يحمل execute bit.
         */
        List<String> shellCommand =
                new ArrayList<>();

        shellCommand.add("sh");

        shellCommand.add(
                wrapper.getAbsolutePath()
        );

        for (int i = 1;
             i < command.size();
             i++) {

            shellCommand.add(
                    command.get(i)
            );
        }

        return shellCommand;
    }

    /**
     * البحث عن APK بشكل recursive.
     */
    private File findApkRecursively(
            File directory
    ) {

        if (directory == null ||
                !directory.isDirectory()) {

            return null;
        }

        File[] children =
                directory.listFiles();

        if (children == null) {
            return null;
        }

        for (File child : children) {

            if (child == null) {
                continue;
            }

            if (child.isFile() &&
                    child.getName()
                            .equalsIgnoreCase(
                                    "app-debug.apk"
                            ) &&
                    isValidApk(child)) {

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

    /**
     * منع خروج APK destination من المشروع.
     */
    private File resolveSafeDestination(
            File workspaceRoot,
            String relativePath
    ) {

        if (workspaceRoot == null ||
                relativePath == null) {

            return null;
        }

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
                    normalized.substring(1);
        }

        if (normalized.isEmpty() ||
                normalized.equals(".") ||
                normalized.contains("../") ||
                normalized.startsWith("..")) {

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

            if (!target.startsWith(root)) {
                return null;
            }

            return destination;

        } catch (IOException e) {

            return null;
        }
    }

    /**
     * إنشاء رسالة فشل مفيدة من Gradle output.
     */
    private String createFailureMessage(
            int exitCode,
            List<String> output
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Gradle failed with exit code "
        );

        builder.append(exitCode);

        builder.append(".");

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
     * معلومات Build Environment.
     */
    public static final class BuildEnvironment {

        private final boolean hasUnixWrapper;
        private final boolean hasWindowsWrapper;
        private final boolean hasSettings;
        private final boolean hasRootBuild;
        private final boolean hasAppModule;
        private final boolean hasBuildFile;

        private BuildEnvironment(
                boolean hasUnixWrapper,
                boolean hasWindowsWrapper,
                boolean hasSettings,
                boolean hasRootBuild,
                boolean hasAppModule,
                boolean hasBuildFile
        ) {

            this.hasUnixWrapper =
                    hasUnixWrapper;

            this.hasWindowsWrapper =
                    hasWindowsWrapper;

            this.hasSettings =
                    hasSettings;

            this.hasRootBuild =
                    hasRootBuild;

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

        public boolean hasSettings() {
            return hasSettings;
        }

        public boolean hasRootBuild() {
            return hasRootBuild;
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
                    && hasSettings
                    && hasRootBuild
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

            if (!hasSettings) {

                return "Gradle settings file is missing.";
            }

            if (!hasRootBuild) {

                return "Root Gradle build file is missing.";
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
     * النتيجة الكاملة للـBuild.
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
                                    command == null
                                            ? Collections
                                                    .<String>emptyList()
                                            : command
                            )
                    );

            this.exitCode =
                    exitCode;

            this.output =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    output == null
                                            ? Collections
                                                    .<String>emptyList()
                                            : output
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
                    summary == null
                            ? ""
                            : summary;
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