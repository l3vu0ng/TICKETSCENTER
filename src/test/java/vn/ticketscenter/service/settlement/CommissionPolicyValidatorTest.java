package vn.ticketscenter.service.settlement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.ticketscenter.dto.settlement.CommissionPolicyCommand;
import vn.ticketscenter.exception.BusinessException;

class CommissionPolicyValidatorTest {
    private static final Instant FROM = Instant.parse("2026-10-06T03:00:00Z");
    private static final Instant TO = Instant.parse("2027-01-01T00:00:00Z");
    private final CommissionPolicyValidator validator = new CommissionPolicyValidator();

    @ParameterizedTest(name = "accepts {0}")
    @MethodSource("validPolicies")
    void acceptsRepresentableNonnegativeTermsWithoutAddingABusinessRateCap(
            String scenario, CommissionPolicyCommand command) {
        assertDoesNotThrow(() -> validator.validate(command));
    }

    static Stream<Arguments> validPolicies() {
        return Stream.of(
                Arguments.of("zero rate and fee", policy("0", "0", FROM, TO)),
                Arguments.of("ordinary initial policy", policy("10", "10000", FROM, TO)),
                Arguments.of("smallest rate unit", policy("0.000001", "1", FROM, TO)),
                Arguments.of(
                        "insignificant trailing zeros",
                        policy("2.500000000", "10000.000", FROM, TO)),
                Arguments.of("rate above 100 has no business cap", policy("250", "100", FROM, TO)),
                Arguments.of(
                        "SQL numeric upper boundaries",
                        policy("9999999999999.999999", "9999999999999999999", FROM, TO)),
                Arguments.of(
                        "exact numeric values with exponent notation",
                        policy("1E+3", "1E+3", FROM, TO)));
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("invalidPolicies")
    void rejectsInvalidPolicyBeforeDatabaseWork(String scenario, CommissionPolicyCommand command) {
        BusinessException error =
                assertThrows(BusinessException.class, () -> validator.validate(command));
        assertEquals(400, error.getHttpStatus());
        assertEquals("INVALID_COMMISSION_POLICY", error.getCode());
    }

    static Stream<Arguments> invalidPolicies() {
        return Stream.of(
                Arguments.of("null command", null),
                Arguments.of("missing rate", policy(null, "10000", FROM, TO)),
                Arguments.of("missing fee", policy("10", null, FROM, TO)),
                Arguments.of("negative rate", policy("-0.000001", "0", FROM, TO)),
                Arguments.of("negative fee", policy("0", "-1", FROM, TO)),
                Arguments.of("fractional VND fee", policy("10", "0.1", FROM, TO)),
                Arguments.of(
                        "money over decimal(19,0)", policy("10", "10000000000000000000", FROM, TO)),
                Arguments.of("rate over decimal(19,6)", policy("10000000000000", "0", FROM, TO)),
                Arguments.of("rate needing rounding", policy("0.0000001", "0", FROM, TO)),
                Arguments.of(
                        "rate with nonzero seventh decimal", policy("2.5000001", "0", FROM, TO)),
                Arguments.of("missing effectiveFrom", policy("10", "10000", null, TO)),
                Arguments.of("missing effectiveTo", policy("10", "10000", FROM, null)),
                Arguments.of("empty interval", policy("10", "10000", FROM, FROM)),
                Arguments.of("reversed interval", policy("10", "10000", TO, FROM)));
    }

    private static CommissionPolicyCommand policy(
            String rate, String fee, Instant from, Instant to) {
        return new CommissionPolicyCommand(
                rate == null ? null : new BigDecimal(rate),
                fee == null ? null : new BigDecimal(fee),
                from,
                to);
    }
}
