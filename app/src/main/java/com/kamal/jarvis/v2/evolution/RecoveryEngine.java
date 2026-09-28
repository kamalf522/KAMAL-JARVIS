package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2 - Recovery Engine
 *
 * مسؤول عن:
 *
 * 1. استقبال نتيجة Build.
 * 2. تحليل نتيجة SelfTest.
 * 3. تنفيذ Rollback آمن عند الفشل.
 * 4. حذف الملفات التي أنشأتها عملية البناء الحالية فقط.
 * 5. منع Recovery من لمس Owner/Security boundaries.
 * 6. إعطاء Evolution نتيجة واضحة:
 *      RECOVERED
 *      RECOVERY_FAILED
 *      NOTHING_TO_RECOVER
 *
 * Recovery لا يقوم بتعديل APK المثبت.
 * هو مسؤول عن Workspace artifacts التي أنشأها Builder.
 */
public final class RecoveryEngine {

    private static final String ENGINE_ID =
            "v2.recovery_engine";

    private final File workspaceRoot;

    private final List<RecoveryRecord> history =
            new ArrayList<>();

    private volatile RecoveryRecord lastRecovery;

    public RecoveryEngine(
            File workspaceRoot
    ) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException(
                    "workspaceRoot cannot be null."
            );
        }

        try {
            this.workspaceRoot =
                    workspaceRoot.getCanonicalFile();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid workspace root.",
                    e
            );
        }
    }

    /**
     * Recovery بعد فشل الاختبار.
     */
    public synchronized JarvisResult<RecoveryRecord> recover(
            CapabilitySpec spec,
            SelfBuilder.BuildResult buildResult,
            SelfTestEngine.TestReport testReport
    ) {
        if (spec == null) {
            return failure(
                    "CapabilitySpec cannot be null."
            );
        }

        if (buildResult == null) {
            return failure(
                    "BuildResult cannot be null."
            );
        }

        if (testReport == null) {
            return failure(
                    "TestReport cannot be null."
            );
        }

        if (testReport.isPassed()) {
            RecoveryRecord record =
                    new RecoveryRecord(
                            spec.getCapabilityId(),
                            RecoveryStatus.NOTHING_TO_RECOVER,
                            0,
                            Collections.emptyList(),
                            "Tests passed. Recovery was not required."
                    );

            saveRecord(record);

            return JarvisResult.success(
                    record,
                    record.getMessage()
            );
        }

        try {
            validateBuildResult(buildResult);

            List<File> targets =
                    new ArrayList<>(
                            buildResult.getCreatedFiles()
                    );

            /*
             * نرتب الملفات من الداخل للخارج.
             * هذا يسمح بحذف الملفات ثم المجلدات الفارغة.
             */
            Collections.reverse(targets);

            List<String> removed =
                    new ArrayList<>();

            List<String> failed =
                    new ArrayList<>();

            for (File file : targets) {

                if (file == null) {
                    continue;
                }

                if (!isInsideWorkspace(file)) {
                    failed.add(
                            "Outside workspace: "
                                    + file.getAbsolutePath()
                    );
                    continue;
                }

                if (isProtectedPath(file)) {
                    failed.add(
                            "Protected path: "
                                    + file.getAbsolutePath()
                    );
                    continue;
                }

                if (!file.exists()) {
                    continue;
                }

                if (deleteRecursivelyIfSafe(file)) {
                    removed.add(
                            file.getAbsolutePath()
                    );
                } else {
                    failed.add(
                            file.getAbsolutePath()
                    );
                }
            }

            /*
             * نحاول حذف مجلد capability إذا أصبح فارغاً.
             */
            File capabilityDirectory =
                    buildResult.getCapabilityDirectory();

            if (capabilityDirectory != null
                    && capabilityDirectory.exists()
                    && isInsideWorkspace(
                    capabilityDirectory)
                    && !isProtectedPath(
                    capabilityDirectory)) {

                if (isDirectoryEmpty(
                        capabilityDirectory)) {

                    if (capabilityDirectory.delete()) {
                        removed.add(
                                capabilityDirectory
                                        .getAbsolutePath()
                        );
                    } else {
                        failed.add(
                                capabilityDirectory
                                        .getAbsolutePath()
                        );
                    }
                }
            }

            RecoveryStatus status;

            if (failed.isEmpty()) {
                status =
                        RecoveryStatus.RECOVERED;
            } else {
                status =
                        RecoveryStatus.RECOVERY_FAILED;
            }

            String message;

            if (status == RecoveryStatus.RECOVERED) {
                message =
                        "Recovery completed successfully. "
                                + removed.size()
                                + " artifacts removed.";
            } else {
                message =
                        "Recovery completed partially. "
                                + removed.size()
                                + " artifacts removed and "
                                + failed.size()
                                + " artifacts could not be removed.";
            }

            RecoveryRecord record =
                    new RecoveryRecord(
                            spec.getCapabilityId(),
                            status,
                            removed.size(),
                            removed,
                            message,
                            failed
                    );

            saveRecord(record);

            if (status == RecoveryStatus.RECOVERED) {
                return JarvisResult.success(
                        record,
                        message
                );
            }

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.RECOVERY_FAILED,
                            message
                    )
            );

        } catch (SecurityException e) {

            RecoveryRecord record =
                    new RecoveryRecord(
                            spec.getCapabilityId(),
                            RecoveryStatus.RECOVERY_FAILED,
                            0,
                            Collections.emptyList(),
                            "Recovery blocked by security boundary.",
                            Collections.singletonList(
                                    e.getMessage()
                            )
                    );

            saveRecord(record);

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.RECOVERY_FAILED,
                            record.getMessage()
                    )
            );

        } catch (Exception e) {

            RecoveryRecord record =
                    new RecoveryRecord(
                            spec.getCapabilityId(),
                            RecoveryStatus.RECOVERY_FAILED,
                            0,
                            Collections.emptyList(),
                            "Recovery failed: "
                                    + e.getMessage(),
                            Collections.singletonList(
                                    e.getMessage()
                            )
                    );

            saveRecord(record);

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.RECOVERY_FAILED,
                            e
                    )
            );
        }
    }

    /**
     * يتحقق أن BuildResult فعلاً تابع للـWorkspace
     * الذي يديره Recovery.
     */
    private void validateBuildResult(
            SelfBuilder.BuildResult result
    ) throws Exception {

        File directory =
                result.getCapabilityDirectory();

        if (directory == null) {
            throw new SecurityException(
                    "Capability directory is null."
            );
        }

        if (!isInsideWorkspace(directory)) {
            throw new SecurityException(
                    "Capability directory is outside workspace."
            );
        }

        if (isProtectedPath(directory)) {
            throw new SecurityException(
                    "Capability directory is protected."
            );
        }

        List<File> files =
                result.getCreatedFiles();

        if (files == null) {
            throw new SecurityException(
                    "Created files list is null."
            );
        }

        for (File file : files) {

            if (file == null) {
                continue;
            }

            if (!isInsideWorkspace(file)) {
                throw new SecurityException(
                        "Build artifact is outside workspace: "
                                + file.getAbsolutePath()
                );
            }

            if (isProtectedPath(file)) {
                throw new SecurityException(
                        "Build artifact is protected: "
                                + file.getAbsolutePath()
                );
            }
        }
    }

    /**
     * حذف آمن.
     *
     * Recovery لا يحذف مجلدات عشوائية.
     * إذا كان المسار مجلداً، يجب أن يكون فارغاً
     * أو يحتوي فقط على artifacts تابعة للـBuild.
     */
    private boolean deleteRecursivelyIfSafe(
            File file
    ) {

        if (file == null) {
            return false;
        }

        if (!isInsideWorkspace(file)) {
            return false;
        }

        if (isProtectedPath(file)) {
            return false;
        }

        if (!file.exists()) {
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children == null) {
                return false;
            }

            /*
             * نحذف محتويات المجلد فقط إذا كانت
             * داخل Workspace وغير محمية.
             */
            for (File child : children) {

                if (!isInsideWorkspace(child)
                        || isProtectedPath(child)) {

                    return false;
                }

                if (!deleteRecursivelyIfSafe(child)) {
                    return false;
                }
            }

            return file.delete();
        }

        return false;
    }

    private boolean isDirectoryEmpty(
            File directory
    ) {
        if (directory == null
                || !directory.isDirectory()) {
            return false;
        }

        File[] children =
                directory.listFiles();

        return children != null
                && children.length == 0;
    }

    /**
     * يتحقق من أن الملف داخل Workspace فقط.
     */
    private boolean isInsideWorkspace(
            File file
    ) {
        try {
            File canonicalFile =
                    file.getCanonicalFile();

            String root =
                    workspaceRoot
                            .getCanonicalPath();

            String target =
                    canonicalFile
                            .getCanonicalPath();

            return target.equals(root)
                    || target.startsWith(
                    root + File.separator
            );

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * حدود الحماية.
     *
     * Recovery ممنوع يلمس:
     * security/
     * owner/
     * authorization/
     */
    private boolean isProtectedPath(
            File file
    ) {
        if (file == null) {
            return true;
        }

        try {
            String path =
                    file.getCanonicalPath()
                            .replace('\\', '/')
                            .toLowerCase();

            String root =
                    workspaceRoot
                            .getCanonicalPath()
                            .replace('\\', '/')
                            .toLowerCase();

            String security =
                    root + "/security";

            String owner =
                    root + "/owner";

            String authorization =
                    root + "/authorization";

            return path.equals(security)
                    || path.startsWith(
                    security + "/")
                    || path.equals(owner)
                    || path.startsWith(
                    owner + "/")
                    || path.equals(authorization)
                    || path.startsWith(
                    authorization + "/");

        } catch (Exception e) {
            return true;
        }
    }

    private JarvisResult<RecoveryRecord> failure(
            String message
    ) {
        RecoveryRecord record =
                new RecoveryRecord(
                        "unknown",
                        RecoveryStatus.RECOVERY_FAILED,
                        0,
                        Collections.emptyList(),
                        message
                );

        saveRecord(record);

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.RECOVERY_FAILED,
                        message
                )
        );
    }

    private void saveRecord(
            RecoveryRecord record
    ) {
        lastRecovery = record;
        history.add(record);

        /*
         * نحافظ على حجم الذاكرة محدوداً.
         */
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public RecoveryRecord getLastRecovery() {
        return lastRecovery;
    }

    public List<RecoveryRecord> getHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(history)
        );
    }

    public int getRecoveryCount() {
        return history.size();
    }

    public boolean wasLastRecoverySuccessful() {
        return lastRecovery != null
                && (
                lastRecovery.getStatus()
                        == RecoveryStatus.RECOVERED
                        ||
                        lastRecovery.getStatus()
                                == RecoveryStatus.NOTHING_TO_RECOVER
        );
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
    }

    public static String getEngineId() {
        return ENGINE_ID;
    }

    public enum RecoveryStatus {
        RECOVERED,
        RECOVERY_FAILED,
        NOTHING_TO_RECOVER
    }

    /**
     * سجل Recovery كامل.
     */
    public static final class RecoveryRecord {

        private final String capabilityId;
        private final RecoveryStatus status;
        private final int removedCount;
        private final List<String> removedPaths;
        private final List<String> failedPaths;
        private final String message;
        private final long timestamp;

        private RecoveryRecord(
                String capabilityId,
                RecoveryStatus status,
                int removedCount,
                List<String> removedPaths,
                String message
        ) {
            this(
                    capabilityId,
                    status,
                    removedCount,
                    removedPaths,
                    message,
                    Collections.emptyList()
            );
        }

        private RecoveryRecord(
                String capabilityId,
                RecoveryStatus status,
                int removedCount,
                List<String> removedPaths,
                String message,
                List<String> failedPaths
        ) {
            this.capabilityId =
                    capabilityId;

            this.status =
                    status;

            this.removedCount =
                    removedCount;

            this.removedPaths =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    removedPaths == null
                                            ? Collections.emptyList()
                                            : removedPaths
                            )
                    );

            this.failedPaths =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    failedPaths == null
                                            ? Collections.emptyList()
                                            : failedPaths
                            )
                    );

            this.message =
                    message == null
                            ? ""
                            : message;

            this.timestamp =
                    System.currentTimeMillis();
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public RecoveryStatus getStatus() {
            return status;
        }

        public int getRemovedCount() {
            return removedCount;
        }

        public List<String> getRemovedPaths() {
            return removedPaths;
        }

        public List<String> getFailedPaths() {
            return failedPaths;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isSuccessful() {
            return status
                    == RecoveryStatus.RECOVERED
                    || status
                    == RecoveryStatus.NOTHING_TO_RECOVER;
        }

        @Override
        public String toString() {
            return "RecoveryRecord{" +
                    "capabilityId='" +
                    capabilityId + '\'' +
                    ", status=" +
                    status +
                    ", removedCount=" +
                    removedCount +
                    ", failedCount=" +
                    failedPaths.size() +
                    ", message='" +
                    message + '\'' +
                    '}';
        }
    }
}