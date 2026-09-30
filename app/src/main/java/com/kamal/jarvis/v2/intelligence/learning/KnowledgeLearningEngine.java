package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2
 *
 * KnowledgeLearningEngine
 *
 * دورة التعلم:
 *
 * Query
 *   ↓
 * Internet Source Discovery
 *   ↓
 * Trust Evaluation
 *   ↓
 * Knowledge Acquisition
 *   ↓
 * Knowledge Verification
 *   ↓
 * Knowledge Memory
 *   ↓
 * Continuous Learning
 *
 * هذا المحرك لا يحتاج من المستخدم إضافة المصادر.
 *
 * KnowledgeAcquisitionEngine هو المسؤول عن:
 * - اكتشاف المصادر
 * - تقييم المصادر
 * - جمع المعرفة
 *
 * KnowledgeVerificationEngine هو المسؤول عن:
 * - التحقق
 * - مقارنة المعرفة
 * - اكتشاف التعارض
 *
 * KnowledgeMemory هو المسؤول عن:
 * - حفظ المعرفة المقبولة
 * - استرجاعها
 */
public final class KnowledgeLearningEngine {

    private static final String ENGINE_ID =
            "v2.knowledge_learning";

    private final KnowledgeAcquisitionEngine acquisitionEngine;

    private final KnowledgeVerificationEngine verificationEngine;

    private final KnowledgeMemory memory;

    private LearningState state =
            LearningState.IDLE;

    private String lastQuery = "";

    private long learningCount = 0L;

    private long successfulLearningCount = 0L;

    private long failedLearningCount = 0L;

    private LearningResult lastResult;

    /*
     * الوضع الافتراضي:
     *
     * VERIFIED فقط تدخل الذاكرة.
     */
    private boolean storeReviewItems = false;

    /*
     * إذا كان true يستطيع JARVIS إعادة فحص
     * المصادر التي سبق اكتشافها بشكل دوري.
     */
    private boolean continuousSourceRevalidation = true;

    /*
     * عدد دورات التعلم التي يجب بعدها
     * إعادة فحص المصادر.
     */
    private long revalidationInterval = 5L;

    public KnowledgeLearningEngine(
            KnowledgeAcquisitionEngine acquisitionEngine,
            KnowledgeVerificationEngine verificationEngine,
            KnowledgeMemory memory
    ) {

        if (acquisitionEngine == null) {
            throw new IllegalArgumentException(
                    "KnowledgeAcquisitionEngine cannot be null."
            );
        }

        if (verificationEngine == null) {
            throw new IllegalArgumentException(
                    "KnowledgeVerificationEngine cannot be null."
            );
        }

        if (memory == null) {
            throw new IllegalArgumentException(
                    "KnowledgeMemory cannot be null."
            );
        }

        this.acquisitionEngine =
                acquisitionEngine;

        this.verificationEngine =
                verificationEngine;

        this.memory =
                memory;
    }

    /**
     * دورة تعلم كاملة.
     */
    public synchronized LearningResult learn(
            String query
    ) {

        return learn(
                query,
                false
        );
    }

    /**
     * دورة تعلم كاملة.
     *
     * Internet
     * -> Discovery
     * -> Trust
     * -> Acquisition
     * -> Verification
     * -> Memory
     */
    public synchronized LearningResult learn(
            String query,
            boolean includeReviewItems
    ) {

        String normalizedQuery =
                normalize(
                        query
                );

        if (normalizedQuery.isEmpty()) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.failure(
                            query,
                            "Learning query is empty."
                    );

            return lastResult;
        }

        lastQuery =
                query == null
                        ? ""
                        : query.trim();

        learningCount++;

        /*
         * إعادة التحقق من المصادر بشكل دوري.
         *
         * هذا لا يضيف مصادر يدوياً.
         * JARVIS هو الذي يعيد فحصها.
         */
        if (shouldRevalidateSources()) {

            try {

                acquisitionEngine
                        .revalidateSources();

            } catch (Exception ignored) {
                /*
                 * فشل إعادة التحقق لا يمنع دورة
                 * التعلم الحالية من محاولة اكتشاف
                 * مصادر جديدة.
                 */
            }
        }

        /*
         * المرحلة 1:
         *
         * اكتشاف المصادر + الثقة + جمع المعرفة.
         */
        state =
                LearningState.ACQUIRING;

        KnowledgeAcquisitionEngine.AcquisitionResult
                acquisitionResult =
                acquisitionEngine.acquire(
                        lastQuery
                );

