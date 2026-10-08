package vn.ticketscenter.service.settlement;

import java.time.Instant;
import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.settlement.*;

/** M0 contract; implementation and authorization are VUONG-04 (M1). */
public interface CommissionService {
    CommissionRuleDto getEffective(
            ActorContext actor, UUID organizationId, UUID ruleId, Instant now);

    CommissionRuleDto create(
            ActorContext actor, UUID organizationId, CommissionPolicyCommand command);

    CommissionRuleDto editUnapplied(
            ActorContext actor, UUID ruleId, CommissionPolicyCommand command);

    Page<CommissionRuleDto> list(ActorContext actor, UUID organizationId, PageRequest page);
}
