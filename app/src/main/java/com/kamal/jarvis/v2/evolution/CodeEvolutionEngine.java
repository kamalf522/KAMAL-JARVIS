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
 * - قراءة ملفات المشروع.
 * - إنشاء وتعديل وحذف الملفات.
 * - Backup قبل التعديل.
 * - Rollback حقيقي.
 * - منع الخروج من Workspace.
 * - حماية Owner/Security.
 *
 * ملاحظة Android:
 * لا يستعمل Files.readString/writeString
 * حتى يبقى متوافقاً مع بيئة Android.
 */
public final class CodeEvolutionEngine {

    private static final String ENGINE_ID =
            "v2.code_evolution_engine";

    private final File legacyWorkspaceRoot;
    private final ProjectWorkspaceManager projectWorkspaceManager;
    private final OwnerSecurityBoundary securityBoundary;

    private final Map<String, BackupRecord> backups =
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

        this.legacyWorkspaceRoot =
                workspaceRoot.getAbsoluteFile();

        this.projectWorkspaceManager = null;
        this.securityBoundary = securityBoundary;
    }

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

        this.legacyWorkspaceRoot = null;
        this.projectWorkspaceManager =
                projectWorkspaceManager;
        this.securityBoundary = securityBoundary;
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    public synchronized JarvisResult<Boolean>
    initialize() {

        File workspace = resolveWorkspace();

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

    // =========================================================
    // READ
    // =========================================================

    public synchronized JarvisResult<String>
    readFile(
            String relativePath
    ) {

        File file =
                resolveSafeFile(relativePath);

        if (file == null) {
            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Invalid project path."
            );
        }

        if (!file.exists()) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "File does not exist: " + relativePath
            );
        }

        if (!file.isFile()) {
            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Path is not a file: " + relativePath
            );
        }

        try {

            String content =
                    readUtf8(file);

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

    // =========================================================
    // WRITE
    // =========================================================

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
                resolveSafeFile(normalizedPath);

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
                            ? readUtf8(file)
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

            writeUtf8(
                    file,
                    newContent
            );

            String written =
                    readUtf8(file);

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

    // =========================================================
    // CREATE
    // =========================================================

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

    // =========================================================
    // DELETE
    // =========================================================

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

        if (normalized.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Invalid file path."
            );
        }

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
                    readUtf8(file);

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

    // =========================================================
    // ROLLBACK
    // =========================================================

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

        if (report.isSuccessful()) {

            return JarvisResult.success(
                    report,
                    "All pending changes rolled back."
            );
        }

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.RECOVERY_FAILED,
                        "Some files could not be rolled back.",
                        ENGINE_ID
                ),
                report.toString()
        );
    }

    // =========================================================
    // INSPECTION
    // =========================================================

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

    public synchronized JarvisResult<List<String>>
    listFiles() {

        File root =
                resolveWorkspace();

        if (root == null) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Project workspace is not configured."
            );
        }

        if (!root.exists() ||
                !root.isDirectory()) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Project workspace is invalid."
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
                Collections.unmodifiableList(result),
                "Project files listed successfully."
        );
    }

    public synchronized int
    getPendingRollbackCount() {

        return backups.size();
    }

    public File
    getWorkspaceRoot() {

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

    public String
    getEngineId() {

        return ENGINE_ID;
    }

    // =========================================================
    // WORKSPACE
    // =========================================================

    private File
    resolveWorkspace() {

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

        if (legacyWorkspaceRoot == null) {
            return null;
        }

        return legacyWorkspaceRoot
                .getAbsoluteFile();
    }

    private File
    resolveSafeFile(
            String relativePath
    ) {

        String normalized =
                normalizePath(relativePath);

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.startsWith("/") ||
                normalized.contains(":")) {
            return null;
        }

        String[] parts =
                normalized.split("/");

        for (String part : parts) {

            if ("..".equals(part)) {
                return null;
            }
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

    // =========================================================
    // SECURITY
    // =========================================================

    private boolean
    isProtectedPath(
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

        return normalized.equals(
                "owner.json"
        ) ||
                normalized.equals(
                        "security.json"
                );
    }

    // =========================================================
    // BACKUP
    // =========================================================

    private File
    createBackup(
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
                        .replace('/', '_')
                        .replace('\\', '_');

        File backup =
                new File(
                        backupDirectory,
                        System.currentTimeMillis()
                                + "_"
                                + safeName
                );

        writeUtf8(
                backup,
                previousContent
        );

        return backup;
    }

    private void
    restoreBackup(
            File backupFile,
            File target,
            String previousContent,
            boolean existed
    ) throws IOException {

        if (existed) {

            String content = null;

            if (backupFile != null &&
                    backupFile.isFile()) {

                content =
                        readUtf8(backupFile);

            } else if (previousContent != null) {

                content =
                        previousContent;
            }

            if (content == null) {
                throw new IOException(
                        "No previous content available."
                );
            }

            File parent =
                    target.getParentFile();

            if (parent != null &&
                    !parent.exists() &&
                    !parent.mkdirs()) {

                throw new IOException(
                        "Could not recreate parent directory."
                );
            }

            writeUtf8(
                    target,
                    content
            );

        } else {

            if (target.exists() &&
                    !target.delete()) {

                throw new IOException(
                        "Could not remove newly created file."
                );
            }
        }
    }

    // =========================================================
    // FILE IO - ANDROID COMPATIBLE
    // =========================================================

    private String
    readUtf8(
            File file
    ) throws IOException {

        byte[] bytes =
                Files.readAllBytes(
                        file.toPath()
                );

        return new String(
                bytes,
                StandardCharsets.UTF_8
        );
    }

    private void
    writeUtf8(
            File file,
            String content
    ) throws IOException {

        File parent =
                file.getParentFile();

        if (parent != null &&
                !parent.exists() &&
                !parent.mkdirs()) {

            throw new IOException(
                    "Could not create parent directory."
            );
        }

        Files.write(
                file.toPath(),
                content.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    // =========================================================
    // PATH HELPERS
    // =========================================================

    private String
    getRelativePath(
            File file
    ) {

        File root =
                resolveWorkspace();

        if (root == null ||
                file == null) {

            return "";
        }

        return getRelativePathFrom(
                root,
                file
        );
    }

    private void
    collectFiles(
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

    private String
    getRelativePathFrom(
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

    private String
    normalizePath(
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

    // =========================================================
    // RESULT HELPER
    // =========================================================

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
    // BACKUP RECORD
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
    // CHANGE RECORD
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
    // ROLLBACK REPORT
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