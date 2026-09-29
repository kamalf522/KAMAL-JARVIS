package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Code Evolution Engine
 *
 * محرك تعديل الكود الحقيقي.
 *
 * المسؤوليات:
 * - قراءة ملفات المشروع الحقيقي.
 * - إنشاء ملفات.
 * - تعديل ملفات.
 * - حذف ملفات.
 * - إنشاء Backup قبل التعديل.
 * - Rollback حقيقي.
 * - منع الخروج من Workspace.
 * - منع تعديل ملفات Owner/Security.
 *
 * مهم:
 * هذا المحرك لا يعتبر التعديل ناجحاً لمجرد أن الملف كتب.
 * BuildEngine و SelfTestEngine و EvolutionVerificationEngine
 * هم المسؤولون عن إثبات أن التغيير صالح.
 */
public final class CodeEvolutionEngine {

    private static final String ENGINE_ID =
            "v2.code_evolution_engine";

    /*
     * Legacy/runtime workspace.
     *
     * يبقى فقط للتوافق مع الملفات القديمة.
     * النسخة الجديدة من JarvisSystem ستستعمل
     * ProjectWorkspaceManager حتى تكون عمليات
     * تطور الكود على المشروع الحقيقي.
     */
    private final File legacyWorkspaceRoot;

    /*
     * المشروع الحقيقي.
     *
     * عندما يكون موجوداً ومهيأً:
     * جميع عمليات source evolution تستعمله.
     *
     * عندما لا يكون مهيأً:
     * لا يتم استعمال runtime workspace كبديل صامت.
     */
    private final ProjectWorkspaceManager projectWorkspaceManager;

    private final OwnerSecurityBoundary securityBoundary;

    private final Map<String, BackupRecord> backups =
            new LinkedHashMap<>();

    /**
     * Constructor قديم للتوافق.
     *
     * ملاحظة:
     * هذا constructor يسمح باستعمال workspace محدد مباشرة.
     * لا يجب استعماله في النظام النهائي عندما يكون
     * ProjectWorkspaceManager متوفراً.
     */
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

        this.legacyWorkspaceRoot =
                workspaceRoot.getAbsoluteFile();

        this.projectWorkspaceManager =
                null;

