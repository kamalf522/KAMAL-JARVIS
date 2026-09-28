package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JARVIS V2 - Capability Activation
 *
 * مسؤول عن نقل Capability من:
 *
 * BUILT / TESTED
 *        ↓
 * ACTIVATION CHECK
 *        ↓
 * ACTIVE
 *
 * التفعيل هنا ليس مجرد تسجيل اسم.
 *
 * القدرة لا تعتبر ACTIVE إلا إذا:
 * 1. كانت بياناتها صحيحة.
 * 2. عندها Tool حقيقية قابلة للتنفيذ.
 * 3. الـTool موجودة في ToolRegistry.
 * 4. الـTool متاحة.
 *
 * هذه الطبقة لا تمنح Android permissions.
 * ولا تتجاوز OwnerSecurityBoundary.
 * ولا تبني الكود.
 *
 * البناء والاختبار مسؤولية Evolution pipeline.
 * التنفيذ مسؤولية CapabilityExecutor.
 */
public final class CapabilityActivation {

    private static final String ENGINE_ID =
            "v2.capability_activation";

    private final ToolRegistry toolRegistry;

    private final Map<String, ActivationRecord>
            activations =
            new LinkedHashMap<>();

    public CapabilityActivation(
            ToolRegistry toolRegistry
    ) {

        if (toolRegistry == null) {
            throw new IllegalArgumentException(
                    "toolRegistry cannot be null."
            );
        }

        this.toolRegistry =
                toolRegistry;
    }

    /**
     * تفعيل Capability بعد التأكد من وجود
     * Tool حقيقية قابلة للتنفيذ.
     */
    public synchronized JarvisResult<ActivationRecord>
    activate(
            CapabilitySpec spec
    ) {

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (!spec.isValid()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "CapabilitySpec is invalid."
            );
        }

        String capabilityId =
                normalizeId(
                        spec.getCapabilityId()
                );

