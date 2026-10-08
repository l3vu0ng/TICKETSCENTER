package vn.ticketscenter.model.event;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.model.fulfillment.Ticket;
import vn.ticketscenter.model.fulfillment.TicketStatus;
import vn.ticketscenter.model.ticketing.TicketHoldItem;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeatTest {
    private Zone seatedZone() {
        Event event = EventTest.draft();
        Zone zone = Zone.create(UUID.randomUUID(), event, "Balcony", ZoneType.SEATED, BigDecimal.ZERO);
        event.addZone(zone);
        zone.configureSeating(1, 2);
        return zone;
    }

    private TicketHoldItem item(Zone zone, Seat seat, int quantity) {
        TicketHoldItem item = mock(TicketHoldItem.class);
        when(item.getZone()).thenReturn(zone);
        when(item.getSeat()).thenReturn(seat);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    @Test
    void matchingAllocationMovesThroughHoldReleaseSoldAndRefund() {
        Zone zone = seatedZone();
        Seat seat = zone.getSeats().getFirst();
        TicketHoldItem item = item(zone, seat, 1);
        seat.hold(item);
        seat.release(item);
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        seat.hold(item);
        seat.markSold(item);
        Ticket ticket = mock(Ticket.class);
        when(ticket.getSeat()).thenReturn(seat);
        when(ticket.getZone()).thenReturn(zone);
        when(ticket.getStatus()).thenReturn(TicketStatus.REFUNDED);
        seat.returnToInventory(ticket);
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertThrows(IllegalStateException.class, () -> seat.returnToInventory(ticket));
    }

    @Test
    void wrongSeatQuantityAndStandingItemDoNotMutateInventory() {
        Zone zone = seatedZone();
        Seat seat = zone.getSeats().getFirst();
        assertThrows(IllegalArgumentException.class, () -> seat.hold(item(zone, zone.getSeats().get(1), 1)));
        assertThrows(IllegalArgumentException.class, () -> seat.hold(item(zone, seat, 2)));
        Zone standing = EventTest.configuredZone(EventTest.draft());
        assertThrows(IllegalArgumentException.class, () -> seat.hold(item(standing, null, 1)));
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void unrefundedOrForeignTicketCannotReturnSoldInventory() {
        Zone zone = seatedZone();
        Seat seat = zone.getSeats().getFirst();
        seat.hold(item(zone, seat, 1));
        seat.markSold(item(zone, seat, 1));
        Ticket ticket = mock(Ticket.class);
        when(ticket.getZone()).thenReturn(zone);
        when(ticket.getSeat()).thenReturn(seat);
        when(ticket.getStatus()).thenReturn(TicketStatus.REFUND_PENDING);
        assertThrows(IllegalArgumentException.class, () -> seat.returnToInventory(ticket));
        when(ticket.getStatus()).thenReturn(TicketStatus.REFUNDED);
        when(ticket.getSeat()).thenReturn(zone.getSeats().get(1));
        assertThrows(IllegalArgumentException.class, () -> seat.returnToInventory(ticket));
        assertEquals(SeatStatus.SOLD, seat.getStatus());
    }
}
