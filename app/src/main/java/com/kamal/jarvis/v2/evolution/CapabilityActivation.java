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
 * مسؤول عن المرحلة الأخيرة من دورة القدرة:
 *
 * DISCOVERED
 *     ↓
 * BUILT
 *     ↓
 * VERIFIED
 *     ↓
 * REGISTERED
 *     ↓
 * ACTIVE
 *
 * هذه الطبقة لا تبني الكود ولا تمنح Android permissions.
 *
 * مسؤوليتها:
 *
 * 1. التحقق من Capability.
 * 2. التحقق من Tool القابلة للتنفيذ.
 * 3. تسجيل Tool الجديدة عند الحاجة.
 * 4. تفعيل Capability.
 * 5. منع التفعيل إذا فشل التسجيل.
 * 6. Rollback للتسجيل عند فشل التفعيل.
 * 7. إعادة التحقق من حالة Tool وقت الاستعمال.
 *
 * مهم:
 *
 * تسجيل Tool هنا يعني أن Tool أصبحت جزءاً من Runtime
 * الحالي. أما إنشاء Java source جديد أو بناء APK جديد
 * فهو مسؤولية Evolution pipeline.
 */
public final class CapabilityActivation {

    private static final String ENGINE_ID =
            "v2.capability_activation";

    private final ToolRegistry toolRegistry;

    private final Map<String, ActivationRecord> activations =
            new LinkedHashMap<>();

    public CapabilityActivation(
            ToolRegistry toolRegistry
    ) {

        if (toolRegistry == null) {
            throw new IllegalArgumentException(
                    "toolRegistry cannot be null."
            );
        }

        this.toolRegistry = toolRegistry;
    }

    /**
     * تفعيل Capability باستعمال Tool موجودة مسبقاً.
     *
     * هذا هو المسار القديم والمحافظ على التوافق.
     */
    public synchronized JarvisResult<ActivationRecord> activate(
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
                normalizeId(spec.getCapabilityId());

        if (capabilityId.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability ID cannot be empty."
            );
        }

        List<String> toolIds =
                collectToolIds(spec);

