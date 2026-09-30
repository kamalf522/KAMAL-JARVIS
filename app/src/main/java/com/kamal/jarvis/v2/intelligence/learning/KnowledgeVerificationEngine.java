package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JARVIS V2
 *
 * KnowledgeVerificationEngine
 *
 * مسؤول عن فحص المعرفة المكتسبة قبل اعتمادها.
 *
 * المسار:
 *
 * KnowledgeItem
 *      ↓
 * Normalize
 *      ↓
 * Compare Sources
 *      ↓
 * Detect Conflicts
 *      ↓
 * Calculate Confidence
 *      ↓
 * VERIFIED / NEEDS_REVIEW / REJECTED
 *
 * هذا المحرك لا يفترض أن مصدر واحد صحيح دائماً.
 */
public final class KnowledgeVerificationEngine {

    private static final String ENGINE_ID =
            "v2.knowledge_verification";

    private static final double VERIFIED_THRESHOLD = 0.75;
    private static final double REVIEW_THRESHOLD = 0.40;

    /**
     * التحقق من مجموعة من المعلومات.
     */
    public VerificationReport verify(
            List<KnowledgeItem> items
    ) {

        if (items == null || items.isEmpty()) {

            return VerificationReport.failure(
                    "No knowledge items were provided."
            );
        }

        List<KnowledgeItem> usable =
                new ArrayList<>();

        for (KnowledgeItem item : items) {

            if (item == null) {
                continue;
            }

            if (!isValidContent(item)) {
                continue;
            }

            usable.add(item);
        }

        if (usable.isEmpty()) {

            return VerificationReport.failure(
                    "No usable knowledge items were found."
            );
        }

        Map<String, List<KnowledgeItem>> groups =
                groupByTopic(usable);

        List<KnowledgeItem> verifiedItems =
                new ArrayList<>();

        List<KnowledgeItem> reviewItems =
                new ArrayList<>();

        List<KnowledgeItem> rejectedItems =
                new ArrayList<>();

        List<VerificationCheck> checks =
                new ArrayList<>();

        for (Map.Entry<String, List<KnowledgeItem>> entry
                : groups.entrySet()) {

            List<KnowledgeItem> group =
                    entry.getValue();

            VerificationDecision decision =
                    verifyGroup(group);

            checks.addAll(
                    decision.getChecks()
            );

            verifiedItems.addAll(
                    decision.getVerifiedItems()
            );

            reviewItems.addAll(
                    decision.getReviewItems()
            );

            rejectedItems.addAll(
                    decision.getRejectedItems()
            );
        }

        return VerificationReport.success(
                verifiedItems,
                reviewItems,
                rejectedItems,
                checks
        );
    }

    /**
     * التحقق من عنصر واحد.
     *
     * عنصر واحد لا يحصل تلقائياً على
     * ثقة عالية لأنه لا توجد مقارنة كافية.
     */
    public VerificationReport verifySingle(
            KnowledgeItem item
    ) {

        if (!isValidContent(item)) {

            return VerificationReport.failure(
                    "Knowledge item is invalid."
            );
        }

        List<VerificationCheck> checks =
                new ArrayList<>();

        double confidence =
                calculateSingleSourceConfidence(
                        item,
                        checks
                );

        KnowledgeItem result;

        if (confidence >= VERIFIED_THRESHOLD) {

            result = item
                    .withStatus(
                            KnowledgeItem.KnowledgeStatus.VERIFIED
                    )
                    .withConfidence(confidence);

        } else if (confidence >= REVIEW_THRESHOLD) {

            result = item
                    .withStatus(
                            KnowledgeItem.KnowledgeStatus.NEEDS_REVIEW
                    )
                    .withConfidence(confidence);

        } else {

            result = item
                    .withStatus(
                            KnowledgeItem.KnowledgeStatus.REJECTED
                    )
                    .withConfidence(confidence);
        }

        List<KnowledgeItem> verified =
                new ArrayList<>();

        List<KnowledgeItem> review =
                new ArrayList<>();

        List<KnowledgeItem> rejected =
                new ArrayList<>();

        if (result.isVerified()) {
            verified.add(result);
        } else if (
                result.getStatus()
                        == KnowledgeItem.KnowledgeStatus.NEEDS_REVIEW
        ) {
            review.add(result);
        } else {
            rejected.add(result);
        }

        return VerificationReport.success(
                verified,
                review,
                rejected,
                checks
        );
    }

