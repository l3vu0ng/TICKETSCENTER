package vn.ticketscenter.model.event;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Inventory methods require caller-controlled locks and persistence idempotency. */
public class Zone {
    private UUID id;
    private Event event;
    private String name;
    private ZoneType type;
    private BigDecimal price;
    private Integer standingCapacity;
    private Integer standingHeld;
    private Integer standingSold;
    private List<Seat> seats = new ArrayList<>();

    protected Zone() {
    }

    public static Zone create(UUID id, Event event, String name, ZoneType type, BigDecimal price) {
        Zone result = new Zone();
        result.id = Objects.requireNonNull(id, "id");
        result.event = Objects.requireNonNull(event, "event");
        result.requireEditable();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Zone name is required");
        }
        result.name = Normalizer.normalize(name.strip(), Normalizer.Form.NFC);
        if (result.name.length() > 100) {
            throw new IllegalArgumentException("Zone name is too long");
        }
        result.type = Objects.requireNonNull(type, "type");
        result.price = vnd(price);
        if (type == ZoneType.STANDING) {
            result.standingHeld = 0;
            result.standingSold = 0;
        }
        return result;
    }

    public void changePrice(BigDecimal price) {
        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED
                && event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Price cannot change in the current event state");
        }
        this.price = vnd(price);
    }

    public void configureSeating(Integer rows, Integer seatsPerRow) {
        requireEditable();
        if (type != ZoneType.SEATED) {
            throw new IllegalStateException("Only a seated zone can have seats");
        }
        if (rows == null || rows < 1 || rows > 100 || seatsPerRow == null || seatsPerRow < 1 || seatsPerRow > 100) {
            throw new IllegalArgumentException("Rows and seats per row must each be between 1 and 100");
        }
        if (hasAllocation()) {
            throw new IllegalStateException("Allocated seats cannot be reconfigured");
        }
        List<Seat> replacement = new ArrayList<>(rows * seatsPerRow);
        for (int row = 1; row <= rows; row++) {
            for (int number = 1; number <= seatsPerRow; number++) {
                replacement.add(new Seat(UUID.randomUUID(), this, rowName(row), number));
            }
        }
        seats = replacement;
    }

    public void setStandingCapacity(Integer capacity) {
        requireEditable();
        if (type != ZoneType.STANDING) {
            throw new IllegalStateException("Only a standing zone can have a standing capacity");
        }
        if (capacity == null || capacity < 1 || capacity > 1000000) {
            throw new IllegalArgumentException("Standing capacity must be between 1 and 1000000");
        }
        if (capacity < standingHeld + standingSold) {
            throw new IllegalStateException("Capacity cannot be lower than allocated inventory");
        }
        standingCapacity = capacity;
    }

    public void holdStanding(Integer quantity) {
        int amount = standingQuantity(quantity);
        if (amount > getAvailable()) {
            throw new IllegalStateException("Not enough standing inventory");
        }
        standingHeld += amount;
    }

    public void releaseHeldStanding(Integer quantity) {
        int amount = standingQuantity(quantity);
        if (amount > standingHeld) {
            throw new IllegalStateException("Cannot release more than held inventory");
        }
        standingHeld -= amount;
    }

    public void sellHeldStanding(Integer quantity) {
        int amount = standingQuantity(quantity);
        if (amount > standingHeld) {
            throw new IllegalStateException("Cannot sell more than held inventory");
        }
        standingHeld -= amount;
        standingSold += amount;
    }

    public void returnSoldStanding(Integer quantity) {
        int amount = standingQuantity(quantity);
        if (amount > standingSold) {
            throw new IllegalStateException("Cannot return more than sold inventory");
        }
        standingSold -= amount;
    }

    public Integer getCapacity() {
        if (type == ZoneType.SEATED) {
            return seats.size();
        }
        return standingCapacity;
    }
    public Integer getHeld() { return type == ZoneType.SEATED ? count(SeatStatus.HELD) : standingHeld; }
    public Integer getSold() { return type == ZoneType.SEATED ? count(SeatStatus.SOLD) : standingSold; }
    public Integer getAvailable() {
        if (type == ZoneType.SEATED) {
            return count(SeatStatus.AVAILABLE);
        }
        return standingCapacity == null ? 0 : standingCapacity - standingHeld - standingSold;
    }

    boolean isConfigured() {
        if (type == ZoneType.SEATED) {
            return !seats.isEmpty() && seats.stream().allMatch(seat -> seat.getZone() != null
                    && id.equals(seat.getZone().getId()));
        }
        return standingCapacity != null && standingCapacity > 0 && standingHeld != null && standingHeld >= 0
                && standingSold != null && standingSold >= 0 && standingHeld + standingSold <= standingCapacity;
    }

    boolean hasAllocation() { return getHeld() > 0 || getSold() > 0; }

    void attachTo(Event owner) {
        if (!event.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Cannot move a zone to another event");
        }
        event = owner;
    }

    private int count(SeatStatus status) {
        return (int) seats.stream().filter(seat -> seat.getStatus() == status).count();
    }

    private void requireEditable() {
        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED) {
            throw new IllegalStateException("Event layout is locked");
        }
    }

    private int standingQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (type != ZoneType.STANDING || standingCapacity == null) {
            throw new IllegalStateException("Standing inventory is not configured");
        }
        return quantity;
    }

    private static BigDecimal vnd(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Price must be nonnegative");
        }
        try {
            BigDecimal whole = amount.setScale(0, java.math.RoundingMode.UNNECESSARY);
            if (whole.precision() > 19) {
                throw new IllegalArgumentException("Price exceeds decimal(19,0)");
            }
            return whole;
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Price must be a whole VND amount", error);
        }
    }

    private static String rowName(int number) {
        StringBuilder result = new StringBuilder();
        for (int current = number; current > 0; current /= 26) {
            current--;
            result.append((char) ('A' + current % 26));
        }
        return result.reverse().toString();
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public String getName() { return name; }
    public ZoneType getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public Integer getStandingCapacity() { return standingCapacity; }
    public Integer getStandingHeld() { return standingHeld; }
    public Integer getStandingSold() { return standingSold; }
    public List<Seat> getSeats() { return List.copyOf(seats); }
}
