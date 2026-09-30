package com.kamal.jarvis.v2.intelligence.learning;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JARVIS V2
 *
 * KnowledgeAcquisitionEngine
 *
 * مسؤول عن اكتساب المعرفة من الإنترنت بشكل مستقل.
 *
 * المسار:
 *
 * User/Brain
 *      ↓
 * Query
 *      ↓
 * Internet
 *      ↓
 * Source Discovery
 *      ↓
 * Source Trust Evaluation
 *      ↓
 * Trusted / Accepted Sources
 *      ↓
 * Web Acquisition
 *      ↓
 * KnowledgeItem
 *      ↓
 * Verification
 *      ↓
 * KnowledgeMemory
 *
 * ملاحظة:
 *
 * هذا المحرك لم يعد يعتمد على registerSource()
 * حتى لا يحتاج JARVIS إلى إضافة المصادر يدوياً.
 *
 * اكتشاف المصادر يتم بواسطة:
 *
 * KnowledgeSourceTrustEngine
 *
 * ثم يتم إنشاء NetworkKnowledgeSource داخلياً
 * للمصادر المقبولة.
 */
public final class KnowledgeAcquisitionEngine {

    private static final String ENGINE_ID =
            "v2.knowledge_acquisition";

    private static final int DEFAULT_RESULTS = 8;

    private static final int CONNECT_TIMEOUT_MS = 10000;

    private static final int READ_TIMEOUT_MS = 15000;

    private static final int MAX_CONTENT_SIZE = 1_500_000;

    private static final int MAX_LINKS = 40;

    private final KnowledgeSourceTrustEngine trustEngine;

    /*
     * يحتفظ فقط بالمصادر التي اكتشفها JARVIS.
     *
     * المستخدم لا يحتاج إلى تسجيلها.
     */
    private final Map<String, KnowledgeSource> discoveredSources =
            new LinkedHashMap<>();

    public KnowledgeAcquisitionEngine() {

        this(
                new KnowledgeSourceTrustEngine()
        );
    }

    public KnowledgeAcquisitionEngine(
            KnowledgeSourceTrustEngine trustEngine
    ) {

        this.trustEngine =
                trustEngine == null
                        ? new KnowledgeSourceTrustEngine()
                        : trustEngine;
    }

    /**
     * البحث المستقل في الإنترنت.
     *
     * JARVIS هو الذي:
     *
     * 1. يبحث.
     * 2. يكتشف المصادر.
     * 3. يقيّم الثقة.
     * 4. يختار المصادر المقبولة.
     * 5. يجلب المعرفة.
     */
    public synchronized AcquisitionResult acquire(
            String query
    ) {

        return acquire(
                new KnowledgeSource.SearchRequest(
                        query,
                        DEFAULT_RESULTS
                )
        );
    }

