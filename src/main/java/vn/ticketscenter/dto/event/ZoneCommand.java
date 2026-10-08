package vn.ticketscenter.dto.event;

import java.math.BigDecimal;
import vn.ticketscenter.model.event.ZoneType;

public record ZoneCommand(
        String name,
        ZoneType type,
        BigDecimal price,
        Integer rows,
        Integer seatsPerRow,
        Integer standingCapacity) {
}
