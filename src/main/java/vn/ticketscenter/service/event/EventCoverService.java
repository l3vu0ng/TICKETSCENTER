package vn.ticketscenter.service.event;

import java.util.UUID;
import java.io.InputStream;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.event.CoverDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface EventCoverService {
    CoverDto replace(ActorContext actor, UUID eventId, InputStream content, long declaredSize);
}