    /**
     * البحث باستخدام SearchRequest.
     */
    public synchronized AcquisitionResult acquire(
            KnowledgeSource.SearchRequest request
    ) {

        if (request == null) {

            return AcquisitionResult.failure(
                    "SearchRequest cannot be null."
            );
        }

        String query =
                safe(
                        request.getQuery()
                );

        if (query.isEmpty()) {

            return AcquisitionResult.failure(
                    "Search query cannot be empty."
            );
        }

        /*
         * المرحلة الأولى:
         *
         * اكتشاف مصادر جديدة من الإنترنت.
         */
        KnowledgeSourceTrustEngine.DiscoveryResult discovery =
                trustEngine.discover(
                        query
                );

        if (discovery == null) {

            return AcquisitionResult.failure(
                    "Source discovery returned no result."
            );
        }

        if (!discovery.isSuccess()) {

            return AcquisitionResult.failure(
                    discovery.getMessage()
            );
        }

        /*
         * المرحلة الثانية:
         *
         * تحويل المصادر المقبولة إلى NetworkKnowledgeSource.
         */
        List<KnowledgeSource> usableSources =
                new ArrayList<>();

        for (
                KnowledgeSourceTrustEngine.SourceProfile profile
                : discovery.getSources()
        ) {

            if (profile == null) {
                continue;
            }

            KnowledgeSourceTrustEngine.TrustAssessment assessment =
                    trustEngine.getAssessment(
                            profile.getId()
                    );

            if (assessment == null) {
                continue;
            }

            if (!assessment.canUse()) {
                continue;
            }

            NetworkKnowledgeSource source =
                    new NetworkKnowledgeSource(
                            profile
                    );

            if (!source.isAvailable()) {
                continue;
            }

            discoveredSources.put(
                    source.getId(),
                    source
            );

            usableSources.add(
                    source
            );
        }

        /*
         * إذا لم نجد أي مصدر قابل للاستخدام،
         * لا ندّعي أن التعلم نجح.
         */
        if (usableSources.isEmpty()) {

            return AcquisitionResult.failure(
                    "No usable Internet source passed "
                            + "the trust evaluation."
            );
        }

        /*
         * المرحلة الثالثة:
         *
         * جمع المعرفة من المصادر المقبولة.
         */
        List<KnowledgeItem> collected =
                new ArrayList<>();

        List<String> errors =
                new ArrayList<>();

        int attempted =
                0;

        int successful =
                0;

        for (
                KnowledgeSource source
                : usableSources
        ) {

            if (source == null
                    || !source.isAvailable()) {
                continue;
            }

            attempted++;

            try {

                KnowledgeSource.SearchResult result =
                        source.search(
                                request
                        );

                if (result == null) {

                    errors.add(
                            source.getId()
                                    + ": empty result."
                    );

                    continue;
                }

                if (!result.isSuccess()) {

                    errors.add(
                            source.getId()
                                    + ": "
                                    + result.getMessage()
                    );

                    continue;
                }

                successful++;

                List<KnowledgeSource.KnowledgeCandidate>
                        candidates =
                        result.getCandidates();

                if (candidates == null) {
                    continue;
                }

                for (
                        KnowledgeSource.KnowledgeCandidate candidate
                        : candidates
                ) {

                    KnowledgeItem item =
                            convertCandidate(
                                    source,
                                    request,
                                    candidate
                            );

                    if (item != null) {
                        collected.add(
                                item
                        );
                    }
                }

            } catch (Exception exception) {

                errors.add(
                        source.getId()
                                + ": "
                                + safeMessage(
                                exception
                        )
                );
            }
        }

        /*
         * لا نعتبر المعرفة "موثوقة" هنا.
         *
         * KnowledgeVerificationEngine هو المسؤول
         * عن التحقق النهائي.
         */
        return AcquisitionResult.success(
                query,
                collected,
                attempted,
                successful,
                errors
        );
    }

    /**
     * إعادة اكتشاف المصادر والبحث من الإنترنت.
     *
     * مفيدة للتعلم المستمر.
     */
    public synchronized AcquisitionResult discoverAndAcquire(
            String query
    ) {

        return acquire(
                query
        );
    }

    /**
     * إعادة التحقق من المصادر التي اكتشفها JARVIS.
     */
    public synchronized KnowledgeSourceTrustEngine.RevalidationResult
    revalidateSources() {

        KnowledgeSourceTrustEngine.RevalidationResult result =
                trustEngine.revalidateAll();

        /*
         * المصادر القديمة قد تكون تغيرت.
         *
         * نحذف المصادر التي لم تعد قابلة للاستخدام.
         */
        List<String> removeIds =
                new ArrayList<>();

        for (
                Map.Entry<String, KnowledgeSource> entry
                : discoveredSources.entrySet()
        ) {

            KnowledgeSourceTrustEngine.TrustAssessment assessment =
                    trustEngine.getAssessment(
                            entry.getKey()
                    );

            if (assessment == null
                    || !assessment.canUse()) {

                removeIds.add(
                        entry.getKey()
                );
            }
        }

        for (String id : removeIds) {
            discoveredSources.remove(
                    id
            );
        }

        return result;
    }

    /**
     * الحصول على مصدر اكتشفه JARVIS.
     */
    public synchronized KnowledgeSource getSource(
            String sourceId
    ) {

        if (sourceId == null) {
            return null;
        }

        return discoveredSources.get(
                sourceId.trim()
        );
    }

