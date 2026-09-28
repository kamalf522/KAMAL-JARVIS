package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Code Evolution Engine
 *
 * محرك تطور الكود.
 *
 * الوظيفة:
 *
 * 1. قراءة ملفات المشروع.
 * 2. التحقق من أن الملف مسموح بتعديله.
 * 3. إنشاء Backup قبل التعديل.
 * 4. كتابة النسخة الجديدة.
 * 5. التحقق من أن الكتابة تمت فعلاً.
 * 6. توفير Rollback للنسخة السابقة.
 *
 * مهم جداً:
 *
 * هذا المحرك لا يستطيع تجاوز Android أو GitHub
 * أو نظام الملفات خارج المساحة التي أعطيت له.
 *
 * كذلك لا يسمح لنفسه بتعديل:
 *
 * - Owner Identity
 * - Security Policy
 * - Authorization
 * - Evolution Security
 * - Recovery Security
 *
 * ولا يعتبر الكود "صحيحاً" لمجرد أنه تمت كتابته.
 * Build/Test Engine هو الذي يقرر لاحقاً هل التغيير
 * صالح فعلاً.
 */
public final class CodeEvolutionEngine {

    private static final String ENGINE_ID =
            "v2.code_evolution_engine";

    private final File workspaceRoot;
    private final OwnerSecurityBoundary securityBoundary;

    private final Map<String, BackupRecord>
            backups =
            new LinkedHashMap<>();

