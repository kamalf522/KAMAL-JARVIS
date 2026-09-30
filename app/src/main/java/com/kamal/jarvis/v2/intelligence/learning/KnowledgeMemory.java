package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JARVIS V2
 *
 * KnowledgeMemory
 *
 * الذاكرة الدائمة داخل دورة المعرفة.
 *
 * المسؤوليات:
 * - تخزين المعرفة المقبولة
 * - تحديث المعرفة الموجودة
 * - البحث في المعرفة
 * - حذف المعرفة
 * - منع التكرار البسيط
 * - إعطاء المعرفة الأعلى ثقة
 *
 * ملاحظة:
 * التخزين هنا داخل الذاكرة الحالية للتطبيق.
 * طبقة التخزين الدائم يمكن ربطها لاحقاً بقاعدة بيانات
 * بدون تغيير واجهة هذا المحرك.
 */
public final class KnowledgeMemory {

    private static final String ENGINE_ID =
            "v2.knowledge_memory";

    private final Map<String, KnowledgeItem> memory =
            new LinkedHashMap<>();

    /**
     * إضافة معرفة جديدة.
     */
    public synchronized boolean store(
            KnowledgeItem item
    ) {

        if (!isStorable(item)) {
            return false;
        }

        KnowledgeItem existing =
                memory.get(item.getId());

        if (existing == null) {

            memory.put(
                    item.getId(),
                    item
            );

            return true;
        }

        /*
         * لا نستبدل معرفة أفضل بمعرفة أضعف.
         */
        if (item.getConfidence()
                >= existing.getConfidence()) {

            memory.put(
                    item.getId(),
                    item
            );

            return true;
        }

        return false;
    }

    /**
     * تخزين مجموعة من المعارف.
     */
    public synchronized int storeAll(
            List<KnowledgeItem> items
    ) {

        if (items == null
                || items.isEmpty()) {

            return 0;
        }

        int stored = 0;

        for (KnowledgeItem item : items) {

            if (store(item)) {
                stored++;
            }
        }

        return stored;
    }

    /**
     * تحديث معرفة موجودة.
     */
    public synchronized boolean update(
            KnowledgeItem item
    ) {

        if (!isStorable(item)) {
            return false;
        }

        if (!memory.containsKey(item.getId())) {
            return false;
        }

        memory.put(
                item.getId(),
                item
        );

        return true;
    }

    /**
     * الحصول على معرفة بواسطة ID.
     */
    public synchronized KnowledgeItem get(
            String id
    ) {

        if (id == null
                || id.trim().isEmpty()) {

            return null;
        }

        return memory.get(
                id.trim()
        );
    }

    /**
     * التحقق من وجود معرفة.
     */
    public synchronized boolean contains(
            String id
    ) {

        if (id == null
                || id.trim().isEmpty()) {

            return false;
        }

        return memory.containsKey(
                id.trim()
        );
    }

    /**
     * حذف معرفة.
     */
    public synchronized boolean remove(
            String id
    ) {

        if (id == null
                || id.trim().isEmpty()) {

            return false;
        }

        return memory.remove(
                id.trim()
        ) != null;
    }

