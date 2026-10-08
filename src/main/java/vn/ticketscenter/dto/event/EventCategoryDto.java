package vn.ticketscenter.dto.event;

import java.util.UUID;

public record EventCategoryDto(
        UUID id,
        String code,
        String name,
        int displayOrder) {
}
