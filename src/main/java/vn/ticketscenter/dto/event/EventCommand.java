package vn.ticketscenter.dto.event;

import java.time.Instant;
import java.util.UUID;

public record EventCommand(
        String title,
        String description,
        UUID categoryId,
        String venueName,
        String venueAddress,
        Instant saleStart,
        Instant saleEnd,
        Instant startTime,
        Instant endTime) {
}
