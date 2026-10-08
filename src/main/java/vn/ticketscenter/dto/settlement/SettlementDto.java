package vn.ticketscenter.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.model.settlement.SettlementStatus;

public record SettlementDto(
        UUID id,
        UUID eventId,
        SettlementStatus status,
        BigDecimal grossRevenue,
        BigDecimal totalRefund,
        BigDecimal totalCommission,
        BigDecimal netPayable,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        BigDecimal availableToPay,
        Instant confirmedAt,
        Page<SettlementOrderDto> orders) {}