    /**
     * البحث في مصدر اكتشفه JARVIS سابقاً.
     */
    public synchronized AcquisitionResult acquireFrom(
            String sourceId,
            String query
    ) {

        if (sourceId == null
                || sourceId.trim().isEmpty()) {

            return AcquisitionResult.failure(
                    "sourceId cannot be empty."
            );
        }

        KnowledgeSource source =
                discoveredSources.get(
                        sourceId.trim()
                );

        if (source == null) {

            return AcquisitionResult.failure(
                    "Knowledge source not found: "
                            + sourceId
            );
        }

        KnowledgeSourceTrustEngine.TrustAssessment assessment =
                trustEngine.getAssessment(
                        source.getId()
                );

        if (assessment == null
                || !assessment.canUse()) {

            return AcquisitionResult.failure(
                    "Source is not currently trusted "
                            + "enough for acquisition."
            );
        }

        KnowledgeSource.SearchRequest request =
                new KnowledgeSource.SearchRequest(
                        query,
                        DEFAULT_RESULTS
                );

        try {

            KnowledgeSource.SearchResult result =
                    source.search(
                            request
                    );

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

            List<KnowledgeItem> items =
                    new ArrayList<>();

            for (
                    KnowledgeSource.KnowledgeCandidate candidate
                    : result.getCandidates()
            ) {

                KnowledgeItem item =
                        convertCandidate(
                                source,
                                request,
                                candidate
                        );

                if (item != null) {
                    items.add(
                            item
                    );
                }
            }

            return AcquisitionResult.success(
                    query,
                    items,
                    1,
                    1,
                    Collections.emptyList()
            );

        } catch (Exception exception) {

            return AcquisitionResult.failure(
                    safeMessage(
                            exception
                    )
            );
        }
    }