    /**
     * التحقق من مجموعة معلومات حول نفس الموضوع.
     */
    private VerificationDecision verifyGroup(
            List<KnowledgeItem> group
    ) {

        List<KnowledgeItem> verified =
                new ArrayList<>();

        List<KnowledgeItem> review =
                new ArrayList<>();

        List<KnowledgeItem> rejected =
                new ArrayList<>();

        List<VerificationCheck> checks =
                new ArrayList<>();

        if (group == null || group.isEmpty()) {

            return new VerificationDecision(
                    verified,
                    review,
                    rejected,
                    checks
            );
        }

        int independentSources =
                countIndependentSources(group);

        boolean conflicting =
                hasConflict(group);

        double agreement =
                calculateAgreement(group);

        for (KnowledgeItem item : group) {

            double confidence =
                    calculateConfidence(
                            item,
                            independentSources,
                            agreement,
                            conflicting,
                            checks
                    );

            KnowledgeItem updated;

            if (confidence >= VERIFIED_THRESHOLD
                    && !conflicting) {

                updated = item
                        .withStatus(
                                KnowledgeItem.KnowledgeStatus.VERIFIED
                        )
                        .withConfidence(
                                confidence
                        );

                verified.add(updated);

            } else if (
                    confidence >= REVIEW_THRESHOLD
            ) {

                updated = item
                        .withStatus(
                                KnowledgeItem.KnowledgeStatus.NEEDS_REVIEW
                        )
                        .withConfidence(
                                confidence
                        );

                review.add(updated);

            } else {

                updated = item
                        .withStatus(
                                KnowledgeItem.KnowledgeStatus.REJECTED
                        )
                        .withConfidence(
                                confidence
                        );

                rejected.add(updated);
            }
        }

        return new VerificationDecision(
                verified,
                review,
                rejected,
                checks
        );
    }