    public CodeEvolutionEngine(
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
     * تهيئة workspace.
     */
    public synchronized JarvisResult<Boolean>
    initialize() {

        try {

            if (!workspaceRoot.exists()) {

                if (!workspaceRoot.mkdirs()) {

                    return failure(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Could not create workspace."
                    );
                }
            }

            if (!workspaceRoot.isDirectory()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Workspace is not a directory."
                );
            }

            return JarvisResult.success(
                    true,
                    "Code evolution workspace is ready."
            );

        } catch (Exception e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Workspace initialization failed.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * قراءة ملف من داخل Workspace.
     */
    public synchronized JarvisResult<String>
    readFile(
            String relativePath
    ) {

        File file =
                resolveSafeFile(
                        relativePath
                );

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "File path is outside the allowed workspace or is protected."
            );
        }

        if (!file.exists()) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "File does not exist: "
                            + relativePath
            );
        }

        if (!file.isFile()) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Path is not a file: "
                            + relativePath
            );
        }

        try {

            String content =
                    Files.readString(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    );

            return JarvisResult.success(
                    content,
                    "File read successfully."
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Could not read file.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * كتابة/تحديث ملف.
     *
     * قبل الكتابة يتم إنشاء Backup.
     */
    public synchronized JarvisResult<ChangeRecord>
    writeFile(
            String relativePath,
            String newContent
    ) {

        if (relativePath == null ||
                relativePath.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File path cannot be empty."
            );
        }

        if (newContent == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File content cannot be null."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Security boundary is not active."
            );
        }

        String normalizedPath =
                normalizePath(
                        relativePath
                );

        if (isProtectedPath(
                normalizedPath
        )) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security file/path cannot be modified."
            );
        }

        JarvisResult<Boolean>
                authorization =
                securityBoundary
                        .authorizeEvolutionChange(
                                "CODE_FILE_MODIFICATION"
                        );

        if (authorization == null ||
                !authorization.isSuccess()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Evolution change was not authorized."
            );
        }

        File file =
                resolveSafeFile(
                        normalizedPath
                );

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Target path is outside the workspace."
            );
        }

        try {

            JarvisResult<Boolean>
                    initialized =
                    initialize();

            if (!initialized.isSuccess()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        initialized.getMessage()
                );
            }

            boolean existed =
                    file.exists();

            String previousContent =
                    existed && file.isFile()
                            ? Files.readString(
                                    file.toPath(),
                                    StandardCharsets.UTF_8
                            )
                            : null;

            File backupFile =
                    createBackup(
                            file,
                            normalizedPath,
                            previousContent,
                            existed
                    );

            String backupPath =
                    backupFile == null
                            ? ""
                            : getRelativePath(
                                    backupFile
                            );

            File parent =
                    file.getParentFile();

            if (parent != null &&
                    !parent.exists() &&
                    !parent.mkdirs()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Could not create parent directory."
                );
            }

            Files.writeString(
                    file.toPath(),
                    newContent,
                    StandardCharsets.UTF_8
            );

            /*
             * تحقق حقيقي من أن المحتوى المكتوب
             * يساوي المحتوى المطلوب.
             */
            String writtenContent =
                    Files.readString(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    );

            if (!newContent.equals(
                    writtenContent
            )) {

                /*
                 * إذا فشل التحقق، نحاول Rollback مباشرة.
                 */
                restoreBackup(
                        backupFile,
                        file,
                        previousContent,
                        existed
                );

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Written content verification failed; previous state was restored."
                );
            }

            ChangeRecord record =
                    new ChangeRecord(
                            normalizedPath,
                            existed,
                            backupPath,
                            newContent.length(),
                            "File changed successfully."
                    );

            backups.put(
                    normalizedPath,
                    new BackupRecord(
                            normalizedPath,
                            backupFile,
                            previousContent,
                            existed
                    )
            );

            return JarvisResult.success(
                    record,
                    record.getMessage()
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Code file modification failed.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * إنشاء ملف جديد فقط إذا لم يكن موجوداً.
     */
    public synchronized JarvisResult<ChangeRecord>
    createFile(
            String relativePath,
            String content
    ) {

        if (relativePath == null ||
                relativePath.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File path cannot be empty."
            );
        }

        File file =
                resolveSafeFile(
                        relativePath
                );

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Invalid or protected file path."
            );
        }

        if (file.exists()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "File already exists. Use writeFile for replacement."
            );
        }

        return writeFile(
                relativePath,
                content
        );
    }

    /**
     * حذف ملف من داخل Workspace.
     *
     * لا يسمح بحذف الملفات المحمية.
     */
    public synchronized JarvisResult<Boolean>
    deleteFile(
            String relativePath
    ) {

        if (relativePath == null ||
                relativePath.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File path cannot be empty."
            );
        }

        String normalizedPath =
                normalizePath(
                        relativePath
                );

        if (isProtectedPath(
                normalizedPath
        )) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security path cannot be deleted."
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
                                "CODE_FILE_DELETION"
                        );

        if (authorization == null ||
                !authorization.isSuccess()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "File deletion was not authorized."
            );
        }

        File file =
                resolveSafeFile(
                        normalizedPath
                );

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Invalid file path."
            );
        }

        if (!file.exists()) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "File does not exist."
            );
        }

        if (!file.isFile()) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Only files can be deleted by this operation."
            );
        }

        try {

            String previousContent =
                    Files.readString(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    );

            File backup =
                    createBackup(
                            file,
                            normalizedPath,
                            previousContent,
                            true
                    );

            if (!file.delete()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Could not delete file."
                );
            }

            backups.put(
                    normalizedPath,
                    new BackupRecord(
                            normalizedPath,
                            backup,
                            previousContent,
                            true
                    )
            );

            return JarvisResult.success(
                    true,
                    "File deleted and backed up."
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Could not backup/delete file.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * Rollback لآخر تغيير على ملف محدد.
     */
    public synchronized JarvisResult<Boolean>
    rollback(
            String relativePath
    ) {

        String normalizedPath =
                normalizePath(
                        relativePath
                );

        if (normalizedPath.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File path cannot be empty."
            );
        }

        if (isProtectedPath(
                normalizedPath
        )) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security path cannot be rolled back."
            );
        }

        BackupRecord backup =
                backups.get(
                        normalizedPath
                );

        if (backup == null) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "No rollback state exists for this file."
            );
        }

        File target =
                resolveSafeFile(
                        normalizedPath
                );

        if (target == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Rollback target is outside the workspace."
            );
        }

        try {

            restoreBackup(
                    backup.getBackupFile(),
                    target,
                    backup.getPreviousContent(),
                    backup.existedBeforeChange()
            );

            backups.remove(
                    normalizedPath
            );

            return JarvisResult.success(
                    true,
                    "File rollback completed."
            );

        } catch (IOException e) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.RECOVERY_FAILED,
                            "File rollback failed.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * Rollback لكل التغييرات المسجلة.
     */
    public synchronized JarvisResult<RollbackReport>
    rollbackAll() {

        List<String> restored =
                new ArrayList<>();

        List<String> failed =
                new ArrayList<>();

        List<String> paths =
                new ArrayList<>(
                        backups.keySet()
                );

        for (String path : paths) {

            JarvisResult<Boolean> result =
                    rollback(
                            path
                    );

            if (result != null &&
                    result.isSuccess()) {

                restored.add(
                        path
                );

            } else {

                failed.add(
                        path
                );
            }
        }

        RollbackReport report =
                new RollbackReport(
                        restored,
                        failed
                );

        if (!failed.isEmpty()) {

            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.RECOVERY_FAILED,
                            "Some code changes could not be rolled back.",
                            ENGINE_ID
                    ),
                    report.toString()
            );
        }

        return JarvisResult.success(
                report,
                "All recorded code changes were rolled back."
        );
    }

    /**
     * فحص هل الملف قابل للتعديل.
     */
    public boolean isModifiable(
            String relativePath
    ) {

        String normalized =
                normalizePath(
                        relativePath
                );

        if (normalized.isEmpty()) {
            return false;
        }

        if (isProtectedPath(
                normalized
        )) {
            return false;
        }

        return resolveSafeFile(
                normalized
        ) != null;
    }

    /**
     * الحصول على ملفات المشروع الموجودة.
     */
    public synchronized JarvisResult<List<String>>
    listFiles() {

        if (!workspaceRoot.exists() ||
                !workspaceRoot.isDirectory()) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Workspace does not exist."
            );
        }

        List<String> result =
                new ArrayList<>();

        collectFiles(
                workspaceRoot,
                result
        );

        Collections.sort(
                result
        );

        return JarvisResult.success(
                Collections.unmodifiableList(
                        result
                ),
                "Workspace files listed."
        );
    }

    /**
     * عدد التغييرات التي لها Rollback state.
     */
    public synchronized int getPendingRollbackCount() {
        return backups.size();
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

    /**
     * تحويل path إلى File آمن داخل Workspace.
     */
    private File resolveSafeFile(
            String relativePath
    ) {

        if (relativePath == null) {
            return null;
        }

        String normalized =
                normalizePath(
                        relativePath
                );

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.startsWith("../")
                || normalized.equals("..")
                || normalized.contains(
                "/../"
        )) {
            return null;
        }

        if (isProtectedPath(
                normalized
        )) {
            return null;
        }

        File file =
                new File(
                        workspaceRoot,
                        normalized
                );

        try {

            Path root =
                    workspaceRoot
                            .getCanonicalFile()
                            .toPath();

            Path target =
                    file.getCanonicalFile()
                            .toPath();

            if (!target.startsWith(
                    root
            )) {
                return null;
            }

            return file;

        } catch (IOException e) {

            return null;
        }
    }

    /**
     * حماية مسارات Security وOwner.
     */
    private boolean isProtectedPath(
            String relativePath
    ) {

        if (relativePath == null) {
            return true;
        }

        String path =
                normalizePath(
                        relativePath
                ).toLowerCase();

        if (path.isEmpty()) {
            return true;
        }

        /*
         * يمنع التعديل على أي ملف واضح
         * بأنه جزء من Security boundary.
         */
        String[] protectedTokens = {
                "ownersecurityboundary",
                "ownersecurity",
                "ownercontrol",
                "securityboundary",
                "security_policy",
                "security-policy",
                "authorization",
                "evolutionsecurity",
                "recoverysecurity"
        };

        for (String token :
                protectedTokens) {

            if (path.contains(token)) {
                return true;
            }
        }

        /*
         * يمنع أيضاً التعديل على مجلدات أمنية
         * بأسماء واضحة.
         */
        String[] protectedDirectories = {
                "/security/",
                "security/",
                "/owner/",
                "owner/"
        };

        for (String directory :
                protectedDirectories) {

            if (path.contains(directory)) {
                return true;
            }
        }

        return false;
    }

    private File createBackup(
            File original,
            String relativePath,
            String previousContent,
            boolean existed
    ) throws IOException {

        File backupDirectory =
                new File(
                        workspaceRoot,
                        ".jarvis_backups"
                );

        if (!backupDirectory.exists() &&
                !backupDirectory.mkdirs()) {

            throw new IOException(
                    "Could not create backup directory."
            );
        }

        String safeName =
                relativePath
                        .replace(
                                '/',
                                '_'
                        )
                        .replace(
                                '\\',
                                '_'
                        );

        File backupFile =
                new File(
                        backupDirectory,
                        System.nanoTime()
                                + "_"
                                + safeName
                );

        if (existed &&
                original.exists()) {

            Files.copy(
                    original.toPath(),
                    backupFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } else {

            Files.writeString(
                    backupFile.toPath(),
                    "",
                    StandardCharsets.UTF_8
            );
        }

        return backupFile;
    }

    private void restoreBackup(
            File backupFile,
            File target,
            String previousContent,
            boolean existed
    ) throws IOException {

        if (existed) {

            if (backupFile != null &&
                    backupFile.exists()) {

                File parent =
                        target.getParentFile();

                if (parent != null &&
                        !parent.exists() &&
                        !parent.mkdirs()) {

                    throw new IOException(
                            "Could not create rollback parent."
                    );
                }

                Files.copy(
                        backupFile.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );

            } else if (previousContent != null) {

                Files.writeString(
                        target.toPath(),
                        previousContent,
                        StandardCharsets.UTF_8
                );

            } else {

                throw new IOException(
                        "Rollback content is unavailable."
                );
            }

        } else {

            if (target.exists() &&
                    !target.delete()) {

                throw new IOException(
                        "Could not remove newly created file."
                );
            }
        }
    }

    private void collectFiles(
            File directory,
            List<String> result
    ) {

        File[] children =
                directory.listFiles();

        if (children == null) {
            return;
        }

        for (File child :
                children) {

            if (child == null) {
                continue;
            }

            String relative =
                    getRelativePath(
                            child
                    );

            if (child.isDirectory()) {

                if (".jarvis_backups".equals(
                        child.getName()
                )) {
                    continue;
                }

                collectFiles(
                        child,
                        result
                );

            } else {

                if (!isProtectedPath(
                        relative
                )) {

                    result.add(
                            relative
                    );
                }
            }
        }
    }

    private String getRelativePath(
            File file
    ) {

        try {

            Path root =
                    workspaceRoot
                            .getCanonicalFile()
                            .toPath();

            Path target =
                    file.getCanonicalFile()
                            .toPath();

            return root
                    .relativize(
                            target
                    )
                    .toString()
                    .replace(
                            File.separatorChar,
                            '/'
                    );

        } catch (IOException e) {

            return file.getName();
        }
    }

    private String normalizePath(
            String path
    ) {

        if (path == null) {
            return "";
        }

        return path
                .trim()
                .replace(
                        '\\',
                        '/'
                )
                .replaceAll(
                        "/+",
                        "/"
                );
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

    /**
     * معلومات آخر نسخة احتياطية.
     */
    public static final class BackupRecord {

        private final String relativePath;
        private final File backupFile;
        private final String previousContent;
        private final boolean existedBeforeChange;

        private BackupRecord(
                String relativePath,
                File backupFile,
                String previousContent,
                boolean existedBeforeChange
        ) {

            this.relativePath =
                    relativePath;

            this.backupFile =
                    backupFile;

            this.previousContent =
                    previousContent;

            this.existedBeforeChange =
                    existedBeforeChange;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public File getBackupFile() {
            return backupFile;
        }

        public String getPreviousContent() {
            return previousContent;
        }

        public boolean existedBeforeChange() {
            return existedBeforeChange;
        }
    }

    /**
     * نتيجة تغيير كود.
     */
    public static final class ChangeRecord {

        private final String relativePath;
        private final boolean replacedExistingFile;
        private final String backupPath;
        private final int contentLength;
        private final String message;

        private ChangeRecord(
                String relativePath,
                boolean replacedExistingFile,
                String backupPath,
                int contentLength,
                String message
        ) {

            this.relativePath =
                    relativePath;

            this.replacedExistingFile =
                    replacedExistingFile;

            this.backupPath =
                    backupPath;

            this.contentLength =
                    contentLength;

            this.message =
                    message;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public boolean isReplacement() {
            return replacedExistingFile;
        }

        public String getBackupPath() {
            return backupPath;
        }

        public int getContentLength() {
            return contentLength;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * تقرير Rollback.
     */
    public static final class RollbackReport {

        private final List<String> restored;
        private final List<String> failed;

        private RollbackReport(
                List<String> restored,
                List<String> failed
        ) {

            this.restored =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    restored
                            )
                    );

            this.failed =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    failed
                            )
                    );
        }

        public List<String> getRestored() {
            return restored;
        }

        public List<String> getFailed() {
            return failed;
        }

        public boolean isSuccessful() {
            return failed.isEmpty();
        }

        @Override
        public String toString() {

            return "RollbackReport{" +
                    "restored=" +
                    restored +
                    ", failed=" +
                    failed +
                    '}';
        }
    }
}