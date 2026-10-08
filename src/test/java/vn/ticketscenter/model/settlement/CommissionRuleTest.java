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
import vn.ticketscenter.model.identity.Organization;

class CommissionRuleTest {
    private static final Instant FROM = Instant.parse("2026-10-08T00:00:00Z");
    private static final Instant TO = FROM.plusSeconds(3600);

    private CommissionRule rule(String rate, String fee) {
        return new CommissionRule(
                UUID.randomUUID(),
                emptyJpaEntity(Organization.class),
                new BigDecimal(rate),
                new BigDecimal(fee),
                FROM,
                TO);
    }

    @ParameterizedTest
    @CsvSource({
        "200001,2.5,0,5000",
        "20,10,100,20",
        "0,10,100,0",
        "25,10,0,3",
        "30000,0,50000,30000",
        "100,250,0,100",
        "9999999999999999999,0,0,0"
    })
    void calculatesRoundedCappedFee(String remaining, String rate, String fixed, String expected) {
        assertEquals(
                new BigDecimal(expected),
                rule(rate, fixed).calculateFee(new BigDecimal(remaining)));
    }

    @Test
    void usesHalfOpenEffectiveInterval() {
        CommissionRule rule = rule("10", "0");
        assertFalse(rule.isEffectiveAt(FROM.minusNanos(1)));
        assertTrue(rule.isEffectiveAt(FROM));
        assertTrue(rule.isEffectiveAt(TO.minusNanos(1)));
        assertFalse(rule.isEffectiveAt(TO));
        assertThrows(BusinessException.class, () -> rule.isEffectiveAt(null));
    }

    @ParameterizedTest
    @CsvSource({"-1", "0.1", "10000000000000000000"})
    void rejectsUnrepresentableRemaining(String amount) {
        assertThrows(
                BusinessException.class,
                () -> rule("10", "0").calculateFee(new BigDecimal(amount)));
    }

    @Test
    void rejectsMissingInputAndInvalidTerms() {
        assertThrows(BusinessException.class, () -> rule("10", "0").calculateFee(null));
        assertThrows(BusinessException.class, () -> rule("-1", "0"));
        assertThrows(BusinessException.class, () -> rule("10", "0.1"));
        assertThrows(
                BusinessException.class,
                () ->
                        new CommissionRule(
                                UUID.randomUUID(),
                                emptyJpaEntity(Organization.class),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                TO,
                                FROM));
        assertThrows(
                BusinessException.class,
                () ->
                        new CommissionRule(
                                UUID.randomUUID(),
                                null,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                FROM,
                                TO));
    }
}
