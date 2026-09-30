package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JARVIS V2 - Code Generation Engine
 *
 * مسؤول عن طبقة توليد الكود قبل الكتابة الفعلية للمشروع.
 *
 * المسار:
 *
 * GenerationRequest
 *      ↓
 * Provider Selection
 *      ↓
 * AI Code Generator
 *      ↓
 * GeneratedCode
 *      ↓
 * Structural Validation
 *      ↓
 * ChangeSet
 *      ↓
 * SourceEvolutionEngine
 *      ↓
 * CodeEvolutionEngine
 *
 * هذا المحرك لا يكتب مباشرة إلى ملفات المشروع.
 *
 * يمكن ربطه بعدة مصادر توليد:
 * - Local AI
 * - Remote AI
 * - Open-source model
 * - Future JARVIS native generator
 *
 * الهدف هو عدم ربط JARVIS بمصدر واحد.
 */
public final class CodeGenerationEngine {

    private static final String ENGINE_ID =
            "v2.code_generation_engine";

    private final OwnerSecurityBoundary securityBoundary;

    private final Map<String, CodeGeneratorProvider> providers =
            new LinkedHashMap<>();

    private volatile String preferredProviderId;

    private volatile GenerationRecord lastRecord;

    public CodeGenerationEngine(
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
     * إضافة مصدر توليد.
     */
    public synchronized JarvisResult<Void> registerProvider(
            CodeGeneratorProvider provider
    ) {

        if (provider == null) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Provider cannot be null."
            );
        }

