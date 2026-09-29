package com.kamal.jarvis.v2;

import android.content.Context;
import android.content.SharedPreferences;

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
import com.kamal.jarvis.v2.evolution.RecoveryEngine;
import com.kamal.jarvis.v2.evolution.SelfBuilder;
import com.kamal.jarvis.v2.evolution.SelfTestEngine;
import com.kamal.jarvis.v2.evolution.SourceEvolutionEngine;
import com.kamal.jarvis.v2.intelligence.JarvisBrain;
import com.kamal.jarvis.v2.permissions.AndroidPermissionBridge;
import com.kamal.jarvis.v2.permissions.PermissionManager;
import com.kamal.jarvis.v2.security.OwnerSecurityBoundary;
import com.kamal.jarvis.v2.tools.ToolRegistry;

import java.io.File;

/**
 * JARVIS V2 - System
 *
 * نقطة التجميع الحقيقية للنظام.
 *
 * المسؤوليات:
 *
 * 1. إنشاء جميع المكونات الأساسية.
 * 2. ربط المكونات مع بعضها.
 * 3. تهيئة Owner Security.
 * 4. تهيئة Permission Manager.
 * 5. تهيئة Workspace.
 * 6. تشغيل Runtime.
 * 7. إعطاء المستوى الأعلى نقطة واحدة للتعامل مع JARVIS.
 *
 * هذا الملف لا يحتوي على منطق وهمي.
 * جميع المكونات التي يتم إنشاؤها هنا هي المكونات
 * الفعلية الموجودة داخل JARVIS V2.
 */
public final class JarvisSystem {

    private static final String PREFS_NAME =
            "jarvis_v2_system";

    private static final String OWNER_ID_KEY =
            "owner_id";

    private static final String DEFAULT_OWNER_ID =
            "KAMAL_OWNER";

    private final Context context;

    private final File workspaceRoot;

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

        this.codeEvolutionEngine =
                new CodeEvolutionEngine(
                        this.workspaceRoot,
                        this.securityBoundary
                );

        this.sourceEvolutionEngine =
                new SourceEvolutionEngine(
                        this.codeEvolutionEngine
                );

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

        this.buildEngine =
                new BuildEngine(
                        this.workspaceRoot,
                        this.securityBoundary
                );

        this.verificationEngine =
                new EvolutionVerificationEngine(
                        this.workspaceRoot,
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
     * تشغيل JARVIS بالكامل.
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
             * -----------------------------------------------------
             * 1. Owner identity
             * -----------------------------------------------------
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
             * -----------------------------------------------------
             * 2. Workspace
             * -----------------------------------------------------
             */

            if (!workspaceRoot.exists()) {

                if (!workspaceRoot.mkdirs()) {

                    return JarvisResult.failure(
                            com.kamal.jarvis.v2.core.JarvisError.of(
                                    com.kamal.jarvis.v2.core.JarvisError.Type.FILE_OPERATION_FAILED,
                                    "Could not create JARVIS workspace.",
                                    "JarvisSystem"
                            )
                    );
                }
            }

            if (!workspaceRoot.isDirectory()) {

                return JarvisResult.failure(
                        com.kamal.jarvis.v2.core.JarvisError.of(
                                com.kamal.jarvis.v2.core.JarvisError.Type.FILE_OPERATION_FAILED,
                                "JARVIS workspace is not a directory.",
                                "JarvisSystem"
                        )
                );
            }

            /*
             * -----------------------------------------------------
             * 3. Code evolution
             * -----------------------------------------------------
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
             * -----------------------------------------------------
             * 4. Source evolution
             * -----------------------------------------------------
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
             * -----------------------------------------------------
             * 5. Android permission state
             * -----------------------------------------------------
             */

            permissionBridge.synchronize(
                    permissionManager
            );

            /*
             * -----------------------------------------------------
             * 6. Runtime
             * -----------------------------------------------------
             */

            JarvisResult<Boolean> runtimeResult =
                    runtime.start();

            if (!runtimeResult.isSuccess()) {

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
                    com.kamal.jarvis.v2.core.JarvisError.fromException(
                            com.kamal.jarvis.v2.core.JarvisError.Type.INTERNAL_ERROR,
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
                    com.kamal.jarvis.v2.core.JarvisError.fromException(
                            com.kamal.jarvis.v2.core.JarvisError.Type.INTERNAL_ERROR,
                            "JARVIS V2 stop failed.",
                            "JarvisSystem",
                            exception
                    )
            );
        }
    }

