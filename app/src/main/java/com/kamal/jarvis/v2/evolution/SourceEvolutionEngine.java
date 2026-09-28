package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JARVIS V2 - Source Evolution Engine
 *
 * طبقة تنفيذ تغييرات منظمة فوق CodeEvolutionEngine.
 *
 * المسؤوليات:
 *
 * 1. استقبال ChangeSet من نظام التطور.
 * 2. التحقق من صحة جميع التغييرات قبل التنفيذ.
 * 3. منع تكرار نفس الملف داخل ChangeSet واحد.
 * 4. تنفيذ التغييرات بالترتيب.
 * 5. الاعتماد على CodeEvolutionEngine للـ backup والحماية.
 * 6. Rollback للتغييرات التي تمت إذا فشلت العملية.
 * 7. عدم لمس ملفات Owner/Security المحمية.
 *
 * ملاحظة:
 * هذا المحرك لا "يخترع" الكود من نفسه.
 * هو ينفذ ChangeSet موثوق ومحدد من طبقة التخطيط/التطور.
 */
public final class SourceEvolutionEngine {

    private static final String ENGINE_ID =
            "v2.source_evolution_engine";

    private final CodeEvolutionEngine codeEvolutionEngine;

    private boolean initialized;

    private ChangeSet lastAppliedChangeSet;

    public SourceEvolutionEngine(
            CodeEvolutionEngine codeEvolutionEngine
    ) {
        if (codeEvolutionEngine == null) {
            throw new IllegalArgumentException(
                    "codeEvolutionEngine cannot be null."
            );
        }

        this.codeEvolutionEngine = codeEvolutionEngine;
        this.initialized = false;
    }

