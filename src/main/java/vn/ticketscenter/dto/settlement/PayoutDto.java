package vn.ticketscenter.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.model.settlement.TransferStatus;

public record PayoutDto(
        UUID payoutId,
        UUID settlementId,
        BigDecimal amount,
        TransferStatus status,
        String reference,
        Instant paidAt) {}