    /**
     * إرسال أمر حقيقي إلى Brain.
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
     * تسجيل Tool حقيقي داخل النظام.
     */
    public synchronized JarvisResult<Boolean> registerTool(
            ToolContract tool
    ) {

        if (tool == null) {

            return JarvisResult.failure(
                    com.kamal.jarvis.v2.core.JarvisError.of(
                            com.kamal.jarvis.v2.core.JarvisError.Type.INVALID_REQUEST,
                            "Tool cannot be null.",
                            "JarvisSystem"
                    )
            );
        }

        return toolRegistry.register(
                tool
        );
    }

    /**
     * فحص صلاحيات Android الحالية ومزامنتها.
     */
    public synchronized void synchronizePermissions() {

        permissionBridge.synchronize(
                permissionManager
        );
    }

    /**
     * التحقق من حالة JARVIS.
     */
    public synchronized SystemStatus getStatus() {

        return new SystemStatus(
                initialized,
                securityBoundary.isActive(),
                runtime.isRunning(),
                workspaceRoot.exists(),
                toolRegistry.getToolCount(),
                permissionManager.getGrantedPermissions().size(),
                permissionManager.getRequestedPermissions().size(),
                evolutionOrchestrator.getState().name()
        );
    }

    public Context getContext() {
        return context;
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
    }

    public OwnerSecurityBoundary getSecurityBoundary() {
        return securityBoundary;
    }

    public JarvisRuntime getRuntime() {
        return runtime;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public AndroidPermissionBridge getPermissionBridge() {
        return permissionBridge;
    }

    public CapabilityDiscovery getCapabilityDiscovery() {
        return capabilityDiscovery;
    }

    public CapabilityExecutor getCapabilityExecutor() {
        return capabilityExecutor;
    }

    public CodeEvolutionEngine getCodeEvolutionEngine() {
        return codeEvolutionEngine;
    }

    public SourceEvolutionEngine getSourceEvolutionEngine() {
        return sourceEvolutionEngine;
    }

    public SelfBuilder getSelfBuilder() {
        return selfBuilder;
    }

    public SelfTestEngine getSelfTestEngine() {
        return selfTestEngine;
    }

    public RecoveryEngine getRecoveryEngine() {
        return recoveryEngine;
    }

    public BuildEngine getBuildEngine() {
        return buildEngine;
    }

    public EvolutionVerificationEngine getVerificationEngine() {
        return verificationEngine;
    }

    public EvolutionOrchestrator getEvolutionOrchestrator() {
        return evolutionOrchestrator;
    }

    public EvolutionCore getEvolutionCore() {
        return evolutionCore;
    }

    public JarvisBrain getBrain() {
        return brain;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * تهيئة Owner identity بشكل دائم داخل مساحة التطبيق الخاصة.
     *
     * هذا يحافظ على نفس هوية المالك بين تشغيلات التطبيق.
     *
     * ملاحظة:
     * هذه هوية داخلية وليست بديلاً عن Biometric/PIN.
     * المصادقة القوية سنربطها لاحقاً بحدود الأمان نفسها.
     */
    private JarvisResult<Boolean> initializeOwner() {

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

                return JarvisResult.failure(
                        com.kamal.jarvis.v2.core.JarvisError.of(
                                com.kamal.jarvis.v2.core.JarvisError.Type.MEMORY_ERROR,
                                "Could not persist JARVIS owner identity.",
                                "JarvisSystem"
                        )
                );
            }
        }

        if (securityBoundary.isInitialized()) {

            if (!securityBoundary.isOwner(
                    ownerId
            )) {

                return JarvisResult.failure(
                        com.kamal.jarvis.v2.core.JarvisError.of(
                                com.kamal.jarvis.v2.core.JarvisError.Type.NOT_AUTHORIZED,
                                "Stored owner identity does not match the active security boundary.",
                                "JarvisSystem"
                        )
                );
            }

            return JarvisResult.success(
                    true,
                    "Owner security is already active."
            );
        }

        JarvisResult<Void> result =
                securityBoundary.initializeOwner(
                        ownerId
                );

        if (!result.isSuccess()) {

            return JarvisResult.failure(
                    result.getError(),
                    result.getMessage()
            );
        }

        return JarvisResult.success(
                true,
                "Owner security initialized."
        );
    }

    /**
     * حالة النظام.
     */
    public static final class SystemStatus {

        private final boolean initialized;
        private final boolean securityActive;
        private final boolean runtimeRunning;
        private final boolean workspaceReady;
        private final int toolCount;
        private final int grantedPermissionCount;
        private final int requestedPermissionCount;
        private final String evolutionState;

        private SystemStatus(
                boolean initialized,
                boolean securityActive,
                boolean runtimeRunning,
                boolean workspaceReady,
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

            this.workspaceReady =
                    workspaceReady;

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

        public boolean isWorkspaceReady() {
            return workspaceReady;
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
    }
}