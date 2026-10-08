package vn.ticketscenter.repository.identity;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import vn.ticketscenter.dto.identity.MembershipAccess;

public interface MembershipAuthorizationRepository {
    Optional<MembershipAccess> findCurrent(EntityManager em, UUID userId, UUID organizationId);
}
