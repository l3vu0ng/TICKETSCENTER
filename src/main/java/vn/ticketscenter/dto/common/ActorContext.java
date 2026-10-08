package vn.ticketscenter.dto.common;

import java.util.UUID;
import vn.ticketscenter.model.identity.PlatformRole;

/**
 * Immutable actor context for every authenticated request. Created from DB-verified session only —
 * NEVER from request body.
 */
public record ActorContext(
        UUID actorId,
        PlatformRole platformRole,
        int authVersion,
        boolean emailVerified,
        ActorType type) {
    public ActorContext {
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(platformRole, "platformRole");
        if (type == ActorType.USER && (actorId == null || authVersion < 1)) {
            throw new IllegalArgumentException("User actor requires identity and authVersion");
        }
        if (type == ActorType.SYSTEM
                && (actorId != null
                        || platformRole != PlatformRole.CUSTOMER
                        || authVersion != 0
                        || emailVerified)) {
            throw new IllegalArgumentException("System actor cannot impersonate a user");
        }
    }

    public static ActorContext ofUser(
            UUID actorId, PlatformRole platformRole, int authVersion, boolean emailVerified) {
        return new ActorContext(actorId, platformRole, authVersion, emailVerified, ActorType.USER);
    }

    public static ActorContext system() {
        return new ActorContext(null, PlatformRole.CUSTOMER, 0, false, ActorType.SYSTEM);
    }

    public boolean isAdmin() {
        return type == ActorType.USER && platformRole == PlatformRole.ADMIN;
    }
}
