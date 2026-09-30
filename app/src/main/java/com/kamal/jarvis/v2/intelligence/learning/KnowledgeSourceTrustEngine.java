package com.kamal.jarvis.v2.intelligence.learning;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates the trustworthiness of knowledge sources.
 *
 * This engine does not maintain a hard-coded "trusted sources" list.
 * Instead, it evaluates available evidence and produces a trust assessment.
 *
 * The result can be:
 * - TRUSTED
 * - ACCEPTED_WITH_REVIEW
 * - NEEDS_REVIEW
 * - REJECTED
 * - INSUFFICIENT_EVIDENCE
 *
 * The engine is intentionally conservative:
 * lack of evidence is not treated as proof of trust.
 */
public final class KnowledgeSourceTrustEngine {

    private static final String ENGINE_ID = "v2.knowledge_source_trust";

    private static final double TRUSTED_THRESHOLD = 0.80;
    private static final double ACCEPTED_THRESHOLD = 0.65;
    private static final double REVIEW_THRESHOLD = 0.45;

    private static final int MAX_REASON_LENGTH = 500;

    public synchronized TrustAssessment evaluate(SourceEvidence evidence) {
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

        List<TrustCheck> checks = new ArrayList<>();

        double score = 0.0;
        double weight = 0.0;

        score = addCheck(
                checks,
                score,
                0.18,
                "source_identity",
                evaluateIdentity(evidence),
                "Source identity and location"
        );
        weight += 0.18;

        score = addCheck(
                checks,
                score,
                0.18,
                "ownership",
                evaluateOwnership(evidence),
                "Ownership or responsible organization"
        );
        weight += 0.18;

        score = addCheck(
                checks,
                score,
                0.16,
                "reputation",
                normalize(evidence.getReputationScore()),
                "Reputation evidence"
        );
        weight += 0.16;

        score = addCheck(
                checks,
                score,
                0.14,
                "references",
                normalize(evidence.getReferenceQuality()),
                "References and supporting evidence"
        );
        weight += 0.14;

        score = addCheck(
                checks,
                score,
                0.12,
                "independence",
                normalize(evidence.getIndependentCorroboration()),
                "Independent corroboration"
        );
        weight += 0.12;

        score = addCheck(
                checks,
                score,
                0.08,
                "freshness",
                normalize(evidence.getFreshnessScore()),
                "Freshness and update history"
        );
        weight += 0.08;

        score = addCheck(
                checks,
                score,
                0.07,
                "transparency",
                normalize(evidence.getTransparencyScore()),
                "Transparency"
        );
        weight += 0.07;

        score = addCheck(
                checks,
                score,
                0.07,
                "domain_relevance",
                normalize(evidence.getDomainRelevance()),
                "Domain relevance"
        );
        weight += 0.07;

        double finalScore = weight <= 0.0
                ? 0.0
                : clamp(score / weight);

        TrustLevel level = determineLevel(
                finalScore,
                evidence
        );

        String message = buildMessage(
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

        SourceEvidence effectiveEvidence = evidence;

        if (effectiveEvidence == null) {
            effectiveEvidence = SourceEvidence.builder(
                    source.getId()
            )
                    .sourceName(source.getName())
                    .sourceDescription(source.getDescription())
                    .sourceType(source.getType())
                    .location(extractLocation(source))
                    .available(source.isAvailable())
                    .build();
        }

        return evaluate(effectiveEvidence);
    }

    public synchronized boolean isTrusted(
            SourceEvidence evidence
    ) {
        TrustAssessment assessment = evaluate(evidence);

        return assessment.isSuccess()
                && assessment.getLevel() == TrustLevel.TRUSTED;
    }

    public synchronized boolean canUse(
            SourceEvidence evidence
    ) {
        TrustAssessment assessment = evaluate(evidence);

        if (!assessment.isSuccess()) {
            return false;
        }

        return assessment.getLevel() == TrustLevel.TRUSTED
                || assessment.getLevel() == TrustLevel.ACCEPTED_WITH_REVIEW;
    }

    public synchronized int compare(
            SourceEvidence first,
            SourceEvidence second
    ) {
        TrustAssessment firstAssessment = evaluate(first);
        TrustAssessment secondAssessment = evaluate(second);

        if (!firstAssessment.isSuccess()
                && !secondAssessment.isSuccess()) {
            return 0;
        }

        if (!firstAssessment.isSuccess()) {
            return -1;
        }

        if (!secondAssessment.isSuccess()) {
            return 1;
        }

        return Double.compare(
                firstAssessment.getScore(),
                secondAssessment.getScore()
        );
    }

    public synchronized String getEngineId() {
        return ENGINE_ID;
    }

    private double addCheck(
            List<TrustCheck> checks,
            double currentScore,
            double weight,
            String id,
            double value,
            String description
    ) {
        double normalized = clamp(value);

        checks.add(
                new TrustCheck(
                        id,
                        description,
                        normalized,
                        weight,
                        determineCheckLevel(normalized)
                )
        );

        return currentScore + (normalized * weight);
    }

    private double evaluateIdentity(
            SourceEvidence evidence
    ) {
        double score = 0.0;

        if (!isBlank(evidence.getSourceId())) {
            score += 0.25;
        }

        if (!isBlank(evidence.getSourceName())) {
            score += 0.25;
        }

        if (!isBlank(evidence.getLocation())) {
            score += 0.25;
        }

        if (evidence.isAvailable()) {
            score += 0.25;
        }

        return score;
    }

    private double evaluateOwnership(
            SourceEvidence evidence
    ) {
        double score = normalize(
                evidence.getOwnershipScore()
        );

        if (!isBlank(evidence.getOwnerName())) {
            score = Math.max(score, 0.35);
        }

        if (evidence.isOfficialOrganization()) {
            score = Math.max(score, 0.75);
        }

        if (evidence.isVerifiedOwnership()) {
            score = Math.max(score, 0.90);
        }

        return clamp(score);
    }

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
        StringBuilder message = new StringBuilder();

        message.append("Trust level: ")
                .append(level.name())
                .append(". ");

        message.append("Score: ")
                .append(String.format(
                        Locale.US,
                        "%.2f",
                        score
                ))
                .append(". ");

        int warnings = 0;
        int failures = 0;

        for (TrustCheck check : checks) {
            if (check.getLevel() == CheckLevel.WARNING) {
                warnings++;
            }

            if (check.getLevel() == CheckLevel.FAIL) {
                failures++;
            }
        }

        if (failures > 0) {
            message.append("Some trust checks failed.");
        } else if (warnings > 0) {
            message.append("Additional verification is recommended.");
        } else {
            message.append("Available evidence is consistent with the assessment.");
        }

        return limit(
                message.toString(),
                MAX_REASON_LENGTH
        );
    }

