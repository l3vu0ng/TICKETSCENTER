package vn.ticketscenter.service.identity;

import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.identity.OrganizationCommand;
import vn.ticketscenter.dto.identity.OrganizationDto;
import vn.ticketscenter.dto.identity.RequestFilter;
import vn.ticketscenter.dto.settlement.CommissionPolicyCommand;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface OrganizationService {
    OrganizationDto submit(ActorContext actor, OrganizationCommand command);
    OrganizationDto get(ActorContext actor, UUID organizationId);
    OrganizationDto approve(ActorContext actor, UUID organizationId, CommissionPolicyCommand initialPolicy);
    OrganizationDto reject(ActorContext actor, UUID organizationId, String reason);
    Page<OrganizationDto> listRequests(ActorContext actor, RequestFilter filter, PageRequest page);
    Page<OrganizationDto> listOwnRequests(ActorContext actor, PageRequest page);
}
