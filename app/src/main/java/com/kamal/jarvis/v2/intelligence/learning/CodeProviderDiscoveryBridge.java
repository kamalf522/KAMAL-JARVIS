package com.kamal.jarvis.v2.intelligence.learning;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.evolution.CodeGenerationEngine;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * JARVIS V2
 *
 * CodeProviderDiscoveryBridge
 *
 * حلقة الوصل بين:
 *
 * Internet / Source Trust
 *          ↓
 * Provider Discovery
 *          ↓
 * CodeGenerationEngine
 *
 * المسؤوليات:
 *
 * 1. البحث عن مصادر محتملة لتوليد الكود.
 * 2. استعمال Trust Engine قبل قبول المصدر.
 * 3. التحقق أن المصدر يمكن أن يمثل Code Generator فعلياً.
 * 4. عدم اعتبار صفحات الإنترنت العادية Code Generators.
 * 5. إعادة مزودي التوليد الحقيقيين فقط إلى CodeGenerationEngine.
 *
 * ملاحظة مهمة:
 *
 * هذا الملف لا يخترع API لمصدر لا يملكه.
 * المصدر يجب أن يقدم عقداً واضحاً أو endpoint معروفاً.
 */
public final class CodeProviderDiscoveryBridge
        implements CodeGenerationEngine.ProviderDiscovery {

    private static final String ENGINE_ID =
            "v2.code_provider_discovery_bridge";

    private static final int CONNECT_TIMEOUT_MS = 7000;

    private static final int READ_TIMEOUT_MS = 9000;

    private static final int MAX_DISCOVERY_SOURCES = 12;

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

    /**
     * معرف محرك الاكتشاف.
     */
    @Override
    public String getId() {
        return ENGINE_ID;
    }

    /**
     * يحدد هل المحرك متاح.
     */
    @Override
    public boolean isAvailable() {
        return enabled && trustEngine != null;
    }

    /**
     * تفعيل المحرك.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * تعطيل المحرك.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * اكتشاف مزودي توليد الكود.
     *
     * العملية:
     *
     * 1. بناء استعلام مناسب.
     * 2. اكتشاف المصادر.
     * 3. تقييم الثقة.
     * 4. فحص قابلية المصدر للاستعمال كـ API.
     * 5. إنشاء Provider فقط إذا كان هناك دليل كاف.
     */
    @Override
    public synchronized List<CodeGenerationEngine.CodeGeneratorProvider>
    discover(
            CodeGenerationEngine.GenerationRequest request
    ) {

        if (!isAvailable() ||
                request == null ||
                !request.isValid()) {

            return Collections.emptyList();
        }

        List<CodeGenerationEngine.CodeGeneratorProvider>
                providers =
                new ArrayList<>();

        try {

            String query =
                    buildDiscoveryQuery(request);

            KnowledgeSourceTrustEngine.DiscoveryResult
                    discovery =
                    trustEngine.discover(query);

            if (discovery == null ||
                    !discovery.isSuccess()) {

                return providers;
            }

            int checked = 0;

            for (
                    KnowledgeSourceTrustEngine.SourceProfile
                    profile
                    : discovery.getSources()
            ) {

                if (profile == null) {
                    continue;
                }

                if (checked >=
                        MAX_DISCOVERY_SOURCES) {
                    break;
                }

                checked++;

                if (!isTrustworthy(profile)) {
                    continue;
                }

                ProviderEndpoint endpoint =
                        inspectProviderEndpoint(
                                profile
                        );

                if (endpoint == null ||
                        !endpoint.isUsable()) {
                    continue;
                }

                CodeGenerationEngine.CodeGeneratorProvider
                        provider =
                        new HttpCodeGeneratorProvider(
                                endpoint
                        );

                if (!provider.isConfigured()) {
                    continue;
                }

                if (!provider.isAvailable()) {
                    continue;
                }

                providers.add(provider);
            }

        } catch (Exception ignored) {
            /*
             * فشل اكتشاف مصدر واحد لا يجب أن يوقف
             * بقية نظام JARVIS.
             */
        }

        return providers;
    }

    /**
     * البحث عن مصدر توليد كود.
     */
    private String buildDiscoveryQuery(
            CodeGenerationEngine.GenerationRequest request
    ) {

        String goal =
                request.getGoal();

        String description =
                request.getDescription();

        return
                "free open source code generation API "
                        + "programmatic code generation endpoint "
                        + "JSON API "
                        + safe(goal)
                        + " "
                        + safe(description);
    }

    /**
     * تقييم المصدر قبل أي استعمال.
     */
    private boolean isTrustworthy(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        if (profile == null ||
                !profile.isAvailable()) {

            return false;
        }

        KnowledgeSourceTrustEngine.TrustAssessment
                assessment =
                trustEngine.getAssessment(
                        profile.getId()
                );

        if (assessment == null ||
                !assessment.canUse()) {

            return false;
        }

        /*
         * بالنسبة لمصدر Code Generation،
         * نحتاج مستوى ثقة أعلى من مجرد مصدر معلومات.
         */
        double trustScore =
                calculateTrustScore(
                        profile
                );

        return trustScore >= 0.70;
    }

    /**
     * حساب ثقة مركبة للمصدر.
     */
    private double calculateTrustScore(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        double ownership =
                clamp(
                        profile.getOwnershipScore()
                );

        double reputation =
                clamp(
                        profile.getReputationScore()
                );

        double references =
                clamp(
                        profile.getReferenceQuality()
                );

        double corroboration =
                clamp(
                        profile.getIndependentCorroboration()
                );

        double freshness =
                clamp(
                        profile.getFreshnessScore()
                );

        double transparency =
                clamp(
                        profile.getTransparencyScore()
                );

        double relevance =
                clamp(
                        profile.getDomainRelevance()
                );

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
     * فحص endpoint للمصدر.
     *
     * لا نرسل بيانات توليد هنا.
     * الغرض فقط التأكد من وجود endpoint يمكن
     * أن يكون API فعلياً.
     */
    private ProviderEndpoint inspectProviderEndpoint(
            KnowledgeSourceTrustEngine.SourceProfile profile
    ) {

        String sourceUrl =
                safe(profile.getUrl());

        if (sourceUrl.isEmpty()) {
            return null;
        }

        if (!looksLikeApiSource(
                profile,
                sourceUrl
        )) {
            return null;
        }

        try {

            URI uri =
                    URI.create(
                            sourceUrl
                    );

            URL url =
                    uri.toURL();

            HttpURLConnection connection =
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
                    "Kamal-JARVIS/2.0"
            );

            int responseCode =
                    connection.getResponseCode();

            String contentType =
                    safe(
                            connection.getContentType()
                    );

            connection.disconnect();

            if (responseCode < 200 ||
                    responseCode >= 400) {

                return null;
            }

            /*
             * API المصدر يجب أن يرجع JSON أو يعلن
             * صراحة أنه endpoint برمجي.
             */
            if (!looksLikeJson(
                    contentType
            )) {

                return null;
            }

            return new ProviderEndpoint(
                    profile.getId(),
                    profile.getName(),
                    sourceUrl,
                    profile.getOwner(),
                    profile.isVerifiedOwnership(),
                    profile.isOfficial(),
                    responseCode
            );

        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * تحديد ما إذا كان المصدر يبدو API.
     */
    private boolean looksLikeApiSource(
            KnowledgeSourceTrustEngine.SourceProfile profile,
            String url
    ) {

        String lowerUrl =
                url.toLowerCase(
                        Locale.ROOT
                );

        String name =
                safe(profile.getName())
                        .toLowerCase(
                                Locale.ROOT
                        );

        String description =
                safe(profile.getDescription())
                        .toLowerCase(
                                Locale.ROOT
                        );

        String combined =
                lowerUrl
                        + " "
                        + name
                        + " "
                        + description;

        String[] apiSignals = {
                "/api/",
                "api.",
                "/v1/",
                "/v2/",
                "/generate",
                "/completion",
                "/chat/completions",
                "inference",
                "openai-compatible",
                "llm api",
                "code generation api",
                "json api"
        };

        for (String signal :
                apiSignals) {

            if (combined.contains(signal)) {
                return true;
            }
        }

        return false;
    }

    private boolean looksLikeJson(
            String contentType
    ) {

        String value =
                safe(contentType)
                        .toLowerCase(
                                Locale.ROOT
                        );

        return value.contains(
                "application/json"
        ) ||
                value.contains(
                        "+json"
                );
    }

    private double clamp(
            double value
    ) {

        if (Double.isNaN(value)) {
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

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    public KnowledgeSourceTrustEngine
    getTrustEngine() {

        return trustEngine;
    }

    public String getEngineId() {
        return ENGINE_ID;
    }

    /**
     * معلومات endpoint المكتشف.
     */
    private static final class ProviderEndpoint {

        private final String sourceId;
        private final String name;
        private final String url;
        private final String owner;
        private final boolean verifiedOwnership;
        private final boolean official;
        private final int responseCode;

        private ProviderEndpoint(
                String sourceId,
                String name,
                String url,
                String owner,
                boolean verifiedOwnership,
                boolean official,
                int responseCode
        ) {

            this.sourceId =
                    sourceId;

            this.name =
                    name;

            this.url =
                    url;

            this.owner =
                    owner;

            this.verifiedOwnership =
                    verifiedOwnership;

            this.official =
                    official;

            this.responseCode =
                    responseCode;
        }

        private boolean isUsable() {

            return !safe(sourceId).isEmpty()
                    && !safe(url).isEmpty()
                    && responseCode >= 200
                    && responseCode < 400;
        }
    }

    /**
     * مزود HTTP عام.
     *
     * ملاحظة:
     *
     * هذه الطبقة لا تفترض schema معيناً للمصدر.
     * لذلك لن ترسل طلب توليد إلى API مجهول.
     *
     * قبل التوليد الفعلي يجب أن يثبت المصدر
     * contract متوافقاً مع CodeGenerationEngine.
     */
    private static final class HttpCodeGeneratorProvider
            implements CodeGenerationEngine.CodeGeneratorProvider {

        private final ProviderEndpoint endpoint;

        private HttpCodeGeneratorProvider(
                ProviderEndpoint endpoint
        ) {
            this.endpoint =
                    endpoint;
        }

        @Override
        public String getId() {

            return "network."
                    + safeStatic(
                            endpoint.sourceId
                    );
        }

        @Override
        public String getName() {

            return safeStatic(
                    endpoint.name
            );
        }

        @Override
        public int getPriority() {

            int priority = 50;

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

            return isConfigured();
        }

        @Override
        public JarvisResult<
                CodeGenerationEngine.GeneratedCode
                > generate(
                CodeGenerationEngine.GenerationRequest request
        ) {

            /*
             * لا ندعي نجاح التوليد قبل اكتشاف
             * contract حقيقي للـAPI.
             *
             * هذا يمنع JARVIS من إرسال طلبات
             * عشوائية إلى الإنترنت.
             */
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.TOOL_UNAVAILABLE,
                            "Provider discovered but its code-generation "
                                    + "request contract has not been verified.",
                            "v2.code_provider_discovery_bridge"
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
    }
}