package vn.ticketscenter.service.identity;

import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.identity.MembershipCommand;
import vn.ticketscenter.dto.identity.MembershipRoleCommand;
import vn.ticketscenter.dto.identity.MemberFilter;
import vn.ticketscenter.dto.identity.MembershipDto;

/** M0 API declaration; implementations follow the owning DONG use-case tasks. */
public interface MembershipService {
    Page<MembershipDto> listForUser(ActorContext actor, PageRequest page);
    Page<MembershipDto> listMembers(ActorContext actor, UUID organizationId, MemberFilter filter, PageRequest page);
    MembershipDto add(ActorContext actor, UUID organizationId, MembershipCommand command);
    MembershipDto changeRole(ActorContext actor, UUID organizationId, UUID userId, MembershipRoleCommand command);
    MembershipDto deactivate(ActorContext actor, UUID organizationId, UUID userId);
    MembershipDto activate(ActorContext actor, UUID organizationId, UUID userId);
}