        String id =
                normalize(
                        provider.getId()
                );

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Provider ID cannot be empty."
            );
        }

        if (!provider.isConfigured()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Provider is not configured: " + id
            );
        }

        providers.put(
                id,
                provider
        );

        if (preferredProviderId == null) {
            preferredProviderId = id;
        }

        return JarvisResult.success(
                null,
                "Code generation provider registered: " + id
        );
    }

    /**
     * حذف مصدر توليد.
     */
    public synchronized JarvisResult<Void> removeProvider(
            String providerId
    ) {

        String id =
                normalize(providerId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Provider ID cannot be empty."
            );
        }

        if (providers.remove(id) == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Provider not found: " + id
            );
        }

        if (id.equals(preferredProviderId)) {

            preferredProviderId =
                    providers.isEmpty()
                            ? null
                            : providers.keySet()
                                    .iterator()
                                    .next();
        }

        return JarvisResult.success(
                null,
                "Provider removed: " + id
        );
    }

    /**
     * تحديد المصدر المفضل.
     */
    public synchronized JarvisResult<Void> setPreferredProvider(
            String providerId
    ) {

        String id =
                normalize(providerId);

        CodeGeneratorProvider provider =
                providers.get(id);

        if (provider == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Provider not found: " + id
            );
        }

        if (!provider.isAvailable()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Provider is not available: " + id
            );
        }

        preferredProviderId = id;

        return JarvisResult.success(
                null,
                "Preferred provider selected: " + id
        );
    }

    /**
     * اختيار أفضل مصدر متوفر.
     */
    private CodeGeneratorProvider selectProvider(
            GenerationRequest request
    ) {

        /*
         * 1. المصدر المطلوب في الطلب.
         */
        if (request != null &&
                !request.getPreferredProviderId().isEmpty()) {

            CodeGeneratorProvider requested =
                    providers.get(
                            request.getPreferredProviderId()
                    );

            if (requested != null &&
                    requested.isAvailable()) {

                return requested;
            }
        }

        /*
         * 2. المصدر المفضل.
         */
        if (preferredProviderId != null) {

            CodeGeneratorProvider preferred =
                    providers.get(
                            preferredProviderId
                    );

            if (preferred != null &&
                    preferred.isAvailable()) {

                return preferred;
            }
        }

        /*
         * 3. أعلى أولوية بين المصادر المتوفرة.
         */
        CodeGeneratorProvider best =
                null;

        for (CodeGeneratorProvider provider :
                providers.values()) {

            if (provider == null ||
                    !provider.isAvailable()) {
                continue;
            }

            if (best == null ||
                    provider.getPriority() >
                            best.getPriority()) {

                best = provider;
            }
        }

        return best;
    }

    /**
     * توليد الكود.
     *
     * لا توجد كتابة للمشروع هنا.
     */
    public synchronized JarvisResult<GeneratedCode> generate(
            GenerationRequest request
    ) {

        if (request == null ||
                !request.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generation request is invalid."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Owner security boundary is not active."
            );
        }

        CodeGeneratorProvider provider =
                selectProvider(request);

        if (provider == null) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No code generation provider is currently available."
            );
        }

        long startedAt =
                System.currentTimeMillis();

        try {

            JarvisResult<GeneratedCode> result =
                    provider.generate(request);

            if (result == null ||
                    !result.isSuccess()) {

                String message =
                        result == null
                                ? "Provider returned no result."
                                : result.getMessage();

                saveRecord(
                        new GenerationRecord(
                                request.getCapabilityId(),
                                provider.getId(),
                                false,
                                message,
                                startedAt,
                                System.currentTimeMillis()
                        )
                );

                if (result == null) {
                    return failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            message
                    );
                }

                return result;
            }

            GeneratedCode generated =
                    result.getData();

            JarvisResult<Void> validation =
                    validateGeneratedCode(
                            request,
                            generated
                    );

            if (validation == null ||
                    !validation.isSuccess()) {

                String message =
                        validation == null
                                ? "Generated code validation failed."
                                : validation.getMessage();

                saveRecord(
                        new GenerationRecord(
                                request.getCapabilityId(),
                                provider.getId(),
                                false,
                                message,
                                startedAt,
                                System.currentTimeMillis()
                        )
                );

                return failure(
                        validation == null ||
                                validation.getError() == null
                                ? JarvisError.Type.VALIDATION_FAILED
                                : validation.getError().getType(),
                        message
                );
            }

            saveRecord(
                    new GenerationRecord(
                            request.getCapabilityId(),
                            provider.getId(),
                            true,
                            "Code generated and structurally validated.",
                            startedAt,
                            System.currentTimeMillis()
                    )
            );

            return JarvisResult.success(
                    generated,
                    "Code generated and structurally validated."
            );

        } catch (Exception e) {

            String message =
                    "Code generation failed: "
                            + safeMessage(e);

            saveRecord(
                    new GenerationRecord(
                            request.getCapabilityId(),
                            provider.getId(),
                            false,
                            message,
                            startedAt,
                            System.currentTimeMillis()
                    )
            );

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.EVOLUTION_FAILED,
                            message,
                            ENGINE_ID,
                            e
                    )
            );
        }
    }

    /**
     * تحويل الكود المولد إلى ChangeSet.
     *
     * لا تتم الكتابة هنا.
     */
    public JarvisResult<SourceEvolutionEngine.ChangeSet>
    createChangeSet(
            GenerationRequest request,
            GeneratedCode generatedCode
    ) {

        if (request == null ||
                !request.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generation request is invalid."
            );
        }

        JarvisResult<Void> validation =
                validateGeneratedCode(
                        request,
                        generatedCode
                );

        if (validation == null ||
                !validation.isSuccess()) {

            return failure(
                    validation == null ||
                            validation.getError() == null
                            ? JarvisError.Type.VALIDATION_FAILED
                            : validation.getError().getType(),
                    validation == null
                            ? "Generated code validation failed."
                            : validation.getMessage()
            );
        }

        SourceEvolutionEngine.ChangeSet.Builder builder =
                SourceEvolutionEngine.ChangeSet.builder(
                        buildChangeSetId(request),
                        request.getReason()
                );

        for (GeneratedFile file :
                generatedCode.getFiles()) {

            if (file == null) {
                continue;
            }

            SourceEvolutionEngine.FileChange.Operation operation =
                    file.getOperation();

            if (operation ==
                    SourceEvolutionEngine.FileChange.Operation.CREATE) {

                builder.create(
                        file.getPath(),
                        file.getContent()
                );

            } else if (operation ==
                    SourceEvolutionEngine.FileChange.Operation.WRITE) {

                builder.write(
                        file.getPath(),
                        file.getContent()
                );

            } else if (operation ==
                    SourceEvolutionEngine.FileChange.Operation.DELETE) {

                builder.delete(
                        file.getPath()
                );
            }
        }

        builder.metadata(
                "generator",
                generatedCode.getProviderId()
        );

        builder.metadata(
                "generation_id",
                generatedCode.getGenerationId()
        );

        builder.metadata(
                "capability_id",
                request.getCapabilityId()
        );

        builder.metadata(
                "validated",
                true
        );

        SourceEvolutionEngine.ChangeSet changeSet =
                builder.build();

        if (changeSet == null ||
                !changeSet.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generated ChangeSet is invalid."
            );
        }

        return JarvisResult.success(
                changeSet,
                "Validated ChangeSet created."
        );
    }

    /**
     * التحقق البنيوي للكود المولد.
     */
    public JarvisResult<Void> validateGeneratedCode(
            GenerationRequest request,
            GeneratedCode generatedCode
    ) {

        if (generatedCode == null) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generated code cannot be null."
            );
        }

        if (generatedCode.getGenerationId().isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generation ID cannot be empty."
            );
        }

        if (generatedCode.getProviderId().isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Provider ID cannot be empty."
            );
        }

        if (generatedCode.getFiles().isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generated code contains no files."
            );
        }

        Set<String> paths =
                new LinkedHashSet<>();

        for (GeneratedFile file :
                generatedCode.getFiles()) {

            if (file == null ||
                    !file.isValid()) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Generated file is invalid."
                );
            }

            String path =
                    normalizePath(
                            file.getPath()
                    );

            if (!paths.add(path)) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Duplicate generated path: " + path
                );
            }

            if (isProtectedPath(path)) {

                return failure(
                        JarvisError.Type.NOT_AUTHORIZED,
                        "Generated code targets a protected path: "
                                + path
                );
            }

            if (file.getOperation() !=
                    SourceEvolutionEngine.FileChange.Operation.DELETE &&
                    file.getContent().trim().isEmpty()) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Generated source is empty: " + path
                );
            }
        }

        /*
         * إذا كان الطلب محدداً بمسارات مسموحة،
         * لا يسمح للكود بالخروج منها.
         */
        if (request != null &&
                !request.getAllowedPaths().isEmpty()) {

            for (String path : paths) {

                if (!isAllowedPath(
                        path,
                        request.getAllowedPaths()
                )) {

                    return failure(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Generated path is outside allowed scope: "
                                    + path
                    );
                }
            }
        }

        return JarvisResult.success(
                null,
                "Generated code passed structural validation."
        );
    }

    /**
     * التحقق من أن المسار داخل المجال المسموح.
     */
    private boolean isAllowedPath(
            String path,
            Set<String> allowedPaths
    ) {

        for (String allowed :
                allowedPaths) {

            String normalized =
                    normalizePath(allowed);

            if (path.equals(normalized) ||
                    path.startsWith(
                            normalized + "/"
                    )) {

                return true;
            }
        }

        return false;
    }

    /**
     * حماية الملفات الحساسة.
     *
     * Security Boundary لا يمكن للكود المولد
     * أن يعدله مباشرة.
     */
    private boolean isProtectedPath(
            String path
    ) {

        String lower =
                normalizePath(path)
                        .toLowerCase();

        String[] protectedFragments = {

                "security/",

                "/security/",

                "ownersecurityboundary",

                "owner_security",

                "androidmanifest.xml"
        };

        for (String fragment :
                protectedFragments) {

            if (lower.contains(fragment)) {
                return true;
            }
        }

        return false;
    }

    /**
     * إنشاء ID فريد للتغيير.
     */
    private String buildChangeSetId(
            GenerationRequest request
    ) {

        String seed =
                request.getCapabilityId()
                        + "|"
                        + request.getGoal()
                        + "|"
                        + request.getReason()
                        + "|"
                        + System.currentTimeMillis();

        return "generated_"
                + sha256(seed)
                .substring(0, 16);
    }

    private String sha256(
            String value
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] bytes =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder builder =
                    new StringBuilder();

            for (byte b : bytes) {

                builder.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return builder.toString();

        } catch (Exception e) {

            return Integer.toHexString(
                    value.hashCode()
            );
        }
    }

    private void saveRecord(
            GenerationRecord record
    ) {

        lastRecord =
                record;
    }

    private String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value.trim()
                        .toLowerCase();
    }

    private String normalizePath(
            String path
    ) {

        if (path == null) {
            return "";
        }

        return path
                .trim()
                .replace('\\', '/')
                .replaceAll(
                        "^\\./+",
                        ""
                )
                .replaceAll(
                        "/+",
                        "/"
                );
    }

    private String safeMessage(
            Exception e
    ) {

        if (e == null ||
                e.getMessage() == null) {

            return "unknown error";
        }

        return e.getMessage();
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

    public synchronized List<String> getProviderIds() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        providers.keySet()
                )
        );
    }

    public synchronized int getProviderCount() {
        return providers.size();
    }

    public String getPreferredProviderId() {
        return preferredProviderId;
    }

    public GenerationRecord getLastRecord() {
        return lastRecord;
    }

    public OwnerSecurityBoundary getSecurityBoundary() {
        return securityBoundary;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * عقد أي مصدر توليد.
     *
     * يمكن لاحقاً ربط:
     *
     * Local AI
     * Remote AI
     * Open-source model
     * JARVIS Native Generator
     */
    public interface CodeGeneratorProvider {

        String getId();

        String getName();

        int getPriority();

        boolean isConfigured();

        boolean isAvailable();

        JarvisResult<GeneratedCode> generate(
                GenerationRequest request
        );
    }

    /**
     * طلب توليد كود.
     */
    public static final class GenerationRequest {

        private final String capabilityId;
        private final String goal;
        private final String description;
        private final String reason;
        private final String projectContext;
        private final String preferredProviderId;

        private final Set<String> allowedPaths;
        private final Set<String> requiredFiles;
        private final Set<String> requiredTools;
        private final Set<String> successCriteria;

        public GenerationRequest(
                String capabilityId,
                String goal,
                String description,
                String reason,
                String projectContext,
                String preferredProviderId,
                Set<String> allowedPaths,
                Set<String> requiredFiles,
                Set<String> requiredTools,
                Set<String> successCriteria
        ) {

            this.capabilityId =
                    safe(capabilityId);

            this.goal =
                    safe(goal);

            this.description =
                    safe(description);

            this.reason =
                    safe(reason);

            this.projectContext =
                    safe(projectContext);

            this.preferredProviderId =
                    safe(preferredProviderId);

            this.allowedPaths =
                    copy(allowedPaths);

            this.requiredFiles =
                    copy(requiredFiles);

            this.requiredTools =
                    copy(requiredTools);

            this.successCriteria =
                    copy(successCriteria);
        }

        public boolean isValid() {

            return !capabilityId.isEmpty() &&
                    !goal.isEmpty() &&
                    !reason.isEmpty();
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public String getGoal() {
            return goal;
        }

        public String getDescription() {
            return description;
        }

        public String getReason() {
            return reason;
        }

        public String getProjectContext() {
            return projectContext;
        }

        public String getPreferredProviderId() {
            return preferredProviderId;
        }

        public Set<String> getAllowedPaths() {

            return Collections.unmodifiableSet(
                    allowedPaths
            );
        }

        public Set<String> getRequiredFiles() {

            return Collections.unmodifiableSet(
                    requiredFiles
            );
        }

        public Set<String> getRequiredTools() {

            return Collections.unmodifiableSet(
                    requiredTools
            );
        }

        public Set<String> getSuccessCriteria() {

            return Collections.unmodifiableSet(
                    successCriteria
            );
        }

        private static String safe(
                String value
        ) {

            return value == null
                    ? ""
                    : value.trim();
        }

        private static Set<String> copy(
                Set<String> source
        ) {

            Set<String> result =
                    new LinkedHashSet<>();

            if (source == null) {
                return result;
            }

            for (String value :
                    source) {

                if (value == null) {
                    continue;
                }

                String clean =
                        value.trim();

                if (!clean.isEmpty()) {
                    result.add(clean);
                }
            }

            return result;
        }
    }

    /**
     * الكود الذي رجع من مصدر التوليد.
     */
    public static final class GeneratedCode {

        private final String generationId;
        private final String providerId;
        private final String explanation;
        private final List<GeneratedFile> files;

        public GeneratedCode(
                String generationId,
                String providerId,
                String explanation,
                List<GeneratedFile> files
        ) {

            this.generationId =
                    safe(generationId);

            this.providerId =
                    safe(providerId);

            this.explanation =
                    safe(explanation);

            List<GeneratedFile> copy =
                    new ArrayList<>();

            if (files != null) {

                for (GeneratedFile file :
                        files) {

                    if (file != null) {
                        copy.add(file);
                    }
                }
            }

            this.files =
                    copy;
        }

        public String getGenerationId() {
            return generationId;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getExplanation() {
            return explanation;
        }

        public List<GeneratedFile> getFiles() {

            return Collections.unmodifiableList(
                    files
            );
        }

        private static String safe(
                String value
        ) {

            return value == null
                    ? ""
                    : value.trim();
        }
    }

    /**
     * ملف واحد ناتج عن التوليد.
     */
    public static final class GeneratedFile {

        private final String path;

        private final SourceEvolutionEngine.FileChange.Operation
                operation;

        private final String content;

        public GeneratedFile(
                String path,
                SourceEvolutionEngine.FileChange.Operation operation,
                String content
        ) {

            this.path =
                    path == null
                            ? ""
                            : path.trim();

            this.operation =
                    operation;

            this.content =
                    content == null
                            ? ""
                            : content;
        }

        public boolean isValid() {

            if (path.isEmpty() ||
                    operation == null) {

                return false;
            }

            String normalized =
                    path.replace(
                            '\\',
                            '/'
                    );

            if (normalized.startsWith("/") ||
                    normalized.contains("../") ||
                    normalized.equals("..") ||
                    normalized.indexOf('\0') >= 0) {

                return false;
            }

            if (operation !=
                    SourceEvolutionEngine.FileChange.Operation.DELETE &&
                    content.indexOf('\0') >= 0) {

                return false;
            }

            return true;
        }

        public String getPath() {
            return path;
        }

        public SourceEvolutionEngine.FileChange.Operation
        getOperation() {

            return operation;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * سجل آخر عملية توليد.
     */
    public static final class GenerationRecord {

        private final String capabilityId;
        private final String providerId;
        private final boolean success;
        private final String message;

        private final long startedAt;
        private final long finishedAt;

        private GenerationRecord(
                String capabilityId,
                String providerId,
                boolean success,
                String message,
                long startedAt,
                long finishedAt
        ) {

            this.capabilityId =
                    capabilityId;

            this.providerId =
                    providerId;

            this.success =
                    success;

            this.message =
                    message;

            this.startedAt =
                    startedAt;

            this.finishedAt =
                    finishedAt;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public String getProviderId() {
            return providerId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getStartedAt() {
            return startedAt;
        }

        public long getFinishedAt() {
            return finishedAt;
        }

        public long getDurationMillis() {

            return finishedAt -
                    startedAt;
        }
    }
}