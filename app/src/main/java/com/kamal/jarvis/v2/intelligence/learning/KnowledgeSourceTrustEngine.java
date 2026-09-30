package com.kamal.jarvis.v2.intelligence.learning;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central intelligence engine for discovering, inspecting and evaluating
 * Internet knowledge sources.
 *
 * Responsibilities:
 *
 * 1. Connect to the Internet when available.
 * 2. Search for information.
 * 3. Discover previously unknown source URLs.
 * 4. Inspect discovered sources.
 * 5. Extract source identity and ownership signals.
 * 6. Evaluate source trustworthiness.
 * 7. Keep discovered source profiles in memory.
 * 8. Select sources that are usable for knowledge acquisition.
 * 9. Compare independent sources.
 * 10. Revalidate sources over time.
 *
 * Important:
 * This engine does NOT blindly trust Internet content.
 *
 * Network search is infrastructure.
 * A discovered website is not automatically trusted.
 *
 * This class intentionally uses HttpURLConnection so that no additional
 * networking dependency is required.
 *
 * Network operations must be executed from a background thread.
 */
public final class KnowledgeSourceTrustEngine {

    private static final String ENGINE_ID =
            "v2.knowledge_source_trust";

    private static final double TRUSTED_THRESHOLD = 0.80;
    private static final double ACCEPTED_THRESHOLD = 0.65;
    private static final double REVIEW_THRESHOLD = 0.45;

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private static final int MAX_RESPONSE_BYTES = 2_000_000;
    private static final int MAX_SEARCH_RESULTS = 12;
    private static final int MAX_DISCOVERED_SOURCES = 40;
    private static final int MAX_LINKS_PER_PAGE = 80;

    private static final String USER_AGENT =
            "Kamal-JARVIS/2.0 KnowledgeEngine";

    /*
     * Search infrastructure.
     *
     * These are search mechanisms, NOT trusted knowledge sources.
     * Results are still independently evaluated before being accepted.
     */
    private static final String SEARCH_ENDPOINT =
            "https://html.duckduckgo.com/html/?q=";

    private final Map<String, SourceProfile> discoveredSources =
            new LinkedHashMap<>();

    private final Map<String, TrustAssessment> assessments =
            new HashMap<>();

