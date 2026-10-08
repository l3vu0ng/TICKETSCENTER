package vn.ticketscenter.dto.identity;

import java.util.UUID;
import vn.ticketscenter.model.identity.OrganizationStatus;
import vn.ticketscenter.model.identity.OrganizationRole;

public record MembershipAccess(
        UUID organizationId,
        OrganizationRole role,
        boolean active,
        OrganizationStatus organizationStatus) {
}