        if (capabilityId.isEmpty()) {

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability ID cannot be empty."
            );
        }

        /*
         * نبحث أولاً عن الأدوات المطلوبة.
         */
        List<String> toolIds =
                collectToolIds(
                        spec
                );

        if (toolIds.isEmpty()) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Capability has no execution tool."
            );
        }

        List<String> availableTools =
                new ArrayList<>();

        List<String> unavailableTools =
                new ArrayList<>();

        for (String toolId : toolIds) {

            ToolContract tool =
                    getTool(
                            toolId
                    );

            if (tool == null) {

                unavailableTools.add(
                        toolId
                );

                continue;
            }

            if (!tool.isAvailable()) {

                unavailableTools.add(
                        toolId
                );

                continue;
            }

            availableTools.add(
                    toolId
            );
        }

        /*
         * لا يوجد Tool حقيقي:
         * لا يتم التفعيل.
         */
        if (availableTools.isEmpty()) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Capability cannot be activated because no required execution tool is available."
            );
        }

        ActivationRecord record =
                new ActivationRecord(
                        capabilityId,
                        spec.getName(),
                        ActivationState.ACTIVE,
                        availableTools,
                        unavailableTools,
                        "Capability activated with executable tools."
                );

        activations.put(
                capabilityId,
                record
        );

        return JarvisResult.success(
                record,
                record.getMessage()
        );
    }

    /**
     * تفعيل Capability باستعمال Tool IDs محددة.
     *
     * مفيدة عندما يكون الـEvolution pipeline
     * قد حدد الأدوات بنفسه.
     */
    public synchronized JarvisResult<ActivationRecord>
    activate(
            String capabilityId,
            String name,
            List<String> toolIds
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        if (id.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        if (toolIds == null ||
                toolIds.isEmpty()) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No execution tools were supplied."
            );
        }

        List<String> available =
                new ArrayList<>();

        List<String> unavailable =
                new ArrayList<>();

        for (String toolId : toolIds) {

            if (toolId == null ||
                    toolId.trim().isEmpty()) {
                continue;
            }

            String normalizedTool =
                    toolId.trim();

            ToolContract tool =
                    getTool(
                            normalizedTool
                    );

            if (tool != null &&
                    tool.isAvailable()) {

                available.add(
                        normalizedTool
                );

            } else {

                unavailable.add(
                        normalizedTool
                );
            }
        }

        if (available.isEmpty()) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No executable tool is available for capability: "
                            + id
            );
        }

        ActivationRecord record =
                new ActivationRecord(
                        id,
                        name,
                        ActivationState.ACTIVE,
                        available,
                        unavailable,
                        "Capability activated."
                );

        activations.put(
                id,
                record
        );

        return JarvisResult.success(
                record,
                record.getMessage()
        );
    }

    /**
     * تعطيل Capability.
     *
     * التعطيل يحذف حالة التفعيل فقط.
     * لا يحذف الملفات ولا الأدوات.
     */
    public synchronized JarvisResult<ActivationRecord>
    deactivate(
            String capabilityId
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        if (id.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        ActivationRecord current =
                activations.get(id);

        if (current == null) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Capability is not activated: "
                            + id
            );
        }

        ActivationRecord disabled =
                new ActivationRecord(
                        current.getCapabilityId(),
                        current.getName(),
                        ActivationState.DISABLED,
                        current.getAvailableTools(),
                        current.getUnavailableTools(),
                        "Capability deactivated."
                );

        activations.put(
                id,
                disabled
        );

        return JarvisResult.success(
                disabled,
                disabled.getMessage()
        );
    }

    /**
     * فحص هل Capability مفعلة وقابلة للاستعمال.
     */
    public synchronized boolean isActive(
            String capabilityId
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        ActivationRecord record =
                activations.get(id);

        if (record == null ||
                record.getState()
                        != ActivationState.ACTIVE) {

            return false;
        }

        /*
         * نعيد فحص الأدوات وقت الطلب،
         * لأن Tool قد تصبح unavailable بعد التفعيل.
         */
        for (
                String toolId
                : record.getAvailableTools()
        ) {

            ToolContract tool =
                    getTool(
                            toolId
                    );

            if (tool != null &&
                    tool.isAvailable()) {

                return true;
            }
        }

        return false;
    }

    /**
     * إرجاع سجل Capability.
     */
    public synchronized ActivationRecord get(
            String capabilityId
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        return activations.get(
                id
        );
    }

    /**
     * فحص وجود سجل.
     */
    public synchronized boolean contains(
            String capabilityId
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        return activations.containsKey(
                id
        );
    }

    /**
     * إرجاع كل Capabilities.
     */
    public synchronized List<ActivationRecord>
    getAll() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        activations.values()
                )
        );
    }

    /**
     * عدد Capabilities المفعلة فعلياً.
     */
    public synchronized int getActiveCount() {

        int count = 0;

        for (
                ActivationRecord record
                : activations.values()
        ) {

            if (record.getState()
                    == ActivationState.ACTIVE) {

                count++;
            }
        }

        return count;
    }

    /**
     * إزالة سجل Capability.
     *
     * لا تحذف Tool ولا ملفات المشروع.
     */
    public synchronized JarvisResult<Boolean>
    remove(
            String capabilityId
    ) {

        String id =
                normalizeId(
                        capabilityId
                );

        if (id.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        if (!activations.containsKey(id)) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Capability activation record not found: "
                            + id
            );
        }

        activations.remove(
                id
        );

        return JarvisResult.success(
                true,
                "Capability activation record removed."
        );
    }

    /**
     * يمسح سجلات التفعيل فقط.
     */
    public synchronized void clear() {
        activations.clear();
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    public synchronized String getStatus() {

        return "active="
                + getActiveCount()
                + ", registered="
                + activations.size();
    }

    /**
     * استخراج الأدوات من CapabilitySpec.
     */
    private List<String> collectToolIds(
            CapabilitySpec spec
    ) {

        LinkedHashMap<String, Boolean>
                unique =
                new LinkedHashMap<>();

        for (
                String id
                : spec.getRequiredTools()
        ) {

            addToolId(
                    unique,
                    id
            );
        }

        for (
                String id
                : spec.getPreferredTools()
        ) {

            addToolId(
                    unique,
                    id
            );
        }

        for (
                String id
                : spec.getAlternativeTools()
        ) {

            addToolId(
                    unique,
                    id
            );
        }

        return new ArrayList<>(
                unique.keySet()
        );
    }

    private void addToolId(
            Map<String, Boolean> target,
            String id
    ) {

        if (id == null ||
                id.trim().isEmpty()) {

            return;
        }

        target.put(
                id.trim(),
                Boolean.TRUE
        );
    }

    /**
     * الوصول إلى ToolRegistry بطريقة
     * متوافقة مع Result-based API.
     */
    private ToolContract getTool(
            String toolId
    ) {

        if (toolId == null ||
                toolId.trim().isEmpty()) {

            return null;
        }

        try {

            JarvisResult<ToolContract>
                    result =
                    toolRegistry.get(
                            toolId.trim()
                    );

            if (result == null ||
                    !result.isSuccess()) {

                return null;
            }

            return result.getData();

        } catch (Exception ignored) {

            return null;
        }
    }

    private String normalizeId(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        ENGINE_ID
                )
        );
    }

    public enum ActivationState {

        ACTIVE,

        DISABLED
    }

    /**
     * سجل التفعيل.
     */
    public static final class ActivationRecord {

        private final String capabilityId;
        private final String name;
        private final ActivationState state;
        private final List<String> availableTools;
        private final List<String> unavailableTools;
        private final String message;

        private ActivationRecord(
                String capabilityId,
                String name,
                ActivationState state,
                List<String> availableTools,
                List<String> unavailableTools,
                String message
        ) {

            this.capabilityId =
                    capabilityId == null
                            ? ""
                            : capabilityId;

            this.name =
                    name == null
                            ? ""
                            : name;

            this.state =
                    state == null
                            ? ActivationState.DISABLED
                            : state;

            this.availableTools =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    availableTools == null
                                            ? Collections.emptyList()
                                            : availableTools
                            )
                    );

            this.unavailableTools =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    unavailableTools == null
                                            ? Collections.emptyList()
                                            : unavailableTools
                            )
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public String getName() {
            return name;
        }

        public ActivationState getState() {
            return state;
        }

        public List<String> getAvailableTools() {
            return availableTools;
        }

        public List<String> getUnavailableTools() {
            return unavailableTools;
        }

        public String getMessage() {
            return message;
        }

        public boolean isActive() {
            return state ==
                    ActivationState.ACTIVE;
        }

        public boolean hasExecutableTool() {
            return !availableTools.isEmpty();
        }

        @Override
        public String toString() {

            return "ActivationRecord{" +
                    "capabilityId='" +
                    capabilityId + '\'' +
                    ", name='" +
                    name + '\'' +
                    ", state=" +
                    state +
                    ", availableTools=" +
                    availableTools +
                    ", unavailableTools=" +
                    unavailableTools +
                    '}';
        }
    }
}