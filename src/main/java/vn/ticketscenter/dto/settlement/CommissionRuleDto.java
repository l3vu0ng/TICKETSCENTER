package vn.ticketscenter.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionRuleDto(
        UUID id,
        UUID organizationId,
        BigDecimal ratePercent,
        BigDecimal fixedFee,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean applied) {}
