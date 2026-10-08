package vn.ticketscenter.dto.identity;

import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.model.identity.OrganizationStatus;

public record OrganizationDto(
        UUID id,
        String name,
        String contactEmail,
        String contactPhone,
        String description,
        UUID requesterId,
        String requesterUserName,
        OrganizationStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant submittedAt,
        Instant decidedAt) {
}
