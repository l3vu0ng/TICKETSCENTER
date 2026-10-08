package vn.ticketscenter.service.event;

import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.event.EventCommand;
import vn.ticketscenter.dto.event.EventDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface EventService {
    EventDto create(ActorContext actor, UUID organizationId, EventCommand command);
    EventDto edit(ActorContext actor, UUID eventId, EventCommand command);
    void deleteDraft(ActorContext actor, UUID eventId);
    EventDto submit(ActorContext actor, UUID eventId);
    EventDto publish(ActorContext actor, UUID eventId, UUID commissionRuleId);
    EventDto reject(ActorContext actor, UUID eventId, String reason);
}
