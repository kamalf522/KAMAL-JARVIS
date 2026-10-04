package com.kamal.jarvis.v2.intelligence.learning;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.evolution.CodeGenerationEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * JARVIS V2
 *
 * Code Provider Discovery Bridge
 *
 * Internet
 *   ↓
 * Source Discovery
 *   ↓
 * Trust Evaluation
 *   ↓
 * Protocol Detection
 *   ↓
 * OpenAI-Compatible Provider
 *   ↓
 * CodeGenerationEngine
 *
 * هذه النسخة لا تعتبر أي موقع عشوائي مولداً للكود.
 *
 * المصدر لا يصبح Provider إلا إذا:
 *
 * 1. تم اكتشافه بواسطة Trust Engine.
 * 2. اجتاز مستوى الثقة المطلوب.
 * 3. تم العثور على OpenAI-compatible endpoint.
 * 4. endpoint /v1/models يجيب فعلياً.
 * 5. تم العثور على model صالح.
 *
 * وبعد ذلك فقط يستطيع JARVIS إرسال طلب توليد حقيقي.
 */
public final class CodeProviderDiscoveryBridge
        implements CodeGenerationEngine.ProviderDiscovery {

    private static final String ENGINE_ID =
            "v2.code_provider_discovery_bridge";

    private static final String USER_AGENT =
            "Kamal-JARVIS/2.0";

    private static final int CONNECT_TIMEOUT_MS =
            8000;

    private static final int READ_TIMEOUT_MS =
            20000;

    private static final int MAX_DISCOVERY_SOURCES =
            12;

    private static final int MAX_RESPONSE_BYTES =
            2_000_000;

    private static final double MIN_TRUST_SCORE =
            0.70;

    private final KnowledgeSourceTrustEngine trustEngine;

    private volatile boolean enabled = true;

    public CodeProviderDiscoveryBridge(
            KnowledgeSourceTrustEngine trustEngine
    ) {
        this.trustEngine =
                trustEngine == null
                        ? new KnowledgeSourceTrustEngine()
                        : trustEngine;
    }

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    public boolean isAvailable() {
        return enabled && trustEngine != null;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    /**
     * اكتشاف Providers حقيقيين.
     */
    @Override
    public synchronized List<
            CodeGenerationEngine.CodeGeneratorProvider
            > discover(
            CodeGenerationEngine.GenerationRequest request
    ) {

        if (!isAvailable()
                || request == null
                || !request.isValid()) {

            return Collections.emptyList();
        }

        List<
                CodeGenerationEngine.CodeGeneratorProvider
                > providers =
                new ArrayList<>();

        Set<String> providerIds =
                new LinkedHashSet<>();

        try {

            String query =
                    buildDiscoveryQuery(request);

            KnowledgeSourceTrustEngine.DiscoveryResult
                    discovery =
                    trustEngine.discover(query);

            if (discovery == null
                    || !discovery.isSuccess()) {

                return providers;
            }

            int checked = 0;

            for (
                    KnowledgeSourceTrustEngine.SourceProfile profile
                    : discovery.getSources()
            ) {

                if (profile == null) {
                    continue;
                }

                if (checked >= MAX_DISCOVERY_SOURCES) {
                    break;
                }

                checked++;

                if (!isTrustworthy(profile)) {
                    continue;
                }

                List<ApiEndpoint> endpoints =
                        discoverCompatibleEndpoints(
                                profile
                        );

                for (ApiEndpoint endpoint : endpoints) {

                    if (endpoint == null
                            || !endpoint.isUsable()) {
                        continue;
                    }
                    String providerId =
                            endpoint.providerId();

                    if (!providerIds.add(providerId)) {
                        continue;
                    }

                    OpenAiCompatibleProvider provider =
                            new OpenAiCompatibleProvider(
                                    endpoint
                            );

                    if (!provider.isConfigured()
                            || !provider.isAvailable()) {
                        continue;
                    }

                    providers.add(provider);
                }
            }

        } catch (Exception ignored) {
            /*
             * فشل مصدر واحد لا يوقف بقية الاكتشاف.
             */
        }

        return providers;
    }

    /**
     * يبني استعلاماً عاماً.
     *
     * JARVIS لا يبحث عن Provider واحد محدد.
     */
    private String buildDiscoveryQuery(
            CodeGenerationEngine.GenerationRequest request
    ) {

        return
                "free open source AI code generation API "
                        + "OpenAI compatible API "
                        + "LLM inference API "
                        + "v1 models chat completions "
                        + safe(request.getGoal())
                        + " "
                        + safe(request.getDescription());
    }

    /**
     * تقييم Trust قبل استعمال أي مصدر.
     */
    private boolean isTrustworthy(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        if (profile == null
                || !profile.isAvailable()) {

            return false;
        }

        KnowledgeSourceTrustEngine.TrustAssessment assessment =
                trustEngine.getAssessment(
                        profile.getId()
                );

        if (assessment == null
                || !assessment.canUse()) {

            return false;
        }

        double score =
                calculateTrustScore(profile);

        return score >= MIN_TRUST_SCORE;
    }

    private double calculateTrustScore(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        double ownership =
                clamp(profile.getOwnershipScore());

        double reputation =
                clamp(profile.getReputationScore());

        double references =
                clamp(profile.getReferenceQuality());

        double corroboration =
                clamp(
                        profile.getIndependentCorroboration()
                );

        double freshness =
                clamp(profile.getFreshnessScore());

        double transparency =
                clamp(profile.getTransparencyScore());

        double relevance =
                clamp(profile.getDomainRelevance());

        return
                ownership * 0.20
                        + reputation * 0.20
                        + references * 0.15
                        + corroboration * 0.15
                        + freshness * 0.10
                        + transparency * 0.10
                        + relevance * 0.10;
    }

    /**
     * يحاول اكتشاف OpenAI-compatible API فعلياً.
     *
     * لا نعتمد فقط على اسم الرابط.
     *
     * الاختبار الحقيقي:
     *
     * GET /v1/models
     */
    private List<ApiEndpoint> discoverCompatibleEndpoints(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        List<ApiEndpoint> result =
                new ArrayList<>();

        String sourceUrl =
                safe(profile.getUrl());

        if (sourceUrl.isEmpty()) {
            return result;
        }

        List<String> candidates =
                buildEndpointCandidates(
                        sourceUrl
                );

        for (String baseUrl : candidates) {

            ApiEndpoint endpoint =
                    inspectOpenAiCompatibleEndpoint(
                            profile,
                            baseUrl
                    );

            if (endpoint != null
                    && endpoint.isUsable()) {

                result.add(endpoint);

                /*
                 * يكفي Provider واحد صالح
                 * من نفس المصدر.
                 */
                break;
            }
        }

        return result;
    }

    /**
     * إنشاء عناوين محتملة للـAPI.
     */
    private List<String> buildEndpointCandidates(
            String sourceUrl
    ) {

        Set<String> candidates =
                new LinkedHashSet<>();

        try {
            URI uri =
                    URI.create(sourceUrl);

            String scheme =
                    uri.getScheme();

            String authority =
                    uri.getRawAuthority();

            if (scheme != null
                    && authority != null) {

                String root =
                        scheme
                                + "://"
                                + authority;

                String path =
                        uri.getPath();

                if (path != null
                        && !path.trim().isEmpty()) {

                    String cleanPath =
                            path.trim()
                                    .replaceAll(
                                            "/+$",
                                            ""
                                    );

                    if (cleanPath.endsWith(
                            "/v1"
                    )) {

                        candidates.add(
                                root
                                        + cleanPath
                        );

                    } else {

                        candidates.add(
                                root
                                        + cleanPath
                                        + "/v1"
                        );
                    }
                }

                candidates.add(
                        root + "/v1"
                );

                candidates.add(root);
            }

        } catch (Exception ignored) {
            /*
             * مصدر غير صالح.
             */
        }

        return new ArrayList<>(candidates);
    }

    /**
     * الاختبار الفعلي للـAPI.
     */
    private ApiEndpoint inspectOpenAiCompatibleEndpoint(
            KnowledgeSourceTrustEngine.SourceProfile profile,
            String baseUrl
    ) {

        String modelsUrl =
                joinUrl(
                        baseUrl,
                        "/models"
                );

        HttpResponse response =
                httpGet(
                        modelsUrl
                );

        if (response == null
                || !response.success) {

            return null;
        }

        String body =
                response.body;

        if (!looksLikeJson(body)) {
            return null;
        }

        try {

            JSONObject json =
                    new JSONObject(body);

            JSONArray data =
                    json.optJSONArray(
                            "data"
                    );

            if (data == null
                    || data.length() == 0) {

                return null;
            }

            String model =
                    findUsableModel(data);

            if (model.isEmpty()) {
                return null;
            }

            String chatEndpoint =
                    joinUrl(
                            baseUrl,
                            "/chat/completions"
                    );

            /*
             * لا نرسل Prompt هنا.
             *
             * يكفي اكتشاف:
             * - /models
             * - model
             * - endpoint
             *
             * التوليد الحقيقي يتم لاحقاً.
             */
            return new ApiEndpoint(
                    profile.getId(),
                    profile.getName(),
                    baseUrl,
                    chatEndpoint,
                    model,
                    profile.getOwner(),
                    profile.isOfficial(),
                    profile.isVerifiedOwnership()
            );

        } catch (Exception ignored) {
            return null;
        }
    }

    private String findUsableModel(
            JSONArray data
    ) {

        String fallback = "";

        for (int i = 0;
             i < data.length();
             i++) {

            try {

                JSONObject item =
                        data.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String id =
                        item.optString(
                                "id",
                                ""
                        ).trim();

                if (id.isEmpty()) {
                    continue;
                }

                String lower =
                        id.toLowerCase(
                                Locale.ROOT
                        );

                /*
                 * الأفضلية للموديلات التي يمكن
                 * استعمالها عادةً في النص/الكود.
                 */
                if (lower.contains("code")
                        || lower.contains("coder")
                        || lower.contains("instruct")
                        || lower.contains("chat")
                        || lower.contains("qwen")
                        || lower.contains("llama")
                        || lower.contains("deepseek")) {

                    return id;
                }

                if (fallback.isEmpty()) {
                    fallback = id;
                }

            } catch (Exception ignored) {
                // تجاهل model غير صالح.
            }
        }

        return fallback;
    }

    /**
     * Provider حقيقي لـ OpenAI-compatible API.
     */
    private static final class OpenAiCompatibleProvider
            implements CodeGenerationEngine.CodeGeneratorProvider {

        private final ApiEndpoint endpoint;

        private OpenAiCompatibleProvider(
                ApiEndpoint endpoint
        ) {
            this.endpoint = endpoint;
        }

        @Override
        public String getId() {

            return endpoint.providerId();
        }

        @Override
        public String getName() {

            return endpoint.name;
        }

        @Override
        public int getPriority() {

            int priority = 60;

            if (endpoint.official) {
                priority += 15;
            }

            if (endpoint.verifiedOwnership) {
                priority += 15;
            }

            return priority;
        }

        @Override
        public boolean isConfigured() {

            return endpoint != null
                    && endpoint.isUsable();
        }

        @Override
        public boolean isAvailable() {

            if (!isConfigured()) {
                return false;
            }

            /*
             * إعادة فحص models قبل الاستعمال.
             */
            HttpResponse response =
                    httpGetStatic(
                            endpoint.modelsUrl()
                    );

            if (response == null
                    || !response.success
                    || !looksLikeJsonStatic(
                    response.body
            )) {

                return false;
            }

            try {

                JSONObject json =
                        new JSONObject(
                                response.body
                        );

                JSONArray data =
                        json.optJSONArray(
                                "data"
                        );

                return data != null
                        && data.length() > 0;

            } catch (Exception e) {

                return false;
            }
        }

        /**
         * التوليد الحقيقي.
         *
         * نرسل طلباً متوافقاً مع:
         *
         * POST /v1/chat/completions
         *
         * والـmodel المكتشف تلقائياً.
         */
        @Override
        public JarvisResult<
                CodeGenerationEngine.GeneratedCode
                > generate(
                CodeGenerationEngine.GenerationRequest request
        ) {

            if (request == null
                    || !request.isValid()) {

                return failure(
                        JarvisError.Type.VALIDATION_FAILED,
                        "Generation request is invalid."
                );
            }

            if (!isConfigured()) {

                return failure(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        "Discovered provider is unavailable."
                );
            }

            String prompt =
                    buildPrompt(request);

            JSONObject payload =
                    new JSONObject();

            try {

                payload.put(
                        "model",
                        endpoint.model
                );

                payload.put(
                        "temperature",
                        0.1
                );

                payload.put(
                        "stream",
                        false
                );

                JSONArray messages =
                        new JSONArray();

                JSONObject system =
                        new JSONObject();

                system.put(
                        "role",
                        "system"
                );

                system.put(
                        "content",
                        buildSystemPrompt()
                );

                messages.put(system);

                JSONObject user =
                        new JSONObject();

                user.put(
                        "role",
                        "user"
                );

                user.put(
                        "content",
                        prompt
                );

                messages.put(user);

                payload.put(
                        "messages",
                        messages
                );

            } catch (Exception e) {

                return failure(
                        JarvisError.Type.INTERNAL_ERROR,
                        "Could not create generation request."
                );
            }

            HttpResponse response =
                    postJson(
                            endpoint.chatUrl,
                            payload.toString()
                    );

            if (response == null
                    || !response.success) {

                String message =
                        response == null
                                ? "Provider returned no response."
                                : response.message;

                return failure(
                        JarvisError.Type.TOOL_UNAVAILABLE,
                        message
                );
            }

            try {

                String generatedText =
                        extractAssistantContent(
                                response.body
                        );

                if (generatedText.isEmpty()) {

                    return failure(
                            JarvisError.Type.EVOLUTION_FAILED,
                            "Provider returned empty generated content."
                    );
                }

                List<
                        CodeGenerationEngine.GeneratedFile
                        > files =
                        parseGeneratedFiles(
                                generatedText,
                                request
                        );

                if (files.isEmpty()) {

                    return failure(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Provider response did not contain valid "
                                    + "file definitions."
                    );
                }

                String generationId =
                        "network_"
                                + Long.toHexString(
                                System.currentTimeMillis()
                        );

                CodeGenerationEngine.GeneratedCode
                        generated =
                        new CodeGenerationEngine.GeneratedCode(
                                generationId,
                                getId(),
                                "Generated through verified "
                                        + "OpenAI-compatible provider: "
                                        + endpoint.name,
                                files
                        );

                return JarvisResult.success(
                        generated,
                        "Code generated by discovered provider."
                );

            } catch (Exception e) {

                return failure(
                        JarvisError.Type.EVOLUTION_FAILED,
                        "Could not parse provider response: "
                                + safeStatic(
                                e.getMessage()
                        )
                );
            }
        }

        /**
         * Prompt صارم لتقليل الكود العشوائي.
         */
        private String buildPrompt(
                CodeGenerationEngine.GenerationRequest request
        ) {

            StringBuilder builder =
                    new StringBuilder();

            builder.append(
                    "CAPABILITY ID:\n"
            );

            builder.append(
                    request.getCapabilityId()
            );

            builder.append(
                    "\n\nGOAL:\n"
            );

            builder.append(
                    request.getGoal()
            );

            builder.append(
                    "\n\nDESCRIPTION:\n"
            );

            builder.append(
                    request.getDescription()
            );

            builder.append(
                    "\n\nPROJECT CONTEXT:\n"
            );

            builder.append(
                    request.getProjectContext()
            );

            builder.append(
                    "\n\nREQUIRED FILES:\n"
            );
            );

            appendSet(
                    builder,
                    request.getRequiredFiles()
            );

            builder.append(
                    "\n\nREQUIRED TOOLS:\n"
            );

            appendSet(
                    builder,
                    request.getRequiredTools()
            );

            builder.append(
                    "\n\nSUCCESS CRITERIA:\n"
            );

            appendSet(
                    builder,
                    request.getSuccessCriteria()
            );

            builder.append(
                    "\n\nALLOWED PATHS:\n"
            );

            appendSet(
                    builder,
                    request.getAllowedPaths()
            );

            builder.append(
                    "\n\n"
            );

            builder.append(
                    "Generate the required implementation.\n"
            );

            builder.append(
                    "Do not modify protected security files.\n"
            );

            builder.append(
                    "Do not invent external APIs.\n"
            );

            builder.append(
                    "Return only file blocks.\n"
            );

            builder.append(
                    "Use exactly this format:\n\n"
            );

            builder.append(
                    "[FILE path/to/File.java]\n"
            );

            builder.append(
                    "complete file content\n"
            );

            builder.append(
                    "[/FILE]\n"
            );

            return builder.toString();
        }

        private String buildSystemPrompt() {

            return
                    "You are the code generation component of "
                            + "JARVIS V2. "
                            + "Generate complete production-oriented "
                            + "source files. "
                            + "Never claim that code works without "
                            + "providing the implementation. "
                            + "Respect the requested paths. "
                            + "Do not modify security boundaries. "
                            + "Return only [FILE path] blocks.";
        }

        private void appendSet(
                StringBuilder builder,
                Set<String> values
        ) {

            if (values == null
                    || values.isEmpty()) {

                builder.append(
                        "(none)\n"
                );

                return;
            }

            for (String value : values) {

                if (value == null) {
                    continue;
                }

                builder.append(
                        "- "
                );

                builder.append(
                        value
                );

                builder.append(
                        "\n"
                );
            }
        }

        /**
         * استخراج محتوى assistant من OpenAI-compatible response.
         */
        private String extractAssistantContent(
                String body
        ) {

            JSONObject json =
                    new JSONObject(body);

            JSONArray choices =
                    json.optJSONArray(
                            "choices"
                    );

            if (choices == null
                    || choices.length() == 0) {

                return "";
            }

            JSONObject choice =
                    choices.optJSONObject(0);

            if (choice == null) {
                return "";
            }

            JSONObject message =
                    choice.optJSONObject(
                            "message"
                    );

            if (message != null) {

                return message.optString(
                        "content",
                        ""
                ).trim();
            }

            return choice.optString(
                    "text",
                    ""
            ).trim();
        }

        /**
         * تحويل response إلى ملفات.
         */
        private List<
                CodeGenerationEngine.GeneratedFile
                > parseGeneratedFiles(
                String text,
                CodeGenerationEngine.GenerationRequest request
        ) {

            List<
                    CodeGenerationEngine.GeneratedFile
                    > files =
                    new ArrayList<>();

            String markerStart =
                    "[FILE ";

            String markerEnd =
                    "[/FILE]";

            int cursor = 0;

            while (cursor < text.length()) {

                int start =
                        text.indexOf(
                                markerStart,
                                cursor
                        );

                if (start < 0) {
                    break;
                }

                int headerEnd =
                        text.indexOf(
                                "]",
                                start
                        );

                if (headerEnd < 0) {
                    break;
                }

                String path =
                        text.substring(
                                start + markerStart.length(),
                                headerEnd
                        ).trim();

                int contentStart =
                        headerEnd + 1;

                int end =
                        text.indexOf(
                                markerEnd,
                                contentStart
                        );

                if (end < 0) {
                    break;
                }

                String content =
                        text.substring(
                                contentStart,
                                end
                        ).trim();

                if (!path.isEmpty()
                        && !content.isEmpty()) {

                    files.add(
                            new CodeGenerationEngine.GeneratedFile(
                                    path,
                                    chooseOperation(
                                            path,
                                            request
                                    ),
                                    content
                            )
                    );
                }

                cursor =
                        end + markerEnd.length();
            }

            /*
             * إذا الموديل رجع ملف واحد بدون markers،
             * لا نخمن المسار إلا إذا كان هناك ملف واحد
             * مطلوب بشكل صريح.
             */
            if (files.isEmpty()
                    && request.getRequiredFiles().size() == 1) {

                String path =
                        request.getRequiredFiles()
                                .iterator()
                                .next();

                String cleaned =
                        stripCodeFence(text);

                if (!cleaned.isEmpty()) {

                    files.add(
                            new CodeGenerationEngine.GeneratedFile(
                                    path,
                                    chooseOperation(
                                            path,
                                            request
                                    ),
                                    cleaned
                            )
                    );
                }
            }

            return files;
        }

        private
        com.kamal.jarvis.v2.evolution.SourceEvolutionEngine
                .FileChange.Operation
        chooseOperation(
                String path,
                CodeGenerationEngine.GenerationRequest request
        ) {

            if (request.getRequiredFiles()
                    .contains(path)) {

                /*
                 * الملفات المطلوبة يمكن أن تكون موجودة
                 * أو جديدة؛ WRITE يسمح لـ
                 * SourceEvolutionEngine بإدارة التغيير.
                 */
                return
                        com.kamal.jarvis.v2.evolution
                                .SourceEvolutionEngine
                                .FileChange
                                .Operation
                                .WRITE;
            }

            return
                    com.kamal.jarvis.v2.evolution
                            .SourceEvolutionEngine
                            .FileChange
                            .Operation
                            .CREATE;
        }

        private String stripCodeFence(
                String text
        ) {

            String value =
                    text == null
                            ? ""
                            : text.trim();

            if (value.startsWith("```")
                    && value.endsWith("```")) {

                int firstNewLine =
                        value.indexOf('\n');

                if (firstNewLine >= 0) {

                    value =
                            value.substring(
                                    firstNewLine + 1,
                                    value.length() - 3
                            ).trim();
                }
            }

            return value;
        }

        private JarvisResult<
                CodeGenerationEngine.GeneratedCode
                > failure(
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
    }

    /**
     * معلومات API المكتشف.
     */
    private static final class ApiEndpoint {

        private final String sourceId;
        private final String name;
        private final String baseUrl;
        private final String chatUrl;
        private final String model;
        private final String owner;
        private final boolean official;
        private final boolean verifiedOwnership;

        private ApiEndpoint(
                String sourceId,
                String name,
                String baseUrl,
                String chatUrl,
                String model,
                String owner,
                boolean official,
                boolean verifiedOwnership
        ) {

            this.sourceId = safeStatic(sourceId);
            this.name = safeStatic(name);
            this.baseUrl = safeStatic(baseUrl);
            this.chatUrl = safeStatic(chatUrl);
            this.model = safeStatic(model);
            this.owner = safeStatic(owner);
            this.official = official;
            this.verifiedOwnership =
                    verifiedOwnership;
        }

        private boolean isUsable() {

            return !sourceId.isEmpty()
                    && !baseUrl.isEmpty()
                    && !chatUrl.isEmpty()
                    && !model.isEmpty();
        }

        private String modelsUrl() {

            return joinUrlStatic(
                    baseUrl,
                    "/models"
            );
        }

        private String providerId() {

            return "network."
                    + sourceId
                    + "."
                    + Integer.toHexString(
                    baseUrl.hashCode()
            );
        }
    }

    private static HttpResponse httpGetStatic(
            String urlString
    ) {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    URI.create(
                            urlString
                    ).toURL();

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );

            connection.setReadTimeout(
                    READ_TIMEOUT_MS
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            int code =
                    connection.getResponseCode();

            String body =
                    readBody(
                            connection,
                            code
                    );

            boolean success =
                    code >= 200
                            && code < 300;

            return new HttpResponse(
                    success,
                    code,
                    body,
                    success
                            ? "HTTP request succeeded."
                            : "HTTP request failed: "
                            + code
            );

        } catch (Exception e) {

            return new HttpResponse(
                    false,
                    -1,
                    "",
                    "HTTP GET failed: "
                            + safeStatic(
                            e.getMessage()
                    )
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpResponse httpGet(
            String urlString
    ) {

        return httpGetStatic(
                urlString
        );
    }

    private HttpResponse postJson(
            String urlString,
            String json
    ) {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    URI.create(
                            urlString
                    ).toURL();

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "POST"
            );

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );

            connection.setReadTimeout(
                    READ_TIMEOUT_MS
            );

            connection.setDoOutput(
                    true
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            byte[] data =
                    json.getBytes(
                            StandardCharsets.UTF_8
                    );

            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(data);
                output.flush();
            }

            int code =
                    connection.getResponseCode();

            String body =
                    readBody(
                            connection,
                            code
                    );

            boolean success =
                    code >= 200
                            && code < 300;

            return new HttpResponse(
                    success,
                    code,
                    body,
                    success
                            ? "Provider request succeeded."
                            : "Provider returned HTTP "
                            + code
                            + "."
            );

        } catch (Exception e) {

            return new HttpResponse(
                    false,
                    -1,
                    "",
                    "Provider request failed: "
                            + safeStatic(
                            e.getMessage()
                    )
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(
            HttpURLConnection connection,
            int responseCode
    ) {

        InputStream stream =
                null;

        try {

            if (responseCode >= 200
                    && responseCode < 400) {

                stream =
                        connection.getInputStream();

            } else {

                stream =
                        connection.getErrorStream();
            }

            if (stream == null) {
                return "";
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder builder =
                    new StringBuilder();

            String line;

            int total = 0;

            while ((line =
                    reader.readLine()) != null) {

                total +=
                        line.length();

                if (total >
                        MAX_RESPONSE_BYTES) {

                    break;
                }

                builder.append(line);
                builder.append('\n');
            }

            reader.close();

            return builder.toString();

        } catch (Exception e) {

            return "";

        } finally {

            if (stream != null) {

                try {
                    stream.close();
                } catch (Exception ignored) {
                    // لا شيء.
                }
            }
        }
    }

    private static boolean looksLikeJson(
            String value
    ) {
        if (value == null) {
            return false;
        }

        String clean =
                value.trim();

        return clean.startsWith("{")
                || clean.startsWith("[");
    }

    private static boolean looksLikeJsonStatic(
            String value
    ) {

        return looksLikeJson(value);
    }

    private static String joinUrl(
            String base,
            String path
    ) {

        return joinUrlStatic(
                base,
                path
        );
    }

    private static String joinUrlStatic(
            String base,
            String path
    ) {

        String left =
                safeStatic(base)
                        .replaceAll(
                                "/+$",
                                ""
                        );

        String right =
                safeStatic(path)
                        .replaceAll(
                                "^/+",
                                ""
                        );

        return left
                + "/"
                + right;
    }

    private static double clamp(
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static String safeStatic(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private String safe(
            String value
    ) {

        return safeStatic(value);
    }

    private static final class HttpResponse {

        private final boolean success;
        private final int code;
        private final String body;
        private final String message;

        private HttpResponse(
                boolean success,
                int code,
                String body,
                String message
        ) {

            this.success = success;
            this.code = code;
            this.body =
                    body == null
                            ? ""
                            : body;
            this.message =
                    message == null
                            ? ""
                            : message;
        }
    }
}
