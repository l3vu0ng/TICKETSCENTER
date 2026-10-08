package vn.ticketscenter.repository.identity;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import vn.ticketscenter.dto.identity.MembershipAccess;

/** Uses the caller's EntityManager without opening a connection or transaction. */
public final class JpaMembershipAuthorizationRepository implements MembershipAuthorizationRepository {
    @Override
    public Optional<MembershipAccess> findCurrent(EntityManager em, UUID userId, UUID organizationId) {
        Objects.requireNonNull(em, "em");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(organizationId, "organizationId");
        List<MembershipAccess> matches = em.createQuery("""
                select new vn.ticketscenter.dto.identity.MembershipAccess(
                    membership.organization.id, membership.role, membership.active, membership.organization.status)
                from UserOrganizationRole membership
                where membership.user.id = :userId and membership.organization.id = :organizationId
                """, MembershipAccess.class)
                .setParameter("userId", userId)
                .setParameter("organizationId", organizationId)
                .getResultList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Membership pair is not unique; verify constraint C02");
        }
        // Inactive links must remain visible as active=false; User.status is checked by Khánh.
        return matches.stream().findFirst();
    }
}
