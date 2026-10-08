package vn.ticketscenter.dto.identity;

import vn.ticketscenter.model.identity.OrganizationRole;

public record MembershipRoleCommand(
        OrganizationRole role) {
}