    /**
     * تهيئة المحرك.
     */
    public synchronized JarvisResult<Boolean> initialize() {

        if (initialized) {
            return JarvisResult.success(
                    true,
                    "Source evolution engine is already initialized."
            );
        }

        try {

            JarvisResult<Boolean> result =
                    codeEvolutionEngine.initialize();

            if (result == null) {

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Code evolution engine returned no initialization result."
                );
            }

            if (!result.isSuccess()) {

                return JarvisResult.failure(
                        result.getError(),
                        result.getMessage()
                );
            }

            initialized = true;

            return JarvisResult.success(
                    true,
                    "Source evolution engine initialized."
            );

        } catch (Exception e) {

            initialized = false;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Source evolution initialization failed.",
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * تطبيق ChangeSet كامل.
     *
     * إذا فشل أي تغيير:
     * - لا نكمل التغييرات اللاحقة.
     * - نحاول rollback لكل التغييرات التي نجحت.
     */
    public synchronized JarvisResult<ChangeResult> apply(
            ChangeSet changeSet
    ) {

        if (changeSet == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "ChangeSet cannot be null."
            );
        }

        JarvisResult<Boolean> validation =
                validateChangeSet(changeSet);

        if (!validation.isSuccess()) {

            return JarvisResult.failure(
                    validation.getError(),
                    validation.getMessage()
            );
        }

        if (!initialized) {

            JarvisResult<Boolean> init =
                    initialize();

            if (!init.isSuccess()) {

                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        long startedAt =
                System.currentTimeMillis();

        List<AppliedChange> appliedChanges =
                new ArrayList<>();

        List<String> errors =
                new ArrayList<>();

        for (FileChange change :
                changeSet.getChanges()) {

            JarvisResult<Boolean> result =
                    applySingleChange(change);

            if (result == null ||
                    !result.isSuccess()) {

                String message =
                        result == null
                                ? "Source change returned no result."
                                : result.getMessage();

                if (message == null ||
                        message.trim().isEmpty()) {

                    message =
                            "Source change failed for: "
                                    + change.getPath();
                }

                errors.add(message);

                return failAndRollback(
                        changeSet,
                        appliedChanges,
                        errors,
                        startedAt
                );
            }

            appliedChanges.add(
                    new AppliedChange(
                            change.getPath(),
                            change.getOperation()
                    )
            );
        }

        lastAppliedChangeSet =
                changeSet;

        ChangeResult result =
                new ChangeResult(
                        true,
                        true,
                        changeSet.getId(),
                        appliedChanges,
                        errors,
                        System.currentTimeMillis()
                                - startedAt,
                        "All source changes were applied successfully."
                );

        return JarvisResult.success(
                result,
                result.getMessage()
        );
    }

    /**
     * تنفيذ تغيير واحد.
     */
    private JarvisResult<Boolean> applySingleChange(
            FileChange change
    ) {

        if (change == null ||
                !change.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Invalid file change."
            );
        }

        try {

            switch (change.getOperation()) {

                case CREATE:

                    return convertToBoolean(
                            codeEvolutionEngine.createFile(
                                    change.getPath(),
                                    change.getContent()
                            )
                    );

                case WRITE:

                    return convertToBoolean(
                            codeEvolutionEngine.writeFile(
                                    change.getPath(),
                                    change.getContent()
                            )
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
                            "Source modification failed for: "
                                    + change.getPath(),
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * Rollback للتغييرات التي نجحت.
     *
     * مهم:
     * نستعمل rollback(String) الحقيقي الموجود
     * داخل CodeEvolutionEngine.
     */
    private JarvisResult<ChangeResult> failAndRollback(
            ChangeSet changeSet,
            List<AppliedChange> appliedChanges,
            List<String> errors,
            long startedAt
    ) {

        boolean rollbackSuccessful =
                true;

        /*
         * نرجع التغييرات بالعكس:
         *
         * A
         * B
         * C
         *
         * تصبح:
         *
         * C
         * B
         * A
         */
        for (int i =
                appliedChanges.size() - 1;
                i >= 0;
                i--) {

            AppliedChange applied =
                    appliedChanges.get(i);

            JarvisResult<Boolean> rollback =
                    codeEvolutionEngine.rollback(
                            applied.getPath()
                    );

            if (rollback == null ||
                    !rollback.isSuccess()) {

                rollbackSuccessful =
                        false;

                String message =
                        rollback == null
                                ? "Rollback returned no result for: "
                                    + applied.getPath()
                                : rollback.getMessage();

                if (message == null ||
                        message.trim().isEmpty()) {

                    message =
                            "Rollback failed for: "
                                    + applied.getPath();
                }

                errors.add(message);
            }
        }

        String message;

        if (rollbackSuccessful) {

            message =
                    "Source evolution failed and all applied changes were rolled back.";

        } else {

            message =
                    "Source evolution failed and rollback was incomplete.";
        }

        ChangeResult result =
                new ChangeResult(
                        false,
                        rollbackSuccessful,
                        changeSet.getId(),
                        appliedChanges,
                        errors,
                        System.currentTimeMillis()
                                - startedAt,
                        message
                );

        return JarvisResult.failure(
                JarvisError.of(
                        rollbackSuccessful
                                ? JarvisError.Type.EVOLUTION_FAILED
                                : JarvisError.Type.RECOVERY_FAILED,
                        message,
                        ENGINE_ID
                ),
                message
        );
    }

    /**
     * التحقق الكامل من ChangeSet قبل لمس أي ملف.
     */
    public synchronized JarvisResult<Boolean>
    validateChangeSet(
            ChangeSet changeSet
    ) {

        if (changeSet == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "ChangeSet cannot be null."
            );
        }

        if (!changeSet.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "ChangeSet is invalid."
            );
        }

        Set<String> paths =
                new HashSet<>();

        for (FileChange change :
                changeSet.getChanges()) {

            if (change == null ||
                    !change.isValid()) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "ChangeSet contains an invalid file change."
                );
            }

            String path =
                    normalizePath(
                            change.getPath()
                    );

            if (path.isEmpty()) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Change path cannot be empty."
                );
            }

            /*
             * نفس الملف لا يمكن تغييره مرتين
             * داخل نفس ChangeSet.
             *
             * هذا يمنع مشاكل الـ backup/rollback.
             */
            if (!paths.add(path)) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "The same file appears more than once in the same ChangeSet: "
                                + path
                );
            }

            if (!codeEvolutionEngine.isModifiable(
                    path
            )) {

                return failure(
                        JarvisError.Type.NOT_AUTHORIZED,
                        "File is not modifiable: "
                                + path
                );
            }

            /*
             * CREATE:
             * خاص الملف ما يكونش موجود.
             */
            if (change.getOperation() ==
                    FileChange.Operation.CREATE) {

                JarvisResult<String> existing =
                        codeEvolutionEngine.readFile(
                                path
                        );

                if (existing != null &&
                        existing.isSuccess()) {

                    return failure(
                            JarvisError.Type.VALIDATION_FAILED,
                            "CREATE requested for an existing file: "
                                    + path
                    );
                }
            }
        }

        return JarvisResult.success(
                true,
                "ChangeSet validation passed."
        );
    }

    /**
     * قراءة ملف.
     */
    public synchronized JarvisResult<String> read(
            String path
    ) {

        if (!initialized) {

            JarvisResult<Boolean> init =
                    initialize();

            if (!init.isSuccess()) {

                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        return codeEvolutionEngine.readFile(
                path
        );
    }

    /**
     * هل الملف قابل للتعديل؟
     */
    public synchronized boolean canModify(
            String path
    ) {

        if (!initialized) {

            return false;
        }

        return codeEvolutionEngine.isModifiable(
                path
        );
    }

    /**
     * Rollback لملف محدد.
     */
    public synchronized JarvisResult<Boolean>
    rollbackFile(
            String path
    ) {

        if (!initialized) {

            JarvisResult<Boolean> init =
                    initialize();

            if (!init.isSuccess()) {

                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        return codeEvolutionEngine.rollback(
                path
        );
    }

    /**
     * Rollback كامل.
     */
    public synchronized JarvisResult<
            CodeEvolutionEngine.RollbackReport>
    rollbackAll() {

        if (!initialized) {

            JarvisResult<Boolean> init =
                    initialize();

            if (!init.isSuccess()) {

                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        return codeEvolutionEngine.rollbackAll();
    }

    /**
     * ملفات الـworkspace.
     */
    public synchronized JarvisResult<List<String>>
    listFiles() {

        if (!initialized) {

            JarvisResult<Boolean> init =
                    initialize();

            if (!init.isSuccess()) {

                return JarvisResult.failure(
                        init.getError(),
                        init.getMessage()
                );
            }
        }

        return codeEvolutionEngine.listFiles();
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized ChangeSet
    getLastAppliedChangeSet() {
        return lastAppliedChangeSet;
    }

    public CodeEvolutionEngine
    getCodeEvolutionEngine() {
        return codeEvolutionEngine;
    }

    /**
     * تحويل نتيجة تغيير الملف إلى نتيجة Boolean.
     */
    private JarvisResult<Boolean> convertToBoolean(
            JarvisResult<?> result
    ) {

        if (result == null) {

            return failure(
                    JarvisError.Type.FILE_OPERATION_FAILED,
                    "Code evolution returned no result."
            );
        }

        if (!result.isSuccess()) {

            return JarvisResult.failure(
                    result.getError(),
                    result.getMessage()
            );
        }

        return JarvisResult.success(
                true,
                result.getMessage()
        );
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
                ),
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

            this.path =
                    normalize(
                            path
                    );

            this.operation =
                    operation;

            this.content =
                    content == null
                            ? ""
                            : content;
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

            if (path == null ||
                    path.isEmpty()) {

                return false;
            }

            if (path.startsWith("/") ||
                    path.startsWith("\\") ||
                    path.contains("../") ||
                    path.contains("..\\") ||
                    path.contains("\0")) {

                return false;
            }

            if (operation == null) {
                return false;
            }

            if ((operation ==
                    Operation.CREATE ||
                    operation ==
                    Operation.WRITE) &&
                    content == null) {

                return false;
            }

            return true;
        }

        private static String normalize(
                String value
        ) {

            if (value == null) {
                return "";
            }

            return value
                    .trim()
                    .replace(
                            '\\',
                            '/'
                    );
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

            this.id =
                    id == null
                            ? ""
                            : id.trim();

            this.reason =
                    reason == null
                            ? ""
                            : reason.trim();

            this.changes =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    changes
                            )
                    );

            this.metadata =
                    Collections.unmodifiableMap(
                            new LinkedHashMap<>(
                                    metadata
                            )
                    );
        }

        public static Builder builder(
                String id,
                String reason
        ) {

            return new Builder(
                    id,
                    reason
            );
        }

        public String getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }

        public List<FileChange>
        getChanges() {
            return changes;
        }

        public Map<String, String>
        getMetadata() {
            return metadata;
        }

        public boolean isValid() {

            if (id.isEmpty()) {
                return false;
            }

            if (changes.isEmpty()) {
                return false;
            }

            Set<String> paths =
                    new HashSet<>();

            for (FileChange change :
                    changes) {

                if (change == null ||
                        !change.isValid()) {

                    return false;
                }

                String path =
                        normalize(
                                change.getPath()
                        );

                if (path.isEmpty() ||
                        !paths.add(path)) {

                    return false;
                }
            }

            return true;
        }

        private static String normalize(
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

        public static final class Builder {

            private final String id;
            private final String reason;

            private final List<FileChange>
                    changes =
                    new ArrayList<>();

            private final Map<String, String>
                    metadata =
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
                        FileChange.create(
                                path,
                                content
                        )
                );

                return this;
            }

            public Builder write(
                    String path,
                    String content
            ) {

                changes.add(
                        FileChange.write(
                                path,
                                content
                        )
                );

                return this;
            }

            public Builder delete(
                    String path
            ) {

                changes.add(
                        FileChange.delete(
                                path
                        )
                );

                return this;
            }

            public Builder metadata(
                    String key,
                    String value
            ) {

                if (key != null &&
                        !key.trim().isEmpty()) {

                    metadata.put(
                            key.trim(),
                            value == null
                                    ? ""
                                    : value
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

        private AppliedChange(
                String path,
                FileChange.Operation operation
        ) {

            this.path = path;
            this.operation = operation;
        }

        public String getPath() {
            return path;
        }

        public FileChange.Operation
        getOperation() {
            return operation;
        }
    }

    // =========================================================
    // ChangeResult
    // =========================================================

    public static final class ChangeResult {

        private final boolean success;
        private final boolean rollbackSuccessful;
        private final String changeSetId;
        private final List<AppliedChange>
                appliedChanges;
        private final List<String> errors;
        private final long durationMs;
        private final String message;

        private ChangeResult(
                boolean success,
                boolean rollbackSuccessful,
                String changeSetId,
                List<AppliedChange> appliedChanges,
                List<String> errors,
                long durationMs,
                String message
        ) {

            this.success = success;

            this.rollbackSuccessful =
                    rollbackSuccessful;

            this.changeSetId =
                    changeSetId;

            this.appliedChanges =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    appliedChanges == null
                                            ? Collections
                                                    .<AppliedChange>
                                                    emptyList()
                                            : appliedChanges
                            )
                    );

            this.errors =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    errors == null
                                            ? Collections
                                                    .<String>
                                                    emptyList()
                                            : errors
                            )
                    );

            this.durationMs =
                    durationMs;

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isRollbackSuccessful() {
            return rollbackSuccessful;
        }

        public String getChangeSetId() {
            return changeSetId;
        }

        public List<AppliedChange>
        getAppliedChanges() {
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