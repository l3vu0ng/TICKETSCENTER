package vn.ticketscenter.persistence.settlement;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Historical row written by SP14/TX14 and frozen by SP15/TX15. */
@Entity @Immutable @Table(name = "SettlementOrderSnapshot", schema = "dbo")
public class SettlementOrderSnapshotRecord {
    @EmbeddedId private Key id;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal grossAmount;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal refundAmount;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal commissionAmount;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal netAmount;
    protected SettlementOrderSnapshotRecord() { }
    public Key getId() { return id; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public BigDecimal getNetAmount() { return netAmount; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(nullable = false, columnDefinition = "uniqueidentifier") private UUID settlementId;
        @Column(nullable = false, columnDefinition = "uniqueidentifier") private UUID orderId;
        protected Key() { }
        public Key(UUID settlementId, UUID orderId) {
            this.settlementId = Objects.requireNonNull(settlementId);
            this.orderId = Objects.requireNonNull(orderId);
        }
        public UUID getSettlementId() { return settlementId; }
        public UUID getOrderId() { return orderId; }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(settlementId, key.settlementId)
                    && Objects.equals(orderId, key.orderId);
        }
        @Override public int hashCode() { return Objects.hash(settlementId, orderId); }
    }
}
