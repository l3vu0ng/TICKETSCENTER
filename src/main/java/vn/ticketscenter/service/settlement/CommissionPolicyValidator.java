package vn.ticketscenter.service.settlement;

import vn.ticketscenter.dto.settlement.CommissionPolicyCommand;
import vn.ticketscenter.exception.BusinessException;

import java.math.BigDecimal;

/** Validates terms before SP01 stores them in the organization approval transaction. */
public final class CommissionPolicyValidator {
    private static final BigDecimal MAX_RATE = new BigDecimal("9999999999999.999999");
    private static final BigDecimal MAX_FEE = new BigDecimal("9999999999999999999");

    public void validate(CommissionPolicyCommand command) {
        if (command == null) {
            throw invalid("Thiếu chính sách phí ban đầu.");
        }
        validateDecimal(command.ratePercent(), MAX_RATE, 6, "ratePercent");
        validateDecimal(command.fixedFee(), MAX_FEE, 0, "fixedFee");
        if (command.effectiveFrom() == null || command.effectiveTo() == null
                || !command.effectiveFrom().isBefore(command.effectiveTo())) {
            throw invalid("Khoảng hiệu lực phải có ngày bắt đầu trước ngày kết thúc.");
        }
    }

    private static void validateDecimal(BigDecimal value, BigDecimal maximum, int scale, String field) {
        // Check magnitude first; never round caller values to make them fit SQL.
        if (value == null || value.signum() < 0 || value.compareTo(maximum) > 0
                || value.stripTrailingZeros().scale() > scale) {
            throw invalid(field + " phải không âm và biểu diễn chính xác trong decimal(19," + scale + ").");
        }
    }

    private static BusinessException invalid(String message) {
        return BusinessException.badRequest("INVALID_COMMISSION_POLICY", message);
    }
}
