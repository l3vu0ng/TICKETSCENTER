package vn.ticketscenter.model.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.ticketscenter.dto.settlement.CommissionPolicyCommand;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.identity.Organization;
import vn.ticketscenter.service.settlement.CommissionPolicyValidator;

@Entity
@Table(name = "CommissionRule", schema = "dbo")
public class CommissionRule {
    @Id
    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizationId", nullable = false)
    private Organization organization;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal ratePercent;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal fixedFee;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, columnDefinition = "datetime2(7)")
    private Instant effectiveFrom;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, columnDefinition = "datetime2(7)")
    private Instant effectiveTo;

    @Version private int version;

    protected CommissionRule() {}

    public CommissionRule(
            UUID id,
            Organization organization,
            BigDecimal ratePercent,
            BigDecimal fixedFee,
            Instant effectiveFrom,
            Instant effectiveTo) {
        if (id == null || organization == null) {
            throw BusinessException.badRequest(
                    "INVALID_COMMISSION_POLICY", "Thiếu ID hoặc tổ chức của chính sách phí.");
        }
        new CommissionPolicyValidator()
                .validate(
                        new CommissionPolicyCommand(
                                ratePercent, fixedFee, effectiveFrom, effectiveTo));
        this.id = id;
        this.organization = organization;
        this.ratePercent = ratePercent.setScale(6);
        this.fixedFee = fixedFee.setScale(0);
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public boolean isEffectiveAt(Instant now) {
        if (now == null)
            throw BusinessException.badRequest(
                    "INVALID_TIME", "Thiếu thời điểm kiểm tra chính sách.");
        return !now.isBefore(effectiveFrom) && now.isBefore(effectiveTo);
    }

    public BigDecimal calculateFee(BigDecimal remainingAmount) {
        BigDecimal remaining = FinancialAmounts.money(remainingAmount);
        if (remaining.signum() == 0) return BigDecimal.ZERO;
        return remaining
                .multiply(ratePercent)
                .movePointLeft(2)
                .add(fixedFee)
                .setScale(0, RoundingMode.HALF_UP)
                .min(remaining);
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public BigDecimal getFixedFee() {
        return fixedFee;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public int getVersion() {
        return version;
    }
}
