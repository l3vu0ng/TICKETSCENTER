package vn.ticketscenter.persistence.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;
import vn.ticketscenter.model.settlement.TransferStatus;

/** Read mapping; reservation/finalization and replay checks are owned by SP16. */
@Entity
@Immutable
@Table(name = "SettlementTransferLog", schema = "dbo")
public class SettlementTransferLogRecord {
    @Id
    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID payoutId;

    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID settlementId;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    @Nationalized
    @Column(length = 255)
    private String reference;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, columnDefinition = "datetime2(7)")
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(columnDefinition = "datetime2(7)")
    private Instant completedAt;

    protected SettlementTransferLogRecord() {}

    public UUID getPayoutId() {
        return payoutId;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
