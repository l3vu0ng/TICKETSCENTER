package vn.ticketscenter.dto.event;

import java.math.BigDecimal;

public record ZonePriceCommand(
        BigDecimal price) {
}
