package vn.ticketscenter.persistence.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;
import vn.ticketscenter.model.settlement.PayoutResult;

/** Durable simulated provider ledger. Never a real bank transfer entity. */
@Entity
@Immutable
@Table(name = "MockPayoutProviderLedger", schema = "dbo")
public class MockPayoutProviderRecord {
    @Id
    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID payoutId;

    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID settlementId;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutResult status;

    @Nationalized
    @Column(length = 255)
    private String reference;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, columnDefinition = "datetime2(7)")
    private Instant acceptedAt;

    protected MockPayoutProviderRecord() {}

    public UUID getPayoutId() {
        return payoutId;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PayoutResult getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
