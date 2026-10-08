package vn.ticketscenter.service.identity;

import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.ActorType;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.identity.OrganizationRole;
import vn.ticketscenter.model.identity.PlatformRole;
import vn.ticketscenter.model.identity.UserStatus;
import vn.ticketscenter.repository.identity.UserRepository;
import vn.ticketscenter.transaction.TransactionContext;

/** Guards use the current transaction; membership decisions belong to Đông. */
public final class AuthorizationService {
    @FunctionalInterface
    public interface MembershipGuard {
        boolean allows(
                EntityManager em, UUID userId, UUID organizationId, Set<OrganizationRole> allowed);
    }

    private final UserRepository users;
    private final MembershipGuard memberships;

    public AuthorizationService(UserRepository users, MembershipGuard memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    private void requireCurrent(ActorContext actor) {
        if (!java.util.Objects.equals(actor, TransactionContext.actor())) {
            throw BusinessException.forbidden("Actor does not match the current transaction");
        }
        if (actor == null || actor.type() != ActorType.USER)
            throw BusinessException.forbidden("User actor required");
        var current =
                users.findById(TransactionContext.entityManager(), actor.actorId())
                        .orElseThrow(() -> BusinessException.forbidden("Access denied"));
        if (current.getStatus() != UserStatus.ACTIVE
                || current.getAuthVersion() != actor.authVersion()
                || current.getPlatformRole() != actor.platformRole())
            throw BusinessException.forbidden("Access denied");
    }

    public void requireAdmin(ActorContext actor) {
        requireCurrent(actor);
        if (actor.platformRole() != PlatformRole.ADMIN)
            throw BusinessException.forbidden("Admin role required");
    }

    public void requireOrganizationRole(
            ActorContext actor, UUID organizationId, Set<OrganizationRole> allowed) {
        requireCurrent(actor);
        if (organizationId == null
                || allowed == null
                || allowed.isEmpty()
                || memberships == null
                || !memberships.allows(
                        TransactionContext.entityManager(),
                        actor.actorId(),
                        organizationId,
                        Set.copyOf(allowed)))
            throw BusinessException.forbidden("Organization role required");
    }
}
