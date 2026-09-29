package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Project Workspace Manager
 *
 * مسؤول عن تحديد وإدارة Workspace حقيقي للمشروع.
 *
 * الفرق بين:
 *
 * 1. Runtime Workspace:
 *    مساحة تخزين بيانات وArtifacts الخاصة بـJARVIS.
 *
 * 2. Project Workspace:
 *    جذر مشروع Android حقيقي يمكن أن يحتوي على:
 *
 *      gradlew
 *      gradlew.bat
 *      settings.gradle
 *      settings.gradle.kts
 *      build.gradle
 *      build.gradle.kts
 *      app/
 *
 * BuildEngine لا يعتبر المشروع قابلاً للبناء
 * إلا إذا اجتاز فحص هذا المكون.
 *
 * هذا المكون لا:
 * - يتجاوز Android sandbox.
 * - يصل إلى GitHub من تلقاء نفسه.
 * - يغير APK المثبت.
 * - يمنح صلاحيات للنظام.
 *
 * هو فقط يحدد Workspace حقيقي وآمن
 * ويمنع الخلط بين مساحة بيانات JARVIS
 * ومشروع Android.
 */
public final class ProjectWorkspaceManager {

    private static final String ENGINE_ID =
            "v2.project_workspace_manager";

    private static final String[] PROTECTED_NAMES = {
            "OwnerSecurityBoundary.java",
            "OwnerSecurityCore.java",
            "securityboundary",
            "owner_security",
            "authorization"
    };

    private final OwnerSecurityBoundary securityBoundary;

    private File projectRoot;

    private WorkspaceState state =
            WorkspaceState.NOT_CONFIGURED;

    private ProjectInspection lastInspection;

    public ProjectWorkspaceManager(
            OwnerSecurityBoundary securityBoundary
    ) {

        if (securityBoundary == null) {
            throw new IllegalArgumentException(
                    "securityBoundary cannot be null."
            );
        }

        this.securityBoundary =
                securityBoundary;
    }

    /**
     * تعيين Project Workspace باستعمال File.
     *
     * لا يتم قبول المسار إلا بعد الفحص.
     */
    public synchronized JarvisResult<ProjectInspection>
    configure(
            File root
    ) {

        if (root == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Project workspace cannot be null."
            );
        }

        File absoluteRoot =
                root.getAbsoluteFile();

        JarvisResult<ProjectInspection>
                inspectionResult =
                inspect(
                        absoluteRoot
                );

        if (inspectionResult == null ||
                !inspectionResult.isSuccess()) {

            state =
                    WorkspaceState.INVALID;

            lastInspection =
                    inspectionResult == null
                            ? null
                            : inspectionResult.getData();

            return inspectionResult == null
                    ? failure(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Project workspace inspection returned no result."
                    )
                    : inspectionResult;
        }

        ProjectInspection inspection =
                inspectionResult.getData();

        if (inspection == null ||
                !inspection.isReady()) {

            state =
                    WorkspaceState.INVALID;

            lastInspection =
                    inspection;

            return failure(
                    JarvisError.Type.BUILD_FAILED,
                    inspection == null
                            ? "Project workspace is invalid."
                            : inspection.getProblem()
            );
        }

        this.projectRoot =
                absoluteRoot;

        this.lastInspection =
                inspection;

        this.state =
                WorkspaceState.READY;

