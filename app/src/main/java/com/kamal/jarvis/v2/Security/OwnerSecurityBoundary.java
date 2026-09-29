package com.kamal.jarvis.v2.security;

import com.kamal.jarvis.v2.core.JarvisError;
import com.kamal.jarvis.v2.core.JarvisResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class OwnerSecurityBoundary {

    private static final String OWNER_ROLE = "OWNER";

    private final Set<String> protectedAreas;
    private boolean initialized;
    private String ownerId;

    public OwnerSecurityBoundary() {
        protectedAreas = new HashSet<>();

        protectedAreas.add("OWNER_IDENTITY");
        protectedAreas.add("SECURITY_POLICY");
        protectedAreas.add("AUTHORIZATION");
        protectedAreas.add("EVOLUTION_SECURITY");
        protectedAreas.add("RECOVERY_SECURITY");

        initialized = false;
        ownerId = "";
    }

    public synchronized JarvisResult<Void> initializeOwner(
            String requestedOwnerId
    ) {
        if (initialized) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner identity is already initialized.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        if (!isValidOwnerId(requestedOwnerId)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.VALIDATION_FAILED,
                            "Invalid owner identity.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        ownerId = normalize(requestedOwnerId);
        initialized = true;

        return JarvisResult.success(
                null,
                "Owner identity initialized."
        );
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized boolean isActive() {
        return initialized;
    }

    public synchronized boolean isOwner(
            String identity
    ) {
        if (!initialized || !isValidOwnerId(identity)) {
            return false;
        }

        return ownerId.equals(normalize(identity));
    }

    public synchronized JarvisResult<Void> authorizeOwnerOperation(
            String identity,
            String operation
    ) {
        if (!initialized) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner security has not been initialized.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        if (!isOwner(identity)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner authorization required.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        if (operation == null || operation.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Operation cannot be empty.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        return JarvisResult.success(
                null,
                "Owner operation authorized."
        );
    }

    public synchronized boolean isProtectedArea(
            String area
    ) {
        if (area == null) {
            return false;
        }

        return protectedAreas.contains(
                normalize(area)
        );
    }

    /**
     * Compatibility-safe authorization method.
     *
     * بعض المكونات كتتعامل معها كـ JarvisResult<Void>
     * وبعضها كـ JarvisResult<Boolean>.
     *
     * الدالة كتستعمل generic type باش بجوج الاستعمالات
     * يبقاو متوافقين بدون تغيير منطق الحماية.
     *
     * قيمة النجاح نفسها لا تحتاجها عملية التفويض؛
     * المهم هو isSuccess().
     */
    public synchronized <T> JarvisResult<T> authorizeEvolutionChange(
            String area
    ) {
        if (area == null || area.trim().isEmpty()) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.INVALID_REQUEST,
                            "Protected area cannot be empty.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        String normalizedArea = normalize(area);

        if (isProtectedArea(normalizedArea)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Evolution cannot modify protected security area: "
                                    + normalizedArea,
                            "OwnerSecurityBoundary"
                    )
            );
        }

        return JarvisResult.success(
                null,
                "Evolution change is outside protected security areas."
        );
    }

    public synchronized Set<String> getProtectedAreas() {
        return Collections.unmodifiableSet(
                new HashSet<>(protectedAreas)
        );
    }

    public synchronized String getSecurityStatus() {
        if (!initialized) {
            return "UNINITIALIZED";
        }

        return "ACTIVE";
    }

    public synchronized JarvisResult<String> getOwnerRole(
            String identity
    ) {
        if (!isOwner(identity)) {
            return JarvisResult.failure(
                    JarvisError.of(
                            JarvisError.Type.NOT_AUTHORIZED,
                            "Owner authorization required.",
                            "OwnerSecurityBoundary"
                    )
            );
        }

        return JarvisResult.success(
                OWNER_ROLE,
                "Owner role verified."
        );
    }

    private boolean isValidOwnerId(
            String identity
    ) {
        if (identity == null) {
            return false;
        }

        String value = identity.trim();

        return !value.isEmpty()
                && value.length() >= 3
                && value.length() <= 128;
    }

    private String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim().toUpperCase();
    }
}