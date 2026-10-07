package vn.ticketscenter.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;

/** Initial commission terms supplied to organization approval in its transaction. */
public record CommissionPolicyCommand(
        BigDecimal ratePercent,
        BigDecimal fixedFee,
        Instant effectiveFrom,
        Instant effectiveTo) {
}
