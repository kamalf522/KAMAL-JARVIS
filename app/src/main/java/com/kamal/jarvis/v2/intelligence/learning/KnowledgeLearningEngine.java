package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS V2
 *
 * KnowledgeLearningEngine
 *
 * حلقة التعلم الرئيسية:
 *
 * Query
 *   ↓
 * Acquisition
 *   ↓
 * Verification
 *   ↓
 * Memory
 *   ↓
 * Knowledge usable by JARVIS
 *
 * هذا المحرك لا يعتمد على مصدر واحد.
 * أي KnowledgeSource مسجل داخل KnowledgeAcquisitionEngine
 * يمكن استعماله ضمن دورة التعلم.
 *
 * مهم:
 * - المعرفة غير الموثقة لا تعتبر معرفة موثوقة.
 * - المعرفة المرفوضة لا تدخل الذاكرة.
 * - المعرفة التي تحتاج مراجعة يمكن الاحتفاظ بها
 *   إذا تم تفعيل ذلك صراحة.
 * - المحرك لا يفرض YouTube أو Web أو أي مصدر محدد.
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

    private LearningResult lastResult;

    /**
     * السلوك الافتراضي:
     *
     * نخزن المعرفة VERIFIED فقط.
     *
     * المعرفة NEEDS_REVIEW تبقى في نتيجة التعلم
     * ولا يتم اعتبارها معرفة موثوقة.
     */
    private boolean storeReviewItems = false;

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
     * تشغيل دورة تعلم كاملة.
     *
     * Query
     * -> Acquire
     * -> Verify
     * -> Store
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
     * تشغيل دورة تعلم كاملة
     * مع إمكانية تخزين المعرفة التي تحتاج مراجعة.
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
         * المرحلة 1:
         * الحصول على المعرفة من جميع المصادر المتاحة.
         */
        state =
                LearningState.ACQUIRING;

        KnowledgeAcquisitionEngine.AcquisitionResult acquisitionResult =
                acquisitionEngine.acquire(
                        lastQuery
                );

        if (acquisitionResult == null) {

            state =
                    LearningState.FAILED;

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

            lastResult =
                    LearningResult.fromAcquisitionFailure(
                            lastQuery,
                            acquisitionResult
                    );

            return lastResult;
        }

        List<KnowledgeItem> acquiredItems =
                acquisitionResult.getItems();

        if (acquiredItems == null
                || acquiredItems.isEmpty()) {

            state =
                    LearningState.COMPLETED;

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
         * التحقق من المعرفة.
         */
        state =
                LearningState.VERIFYING;

        KnowledgeVerificationEngine.VerificationReport verificationReport =
                verificationEngine.verify(
                        acquiredItems
                );

        if (verificationReport == null) {

            state =
                    LearningState.FAILED;

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
                        verificationReport
                                .getVerifiedItems()
                );

        List<KnowledgeItem> reviewItems =
                safeList(
                        verificationReport
                                .getReviewItems()
                );

        List<KnowledgeItem> rejectedItems =
                safeList(
                        verificationReport
                                .getRejectedItems()
                );

        /*
         * المرحلة 3:
         * التخزين.
         */
        state =
                LearningState.STORING;

        List<KnowledgeItem> itemsToStore =
                new ArrayList<>();

        /*
         * المعرفة VERIFIED تدخل الذاكرة.
         */
        itemsToStore.addAll(
                verifiedItems
        );

        /*
         * المعرفة NEEDS_REVIEW لا تدخل افتراضياً.
         * يمكن تفعيلها عبر setStoreReviewItems(true).
         */
        if (includeReviewItems
                || storeReviewItems) {

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
         * انتهاء الدورة.
         */
        state =
                LearningState.COMPLETED;

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
     * تعلم مباشر من عناصر معرفة موجودة مسبقاً.
     *
     * مفيد عندما تأتي البيانات من محرك آخر.
     */
    public synchronized LearningResult learnItems(
            List<KnowledgeItem> items
    ) {

        if (items == null
                || items.isEmpty()) {

            state =
                    LearningState.FAILED;

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

        KnowledgeVerificationEngine.VerificationReport report =
                verificationEngine.verify(
                        items
                );

        if (report == null
                || report.isFailure()) {

            state =
                    LearningState.FAILED;

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
     * تخزين معرفة موثقة يدوياً.
     *
     * هذا لا يتجاوز التحقق:
     * العنصر يجب أن يكون VERIFIED.
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
     * الحصول على أفضل معرفة موثقة مرتبطة بالسؤال.
     */
    public synchronized KnowledgeItem recallBest(
            String query
    ) {

        List<KnowledgeItem> verified =
                memory.searchVerified(
                        query
                );

        if (verified.isEmpty()) {
            return null;
        }

        KnowledgeItem best =
                verified.get(0);

        for (KnowledgeItem item : verified) {

            if (item == null) {
                continue;
            }

            if (item.getConfidence()
                    > best.getConfidence()) {

                best = item;
            }
        }

        return best;
    }

    /**
     * تفعيل أو تعطيل تخزين المعرفة التي تحتاج مراجعة.
     *
     * false هو الوضع الآمن الافتراضي.
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
     * الحالة الحالية.
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

    /**
     * آخر استعلام تعلم.
     */
    public synchronized String getLastQuery() {
        return lastQuery;
    }

    /**
     * عدد دورات التعلم.
     */
    public synchronized long getLearningCount() {
        return learningCount;
    }

    /**
     * آخر نتيجة.
     */
    public synchronized LearningResult getLastResult() {
        return lastResult;
    }

    /**
     * الوصول لمحرك الحصول على المعرفة.
     */
    public KnowledgeAcquisitionEngine
    getAcquisitionEngine() {

        return acquisitionEngine;
    }

    /**
     * الوصول لمحرك التحقق.
     */
    public KnowledgeVerificationEngine
    getVerificationEngine() {

        return verificationEngine;
    }

    /**
     * الوصول للذاكرة.
     */
    public KnowledgeMemory getMemory() {
        return memory;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * تحويل القائمة إلى قائمة آمنة.
     */
    private List<KnowledgeItem> safeList(
            List<KnowledgeItem> items
    ) {

        if (items == null
                || items.isEmpty()) {

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

    /**
     * حالات دورة التعلم.
     */
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