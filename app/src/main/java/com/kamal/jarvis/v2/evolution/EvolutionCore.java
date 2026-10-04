package com.kamal.jarvis.v2.evolution;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.permissions.CapabilityPermission;
import com.kamal.jarvis.v2.permissions.CapabilityRequirement;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JARVIS V2 - Evolution Core
 *
 * العقل المركزي لمنظومة Evolution.
 *
 * REQUEST
 * -> DISCOVERY
 * -> PLANNING
 * -> EXECUTION / PERMISSION / BUILD
 * -> CODE GENERATION
 * -> SOURCE EVOLUTION
 * -> BUILD
 * -> TEST
 * -> VERIFICATION
 * -> ACTIVATION
 * -> READY
 *
 * Learning integration:
 * JarvisBrain يمكنه تمرير المعرفة الخارجية الموثقة
 * عبر ToolInput بدون جعل EvolutionCore مرتبطاً مباشرة
 * بـ KnowledgeLearningEngine.
 *
 * Code generation integration:
 * CodeGenerationEngine مسؤول عن اختيار مصدر توليد
 * الكود، التحقق من الملفات الناتجة، وتحويلها إلى
 * SourceEvolutionEngine.ChangeSet.
 *
 * الكتابة الفعلية للمشروع لا تتم مباشرة هنا.
 * EvolutionOrchestrator هو المسؤول عن تطبيق ChangeSet
 * ثم البناء والاختبار والتحقق والاسترجاع عند الفشل.
 */
public final class EvolutionCore {

    private static final String CORE_ID =
            "v2.evolution_core";

    private final CapabilityDiscovery discovery;
    private final CapabilityExecutor executor;
    private final EvolutionOrchestrator orchestrator;
    private final CapabilityActivation activation;
    private final CodeGenerationEngine codeGenerationEngine;

    private final Map<String, EvolutionRecord> records =
            new ConcurrentHashMap<>();

    private volatile EvolutionState state =
            EvolutionState.IDLE;

