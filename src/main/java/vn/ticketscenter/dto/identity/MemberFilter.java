package vn.ticketscenter.dto.identity;

import vn.ticketscenter.model.identity.OrganizationRole;

public record MemberFilter(
        String keyword,
        OrganizationRole role,
        Boolean active) {
}
