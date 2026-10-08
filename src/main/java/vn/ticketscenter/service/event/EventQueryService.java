package vn.ticketscenter.service.event;

import java.util.UUID;
import java.util.List;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.event.EventDto;
import vn.ticketscenter.dto.event.EventFilter;
import vn.ticketscenter.dto.event.ZoneDto;
import vn.ticketscenter.dto.event.SeatDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface EventQueryService {
    Page<EventDto> listPublic(EventFilter filter, PageRequest page);
    EventDto getPublic(UUID eventId);
    List<ZoneDto> listPublicZones(UUID eventId);
    Page<SeatDto> listPublicSeats(UUID zoneId, PageRequest page);
    Page<EventDto> listForOrganization(ActorContext actor, UUID organizationId, EventFilter filter, PageRequest page);
    List<ZoneDto> listZones(ActorContext actor, UUID eventId);
    Page<SeatDto> listSeats(ActorContext actor, UUID zoneId, PageRequest page);
    Page<EventDto> listAdmin(ActorContext actor, EventFilter filter, PageRequest page);
    EventDto get(ActorContext actor, UUID eventId);
}
