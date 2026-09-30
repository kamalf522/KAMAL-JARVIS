package com.kamal.jarvis.v2.intelligence.learning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JARVIS V2
 *
 * KnowledgeSource
 *
 * العقد العام لأي مصدر خارجي يمكن أن يساهم في
 * نظام التعلم المستمر.
 *
 * الهدف:
 * - عدم ربط نظام التعلم بمصدر واحد.
 * - السماح بإضافة Internet / YouTube / Documentation
 *   / APIs / ملفات / مصادر أخرى لاحقاً.
 * - كل مصدر يعطي معلومات خام فقط.
 * - التحقق والاعتماد يتمان في طبقات أخرى.
 */
public interface KnowledgeSource {

    /**
     * معرف ثابت للمصدر.
     */
    String getId();

    /**
     * اسم المصدر.
     */
    String getName();

    /**
     * وصف المصدر.
     */
    String getDescription();

    /**
     * نوع المصدر.
     */
    SourceType getType();

    /**
     * هل المصدر متاح حالياً؟
     */
    boolean isAvailable();

    /**
     * هل يحتاج اتصالاً بالشبكة؟
     */
    default boolean requiresNetwork() {
        return true;
    }

    /**
     * هل يمكن استعمال المصدر للتعلم التلقائي؟
     */
    default boolean supportsContinuousLearning() {
        return true;
    }

    /**
     * البحث عن معرفة.
     *
     * المصدر مسؤول عن جلب النتائج فقط.
     * لا يجب اعتباره موثوقاً تلقائياً.
     */
    SearchResult search(SearchRequest request);

    /**
     * الحصول على معلومات عن المصدر.
     */
    default Map<String, Object> getMetadata() {
        return Collections.emptyMap();
    }

    /**
     * أنواع مصادر المعرفة.
     */
    enum SourceType {

        INTERNET,

        WEB_PAGE,

        VIDEO,

        DOCUMENTATION,

        API,

        LOCAL_FILE,

        DATABASE,

        OTHER
    }

    /**
     * طلب البحث.
     */
    final class SearchRequest {

        private final String query;
        private final int maxResults;
        private final Map<String, String> filters;

        public SearchRequest(
                String query,
                int maxResults
        ) {
            this(
                    query,
                    maxResults,
                    Collections.emptyMap()
            );
        }

        public SearchRequest(
                String query,
                int maxResults,
                Map<String, String> filters
        ) {

            if (query == null ||
                    query.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "query cannot be empty."
                );
            }

            this.query = query.trim();

            this.maxResults =
                    maxResults <= 0
                            ? 10
                            : Math.min(maxResults, 100);

            Map<String, String> copy =
                    filters == null
                            ? Collections.emptyMap()
                            : new LinkedHashMap<>(filters);

            this.filters =
                    Collections.unmodifiableMap(copy);
        }

        public String getQuery() {
            return query;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public Map<String, String> getFilters() {
            return filters;
        }

        public String getFilter(String key) {

            if (key == null) {
                return null;
            }

            return filters.get(key);
        }

        public boolean hasFilter(String key) {
            return key != null &&
                    filters.containsKey(key);
        }
    }

    /**
     * نتيجة البحث.
     */
    final class SearchResult {

        private final boolean success;
        private final String sourceId;
        private final String query;
        private final java.util.List<KnowledgeCandidate> candidates;
        private final String message;

        private SearchResult(
                boolean success,
                String sourceId,
                String query,
                java.util.List<KnowledgeCandidate> candidates,
                String message
        ) {

            this.success = success;
            this.sourceId = sourceId;
            this.query = query;

            java.util.List<KnowledgeCandidate> copy =
                    candidates == null
                            ? Collections.emptyList()
                            : new java.util.ArrayList<>(
                                    candidates
                            );

            this.candidates =
                    Collections.unmodifiableList(copy);

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public static SearchResult success(
                String sourceId,
                String query,
                java.util.List<KnowledgeCandidate> candidates,
                String message
        ) {

            return new SearchResult(
                    true,
                    sourceId,
                    query,
                    candidates,
                    message
            );
        }

        public static SearchResult failure(
                String sourceId,
                String query,
                String message
        ) {

            return new SearchResult(
                    false,
                    sourceId,
                    query,
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

        public String getSourceId() {
            return sourceId;
        }

        public String getQuery() {
            return query;
        }

        public java.util.List<KnowledgeCandidate>
        getCandidates() {
            return candidates;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasCandidates() {
            return !candidates.isEmpty();
        }
    }

    /**
     * معلومة مرشحة للتعلم.
     *
     * ملاحظة:
     * Candidate لا تعني أن JARVIS صدق المعلومة.
     * الاعتماد يتم بعد مرحلة التحقق.
     */
    final class KnowledgeCandidate {

        private final String title;
        private final String content;
        private final String location;
        private final String author;
        private final Map<String, Object> metadata;

        public KnowledgeCandidate(
                String title,
                String content,
                String location
        ) {

            this(
                    title,
                    content,
                    location,
                    null,
                    Collections.emptyMap()
            );
        }

        public KnowledgeCandidate(
                String title,
                String content,
                String location,
                String author,
                Map<String, Object> metadata
        ) {

            this.title =
                    title == null
                            ? ""
                            : title.trim();

            this.content =
                    content == null
                            ? ""
                            : content.trim();

            this.location =
                    location == null
                            ? ""
                            : location.trim();

            this.author =
                    author == null
                            ? ""
                            : author.trim();

            Map<String, Object> copy =
                    metadata == null
                            ? Collections.emptyMap()
                            : new LinkedHashMap<>(
                                    metadata
                            );

            this.metadata =
                    Collections.unmodifiableMap(copy);
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getLocation() {
            return location;
        }

        public String getAuthor() {
            return author;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public boolean isUsable() {

            return !content.isEmpty()
                    || !title.isEmpty();
        }
    }
}