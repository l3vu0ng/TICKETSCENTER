package vn.ticketscenter.dto.identity;

import java.util.UUID;
import vn.ticketscenter.model.identity.OrganizationStatus;
import vn.ticketscenter.model.identity.OrganizationRole;
import vn.ticketscenter.model.identity.UserStatus;

public record MembershipDto(
        UUID organizationId,
        String organizationName,
        UUID userId,
        String userName,
        String fullName,
        String email,
        OrganizationRole role,
        boolean active,
        UserStatus userStatus,
        OrganizationStatus organizationStatus) {
}
