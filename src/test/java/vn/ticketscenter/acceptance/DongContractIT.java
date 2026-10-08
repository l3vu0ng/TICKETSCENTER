package vn.ticketscenter.acceptance;

import jakarta.persistence.EntityManager;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.dto.event.*;
import vn.ticketscenter.dto.identity.*;
import vn.ticketscenter.dto.settlement.CommissionPolicyCommand;
import vn.ticketscenter.model.event.*;
import vn.ticketscenter.model.identity.*;
import vn.ticketscenter.model.fulfillment.Ticket;
import vn.ticketscenter.model.ticketing.TicketHoldItem;
import vn.ticketscenter.model.settlement.CommissionRule;
import vn.ticketscenter.repository.identity.MembershipAuthorizationRepository;
import vn.ticketscenter.service.event.*;
import vn.ticketscenter.service.identity.*;

import static org.junit.jupiter.api.Assertions.*;

/** API shape assertions; these do not replace SQL/HTTP integration evidence. */
class DongContractIT {
    @Test
    void dtoFieldCatalogIsStable() {
        components(OrganizationCommand.class, "name", "contactEmail", "contactPhone", "description");
        components(OrganizationDto.class, "id", "name", "contactEmail", "contactPhone", "description", "requesterId",
                "requesterUserName", "status", "rejectionReason", "createdAt", "submittedAt", "decidedAt");
        components(RequestFilter.class, "status", "keyword");
        components(MembershipCommand.class, "userName", "role");
        components(MembershipRoleCommand.class, "role");
        components(MemberFilter.class, "keyword", "role", "active");
        components(MembershipDto.class, "organizationId", "organizationName", "userId", "userName", "fullName", "email",
                "role", "active", "userStatus", "organizationStatus");
        components(MembershipAccess.class, "organizationId", "role", "active", "organizationStatus");
        components(EventCommand.class, "title", "description", "categoryId", "venueName", "venueAddress", "saleStart",
                "saleEnd", "startTime", "endTime");
        components(EventDto.class, "id", "organizationId", "organizationName", "title", "description", "categoryId",
                "categoryName", "venueName", "venueAddress", "coverImageUrl", "saleStart", "saleEnd", "startTime", "endTime",
                "status", "rejectionReason", "commissionRuleId", "minPrice", "saleActive", "serverNow");
        components(EventFilter.class, "keyword", "categoryId", "fromUtc", "toUtc", "status");
        components(ZoneCommand.class, "name", "type", "price", "rows", "seatsPerRow", "standingCapacity");
        components(ZonePriceCommand.class, "price");
        components(ZoneDto.class, "id", "eventId", "name", "type", "price", "capacity", "held", "sold", "available");
        components(SeatDto.class, "id", "zoneId", "rowName", "seatNumber", "status");
        components(EventCategoryDto.class, "id", "code", "name", "displayOrder");
        components(CoverDto.class, "eventId", "coverImageUrl");
    }

    @Test
    void peerServiceSignaturesAreStable() throws ReflectiveOperationException {
        method(OrganizationService.class, "approve", OrganizationDto.class,
                ActorContext.class, UUID.class, CommissionPolicyCommand.class);
        method(OrganizationService.class, "reject", OrganizationDto.class, ActorContext.class, UUID.class, String.class);
        method(OrganizationService.class, "listRequests", Page.class, ActorContext.class, RequestFilter.class, PageRequest.class);
        method(OrganizationService.class, "listOwnRequests", Page.class, ActorContext.class, PageRequest.class);
        method(MembershipService.class, "listForUser", Page.class, ActorContext.class, PageRequest.class);
        method(MembershipAuthorizationRepository.class, "findCurrent", Optional.class,
                EntityManager.class, UUID.class, UUID.class);
        method(EventService.class, "publish", EventDto.class, ActorContext.class, UUID.class, UUID.class);
        method(EventService.class, "reject", EventDto.class, ActorContext.class, UUID.class, String.class);
        method(EventQueryService.class, "listAdmin", Page.class, ActorContext.class, EventFilter.class, PageRequest.class);
        method(EventQueryService.class, "get", EventDto.class, ActorContext.class, UUID.class);
    }

    @Test
    void diagramMethodsAndPeerCallerTypesAreStable() throws ReflectiveOperationException {
        method(User.class, "assignRole", void.class, Organization.class, OrganizationRole.class);
        method(User.class, "revokeRole", void.class, Organization.class);
        method(Organization.class, "submitForApproval", void.class);
        method(Organization.class, "approve", void.class);
        method(Organization.class, "reject", void.class, String.class);
        method(Event.class, "updateDetails", void.class, EventDetails.class);
        method(Event.class, "configureSchedule", void.class, EventSchedule.class);
        method(Event.class, "addZone", void.class, Zone.class);
        method(Event.class, "removeZone", void.class, Zone.class);
        method(Event.class, "submitForApproval", void.class);
        method(Event.class, "publish", void.class, CommissionRule.class, Instant.class);
        method(Event.class, "reject", void.class, String.class);
        method(Event.class, "cancel", void.class, Instant.class);
        method(Event.class, "isSaleActive", boolean.class, Instant.class);
        method(Event.class, "isCheckInOpen", boolean.class, Instant.class);
        method(Zone.class, "changePrice", void.class, BigDecimal.class);
        method(Zone.class, "configureSeating", void.class, Integer.class, Integer.class);
        method(Zone.class, "setStandingCapacity", void.class, Integer.class);
        method(Zone.class, "holdStanding", void.class, Integer.class);
        method(Zone.class, "releaseHeldStanding", void.class, Integer.class);
        method(Zone.class, "sellHeldStanding", void.class, Integer.class);
        method(Zone.class, "returnSoldStanding", void.class, Integer.class);
        method(Zone.class, "getCapacity", Integer.class);
        method(Zone.class, "getAvailable", Integer.class);
        method(Seat.class, "hold", void.class, TicketHoldItem.class);
        method(Seat.class, "release", void.class, TicketHoldItem.class);
        method(Seat.class, "markSold", void.class, TicketHoldItem.class);
        method(Seat.class, "returnToInventory", void.class, Ticket.class);
        assertEquals(Organization.class, Event.class.getDeclaredField("organization").getType());
        assertEquals(CommissionRule.class, Event.class.getDeclaredField("commissionRule").getType());
        assertEquals(Event.class, Zone.class.getDeclaredField("event").getType());
        assertEquals(Zone.class, Seat.class.getDeclaredField("zone").getType());
        assertEquals(User.class, Organization.class.getDeclaredField("requester").getType());
        assertEquals(BigDecimal.class, Zone.class.getDeclaredField("price").getType());
        for (String name : List.of("standingCapacity", "standingHeld", "standingSold")) {
            assertEquals(Integer.class, Zone.class.getDeclaredField(name).getType());
        }
    }

    private static void components(Class<?> type, String... names) {
        RecordComponent[] components = type.getRecordComponents();
        assertNotNull(components, type.getName() + " must be a record");
        assertArrayEquals(names, Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new));
    }

    private static void method(Class<?> type, String name, Class<?> returnType, Class<?>... parameters)
            throws ReflectiveOperationException {
        assertEquals(returnType, type.getMethod(name, parameters).getReturnType(), type.getSimpleName() + "." + name);
    }
}
