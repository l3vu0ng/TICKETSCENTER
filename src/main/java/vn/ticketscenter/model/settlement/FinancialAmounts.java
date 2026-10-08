package vn.ticketscenter.model.settlement;

import java.math.BigDecimal;
import vn.ticketscenter.exception.BusinessException;

/** Exact SQL decimal(19,0) validation shared by the two financial models. */
final class FinancialAmounts {
    private static final BigDecimal MAX = new BigDecimal("9999999999999999999");

    private FinancialAmounts() {}

    static BigDecimal money(BigDecimal value) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(MAX) > 0
                || value.stripTrailingZeros().scale() > 0) {
            throw BusinessException.badRequest(
                    "INVALID_MONEY", "Số tiền phải là đồng nguyên không âm trong decimal(19,0).");
        }
        return value.setScale(0);
    }
}
