package com.kamal.jarvis.v2.intelligence.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2
 *
 * KnowledgeSourceRegistry
 *
 * سجل مركزي لمصادر المعرفة.
 *
 * المسؤوليات:
 * - تسجيل مصادر المعرفة
 * - استبدال مصدر موجود
 * - حذف مصدر
 * - البحث عن مصدر
 * - معرفة المصادر المتاحة
 * - إعطاء قائمة المصادر لمحرك التعلم
 *
 * لا يفرض هذا النظام نوعاً محدداً من المصادر.
 *
 * يمكن تسجيل:
 * - Internet
 * - Web
 * - Documentation
 * - API
 * - Video
 * - Local files
 * - Database
 * - أي مصدر آخر يطبق KnowledgeSource
 */
public final class KnowledgeSourceRegistry {

    private static final String REGISTRY_ID =
            "v2.knowledge_source_registry";

    private final Map<String, KnowledgeSource> sources =
            new LinkedHashMap<>();

    /**
     * تسجيل مصدر جديد.
     *
     * إذا كان ID موجوداً يتم استبدال المصدر القديم.
     */
    public synchronized boolean register(
            KnowledgeSource source
    ) {

        if (!isValidSource(source)) {
            return false;
        }

        String id =
                normalizeId(
                        source.getId()
                );

        sources.put(
                id,
                source
        );

        return true;
    }

    /**
     * تسجيل مجموعة مصادر.
     */
    public synchronized int registerAll(
            List<KnowledgeSource> sourceList
    ) {

        if (sourceList == null
                || sourceList.isEmpty()) {

            return 0;
        }

        int registered = 0;

        for (KnowledgeSource source : sourceList) {

            if (register(source)) {
                registered++;
            }
        }

        return registered;
    }

    /**
     * الحصول على مصدر بواسطة ID.
     */
    public synchronized KnowledgeSource get(
            String sourceId
    ) {

        String id =
                normalizeId(sourceId);

        if (id.isEmpty()) {
            return null;
        }

        return sources.get(id);
    }

    /**
     * التحقق من وجود مصدر.
     */
    public synchronized boolean contains(
            String sourceId
    ) {

        String id =
                normalizeId(sourceId);

        if (id.isEmpty()) {
            return false;
        }

        return sources.containsKey(id);
    }

    /**
     * حذف مصدر.
     */
    public synchronized boolean remove(
            String sourceId
    ) {

        String id =
                normalizeId(sourceId);

        if (id.isEmpty()) {
            return false;
        }

        return sources.remove(id) != null;
    }

    /**
     * حذف جميع المصادر.
     */
    public synchronized void clear() {
        sources.clear();
    }

    /**
     * عدد المصادر المسجلة.
     */
    public synchronized int size() {
        return sources.size();
    }

    public synchronized boolean isEmpty() {
        return sources.isEmpty();
    }

    /**
     * الحصول على IDs جميع المصادر.
     */
    public synchronized List<String> getSourceIds() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        sources.keySet()
                )
        );
    }

    /**
     * الحصول على جميع المصادر.
     */
    public synchronized List<KnowledgeSource>
    getSources() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        sources.values()
                )
        );
    }

    /**
     * الحصول على المصادر المتاحة حالياً.
     */
    public synchronized List<KnowledgeSource>
    getAvailableSources() {

        List<KnowledgeSource> result =
                new ArrayList<>();

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            try {

                if (source.isAvailable()) {
                    result.add(source);
                }

            } catch (Exception ignored) {
                /*
                 * مصدر غير قادر على تحديد حالته
                 * لا يتم اعتباره متاحاً.
                 */
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * IDs المصادر المتاحة.
     */
    public synchronized List<String>
    getAvailableSourceIds() {

        List<String> result =
                new ArrayList<>();

        for (KnowledgeSource source
                : getAvailableSources()) {

            if (source == null) {
                continue;
            }

            String id =
                    normalizeId(
                            source.getId()
                    );

            if (!id.isEmpty()) {
                result.add(id);
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * عدد المصادر المتاحة.
     */
    public synchronized int getAvailableCount() {

        int count = 0;

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            try {

                if (source.isAvailable()) {
                    count++;
                }

            } catch (Exception ignored) {
                // غير متاح.
            }
        }

        return count;
    }

    /**
     * معرفة هل يوجد مصدر متاح.
     */
    public synchronized boolean hasAvailableSource() {

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            try {

                if (source.isAvailable()) {
                    return true;
                }

            } catch (Exception ignored) {
                // نستمر مع المصدر التالي.
            }
        }

        return false;
    }

    /**
     * البحث حسب نوع المصدر.
     *
     * مثال:
     * INTERNET
     * DOCUMENTATION
     * VIDEO
     * API
     */
    public synchronized List<KnowledgeSource>
    getSourcesByType(
            KnowledgeSource.SourceType type
    ) {

        if (type == null) {
            return Collections.emptyList();
        }

        List<KnowledgeSource> result =
                new ArrayList<>();

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            if (source.getType() == type) {
                result.add(source);
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * الحصول على المصادر التي تدعم التعلم المستمر.
     */
    public synchronized List<KnowledgeSource>
    getContinuousLearningSources() {

        List<KnowledgeSource> result =
                new ArrayList<>();

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            try {

                if (source.supportsContinuousLearning()) {
                    result.add(source);
                }

            } catch (Exception ignored) {
                // المصدر لا يدعم الميزة.
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * الحصول على المصادر التي تحتاج الشبكة.
     */
    public synchronized List<KnowledgeSource>
    getNetworkSources() {

        List<KnowledgeSource> result =
                new ArrayList<>();

        for (KnowledgeSource source
                : sources.values()) {

            if (source == null) {
                continue;
            }

            try {

                if (source.requiresNetwork()) {
                    result.add(source);
                }

            } catch (Exception ignored) {
                // نستمر.
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * التحقق من صحة المصدر.
     */
    private boolean isValidSource(
            KnowledgeSource source
    ) {

        if (source == null) {
            return false;
        }

        String id =
                normalizeId(
                        source.getId()
                );

        if (id.isEmpty()) {
            return false;
        }

        try {

            String name =
                    source.getName();

            String description =
                    source.getDescription();

            KnowledgeSource.SourceType type =
                    source.getType();

            /*
             * الاسم والوصف ليسا إلزاميين،
             * لكن النوع يجب أن يكون معروفاً.
             */
            return type != null;

        } catch (Exception ignored) {

            return false;
        }
    }

    /**
     * توحيد ID المصدر.
     */
    private String normalizeId(
            String sourceId
    ) {

        if (sourceId == null) {
            return "";
        }

        return sourceId.trim();
    }

    public String getRegistryId() {
        return REGISTRY_ID;
    }
}