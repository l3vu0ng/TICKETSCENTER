package vn.ticketscenter.service.event;

import java.util.List;
import vn.ticketscenter.dto.event.EventCategoryDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface EventCategoryService {
    List<EventCategoryDto> listActive();
}
