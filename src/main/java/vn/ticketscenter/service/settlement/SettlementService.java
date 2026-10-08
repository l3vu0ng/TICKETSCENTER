package vn.ticketscenter.service.settlement;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.settlement.*;
import java.util.List;
import java.util.UUID;
/** M0 contract; source verification, transaction and payout replay logic ship in M3. */
public interface SettlementService {
    SettlementDto get(ActorContext actor, UUID eventId, PageRequest page);
    List<SettlementBlockerDto> blockers(ActorContext actor, UUID eventId);
    SettlementDto recalculate(ActorContext actor, UUID eventId);
    SettlementDto confirm(ActorContext actor, UUID settlementId);
    PayoutDto payout(ActorContext actor, UUID settlementId, PayoutCommand command);
    Page<PayoutDto> listPayouts(ActorContext actor, UUID settlementId, PageRequest page);
}
