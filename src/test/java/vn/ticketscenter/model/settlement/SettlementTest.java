package vn.ticketscenter.model.settlement;

import static org.junit.jupiter.api.Assertions.*;
import static vn.ticketscenter.support.ModelFixtures.emptyJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.event.Event;

class SettlementTest {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    private Settlement draft() {
        return new Settlement(UUID.randomUUID(), emptyJpaEntity(Event.class));
    }

    private Settlement confirmed() {
        Settlement value = draft();
        value.recalculate(money("500000"), money("100000"), money("50000"));
        value.confirm(NOW);
        return value;
    }

    @Test
    void derivesBalancesAndFreezesRevenueAtConfirmation() {
        Settlement value = confirmed();
        assertEquals(money("350000"), value.getNetPayable());
        assertEquals(money("350000"), value.getAvailableToPay());
        assertEquals(SettlementStatus.CONFIRMED, value.getStatus());
        assertEquals(NOW, value.getConfirmedAt());
        assertThrows(
                BusinessException.class,
                () -> value.recalculate(money("1"), money("0"), money("0")));
        assertThrows(BusinessException.class, () -> value.confirm(NOW.plusSeconds(1)));
        assertEquals(money("500000"), value.getGrossRevenue());
        assertEquals(NOW, value.getConfirmedAt());
    }

    @Test
    void completesZeroNetWithoutFabricatedPayout() {
        Settlement value = draft();
        value.recalculate(money("10"), money("10"), money("0"));
        value.confirm(NOW);
        assertEquals(SettlementStatus.PAID, value.getStatus());
        assertEquals(BigDecimal.ZERO, value.getPaidAmount());
        assertEquals(BigDecimal.ZERO, value.getPendingAmount());
        assertThrows(
                BusinessException.class, () -> value.beginPayout(UUID.randomUUID(), money("1")));
    }

    @ParameterizedTest
    @CsvSource({
        "-1,0,0",
        "100,101,0",
        "100,0,101",
        "100,60,41",
        "0.1,0,0",
        "10000000000000000000,0,0"
    })
    void invalidRecalculationLeavesAllTotalsIntact(String gross, String refund, String fee) {
        Settlement value = draft();
        value.recalculate(money("100"), money("20"), money("10"));
        assertThrows(
                BusinessException.class,
                () -> value.recalculate(money(gross), money(refund), money(fee)));
        assertEquals(money("100"), value.getGrossRevenue());
        assertEquals(money("20"), value.getTotalRefund());
        assertEquals(money("10"), value.getTotalCommission());
        assertEquals(money("70"), value.getAvailableToPay());
    }

    @Test
    void reservesConcurrentAmountsAndReleasesFailureBeforeNewAttempt() {
        Settlement value = confirmed();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        value.beginPayout(first, money("100000"));
        value.beginPayout(second, money("250000"));
        assertEquals(money("350000"), value.getPendingAmount());
        assertEquals(money("0"), value.getAvailableToPay());
        assertThrows(
                BusinessException.class, () -> value.beginPayout(UUID.randomUUID(), money("1")));
        value.recordPayoutResult(first, money("100000"), PayoutResult.FAILED, null);
        assertEquals(money("100000"), value.getAvailableToPay());
        value.recordPayoutResult(second, money("250000"), PayoutResult.SUCCEEDED, "verified-2");
        assertEquals(SettlementStatus.CONFIRMED, value.getStatus());
        UUID retry = UUID.randomUUID();
        value.beginPayout(retry, money("100000"));
        value.recordPayoutResult(retry, money("100000"), PayoutResult.SUCCEEDED, null);
        assertEquals(SettlementStatus.PAID, value.getStatus());
        assertEquals(money("350000"), value.getPaidAmount());
        assertEquals(money("0"), value.getPendingAmount());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "0.1", "350001"})
    void invalidReservationDoesNotChangeBalance(String amount) {
        Settlement value = confirmed();
        assertThrows(
                BusinessException.class, () -> value.beginPayout(UUID.randomUUID(), money(amount)));
        assertEquals(BigDecimal.ZERO, value.getPendingAmount());
        assertEquals(money("350000"), value.getAvailableToPay());
    }

    @Test
    void rejectsMissingIdentityUnreservedResultsAndInvalidInputAtomically() {
        Settlement value = confirmed();
        UUID id = UUID.randomUUID();
        assertThrows(BusinessException.class, () -> value.beginPayout(null, money("1")));
        assertThrows(BusinessException.class, () -> value.beginPayout(id, null));
        assertThrows(
                BusinessException.class,
                () -> value.recordPayoutResult(id, money("1"), PayoutResult.SUCCEEDED, null));
        value.beginPayout(id, money("10"));
        assertThrows(
                BusinessException.class,
                () -> value.recordPayoutResult(id, money("11"), PayoutResult.SUCCEEDED, null));
        assertThrows(
                BusinessException.class,
                () -> value.recordPayoutResult(id, money("10"), null, null));
        assertThrows(
                BusinessException.class,
                () ->
                        value.recordPayoutResult(
                                id, money("10"), PayoutResult.SUCCEEDED, "x".repeat(256)));
        assertEquals(money("10"), value.getPendingAmount());
        assertEquals(BigDecimal.ZERO, value.getPaidAmount());
        Settlement draft = draft();
        assertThrows(BusinessException.class, () -> draft.beginPayout(id, money("1")));
        assertThrows(BusinessException.class, () -> draft.confirm(null));
        assertThrows(
                BusinessException.class, () -> draft.recalculate(null, money("0"), money("0")));
        assertEquals(SettlementStatus.DRAFT, draft.getStatus());
    }
}
