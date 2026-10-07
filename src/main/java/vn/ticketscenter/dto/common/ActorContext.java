package vn.ticketscenter.dto.common;

import vn.ticketscenter.model.identity.PlatformRole;

import java.util.UUID;

/**
 * Immutable actor context for every authenticated request.
 * Created from DB-verified session only — NEVER from request body.
 * Owner: Khánh (KHANH-02/06)
 */
public record ActorContext(
        UUID actorId,
        PlatformRole platformRole,
        int authVersion,
        boolean emailVerified,
        ActorType type
) {
    public static ActorContext ofUser(UUID actorId, PlatformRole platformRole,
                                      int authVersion, boolean emailVerified) {
        return new ActorContext(actorId, platformRole, authVersion,
                emailVerified, ActorType.USER);
    }

    public static ActorContext system() {
        return new ActorContext(null, PlatformRole.CUSTOMER, 0, false, ActorType.SYSTEM);
    }

    public boolean isAdmin() { return platformRole == PlatformRole.ADMIN; }
}
