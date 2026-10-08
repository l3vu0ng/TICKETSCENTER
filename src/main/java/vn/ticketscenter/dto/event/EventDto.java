package vn.ticketscenter.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.model.event.EventStatus;

public record EventDto(
        UUID id,
        UUID organizationId,
        String organizationName,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        String venueName,
        String venueAddress,
        String coverImageUrl,
        Instant saleStart,
        Instant saleEnd,
        Instant startTime,
        Instant endTime,
        EventStatus status,
        String rejectionReason,
        UUID commissionRuleId,
        BigDecimal minPrice,
        boolean saleActive,
        Instant serverNow) {
}