    /**
     * حساب الثقة.
     */
    private double calculateConfidence(
            KnowledgeItem item,
            int independentSources,
            double agreement,
            boolean conflicting,
            List<VerificationCheck> checks
    ) {

        double score = 0.0;

        /*
         * جودة المحتوى.
         */
        if (isValidContent(item)) {

            score += 0.20;

            checks.add(
                    VerificationCheck.pass(
                            item.getId(),
                            "CONTENT_VALID",
                            "Content is usable."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.fail(
                            item.getId(),
                            "CONTENT_VALID",
                            "Content is empty or invalid."
                    )
            );

            return 0.0;
        }

        /*
         * وجود مصدر.
         */
        if (hasSource(item)) {

            score += 0.15;

            checks.add(
                    VerificationCheck.pass(
                            item.getId(),
                            "SOURCE_PRESENT",
                            "A source is attached."
                    )
            );

        } else {

            checks.add(
                    VerificationCheck.fail(
                            item.getId(),
                            "SOURCE_PRESENT",
                            "No source is attached."
                    )
            );
        }

        /*
         * تعدد المصادر المستقلة.
         */
        if (independentSources >= 3) {

            score += 0.30;

        } else if (independentSources == 2) {

            score += 0.22;

        } else if (independentSources == 1) {

            score += 0.08;
        }

        /*
         * اتفاق المصادر.
         */
        score +=
                Math.min(
                        0.25,
                        agreement * 0.25
                );

        /*
         * التعارض يخفض الثقة.
         */
        if (conflicting) {

            score -= 0.25;

            checks.add(
                    VerificationCheck.warning(
                            item.getId(),
                            "SOURCE_CONFLICT",
                            "Conflicting information detected."
                    )
            );
        }

        /*
         * الثقة السابقة لا تمنح صلاحية مطلقة.
         * نستعمل جزءاً صغيراً منها فقط.
         */
        double previousConfidence =
                clamp(
                        item.getConfidence(),
                        0.0,
                        1.0
                );

        score +=
                previousConfidence * 0.10;

        return clamp(
                score,
                0.0,
                1.0
        );
    }

    /**
     * الثقة في مصدر واحد.
     */
    private double calculateSingleSourceConfidence(
            KnowledgeItem item,
            List<VerificationCheck> checks
    ) {

        double score = 0.0;

        if (isValidContent(item)) {

            score += 0.30;

            checks.add(
                    VerificationCheck.pass(
                            item.getId(),
                            "CONTENT_VALID",
                            "Content is usable."
                    )
            );

        } else {

            return 0.0;
        }

        if (hasSource(item)) {

            score += 0.20;

            checks.add(
                    VerificationCheck.pass(
                            item.getId(),
                            "SOURCE_PRESENT",
                            "Source information exists."
                    )
            );
        }

        if (item.getTitle() != null
                && !item.getTitle().trim().isEmpty()) {

            score += 0.10;
        }

        if (item.getSourceLocation() != null
                && !item.getSourceLocation()
                .trim()
                .isEmpty()) {

            score += 0.10;
        }

        /*
         * الحد الأعلى لمصدر واحد يبقى محدوداً.
         * لا نعتبره حقيقة مطلقة.
         */
        score = Math.min(
                score,
                0.70
        );

        return score;
    }

    /**
     * تجميع المعلومات حسب الموضوع.
     */
    private Map<String, List<KnowledgeItem>>
    groupByTopic(
            List<KnowledgeItem> items
    ) {

        Map<String, List<KnowledgeItem>> groups =
                new LinkedHashMap<>();

        for (KnowledgeItem item : items) {

            String topic =
                    normalize(
                            item.getTopic()
                    );

            if (topic.isEmpty()) {
                topic = "unknown";
            }

            List<KnowledgeItem> group =
                    groups.get(topic);

            if (group == null) {

                group = new ArrayList<>();

                groups.put(
                        topic,
                        group
                );
            }

            group.add(item);
        }

        return groups;
    }

    /**
     * حساب عدد المصادر المستقلة.
     */
    private int countIndependentSources(
            List<KnowledgeItem> items
    ) {

        Set<String> sourceIds =
                new HashSet<>();

        for (KnowledgeItem item : items) {

            if (!hasSource(item)) {
                continue;
            }

            sourceIds.add(
                    normalize(
                            item.getSourceId()
                    )
            );
        }

        return sourceIds.size();
    }

    /**
     * فحص وجود تعارض واضح.
     *
     * نستعمل تشابه المحتوى كإشارة أولية،
     * وليس كحكم لغوي كامل.
     */
    private boolean hasConflict(
            List<KnowledgeItem> items
    ) {

        if (items == null
                || items.size() < 2) {

            return false;
        }

        for (int i = 0;
             i < items.size();
             i++) {

            KnowledgeItem first =
                    items.get(i);

            String firstContent =
                    normalize(
                            first.getContent()
                    );

            if (firstContent.isEmpty()) {
                continue;
            }

            for (int j = i + 1;
                 j < items.size();
                 j++) {

                KnowledgeItem second =
                        items.get(j);

                String secondContent =
                        normalize(
                                second.getContent()
                        );

                if (secondContent.isEmpty()) {
                    continue;
                }

                /*
                 * إذا كانا يتحدثان عن نفس الموضوع
                 * لكن لا يوجد أي تشابه لغوي،
                 * نعتبرها إشارة تحتاج للمراجعة،
                 * وليس إثباتاً قطعياً للتناقض.
                 */
                double similarity =
                        calculateSimilarity(
                                firstContent,
                                secondContent
                        );

                if (similarity < 0.08) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * نسبة اتفاق تقريبية.
     */
    private double calculateAgreement(
            List<KnowledgeItem> items
    ) {

        if (items == null
                || items.size() <= 1) {

            return 0.0;
        }

        double total = 0.0;
        int comparisons = 0;

        for (int i = 0;
             i < items.size();
             i++) {

            for (int j = i + 1;
                 j < items.size();
                 j++) {

                total +=
                        calculateSimilarity(
                                normalize(
                                        items.get(i)
                                                .getContent()
                                ),
                                normalize(
                                        items.get(j)
                                                .getContent()
                                )
                        );

                comparisons++;
            }
        }

        if (comparisons == 0) {
            return 0.0;
        }

        return clamp(
                total / comparisons,
                0.0,
                1.0
        );
    }

    /**
     * تشابه بسيط بدون مكتبات خارجية.
     */
    private double calculateSimilarity(
            String first,
            String second
    ) {

        if (first == null
                || second == null
                || first.isEmpty()
                || second.isEmpty()) {

            return 0.0;
        }

        if (first.equals(second)) {
            return 1.0;
        }

        Set<String> firstWords =
                tokenize(first);

        Set<String> secondWords =
                tokenize(second);

        if (firstWords.isEmpty()
                || secondWords.isEmpty()) {

            return 0.0;
        }

        Set<String> intersection =
                new HashSet<>(firstWords);

        intersection.retainAll(
                secondWords
        );

        Set<String> union =
                new HashSet<>(firstWords);

        union.addAll(
                secondWords
        );

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size()
                / (double) union.size();
    }

    /**
     * تقسيم النص إلى كلمات.
     */
    private Set<String> tokenize(
            String value
    ) {

        Set<String> result =
                new HashSet<>();

        if (value == null) {
            return result;
        }

        String normalized =
                normalize(value);

        if (normalized.isEmpty()) {
            return result;
        }

        String[] words =
                normalized.split(
                        "\\s+"
                );

        for (String word : words) {

            if (word.length() >= 2) {

                result.add(word);
            }
        }

        return result;
    }

    /**
     * التحقق من صلاحية المحتوى.
     */
    private boolean isValidContent(
            KnowledgeItem item
    ) {

        if (item == null) {
            return false;
        }

        String content =
                item.getContent();

        return content != null
                && !content.trim().isEmpty()
                && content.trim().length() >= 10;
    }

    /**
     * هل يوجد مصدر؟
     */
    private boolean hasSource(
            KnowledgeItem item
    ) {

        return item != null
                && item.getSourceId() != null
                && !item.getSourceId()
                .trim()
                .isEmpty();
    }

    /**
     * توحيد النص.
     */
    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    private double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * نتيجة داخلية للتحقق.
     */
    private static final class VerificationDecision {

        private final List<KnowledgeItem> verifiedItems;
        private final List<KnowledgeItem> reviewItems;
        private final List<KnowledgeItem> rejectedItems;
        private final List<VerificationCheck> checks;

        private VerificationDecision(
                List<KnowledgeItem> verifiedItems,
                List<KnowledgeItem> reviewItems,
                List<KnowledgeItem> rejectedItems,
                List<VerificationCheck> checks
        ) {

            this.verifiedItems =
                    verifiedItems;

            this.reviewItems =
                    reviewItems;

            this.rejectedItems =
                    rejectedItems;

            this.checks =
                    checks;
        }

        public List<KnowledgeItem> getVerifiedItems() {
            return verifiedItems;
        }

        public List<KnowledgeItem> getReviewItems() {
            return reviewItems;
        }

        public List<KnowledgeItem> getRejectedItems() {
            return rejectedItems;
        }

        public List<VerificationCheck> getChecks() {
            return checks;
        }
    }

    /**
     * نتيجة فحص واحد.
     */
    public static final class VerificationCheck {

        public enum Level {
            PASS,
            WARNING,
            FAIL
        }

        private final String itemId;
        private final String rule;
        private final Level level;
        private final String message;

        private VerificationCheck(
                String itemId,
                String rule,
                Level level,
                String message
        ) {

            this.itemId =
                    itemId == null
                            ? ""
                            : itemId;

            this.rule =
                    rule == null
                            ? ""
                            : rule;

            this.level =
                    level == null
                            ? Level.WARNING
                            : level;

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public static VerificationCheck pass(
                String itemId,
                String rule,
                String message
        ) {

            return new VerificationCheck(
                    itemId,
                    rule,
                    Level.PASS,
                    message
            );
        }

        public static VerificationCheck warning(
                String itemId,
                String rule,
                String message
        ) {

            return new VerificationCheck(
                    itemId,
                    rule,
                    Level.WARNING,
                    message
            );
        }

        public static VerificationCheck fail(
                String itemId,
                String rule,
                String message
        ) {

            return new VerificationCheck(
                    itemId,
                    rule,
                    Level.FAIL,
                    message
            );
        }

        public String getItemId() {
            return itemId;
        }

        public String getRule() {
            return rule;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public boolean isPass() {
            return level == Level.PASS;
        }

        public boolean isWarning() {
            return level == Level.WARNING;
        }

        public boolean isFail() {
            return level == Level.FAIL;
        }
    }

    /**
     * التقرير النهائي.
     */
    public static final class VerificationReport {

        private final boolean success;
        private final List<KnowledgeItem> verifiedItems;
        private final List<KnowledgeItem> reviewItems;
        private final List<KnowledgeItem> rejectedItems;
        private final List<VerificationCheck> checks;
        private final String message;

        private VerificationReport(
                boolean success,
                List<KnowledgeItem> verifiedItems,
                List<KnowledgeItem> reviewItems,
                List<KnowledgeItem> rejectedItems,
                List<VerificationCheck> checks,
                String message
        ) {

            this.success =
                    success;

            this.verifiedItems =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    verifiedItems == null
                                            ? Collections.emptyList()
                                            : verifiedItems
                            )
                    );

            this.reviewItems =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    reviewItems == null
                                            ? Collections.emptyList()
                                            : reviewItems
                            )
                    );

            this.rejectedItems =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    rejectedItems == null
                                            ? Collections.emptyList()
                                            : rejectedItems
                            )
                    );

            this.checks =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    checks == null
                                            ? Collections.emptyList()
                                            : checks
                            )
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public static VerificationReport success(
                List<KnowledgeItem> verifiedItems,
                List<KnowledgeItem> reviewItems,
                List<KnowledgeItem> rejectedItems,
                List<VerificationCheck> checks
        ) {

            return new VerificationReport(
                    true,
                    verifiedItems,
                    reviewItems,
                    rejectedItems,
                    checks,
                    "Knowledge verification completed."
            );
        }

        public static VerificationReport failure(
                String message
        ) {

            return new VerificationReport(
                    false,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
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

        public List<KnowledgeItem> getVerifiedItems() {
            return verifiedItems;
        }

        public List<KnowledgeItem> getReviewItems() {
            return reviewItems;
        }

        public List<KnowledgeItem> getRejectedItems() {
            return rejectedItems;
        }

        public List<VerificationCheck> getChecks() {
            return checks;
        }

        public String getMessage() {
            return message;
        }

        public int getVerifiedCount() {
            return verifiedItems.size();
        }

        public int getReviewCount() {
            return reviewItems.size();
        }

        public int getRejectedCount() {
            return rejectedItems.size();
        }

        public boolean hasVerifiedKnowledge() {
            return !verifiedItems.isEmpty();
        }

        public boolean needsReview() {
            return !reviewItems.isEmpty();
        }
    }
}