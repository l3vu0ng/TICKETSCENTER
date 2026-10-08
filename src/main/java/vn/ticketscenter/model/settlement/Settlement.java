package vn.ticketscenter.model.settlement;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.event.Event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Financial aggregate. The service owns source verification, event/blocker checks,
 * locking and payoutId/amount/replay checks in the persistent transfer log. */
@Entity
@Table(name = "Settlement", schema = "dbo", uniqueConstraints =
        @UniqueConstraint(name = "UQ_Settlement_Event", columnNames = "eventId"))
public class Settlement {
    @Id @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eventId", nullable = false, unique = true)
    private Event event;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private SettlementStatus status = SettlementStatus.DRAFT;
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal totalRefund = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal totalCommission = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(columnDefinition = "datetime2(7)")
    private Instant confirmedAt;
    @Version private int version;

    protected Settlement() { }

    public Settlement(UUID id, Event event) {
        if (id == null || event == null) throw BusinessException.badRequest("INVALID_SETTLEMENT", "Thiếu ID hoặc sự kiện đối soát.");
        this.id = id;
        this.event = event;
    }

    public void recalculate(BigDecimal grossRevenue, BigDecimal totalRefund, BigDecimal totalCommission) {
        require(SettlementStatus.DRAFT);
        BigDecimal gross = FinancialAmounts.money(grossRevenue);
        BigDecimal refund = FinancialAmounts.money(totalRefund);
        BigDecimal fee = FinancialAmounts.money(totalCommission);
        if (refund.add(fee).compareTo(gross) > 0) throw conflict("Tổng hoàn và phí vượt doanh thu.");
        this.grossRevenue = gross;
        this.totalRefund = refund;
        this.totalCommission = fee;
    }

    /** Caller must verify Event endTime and unresolved obligations inside TX15. */
    public void confirm(Instant now) {
        require(SettlementStatus.DRAFT);
        if (now == null) throw BusinessException.badRequest("INVALID_TIME", "Thiếu thời điểm chốt đối soát.");
        confirmedAt = now;
        status = getNetPayable().signum() == 0 ? SettlementStatus.PAID : SettlementStatus.CONFIRMED;
    }

    /** Invoke only after checking a new payoutId in the locked transfer log. */
    public void beginPayout(UUID payoutId, BigDecimal amount) {
        require(SettlementStatus.CONFIRMED);
        BigDecimal money = payoutAmount(payoutId, amount);
        if (money.compareTo(getAvailableToPay()) > 0) throw conflict("Khoản chi vượt số dư khả dụng.");
        pendingAmount = pendingAmount.add(money);
    }

    /** Invoke once after the service verifies a matching PENDING log and final result.
     * The reference belongs to that technical log, not to this aggregate. */
    public void recordPayoutResult(UUID payoutId, BigDecimal amount, PayoutResult result, String reference) {
        require(SettlementStatus.CONFIRMED);
        BigDecimal money = payoutAmount(payoutId, amount);
        if (result == null || (reference != null && reference.length() > 255)) {
            throw BusinessException.badRequest("INVALID_PAYOUT_RESULT", "Thiếu kết quả cuối hoặc tham chiếu quá dài.");
        }
        if (money.compareTo(pendingAmount) > 0) throw conflict("Khoản chi chưa được giữ trong số dư chờ.");
        pendingAmount = pendingAmount.subtract(money);
        if (result == PayoutResult.SUCCEEDED) paidAmount = paidAmount.add(money);
        if (paidAmount.compareTo(getNetPayable()) == 0 && pendingAmount.signum() == 0) status = SettlementStatus.PAID;
    }

    private BigDecimal payoutAmount(UUID payoutId, BigDecimal amount) {
        if (payoutId == null) throw BusinessException.badRequest("INVALID_PAYOUT", "Thiếu ID lần chi.");
        BigDecimal money = FinancialAmounts.money(amount);
        if (money.signum() == 0) throw BusinessException.badRequest("INVALID_PAYOUT", "Khoản chi phải lớn hơn 0.");
        return money;
    }

    private void require(SettlementStatus expected) {
        if (status != expected) throw conflict("Trạng thái đối soát không cho phép thao tác này.");
    }
    private static BusinessException conflict(String message) {
        return BusinessException.conflict("SETTLEMENT_CONFLICT", message);
    }

    @Transient public BigDecimal getNetPayable() { return grossRevenue.subtract(totalRefund).subtract(totalCommission); }
    @Transient public BigDecimal getAvailableToPay() { return getNetPayable().subtract(paidAmount).subtract(pendingAmount); }
    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public SettlementStatus getStatus() { return status; }
    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public BigDecimal getTotalRefund() { return totalRefund; }
    public BigDecimal getTotalCommission() { return totalCommission; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public int getVersion() { return version; }
}
