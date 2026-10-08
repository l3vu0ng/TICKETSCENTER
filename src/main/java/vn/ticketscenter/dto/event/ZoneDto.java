package vn.ticketscenter.dto.event;

import java.math.BigDecimal;
import java.util.UUID;
import vn.ticketscenter.model.event.ZoneType;

public record ZoneDto(
        UUID id,
        UUID eventId,
        String name,
        ZoneType type,
        BigDecimal price,
        int capacity,
        int held,
        int sold,
        int available) {
}