        this.securityBoundary =
                securityBoundary;
    }

    /**
     * Constructor الجديد.
     *
     * يستعمل ProjectWorkspaceManager حتى لا يتم
     * الخلط بين runtime workspace والمشروع الحقيقي.
     */
    public CodeEvolutionEngine(
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

        this.legacyWorkspaceRoot =
                null;

        this.projectWorkspaceManager =
                projectWorkspaceManager;

        this.securityBoundary =
                securityBoundary;
    }

    /**
     * تهيئة المحرك.
     */
    public synchronized JarvisResult<Boolean>
    initialize() {

        File workspace =
                resolveWorkspace();

        if (workspace == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Real project workspace is not configured."
            );
        }

        try {

            if (!workspace.exists()) {

                return failure(
                        JarvisError.Type.NOT_FOUND,
                        "Evolution workspace does not exist."
                );
            }

            if (!workspace.isDirectory()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Evolution workspace is not a directory."
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
     * قراءة ملف حقيقي من المشروع.
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
                    "Invalid project path."
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
                            "Could not read project file.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * تعديل أو إنشاء ملف.
     *
     * يتم Backup قبل الكتابة.
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
                normalizePath(relativePath);

        if (normalizedPath.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Invalid file path."
            );
        }

        if (isProtectedPath(normalizedPath)) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security path cannot be modified."
            );
        }

        JarvisResult<Boolean> authorization =
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
                    "Target path is outside the project workspace."
            );
        }

        try {

            JarvisResult<Boolean> initialized =
                    initialize();

            if (!initialized.isSuccess()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        initialized.getMessage()
                );
            }

            boolean existed =
                    file.exists();

            if (existed &&
                    !file.isFile()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Target path is not a file."
                );
            }

            String previousContent =
                    existed
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

            String written =
                    Files.readString(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    );

            if (!newContent.equals(written)) {

                restoreBackup(
                        backupFile,
                        file,
                        previousContent,
                        existed
                );

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Written content verification failed. Previous state restored."
                );
            }

            BackupRecord backupRecord =
                    new BackupRecord(
                            normalizedPath,
                            backupFile,
                            previousContent,
                            existed
                    );

            backups.put(
                    normalizedPath,
                    backupRecord
            );

            ChangeRecord record =
                    new ChangeRecord(
                            normalizedPath,
                            existed,
                            backupFile == null
                                    ? ""
                                    : getRelativePath(
                                            backupFile
                                    ),
                            newContent.length(),
                            "File changed successfully."
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
     * إنشاء ملف جديد.
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
                resolveSafeFile(relativePath);

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Invalid project path."
            );
        }

        if (file.exists()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "File already exists. Use writeFile instead."
            );
        }

        return writeFile(
                relativePath,
                content
        );
    }

    /**
     * حذف ملف مع Backup.
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

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Security boundary is not active."
            );
        }

        String normalized =
                normalizePath(relativePath);

        if (isProtectedPath(normalized)) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security path cannot be deleted."
            );
        }

        JarvisResult<Boolean> authorization =
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
                resolveSafeFile(normalized);

        if (file == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Invalid project path."
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
                    "Only files can be deleted."
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
                            normalized,
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
                    normalized,
                    new BackupRecord(
                            normalized,
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
                            "Could not delete project file.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * Rollback لملف واحد.
     */
    public synchronized JarvisResult<Boolean>
    rollback(
            String relativePath
    ) {

        String normalized =
                normalizePath(relativePath);

        if (normalized.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "File path cannot be empty."
            );
        }

        if (isProtectedPath(normalized)) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Protected security path cannot be rolled back."
            );
        }

        BackupRecord backup =
                backups.get(normalized);

        if (backup == null) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "No rollback state exists for this file."
            );
        }

        File target =
                resolveSafeFile(normalized);

        if (target == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Rollback target is invalid."
            );
        }

        try {

            restoreBackup(
                    backup.getBackupFile(),
                    target,
                    backup.getPreviousContent(),
                    backup.existedBeforeChange()
            );

            backups.remove(normalized);

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
     * Rollback لكل التغييرات.
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

        Collections.reverse(paths);

        for (String path : paths) {

            JarvisResult<Boolean> result =
                    rollback(path);

            if (result != null &&
                    result.isSuccess()) {

                restored.add(path);

            } else {

                failed.add(path);
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
                            "Some changes could not be rolled back.",
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
     * هل الملف قابل للتعديل؟
     */
    public synchronized boolean
    isModifiable(
            String relativePath
    ) {

        String normalized =
                normalizePath(relativePath);

        if (normalized.isEmpty()) {
            return false;
        }

        if (isProtectedPath(normalized)) {
            return false;
        }

        return resolveSafeFile(normalized) != null;
    }

    /**
     * قائمة ملفات المشروع.
     */
    public synchronized JarvisResult<List<String>>
    listFiles() {

        File root =
                resolveWorkspace();

        if (root == null ||
                !root.isDirectory()) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Project workspace is not ready."
            );
        }

        List<String> result =
                new ArrayList<>();

        collectFiles(
                root,
                root,
                result
        );

        Collections.sort(result);

        return JarvisResult.success(
                result,
                "Project files listed successfully."
        );
    }

    public synchronized int
    getPendingRollbackCount() {
        return backups.size();
    }

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
     * تحديد Workspace.
     */
    private File resolveWorkspace() {

        /*
         * النظام الجديد:
         * ProjectWorkspaceManager هو المصدر الحقيقي.
         */
        if (projectWorkspaceManager != null) {

            if (!projectWorkspaceManager.isReady()) {
                return null;
            }

            File project =
                    projectWorkspaceManager
                            .getProjectRoot();

            if (project == null) {
                return null;
            }

            return project.getAbsoluteFile();
        }

        /*
         * التوافق مع constructor القديم.
         */
        if (legacyWorkspaceRoot != null) {
            return legacyWorkspaceRoot;
        }

        return null;
    }

    /**
     * Resolve آمن داخل المشروع.
     */
    private File resolveSafeFile(
            String relativePath
    ) {

        String normalized =
                normalizePath(relativePath);

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.startsWith("/") ||
                normalized.startsWith("\\") ||
                normalized.contains("\0") ||
                normalized.equals("..") ||
                normalized.startsWith("../") ||
                normalized.contains("/../")) {

            return null;
        }

        if (isProtectedPath(normalized)) {
            return null;
        }

        File root =
                resolveWorkspace();

        if (root == null ||
                !root.exists() ||
                !root.isDirectory()) {

            return null;
        }

        File target =
                new File(
                        root,
                        normalized
                );

        try {

            Path canonicalRoot =
                    root.getCanonicalFile()
                            .toPath();

            Path canonicalTarget =
                    target.getCanonicalFile()
                            .toPath();

            if (!canonicalTarget.startsWith(
                    canonicalRoot
            )) {
                return null;
            }

            return target;

        } catch (IOException e) {

            return null;
        }
    }

    /**
     * حماية Owner/Security.
     */
    private boolean isProtectedPath(
            String path
    ) {

        if (path == null) {
            return true;
        }

        String normalized =
                normalizePath(path)
                        .toLowerCase();

        if (normalized.isEmpty()) {
            return true;
        }

        /*
         * لا نسمح بتعديل ملفات الحماية
         * أو ملفات الأسرار/المفاتيح.
         */
        String[] protectedParts = {
                "ownersecurityboundary",
                "ownersecuritycore",
                "ownercontrolcore",
                "securityboundary",
                "securitypolicy",
                "authorization",
                "evolutionsecurity",
                "recoverysecurity",
                "keystore",
                "secret",
                ".git"
        };

        for (String protectedPart :
                protectedParts) {

            if (normalized.contains(
                    protectedPart
            )) {
                return true;
            }
        }

        /*
         * منع تعديل manifest الخاص
         * بالحماية إذا كان يحتوي على
         * إعدادات أمنية حساسة.
         *
         * AndroidManifest نفسه ليس محمياً
         * بالكامل؛ التعديل عليه يحتاج
         * مسار evolution خاص لاحقاً.
         */
        if (normalized.equals(
                "owner.json"
        ) ||
                normalized.equals(
                        "security.json"
                )) {

            return true;
        }

        return false;
    }

    /**
     * إنشاء Backup حقيقي.
     */
    private File createBackup(
            File original,
            String normalizedPath,
            String previousContent,
            boolean existed
    ) throws IOException {

        if (!existed ||
                previousContent == null) {

            return null;
        }

        File root =
                resolveWorkspace();

        if (root == null) {
            throw new IOException(
                    "Workspace is unavailable."
            );
        }

        File backupDirectory =
                new File(
                        root,
                        ".jarvis_backups"
                );

        if (!backupDirectory.exists() &&
                !backupDirectory.mkdirs()) {

            throw new IOException(
                    "Could not create backup directory."
            );
        }

        String safeName =
                normalizedPath
                        .replace(
                                '/',
                                '_'
                        )
                        .replace(
                                '\\',
                                '_'
                        );

        File backup =
                new File(
                        backupDirectory,
                        System.currentTimeMillis()
                                + "_"
                                + safeName
                );

        Files.writeString(
                backup.toPath(),
                previousContent,
                StandardCharsets.UTF_8
        );

        return backup;
    }

    /**
     * Restore Backup.
     */
    private void restoreBackup(
            File backupFile,
            File target,
            String previousContent,
            boolean existed
    ) throws IOException {

        if (existed) {

            if (backupFile != null &&
                    backupFile.isFile()) {

                String backupContent =
                        Files.readString(
                                backupFile.toPath(),
                                StandardCharsets.UTF_8
                        );

                File parent =
                        target.getParentFile();

                if (parent != null &&
                        !parent.exists() &&
                        !parent.mkdirs()) {

                    throw new IOException(
                            "Could not recreate parent directory."
                    );
                }

                Files.writeString(
                        target.toPath(),
                        backupContent,
                        StandardCharsets.UTF_8
                );

            } else if (previousContent != null) {

                File parent =
                        target.getParentFile();

                if (parent != null &&
                        !parent.exists() &&
                        !parent.mkdirs()) {

                    throw new IOException(
                            "Could not recreate parent directory."
                    );
                }

                Files.writeString(
                        target.toPath(),
                        previousContent,
                        StandardCharsets.UTF_8
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

    /**
     * الحصول على المسار النسبي للـBackup.
     */
    private String getRelativePath(
            File file
    ) {

        File root =
                resolveWorkspace();

        if (root == null ||
                file == null) {

            return "";
        }

        try {

            Path rootPath =
                    root.getCanonicalFile()
                            .toPath();

            Path filePath =
                    file.getCanonicalFile()
                            .toPath();

            if (!filePath.startsWith(
                    rootPath
            )) {

                return "";
            }

            return rootPath
                    .relativize(filePath)
                    .toString()
                    .replace(
                            '\\',
                            '/'
                    );

        } catch (IOException e) {

            return "";
        }
    }

    private void collectFiles(
            File root,
            File current,
            List<String> result
    ) {

        File[] children =
                current.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {

            /*
             * Backups ليست source files.
             */
            if (child.isDirectory() &&
                    child.getName().equals(
                            ".jarvis_backups"
                    )) {
                continue;
            }

            if (child.isDirectory()) {

                collectFiles(
                        root,
                        child,
                        result
                );

            } else {

                String relative =
                        getRelativePathFrom(
                                root,
                                child
                        );

                if (!relative.isEmpty()) {
                    result.add(relative);
                }
            }
        }
    }

    private String getRelativePathFrom(
            File root,
            File file
    ) {

        try {

            Path rootPath =
                    root.getCanonicalFile()
                            .toPath();

            Path filePath =
                    file.getCanonicalFile()
                            .toPath();

            if (!filePath.startsWith(
                    rootPath
            )) {
                return "";
            }

            return rootPath
                    .relativize(filePath)
                    .toString()
                    .replace(
                            '\\',
                            '/'
                    );

        } catch (IOException e) {

            return "";
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
                ),
                message
        );
    }

    // =========================================================
    // BackupRecord
    // =========================================================

    public static final class BackupRecord {

        private final String path;
        private final File backupFile;
        private final String previousContent;
        private final boolean existedBeforeChange;

        private BackupRecord(
                String path,
                File backupFile,
                String previousContent,
                boolean existedBeforeChange
        ) {

            this.path = path;
            this.backupFile = backupFile;
            this.previousContent = previousContent;
            this.existedBeforeChange =
                    existedBeforeChange;
        }

        public String getPath() {
            return path;
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

    // =========================================================
    // ChangeRecord
    // =========================================================

    public static final class ChangeRecord {

        private final String path;
        private final boolean existedBeforeChange;
        private final String backupPath;
        private final int contentLength;
        private final String message;

        private ChangeRecord(
                String path,
                boolean existedBeforeChange,
                String backupPath,
                int contentLength,
                String message
        ) {

            this.path = path;
            this.existedBeforeChange =
                    existedBeforeChange;
            this.backupPath =
                    backupPath == null
                            ? ""
                            : backupPath;
            this.contentLength =
                    contentLength;
            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public String getPath() {
            return path;
        }

        public boolean existedBeforeChange() {
            return existedBeforeChange;
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

    // =========================================================
    // RollbackReport
    // =========================================================

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
                                    restored == null
                                            ? Collections
                                                    .<String>
                                                    emptyList()
                                            : restored
                            )
                    );

            this.failed =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    failed == null
                                            ? Collections
                                                    .<String>
                                                    emptyList()
                                            : failed
                            )
                    );
        }

        public List<String> getRestored() {
            return restored;
        }

        public List<String> getFailed() {
            return failed;
        }

        public int getRestoredCount() {
            return restored.size();
        }

        public int getFailedCount() {
            return failed.size();
        }

        public boolean isSuccessful() {
            return failed.isEmpty();
        }

        @Override
        public String toString() {

            return "RollbackReport{" +
                    "restored=" +
                    restored.size() +
                    ", failed=" +
                    failed.size() +
                    '}';
        }
    }
}