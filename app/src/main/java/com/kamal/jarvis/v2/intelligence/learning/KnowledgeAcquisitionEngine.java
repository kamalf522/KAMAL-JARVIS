package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2
 *
 * KnowledgeAcquisitionEngine
 *
 * مسؤول عن مرحلة اكتساب المعرفة من المصادر المسجلة.
 *
 * المسار:
 *
 * Query
 *   ↓
 * Sources
 *   ↓
 * Search
 *   ↓
 * Candidates
 *   ↓
 * KnowledgeItem
 *
 * ملاحظة مهمة:
 * هذا المحرك لا يعتبر أي نتيجة صحيحة تلقائياً.
 * التحقق والاعتماد يتمان في طبقة مستقلة.
 */
public final class KnowledgeAcquisitionEngine {

    private static final String ENGINE_ID =
            "v2.knowledge_acquisition";

    private final Map<String, KnowledgeSource> sources =
            new LinkedHashMap<>();

    /**
     * تسجيل مصدر معرفة.
     */
    public synchronized boolean registerSource(
            KnowledgeSource source
    ) {

        if (source == null) {
            return false;
        }

        String id = source.getId();

        if (id == null ||
                id.trim().isEmpty()) {

            return false;
        }

        sources.put(
                id.trim(),
                source
        );

        return true;
    }

    /**
     * إزالة مصدر.
     */
    public synchronized boolean unregisterSource(
            String sourceId
    ) {

        if (sourceId == null ||
                sourceId.trim().isEmpty()) {

            return false;
        }

        return sources.remove(
                sourceId.trim()
        ) != null;
    }

    /**
     * الحصول على مصدر.
     */
    public synchronized KnowledgeSource getSource(
            String sourceId
    ) {

        if (sourceId == null) {
            return null;
        }

        return sources.get(
                sourceId.trim()
        );
    }

    /**
     * البحث في جميع المصادر المتاحة.
     */
    public synchronized AcquisitionResult acquire(
            String query
    ) {

        return acquire(
                new KnowledgeSource.SearchRequest(
                        query,
                        10
                )
        );
    }

    /**
     * البحث باستخدام طلب مخصص.
     */
    public synchronized AcquisitionResult acquire(
            KnowledgeSource.SearchRequest request
    ) {

        if (request == null) {

            return AcquisitionResult.failure(
                    "SearchRequest cannot be null."
            );
        }

        List<KnowledgeItem> collected =
                new ArrayList<>();

        List<String> sourceErrors =
                new ArrayList<>();

        int attemptedSources = 0;
        int successfulSources = 0;

        List<KnowledgeSource> snapshot =
                new ArrayList<>(
                        sources.values()
                );

        for (KnowledgeSource source : snapshot) {

            if (source == null) {
                continue;
            }

            if (!source.isAvailable()) {
                continue;
            }

            if (!source.supportsContinuousLearning()) {
                continue;
            }

            attemptedSources++;

            try {

                KnowledgeSource.SearchResult result =
                        source.search(request);

                if (result == null) {

                    sourceErrors.add(
                            source.getId()
                                    + ": empty result."
                    );

                    continue;
                }

                if (!result.isSuccess()) {

                    sourceErrors.add(
                            source.getId()
                                    + ": "
                                    + result.getMessage()
                    );

                    continue;
                }

                successfulSources++;

                List<KnowledgeSource.KnowledgeCandidate>
                        candidates =
                        result.getCandidates();

                if (candidates == null) {
                    continue;
                }

                for (
                        KnowledgeSource.KnowledgeCandidate
                                candidate
                        : candidates
                ) {

                    KnowledgeItem item =
                            convertCandidate(
                                    source,
                                    request,
                                    candidate
                            );

                    if (item != null) {
                        collected.add(item);
                    }
                }

            } catch (Exception exception) {

                sourceErrors.add(
                        source.getId()
                                + ": "
                                + safeMessage(exception)
                );
            }
        }

        return AcquisitionResult.success(
                request.getQuery(),
                collected,
                attemptedSources,
                successfulSources,
                sourceErrors
        );
    }

    /**
     * البحث في مصدر محدد.
     */
    public synchronized AcquisitionResult acquireFrom(
            String sourceId,
            String query
    ) {

        if (sourceId == null ||
                sourceId.trim().isEmpty()) {

            return AcquisitionResult.failure(
                    "sourceId cannot be empty."
            );
        }

        KnowledgeSource source =
                sources.get(
                        sourceId.trim()
                );

        if (source == null) {

            return AcquisitionResult.failure(
                    "Knowledge source not found: "
                            + sourceId
            );
        }

        if (!source.isAvailable()) {

            return AcquisitionResult.failure(
                    "Knowledge source is unavailable: "
                            + sourceId
            );
        }

        KnowledgeSource.SearchRequest request =
                new KnowledgeSource.SearchRequest(
                        query,
                        10
                );

        try {

            KnowledgeSource.SearchResult result =
                    source.search(request);

            if (result == null) {

                return AcquisitionResult.failure(
                        "Source returned no result."
                );
            }

            if (!result.isSuccess()) {

                return AcquisitionResult.failure(
                        result.getMessage()
                );
            }

            List<KnowledgeItem> collected =
                    new ArrayList<>();

            List<KnowledgeSource.KnowledgeCandidate>
                    candidates =
                    result.getCandidates();

            if (candidates != null) {

                for (
                        KnowledgeSource.KnowledgeCandidate
                                candidate
                        : candidates
                ) {

                    KnowledgeItem item =
                            convertCandidate(
                                    source,
                                    request,
                                    candidate
                            );

                    if (item != null) {
                        collected.add(item);
                    }
                }
            }

            return AcquisitionResult.success(
                    query,
                    collected,
                    1,
                    1,
                    Collections.emptyList()
            );

        } catch (Exception exception) {

            return AcquisitionResult.failure(
                    sourceId
                            + ": "
                            + safeMessage(exception)
            );
        }
    }

