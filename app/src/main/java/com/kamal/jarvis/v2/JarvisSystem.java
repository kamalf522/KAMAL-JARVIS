package com.kamal.jarvis.v2;

import android.content.Context;
import android.content.SharedPreferences;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;
import com.kamal.jarvis.v2.core.JarvisRuntime;
import com.kamal.jarvis.v2.core.ToolContract;
import com.kamal.jarvis.v2.evolution.BuildEngine;
import com.kamal.jarvis.v2.evolution.CapabilityDiscovery;
import com.kamal.jarvis.v2.evolution.CapabilityExecutor;
import com.kamal.jarvis.v2.evolution.CodeEvolutionEngine;
import com.kamal.jarvis.v2.evolution.EvolutionCore;
import com.kamal.jarvis.v2.evolution.EvolutionOrchestrator;
import com.kamal.jarvis.v2.evolution.EvolutionVerificationEngine;
import com.kamal.jarvis.v2.evolution.ProjectWorkspaceManager;
import com.kamal.jarvis.v2.evolution.RecoveryEngine;
import com.kamal.jarvis.v2.evolution.SelfBuilder;
import com.kamal.jarvis.v2.evolution.SelfTestEngine;
import com.kamal.jarvis.v2.evolution.SourceEvolutionEngine;
import com.kamal.jarvis.v2.intelligence.JarvisBrain;
import com.kamal.jarvis.v2.permissions.AndroidPermissionBridge;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;
import com.kamal.jarvis.v2.tools.AndroidIntentTool;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.io.File;

/**
 * JARVIS V2 - System
 *
 * نقطة التجميع الرئيسية للنظام.
 *
 * المسؤول عن:
 *
 * - Security
 * - Runtime
 * - Tools
 * - Permissions
 * - Evolution
 * - Project Workspace
 * - Brain
 *
 * مهم:
 *
 * يوجد فرق بين:
 *
 * 1. jarvis_workspace
 *    مساحة JARVIS الداخلية للـartifacts والبيانات المؤقتة.
 *
 * 2. ProjectWorkspaceManager
 *    المشروع Android الحقيقي الذي يمكن أن يخضع لـ:
 *
 *    Source Evolution
 *    Build
 *    Verification
 *
 * JARVIS لا يعتبر workspace الداخلي مشروع Android
 * حقيقياً ولا يستعمله كبديل صامت.
 */
public final class JarvisSystem {

    private static final String PREFS_NAME =
            "jarvis_v2_system";

    private static final String OWNER_ID_KEY =
            "owner_id";

    private static final String DEFAULT_OWNER_ID =
            "KAMAL_OWNER";

    private final Context context;

    /*
     * Workspace داخلي لـJARVIS.
     */
    private final File workspaceRoot;

    /*
     * مدير المشروع الحقيقي.
     */
    private final ProjectWorkspaceManager projectWorkspaceManager;

    private final OwnerSecurityBoundary securityBoundary;

    private final JarvisRuntime runtime;

    private final ToolRegistry toolRegistry;

    private final PermissionManager permissionManager;

    private final AndroidPermissionBridge permissionBridge;

    private final CapabilityDiscovery capabilityDiscovery;

    private final CapabilityExecutor capabilityExecutor;

    private final CodeEvolutionEngine codeEvolutionEngine;

    private final SourceEvolutionEngine sourceEvolutionEngine;

    private final SelfBuilder selfBuilder;

    private final SelfTestEngine selfTestEngine;

    private final RecoveryEngine recoveryEngine;

    private final BuildEngine buildEngine;

    private final EvolutionVerificationEngine verificationEngine;

    private final EvolutionOrchestrator evolutionOrchestrator;

    private final EvolutionCore evolutionCore;

    private final JarvisBrain brain;

    private final AndroidIntentTool androidIntentTool;

    private boolean initialized;

