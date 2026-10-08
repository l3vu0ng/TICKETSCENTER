package vn.ticketscenter.dto.event;

import java.util.UUID;
import vn.ticketscenter.model.event.SeatStatus;

public record SeatDto(
        UUID id,
        UUID zoneId,
        String rowName,
        int seatNumber,
        SeatStatus status) {
}
