package vn.ticketscenter.dto.event;

import java.util.UUID;

public record CoverDto(
        UUID eventId,
        String coverImageUrl) {
}