    public JarvisSystem(
            Context context
    ) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "context cannot be null."
            );
        }

        this.context =
                context.getApplicationContext();

        /*
         * =========================================================
         * INTERNAL WORKSPACE
         * =========================================================
         */

        this.workspaceRoot =
                new File(
                        this.context.getFilesDir(),
                        "jarvis_workspace"
                );

        /*
         * =========================================================
         * SECURITY
         * =========================================================
         */

        this.securityBoundary =
                new OwnerSecurityBoundary();

        /*
         * =========================================================
         * PROJECT WORKSPACE
         * =========================================================
         *
         * هذا لا يعني أننا نفترض أن المشروع الحقيقي موجود
         * داخل jarvis_workspace.
         *
         * المشروع الحقيقي خاصو يتسجل بشكل مستقل.
         */

        this.projectWorkspaceManager =
                new ProjectWorkspaceManager(
                        this.securityBoundary
                );

        /*
         * =========================================================
         * RUNTIME
         * =========================================================
         */

        this.runtime =
                new JarvisRuntime();

        /*
         * =========================================================
         * TOOLS
         * =========================================================
         */

        this.toolRegistry =
                new ToolRegistry();

        this.androidIntentTool =
                new AndroidIntentTool(
                        this.context
                );

        /*
         * =========================================================
         * PERMISSIONS
         * =========================================================
         */

        this.permissionManager =
                new PermissionManager();

        this.permissionBridge =
                new AndroidPermissionBridge(
                        this.context
                );

        /*
         * =========================================================
         * EVOLUTION FOUNDATION
         * =========================================================
         */

        /*
         * CodeEvolutionEngine أصبح مربوطاً بـ
         * ProjectWorkspaceManager.
         *
         * لذلك عندما لا يكون المشروع الحقيقي configured:
         * لا يتم تحويل jarvis_workspace إلى مشروع مزيف.
         */

        this.codeEvolutionEngine =
                new CodeEvolutionEngine(
                        this.projectWorkspaceManager,
                        this.securityBoundary
                );

        this.sourceEvolutionEngine =
                new SourceEvolutionEngine(
                        this.codeEvolutionEngine
                );

        /*
         * SelfBuilder مازال يستعمل workspace الداخلي
         * لإنشاء capability artifacts.
         */
        this.selfBuilder =
                new SelfBuilder(
                        this.securityBoundary,
                        this.workspaceRoot
                );

        this.selfTestEngine =
                new SelfTestEngine();

        this.recoveryEngine =
                new RecoveryEngine(
                        this.workspaceRoot
                );

        /*
         * BuildEngine مربوط بالمشروع الحقيقي.
         */
        this.buildEngine =
                new BuildEngine(
                        this.projectWorkspaceManager,
                        this.securityBoundary
                );

        /*
         * Verification:
         *
         * workspaceRoot
         *     = artifact workspace
         *
         * projectWorkspaceManager
         *     = real Android project
         */
        this.verificationEngine =
                new EvolutionVerificationEngine(
                        this.workspaceRoot,
                        this.projectWorkspaceManager,
                        this.securityBoundary
                );

        /*
         * =========================================================
         * ORCHESTRATOR
         * =========================================================
         */

        this.evolutionOrchestrator =
                new EvolutionOrchestrator(
                        this.securityBoundary,
                        this.selfBuilder,
                        this.selfTestEngine,
                        this.recoveryEngine,
                        this.codeEvolutionEngine,
                        this.sourceEvolutionEngine,
                        this.buildEngine,
                        this.verificationEngine
                );

        /*
         * =========================================================
         * DISCOVERY + EXECUTION
         * =========================================================
         */

        this.capabilityDiscovery =
                new CapabilityDiscovery(
                        this.toolRegistry,
                        this.permissionManager
                );

        this.capabilityExecutor =
                new CapabilityExecutor(
                        this.runtime,
                        this.toolRegistry,
                        this.permissionManager
                );

        /*
         * =========================================================
         * EVOLUTION CORE
         * =========================================================
         */

        this.evolutionCore =
                new EvolutionCore(
                        this.capabilityDiscovery,
                        this.capabilityExecutor,
                        this.evolutionOrchestrator
                );

        /*
         * =========================================================
         * BRAIN
         * =========================================================
         */

        this.brain =
                new JarvisBrain(
                        this.evolutionCore,
                        this.runtime
                );

        this.initialized =
                false;
    }

    /**
     * تشغيل JARVIS.
     */
    public synchronized JarvisResult<Boolean> start() {

        if (initialized) {

            return JarvisResult.success(
                    true,
                    "JARVIS V2 is already running."
            );
        }

        try {

            /*
             * =====================================================
             * 1. OWNER
             * =====================================================
             */

            JarvisResult<Boolean> ownerResult =
                    initializeOwner();

            if (!ownerResult.isSuccess()) {

                return JarvisResult.failure(
                        ownerResult.getError(),
                        ownerResult.getMessage()
                );
            }

            /*
             * =====================================================
             * 2. INTERNAL WORKSPACE
             * =====================================================
             */

            if (!workspaceRoot.exists()) {

                if (!workspaceRoot.mkdirs()) {

                    return failure(
                            JarvisError.Type.FILE_OPERATION_FAILED,
                            "Could not create JARVIS workspace."
                    );
                }
            }

            if (!workspaceRoot.isDirectory()) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "JARVIS workspace is not a directory."
                );
            }

            /*
             * =====================================================
             * 3. CODE EVOLUTION
             * =====================================================
             */

            JarvisResult<Boolean> codeResult =
                    codeEvolutionEngine.initialize();

            if (!codeResult.isSuccess()) {

                return JarvisResult.failure(
                        codeResult.getError(),
                        codeResult.getMessage()
                );
            }

            /*
             * =====================================================
             * 4. SOURCE EVOLUTION
             * =====================================================
             */

            JarvisResult<Boolean> sourceResult =
                    sourceEvolutionEngine.initialize();

            if (!sourceResult.isSuccess()) {

                return JarvisResult.failure(
                        sourceResult.getError(),
                        sourceResult.getMessage()
                );
            }

            /*
             * =====================================================
             * 5. PROJECT REVALIDATION
             * =====================================================
             *
             * إذا لم يكن هناك مشروع حقيقي configured:
             * لا نعتبر ذلك خطأ startup.
             *
             * JARVIS يقدر يخدم runtime capabilities
             * ويستعمل evolution الداخلي.
             *
             * أما Build/Source Evolution الحقيقي:
             * يحتاج مشروعاً حقيقياً configured.
             */

            projectWorkspaceManager.revalidate();

            /*
             * =====================================================
             * 6. ANDROID PERMISSIONS
             * =====================================================
             */

            permissionBridge.synchronize(
                    permissionManager
            );

            /*
             * =====================================================
             * 7. BUILT-IN TOOLS
             * =====================================================
             */

            JarvisResult<Boolean> toolResult =
                    registerTool(
                            androidIntentTool
                    );

            if (!toolResult.isSuccess()) {

                return JarvisResult.failure(
                        toolResult.getError(),
                        toolResult.getMessage()
                );
            }

            /*
             * =====================================================
             * 8. RUNTIME
             * =====================================================
             */

            JarvisResult<Boolean> runtimeResult =
                    runtime.start();

            if (!runtimeResult.isSuccess()) {

                toolRegistry.remove(
                        AndroidIntentTool.TOOL_ID
                );

                runtime.unregisterTool(
                        AndroidIntentTool.TOOL_ID
                );

                return JarvisResult.failure(
                        runtimeResult.getError(),
                        runtimeResult.getMessage()
                );
            }

            initialized =
                    true;

            return JarvisResult.success(
                    true,
                    "JARVIS V2 started successfully."
            );

        } catch (Exception exception) {

            initialized =
                    false;

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.INTERNAL_ERROR,
                            "JARVIS V2 startup failed.",
                            "JarvisSystem",
                            exception
                    )
            );
        }
    }

    /**
     * إيقاف JARVIS.
     */
    public synchronized JarvisResult<Boolean> stop() {

        try {

            runtime.stop();

            initialized =
                    false;

            return JarvisResult.success(
                    true,
                    "JARVIS V2 stopped."
            );

        } catch (Exception exception) {

            return JarvisResult.failure(
                    JarvisError.fromException(
                            JarvisError.Type.INTERNAL_ERROR,
                            "JARVIS V2 stop failed.",
                            "JarvisSystem",
                            exception
                    )
            );
        }
    }

    /**
     * إرسال أمر إلى Brain.
     */
    public synchronized JarvisResult<JarvisBrain.BrainResponse>
    processCommand(
            String command
    ) {

        if (!initialized) {

            JarvisResult<Boolean> startResult =
                    start();

            if (!startResult.isSuccess()) {

                return JarvisResult.failure(
                        startResult.getError(),
                        startResult.getMessage()
                );
            }
        }

        return brain.process(
                command
        );
    }

    /**
     * تسجيل Tool في Registry وRuntime معاً.
     */
    public synchronized JarvisResult<Boolean> registerTool(
            ToolContract tool
    ) {

        if (tool == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool cannot be null."
            );
        }

        String toolId =
                normalizeToolId(
                        tool.getId()
                );

        if (toolId.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool ID cannot be empty."
            );
        }

        if (!tool.isAvailable()) {

            return failure(
                    JarvisError.Type.TOOL_UNAVAILABLE,
                    "Tool is not available: " + toolId
            );
        }

        /*
         * الحالة المتناسقة موجودة بالفعل.
         */
        if (toolRegistry.contains(toolId) &&
                runtime.containsTool(toolId)) {

            return JarvisResult.success(
                    true,
                    "Tool is already registered: "
                            + toolId
            );
        }

        /*
         * تنظيف أي حالة جزئية.
         */
        if (toolRegistry.contains(toolId) ||
                runtime.containsTool(toolId)) {

            toolRegistry.remove(
                    toolId
            );

            runtime.unregisterTool(
                    toolId
            );
        }

        /*
         * Registry.
         */
        JarvisResult<ToolContract> registryResult =
                toolRegistry.register(
                        tool
                );

        if (!registryResult.isSuccess()) {

            return JarvisResult.failure(
                    registryResult.getError(),
                    registryResult.getMessage()
            );
        }

        /*
         * Runtime.
         */
        JarvisResult<Boolean> runtimeResult =
                runtime.registerTool(
                        tool
                );

        if (!runtimeResult.isSuccess()) {

            toolRegistry.remove(
                    toolId
            );

            return JarvisResult.failure(
                    runtimeResult.getError(),
                    "Tool registration failed in Runtime: "
                            + runtimeResult.getMessage()
            );
        }

        /*
         * تحقق نهائي.
         */
        if (!toolRegistry.contains(toolId) ||
                !runtime.containsTool(toolId)) {

            toolRegistry.remove(
                    toolId
            );

            runtime.unregisterTool(
                    toolId
            );

            return failure(
                    JarvisError.Type.INTERNAL_ERROR,
                    "Tool registration consistency check failed: "
                            + toolId
            );
        }

        return JarvisResult.success(
                true,
                "Tool registered in Registry and Runtime: "
                        + toolId
        );
    }

    /**
     * إزالة Tool من Registry وRuntime.
     */
    public synchronized JarvisResult<Boolean> unregisterTool(
            String toolId
    ) {

        String normalizedId =
                normalizeToolId(
                        toolId
                );

        if (normalizedId.isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Tool ID cannot be empty."
            );
        }

        boolean registryHadTool =
                toolRegistry.contains(
                        normalizedId
                );

        boolean runtimeHadTool =
                runtime.containsTool(
                        normalizedId
                );

        if (!registryHadTool &&
                !runtimeHadTool) {

            return failure(
                    JarvisError.Type.NOT_FOUND,
                    "Tool not found: " + normalizedId
            );
        }

        JarvisResult<ToolContract> registryResult =
                toolRegistry.remove(
                        normalizedId
                );

        if (!registryResult.isSuccess() &&
                registryHadTool) {

            return JarvisResult.failure(
                    registryResult.getError(),
                    registryResult.getMessage()
            );
        }

        JarvisResult<Boolean> runtimeResult =
                runtime.unregisterTool(
                        normalizedId
                );

        if (!runtimeResult.isSuccess() &&
                runtimeHadTool) {

            return JarvisResult.failure(
                    runtimeResult.getError(),
                    "Tool removed from Registry but could not be removed "
                            + "from Runtime: "
                            + runtimeResult.getMessage()
            );
        }

        return JarvisResult.success(
                true,
                "Tool removed from Registry and Runtime: "
                        + normalizedId
        );
    }

    /**
     * =========================================================
     * PROJECT WORKSPACE
     * =========================================================
     *
     * يسمح للنظام بتحديد مشروع Android الحقيقي.
     *
     * لا يتم استعمال jarvis_workspace كبديل.
     */
    public synchronized JarvisResult<Boolean>
    configureProjectWorkspace(
            File projectRoot
    ) {

        if (projectRoot == null) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Project root cannot be null."
            );
        }

        JarvisResult<Boolean> result =
                projectWorkspaceManager.configure(
                        projectRoot
                );

        if (!result.isSuccess()) {

            return result;
        }

        /*
         * بعد configuration:
         * نعيد فحص المكونات التي تعتمد على المشروع.
         */
        projectWorkspaceManager.revalidate();

        return JarvisResult.success(
                true,
                "Real Android project workspace configured."
        );
    }

    /**
     * Configuration بواسطة path.
     */
    public synchronized JarvisResult<Boolean>
    configureProjectWorkspace(
            String projectPath
    ) {

        if (projectPath == null ||
                projectPath.trim().isEmpty()) {

            return failure(
                    JarvisError.Type.INVALID_REQUEST,
                    "Project path cannot be empty."
            );
        }

        JarvisResult<Boolean> result =
                projectWorkspaceManager.configurePath(
                        projectPath
                );

        if (!result.isSuccess()) {

            return result;
        }

        projectWorkspaceManager.revalidate();

        return JarvisResult.success(
                true,
                "Real Android project workspace configured."
        );
    }

    /**
     * حذف project workspace من إعدادات JARVIS.
     *
     * هذا لا يحذف ملفات المشروع.
     */
    public synchronized JarvisResult<Boolean>
    clearProjectWorkspace() {

        projectWorkspaceManager.clear();

        return JarvisResult.success(
                true,
                "Project workspace configuration cleared."
        );
    }

    /**
     * إعادة فحص المشروع الحقيقي.
     */
    public synchronized JarvisResult<Boolean>
    revalidateProjectWorkspace() {

        return projectWorkspaceManager.revalidate();
    }

    /**
     * هل المشروع الحقيقي جاهز؟
     */
    public synchronized boolean
    isProjectWorkspaceReady() {

        return projectWorkspaceManager.isReady();
    }

    /**
     * مزامنة صلاحيات Android.
     */
    public synchronized void synchronizePermissions() {

        permissionBridge.synchronize(
                permissionManager
        );
    }

    /**
     * حالة النظام.
     */
    public synchronized SystemStatus getStatus() {

        return new SystemStatus(
                initialized,
                securityBoundary.isActive(),
                runtime.isRunning(),
                workspaceRoot.exists(),
                projectWorkspaceManager.isReady(),
                toolRegistry.getToolCount(),
                permissionManager
                        .getGrantedPermissions()
                        .size(),
                permissionManager
                        .getRequestedPermissions()
                        .size(),
                evolutionOrchestrator
                        .getState()
                        .name()
        );
    }

    public Context getContext() {
        return context;
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
    }

    public ProjectWorkspaceManager
    getProjectWorkspaceManager() {

        return projectWorkspaceManager;
    }

    public OwnerSecurityBoundary
    getSecurityBoundary() {

        return securityBoundary;
    }

    public JarvisRuntime getRuntime() {
        return runtime;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public PermissionManager
    getPermissionManager() {

        return permissionManager;
    }

    public AndroidPermissionBridge
    getPermissionBridge() {

        return permissionBridge;
    }

    public CapabilityDiscovery
    getCapabilityDiscovery() {

        return capabilityDiscovery;
    }

    public CapabilityExecutor
    getCapabilityExecutor() {

        return capabilityExecutor;
    }

    public CodeEvolutionEngine
    getCodeEvolutionEngine() {

        return codeEvolutionEngine;
    }

    public SourceEvolutionEngine
    getSourceEvolutionEngine() {

        return sourceEvolutionEngine;
    }

    public SelfBuilder getSelfBuilder() {
        return selfBuilder;
    }

    public SelfTestEngine
    getSelfTestEngine() {

        return selfTestEngine;
    }

    public RecoveryEngine
    getRecoveryEngine() {

        return recoveryEngine;
    }

    public BuildEngine getBuildEngine() {
        return buildEngine;
    }

    public EvolutionVerificationEngine
    getVerificationEngine() {

        return verificationEngine;
    }

    public EvolutionOrchestrator
    getEvolutionOrchestrator() {

        return evolutionOrchestrator;
    }

    public EvolutionCore getEvolutionCore() {
        return evolutionCore;
    }

    public JarvisBrain getBrain() {
        return brain;
    }

    public AndroidIntentTool
    getAndroidIntentTool() {

        return androidIntentTool;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Owner initialization.
     */
    private JarvisResult<Boolean>
    initializeOwner() {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        String ownerId =
                preferences.getString(
                        OWNER_ID_KEY,
                        null
                );

        if (ownerId == null ||
                ownerId.trim().isEmpty()) {

            ownerId =
                    DEFAULT_OWNER_ID;

            boolean saved =
                    preferences.edit()
                            .putString(
                                    OWNER_ID_KEY,
                                    ownerId
                            )
                            .commit();

            if (!saved) {

                return failure(
                        JarvisError.Type.FILE_OPERATION_FAILED,
                        "Could not persist owner identity."
                );
            }
        }

        if (!securityBoundary.isInitialized()) {

            JarvisResult<Void> initializeResult =
                    securityBoundary.initializeOwner(
                            ownerId
                    );

            if (!initializeResult.isSuccess()) {

                return JarvisResult.failure(
                        initializeResult.getError(),
                        initializeResult.getMessage()
                );
            }

        } else if (!securityBoundary.isOwner(ownerId)) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Stored owner identity does not match security boundary."
            );
        }

        if (!securityBoundary.isActive()) {

            return failure(
                    JarvisError.Type.NOT_AUTHORIZED,
                    "Owner security boundary is not active."
            );
        }

        return JarvisResult.success(
                true,
                "Owner identity initialized."
        );
    }

    private static String normalizeToolId(
            String toolId
    ) {

        if (toolId == null) {
            return "";
        }

        return toolId.trim();
    }

    private JarvisResult<Boolean> failure(
            JarvisError.Type type,
            String message
    ) {

        return JarvisResult.failure(
                JarvisError.of(
                        type,
                        message,
                        "JarvisSystem"
                )
        );
    }

    /**
     * حالة النظام.
     */
    public static final class SystemStatus {

        private final boolean initialized;

        private final boolean securityActive;

        private final boolean runtimeRunning;

        private final boolean workspaceAvailable;

        private final boolean projectWorkspaceReady;

        private final int toolCount;

        private final int grantedPermissionCount;

        private final int requestedPermissionCount;

        private final String evolutionState;

        private SystemStatus(
                boolean initialized,
                boolean securityActive,
                boolean runtimeRunning,
                boolean workspaceAvailable,
                boolean projectWorkspaceReady,
                int toolCount,
                int grantedPermissionCount,
                int requestedPermissionCount,
                String evolutionState
        ) {

            this.initialized =
                    initialized;

            this.securityActive =
                    securityActive;

            this.runtimeRunning =
                    runtimeRunning;

            this.workspaceAvailable =
                    workspaceAvailable;

            this.projectWorkspaceReady =
                    projectWorkspaceReady;

            this.toolCount =
                    toolCount;

            this.grantedPermissionCount =
                    grantedPermissionCount;

            this.requestedPermissionCount =
                    requestedPermissionCount;

            this.evolutionState =
                    evolutionState;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public boolean isSecurityActive() {
            return securityActive;
        }

        public boolean isRuntimeRunning() {
            return runtimeRunning;
        }

        public boolean isWorkspaceAvailable() {
            return workspaceAvailable;
        }

        public boolean isProjectWorkspaceReady() {
            return projectWorkspaceReady;
        }

        public int getToolCount() {
            return toolCount;
        }

        public int getGrantedPermissionCount() {
            return grantedPermissionCount;
        }

        public int getRequestedPermissionCount() {
            return requestedPermissionCount;
        }

        public String getEvolutionState() {
            return evolutionState;
        }

        @Override
        public String toString() {

            return "SystemStatus{" +
                    "initialized=" +
                    initialized +
                    ", securityActive=" +
                    securityActive +
                    ", runtimeRunning=" +
                    runtimeRunning +
                    ", workspaceAvailable=" +
                    workspaceAvailable +
                    ", projectWorkspaceReady=" +
                    projectWorkspaceReady +
                    ", toolCount=" +
                    toolCount +
                    ", grantedPermissionCount=" +
                    grantedPermissionCount +
                    ", requestedPermissionCount=" +
                    requestedPermissionCount +
                    ", evolutionState='" +
                    evolutionState +
                    '\'' +
                    '}';
        }
    }
}