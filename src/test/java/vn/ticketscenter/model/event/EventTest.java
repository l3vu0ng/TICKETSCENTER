package vn.ticketscenter.model.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.model.identity.Organization;
import vn.ticketscenter.model.settlement.CommissionRule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventTest {
    static final Instant SALE = Instant.parse("2026-10-06T03:00:00Z");
    static final Instant START = SALE.plusSeconds(7200);
    static final Instant END = START.plusSeconds(7200);

    static Organization organization() {
        Organization owner = mock(Organization.class);
        when(owner.getId()).thenReturn(UUID.randomUUID());
        return owner;
    }

    static Event draft() {
        EventCategory category = new EventCategory(UUID.randomUUID(), "MUSIC", "Music", true, 1);
        return Event.draft(UUID.randomUUID(), organization(),
                new EventDetails("Title", "Description", category, "Venue", "Address", "https://example.test/cover"),
                new EventSchedule(SALE, START, START, END), SALE);
    }

    static Zone configuredZone(Event event) {
        Zone zone = Zone.create(UUID.randomUUID(), event, "Floor", ZoneType.STANDING, BigDecimal.ZERO);
        zone.setStandingCapacity(3);
        event.addZone(zone);
        return zone;
    }

    static Event published() {
        Event event = draft();
        configuredZone(event);
        event.submitForApproval();
        event.publish(effectiveRule(event.getOrganization()), SALE);
        return event;
    }

    static CommissionRule effectiveRule(Organization owner) {
        CommissionRule rule = mock(CommissionRule.class);
        when(rule.getId()).thenReturn(UUID.randomUUID());
        when(rule.getOrganization()).thenReturn(owner);
        when(rule.isEffectiveAt(SALE)).thenReturn(true);
        return rule;
    }

    @Test
    void independentValuesRejectInvalidInput() {
        DomainValuesCheck.verify();
    }

    @Test
    void saleAndCheckInAreHalfOpenAndRequirePublishedState() {
        Event event = published();
        assertFalse(event.isSaleActive(SALE.minusNanos(1)));
        assertTrue(event.isSaleActive(SALE));
        assertFalse(event.isSaleActive(START));
        assertFalse(event.isCheckInOpen(START.minusSeconds(3600).minusNanos(1)));
        assertTrue(event.isCheckInOpen(START.minusSeconds(3600)));
        assertFalse(event.isCheckInOpen(END));
        assertFalse(draft().isSaleActive(SALE));
    }

    @Test
    void submissionRequiresAConfiguredZoneAndCover() {
        Event event = draft();
        assertThrows(IllegalStateException.class, event::submitForApproval);
        configuredZone(event);
        event.updateDetails(new EventDetails(event.getTitle(), event.getDescription(), event.getCategory(),
                event.getVenueName(), event.getVenueAddress(), null));
        assertThrows(IllegalStateException.class, event::submitForApproval);
        assertEquals(EventStatus.DRAFT, event.getStatus());
    }

    @Test
    void publicationRejectsForeignAndExpiredRulesWithoutChangingState() {
        Event event = draft();
        configuredZone(event);
        event.submitForApproval();
        assertThrows(IllegalArgumentException.class, () -> event.publish(effectiveRule(organization()), SALE));
        CommissionRule expired = effectiveRule(event.getOrganization());
        when(expired.isEffectiveAt(SALE)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> event.publish(expired, SALE));
        assertEquals(EventStatus.PENDING_APPROVAL, event.getStatus());
        assertNull(event.getCommissionRule());
    }

    @Test
    void rejectionCanBeEditedAndResubmitted() {
        Event event = draft();
        configuredZone(event);
        event.submitForApproval();
        assertThrows(IllegalStateException.class, () -> event.configureSchedule(new EventSchedule(SALE, START, START, END)));
        event.reject("reason");
        event.configureSchedule(new EventSchedule(SALE, START, START, END));
        event.submitForApproval();
        assertEquals(EventStatus.PENDING_APPROVAL, event.getStatus());
        assertNull(event.getRejectionReason());
    }

    @Test
    void cancellationAtStartIsRejectedAndEarlierCancellationClosesWindows() {
        Event event = published();
        assertThrows(IllegalStateException.class, () -> event.cancel(START));
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        event.cancel(START.minusNanos(1));
        event.cancel(START);
        assertEquals(EventStatus.CANCELLED, event.getStatus());
        assertFalse(event.isSaleActive(SALE));
        assertFalse(event.isCheckInOpen(START));
    }
}
