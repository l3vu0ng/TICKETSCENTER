package vn.ticketscenter.dto.event;

import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.model.event.EventStatus;

public record EventFilter(
        String keyword,
        UUID categoryId,
        Instant fromUtc,
        Instant toUtc,
        EventStatus status) {
}
