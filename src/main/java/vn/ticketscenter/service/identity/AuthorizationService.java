package vn.ticketscenter.service.identity;

import jakarta.persistence.EntityManager;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.identity.OrganizationRole;

import java.util.Set;
import java.util.UUID;

/**
 * Checks authorization based on current DB state.
 * Reads live membership from DB — not from session.
 * BLOCKED: Full implementation in KHANH-06 / DONG-01 (MembershipAuthorizationRepository).
 * Owner: Khánh (KHANH-06)
 */
public class AuthorizationService {

    private static final AuthorizationService INSTANCE = new AuthorizationService();
    private AuthorizationService() {}
    public static AuthorizationService getInstance() { return INSTANCE; }

    /**
     * Throws BusinessException(403) if actor does not have ADMIN platform role.
     */
    public void requireAdmin(ActorContext actor) {
        if (!actor.isAdmin()) {
            throw BusinessException.forbidden("Admin role required.");
        }
    }

    /**
     * Throws BusinessException(403) if actor does not hold one of the allowed
     * OrganizationRoles in the given organization, OR organization is not APPROVED.
     * Reads membership from DB using EntityManager of the current transaction.
     *
     * BLOCKED KHANH-06/DONG-01: MembershipAuthorizationRepository not yet available.
     */
    public void requireOrganizationRole(
            ActorContext actor,
            UUID organizationId,
            Set<OrganizationRole> allowed,
            EntityManager em) {
        // TODO KHANH-06: call MembershipAuthorizationRepository.findCurrent(em, actorId, orgId)
        throw BusinessException.forbidden(
                "Organization role check not yet implemented. Task: KHANH-06/DONG-01.");
    }
}