        return JarvisResult.success(
                inspection,
                "Real Android project workspace configured."
        );
    }

    /**
     * يفحص Workspace بدون تغييره.
     */
    public synchronized JarvisResult<ProjectInspection>
    inspect(
            File root
    ) {

        if (root == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Project workspace cannot be null."
            );
        }

        File absoluteRoot =
                root.getAbsoluteFile();

        try {

            if (!absoluteRoot.exists()) {

                return failure(
                        JarvisError.Type.NOT_FOUND,
                        "Project workspace does not exist: "
                                + absoluteRoot.getAbsolutePath()
                );
            }

            if (!absoluteRoot.isDirectory()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Project workspace is not a directory."
                );
            }

            File settingsGradle =
                    new File(
                            absoluteRoot,
                            "settings.gradle"
                    );

            File settingsGradleKts =
                    new File(
                            absoluteRoot,
                            "settings.gradle.kts"
                    );

            boolean hasSettings =
                    settingsGradle.isFile()
                            || settingsGradleKts.isFile();

            File rootBuildGradle =
                    new File(
                            absoluteRoot,
                            "build.gradle"
                    );

            File rootBuildGradleKts =
                    new File(
                            absoluteRoot,
                            "build.gradle.kts"
                    );

            boolean hasRootBuild =
                    rootBuildGradle.isFile()
                            || rootBuildGradleKts.isFile();

            File appDirectory =
                    new File(
                            absoluteRoot,
                            "app"
                    );

            boolean hasAppModule =
                    appDirectory.isDirectory();

            File appBuildGradle =
                    new File(
                            appDirectory,
                            "build.gradle"
                    );

            File appBuildGradleKts =
                    new File(
                            appDirectory,
                            "build.gradle.kts"
                    );

            boolean hasAppBuild =
                    appBuildGradle.isFile()
                            || appBuildGradleKts.isFile();

            File gradlew =
                    new File(
                            absoluteRoot,
                            "gradlew"
                    );

            File gradlewBat =
                    new File(
                            absoluteRoot,
                            "gradlew.bat"
                    );

            boolean hasGradleWrapper =
                    gradlew.isFile()
                            || gradlewBat.isFile();

            File gradleDirectory =
                    new File(
                            absoluteRoot,
                            "gradle"
                    );

            boolean hasGradleDirectory =
                    gradleDirectory.isDirectory();

            List<String> missing =
                    new ArrayList<>();

            if (!hasSettings) {
                missing.add(
                        "settings.gradle or settings.gradle.kts"
                );
            }

            if (!hasRootBuild) {
                missing.add(
                        "build.gradle or build.gradle.kts"
                );
            }

            if (!hasAppModule) {
                missing.add(
                        "app/"
                );
            }

            if (!hasAppBuild) {
                missing.add(
                        "app/build.gradle or app/build.gradle.kts"
                );
            }

            if (!hasGradleWrapper) {
                missing.add(
                        "gradlew or gradlew.bat"
                );
            }

            boolean ready =
                    missing.isEmpty();

            String problem =
                    ready
                            ? ""
                            : "Project is incomplete. Missing: "
                                    + join(
                                            missing
                                    );

            ProjectInspection inspection =
                    new ProjectInspection(
                            absoluteRoot,
                            hasSettings,
                            hasRootBuild,
                            hasAppModule,
                            hasAppBuild,
                            hasGradleWrapper,
                            hasGradleDirectory,
                            ready,
                            problem,
                            missing
                    );

            return JarvisResult.success(
                    inspection,
                    ready
                            ? "Android project is ready for build."
                            : "Android project is incomplete."
            );

        } catch (Exception exception) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Project workspace inspection failed.",
                            ENGINE_ID,
                            exception
                    )
            );
        }
    }

    /**
     * يحدد Workspace باستعمال مسار نصي.
     */
    public synchronized JarvisResult<ProjectInspection>
    configurePath(
            String path
    ) {

        if (path == null ||
                path.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Project workspace path cannot be empty."
            );
        }

        return configure(
                new File(
                        path.trim()
                )
        );
    }

    /**
     * فحص هل Workspace الحالي صالح للبناء.
     */
    public synchronized boolean isReady() {

        return projectRoot != null
                && state == WorkspaceState.READY
                && lastInspection != null
                && lastInspection.isReady();
    }

    /**
     * إرجاع جذر المشروع الحالي.
     */
    public synchronized File getProjectRoot() {

        if (projectRoot == null) {
            return null;
        }

        return projectRoot;
    }

    /**
     * إرجاع المسار.
     */
    public synchronized String getProjectRootPath() {

        return projectRoot == null
                ? ""
                : projectRoot.getAbsolutePath();
    }

    /**
     * إرجاع الحالة.
     */
    public synchronized WorkspaceState getState() {

        return state;
    }

    /**
     * إرجاع آخر Inspection.
     */
    public synchronized ProjectInspection
    getLastInspection() {

        return lastInspection;
    }

    /**
     * إعادة فحص المشروع الحالي.
     */
    public synchronized JarvisResult<ProjectInspection>
    revalidate() {

        if (projectRoot == null) {

            state =
                    WorkspaceState.NOT_CONFIGURED;

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "No project workspace has been configured."
            );
        }

        JarvisResult<ProjectInspection>
                result =
                inspect(
                        projectRoot
                );

        if (result != null &&
                result.isSuccess() &&
                result.getData() != null &&
                result.getData().isReady()) {

            lastInspection =
                    result.getData();

            state =
                    WorkspaceState.READY;

        } else {

            state =
                    WorkspaceState.INVALID;

            if (result != null) {
                lastInspection =
                        result.getData();
            }
        }

        return result;
    }

    /**
     * إلغاء Workspace الحالي.
     */
    public synchronized void clear() {

        projectRoot =
                null;

        lastInspection =
                null;

        state =
                WorkspaceState.NOT_CONFIGURED;
    }

    /**
     * فحص أمان المسار.
     *
     * يمنع استعمال مسارات تحتوي على مناطق أمنية
     * كمشروع تطور.
     */
    public boolean isSafeProjectPath(
            File root
    ) {

        if (root == null) {
            return false;
        }

        String path =
                root.getAbsolutePath()
                        .replace(
                                '\\',
                                '/'
                        )
                        .toLowerCase();

        for (
                String protectedName
                : PROTECTED_NAMES
        ) {

            String normalized =
                    protectedName
                            .toLowerCase();

            if (path.contains(
                    "/" + normalized + "/"
            )
                    || path.endsWith(
                    "/" + normalized
            )) {

                return false;
            }
        }

        return !path.contains(
                "../"
        );
    }

    /**
     * التحقق من أن ملفاً يقع داخل المشروع.
     */
    public synchronized boolean isInsideProject(
            File file
    ) {

        if (file == null ||
                projectRoot == null) {

            return false;
        }

        try {

            String rootPath =
                    projectRoot
                            .getCanonicalPath();

            String filePath =
                    file.getCanonicalPath();

            return filePath.equals(
                    rootPath
            )
                    || filePath.startsWith(
                    rootPath + File.separator
            );

        } catch (Exception ignored) {

            return false;
        }
    }

    /**
     * إنشاء مسار آمن داخل المشروع.
     */
    public synchronized File resolveProjectFile(
            String relativePath
    ) {

        if (!isReady()) {
            return null;
        }

        if (relativePath == null ||
                relativePath.trim().isEmpty()) {

            return null;
        }

        String normalized =
                relativePath
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        if (normalized.startsWith("/")
                || normalized.contains("../")
                || normalized.contains("..\\")) {

            return null;
        }

        File candidate =
                new File(
                        projectRoot,
                        normalized
                );

        if (!isInsideProject(
                candidate
        )) {

            return null;
        }

        return candidate;
    }

    /**
     * معلومات الحالة الحالية.
     */
    public synchronized String getStatus() {

        return "state="
                + state.name()
                + ", ready="
                + isReady()
                + ", root="
                + getProjectRootPath();
    }

    private String join(
            List<String> values
    ) {

        if (values == null ||
                values.isEmpty()) {

            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (
                int i = 0;
                i < values.size();
                i++
        ) {

            if (i > 0) {
                builder.append(
                        ", "
                );
            }

            builder.append(
                    values.get(i)
            );
        }

        return builder.toString();
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

    public enum WorkspaceState {

        NOT_CONFIGURED,

        READY,

        INVALID
    }

    /**
     * نتيجة فحص مشروع Android.
     */
    public static final class ProjectInspection {

        private final File root;

        private final boolean hasSettings;

        private final boolean hasRootBuild;

        private final boolean hasAppModule;

        private final boolean hasAppBuild;

        private final boolean hasGradleWrapper;

        private final boolean hasGradleDirectory;

        private final boolean ready;

        private final String problem;

        private final List<String> missing;

        private ProjectInspection(
                File root,
                boolean hasSettings,
                boolean hasRootBuild,
                boolean hasAppModule,
                boolean hasAppBuild,
                boolean hasGradleWrapper,
                boolean hasGradleDirectory,
                boolean ready,
                String problem,
                List<String> missing
        ) {

            this.root =
                    root;

            this.hasSettings =
                    hasSettings;

            this.hasRootBuild =
                    hasRootBuild;

            this.hasAppModule =
                    hasAppModule;

            this.hasAppBuild =
                    hasAppBuild;

            this.hasGradleWrapper =
                    hasGradleWrapper;

            this.hasGradleDirectory =
                    hasGradleDirectory;

            this.ready =
                    ready;

            this.problem =
                    problem == null
                            ? ""
                            : problem;

            this.missing =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    missing == null
                                            ? Collections.emptyList()
                                            : missing
                            )
                    );
        }

        public File getRoot() {
            return root;
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

        public boolean hasAppBuild() {
            return hasAppBuild;
        }

        public boolean hasGradleWrapper() {
            return hasGradleWrapper;
        }

        public boolean hasGradleDirectory() {
            return hasGradleDirectory;
        }

        public boolean isReady() {
            return ready;
        }

        public String getProblem() {
            return problem;
        }

        public List<String> getMissing() {
            return missing;
        }

        @Override
        public String toString() {

            return "ProjectInspection{" +
                    "root=" +
                    root +
                    ", hasSettings=" +
                    hasSettings +
                    ", hasRootBuild=" +
                    hasRootBuild +
                    ", hasAppModule=" +
                    hasAppModule +
                    ", hasAppBuild=" +
                    hasAppBuild +
                    ", hasGradleWrapper=" +
                    hasGradleWrapper +
                    ", hasGradleDirectory=" +
                    hasGradleDirectory +
                    ", ready=" +
                    ready +
                    ", problem='" +
                    problem +
                    '\'' +
                    ", missing=" +
                    missing +
                    '}';
        }
    }
}