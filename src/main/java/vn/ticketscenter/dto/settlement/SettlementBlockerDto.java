package vn.ticketscenter.dto.settlement;

import java.util.UUID;

public record SettlementBlockerDto(String type, UUID objectId, String message, String href) {}
