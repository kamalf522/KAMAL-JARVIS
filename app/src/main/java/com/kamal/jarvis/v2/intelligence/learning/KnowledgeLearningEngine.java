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
 * Evolution Context
 *   ↓
 * Continuous Learning / Evolution
 *
 * هذا المحرك لا يحتاج من المستخدم إضافة المصادر.
 *
 * KnowledgeAcquisitionEngine:
 * - اكتشاف المصادر
 * - تقييم المصادر
 * - جمع المعرفة
 *
 * KnowledgeVerificationEngine:
 * - التحقق
 * - مقارنة المعرفة
 * - اكتشاف التعارض
 *
 * KnowledgeMemory:
 * - حفظ المعرفة المقبولة
 * - استرجاعها
 *
 * EvolutionContext:
 * - يحمل المعرفة الموثقة التي يمكن أن يستفيد منها
 *   نظام التطور.
 *
 * مهم:
 * هذا المحرك لا يقوم بنفسه بتعديل المشروع.
 * ولا ينشئ كوداً من معرفة غير موثقة.
 * نظام Evolution هو الذي يقرر ماذا يجب تطويره.
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
     * VERIFIED فقط تدخل الذاكرة
     * في الوضع الافتراضي.
     */
    private boolean storeReviewItems = false;

    /*
     * إعادة فحص المصادر المكتشفة بشكل دوري.
     */
    private boolean continuousSourceRevalidation = true;

    /*
     * عدد دورات التعلم بين عمليات إعادة الفحص.
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
     * دورة تعلم كاملة:
     *
     * Internet
     * -> Discovery
     * -> Trust
     * -> Acquisition
     * -> Verification
     * -> Memory
     * -> Evolution Context
     */
    public synchronized LearningResult learn(
            String query,
            boolean includeReviewItems
    ) {

        String normalizedQuery =
                normalize(query);

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
         */
        if (shouldRevalidateSources()) {

            try {

                acquisitionEngine
                        .revalidateSources();

            } catch (Exception ignored) {

                /*
                 * فشل إعادة التحقق لا يمنع
                 * محاولة التعلم الحالية.
                 */
            }
        }

        /*
         * =========================================
         * 1. ACQUISITION
         * =========================================
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

            EvolutionContext evolutionContext =
                    EvolutionContext.empty(
                            lastQuery
                    );

            lastResult =
                    LearningResult.success(
                            lastQuery,
                            acquisitionResult,
                            null,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            0,
                            evolutionContext
                    );

            return lastResult;
        }

        /*
         * =========================================
         * 2. VERIFICATION
         * =========================================
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
         * =========================================
         * 3. MEMORY
         * =========================================
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
         * إلا إذا سمح النظام بذلك.
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
         * =========================================
         * 4. EVOLUTION CONTEXT
         * =========================================
         *
         * المعرفة الموثقة تصبح متاحة لنظام التطور.
         *
         * لا يتم هنا تعديل أي ملف أو كود.
         */
        EvolutionContext evolutionContext =
                createEvolutionContext(
                        lastQuery,
                        verifiedItems,
                        reviewItems,
                        rejectedItems
                );

        /*
         * =========================================
         * 5. COMPLETE
         * =========================================
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
                        storedCount,
                        evolutionContext
                );

        return lastResult;
    }

    /**
     * تعلم من معرفة موجودة مسبقاً.
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

        EvolutionContext evolutionContext =
                createEvolutionContext(
                        "",
                        verified,
                        review,
                        rejected
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
                        stored,
                        evolutionContext
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
     * استرجاع آخر Evolution Context.
     *
     * هذا هو الجسر المعماري بين:
     *
     * Learning
     * و
     * Evolution
     */
    public synchronized EvolutionContext
    getLastEvolutionContext() {

        if (lastResult == null) {
            return null;
        }

        return lastResult.getEvolutionContext();
    }

    /**
     * هل توجد معرفة موثقة يمكن لنظام التطور
     * الاستفادة منها؟
     */
    public synchronized boolean
    hasEvolutionKnowledge() {

        EvolutionContext context =
                getLastEvolutionContext();

        return context != null
                && context.hasVerifiedKnowledge();
    }

    /**
     * الحصول على المعرفة التي يمكن أن يستفيد
     * منها نظام التطور.
     */
    public synchronized List<KnowledgeItem>
    getEvolutionKnowledge() {

        EvolutionContext context =
                getLastEvolutionContext();

        if (context == null) {

            return Collections.emptyList();
        }

        return context.getVerifiedKnowledge();
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

    public synchronized boolean
    isStoreReviewItemsEnabled() {

        return storeReviewItems;
    }

    /**
     * تفعيل / تعطيل إعادة فحص المصادر.
     */
    public synchronized void
    setContinuousSourceRevalidation(
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

    public synchronized long
    getRevalidationInterval() {

        return revalidationInterval;
    }

    /**
     * إجبار JARVIS على إعادة فحص مصادره الآن.
     */
    public synchronized
    KnowledgeSourceTrustEngine.RevalidationResult
    revalidateSourcesNow() {

        return acquisitionEngine
                .revalidateSources();
    }

    /**
     * مصادر الإنترنت التي اكتشفها JARVIS.
     */
    public synchronized List<
            KnowledgeSourceTrustEngine.SourceProfile
            >
    getDiscoveredSources() {

        return acquisitionEngine
                .getDiscoveredSourceProfiles();
    }

    /**
     * المصادر التي يسمح نظام الثقة باستخدامها.
     */
    public synchronized List<
            KnowledgeSourceTrustEngine.SourceProfile
            >
    getUsableSources() {

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

    public synchronized long
    getSuccessfulLearningCount() {

        return successfulLearningCount;
    }

    public synchronized long
    getFailedLearningCount() {

        return failedLearningCount;
    }

    public synchronized LearningResult
    getLastResult() {

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

    /**
     * بناء السياق الذي سيمر لاحقاً
     * إلى نظام التطور.
     */
    private EvolutionContext
    createEvolutionContext(
            String query,
            List<KnowledgeItem> verifiedItems,
            List<KnowledgeItem> reviewItems,
            List<KnowledgeItem> rejectedItems
    ) {

        return new EvolutionContext(
                query,
                verifiedItems,
                reviewItems,
                rejectedItems,
                System.currentTimeMillis()
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
     * السياق المعرفي الذي ينتقل من Learning
     * إلى Evolution.
     *
     * لا ينفذ أي تغيير بنفسه.
     */
    public static final class EvolutionContext {

        private final String query;

        private final List<KnowledgeItem>
                verifiedKnowledge;

        private final List<KnowledgeItem>
                reviewKnowledge;

        private final List<KnowledgeItem>
                rejectedKnowledge;

        private final long createdAt;

        private EvolutionContext(
                String query,
                List<KnowledgeItem> verifiedKnowledge,
                List<KnowledgeItem> reviewKnowledge,
                List<KnowledgeItem> rejectedKnowledge,
                long createdAt
        ) {

            this.query =
                    query == null
                            ? ""
                            : query;

            this.verifiedKnowledge =
                    immutableList(
                            verifiedKnowledge
                    );

            this.reviewKnowledge =
                    immutableList(
                            reviewKnowledge
                    );

            this.rejectedKnowledge =
                    immutableList(
                            rejectedKnowledge
                    );

            this.createdAt =
                    createdAt;
        }

        public static EvolutionContext empty(
                String query
        ) {

            return new EvolutionContext(
                    query,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    System.currentTimeMillis()
            );
        }

        public String getQuery() {

            return query;
        }

        public List<KnowledgeItem>
        getVerifiedKnowledge() {

            return verifiedKnowledge;
        }

        public List<KnowledgeItem>
        getReviewKnowledge() {

            return reviewKnowledge;
        }

        public List<KnowledgeItem>
        getRejectedKnowledge() {

            return rejectedKnowledge;
        }

        public int getVerifiedCount() {

            return verifiedKnowledge.size();
        }

        public int getReviewCount() {

            return reviewKnowledge.size();
        }

        public int getRejectedCount() {

            return rejectedKnowledge.size();
        }

        public boolean hasVerifiedKnowledge() {

            return !verifiedKnowledge.isEmpty();
        }

        public boolean hasReviewKnowledge() {

            return !reviewKnowledge.isEmpty();
        }

        public boolean isEmpty() {

            return verifiedKnowledge.isEmpty()
                    && reviewKnowledge.isEmpty()
                    && rejectedKnowledge.isEmpty();
        }

        public long getCreatedAt() {

            return createdAt;
        }

        private static List<KnowledgeItem>
        immutableList(
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

        /*
         * الجسر المعرفي نحو Evolution.
         */
        private final EvolutionContext
                evolutionContext;

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
                String message,
                EvolutionContext evolutionContext
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

            this.evolutionContext =
                    evolutionContext;
        }

        private static List<KnowledgeItem>
        immutableList(
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
                int storedCount,
                EvolutionContext evolutionContext
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
                    "Knowledge learning cycle completed.",
                    evolutionContext
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
                    message,
                    null
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
                    message,
                    null
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
                    message,
                    null
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

        public List<KnowledgeItem>
        getVerifiedItems() {

            return verifiedItems;
        }

        public List<KnowledgeItem>
        getReviewItems() {

            return reviewItems;
        }

        public List<KnowledgeItem>
        getRejectedItems() {

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

        /**
         * السياق الذي يمكن لنظام Evolution استعماله.
         */
        public EvolutionContext
        getEvolutionContext() {

            return evolutionContext;
        }

        public boolean
        hasEvolutionKnowledge() {

            return evolutionContext != null
                    && evolutionContext
                    .hasVerifiedKnowledge();
        }
    }
}