    private String extractLocation(
            KnowledgeSource source
    ) {
        try {
            Map<String, Object> metadata =
                    source.getMetadata();

            if (metadata != null) {
                Object location = metadata.get("location");

                if (location != null) {
                    return String.valueOf(location);
                }

                Object url = metadata.get("url");

                if (url != null) {
                    return String.valueOf(url);
                }
            }
        } catch (Exception ignored) {
            // Metadata is optional.
        }

        return "";
    }

    private double normalize(
            double value
    ) {
        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        if (value > 1.0 && value <= 100.0) {
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
                Math.min(1.0, value)
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

        return value.substring(0, max);
    }

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

            this.checks = checks == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(
                            new ArrayList<>(checks)
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
                    && level == TrustLevel.TRUSTED;
        }

        public boolean canUse() {
            return success
                    && (level == TrustLevel.TRUSTED
                    || level == TrustLevel.ACCEPTED_WITH_REVIEW);
        }

        public boolean needsReview() {
            return level == TrustLevel.NEEDS_REVIEW
                    || level == TrustLevel.INSUFFICIENT_EVIDENCE;
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
            this.sourceId = builder.sourceId;
            this.sourceName = builder.sourceName;
            this.sourceDescription = builder.sourceDescription;
            this.sourceType = builder.sourceType;
            this.location = builder.location;
            this.ownerName = builder.ownerName;

            this.available = builder.available;
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
                            new HashMap<>(builder.evidence)
                    );

            this.evidenceCount =
                    Math.max(
                            builder.evidenceCount,
                            this.evidence.size()
                    );
        }

        public static Builder builder(
                String sourceId
        ) {
            return new Builder(sourceId);
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
            private boolean officialOrganization = false;
            private boolean verifiedOwnership = false;
            private boolean explicitlyRejected = false;

            private double ownershipScore = 0.0;
            private double reputationScore = 0.0;
            private double referenceQuality = 0.0;
            private double independentCorroboration = 0.0;
            private double freshnessScore = 0.0;
            private double transparencyScore = 0.0;
            private double domainRelevance = 0.0;

            private int evidenceCount = 0;

            private final Map<String, String> evidence =
                    new HashMap<>();

            private Builder(
                    String sourceId
            ) {
                this.sourceId = sourceId;
            }

            public Builder sourceName(
                    String value
            ) {
                this.sourceName = value;
                return this;
            }

            public Builder sourceDescription(
                    String value
            ) {
                this.sourceDescription = value;
                return this;
            }

            public Builder sourceType(
                    KnowledgeSource.SourceType value
            ) {
                if (value != null) {
                    this.sourceType = value;
                }

                return this;
            }

            public Builder location(
                    String value
            ) {
                this.location = value;
                return this;
            }

            public Builder ownerName(
                    String value
            ) {
                this.ownerName = value;
                return this;
            }

            public Builder available(
                    boolean value
            ) {
                this.available = value;
                return this;
            }

            public Builder officialOrganization(
                    boolean value
            ) {
                this.officialOrganization = value;
                return this;
            }

            public Builder verifiedOwnership(
                    boolean value
            ) {
                this.verifiedOwnership = value;
                return this;
            }

            public Builder explicitlyRejected(
                    boolean value
            ) {
                this.explicitlyRejected = value;
                return this;
            }

            public Builder ownershipScore(
                    double value
            ) {
                this.ownershipScore = value;
                return this;
            }

            public Builder reputationScore(
                    double value
            ) {
                this.reputationScore = value;
                return this;
            }

            public Builder referenceQuality(
                    double value
            ) {
                this.referenceQuality = value;
                return this;
            }

            public Builder independentCorroboration(
                    double value
            ) {
                this.independentCorroboration = value;
                return this;
            }

            public Builder freshnessScore(
                    double value
            ) {
                this.freshnessScore = value;
                return this;
            }

            public Builder transparencyScore(
                    double value
            ) {
                this.transparencyScore = value;
                return this;
            }

            public Builder domainRelevance(
                    double value
            ) {
                this.domainRelevance = value;
                return this;
            }

            public Builder evidenceCount(
                    int value
            ) {
                this.evidenceCount =
                        Math.max(0, value);
                return this;
            }

            public Builder addEvidence(
                    String key,
                    String value
            ) {
                if (key != null
                        && !key.trim().isEmpty()
                        && value != null) {
                    evidence.put(
                            key,
                            value
                    );
                }

                return this;
            }

            public SourceEvidence build() {
                return new SourceEvidence(this);
            }
        }
    }
}