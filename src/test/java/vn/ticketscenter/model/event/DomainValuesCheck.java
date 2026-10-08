package vn.ticketscenter.model.event;

import java.time.Instant;
import java.util.UUID;

/** Executable check for independent value types when the shared Maven build is unavailable. */
public final class DomainValuesCheck {
    private DomainValuesCheck() {
    }

    public static void main(String[] args) {
        verify();
        System.out.println("PASS: 12 domain value assertions (no peer types or DB)");
    }

    public static void verify() {
        Instant sale = Instant.parse("2026-10-06T03:00:00Z");
        Instant start = sale.plusSeconds(7200);
        EventSchedule schedule = new EventSchedule(sale, start, start, start.plusSeconds(3600));
        check(schedule.saleEnd().equals(schedule.startTime()), "saleEnd=start accepted");
        fails(() -> new EventSchedule(sale, sale, start, start.plusSeconds(3600)));
        fails(() -> new EventSchedule(sale, start.plusSeconds(1), start, start.plusSeconds(3600)));
        fails(() -> new EventSchedule(sale, start, start, start));
        fails(() -> new EventSchedule(null, start, start, start.plusSeconds(3600)));
        EventCategory category = new EventCategory(UUID.randomUUID(), "MUSIC", "Music", true, 1);
        EventDetails details = new EventDetails("  Cafe\u0301  ", "Text", category, " Theatre ", " Address ", null);
        check(details.title().equals("Caf\u00e9"), "trim and NFC");
        check(details.venueName().equals("Theatre"), "venue trim");
        check(details.coverImageUrl() == null, "draft cover nullable");
        fails(() -> new EventDetails(" ", "Text", category, "Theatre", "Address", null));
        fails(() -> new EventDetails("A".repeat(201), "Text", category, "Theatre", "Address", null));
        fails(() -> new EventDetails("Title", "Text", null, "Theatre", "Address", null));
        fails(() -> new EventDetails("Title", "Text", category, "Theatre", "Address", "x".repeat(2049)));
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }

    private static void fails(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid input accepted");
    }
}