    /**
     * البحث في المعرفة.
     *
     * يبحث في:
     * - الموضوع
     * - العنوان
     * - المحتوى
     * - المصدر
     */
    public synchronized List<KnowledgeItem> search(
            String query
    ) {

        if (query == null
                || query.trim().isEmpty()) {

            return Collections.emptyList();
        }

        String normalizedQuery =
                normalize(query);

        String[] queryWords =
                normalizedQuery.split(
                        "\\s+"
                );

        List<ScoredItem> scored =
                new ArrayList<>();

        for (KnowledgeItem item
                : memory.values()) {

            if (item == null
                    || !item.isUsable()) {

                continue;
            }

            double score =
                    calculateRelevance(
                            item,
                            normalizedQuery,
                            queryWords
                    );

            if (score > 0.0) {

                scored.add(
                        new ScoredItem(
                                item,
                                score
                        )
                );
            }
        }

        Collections.sort(
                scored,
                (first, second) ->
                        Double.compare(
                                second.score,
                                first.score
                        )
        );

        List<KnowledgeItem> result =
                new ArrayList<>();

        for (ScoredItem scoredItem
                : scored) {

            result.add(
                    scoredItem.item
            );
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * البحث عن المعرفة الموثقة فقط.
     */
    public synchronized List<KnowledgeItem>
    searchVerified(
            String query
    ) {

        List<KnowledgeItem> all =
                search(query);

        List<KnowledgeItem> verified =
                new ArrayList<>();

        for (KnowledgeItem item : all) {

            if (item != null
                    && item.isVerified()) {

                verified.add(item);
            }
        }

        return Collections.unmodifiableList(
                verified
        );
    }

    /**
     * الحصول على المعرفة الأعلى ثقة
     * المرتبطة بالاستعلام.
     */
    public synchronized KnowledgeItem
    findBest(
            String query
    ) {

        List<KnowledgeItem> results =
                search(query);

        if (results.isEmpty()) {
            return null;
        }

        KnowledgeItem best =
                results.get(0);

        /*
         * إذا كانت هناك معرفة موثقة
         * نفضلها على معرفة غير موثقة.
         */
        for (KnowledgeItem item : results) {

            if (item.isVerified()
                    && !best.isVerified()) {

                best = item;
                break;
            }

            if (item.getConfidence()
                    > best.getConfidence()) {

                best = item;
            }
        }

        return best;
    }

    /**
     * الحصول على كل المعرفة.
     */
    public synchronized List<KnowledgeItem>
    getAll() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        memory.values()
                )
        );
    }

    /**
     * الحصول على IDs.
     */
    public synchronized List<String>
    getIds() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        memory.keySet()
                )
        );
    }

    /**
     * عدد المعارف.
     */
    public synchronized int size() {
        return memory.size();
    }

    public synchronized boolean isEmpty() {
        return memory.isEmpty();
    }

    /**
     * عدد المعارف الموثقة.
     */
    public synchronized int getVerifiedCount() {

        int count = 0;

        for (KnowledgeItem item
                : memory.values()) {

            if (item != null
                    && item.isVerified()) {

                count++;
            }
        }

        return count;
    }

    /**
     * عدد المعارف التي تحتاج مراجعة.
     */
    public synchronized int getReviewCount() {

        int count = 0;

        for (KnowledgeItem item
                : memory.values()) {

            if (item != null
                    && item.getStatus()
                    == KnowledgeItem.KnowledgeStatus.NEEDS_REVIEW) {

                count++;
            }
        }

        return count;
    }

    /**
     * حذف المعرفة المرفوضة.
     */
    public synchronized int removeRejected() {

        List<String> ids =
                new ArrayList<>();

        for (KnowledgeItem item
                : memory.values()) {

            if (item != null
                    && item.getStatus()
                    == KnowledgeItem.KnowledgeStatus.REJECTED) {

                ids.add(
                        item.getId()
                );
            }
        }

        for (String id : ids) {
            memory.remove(id);
        }

        return ids.size();
    }

    /**
     * حذف المعرفة القديمة.
     */
    public synchronized int removeOutdated() {

        List<String> ids =
                new ArrayList<>();

        for (KnowledgeItem item
                : memory.values()) {

            if (item != null
                    && item.getStatus()
                    == KnowledgeItem.KnowledgeStatus.OUTDATED) {

                ids.add(
                        item.getId()
                );
            }
        }

        for (String id : ids) {
            memory.remove(id);
        }

        return ids.size();
    }

    /**
     * مسح الذاكرة.
     */
    public synchronized void clear() {
        memory.clear();
    }

    /**
     * حساب علاقة المعرفة بالبحث.
     */
    private double calculateRelevance(
            KnowledgeItem item,
            String query,
            String[] queryWords
    ) {

        double score = 0.0;

        String topic =
                normalize(
                        item.getTopic()
                );

        String title =
                normalize(
                        item.getTitle()
                );

        String content =
                normalize(
                        item.getContent()
                );

        String source =
                normalize(
                        item.getSourceId()
                );

        if (!query.isEmpty()) {

            if (topic.contains(query)) {
                score += 0.40;
            }

            if (title.contains(query)) {
                score += 0.50;
            }

            if (content.contains(query)) {
                score += 0.30;
            }

            if (source.contains(query)) {
                score += 0.10;
            }
        }

        /*
         * مطابقة الكلمات بشكل منفصل.
         */
        if (queryWords != null) {

            for (String word : queryWords) {

                if (word == null
                        || word.length() < 2) {

                    continue;
                }

                if (topic.contains(word)) {
                    score += 0.12;
                }

                if (title.contains(word)) {
                    score += 0.16;
                }

                if (content.contains(word)) {
                    score += 0.08;
                }
            }
        }

        /*
         * المعرفة الموثقة لها أولوية.
         */
        if (item.isVerified()) {
            score += 0.20;
        }

        /*
         * الثقة تدخل في ترتيب النتائج.
         */
        score +=
                item.getConfidence()
                        * 0.15;

        return score;
    }

    /**
     * التحقق من إمكانية التخزين.
     */
    private boolean isStorable(
            KnowledgeItem item
    ) {

        if (item == null) {
            return false;
        }

        if (item.getId() == null
                || item.getId().trim().isEmpty()) {

            return false;
        }

        if (item.getContent() == null
                || item.getContent()
                .trim()
                .isEmpty()) {

            return false;
        }

        /*
         * المعرفة المرفوضة لا تدخل الذاكرة.
         */
        return item.getStatus()
                != KnowledgeItem.KnowledgeStatus.REJECTED;
    }

    /**
     * توحيد النص للبحث.
     */
    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * عنصر داخلي مع درجة البحث.
     */
    private static final class ScoredItem {

        private final KnowledgeItem item;
        private final double score;

        private ScoredItem(
                KnowledgeItem item,
                double score
        ) {

            this.item = item;
            this.score = score;
        }
    }
}