    /**
     * تحويل النتيجة الخام إلى KnowledgeItem.
     */
    private KnowledgeItem convertCandidate(
            KnowledgeSource source,
            KnowledgeSource.SearchRequest request,
            KnowledgeSource.KnowledgeCandidate candidate
    ) {

        if (source == null ||
                request == null ||
                candidate == null ||
                !candidate.isUsable()) {

            return null;
        }

        String sourceId =
                safe(source.getId());

        String title =
                safe(candidate.getTitle());

        String content =
                safe(candidate.getContent());

        String location =
                safe(candidate.getLocation());

        String id =
                createKnowledgeId(
                        sourceId,
                        request.getQuery(),
                        title,
                        content,
                        location
                );

        KnowledgeItem item =
                KnowledgeItem.create(
                        id,
                        request.getQuery(),
                        title,
                        content,
                        sourceId,
                        location
                );

        /*
         * المعرفة في البداية تبقى NEW/COLLECTED.
         *
         * لا نرفع الثقة هنا.
         */
        item =
                item.withStatus(
                        KnowledgeItem.KnowledgeStatus.COLLECTED
                );

        /*
         * نحتفظ بالمعلومات الإضافية
         * التي قدمها المصدر.
         */
        if (!candidate.getMetadata().isEmpty()) {

            for (
                    Map.Entry<String, Object> entry
                    : candidate.getMetadata().entrySet()
            ) {

                if (entry.getKey() == null) {
                    continue;
                }

                item =
                        item.withMetadata(
                                entry.getKey(),
                                entry.getValue()
                        );
            }
        }

        item =
                item.withMetadata(
                        "source_type",
                        source.getType().name()
                );

        item =
                item.withMetadata(
                        "query",
                        request.getQuery()
                );

        item =
                item.withMetadata(
                        "continuous_learning",
                        source.supportsContinuousLearning()
                );

        return item;
    }

    /**
     * إنشاء معرف ثابت نسبياً للمعرفة.
     */
    private String createKnowledgeId(
            String sourceId,
            String query,
            String title,
            String content,
            String location
    ) {

        String raw =
                sourceId
                        + "|"
                        + query
                        + "|"
                        + title
                        + "|"
                        + content
                        + "|"
                        + location;

        return ENGINE_ID
                + "."
                + Integer.toHexString(
                        raw.hashCode()
                );
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private String safeMessage(
            Exception exception
    ) {

        if (exception == null) {
            return "unknown error";
        }

        String message =
                exception.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    public synchronized int getSourceCount() {
        return sources.size();
    }

    public synchronized boolean hasSource(
            String sourceId
    ) {

        return sourceId != null
                &&
                sources.containsKey(
                        sourceId.trim()
                );
    }

    public synchronized List<String>
    getSourceIds() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        sources.keySet()
                )
        );
    }

    public synchronized List<KnowledgeSource>
    getSources() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        sources.values()
                )
        );
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * نتيجة اكتساب المعرفة.
     */
    public static final class AcquisitionResult {

        private final boolean success;
        private final String query;
        private final List<KnowledgeItem> items;
        private final int attemptedSources;
        private final int successfulSources;
        private final List<String> sourceErrors;
        private final String message;

        private AcquisitionResult(
                boolean success,
                String query,
                List<KnowledgeItem> items,
                int attemptedSources,
                int successfulSources,
                List<String> sourceErrors,
                String message
        ) {

            this.success = success;
            this.query =
                    query == null
                            ? ""
                            : query;

            this.items =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    items == null
                                            ? Collections.emptyList()
                                            : items
                            )
                    );

            this.attemptedSources =
                    Math.max(
                            0,
                            attemptedSources
                    );

            this.successfulSources =
                    Math.max(
                            0,
                            successfulSources
                    );

            this.sourceErrors =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    sourceErrors == null
                                            ? Collections.emptyList()
                                            : sourceErrors
                            )
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public static AcquisitionResult success(
                String query,
                List<KnowledgeItem> items,
                int attemptedSources,
                int successfulSources,
                List<String> sourceErrors
        ) {

            return new AcquisitionResult(
                    true,
                    query,
                    items,
                    attemptedSources,
                    successfulSources,
                    sourceErrors,
                    "Knowledge acquisition completed."
            );
        }

        public static AcquisitionResult failure(
                String message
        ) {

            return new AcquisitionResult(
                    false,
                    "",
                    Collections.emptyList(),
                    0,
                    0,
                    Collections.emptyList(),
                    message
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isFailure() {
            return !success;
        }

        public String getQuery() {
            return query;
        }

        public List<KnowledgeItem> getItems() {
            return items;
        }

        public int getItemCount() {
            return items.size();
        }

        public int getAttemptedSources() {
            return attemptedSources;
        }

        public int getSuccessfulSources() {
            return successfulSources;
        }

        public List<String> getSourceErrors() {
            return sourceErrors;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasKnowledge() {
            return !items.isEmpty();
        }
    }
}