package vn.ticketscenter.dto.settlement;
import java.math.BigDecimal;
import java.util.UUID;
/** No caller-supplied status/result: verification belongs to the backend. */
public record PayoutCommand(UUID payoutId, BigDecimal amount, String reference) { }
