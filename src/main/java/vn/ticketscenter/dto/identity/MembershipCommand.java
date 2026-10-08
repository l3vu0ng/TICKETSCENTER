package vn.ticketscenter.dto.identity;

import vn.ticketscenter.model.identity.OrganizationRole;

public record MembershipCommand(
        String userName,
        OrganizationRole role) {
}
