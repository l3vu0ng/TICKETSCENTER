package vn.ticketscenter.dto.identity;

import vn.ticketscenter.model.identity.OrganizationStatus;

public record RequestFilter(
        OrganizationStatus status,
        String keyword) {
}
