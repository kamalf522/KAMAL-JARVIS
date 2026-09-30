package com.kamal.jarvis.v2.intelligence.learning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JARVIS V2
 *
 * KnowledgeItem
 *
 * يمثل وحدة معرفة يمكن لنظام التعلم الاحتفاظ بها
 * وتحليلها والتحقق منها واستعمالها في التطور.
 *
 * مهم:
 * وجود المعلومة هنا لا يعني أنها صحيحة.
 * الحالة ودرجة الثقة تحددان مستوى الاعتماد عليها.
 */
public final class KnowledgeItem {

    private final String id;
    private final String topic;
    private final String title;
    private final String content;
    private final String sourceId;
    private final String sourceLocation;
    private final long collectedAt;
    private final long updatedAt;
    private final KnowledgeStatus status;
    private final double confidence;
    private final Map<String, Object> metadata;

    private KnowledgeItem(
            String id,
            String topic,
            String title,
            String content,
            String sourceId,
            String sourceLocation,
            long collectedAt,
            long updatedAt,
            KnowledgeStatus status,
            double confidence,
            Map<String, Object> metadata
    ) {

        this.id = normalize(id);
        this.topic = normalize(topic);
        this.title = normalize(title);
        this.content = normalize(content);
        this.sourceId = normalize(sourceId);
        this.sourceLocation = normalize(sourceLocation);

        this.collectedAt = collectedAt;
        this.updatedAt = updatedAt;

        this.status =
                status == null
                        ? KnowledgeStatus.NEW
                        : status;

        this.confidence =
                clampConfidence(confidence);

        Map<String, Object> copy =
                metadata == null
                        ? Collections.emptyMap()
                        : new LinkedHashMap<>(metadata);

        this.metadata =
                Collections.unmodifiableMap(copy);
    }

    /**
     * إنشاء معرفة جديدة.
     */
    public static KnowledgeItem create(
            String id,
            String topic,
            String title,
            String content,
            String sourceId,
            String sourceLocation
    ) {

        long now =
                System.currentTimeMillis();

        return new KnowledgeItem(
                id,
                topic,
                title,
                content,
                sourceId,
                sourceLocation,
                now,
                now,
                KnowledgeStatus.NEW,
                0.0d,
                Collections.emptyMap()
        );
    }

    /**
     * إنشاء نسخة مع metadata.
     */
    public KnowledgeItem withMetadata(
            String key,
            Object value
    ) {

        if (key == null ||
                key.trim().isEmpty()) {

            return this;
        }

        Map<String, Object> copy =
                new LinkedHashMap<>(metadata);

        copy.put(
                key.trim(),
                value
        );

        return copy(
                status,
                confidence,
                copy
        );
    }

    /**
     * تغيير حالة المعرفة.
     */
    public KnowledgeItem withStatus(
            KnowledgeStatus newStatus
    ) {

        if (newStatus == null) {
            return this;
        }

        return copy(
                newStatus,
                confidence,
                metadata
        );
    }

    /**
     * تحديث درجة الثقة.
     */
    public KnowledgeItem withConfidence(
            double newConfidence
    ) {

        return copy(
                status,
                newConfidence,
                metadata
        );
    }

    /**
     * تحديث المحتوى.
     *
     * يستعمل فقط عندما تكون هناك نسخة جديدة
     * من نفس المعرفة.
     */
    public KnowledgeItem withContent(
            String newContent
    ) {

        long now =
                System.currentTimeMillis();

        return new KnowledgeItem(
                id,
                topic,
                title,
                newContent,
                sourceId,
                sourceLocation,
                collectedAt,
                now,
                status,
                confidence,
                metadata
        );
    }

    private KnowledgeItem copy(
            KnowledgeStatus newStatus,
            double newConfidence,
            Map<String, Object> newMetadata
    ) {

        return new KnowledgeItem(
                id,
                topic,
                title,
                content,
                sourceId,
                sourceLocation,
                collectedAt,
                System.currentTimeMillis(),
                newStatus,
                newConfidence,
                newMetadata
        );
    }

    private static String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private static double clampConfidence(
            double value
    ) {

        if (Double.isNaN(value) ||
                Double.isInfinite(value)) {

            return 0.0d;
        }

        if (value < 0.0d) {
            return 0.0d;
        }

        if (value > 1.0d) {
            return 1.0d;
        }

        return value;
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public long getCollectedAt() {
        return collectedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public KnowledgeStatus getStatus() {
        return status;
    }

    public double getConfidence() {
        return confidence;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Object getMetadata(
            String key
    ) {

        if (key == null) {
            return null;
        }

        return metadata.get(key);
    }

    public boolean hasMetadata(
            String key
    ) {

        return key != null &&
                metadata.containsKey(key);
    }

    /**
     * هل المعرفة قابلة للاستعمال؟
     *
     * المعرفة غير المتحققة يمكن أن تبقى مخزنة،
     * لكن لا تعتبر معرفة موثوقة تلقائياً.
     */
    public boolean isUsable() {

        return !content.isEmpty()
                &&
                status != KnowledgeStatus.REJECTED;
    }

    /**
     * هل تم التحقق منها؟
     */
    public boolean isVerified() {

        return status ==
                KnowledgeStatus.VERIFIED;
    }

    /**
     * هل يمكن اعتمادها؟
     */
    public boolean isTrusted() {

        return status ==
                KnowledgeStatus.VERIFIED
                &&
                confidence >= 0.70d;
    }

    @Override
    public String toString() {

        return "KnowledgeItem{" +
                "id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", status=" + status +
                ", confidence=" + confidence +
                '}';
    }

    /**
     * حالة المعرفة.
     */
    public enum KnowledgeStatus {

        NEW,

        COLLECTED,

        ANALYZING,

        VERIFYING,

        VERIFIED,

        NEEDS_REVIEW,

        REJECTED,

        OUTDATED
    }
}