    /**
     * تحويل KnowledgeCandidate إلى KnowledgeItem.
     */
    private KnowledgeItem convertCandidate(
            KnowledgeSource source,
            KnowledgeSource.SearchRequest request,
            KnowledgeSource.KnowledgeCandidate candidate
    ) {

        if (source == null
                || request == null
                || candidate == null
                || !candidate.isUsable()) {

            return null;
        }

        String sourceId =
                safe(
                        source.getId()
                );

        String title =
                safe(
                        candidate.getTitle()
                );

        String content =
                safe(
                        candidate.getContent()
                );

        String location =
                safe(
                        candidate.getLocation()
                );

        if (content.isEmpty()) {
            return null;
        }

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

        item =
                item.withStatus(
                        KnowledgeItem.KnowledgeStatus.COLLECTED
                );

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
                        "source_url",
                        location
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

    public synchronized int getSourceCount() {
        return discoveredSources.size();
    }

    public synchronized boolean hasSource(
            String sourceId
    ) {

        return sourceId != null
                && discoveredSources.containsKey(
                sourceId.trim()
        );
    }

    public synchronized List<String> getSourceIds() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        discoveredSources.keySet()
                )
        );
    }

    public synchronized List<KnowledgeSource> getSources() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        discoveredSources.values()
                )
        );
    }

    public synchronized List<KnowledgeSourceTrustEngine.SourceProfile>
    getDiscoveredSourceProfiles() {

        return trustEngine.getDiscoveredSources();
    }

    public synchronized List<KnowledgeSourceTrustEngine.SourceProfile>
    getUsableSourceProfiles() {

        return trustEngine.getUsableSources();
    }

    public KnowledgeSourceTrustEngine
    getTrustEngine() {

        return trustEngine;
    }

    public String getEngineId() {
        return ENGINE_ID;
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

        if (message == null
                || message.trim().isEmpty()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    /**
     * مصدر شبكي داخلي.
     *
     * لا يحتاج ملف Java منفصل.
     *
     * مهمته:
     *
     * URL
     * ↓
     * HTTP
     * ↓
     * HTML
     * ↓
     * نص
     * ↓
     * KnowledgeCandidate
     */
    private static final class NetworkKnowledgeSource
            implements KnowledgeSource {

        private final KnowledgeSourceTrustEngine.SourceProfile profile;

        private NetworkKnowledgeSource(
                KnowledgeSourceTrustEngine.SourceProfile profile
        ) {

            this.profile =
                    profile;
        }

        @Override
        public String getId() {

            return profile.getId();
        }

        @Override
        public String getName() {

            return profile.getName();
        }

        @Override
        public String getDescription() {

            return profile.getDescription();
        }

        @Override
        public SourceType getType() {

            return profile.getType();
        }

        @Override
        public boolean isAvailable() {

            return profile.isAvailable()
                    && !isBlankStatic(
                    profile.getUrl()
            );
        }

        @Override
        public boolean requiresNetwork() {
            return true;
        }

        @Override
        public boolean supportsContinuousLearning() {
            return true;
        }

        @Override
        public SearchResult search(
                SearchRequest request
        ) {

            if (request == null
                    || isBlankStatic(
                    request.getQuery()
            )) {

                return SearchResult.failure(
                        "Search request is empty."
                );
            }

            /*
             * أولاً نحاول استخدام المصدر نفسه.
             */
            List<KnowledgeCandidate> candidates =
                    fetchPage(
                            profile.getUrl(),
                            request
                    );

            /*
             * إذا لم يعطينا المصدر نتيجة مفيدة،
             * نحاول استخراج الروابط الموجودة داخله
             * والبحث في الصفحات المرتبطة.
             */
            if (candidates.isEmpty()) {

                candidates =
                        fetchLinkedPages(
                                profile.getUrl(),
                                request
                        );
            }

            if (candidates.isEmpty()) {

                return SearchResult.failure(
                        "No usable knowledge found from source."
                );
            }

            return SearchResult.success(
                    candidates
            );
        }

        @Override
        public Map<String, Object> getMetadata() {

            Map<String, Object> metadata =
                    new LinkedHashMap<>();

            metadata.put(
                    "url",
                    profile.getUrl()
            );

            metadata.put(
                    "owner",
                    profile.getOwner()
            );

            metadata.put(
                    "official",
                    profile.isOfficial()
            );

            metadata.put(
                    "verified_ownership",
                    profile.isVerifiedOwnership()
            );

            metadata.put(
                    "trust_domain_relevance",
                    profile.getDomainRelevance()
            );

            return metadata;
        }

        private List<KnowledgeCandidate> fetchPage(
                String url,
                SearchRequest request
        ) {

            HttpURLConnection connection =
                    null;

            try {

                URL target =
                        new URL(
                                url
                        );

                connection =
                        (HttpURLConnection)
                                target.openConnection();

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        CONNECT_TIMEOUT_MS
                );

                connection.setReadTimeout(
                        READ_TIMEOUT_MS
                );

                connection.setInstanceFollowRedirects(
                        true
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Kamal-JARVIS/2.0"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,"
                                + "text/plain;q=0.9"
                );

                int code =
                        connection.getResponseCode();

                if (code < 200
                        || code >= 400) {

                    return Collections.emptyList();
                }

                InputStream stream =
                        connection.getInputStream();

                String html =
                        readLimited(
                                stream,
                                MAX_CONTENT_SIZE
                        );

                return parsePage(
                        url,
                        html,
                        request
                );

            } catch (Exception ignored) {

                return Collections.emptyList();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private List<KnowledgeCandidate> fetchLinkedPages(
                String baseUrl,
                SearchRequest request
        ) {

            HttpURLConnection connection =
                    null;

            try {

                URL target =
                        new URL(
                                baseUrl
                        );

                connection =
                        (HttpURLConnection)
                                target.openConnection();

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        CONNECT_TIMEOUT_MS
                );

                connection.setReadTimeout(
                        READ_TIMEOUT_MS
                );

                connection.setInstanceFollowRedirects(
                        true
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Kamal-JARVIS/2.0"
                );

                int code =
                        connection.getResponseCode();

                if (code < 200
                        || code >= 400) {

                    return Collections.emptyList();
                }

                String html =
                        readLimited(
                                connection.getInputStream(),
                                MAX_CONTENT_SIZE
                        );

                List<String> links =
                        extractLinks(
                                baseUrl,
                                html
                        );

                List<KnowledgeCandidate> results =
                        new ArrayList<>();

                int limit =
                        Math.min(
                                links.size(),
                                MAX_LINKS
                        );

                for (int i = 0;
                     i < limit;
                     i++) {

                    String link =
                            links.get(i);

                    if (!isRelevantLink(
                            link,
                            request.getQuery()
                    )) {
                        continue;
                    }

                    List<KnowledgeCandidate>
                            page =
                            fetchPage(
                                    link,
                                    request
                            );

                    results.addAll(
                            page
                    );

                    if (results.size()
                            >= request.getMaxResults()) {

                        break;
                    }
                }

                return limitResults(
                        results,
                        request.getMaxResults()
                );

            } catch (Exception ignored) {

                return Collections.emptyList();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private List<KnowledgeCandidate> parsePage(
                String url,
                String html,
                SearchRequest request
        ) {

            if (html == null
                    || html.trim().isEmpty()) {

                return Collections.emptyList();
            }

            String title =
                    extractTitle(
                            html
                    );

            String text =
                    extractText(
                            html
                    );

            if (text.length() > MAX_CONTENT_SIZE) {

                text =
                        text.substring(
                                0,
                                MAX_CONTENT_SIZE
                        );
            }

            /*
             * لا نخزن الصفحة كاملة إذا لم تكن
             * مرتبطة بطلب المستخدم.
             *
             * نبحث عن كلمات الطلب داخل النص.
             */
            double relevance =
                    calculateRelevance(
                            request.getQuery(),
                            title,
                            text
                    );

            if (relevance <= 0.0) {

                return Collections.emptyList();
            }

            Map<String, Object> metadata =
                    new LinkedHashMap<>();

            metadata.put(
                    "relevance",
                    relevance
            );

            metadata.put(
                    "source_url",
                    url
            );

            metadata.put(
                    "acquisition",
                    "internet"
            );

            KnowledgeCandidate candidate =
                    new KnowledgeCandidate(
                            title,
                            text,
                            url,
                            profile.getOwner(),
                            metadata
                    );

            List<KnowledgeCandidate> result =
                    new ArrayList<>();

            result.add(
                    candidate
            );

            return result;
        }

        private double calculateRelevance(
                String query,
                String title,
                String content
        ) {

            String q =
                    query.toLowerCase();

            String combined =
                    (
                            safeStatic(title)
                                    + " "
                                    + safeStatic(content)
                    ).toLowerCase();

            String[] words =
                    q.split(
                            "\\s+"
                    );

            if (words.length == 0) {
                return 0.0;
            }

            int matches = 0;

            for (String word : words) {

                if (word.length() < 2) {
                    continue;
                }

                if (combined.contains(
                        word
                )) {

                    matches++;
                }
            }

            return Math.min(
                    1.0,
                    (double) matches
                            / (double) words.length
            );
        }

        private List<String> extractLinks(
                String baseUrl,
                String html
        ) {

            List<String> links =
                    new ArrayList<>();

            Set<String> seen =
                    new HashSet<>();

            Pattern pattern =
                    Pattern.compile(
                            "<a[^>]+href=[\"']([^\"']+)[\"']",
                            Pattern.CASE_INSENSITIVE
                    );

            Matcher matcher =
                    pattern.matcher(
                            html
                    );

            while (matcher.find()
                    && links.size()
                    < MAX_LINKS) {

                String raw =
                        decodeHtml(
                                matcher.group(1)
                        );

                String absolute =
                        resolveUrl(
                                baseUrl,
                                raw
                        );

                if (!isHttpUrl(
                        absolute
                )) {
                    continue;
                }

                String normalized =
                        normalizeUrl(
                                absolute
                        );

                if (seen.add(
                        normalized
                )) {

                    links.add(
                            absolute
                    );
                }
            }

            return links;
        }

        private boolean isRelevantLink(
                String url,
                String query
        ) {

            String lower =
                    url.toLowerCase();

            String[] words =
                    query.toLowerCase()
                            .split(
                                    "\\s+"
                            );

            for (String word : words) {

                if (word.length() >= 3
                        && lower.contains(
                        word
                )) {

                    return true;
                }
            }

            return lower.contains(
                    "article"
            )
                    || lower.contains(
                    "docs"
            )
                    || lower.contains(
                    "learn"
            )
                    || lower.contains(
                    "guide"
            )
                    || lower.contains(
                    "research"
            );
        }

        private static String extractTitle(
                String html
        ) {

            Pattern pattern =
                    Pattern.compile(
                            "<title[^>]*>(.*?)</title>",
                            Pattern.CASE_INSENSITIVE
                                    | Pattern.DOTALL
                    );

            Matcher matcher =
                    pattern.matcher(
                            html
                    );

            if (!matcher.find()) {
                return "";
            }

            return stripHtmlStatic(
                    matcher.group(1)
            );
        }

        private static String extractText(
                String html
        ) {

            String value =
                    html
                            .replaceAll(
                                    "(?is)<script.*?</script>",
                                    " "
                            )
                            .replaceAll(
                                    "(?is)<style.*?</style>",
                                    " "
                            )
                            .replaceAll(
                                    "(?is)<noscript.*?</noscript>",
                                    " "
                            )
                            .replaceAll(
                                    "<[^>]+>",
                                    " "
                            )
                            .replaceAll(
                                    "\\s+",
                                    " "
                            );

            return decodeHtml(
                    value
            ).trim();
        }

        private static String readLimited(
                InputStream input,
                int maxBytes
        ) throws IOException {

            if (input == null) {
                return "";
            }

            byte[] buffer =
                    new byte[8192];

            int total = 0;

            StringBuilder result =
                    new StringBuilder();

            while (true) {

                int read =
                        input.read(
                                buffer
                        );

                if (read < 0) {
                    break;
                }

                int allowed =
                        read;

                if (total + read
                        > maxBytes) {

                    allowed =
                            maxBytes - total;
                }

                if (allowed > 0) {

                    result.append(
                            new String(
                                    buffer,
                                    0,
                                    allowed,
                                    StandardCharsets.UTF_8
                            )
                    );

                    total += allowed;
                }

                if (total >= maxBytes) {
                    break;
                }
            }

            return result.toString();
        }

        private static String resolveUrl(
                String base,
                String link
        ) {

            try {

                URI baseUri =
                        new URI(
                                base
                        );

                URI resolved =
                        baseUri.resolve(
                                link
                        );

                return resolved.toString();

            } catch (Exception e) {

                return "";
            }
        }

        private static boolean isHttpUrl(
                String url
        ) {

            try {

                URI uri =
                        new URI(
                                url
                        );

                String scheme =
                        uri.getScheme();

                return (
                        "http".equalsIgnoreCase(
                                scheme
                        )
                                || "https".equalsIgnoreCase(
                                scheme
                        )
                )
                        && uri.getHost() != null;

            } catch (Exception e) {

                return false;
            }
        }

        private static String normalizeUrl(
                String url
        ) {

            String value =
                    safeStatic(
                            url
                    );

            while (value.endsWith("/")) {

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );
            }

            return value.toLowerCase();
        }

        private static String decodeHtml(
                String value
        ) {

            if (value == null) {
                return "";
            }

            return value
                    .replace(
                            "&amp;",
                            "&"
                    )
                    .replace(
                            "&quot;",
                            "\""
                    )
                    .replace(
                            "&#39;",
                            "'"
                    )
                    .replace(
                            "&lt;",
                            "<"
                    )
                    .replace(
                            "&gt;",
                            ">"
                    );
        }

        private static String stripHtmlStatic(
                String value
        ) {

            if (value == null) {
                return "";
            }

            return decodeHtml(
                    value
                            .replaceAll(
                                    "<[^>]+>",
                                    " "
                            )
                            .replaceAll(
                                    "\\s+",
                                    " "
                            )
            ).trim();
        }

        private static String safeStatic(
                String value
        ) {

            return value == null
                    ? ""
                    : value.trim();
        }

        private static boolean isBlankStatic(
                String value
        ) {

            return value == null
                    || value.trim().isEmpty();
        }

        private static List<KnowledgeCandidate>
        limitResults(
                List<KnowledgeCandidate> input,
                int max
        ) {

            if (input == null
                    || input.isEmpty()) {

                return Collections.emptyList();
            }

            int limit =
                    Math.max(
                            1,
                            max
                    );

            if (input.size() <= limit) {
                return input;
            }

            return new ArrayList<>(
                    input.subList(
                            0,
                            limit
                    )
            );
        }
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

            this.success =
                    success;

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