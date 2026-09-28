package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SourceEvolutionEngine
 *
 * مسؤول عن تنفيذ مجموعة تغييرات منظمة على ملفات المشروع.
 *
 * لا يقوم بتوليد كود عشوائي، ولا يتجاوز OwnerSecurityBoundary.
 * يستعمل CodeEvolutionEngine لتنفيذ عمليات الملفات مع النسخ الاحتياطي
 * والـ rollback.
 */
public final class SourceEvolutionEngine {

    private final CodeEvolutionEngine codeEvolutionEngine;

    private boolean initialized;
    private ChangeSet lastAppliedChangeSet;

    public SourceEvolutionEngine(CodeEvolutionEngine codeEvolutionEngine) {
        if (codeEvolutionEngine == null) {
            throw new IllegalArgumentException(
                    "codeEvolutionEngine cannot be null"
            );
        }

        this.codeEvolutionEngine = codeEvolutionEngine;
        this.initialized = false;
    }

    /**
     * تهيئة المحرك.
     */
    public synchronized JarvisResult<Boolean> initialize() {
        try {
            JarvisResult<Boolean> result = codeEvolutionEngine.initialize();

            if (result == null || !result.isSuccess()) {
                if (result != null) {
                    return JarvisResult.failure(
                            result.getError(),
                            "Source evolution initialization failed: "
                                    + result.getMessage()
                    );
                }

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Code evolution engine returned no initialization result"
                );
            }

            initialized = true;

            return JarvisResult.success(
                    Boolean.TRUE,
                    "Source evolution engine initialized"
            );

        } catch (Exception e) {
            initialized = false;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Failed to initialize source evolution engine",
                            "SourceEvolutionEngine.initialize",
                            e
                    )
            );
        }
    }

    /**
     * تطبيق مجموعة تغييرات كاملة.
     *
     * إذا فشل تغيير واحد، يتم تنفيذ rollback لجميع التغييرات
     * التي تم تنفيذها داخل هذه العملية.
     */
    public synchronized JarvisResult<ChangeResult> apply(
            ChangeSet changeSet
    ) {
        if (changeSet == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "ChangeSet cannot be null"
            );
        }

        if (!changeSet.isValid()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "ChangeSet is invalid"
            );
        }

        if (!initialized) {
            JarvisResult<Boolean> init = initialize();

            if (!init.isSuccess()) {
                return failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        long startedAt = System.currentTimeMillis();

        List<AppliedChange> appliedChanges = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (FileChange change : changeSet.getChanges()) {

            if (change == null || !change.isValid()) {
                errors.add("Invalid file change");

                JarvisResult<ChangeResult> recovery =
                        rollbackAppliedChanges(
                                changeSet,
                                appliedChanges,
                                startedAt,
                                errors
                        );

                return recovery;
            }

            JarvisResult<Boolean> operation =
                    applyChange(change);

            if (!operation.isSuccess()) {

                String message = operation.getMessage();

                if (message == null || message.trim().isEmpty()) {
                    message = "Unknown source modification failure";
                }

                errors.add(message);

                return rollbackAppliedChanges(
                        changeSet,
                        appliedChanges,
                        startedAt,
                        errors
                );
            }

            appliedChanges.add(
                    new AppliedChange(
                            change.getPath(),
                            change.getOperation()
                    )
            );
        }

        lastAppliedChangeSet = changeSet;

        ChangeResult result = new ChangeResult(
                true,
                false,
                changeSet.getId(),
                appliedChanges,
                errors,
                System.currentTimeMillis() - startedAt,
                "Source changes applied successfully"
        );

        return JarvisResult.success(
                result,
                "Source evolution completed successfully"
        );
    }

    /**
     * تطبيق تغيير واحد.
     */
    private JarvisResult<Boolean> applyChange(
            FileChange change
    ) {
        try {
            switch (change.getOperation()) {

                case CREATE:
                    return codeEvolutionEngine.createFile(
                            change.getPath(),
                            change.getContent()
                    );

                case WRITE:
                    return codeEvolutionEngine.writeFile(
                            change.getPath(),
                            change.getContent()
                    );

                case DELETE:
                    return codeEvolutionEngine.deleteFile(
                            change.getPath()
                    );

                default:
                    return failure(
                            JarvisError.Type.INVALID_REQUEST,
                            "Unsupported source operation: "
                                    + change.getOperation()
                    );
            }

        } catch (Exception e) {
            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Source change failed for: "
                                    + change.getPath(),
                            "SourceEvolutionEngine.applyChange",
                            e
                    )
            );
        }
    }

    /**
     * Rollback لكل التغييرات التي نجحت قبل الفشل.
     */
    private JarvisResult<ChangeResult> rollbackAppliedChanges(
            ChangeSet changeSet,
            List<AppliedChange> appliedChanges,
            long startedAt,
            List<String> errors
    ) {
        boolean rollbackSuccessful = false;

        try {
            if (!appliedChanges.isEmpty()) {

                CodeEvolutionEngine.RollbackReport report =
                        codeEvolutionEngine.rollback();

                rollbackSuccessful =
                        report != null
                                && report.isSuccessful();

                if (!rollbackSuccessful) {
                    errors.add(
                            "Automatic rollback was not fully successful"
                    );
                }
            } else {
                rollbackSuccessful = true;
            }

        } catch (Exception e) {
            errors.add(
                    "Rollback exception: " + e.getMessage()
            );
        }

        ChangeResult result = new ChangeResult(
                false,
                rollbackSuccessful,
                changeSet.getId(),
                appliedChanges,
                errors,
                System.currentTimeMillis() - startedAt,
                rollbackSuccessful
                        ? "Source evolution failed and was rolled back"
                        : "Source evolution failed and rollback was incomplete"
        );

        return JarvisResult.failure(
                JarvisError.of(
                        JarvisError.Type.EVOLUTION_FAILED,
                        result.getMessage(),
                        "SourceEvolutionEngine.apply"
                ),
                result.getMessage()
        );
    }

    /**
     * Rollback مباشر لآخر عملية.
     */
    public synchronized JarvisResult<CodeEvolutionEngine.RollbackReport>
    rollbackLast() {

        if (!initialized) {
            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Source evolution engine is not initialized"
            );
        }

        try {
            CodeEvolutionEngine.RollbackReport report =
                    codeEvolutionEngine.rollback();

            if (report == null) {
                return failure(
                        JarvisError.Type.RECOVERY_FAILED,
                        "Rollback returned no report"
                );
            }

            if (!report.isSuccessful()) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.RECOVERY_FAILED,
                                "Rollback was not fully successful",
                                "SourceEvolutionEngine.rollbackLast"
                        )
                );
            }

            return JarvisResult.success(
                    report,
                    "Last source evolution rolled back successfully"
            );

        } catch (Exception e) {
            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.RECOVERY_FAILED,
                            "Failed to rollback source evolution",
                            "SourceEvolutionEngine.rollbackLast",
                            e
                    )
            );
        }
    }

    /**
     * Rollback لجميع التغييرات المسجلة.
     */
    public synchronized JarvisResult<CodeEvolutionEngine.RollbackReport>
    rollbackAll() {

        if (!initialized) {
            return failure(
                    JarvisError.Type.RECOVERY_FAILED,
                    "Source evolution engine is not initialized"
            );
        }

        try {
            CodeEvolutionEngine.RollbackReport report =
                    codeEvolutionEngine.rollbackAll();

            if (report == null) {
                return failure(
                        JarvisError.Type.RECOVERY_FAILED,
                        "Rollback returned no report"
                );
            }

            if (!report.isSuccessful()) {
                return JarvisResult.failure(
                        JarvisError.of(
                                JarvisError.Type.RECOVERY_FAILED,
                                "Full rollback was not successful",
                                "SourceEvolutionEngine.rollbackAll"
                        )
                );
            }

            return JarvisResult.success(
                    report,
                    "All source evolution changes rolled back"
            );

        } catch (Exception e) {
            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.RECOVERY_FAILED,
                            "Failed to rollback all source changes",
                            "SourceEvolutionEngine.rollbackAll",
                            e
                    )
            );
        }
    }

    /**
     * قراءة ملف من المشروع.
     */
    public synchronized JarvisResult<String> read(
            String path
    ) {
        if (!initialized) {
            JarvisResult<Boolean> init = initialize();

            if (!init.isSuccess()) {
                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        return codeEvolutionEngine.readFile(path);
    }

    /**
     * التأكد واش الملف قابل للتعديل.
     */
    public synchronized boolean canModify(
            String path
    ) {
        if (!initialized) {
            return false;
        }

        return codeEvolutionEngine.isModifiable(path);
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized ChangeSet getLastAppliedChangeSet() {
        return lastAppliedChangeSet;
    }

    public synchronized CodeEvolutionEngine getCodeEvolutionEngine() {
        return codeEvolutionEngine;
    }

    private static <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {
        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "SourceEvolutionEngine"
                ),
                message
        );
    }

    private static <T> JarvisResult<T> failure(
            JarvisError error,
            String message
    ) {
        if (error == null) {
            return failure(
                    JarvisError.Type.INTERNAL_ERROR,
                    message
            );
        }

        return JarvisResult.failure(
                error,
                message
        );
    }

    // =========================================================
    // FileChange
    // =========================================================

    public static final class FileChange {

        public enum Operation {
            CREATE,
            WRITE,
            DELETE
        }

        private final String path;
        private final Operation operation;
        private final String content;

        private FileChange(
                String path,
                Operation operation,
                String content
        ) {
            this.path = normalize(path);
            this.operation = operation;
            this.content = content == null ? "" : content;
        }

        public static FileChange create(
                String path,
                String content
        ) {
            return new FileChange(
                    path,
                    Operation.CREATE,
                    content
            );
        }

        public static FileChange write(
                String path,
                String content
        ) {
            return new FileChange(
                    path,
                    Operation.WRITE,
                    content
            );
        }

        public static FileChange delete(
                String path
        ) {
            return new FileChange(
                    path,
                    Operation.DELETE,
                    ""
            );
        }

        public String getPath() {
            return path;
        }

        public Operation getOperation() {
            return operation;
        }

        public String getContent() {
            return content;
        }

        public boolean isValid() {
            if (path == null || path.isEmpty()) {
                return false;
            }

            if (path.startsWith("/")
                    || path.startsWith("\\")
                    || path.contains("../")
                    || path.contains("..\\")
                    || path.contains("\0")) {
                return false;
            }

            if (operation == null) {
                return false;
            }

            if ((operation == Operation.CREATE
                    || operation == Operation.WRITE)
                    && content == null) {
                return false;
            }

            return true;
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }

            return value
                    .trim()
                    .replace('\\', '/');
        }
    }

    // =========================================================
    // ChangeSet
    // =========================================================

    public static final class ChangeSet {

        private final String id;
        private final String reason;
        private final List<FileChange> changes;
        private final Map<String, String> metadata;

        private ChangeSet(
                String id,
                String reason,
                List<FileChange> changes,
                Map<String, String> metadata
        ) {
            this.id = id;
            this.reason = reason == null ? "" : reason;

            this.changes = Collections.unmodifiableList(
                    new ArrayList<>(changes)
            );

            this.metadata = Collections.unmodifiableMap(
                    new LinkedHashMap<>(metadata)
            );
        }

        public static Builder builder(
                String id,
                String reason
        ) {
            return new Builder(id, reason);
        }

        public String getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }

        public List<FileChange> getChanges() {
            return changes;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public boolean isValid() {
            if (id == null || id.trim().isEmpty()) {
                return false;
            }

            if (changes.isEmpty()) {
                return false;
            }

            for (FileChange change : changes) {
                if (change == null || !change.isValid()) {
                    return false;
                }
            }

            return true;
        }

        public static final class Builder {

            private final String id;
            private final String reason;
            private final List<FileChange> changes =
                    new ArrayList<>();

            private final Map<String, String> metadata =
                    new LinkedHashMap<>();

            private Builder(
                    String id,
                    String reason
            ) {
                this.id = id;
                this.reason = reason;
            }

            public Builder add(
                    FileChange change
            ) {
                if (change != null) {
                    changes.add(change);
                }

                return this;
            }

            public Builder create(
                    String path,
                    String content
            ) {
                changes.add(
                        FileChange.create(path, content)
                );

                return this;
            }

            public Builder write(
                    String path,
                    String content
            ) {
                changes.add(
                        FileChange.write(path, content)
                );

                return this;
            }

            public Builder delete(
                    String path
            ) {
                changes.add(
                        FileChange.delete(path)
                );

                return this;
            }

            public Builder metadata(
                    String key,
                    String value
            ) {
                if (key != null && !key.trim().isEmpty()) {
                    metadata.put(
                            key.trim(),
                            value == null ? "" : value
                    );
                }

                return this;
            }

            public ChangeSet build() {
                return new ChangeSet(
                        id,
                        reason,
                        changes,
                        metadata
                );
            }
        }
    }

    // =========================================================
    // AppliedChange
    // =========================================================

    public static final class AppliedChange {

        private final String path;
        private final FileChange.Operation operation;

        public AppliedChange(
                String path,
                FileChange.Operation operation
        ) {
            this.path = path;
            this.operation = operation;
        }

        public String getPath() {
            return path;
        }

        public FileChange.Operation getOperation() {
            return operation;
        }
    }

    // =========================================================
    // ChangeResult
    // =========================================================

    public static final class ChangeResult {

        private final boolean success;
        private final boolean rolledBack;
        private final String changeSetId;
        private final List<AppliedChange> appliedChanges;
        private final List<String> errors;
        private final long durationMs;
        private final String message;

        public ChangeResult(
                boolean success,
                boolean rolledBack,
                String changeSetId,
                List<AppliedChange> appliedChanges,
                List<String> errors,
                long durationMs,
                String message
        ) {
            this.success = success;
            this.rolledBack = rolledBack;
            this.changeSetId = changeSetId;

            this.appliedChanges = Collections.unmodifiableList(
                    new ArrayList<>(
                            appliedChanges == null
                                    ? Collections.<AppliedChange>emptyList()
                                    : appliedChanges
                    )
            );

            this.errors = Collections.unmodifiableList(
                    new ArrayList<>(
                            errors == null
                                    ? Collections.<String>emptyList()
                                    : errors
                    )
            );

            this.durationMs = durationMs;
            this.message = message == null ? "" : message;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isRolledBack() {
            return rolledBack;
        }

        public String getChangeSetId() {
            return changeSetId;
        }

        public List<AppliedChange> getAppliedChanges() {
            return appliedChanges;
        }

        public List<String> getErrors() {
            return errors;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getMessage() {
            return message;
        }

        public int getAppliedChangeCount() {
            return appliedChanges.size();
        }
    }
}