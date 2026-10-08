package vn.ticketscenter.model.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoneTest {
    @Test
    void seatedTwoByThreeProducesUniqueLabelsAndNullStandingCounters() {
        Event event = EventTest.draft();
        Zone zone = Zone.create(UUID.randomUUID(), event, "Balcony", ZoneType.SEATED, BigDecimal.ZERO);
        event.addZone(zone);
        zone.configureSeating(2, 3);
        assertEquals(List.of("A1", "A2", "A3", "B1", "B2", "B3"), zone.getSeats().stream()
                .map(seat -> seat.getRowName() + seat.getSeatNumber()).toList());
        assertEquals(6, zone.getAvailable());
        assertNull(zone.getStandingCapacity());
        assertNull(zone.getStandingHeld());
        assertNull(zone.getStandingSold());
        zone.configureSeating(27, 1);
        assertEquals("AA", zone.getSeats().get(26).getRowName());
    }

    @Test
    void standingInventoryPreservesCapacityAndRejectsUnderflow() {
        Zone zone = EventTest.configuredZone(EventTest.draft());
        zone.holdStanding(2);
        assertEquals(1, zone.getAvailable());
        assertThrows(IllegalArgumentException.class, () -> zone.holdStanding(0));
        assertThrows(IllegalArgumentException.class, () -> zone.holdStanding(-1));
        assertThrows(IllegalStateException.class, () -> zone.holdStanding(2));
        zone.sellHeldStanding(2);
        assertEquals(0, zone.getHeld());
        assertEquals(2, zone.getSold());
        zone.returnSoldStanding(1);
        assertEquals(1, zone.getSold());
        assertEquals(2, zone.getAvailable());
        assertThrows(IllegalStateException.class, () -> zone.releaseHeldStanding(1));
        assertThrows(IllegalStateException.class, () -> zone.returnSoldStanding(2));
        assertEquals(1, zone.getSold());
    }

    @Test
    void pricesAcceptZeroAndRejectFractionNegativeAndOverflow() {
        Zone zone = EventTest.configuredZone(EventTest.draft());
        assertEquals(BigDecimal.ZERO, zone.getPrice());
        assertThrows(IllegalArgumentException.class, () -> zone.changePrice(new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class, () -> zone.changePrice(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> zone.changePrice(new BigDecimal("10000000000000000000")));
        assertEquals(BigDecimal.ZERO, zone.getPrice());
        zone.changePrice(new BigDecimal("9999999999999999999"));
    }

    @Test
    void publishedLayoutIsLockedWhilePriceAndInventoryCanChange() {
        Event event = EventTest.published();
        Zone zone = event.getZones().getFirst();
        assertThrows(IllegalStateException.class, () -> zone.setStandingCapacity(4));
        assertThrows(IllegalStateException.class, () -> event.removeZone(zone));
        assertThrows(IllegalStateException.class, () -> Zone.create(UUID.randomUUID(), event, "New", ZoneType.STANDING, BigDecimal.ZERO));
        zone.changePrice(new BigDecimal("250000"));
        zone.holdStanding(1);
        event.cancel(EventTest.START.minusNanos(1));
        zone.releaseHeldStanding(1);
        assertEquals(3, zone.getAvailable());
        assertThrows(IllegalStateException.class, () -> zone.changePrice(BigDecimal.ZERO));
    }
}
