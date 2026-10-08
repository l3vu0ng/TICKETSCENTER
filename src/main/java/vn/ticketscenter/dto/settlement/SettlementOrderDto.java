package vn.ticketscenter.dto.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementOrderDto(
        UUID orderId,
        BigDecimal grossAmount,
        BigDecimal refundAmount,
        BigDecimal commissionAmount,
        BigDecimal netAmount) {}
