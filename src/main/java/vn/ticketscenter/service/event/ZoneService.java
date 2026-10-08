package vn.ticketscenter.service.event;

import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.event.ZoneCommand;
import vn.ticketscenter.dto.event.ZonePriceCommand;
import vn.ticketscenter.dto.event.ZoneDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface ZoneService {
    ZoneDto create(ActorContext actor, UUID eventId, ZoneCommand command);
    ZoneDto edit(ActorContext actor, UUID zoneId, ZoneCommand command);
    ZoneDto changePrice(ActorContext actor, UUID zoneId, ZonePriceCommand command);
    void deleteDraft(ActorContext actor, UUID zoneId);
}