        if (toolIds.isEmpty()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Capability has no execution tool."
            );
        }

        return activateUsingRegisteredTools(
                capabilityId,
                spec.getName(),
                toolIds
        );
    }

    /**
     * المسار الأساسي الجديد:
     *
     * يسجل Tool جديدة فعلياً ثم يفعل Capability.
     *
     * إذا فشل التسجيل أو التفعيل:
     * لا تبقى Tool جديدة معلقة داخل Registry.
     */
    public synchronized JarvisResult<ActivationRecord> activate(
            CapabilitySpec spec,
            ToolContract generatedTool
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

        if (generatedTool == null) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Generated capability tool cannot be null."
            );
        }

        String capabilityId =
                normalizeId(spec.getCapabilityId());

        if (capabilityId.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Capability ID cannot be empty."
            );
        }

        String toolId =
                normalizeId(generatedTool.getId());

        if (toolId.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Generated capability tool id cannot be empty."
            );
        }

        /*
         * يجب أن تكون Tool قابلة للتنفيذ قبل التسجيل.
         */
        if (!generatedTool.isAvailable()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Generated capability tool is not available."
            );
        }

        /*
         * لا نسمح بتجاوز Tool موجودة بشكل صامت.
         *
         * إذا كانت موجودة، يجب استعمال replace صراحة
         * عبر المسار المخصص أدناه.
         */
        if (toolRegistry.contains(toolId)) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "A tool with this id is already registered: "
                            + toolId
            );
        }

        JarvisResult<Void> registration =
                toolRegistry.register(generatedTool);

        if (registration == null ||
                !registration.isSuccess()) {

            if (registration == null) {
                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Tool registration returned no result."
                );
            }

            return JarvisResult.failure(
                    registration.getError()
            );
        }

        /*
         * من هذه اللحظة Tool أصبحت داخل Runtime Registry.
         *
         * نتحقق مرة ثانية قبل جعل Capability ACTIVE.
         */
        ToolContract registeredTool =
                getTool(toolId);

        if (registeredTool == null ||
                !registeredTool.isAvailable()) {

            /*
             * Rollback:
             * لا نترك Tool نصف مفعلة.
             */
            toolRegistry.remove(toolId);

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Registered capability tool failed activation validation."
            );
        }

        ActivationRecord record =
                createActiveRecord(
                        capabilityId,
                        spec.getName(),
                        Collections.singletonList(toolId),
                        Collections.emptyList(),
                        "Capability was registered and activated successfully."
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
     * تسجيل Tool جديدة وتفعيلها بدون CapabilitySpec.
     *
     * يستعمله Evolution runtime عندما تكون Capability
     * already validated elsewhere.
     */
    public synchronized JarvisResult<ActivationRecord>
    registerAndActivate(
            String capabilityId,
            String name,
            ToolContract tool
    ) {

        String id =
                normalizeId(capabilityId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        if (tool == null) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool cannot be null."
            );
        }

        String toolId =
                normalizeId(tool.getId());

        if (toolId.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool id cannot be empty."
            );
        }

        if (!tool.isAvailable()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool is not available."
            );
        }

        if (toolRegistry.contains(toolId)) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Tool already exists: " + toolId
            );
        }

        JarvisResult<Void> registration =
                toolRegistry.register(tool);

        if (registration == null ||
                !registration.isSuccess()) {

            return registration == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Tool registration returned no result."
                    )
                    : JarvisResult.failure(
                            registration.getError()
                    );
        }

        ToolContract verifiedTool =
                getTool(toolId);

        if (verifiedTool == null ||
                !verifiedTool.isAvailable()) {

            toolRegistry.remove(toolId);

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool failed post-registration validation."
            );
        }

        ActivationRecord record =
                createActiveRecord(
                        id,
                        name,
                        Collections.singletonList(toolId),
                        Collections.emptyList(),
                        "Capability registered and activated."
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
     * استبدال Tool موجودة بنسخة مطورة.
     *
     * هذا المسار مهم عندما JARVIS يطور Capability موجودة
     * إلى إصدار أحدث.
     *
     * إذا فشل replace:
     * تبقى النسخة القديمة كما هي.
     */
    public synchronized JarvisResult<ActivationRecord>
    upgrade(
            String capabilityId,
            String name,
            ToolContract upgradedTool
    ) {

        String id =
                normalizeId(capabilityId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        if (upgradedTool == null) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Upgraded tool cannot be null."
            );
        }

        String toolId =
                normalizeId(upgradedTool.getId());

        if (toolId.isEmpty()) {
            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    "Upgraded tool id cannot be empty."
            );
        }

        if (!upgradedTool.isAvailable()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Upgraded tool is not available."
            );
        }

        ToolContract oldTool =
                getTool(toolId);

        JarvisResult<Void> replaceResult =
                toolRegistry.replace(upgradedTool);

        if (replaceResult == null ||
                !replaceResult.isSuccess()) {

            return replaceResult == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Tool replacement returned no result."
                    )
                    : JarvisResult.failure(
                            replaceResult.getError()
                    );
        }

        ToolContract verifiedTool =
                getTool(toolId);

        if (verifiedTool == null ||
                !verifiedTool.isAvailable()) {

            /*
             * Recovery:
             * إذا كانت النسخة القديمة موجودة، نرجعها.
             * إذا لم تكن موجودة، نحذف النسخة الجديدة.
             */
            if (oldTool != null) {
                toolRegistry.replace(oldTool);
            } else {
                toolRegistry.remove(toolId);
            }

            return failure(
                    JarvisError.Type.RECOVERY_FAILED,
                    "Upgraded tool failed validation and was rolled back."
            );
        }

        List<String> tools =
                Collections.singletonList(toolId);

        ActivationRecord record =
                createActiveRecord(
                        id,
                        name,
                        tools,
                        Collections.emptyList(),
                        "Capability upgraded and activated successfully."
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
     * تفعيل Capability باستعمال مجموعة Tools مسجلة.
     */
    public synchronized JarvisResult<ActivationRecord>
    activate(
            String capabilityId,
            String name,
            List<String> toolIds
    ) {

        String id =
                normalizeId(capabilityId);

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

        return activateUsingRegisteredTools(
                id,
                name,
                toolIds
        );
    }

    /**
     * تعطيل Capability.
     *
     * لا يحذف Tool ولا ملفات المشروع.
     */
    public synchronized JarvisResult<ActivationRecord>
    deactivate(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

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
                    "Capability is not activated: " + id
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
     * فحص Capability وقت الاستعمال.
     */
    public synchronized boolean isActive(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

        ActivationRecord record =
                activations.get(id);

        if (record == null ||
                record.getState() !=
                        ActivationState.ACTIVE) {

            return false;
        }

        /*
         * يجب أن تكون Tool واحدة على الأقل
         * قابلة للتنفيذ في الوقت الحالي.
         */
        for (String toolId :
                record.getAvailableTools()) {

            ToolContract tool =
                    getTool(toolId);

            if (tool != null &&
                    tool.isAvailable()) {

                return true;
            }
        }

        return false;
    }

    /**
     * فحص أن Capability مسجلة وقابلة للتنفيذ.
     */
    public synchronized boolean isExecutable(
            String capabilityId
    ) {

        return isActive(capabilityId);
    }

    public synchronized ActivationRecord get(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

        return activations.get(id);
    }

    public synchronized boolean contains(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

        return activations.containsKey(id);
    }

    public synchronized List<ActivationRecord>
    getAll() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        activations.values()
                )
        );
    }

    public synchronized int getActiveCount() {

        int count = 0;

        for (ActivationRecord record :
                activations.values()) {

            if (record.getState() ==
                    ActivationState.ACTIVE) {

                count++;
            }
        }

        return count;
    }

    /**
     * عدد القدرات المسجلة.
     */
    public synchronized int getCount() {
        return activations.size();
    }

    /**
     * إزالة سجل التفعيل فقط.
     *
     * لا تحذف Tool ولا ملفات المشروع.
     */
    public synchronized JarvisResult<Boolean>
    remove(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

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

        activations.remove(id);

        return JarvisResult.success(
                true,
                "Capability activation record removed."
        );
    }

    /**
     * إزالة Capability من Runtime Registry
     * مع إزالة سجل التفعيل.
     *
     * لا تستعمل هذه العملية لإلغاء ملفات المشروع.
     */
    public synchronized JarvisResult<Boolean>
    unregisterRuntimeTool(
            String capabilityId
    ) {

        String id =
                normalizeId(capabilityId);

        if (id.isEmpty()) {
            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Capability ID cannot be empty."
            );
        }

        ActivationRecord record =
                activations.get(id);

        if (record == null) {
            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Capability activation not found: "
                            + id
            );
        }

        boolean removedAny =
                false;

        for (String toolId :
                record.getAvailableTools()) {

            JarvisResult<Void> result =
                    toolRegistry.remove(toolId);

            if (result != null &&
                    result.isSuccess()) {

                removedAny = true;
            }
        }

        activations.remove(id);

        return JarvisResult.success(
                removedAny,
                "Capability runtime registration removed."
        );
    }

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
                + activations.size()
                + ", runtime_tools="
                + toolRegistry.getToolCount();
    }

    /**
     * تفعيل مجموعة Tools موجودة في Registry.
     */
    private JarvisResult<ActivationRecord>
    activateUsingRegisteredTools(
            String capabilityId,
            String name,
            List<String> toolIds
    ) {

        LinkedHashMap<String, Boolean> unique =
                new LinkedHashMap<>();

        for (String toolId : toolIds) {

            if (toolId == null ||
                    toolId.trim().isEmpty()) {

                continue;
            }

            unique.put(
                    toolId.trim(),
                    Boolean.TRUE
            );
        }

        if (unique.isEmpty()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No valid execution tools were supplied."
            );
        }

        List<String> available =
                new ArrayList<>();

        List<String> unavailable =
                new ArrayList<>();

        for (String toolId :
                unique.keySet()) {

            ToolContract tool =
                    getTool(toolId);

            if (tool == null) {
                unavailable.add(toolId);
                continue;
            }

            if (!tool.isAvailable()) {
                unavailable.add(toolId);
                continue;
            }

            available.add(toolId);
        }

        /*
         * لا نعتبر Capability ACTIVE
         * إذا لم تكن هناك Tool واحدة قابلة للتنفيذ.
         */
        if (available.isEmpty()) {
            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "No executable tool is available for capability: "
                            + capabilityId
            );
        }

        ActivationRecord record =
                createActiveRecord(
                        capabilityId,
                        name,
                        available,
                        unavailable,
                        "Capability activated with executable runtime tools."
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
     * استخراج Tools من CapabilitySpec.
     */
    private List<String> collectToolIds(
            CapabilitySpec spec
    ) {

        LinkedHashMap<String, Boolean> unique =
                new LinkedHashMap<>();

        for (String id :
                spec.getRequiredTools()) {

            addToolId(unique, id);
        }

        for (String id :
                spec.getPreferredTools()) {

            addToolId(unique, id);
        }

        for (String id :
                spec.getAlternativeTools()) {

            addToolId(unique, id);
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
     * جلب Tool من Registry.
     */
    private ToolContract getTool(
            String toolId
    ) {

        if (toolId == null ||
                toolId.trim().isEmpty()) {

            return null;
        }

        try {

            JarvisResult<ToolContract> result =
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

    private ActivationRecord createActiveRecord(
            String capabilityId,
            String name,
            List<String> availableTools,
            List<String> unavailableTools,
            String message
    ) {

        return new ActivationRecord(
                capabilityId,
                name,
                ActivationState.ACTIVE,
                availableTools,
                unavailableTools,
                message
        );
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
     * سجل تفعيل Capability.
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