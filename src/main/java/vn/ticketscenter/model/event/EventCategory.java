package vn.ticketscenter.model.event;

import java.text.Normalizer;
import java.util.Objects;
import java.util.UUID;

/** Lookup reference; not an additional business entity. Persistence mapping belongs to DONG-02. */
public class EventCategory {
    private UUID id;
    private String code;
    private String name;
    private boolean active;
    private int displayOrder;

    protected EventCategory() {
    }

    public EventCategory(UUID id, String code, String name, boolean active, int displayOrder) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = text(code, 64);
        this.name = text(name, 200);
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder cannot be negative");
        }
        this.active = active;
        this.displayOrder = displayOrder;
    }

    private static String text(String value, int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lookup text is required");
        }
        String result = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (result.length() > maximum) {
            throw new IllegalArgumentException("Lookup text is too long");
        }
        return result;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
}
