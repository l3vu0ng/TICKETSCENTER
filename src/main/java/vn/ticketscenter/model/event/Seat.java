package vn.ticketscenter.model.event;

import vn.ticketscenter.model.fulfillment.Ticket;
import vn.ticketscenter.model.fulfillment.TicketStatus;
import vn.ticketscenter.model.ticketing.TicketHoldItem;

import java.util.Objects;
import java.util.UUID;

public class Seat {
    private UUID id;
    private Zone zone;
    private String rowName;
    private int seatNumber;
    private SeatStatus status;

    protected Seat() {
    }

    Seat(UUID id, Zone zone, String rowName, int seatNumber) {
        this.id = Objects.requireNonNull(id, "id");
        this.zone = Objects.requireNonNull(zone, "zone");
        if (zone.getType() != ZoneType.SEATED || rowName == null || rowName.isBlank() || seatNumber < 1) {
            throw new IllegalArgumentException("A seat requires a seated zone and valid label");
        }
        this.rowName = rowName;
        this.seatNumber = seatNumber;
        status = SeatStatus.AVAILABLE;
    }

    public void hold(TicketHoldItem item) {
        requireAllocation(item);
        transition(SeatStatus.AVAILABLE, SeatStatus.HELD);
    }

    public void release(TicketHoldItem item) {
        requireAllocation(item);
        transition(SeatStatus.HELD, SeatStatus.AVAILABLE);
    }

    public void markSold(TicketHoldItem item) {
        requireAllocation(item);
        transition(SeatStatus.HELD, SeatStatus.SOLD);
    }

    public void returnToInventory(Ticket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        if (ticket.getStatus() != TicketStatus.REFUNDED || !matches(ticket.getZone(), ticket.getSeat())) {
            throw new IllegalArgumentException("A refunded ticket allocated to this seat is required");
        }
        transition(SeatStatus.SOLD, SeatStatus.AVAILABLE);
    }

    private void requireAllocation(TicketHoldItem item) {
        Objects.requireNonNull(item, "item");
        if (!Objects.equals(item.getQuantity(), 1) || !matches(item.getZone(), item.getSeat())) {
            throw new IllegalArgumentException("Hold item must select this seat, zone and event with quantity 1");
        }
    }

    private boolean matches(Zone allocatedZone, Seat allocatedSeat) {
        return allocatedZone != null && allocatedSeat != null
                && allocatedZone.getType() == ZoneType.SEATED
                && zone.getId().equals(allocatedZone.getId())
                && id.equals(allocatedSeat.getId())
                && allocatedSeat.getZone() != null
                && zone.getId().equals(allocatedSeat.getZone().getId())
                && allocatedZone.getEvent() != null
                && zone.getEvent().getId().equals(allocatedZone.getEvent().getId());
    }

    private void transition(SeatStatus expected, SeatStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Seat cannot transition from " + status + " to " + next);
        }
        status = next;
    }

    public UUID getId() { return id; }
    public Zone getZone() { return zone; }
    public String getRowName() { return rowName; }
    public int getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }
}