        if (acquisitionResult == null) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.failure(
                            lastQuery,
                            "Knowledge acquisition returned no result."
                    );

            return lastResult;
        }

        if (acquisitionResult.isFailure()) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.fromAcquisitionFailure(
                            lastQuery,
                            acquisitionResult
                    );

            return lastResult;
        }

        List<KnowledgeItem> acquiredItems =
                safeList(
                        acquisitionResult.getItems()
                );

        /*
         * لم نجد معرفة جديدة.
         */
        if (acquiredItems.isEmpty()) {

            state =
                    LearningState.COMPLETED;

            successfulLearningCount++;

            lastResult =
                    LearningResult.success(
                            lastQuery,
                            acquisitionResult,
                            null,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            0
                    );

            return lastResult;
        }

        /*
         * المرحلة 2:
         *
         * التحقق من المعرفة.
         */
        state =
                LearningState.VERIFYING;

        KnowledgeVerificationEngine.VerificationReport
                verificationReport =
                verificationEngine.verify(
                        acquiredItems
                );

        if (verificationReport == null) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.failure(
                            lastQuery,
                            "Knowledge verification returned no result."
                    );

            return lastResult;
        }

        if (verificationReport.isFailure()) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.fromVerificationFailure(
                            lastQuery,
                            acquisitionResult,
                            verificationReport
                    );

            return lastResult;
        }

        List<KnowledgeItem> verifiedItems =
                safeList(
                        verificationReport.getVerifiedItems()
                );

        List<KnowledgeItem> reviewItems =
                safeList(
                        verificationReport.getReviewItems()
                );

        List<KnowledgeItem> rejectedItems =
                safeList(
                        verificationReport.getRejectedItems()
                );

        /*
         * المرحلة 3:
         *
         * اختيار ما يدخل الذاكرة.
         */
        state =
                LearningState.STORING;

        List<KnowledgeItem> itemsToStore =
                new ArrayList<>();

        /*
         * المعرفة الموثقة تدخل دائماً.
         */
        itemsToStore.addAll(
                verifiedItems
        );

        /*
         * المعرفة التي تحتاج مراجعة لا تدخل
         * إلا إذا طلب النظام ذلك صراحة.
         */
        if (
                includeReviewItems
                        || storeReviewItems
        ) {

            itemsToStore.addAll(
                    reviewItems
            );
        }

        int storedCount =
                memory.storeAll(
                        itemsToStore
                );

        /*
         * المرحلة 4:
         *
         * إتمام دورة التعلم.
         */
        state =
                LearningState.COMPLETED;

        successfulLearningCount++;

        lastResult =
                LearningResult.success(
                        lastQuery,
                        acquisitionResult,
                        verificationReport,
                        verifiedItems,
                        reviewItems,
                        rejectedItems,
                        storedCount
                );

        return lastResult;
    }

    /**
     * تعلم من معرفة موجودة مسبقاً.
     *
     * المعرفة تمر عبر التحقق قبل التخزين.
     */
    public synchronized LearningResult learnItems(
            List<KnowledgeItem> items
    ) {

        if (
                items == null
                        || items.isEmpty()
        ) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.failure(
                            "",
                            "No knowledge items were provided."
                    );

            return lastResult;
        }

        learningCount++;

        state =
                LearningState.VERIFYING;

        KnowledgeVerificationEngine.VerificationReport
                report =
                verificationEngine.verify(
                        items
                );

        if (
                report == null
                        || report.isFailure()
        ) {

            state =
                    LearningState.FAILED;

            failedLearningCount++;

            lastResult =
                    LearningResult.fromVerificationFailure(
                            "",
                            null,
                            report
                    );

            return lastResult;
        }

        List<KnowledgeItem> verified =
                safeList(
                        report.getVerifiedItems()
                );

        List<KnowledgeItem> review =
                safeList(
                        report.getReviewItems()
                );

        List<KnowledgeItem> rejected =
                safeList(
                        report.getRejectedItems()
                );

        state =
                LearningState.STORING;

        List<KnowledgeItem> toStore =
                new ArrayList<>();

        toStore.addAll(
                verified
        );

        if (storeReviewItems) {

            toStore.addAll(
                    review
            );
        }

        int stored =
                memory.storeAll(
                        toStore
                );

        state =
                LearningState.COMPLETED;

        successfulLearningCount++;

        lastResult =
                LearningResult.success(
                        "",
                        null,
                        report,
                        verified,
                        review,
                        rejected,
                        stored
                );

        return lastResult;
    }

    /**
     * حفظ معرفة موثقة.
     *
     * لا يمكن تجاوز التحقق.
     */
    public synchronized boolean remember(
            KnowledgeItem item
    ) {

        if (item == null) {
            return false;
        }

        if (!item.isVerified()) {
            return false;
        }

        return memory.store(
                item
        );
    }

    /**
     * استرجاع المعرفة الموثقة.
     */
    public synchronized List<KnowledgeItem> recall(
            String query
    ) {

        return memory.searchVerified(
                query
        );
    }

    /**
     * إيجاد أفضل معرفة مرتبطة بالطلب.
     */
    public synchronized KnowledgeItem recallBest(
            String query
    ) {

        return memory.findBest(
                query
        );
    }

    /**
     * تفعيل / تعطيل تخزين العناصر
     * التي تحتاج مراجعة.
     */
    public synchronized void setStoreReviewItems(
            boolean enabled
    ) {

        storeReviewItems =
                enabled;
    }

    public synchronized boolean isStoreReviewItemsEnabled() {

        return storeReviewItems;
    }

    /**
     * تفعيل / تعطيل إعادة فحص المصادر.
     */
    public synchronized void setContinuousSourceRevalidation(
            boolean enabled
    ) {

        continuousSourceRevalidation =
                enabled;
    }

    public synchronized boolean
    isContinuousSourceRevalidationEnabled() {

        return continuousSourceRevalidation;
    }

    /**
     * تحديد عدد دورات التعلم بين عمليات
     * إعادة فحص المصادر.
     */
    public synchronized void setRevalidationInterval(
            long interval
    ) {

        if (interval < 1L) {

            interval = 1L;
        }

        revalidationInterval =
                interval;
    }

    public synchronized long getRevalidationInterval() {

        return revalidationInterval;
    }

    /**
     * إجبار JARVIS على إعادة فحص مصادره الآن.
     */
    public synchronized KnowledgeSourceTrustEngine.RevalidationResult
    revalidateSourcesNow() {

        return acquisitionEngine
                .revalidateSources();
    }

    /**
     * مصادر الإنترنت التي اكتشفها JARVIS.
     */
    public synchronized List<
            KnowledgeSourceTrustEngine.SourceProfile
            > getDiscoveredSources() {

        return acquisitionEngine
                .getDiscoveredSourceProfiles();
    }

    /**
     * المصادر التي يسمح نظام الثقة باستخدامها.
     */
    public synchronized List<
            KnowledgeSourceTrustEngine.SourceProfile
            > getUsableSources() {

        return acquisitionEngine
                .getUsableSourceProfiles();
    }

    /**
     * عدد المصادر المكتشفة.
     */
    public synchronized int getSourceCount() {

        return acquisitionEngine
                .getSourceCount();
    }

    /**
     * حالة التعلم.
     */
    public synchronized LearningState getState() {

        return state;
    }

    public synchronized boolean isLearning() {

        return state == LearningState.ACQUIRING
                || state == LearningState.VERIFYING
                || state == LearningState.STORING;
    }

    public synchronized boolean isIdle() {

        return state == LearningState.IDLE;
    }

    public synchronized boolean isCompleted() {

        return state == LearningState.COMPLETED;
    }

    public synchronized boolean hasFailed() {

        return state == LearningState.FAILED;
    }

    public synchronized String getLastQuery() {

        return lastQuery;
    }

    public synchronized long getLearningCount() {

        return learningCount;
    }

    public synchronized long getSuccessfulLearningCount() {

        return successfulLearningCount;
    }

    public synchronized long getFailedLearningCount() {

        return failedLearningCount;
    }

    public synchronized LearningResult getLastResult() {

        return lastResult;
    }

    public KnowledgeAcquisitionEngine
    getAcquisitionEngine() {

        return acquisitionEngine;
    }

    public KnowledgeVerificationEngine
    getVerificationEngine() {

        return verificationEngine;
    }

    public KnowledgeMemory getMemory() {

        return memory;
    }

    public String getEngineId() {

        return ENGINE_ID;
    }

    /**
     * تحديد هل حان وقت إعادة فحص المصادر.
     */
    private boolean shouldRevalidateSources() {

        if (!continuousSourceRevalidation) {
            return false;
        }

        if (learningCount <= 1L) {
            return false;
        }

        return (
                learningCount
                        % revalidationInterval
                        == 0L
        );
    }

    private List<KnowledgeItem> safeList(
            List<KnowledgeItem> items
    ) {

        if (
                items == null
                        || items.isEmpty()
        ) {

            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        items
                )
        );
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    public enum LearningState {

        IDLE,

        ACQUIRING,

        VERIFYING,

        STORING,

        COMPLETED,

        FAILED
    }

    /**
     * نتيجة دورة التعلم.
     */
    public static final class LearningResult {

        private final boolean success;

        private final String query;

        private final KnowledgeAcquisitionEngine.AcquisitionResult
                acquisitionResult;

        private final KnowledgeVerificationEngine.VerificationReport
                verificationReport;

        private final List<KnowledgeItem>
                verifiedItems;

        private final List<KnowledgeItem>
                reviewItems;

        private final List<KnowledgeItem>
                rejectedItems;

        private final int storedCount;

        private final String message;

        private LearningResult(
                boolean success,
                String query,
                KnowledgeAcquisitionEngine.AcquisitionResult
                        acquisitionResult,
                KnowledgeVerificationEngine.VerificationReport
                        verificationReport,
                List<KnowledgeItem> verifiedItems,
                List<KnowledgeItem> reviewItems,
                List<KnowledgeItem> rejectedItems,
                int storedCount,
                String message
        ) {

            this.success =
                    success;

            this.query =
                    query == null
                            ? ""
                            : query;

            this.acquisitionResult =
                    acquisitionResult;

            this.verificationReport =
                    verificationReport;

            this.verifiedItems =
                    immutableList(
                            verifiedItems
                    );

            this.reviewItems =
                    immutableList(
                            reviewItems
                    );

            this.rejectedItems =
                    immutableList(
                            rejectedItems
                    );

            this.storedCount =
                    Math.max(
                            0,
                            storedCount
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        private static List<KnowledgeItem> immutableList(
                List<KnowledgeItem> items
        ) {

            if (
                    items == null
                            || items.isEmpty()
            ) {

                return Collections.emptyList();
            }

            return Collections.unmodifiableList(
                    new ArrayList<>(
                            items
                    )
            );
        }

        public static LearningResult success(
                String query,
                KnowledgeAcquisitionEngine.AcquisitionResult
                        acquisitionResult,
                KnowledgeVerificationEngine.VerificationReport
                        verificationReport,
                List<KnowledgeItem> verifiedItems,
                List<KnowledgeItem> reviewItems,
                List<KnowledgeItem> rejectedItems,
                int storedCount
        ) {

            return new LearningResult(
                    true,
                    query,
                    acquisitionResult,
                    verificationReport,
                    verifiedItems,
                    reviewItems,
                    rejectedItems,
                    storedCount,
                    "Knowledge learning cycle completed."
            );
        }

        public static LearningResult failure(
                String query,
                String message
        ) {

            return new LearningResult(
                    false,
                    query,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    message
            );
        }

        public static LearningResult fromAcquisitionFailure(
                String query,
                KnowledgeAcquisitionEngine.AcquisitionResult
                        acquisitionResult
        ) {

            String message =
                    acquisitionResult == null
                            ? "Knowledge acquisition failed."
                            : acquisitionResult.getMessage();

            return new LearningResult(
                    false,
                    query,
                    acquisitionResult,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    message
            );
        }

        public static LearningResult fromVerificationFailure(
                String query,
                KnowledgeAcquisitionEngine.AcquisitionResult
                        acquisitionResult,
                KnowledgeVerificationEngine.VerificationReport
                        verificationReport
        ) {

            String message =
                    verificationReport == null
                            ? "Knowledge verification failed."
                            : verificationReport.getMessage();

            return new LearningResult(
                    false,
                    query,
                    acquisitionResult,
                    verificationReport,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
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

        public KnowledgeAcquisitionEngine.AcquisitionResult
        getAcquisitionResult() {

            return acquisitionResult;
        }

        public KnowledgeVerificationEngine.VerificationReport
        getVerificationReport() {

            return verificationReport;
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

        public int getVerifiedCount() {
            return verifiedItems.size();
        }

        public int getReviewCount() {
            return reviewItems.size();
        }

        public int getRejectedCount() {
            return rejectedItems.size();
        }

        public int getStoredCount() {
            return storedCount;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasNewKnowledge() {
            return storedCount > 0;
        }

        public boolean needsReview() {
            return !reviewItems.isEmpty();
        }
    }
}