    private volatile EvolutionRecord lastRecord;

    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor,
            EvolutionOrchestrator orchestrator
    ) {

        this(
                discovery,
                executor,
                orchestrator,
                null
        );
    }

    public EvolutionCore(
            CapabilityDiscovery discovery,
            CapabilityExecutor executor,
            EvolutionOrchestrator orchestrator,
            ToolRegistry toolRegistry
    ) {

        if (discovery == null) {
            throw new IllegalArgumentException(
                    "discovery cannot be null."
            );
        }

        if (executor == null) {
            throw new IllegalArgumentException(
                    "executor cannot be null."
            );
        }

        if (orchestrator == null) {
            throw new IllegalArgumentException(
                    "orchestrator cannot be null."
            );
        }

        this.discovery =
                discovery;

        this.executor =
                executor;

        this.orchestrator =
                orchestrator;

        this.activation =
                toolRegistry == null
                        ? null
                        : new CapabilityActivation(
                                toolRegistry
                        );

        this.codeGenerationEngine =
                new CodeGenerationEngine(
                        orchestrator.getSecurityBoundary()
                );
    }

    public synchronized JarvisResult<EvolutionAnalysis> analyze(
            CapabilityRequirement requirement
    ) {

        if (requirement == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
            );
        }

        state =
                EvolutionState.ANALYZING;

        CapabilityDiscovery.DiscoveryResult discovered =
                discovery.discover(
                        requirement
                );

        if (discovered == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability discovery returned no result."
            );
        }

        if (!discovered.isValid()) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.VALIDATION_FAILED,
                    discovered.getMessage()
            );
        }

        CapabilityPlan plan =
                CapabilityPlan.fromDiscovery(
                        discovered
                );

        if (plan == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability plan could not be created."
            );
        }

        EvolutionAnalysis analysis =
                new EvolutionAnalysis(
                        requirement,
                        discovered,
                        plan
                );

        switch (plan.getAction()) {

            case EXECUTE_DIRECT:
            case EXECUTE_ALTERNATIVE:

                state =
                        EvolutionState.READY_TO_EXECUTE;

                break;

            case REQUEST_PERMISSION:

                state =
                        EvolutionState.WAITING_FOR_PERMISSION;

                break;

            case BUILD_CAPABILITY:

                state =
                        EvolutionState.BUILD_REQUIRED;

                break;

            case UNAVAILABLE:
            default:

                state =
                        EvolutionState.FAILED;

                break;
        }

        return JarvisResult.success(
                analysis,
                "Evolution analysis completed."
        );
    }

    public synchronized JarvisResult<EvolutionExecutionResult> execute(
            CapabilityRequirement requirement,
            ToolContract.ToolInput input
    ) {

        if (requirement == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilityRequirement cannot be null."
            );
        }

        JarvisResult<EvolutionAnalysis> analysisResult =
                analyze(
                        requirement
                );

        if (analysisResult == null ||
                !analysisResult.isSuccess()) {

            state =
                    EvolutionState.FAILED;

            return analysisResult == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Evolution analysis returned no result."
                    )
                    : JarvisResult.failure(
                            analysisResult.getError()
                    );
        }

        EvolutionAnalysis analysis =
                analysisResult.getData();

        if (analysis == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis returned no data."
            );
        }

        CapabilityPlan plan =
                analysis.getPlan();

        if (plan == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution analysis contains no plan."
            );
        }

        /*
         * DIRECT / ALTERNATIVE
         */
        if (plan.getAction()
                == CapabilityPlan.Action.EXECUTE_DIRECT
                ||
                plan.getAction()
                == CapabilityPlan.Action.EXECUTE_ALTERNATIVE) {

            state =
                    EvolutionState.EXECUTING;

            JarvisResult<ToolContract.ToolOutput>
                    executionResult =
                    executor.execute(
                            plan,
                            input
                    );

            if (executionResult == null ||
                    !executionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return executionResult == null
                        ? failure(
                                JarvisError.Type.EXECUTION_FAILED,
                                "Capability executor returned no result."
                        )
                        : JarvisResult.failure(
                                executionResult.getError()
                        );
            }

            state =
                    EvolutionState.COMPLETED;

            EvolutionExecutionResult result =
                    EvolutionExecutionResult.executed(
                            plan,
                            executionResult.getData(),
                            executionResult.getMessage()
                    );

            return JarvisResult.success(
                    result,
                    result.getMessage()
            );
        }

        /*
         * PERMISSION
         */
        if (plan.getAction()
                == CapabilityPlan.Action.REQUEST_PERMISSION) {

            state =
                    EvolutionState.WAITING_FOR_PERMISSION;

            EvolutionExecutionResult waiting =
                    EvolutionExecutionResult.permissionRequired(
                            plan,
                            plan.getMissingPermissions()
                    );

            return JarvisResult.success(
                    waiting,
                    waiting.getMessage()
            );
        }

        /*
         * BUILD CAPABILITY
         *
         * إذا كان عندنا Provider حقيقي داخل
         * CodeGenerationEngine، نحاول توليد الكود
         * وتحويله إلى ChangeSet.
         *
         * إذا لم يوجد Provider بعد، يبقى المسار
         * القديم محفوظاً ولا يتم اختراع نجاح وهمي.
         */
        if (plan.getAction()
                == CapabilityPlan.Action.BUILD_CAPABILITY) {

            state =
                    EvolutionState.BUILDING;

            CapabilitySpec spec;

            try {

                spec =
                        createCapabilitySpec(
                                requirement,
                                plan,
                                input
                        );

            } catch (Exception exception) {

                state =
                        EvolutionState.FAILED;

                return JarvisResult.failure(
                        JarvisError.fromException(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "Failed to create CapabilitySpec.",
                                CORE_ID,
                                exception
                        )
                );
            }

            if (spec == null ||
                    !spec.isValid()) {

                state =
                        EvolutionState.FAILED;

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Generated CapabilitySpec is invalid."
                );
            }

            /*
             * CODE GENERATION PATH
             *
             * لا نستعمله إلا إذا كان هناك Provider
             * مسجل ومتاح.
             */
            if (codeGenerationEngine != null &&
                    codeGenerationEngine.getProviderCount() > 0) {

                state =
                        EvolutionState.CODE_GENERATING;

                CodeGenerationEngine.GenerationRequest
                        generationRequest;

                try {

                    generationRequest =
                            new CodeGenerationEngine.GenerationRequest(
                                    spec.getCapabilityId(),
                                    spec.getGoal(),
                                    spec.getDescription(),
                                    "Generate implementation for capability: "
                                            + spec.getCapabilityId(),
                                    "",
                                    null,
                                    new java.util.LinkedHashSet<>(
                                            spec.getAllowedFiles()
                                    ),
                                    new java.util.LinkedHashSet<>(
                                            spec.getRequiredFiles()
                                    ),
                                    new java.util.LinkedHashSet<>(
                                            spec.getRequiredTools()
                                    ),
                                    new java.util.LinkedHashSet<>(
                                            spec.getSuccessCriteria()
                                    )
                            );

                } catch (Exception exception) {

                    state =
                            EvolutionState.FAILED;

                    return JarvisResult.failure(
                            JarvisError.fromException(
                                    JarvisError.Type.EVOLUTION_FAILED,
                                    "Failed to create code generation request.",
                                    CORE_ID,
                                    exception
                            )
                    );
                }

                JarvisResult<
                        CodeGenerationEngine.GeneratedCode
                        > generationResult =
                        codeGenerationEngine.generate(
                                generationRequest
                        );

                if (generationResult == null ||
                        !generationResult.isSuccess()) {

                    state =
                            EvolutionState.FAILED;

                    return generationResult == null
                            ? failure(
                                    JarvisError.Type.EVOLUTION_FAILED,
                                    "Code generation returned no result."
                            )
                            : JarvisResult.failure(
                                    generationResult.getError()
                            );
                }

                CodeGenerationEngine.GeneratedCode
                        generatedCode =
                        generationResult.getData();

                if (generatedCode == null) {

                    state =
                            EvolutionState.FAILED;

                    return failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Code generation produced no validated code."
                    );
                }

                JarvisResult<
                        SourceEvolutionEngine.ChangeSet
                        > changeSetResult =
                        codeGenerationEngine.createChangeSet(
                                generationRequest,
                                generatedCode
                        );

                if (changeSetResult == null ||
                        !changeSetResult.isSuccess()) {

                    state =
                            EvolutionState.FAILED;

                    return changeSetResult == null
                            ? failure(
                                    JarvisError.Type.EVOLUTION_FAILED,
                                    "Generated code could not be converted into a ChangeSet."
                            )
                            : JarvisResult.failure(
                                    changeSetResult.getError()
                            );
                }

                SourceEvolutionEngine.ChangeSet
                        changeSet =
                        changeSetResult.getData();

                if (changeSet == null ||
                        !changeSet.isValid()) {

                    state =
                            EvolutionState.FAILED;

                    return failure(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Generated ChangeSet is invalid."
                    );
                }

                state =
                        EvolutionState.APPLYING_SOURCE_CHANGES;

                JarvisResult<
                        EvolutionOrchestrator.EvolutionRecord
                        > evolutionResult =
                        orchestrator.evolve(
                                spec,
                                changeSet
                        );

                if (evolutionResult == null ||
                        !evolutionResult.isSuccess()) {

                    state =
                            EvolutionState.FAILED;

                    return evolutionResult == null
                            ? failure(
                                    JarvisError.Type.EVOLUTION_FAILED,
                                    "EvolutionOrchestrator returned no result after code generation."
                            )
                            : JarvisResult.failure(
                                    evolutionResult.getError()
                            );
                }

                return finishEvolution(
                        requirement,
                        plan,
                        spec,
                        evolutionResult
                );
            }

            /*
             * FALLBACK
             *
             * Provider system موجود ولكن لا يوجد Provider
             * حقيقي بعد، لذلك نستعمل مسار Evolution
             * الحالي بدون ادعاء أن الكود تم توليده.
             */
            JarvisResult<
                    EvolutionOrchestrator.EvolutionRecord
                    > evolutionResult =
                    orchestrator.evolve(
                            spec
                    );

            if (evolutionResult == null ||
                    !evolutionResult.isSuccess()) {

                state =
                        EvolutionState.FAILED;

                return evolutionResult == null
                        ? failure(
                                JarvisError.Type.EVOLUTION_FAILED,
                                "EvolutionOrchestrator returned no result."
                        )
                        : JarvisResult.failure(
                                evolutionResult.getError()
                        );
            }

            return finishEvolution(
                    requirement,
                    plan,
                    spec,
                    evolutionResult
            );
        }

        state =
                EvolutionState.FAILED;

        return failure(
                JarvisError.Type.TOOL_UNAVAILABLE,
                "No valid execution or evolution path is available for capability: "
                        + requirement.getCapabilityId()
        );
    }

    private JarvisResult<EvolutionExecutionResult> finishEvolution(
            CapabilityRequirement requirement,
            CapabilityPlan plan,
            CapabilitySpec spec,
            JarvisResult<
                    EvolutionOrchestrator.EvolutionRecord
                    > evolutionResult
    ) {

        EvolutionOrchestrator.EvolutionRecord
                evolutionRecord =
                evolutionResult.getData();

        if (evolutionRecord == null) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution returned no record."
            );
        }

        if (!evolutionRecord.isReady()) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Evolution completed without producing a ready capability."
            );
        }

        CapabilityActivation.ActivationRecord
                activationRecord = null;

        if (activation != null) {

            state =
                    EvolutionState.ACTIVATING;

            JarvisResult<
                    CapabilityActivation.ActivationRecord
                    > activationResult =
                    activation.activate(
                            spec
                    );

            if (activationResult != null &&
                    activationResult.isSuccess()) {

                activationRecord =
                        activationResult.getData();

                if (activationRecord == null ||
                        !activationRecord.isActive()) {

                    state =
                            EvolutionState.FAILED;

                    return failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Capability activation returned an invalid active state."
                    );
                }

                state =
                        EvolutionState.ACTIVATED;

            } else {

                state =
                        EvolutionState.READY;
            }

        } else {

            state =
                    EvolutionState.READY;
        }

        saveRecord(
                requirement.getCapabilityId(),
                evolutionRecord,
                activationRecord
        );

        EvolutionExecutionResult built =
                EvolutionExecutionResult.built(
                        plan,
                        evolutionRecord,
                        activationRecord,
                        activationRecord != null
                                && activationRecord.isActive()
                                ? "Capability was built, verified and activated."
                                : "Capability was built and verified. Runtime activation is waiting for an executable tool."
                );

        return JarvisResult.success(
                built,
                built.getMessage()
        );
    }

    public synchronized JarvisResult<
            CapabilityActivation.ActivationRecord>
    activateGeneratedTool(
            CapabilitySpec spec,
            ToolContract generatedTool
    ) {

        if (activation == null) {

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Capability activation is not connected to a ToolRegistry."
            );
        }

        if (spec == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "CapabilitySpec cannot be null."
            );
        }

        if (generatedTool == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Generated Tool cannot be null."
            );
        }

        state =
                EvolutionState.ACTIVATING;

        JarvisResult<
                CapabilityActivation.ActivationRecord>
                result =
                activation.activate(
                        spec,
                        generatedTool
                );

        if (result == null ||
                !result.isSuccess()) {

            state =
                    EvolutionState.FAILED;

            return result == null
                    ? failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Capability activation returned no result."
                    )
                    : JarvisResult.failure(
                            result.getError()
                    );
        }

        CapabilityActivation.ActivationRecord
                record =
                result.getData();

        if (record == null ||
                !record.isActive()) {

            state =
                    EvolutionState.FAILED;

            return failure(
                    JarvisError.Type.EVOLUTION_FAILED,
                    "Generated capability was not activated."
            );
        }

        state =
                EvolutionState.ACTIVATED;

        return result;
    }

    /**
     * Creates the capability specification and incorporates
     * verified external knowledge supplied by JarvisBrain.
     */
    private CapabilitySpec createCapabilitySpec(
            CapabilityRequirement requirement,
            CapabilityPlan plan,
            ToolContract.ToolInput input
    ) {

        CapabilitySpec.Builder builder =
                CapabilitySpec.builder(
                        requirement.getCapabilityId(),
                        requirement.getCapabilityId()
                );

        builder.goal(
                requirement.getDescription()
        );

        String learningQuery =
                readStringParameter(
                        input,
                        "learning_query"
                );

        boolean learningCompleted =
                readBooleanParameter(
                        input,
                        "learning_completed"
                );

        List<KnowledgeEvidence>
                knowledgeEvidence =
                readKnowledgeEvidence(
                        input
                );

        StringBuilder description =
                new StringBuilder(
                        "Capability generated by JARVIS Evolution Core."
                );

        if (!learningQuery.isEmpty()) {

            description.append(
                    " Learning query: "
            ).append(
                    compact(
                            learningQuery,
                            500
                    )
            ).append('.');
        }

        if (learningCompleted &&
                !knowledgeEvidence.isEmpty()) {

            description.append(
                    " Development is informed by verified external knowledge."
            );

            builder.successCriterion(
                    "Capability design uses verified external knowledge."
            );

            for (KnowledgeEvidence evidence :
                    knowledgeEvidence) {

                String evidenceText =
                        evidence.toSpecificationText();

                if (!evidenceText.isEmpty()) {

                    builder.successCriterion(
                            "Knowledge evidence: "
                                    + evidenceText
                    );
                }
            }
        }

        builder.description(
                compact(
                        description.toString(),
                        3000
                )
        );

        Set<CapabilityPermission>
                requiredPermissions =
                requirement.getRequiredPermissions();

        if (requiredPermissions != null) {

            for (CapabilityPermission permission :
                    requiredPermissions) {

                builder.requirePermission(
                        permission
                );
            }
        }

        List<String> preferredTools =
                requirement.getPreferredToolIds();

        if (preferredTools != null) {

            for (String toolId :
                    preferredTools) {

                builder.preferTool(
                        toolId
                );
            }
        }

        List<String> alternativeTools =
                requirement.getAlternativeToolIds();

        if (alternativeTools != null) {

            for (String toolId :
                    alternativeTools) {

                builder.alternativeTool(
                        toolId
                );
            }
        }

        if (plan != null &&
                plan.getToolIds() != null) {

            for (String toolId :
                    plan.getToolIds()) {

                builder.requireTool(
                        toolId
                );
            }
        }

        if (requirement.isOwnerAuthorizationRequired()) {

            builder.requireOwnerAuthorization();
        }

        if (requirement.canBuildAlternative()) {

            builder.allowProjectModification();
        }

        builder.requireBuild();
        builder.requireTests();

        builder.successCriterion(
                "Generated capability specification exists."
        );

        builder.successCriterion(
                "Generated capability files are readable."
        );

        builder.successCriterion(
                "Generated capability contains its identifier."
        );

        if (!learningCompleted ||
                knowledgeEvidence.isEmpty()) {

            builder.successCriterion(
                    "No external knowledge was available for this evolution cycle."
            );
        }

        return builder.build();
    }

    private List<KnowledgeEvidence> readKnowledgeEvidence(
            ToolContract.ToolInput input
    ) {

        List<KnowledgeEvidence> result =
                new ArrayList<>();

        if (input == null) {
            return result;
        }

        Object raw =
                input.get(
                        "verified_knowledge_items"
                );

        if (!(raw instanceof List<?>)) {
            return result;
        }

        List<?> items =
                (List<?>) raw;

        int limit =
                Math.min(
                        items.size(),
                        8
                );

        for (int index = 0;
                index < limit;
                index++) {

            Object item =
                    items.get(index);

            if (!(item instanceof Map<?, ?>)) {
                continue;
            }

            Map<?, ?> map =
                    (Map<?, ?>) item;

            String content =
                    valueAsString(
                            map.get("content")
                    );

            if (content.isEmpty()) {
                continue;
            }

            String sourceId =
                    valueAsString(
                            map.get("source_id")
                    );

            String sourceLocation =
                    valueAsString(
                            map.get("source_location")
                    );

            String confidence =
                    valueAsString(
                            map.get("confidence")
                    );

            String status =
                    valueAsString(
                            map.get("status")
                    );

            result.add(
                    new KnowledgeEvidence(
                            content,
                            sourceId,
                            sourceLocation,
                            confidence,
                            status
                    )
            );
        }

        return result;
    }

    private String readStringParameter(
            ToolContract.ToolInput input,
            String key
    ) {

        if (input == null ||
                key == null ||
                key.trim().isEmpty()) {

            return "";
        }

        Object value =
                input.get(key);

        return valueAsString(
                value
        );
    }

    private boolean readBooleanParameter(
            ToolContract.ToolInput input,
            String key
    ) {

        if (input == null ||
                key == null ||
                key.trim().isEmpty()) {

            return false;
        }

        Object value =
                input.get(key);

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return "true".equalsIgnoreCase(
                valueAsString(
                        value
                )
        );
    }

    private String valueAsString(
            Object value
    ) {

        if (value == null) {
            return "";
        }

        return String.valueOf(
                value
        ).trim();
    }

    private static String compact(
            String value,
            int maxLength
    ) {

        if (value == null) {
            return "";
        }

        String cleaned =
                value
                        .replace(
                                '\n',
                                ' '
                        )
                        .replace(
                                '\r',
                                ' '
                        )
                        .replace(
                                '\t',
                                ' '
                        )
                        .trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(
                0,
                Math.max(
                        0,
                        maxLength - 3
                )
        ) + "...";
    }

    private static final class KnowledgeEvidence {

        private final String content;
        private final String sourceId;
        private final String sourceLocation;
        private final String confidence;
        private final String status;

        private KnowledgeEvidence(
                String content,
                String sourceId,
                String sourceLocation,
                String confidence,
                String status
        ) {

            this.content =
                    content;

            this.sourceId =
                    sourceId;

            this.sourceLocation =
                    sourceLocation;

            this.confidence =
                    confidence;

            this.status =
                    status;
        }

        private String toSpecificationText() {

            StringBuilder text =
                    new StringBuilder(
                            compact(
                                    content,
                                    700
                            )
                    );

            if (!sourceId.isEmpty()) {

                text.append(
                        " [source="
                ).append(
                        compact(
                                sourceId,
                                120
                        )
                ).append(']');
            }

            if (!sourceLocation.isEmpty()) {

                text.append(
                        " [location="
                ).append(
                        compact(
                                sourceLocation,
                                180
                        )
                ).append(']');
            }

            if (!confidence.isEmpty()) {

                text.append(
                        " [confidence="
                ).append(
                        compact(
                                confidence,
                                80
                        )
                ).append(']');
            }

            if (!status.isEmpty()) {

                text.append(
                        " [status="
                ).append(
                        compact(
                                status,
                                80
                        )
                ).append(']');
            }

            return compact(
                    text.toString(),
                    1200
            );
        }
    }

    private void saveRecord(
            String capabilityId,
            EvolutionOrchestrator.EvolutionRecord record
    ) {

        saveRecord(
                capabilityId,
                record,
                null
        );
    }

    private void saveRecord(
            String capabilityId,
            EvolutionOrchestrator.EvolutionRecord record,
            CapabilityActivation.ActivationRecord activationRecord
    ) {

        if (capabilityId == null ||
                capabilityId.trim().isEmpty() ||
                record == null) {

            return;
        }

        EvolutionRecord wrapper =
                new EvolutionRecord(
                        capabilityId,
                        record,
                        activationRecord
                );

        records.put(
                capabilityId,
                wrapper
        );

        lastRecord =
                wrapper;
    }

    private <T> JarvisResult<T> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        CORE_ID
                )
        );
    }

    public EvolutionRecord getRecord(
            String capabilityId
    ) {

        if (capabilityId == null) {
            return null;
        }

        return records.get(
                capabilityId
        );
    }

    public EvolutionRecord getLastRecord() {
        return lastRecord;
    }

    public List<EvolutionRecord> getRecords() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        records.values()
                )
        );
    }

    public EvolutionState getState() {
        return state;
    }

    public boolean isBusy() {

        return state ==
                EvolutionState.ANALYZING
                ||
                state ==
                EvolutionState.EXECUTING
                ||
                state ==
                EvolutionState.BUILDING
                ||
                state ==
                EvolutionState.CODE_GENERATING
                ||
                state ==
                EvolutionState.APPLYING_SOURCE_CHANGES
                ||
                state ==
                EvolutionState.ACTIVATING;
    }

    public boolean isReady() {

        return state ==
                EvolutionState.READY_TO_EXECUTE
                ||
                state ==
                EvolutionState.READY
                ||
                state ==
                EvolutionState.ACTIVATED
                ||
                state ==
                EvolutionState.COMPLETED;
    }

    public boolean isActivationConnected() {
        return activation != null;
    }

    public CapabilityActivation getActivation() {
        return activation;
    }

    public CapabilityDiscovery getDiscovery() {
        return discovery;
    }

    public CapabilityExecutor getExecutor() {
        return executor;
    }

    public EvolutionOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public CodeGenerationEngine getCodeGenerationEngine() {
        return codeGenerationEngine;
    }

    public static String getCoreId() {
        return CORE_ID;
    }

    public enum EvolutionState {

        IDLE,

        ANALYZING,

        READY_TO_EXECUTE,

        WAITING_FOR_PERMISSION,

        BUILD_REQUIRED,

        BUILDING,

        CODE_GENERATING,

        APPLYING_SOURCE_CHANGES,

        ACTIVATING,

        ACTIVATED,

        READY,

        EXECUTING,

        COMPLETED,

        FAILED
    }

    public static final class EvolutionAnalysis {

        private final CapabilityRequirement requirement;
        private final CapabilityDiscovery.DiscoveryResult discovery;
        private final CapabilityPlan plan;

        private EvolutionAnalysis(
                CapabilityRequirement requirement,
                CapabilityDiscovery.DiscoveryResult discovery,
                CapabilityPlan plan
        ) {

            this.requirement =
                    requirement;

            this.discovery =
                    discovery;

            this.plan =
                    plan;
        }

        public CapabilityRequirement
        getRequirement() {
            return requirement;
        }

        public CapabilityDiscovery.DiscoveryResult
        getDiscovery() {
            return discovery;
        }

        public CapabilityPlan getPlan() {
            return plan;
        }

        public boolean requiresBuild() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.BUILD_CAPABILITY;
        }

        public boolean canExecuteDirectly() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.EXECUTE_DIRECT;
        }

        public boolean usesAlternative() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.EXECUTE_ALTERNATIVE;
        }

        public boolean needsPermission() {

            return plan != null
                    &&
                    plan.getAction()
                            == CapabilityPlan.Action.REQUEST_PERMISSION;
        }

        public boolean isUnavailable() {

            return plan == null
                    ||
                    plan.getAction()
                            == CapabilityPlan.Action.UNAVAILABLE;
        }

        @Override
        public String toString() {

            return "EvolutionAnalysis{" +
                    "capabilityId='" +
                    requirement.getCapabilityId() +
                    '\'' +
                    ", action=" +
                    (
                            plan == null
                                    ? "null"
                                    : plan.getAction()
                    ) +
                    '}';
        }
    }

    public static final class EvolutionExecutionResult {

        private final ExecutionOutcome outcome;
        private final CapabilityPlan plan;
        private final ToolContract.ToolOutput toolOutput;
        private final EvolutionOrchestrator.EvolutionRecord evolutionRecord;
        private final CapabilityActivation.ActivationRecord activationRecord;
        private final List<CapabilityPermission> missingPermissions;
        private final String message;

        private EvolutionExecutionResult(
                ExecutionOutcome outcome,
                CapabilityPlan plan,
                ToolContract.ToolOutput toolOutput,
                EvolutionOrchestrator.EvolutionRecord evolutionRecord,
                CapabilityActivation.ActivationRecord activationRecord,
                List<CapabilityPermission> missingPermissions,
                String message
        ) {

            this.outcome =
                    outcome;

            this.plan =
                    plan;

            this.toolOutput =
                    toolOutput;

            this.evolutionRecord =
                    evolutionRecord;

            this.activationRecord =
                    activationRecord;

            List<CapabilityPermission> copy =
                    missingPermissions == null
                            ? Collections.emptyList()
                            : new ArrayList<>(
                                    missingPermissions
                            );

            this.missingPermissions =
                    Collections.unmodifiableList(
                            copy
                    );

            this.message =
                    message == null
                            ? ""
                            : message;
        }

        private static EvolutionExecutionResult executed(
                CapabilityPlan plan,
                ToolContract.ToolOutput output,
                String message
        ) {

            return new EvolutionExecutionResult(
                    ExecutionOutcome.EXECUTED,
                    plan,
                    output,
                    null,
                    null,
                    Collections.emptyList(),
                    message
            );
        }

        private static EvolutionExecutionResult
        permissionRequired(
                CapabilityPlan plan,
                Set<CapabilityPermission>
                        missingPermissions
        ) {

            List<CapabilityPermission> list =
                    missingPermissions == null
                            ? Collections.emptyList()
                            : new ArrayList<>(
                                    missingPermissions
                            );

            return new EvolutionExecutionResult(
                    ExecutionOutcome.PERMISSION_REQUIRED,
                    plan,
                    null,
                    null,
                    null,
                    list,
                    "Permission or capability is required."
            );
        }

        private static EvolutionExecutionResult built(
                CapabilityPlan plan,
                EvolutionOrchestrator.EvolutionRecord record,
                CapabilityActivation.ActivationRecord activationRecord,
                String message
        ) {

            return new EvolutionExecutionResult(
                    activationRecord != null
                            && activationRecord.isActive()
                            ? ExecutionOutcome.ACTIVATED
                            : ExecutionOutcome.BUILT,
                    plan,
                    null,
                    record,
                    activationRecord,
                    Collections.emptyList(),
                    message
            );
        }

        public ExecutionOutcome getOutcome() {
            return outcome;
        }

        public CapabilityPlan getPlan() {
            return plan;
        }

        public ToolContract.ToolOutput
        getToolOutput() {
            return toolOutput;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getEvolutionRecord() {
            return evolutionRecord;
        }

        public CapabilityActivation.ActivationRecord
        getActivationRecord() {
            return activationRecord;
        }

        public List<CapabilityPermission>
        getMissingPermissions() {
            return missingPermissions;
        }

        public String getMessage() {
            return message;
        }

        public boolean wasExecuted() {

            return outcome ==
                    ExecutionOutcome.EXECUTED;
        }

        public boolean wasBuilt() {

            return outcome ==
                    ExecutionOutcome.BUILT
                    ||
                    outcome ==
                    ExecutionOutcome.ACTIVATED;
        }

        public boolean wasActivated() {

            return outcome ==
                    ExecutionOutcome.ACTIVATED;
        }

        public boolean needsPermission() {

            return outcome ==
                    ExecutionOutcome.PERMISSION_REQUIRED;
        }

        @Override
        public String toString() {

            return "EvolutionExecutionResult{" +
                    "outcome=" +
                    outcome +
                    ", message='" +
                    message +
                    '\'' +
                    '}';
        }
    }

    public enum ExecutionOutcome {

        EXECUTED,

        BUILT,

        ACTIVATED,

        PERMISSION_REQUIRED
    }

    public static final class EvolutionRecord {

        private final String capabilityId;

        private final EvolutionOrchestrator.EvolutionRecord
                orchestratorRecord;

        private final CapabilityActivation.ActivationRecord
                activationRecord;

        private EvolutionRecord(
                String capabilityId,
                EvolutionOrchestrator.EvolutionRecord
                        orchestratorRecord,
                CapabilityActivation.ActivationRecord
                        activationRecord
        ) {

            this.capabilityId =
                    capabilityId;

            this.orchestratorRecord =
                    orchestratorRecord;

            this.activationRecord =
                    activationRecord;
        }

        public String getCapabilityId() {
            return capabilityId;
        }

        public EvolutionOrchestrator.EvolutionRecord
        getOrchestratorRecord() {
            return orchestratorRecord;
        }

        public CapabilityActivation.ActivationRecord
        getActivationRecord() {
            return activationRecord;
        }

        public boolean isActivated() {

            return activationRecord != null
                    &&
                    activationRecord.isActive();
        }

        public boolean isReady() {

            return orchestratorRecord != null
                    &&
                    orchestratorRecord.isReady();
        }

        public boolean wasRecovered() {

            return orchestratorRecord != null
                    &&
                    orchestratorRecord.wasRecovered();
        }

        public boolean failed() {

            return orchestratorRecord == null
                    ||
                    orchestratorRecord.failed();
        }

        @Override
        public String toString() {

            return "EvolutionRecord{" +
                    "capabilityId='" +
                    capabilityId +
                    '\'' +
                    ", ready=" +
                    isReady() +
                    ", activated=" +
                    isActivated() +
                    '}';
        }
    }
}