    /**
     * Tests whether the Internet can currently be reached.
     */
    public synchronized NetworkStatus checkNetwork() {

        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://www.google.com/generate_204");

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            int code = connection.getResponseCode();

            boolean online =
                    code >= 200 && code < 500;

            return new NetworkStatus(
                    online,
                    code,
                    online
                            ? "Internet connection available."
                            : "Internet connection unavailable."
            );

        } catch (Exception e) {

            return new NetworkStatus(
                    false,
                    -1,
                    "Internet connection failed: "
                            + safeMessage(e)
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Searches the Internet and discovers source candidates.
     *
     * The returned sources are NOT automatically trusted.
     */
    public synchronized DiscoveryResult discover(
            String query
    ) {

        if (isBlank(query)) {
            return DiscoveryResult.failure(
                    "Search query is empty."
            );
        }

        NetworkStatus network = checkNetwork();

        if (!network.isOnline()) {
            return DiscoveryResult.failure(
                    network.getMessage()
            );
        }

        String searchUrl =
                SEARCH_ENDPOINT
                        + urlEncode(query.trim());

        HttpResponse response =
                fetch(searchUrl);

        if (!response.isSuccess()) {
            return DiscoveryResult.failure(
                    "Internet search failed: "
                            + response.getMessage()
            );
        }

        List<DiscoveredSource> candidates =
                parseSearchResults(
                        response.getBody()
                );

        List<SourceProfile> acceptedCandidates =
                new ArrayList<>();

        int inspected = 0;

        for (DiscoveredSource candidate : candidates) {

            if (inspected >= MAX_DISCOVERED_SOURCES) {
                break;
            }

            inspected++;

            SourceProfile profile =
                    inspectSource(
                            candidate.getUrl(),
                            candidate.getTitle(),
                            candidate.getSnippet()
                    );

            if (profile == null) {
                continue;
            }

            discoveredSources.put(
                    profile.getId(),
                    profile
            );

            TrustAssessment assessment =
                    evaluate(
                            profile.toEvidence()
                    );

            assessments.put(
                    profile.getId(),
                    assessment
            );

            acceptedCandidates.add(profile);
        }

        return DiscoveryResult.success(
                query,
                acceptedCandidates
        );
    }

    /**
     * Searches the Internet and returns only sources that are currently
     * usable according to the trust engine.
     */
    public synchronized List<SourceProfile> discoverUsable(
            String query
    ) {

        DiscoveryResult result =
                discover(query);

        if (!result.isSuccess()) {
            return Collections.emptyList();
        }

        List<SourceProfile> usable =
                new ArrayList<>();

        for (SourceProfile profile :
                result.getSources()) {

            TrustAssessment assessment =
                    assessments.get(
                            profile.getId()
                    );

            if (assessment != null
                    && assessment.canUse()) {

                usable.add(profile);
            }
        }

        return Collections.unmodifiableList(
                usable
        );
    }

    /**
     * Fetches and analyzes a specific URL.
     */
    public synchronized SourceProfile inspect(
            String url
    ) {

        if (!isValidHttpUrl(url)) {
            return null;
        }

        SourceProfile profile =
                inspectSource(
                        url,
                        "",
                        ""
                );

        if (profile == null) {
            return null;
        }

        discoveredSources.put(
                profile.getId(),
                profile
        );

        TrustAssessment assessment =
                evaluate(
                        profile.toEvidence()
                );

        assessments.put(
                profile.getId(),
                assessment
        );

        return profile;
    }

    /**
     * Revalidates every discovered source.
     */
    public synchronized RevalidationResult revalidateAll() {

        int checked = 0;
        int available = 0;
        int trusted = 0;
        int rejected = 0;

        List<String> ids =
                new ArrayList<>(
                        discoveredSources.keySet()
                );

        for (String id : ids) {

            SourceProfile old =
                    discoveredSources.get(id);

            if (old == null) {
                continue;
            }

            checked++;

            SourceProfile refreshed =
                    inspectSource(
                            old.getUrl(),
                            old.getName(),
                            old.getDescription()
                    );

            if (refreshed == null) {
                continue;
            }

            discoveredSources.put(
                    refreshed.getId(),
                    refreshed
            );

            TrustAssessment assessment =
                    evaluate(
                            refreshed.toEvidence()
                    );

            assessments.put(
                    refreshed.getId(),
                    assessment
            );

            if (refreshed.isAvailable()) {
                available++;
            }

            if (assessment.isTrusted()) {
                trusted++;
            }

            if (assessment.getLevel()
                    == TrustLevel.REJECTED) {

                rejected++;
            }
        }

        return new RevalidationResult(
                checked,
                available,
                trusted,
                rejected
        );
    }

    /**
     * Returns all discovered sources.
     */
    public synchronized List<SourceProfile> getDiscoveredSources() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        discoveredSources.values()
                )
        );
    }

    /**
     * Returns only currently usable sources.
     */
    public synchronized List<SourceProfile> getUsableSources() {

        List<SourceProfile> result =
                new ArrayList<>();

        for (SourceProfile profile :
                discoveredSources.values()) {

            TrustAssessment assessment =
                    assessments.get(
                            profile.getId()
                    );

            if (assessment != null
                    && assessment.canUse()) {

                result.add(profile);
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * Returns the latest assessment for a source.
     */
    public synchronized TrustAssessment getAssessment(
            String sourceId
    ) {

        if (isBlank(sourceId)) {
            return null;
        }

        return assessments.get(
                sourceId
        );
    }

    /**
     * Evaluates supplied source evidence.
     */
    public synchronized TrustAssessment evaluate(
            SourceEvidence evidence
    ) {

        if (evidence == null) {
            return TrustAssessment.failure(
                    "No source evidence was supplied."
            );
        }

        if (isBlank(evidence.getSourceId())) {
            return TrustAssessment.failure(
                    "Source ID is missing."
            );
        }

        List<TrustCheck> checks =
                new ArrayList<>();

        double score = 0.0;
        double weight = 0.0;

        score = addCheck(
                checks,
                score,
                0.16,
                "identity",
                evaluateIdentity(evidence),
                "Source identity and URL"
        );

        weight += 0.16;

        score = addCheck(
                checks,
                score,
                0.18,
                "ownership",
                evaluateOwnership(evidence),
                "Ownership and responsible organization"
        );

        weight += 0.18;

        score = addCheck(
                checks,
                score,
                0.14,
                "reputation",
                normalize(
                        evidence.getReputationScore()
                ),
                "Reputation signals"
        );

        weight += 0.14;

        score = addCheck(
                checks,
                score,
                0.14,
                "references",
                normalize(
                        evidence.getReferenceQuality()
                ),
                "References and supporting evidence"
        );

        weight += 0.14;

        score = addCheck(
                checks,
                score,
                0.14,
                "independent_corroboration",
                normalize(
                        evidence
                                .getIndependentCorroboration()
                ),
                "Independent corroboration"
        );

        weight += 0.14;

        score = addCheck(
                checks,
                score,
                0.08,
                "freshness",
                normalize(
                        evidence.getFreshnessScore()
                ),
                "Freshness"
        );

        weight += 0.08;

        score = addCheck(
                checks,
                score,
                0.07,
                "transparency",
                normalize(
                        evidence.getTransparencyScore()
                ),
                "Transparency"
        );

        weight += 0.07;

        score = addCheck(
                checks,
                score,
                0.09,
                "domain_relevance",
                normalize(
                        evidence.getDomainRelevance()
                ),
                "Domain relevance"
        );

        weight += 0.09;

        double finalScore =
                weight <= 0.0
                        ? 0.0
                        : clamp(
                                score / weight
                        );

        TrustLevel level =
                determineLevel(
                        finalScore,
                        evidence
                );

        String message =
                buildMessage(
                        level,
                        finalScore,
                        checks
                );

        return TrustAssessment.success(
                evidence.getSourceId(),
                finalScore,
                level,
                checks,
                message
        );
    }

    public synchronized TrustAssessment evaluate(
            KnowledgeSource source,
            SourceEvidence evidence
    ) {

        if (source == null) {
            return TrustAssessment.failure(
                    "Knowledge source is missing."
            );
        }

        SourceEvidence effective =
                evidence;

        if (effective == null) {

            effective =
                    SourceEvidence.builder(
                            source.getId()
                    )
                            .sourceName(
                                    source.getName()
                            )
                            .sourceDescription(
                                    source.getDescription()
                            )
                            .sourceType(
                                    source.getType()
                            )
                            .location(
                                    extractLocation(
                                            source
                                    )
                            )
                            .available(
                                    source.isAvailable()
                            )
                            .build();
        }

        return evaluate(
                effective
        );
    }

    public synchronized boolean isTrusted(
            SourceEvidence evidence
    ) {

        TrustAssessment assessment =
                evaluate(evidence);

        return assessment.isTrusted();
    }

    public synchronized boolean canUse(
            SourceEvidence evidence
    ) {

        TrustAssessment assessment =
                evaluate(evidence);

        return assessment.canUse();
    }

    public synchronized int compare(
            SourceEvidence first,
            SourceEvidence second
    ) {

        TrustAssessment a =
                evaluate(first);

        TrustAssessment b =
                evaluate(second);

        if (!a.isSuccess()
                && !b.isSuccess()) {
            return 0;
        }

        if (!a.isSuccess()) {
            return -1;
        }

        if (!b.isSuccess()) {
            return 1;
        }

        return Double.compare(
                a.getScore(),
                b.getScore()
        );
    }

    public synchronized int getDiscoveredCount() {
        return discoveredSources.size();
    }

    public synchronized int getTrustedCount() {

        int count = 0;

        for (TrustAssessment assessment :
                assessments.values()) {

            if (assessment.isTrusted()) {
                count++;
            }
        }

        return count;
    }

    public synchronized void clear() {
        discoveredSources.clear();
        assessments.clear();
    }

    public synchronized String getEngineId() {
        return ENGINE_ID;
    }

    /*
     * ---------------------------------------------------------------------
     * NETWORK
     * ---------------------------------------------------------------------
     */

    private HttpResponse fetch(
            String urlString
    ) {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

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
                    USER_AGENT
            );

            connection.setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml,"
                            + "application/xml;q=0.9,text/*;q=0.8"
            );

            int code =
                    connection.getResponseCode();

            InputStream stream;

            if (code >= 200 && code < 400) {
                stream =
                        connection.getInputStream();
            } else {
                stream =
                        connection.getErrorStream();
            }

            String body =
                    stream == null
                            ? ""
                            : readLimited(
                                    stream,
                                    MAX_RESPONSE_BYTES
                            );

            return new HttpResponse(
                    code >= 200 && code < 400,
                    code,
                    body,
                    code >= 200 && code < 400
                            ? "OK"
                            : "HTTP error " + code
            );

        } catch (Exception e) {

            return new HttpResponse(
                    false,
                    -1,
                    "",
                    safeMessage(e)
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readLimited(
            InputStream input,
            int maxBytes
    ) throws IOException {

        byte[] buffer =
                new byte[8192];

        int total = 0;

        StringBuilder output =
                new StringBuilder();

        while (true) {

            int read =
                    input.read(buffer);

            if (read < 0) {
                break;
            }

            if (total + read > maxBytes) {

                int allowed =
                        maxBytes - total;

                if (allowed > 0) {

                    output.append(
                            new String(
                                    buffer,
                                    0,
                                    allowed,
                                    StandardCharsets.UTF_8
                            )
                    );
                }

                break;
            }

            output.append(
                    new String(
                            buffer,
                            0,
                            read,
                            StandardCharsets.UTF_8
                    )
            );

            total += read;
        }

        return output.toString();
    }

    /*
     * ---------------------------------------------------------------------
     * SEARCH RESULT DISCOVERY
     * ---------------------------------------------------------------------
     */

    private List<DiscoveredSource> parseSearchResults(
            String html
    ) {

        if (isBlank(html)) {
            return Collections.emptyList();
        }

        List<DiscoveredSource> results =
                new ArrayList<>();

        Set<String> seen =
                new HashSet<>();

        Pattern linkPattern =
                Pattern.compile(
                        "<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>"
                                + "(.*?)</a>",
                        Pattern.CASE_INSENSITIVE
                                | Pattern.DOTALL
                );

        Matcher matcher =
                linkPattern.matcher(
                        html
                );

        while (matcher.find()
                && results.size()
                < MAX_SEARCH_RESULTS) {

            String rawUrl =
                    decodeHtml(
                            matcher.group(1)
                    );

            String anchor =
                    stripHtml(
                            matcher.group(2)
                    );

            String url =
                    extractRealResultUrl(
                            rawUrl
                    );

            if (!isValidHttpUrl(url)) {
                continue;
            }

            String normalized =
                    normalizeUrl(url);

            if (seen.contains(normalized)) {
                continue;
            }

            seen.add(normalized);

            String snippet =
                    findNearbySnippet(
                            html,
                            matcher.start(),
                            matcher.end()
                    );

            results.add(
                    new DiscoveredSource(
                            url,
                            clean(anchor),
                            clean(snippet)
                    )
            );
        }

        return results;
    }

    private String extractRealResultUrl(
            String raw
    ) {

        if (isBlank(raw)) {
            return "";
        }

        String value =
                decodeHtml(
                        raw
                );

        try {

            if (value.contains("uddg=")) {

                int index =
                        value.indexOf(
                                "uddg="
                        );

                String encoded =
                        value.substring(
                                index + 5
                        );

                int amp =
                        encoded.indexOf(
                                '&'
                        );

                if (amp >= 0) {
                    encoded =
                            encoded.substring(
                                    0,
                                    amp
                            );
                }

                return URLDecoder.decode(
                        encoded,
                        StandardCharsets.UTF_8.name()
                );
            }

        } catch (Exception ignored) {
        }

        return value;
    }

    private String findNearbySnippet(
            String html,
            int start,
            int end
    ) {

        int from =
                Math.max(
                        0,
                        start - 300
                );

        int to =
                Math.min(
                        html.length(),
                        end + 800
                );

        if (from >= to) {
            return "";
        }

        return stripHtml(
                html.substring(
                        from,
                        to
                )
        );
    }

    /*
     * ---------------------------------------------------------------------
     * SOURCE INSPECTION
     * ---------------------------------------------------------------------
     */

    private SourceProfile inspectSource(
            String url,
            String searchTitle,
            String searchSnippet
    ) {

        if (!isValidHttpUrl(url)) {
            return null;
        }

        HttpResponse response =
                fetch(url);

        if (!response.isSuccess()) {
            return new SourceProfile(
                    createSourceId(url),
                    clean(
                            isBlank(searchTitle)
                                    ? url
                                    : searchTitle
                    ),
                    clean(searchSnippet),
                    url,
                    KnowledgeSource.SourceType.WEB_PAGE,
                    "",
                    false,
                    false,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        String html =
                response.getBody();

        String title =
                firstNonBlank(
                        extractTitle(html),
                        searchTitle,
                        url
                );

        String description =
                firstNonBlank(
                        extractMetaDescription(html),
                        searchSnippet,
                        ""
                );

        String owner =
                extractOwner(html);

        boolean official =
                looksOfficial(
                        url,
                        html,
                        owner
                );

        boolean verifiedOwnership =
                hasStrongOwnershipSignals(
                        html,
                        owner
                );

        double reputation =
                calculateReputation(
                        url,
                        html
                );

        double ownership =
                calculateOwnership(
                        url,
                        html,
                        owner
                );

        double references =
                calculateReferenceQuality(
                        html
                );

        double freshness =
                calculateFreshness(
                        html
                );

        double transparency =
                calculateTransparency(
                        html
                );

        return new SourceProfile(
                createSourceId(url),
                clean(title),
                clean(description),
                url,
                classifyType(
                        url,
                        html
                ),
                owner,
                true,
                official,
                verifiedOwnership,
                ownership,
                reputation,
                references,
                0.0,
                freshness,
                transparency,
                0.70
        );
    }

    private KnowledgeSource.SourceType classifyType(
            String url,
            String html
    ) {

        String lower =
                (url + " " + html)
                        .toLowerCase(
                                Locale.US
                        );

        if (lower.contains("youtube.com")
                || lower.contains("youtu.be")
                || lower.contains("video")) {

            return KnowledgeSource.SourceType.VIDEO;
        }

        if (lower.contains("api")
                || lower.contains("application/json")) {

            return KnowledgeSource.SourceType.API;
        }

        if (lower.contains("documentation")
                || lower.contains("docs.")
                || lower.contains("developer")) {

            return KnowledgeSource.SourceType.DOCUMENTATION;
        }

        return KnowledgeSource.SourceType.WEB_PAGE;
    }

    /*
     * ---------------------------------------------------------------------
     * TRUST SIGNALS
     * ---------------------------------------------------------------------
     */

    private double evaluateIdentity(
            SourceEvidence evidence
    ) {

        double score = 0.0;

        if (!isBlank(evidence.getSourceId())) {
            score += 0.25;
        }

        if (!isBlank(evidence.getSourceName())) {
            score += 0.20;
        }

        if (!isBlank(evidence.getLocation())) {
            score += 0.30;
        }

        if (evidence.isAvailable()) {
            score += 0.25;
        }

        return score;
    }

    private double evaluateOwnership(
            SourceEvidence evidence
    ) {

        double score =
                normalize(
                        evidence.getOwnershipScore()
                );

        if (!isBlank(
                evidence.getOwnerName()
        )) {
            score =
                    Math.max(
                            score,
                            0.40
                    );
        }

        if (evidence.isOfficialOrganization()) {
            score =
                    Math.max(
                            score,
                            0.75
                    );
        }

        if (evidence.isVerifiedOwnership()) {
            score =
                    Math.max(
                            score,
                            0.90
                    );
        }

        return clamp(score);
    }

    private double calculateOwnership(
            String url,
            String html,
            String owner
    ) {

        double score = 0.0;

        if (!isBlank(owner)) {
            score += 0.35;
        }

        if (hasText(
                html,
                "about",
                "contact",
                "organization",
                "company"
        )) {
            score += 0.20;
        }

        if (isHttps(url)) {
            score += 0.10;
        }

        if (hasStrongOwnershipSignals(
                html,
                owner
        )) {
            score += 0.35;
        }

        return clamp(score);
    }

    private double calculateReputation(
            String url,
            String html
    ) {

        double score = 0.20;

        if (isHttps(url)) {
            score += 0.15;
        }

        if (hasText(
                html,
                "privacy",
                "terms",
                "contact"
        )) {
            score += 0.20;
        }

        if (hasText(
                html,
                "citation",
                "references",
                "bibliography",
                "sources"
        )) {
            score += 0.25;
        }

        if (hasText(
                html,
                "official",
                "institution",
                "university",
                "government"
        )) {
            score += 0.20;
        }

        return clamp(score);
    }

    private double calculateReferenceQuality(
            String html
    ) {

        if (isBlank(html)) {
            return 0.0;
        }

        int references =
                countOccurrences(
                        html,
                        "reference"
                );

        int citations =
                countOccurrences(
                        html,
                        "citation"
                );

        int sources =
                countOccurrences(
                        html,
                        "source"
                );

        int total =
                references
                        + citations
                        + sources;

        if (total >= 10) {
            return 0.90;
        }

        if (total >= 5) {
            return 0.75;
        }

        if (total >= 2) {
            return 0.55;
        }

        if (total >= 1) {
            return 0.35;
        }

        return 0.10;
    }

    private double calculateFreshness(
            String html
    ) {

        if (hasText(
                html,
                "dateModified",
                "datePublished",
                "last updated",
                "updated"
        )) {
            return 0.75;
        }

        return 0.45;
    }

    private double calculateTransparency(
            String html
    ) {

        double score = 0.20;

        if (hasText(
                html,
                "about",
                "contact"
        )) {
            score += 0.25;
        }

        if (hasText(
                html,
                "privacy",
                "terms"
        )) {
            score += 0.20;
        }

        if (hasText(
                html,
                "author",
                "publisher"
        )) {
            score += 0.20;
        }

        if (hasText(
                html,
                "references",
                "sources"
        )) {
            score += 0.15;
        }

        return clamp(score);
    }

    private boolean looksOfficial(
            String url,
            String html,
            String owner
    ) {

        String lower =
                url.toLowerCase(
                        Locale.US
                );

        if (lower.endsWith(".gov")
                || lower.contains(".gov.")) {
            return true;
        }

        if (lower.endsWith(".edu")
                || lower.contains(".edu.")) {
            return true;
        }

        return !isBlank(owner)
                && hasText(
                        html,
                        "official",
                        "organization",
                        "institution"
                );
    }

    private boolean hasStrongOwnershipSignals(
            String html,
            String owner
    ) {

        return !isBlank(owner)
                && hasText(
                        html,
                        "about",
                        "contact",
                        "publisher",
                        "organization"
                );
    }

    private String extractOwner(
            String html
    ) {

        String value =
                extractMeta(
                        html,
                        "author"
                );

        if (!isBlank(value)) {
            return value;
        }

        value =
                extractMeta(
                        html,
                        "publisher"
                );

        if (!isBlank(value)) {
            return value;
        }

        return extractMeta(
                html,
                "og:site_name"
        );
    }

    private String extractTitle(
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

        return clean(
                stripHtml(
                        matcher.group(1)
                )
        );
    }

    private String extractMetaDescription(
            String html
    ) {

        String value =
                extractMeta(
                        html,
                        "description"
                );

        if (!isBlank(value)) {
            return value;
        }

        return extractMeta(
                html,
                "og:description"
        );
    }

    private String extractMeta(
            String html,
            String name
    ) {

        Pattern pattern =
                Pattern.compile(
                        "<meta[^>]+(?:name|property)"
                                + "\\s*=\\s*[\"']"
                                + Pattern.quote(name)
                                + "[\"'][^>]+content"
                                + "\\s*=\\s*[\"']([^\"']*)",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                pattern.matcher(
                        html
                );

        if (matcher.find()) {
            return clean(
                    matcher.group(1)
            );
        }

        return "";
    }

    /*
     * ---------------------------------------------------------------------
     * LEVEL DECISION
     * ---------------------------------------------------------------------
     */

    private TrustLevel determineLevel(
            double score,
            SourceEvidence evidence
    ) {

        if (evidence.isExplicitlyRejected()) {
            return TrustLevel.REJECTED;
        }

        if (!evidence.isAvailable()) {
            return TrustLevel.NEEDS_REVIEW;
        }

        if (evidence.getEvidenceCount() < 2
                && score < TRUSTED_THRESHOLD) {

            return TrustLevel.INSUFFICIENT_EVIDENCE;
        }

        if (score >= TRUSTED_THRESHOLD) {
            return TrustLevel.TRUSTED;
        }

        if (score >= ACCEPTED_THRESHOLD) {
            return TrustLevel.ACCEPTED_WITH_REVIEW;
        }

        if (score >= REVIEW_THRESHOLD) {
            return TrustLevel.NEEDS_REVIEW;
        }

        return TrustLevel.REJECTED;
    }

    private double addCheck(
            List<TrustCheck> checks,
            double currentScore,
            double weight,
            String id,
            double value,
            String description
    ) {

        double normalized =
                clamp(value);

        checks.add(
                new TrustCheck(
                        id,
                        description,
                        normalized,
                        weight,
                        determineCheckLevel(
                                normalized
                        )
                )
        );

        return currentScore
                + normalized * weight;
    }

    private CheckLevel determineCheckLevel(
            double value
    ) {

        if (value >= 0.80) {
            return CheckLevel.STRONG;
        }

        if (value >= 0.60) {
            return CheckLevel.PASS;
        }

        if (value >= 0.40) {
            return CheckLevel.WARNING;
        }

        return CheckLevel.FAIL;
    }

    private String buildMessage(
            TrustLevel level,
            double score,
            List<TrustCheck> checks
    ) {

        int warnings = 0;
        int failures = 0;

        for (TrustCheck check : checks) {

            if (check.getLevel()
                    == CheckLevel.WARNING) {
                warnings++;
            }

            if (check.getLevel()
                    == CheckLevel.FAIL) {
                failures++;
            }
        }

        String state;

        if (failures > 0) {
            state =
                    "Some trust signals failed.";
        } else if (warnings > 0) {
            state =
                    "Additional verification is recommended.";
        } else {
            state =
                    "Available trust signals are consistent.";
        }

        return limit(
                "Trust level: "
                        + level.name()
                        + ". Score: "
                        + String.format(
                                Locale.US,
                                "%.2f",
                                score
                        )
                        + ". "
                        + state,
                500
        );
    }

    /*
     * ---------------------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------------------
     */

    private String extractLocation(
            KnowledgeSource source
    ) {

        try {

            Map<String, Object> metadata =
                    source.getMetadata();

            if (metadata != null) {

                Object location =
                        metadata.get(
                                "location"
                        );

                if (location != null) {
                    return String.valueOf(
                            location
                    );
                }

                Object url =
                        metadata.get(
                                "url"
                        );

                if (url != null) {
                    return String.valueOf(
                            url
                    );
                }
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private String createSourceId(
            String url
    ) {

        String normalized =
                normalizeUrl(url);

        return "web_"
                + Integer.toHexString(
                        normalized.hashCode()
                );
    }

    private boolean isValidHttpUrl(
            String value
    ) {

        if (isBlank(value)) {
            return false;
        }

        try {

            URI uri =
                    new URI(
                            value.trim()
                    );

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            return ("http".equalsIgnoreCase(
                    scheme
            )
                    || "https".equalsIgnoreCase(
                            scheme
                    ))
                    && !isBlank(host);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHttps(
            String url
    ) {

        return !isBlank(url)
                && url
                .toLowerCase(
                        Locale.US
                )
                .startsWith(
                        "https://"
                );
    }

    private String normalizeUrl(
            String url
    ) {

        if (isBlank(url)) {
            return "";
        }

        String value =
                url.trim();

        while (value.endsWith("/")) {
            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value.toLowerCase(
                Locale.US
        );
    }

    private String urlEncode(
            String value
    ) {

        try {

            return java.net.URLEncoder
                    .encode(
                            value,
                            StandardCharsets.UTF_8.name()
                    )
                    .replace(
                            "+",
                            "%20"
                    );

        } catch (Exception e) {
            return value;
        }
    }

    private String decodeHtml(
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

    private String stripHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll(
                        "<[^>]+>",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return stripHtml(
                decodeHtml(
                        value
                )
        ).trim();
    }

    private String firstNonBlank(
            String... values
    ) {

        if (values == null) {
            return "";
        }

        for (String value : values) {

            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private boolean hasText(
            String html,
            String... values
    ) {

        if (isBlank(html)
                || values == null) {
            return false;
        }

        String lower =
                html.toLowerCase(
                        Locale.US
                );

        for (String value : values) {

            if (!isBlank(value)
                    && lower.contains(
                            value.toLowerCase(
                                    Locale.US
                            )
                    )) {

                return true;
            }
        }

        return false;
    }

    private int countOccurrences(
            String text,
            String value
    ) {

        if (isBlank(text)
                || isBlank(value)) {
            return 0;
        }

        String lowerText =
                text.toLowerCase(
                        Locale.US
                );

        String lowerValue =
                value.toLowerCase(
                        Locale.US
                );

        int count = 0;
        int index = 0;

        while (true) {

            index =
                    lowerText.indexOf(
                            lowerValue,
                            index
                    );

            if (index < 0) {
                break;
            }

            count++;
            index +=
                    lowerValue.length();
        }

        return count;
    }

    private double normalize(
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        if (value > 1.0
                && value <= 100.0) {

            return value / 100.0;
        }

        return clamp(value);
    }

    private double clamp(
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String limit(
            String value,
            int max
    ) {

        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(
                0,
                max
        );
    }

    private String safeMessage(
            Exception e
    ) {

        if (e == null
                || e.getMessage() == null) {
            return "Unknown network error.";
        }

        return limit(
                e.getMessage(),
                300
        );
    }

    /*
     * ---------------------------------------------------------------------
     * DATA CLASSES
     * ---------------------------------------------------------------------
     */

    public enum TrustLevel {
        TRUSTED,
        ACCEPTED_WITH_REVIEW,
        NEEDS_REVIEW,
        REJECTED,
        INSUFFICIENT_EVIDENCE
    }

    public enum CheckLevel {
        STRONG,
        PASS,
        WARNING,
        FAIL
    }

    public static final class NetworkStatus {

        private final boolean online;
        private final int responseCode;
        private final String message;

        public NetworkStatus(
                boolean online,
                int responseCode,
                String message
        ) {

            this.online = online;
            this.responseCode = responseCode;
            this.message = message;
        }

        public boolean isOnline() {
            return online;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class DiscoveryResult {

        private final boolean success;
        private final String query;
        private final List<SourceProfile> sources;
        private final String message;

        private DiscoveryResult(
                boolean success,
                String query,
                List<SourceProfile> sources,
                String message
        ) {

            this.success = success;
            this.query = query;

            this.sources =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    sources == null
                                            ? Collections.emptyList()
                                            : sources
                            )
                    );

            this.message = message;
        }

        public static DiscoveryResult success(
                String query,
                List<SourceProfile> sources
        ) {

            return new DiscoveryResult(
                    true,
                    query,
                    sources,
                    "Source discovery completed."
            );
        }

        public static DiscoveryResult failure(
                String message
        ) {

            return new DiscoveryResult(
                    false,
                    "",
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

        public List<SourceProfile> getSources() {
            return sources;
        }

        public int getSourceCount() {
            return sources.size();
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class SourceProfile {

        private final String id;
        private final String name;
        private final String description;
        private final String url;
        private final KnowledgeSource.SourceType type;
        private final String owner;

        private final boolean available;
        private final boolean official;
        private final boolean verifiedOwnership;

        private final double ownershipScore;
        private final double reputationScore;
        private final double referenceQuality;
        private final double independentCorroboration;
        private final double freshnessScore;
        private final double transparencyScore;
        private final double domainRelevance;

        public SourceProfile(
                String id,
                String name,
                String description,
                String url,
                KnowledgeSource.SourceType type,
                String owner,
                boolean available,
                boolean official,
                boolean verifiedOwnership,
                double ownershipScore,
                double reputationScore,
                double referenceQuality,
                double independentCorroboration,
                double freshnessScore,
                double transparencyScore,
                double domainRelevance
        ) {

            this.id = id;
            this.name = name;
            this.description = description;
            this.url = url;
            this.type = type;
            this.owner = owner;

            this.available = available;
            this.official = official;
            this.verifiedOwnership =
                    verifiedOwnership;

            this.ownershipScore =
                    ownershipScore;
            this.reputationScore =
                    reputationScore;
            this.referenceQuality =
                    referenceQuality;
            this.independentCorroboration =
                    independentCorroboration;
            this.freshnessScore =
                    freshnessScore;
            this.transparencyScore =
                    transparencyScore;
            this.domainRelevance =
                    domainRelevance;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUrl() {
            return url;
        }

        public KnowledgeSource.SourceType getType() {
            return type;
        }

        public String getOwner() {
            return owner;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isOfficial() {
            return official;
        }

        public boolean isVerifiedOwnership() {
            return verifiedOwnership;
        }

        public double getOwnershipScore() {
            return ownershipScore;
        }

        public double getReputationScore() {
            return reputationScore;
        }

        public double getReferenceQuality() {
            return referenceQuality;
        }

        public double getIndependentCorroboration() {
            return independentCorroboration;
        }

        public double getFreshnessScore() {
            return freshnessScore;
        }

        public double getTransparencyScore() {
            return transparencyScore;
        }

        public double getDomainRelevance() {
            return domainRelevance;
        }

        public SourceEvidence toEvidence() {

            return SourceEvidence.builder(
                    id
            )
                    .sourceName(name)
                    .sourceDescription(
                            description
                    )
                    .sourceType(type)
                    .location(url)
                    .ownerName(owner)
                    .available(available)
                    .officialOrganization(
                            official
                    )
                    .verifiedOwnership(
                            verifiedOwnership
                    )
                    .ownershipScore(
                            ownershipScore
                    )
                    .reputationScore(
                            reputationScore
                    )
                    .referenceQuality(
                            referenceQuality
                    )
                    .independentCorroboration(
                            independentCorroboration
                    )
                    .freshnessScore(
                            freshnessScore
                    )
                    .transparencyScore(
                            transparencyScore
                    )
                    .domainRelevance(
                            domainRelevance
                    )
                    .evidenceCount(2)
                    .addEvidence(
                            "url",
                            url
                    )
                    .addEvidence(
                            "owner",
                            owner
                    )
                    .build();
        }
    }

    public static final class RevalidationResult {

        private final int checked;
        private final int available;
        private final int trusted;
        private final int rejected;

        public RevalidationResult(
                int checked,
                int available,
                int trusted,
                int rejected
        ) {

            this.checked = checked;
            this.available = available;
            this.trusted = trusted;
            this.rejected = rejected;
        }

        public int getChecked() {
            return checked;
        }

        public int getAvailable() {
            return available;
        }

        public int getTrusted() {
            return trusted;
        }

        public int getRejected() {
            return rejected;
        }
    }

    private static final class DiscoveredSource {

        private final String url;
        private final String title;
        private final String snippet;

        private DiscoveredSource(
                String url,
                String title,
                String snippet
        ) {

            this.url = url;
            this.title = title;
            this.snippet = snippet;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public String getSnippet() {
            return snippet;
        }
    }

    private static final class HttpResponse {

        private final boolean success;
        private final int code;
        private final String body;
        private final String message;

        private HttpResponse(
                boolean success,
                int code,
                String body,
                String message
        ) {

            this.success = success;
            this.code = code;
            this.body = body;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getCode() {
            return code;
        }

        public String getBody() {
            return body;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class TrustCheck {

        private final String id;
        private final String description;
        private final double value;
        private final double weight;
        private final CheckLevel level;

        public TrustCheck(
                String id,
                String description,
                double value,
                double weight,
                CheckLevel level
        ) {

            this.id = id;
            this.description = description;
            this.value = value;
            this.weight = weight;
            this.level = level;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public double getValue() {
            return value;
        }

        public double getWeight() {
            return weight;
        }

        public CheckLevel getLevel() {
            return level;
        }
    }

    public static final class TrustAssessment {

        private final boolean success;
        private final String sourceId;
        private final double score;
        private final TrustLevel level;
        private final List<TrustCheck> checks;
        private final String message;

        private TrustAssessment(
                boolean success,
                String sourceId,
                double score,
                TrustLevel level,
                List<TrustCheck> checks,
                String message
        ) {

            this.success = success;
            this.sourceId = sourceId;
            this.score = score;
            this.level = level;

            this.checks =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    checks == null
                                            ? Collections.emptyList()
                                            : checks
                            )
                    );

            this.message = message;
        }

        public static TrustAssessment success(
                String sourceId,
                double score,
                TrustLevel level,
                List<TrustCheck> checks,
                String message
        ) {

            return new TrustAssessment(
                    true,
                    sourceId,
                    score,
                    level,
                    checks,
                    message
            );
        }

        public static TrustAssessment failure(
                String message
        ) {

            return new TrustAssessment(
                    false,
                    "",
                    0.0,
                    TrustLevel.INSUFFICIENT_EVIDENCE,
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

        public double getScore() {
            return score;
        }

        public TrustLevel getLevel() {
            return level;
        }

        public List<TrustCheck> getChecks() {
            return checks;
        }

        public String getMessage() {
            return message;
        }

        public boolean isTrusted() {

            return success
                    && level
                    == TrustLevel.TRUSTED;
        }

        public boolean canUse() {

            return success
                    && (
                    level
                            == TrustLevel.TRUSTED
                            || level
                            == TrustLevel.ACCEPTED_WITH_REVIEW
            );
        }

        public boolean needsReview() {

            return level
                    == TrustLevel.NEEDS_REVIEW
                    || level
                    == TrustLevel.INSUFFICIENT_EVIDENCE;
        }
    }

    public static final class SourceEvidence {

        private final String sourceId;
        private final String sourceName;
        private final String sourceDescription;
        private final KnowledgeSource.SourceType sourceType;
        private final String location;
        private final String ownerName;

        private final boolean available;
        private final boolean officialOrganization;
        private final boolean verifiedOwnership;
        private final boolean explicitlyRejected;

        private final double ownershipScore;
        private final double reputationScore;
        private final double referenceQuality;
        private final double independentCorroboration;
        private final double freshnessScore;
        private final double transparencyScore;
        private final double domainRelevance;

        private final int evidenceCount;
        private final Map<String, String> evidence;

        private SourceEvidence(
                Builder builder
        ) {

            this.sourceId =
                    builder.sourceId;

            this.sourceName =
                    builder.sourceName;

            this.sourceDescription =
                    builder.sourceDescription;

            this.sourceType =
                    builder.sourceType;

            this.location =
                    builder.location;

            this.ownerName =
                    builder.ownerName;

            this.available =
                    builder.available;

            this.officialOrganization =
                    builder.officialOrganization;

            this.verifiedOwnership =
                    builder.verifiedOwnership;

            this.explicitlyRejected =
                    builder.explicitlyRejected;

            this.ownershipScore =
                    builder.ownershipScore;

            this.reputationScore =
                    builder.reputationScore;

            this.referenceQuality =
                    builder.referenceQuality;

            this.independentCorroboration =
                    builder.independentCorroboration;

            this.freshnessScore =
                    builder.freshnessScore;

            this.transparencyScore =
                    builder.transparencyScore;

            this.domainRelevance =
                    builder.domainRelevance;

            this.evidence =
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    builder.evidence
                            )
                    );

            this.evidenceCount =
                    Math.max(
                            builder.evidenceCount,
                            evidence.size()
                    );
        }

        public static Builder builder(
                String sourceId
        ) {

            return new Builder(
                    sourceId
            );
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getSourceName() {
            return sourceName;
        }

        public String getSourceDescription() {
            return sourceDescription;
        }

        public KnowledgeSource.SourceType getSourceType() {
            return sourceType;
        }

        public String getLocation() {
            return location;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isOfficialOrganization() {
            return officialOrganization;
        }

        public boolean isVerifiedOwnership() {
            return verifiedOwnership;
        }

        public boolean isExplicitlyRejected() {
            return explicitlyRejected;
        }

        public double getOwnershipScore() {
            return ownershipScore;
        }

        public double getReputationScore() {
            return reputationScore;
        }

        public double getReferenceQuality() {
            return referenceQuality;
        }

        public double getIndependentCorroboration() {
            return independentCorroboration;
        }

        public double getFreshnessScore() {
            return freshnessScore;
        }

        public double getTransparencyScore() {
            return transparencyScore;
        }

        public double getDomainRelevance() {
            return domainRelevance;
        }

        public int getEvidenceCount() {
            return evidenceCount;
        }

        public Map<String, String> getEvidence() {
            return evidence;
        }

        public static final class Builder {

            private final String sourceId;

            private String sourceName = "";
            private String sourceDescription = "";

            private KnowledgeSource.SourceType sourceType =
                    KnowledgeSource.SourceType.OTHER;

            private String location = "";
            private String ownerName = "";

            private boolean available = true;
            private boolean officialOrganization;
            private boolean verifiedOwnership;
            private boolean explicitlyRejected;

            private double ownershipScore;
            private double reputationScore;
            private double referenceQuality;
            private double independentCorroboration;
            private double freshnessScore;
            private double transparencyScore;
            private double domainRelevance;

            private int evidenceCount;

            private final Map<String, String> evidence =
                    new HashMap<>();

            private Builder(
                    String sourceId
            ) {

                this.sourceId =
                        sourceId;
            }

            public Builder sourceName(
                    String value
            ) {

                this.sourceName =
                        value;

                return this;
            }

            public Builder sourceDescription(
                    String value
            ) {

                this.sourceDescription =
                        value;

                return this;
            }

            public Builder sourceType(
                    KnowledgeSource.SourceType value
            ) {

                if (value != null) {
                    this.sourceType =
                            value;
                }

                return this;
            }

            public Builder location(
                    String value
            ) {

                this.location =
                        value;

                return this;
            }

            public Builder ownerName(
                    String value
            ) {

                this.ownerName =
                        value;

                return this;
            }

            public Builder available(
                    boolean value
            ) {

                this.available =
                        value;

                return this;
            }

            public Builder officialOrganization(
                    boolean value
            ) {

                this.officialOrganization =
                        value;

                return this;
            }

            public Builder verifiedOwnership(
                    boolean value
            ) {

                this.verifiedOwnership =
                        value;

                return this;
            }

            public Builder explicitlyRejected(
                    boolean value
            ) {

                this.explicitlyRejected =
                        value;

                return this;
            }

            public Builder ownershipScore(
                    double value
            ) {

                this.ownershipScore =
                        value;

                return this;
            }

            public Builder reputationScore(
                    double value
            ) {

                this.reputationScore =
                        value;

                return this;
            }

            public Builder referenceQuality(
                    double value
            ) {

                this.referenceQuality =
                        value;

                return this;
            }

            public Builder independentCorroboration(
                    double value
            ) {

                this.independentCorroboration =
                        value;

                return this;
            }

            public Builder freshnessScore(
                    double value
            ) {

                this.freshnessScore =
                        value;

                return this;
            }

            public Builder transparencyScore(
                    double value
            ) {

                this.transparencyScore =
                        value;

                return this;
            }

            public Builder domainRelevance(
                    double value
            ) {

                this.domainRelevance =
                        value;

                return this;
            }

            public Builder evidenceCount(
                    int value
            ) {

                this.evidenceCount =
                        Math.max(
                                0,
                                value
                        );

                return this;
            }

            public Builder addEvidence(
                    String key,
                    String value
            ) {

                if (!isBlankStatic(key)
                        && value != null) {

                    evidence.put(
                            key,
                            value
                    );
                }

                return this;
            }

            public SourceEvidence build() {

                return new SourceEvidence(
                        this
                );
            }

            private static boolean isBlankStatic(
                    String value
            ) {

                return value == null
                        || value.trim().isEmpty();
            }
        }
    }
}