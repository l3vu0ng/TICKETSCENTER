package vn.ticketscenter.model.event;

import java.text.Normalizer;

public record EventDetails(String title, String description, EventCategory category,
                           String venueName, String venueAddress, String coverImageUrl) {
    public EventDetails {
        title = required(title, "title", 200);
        description = required(description, "description", 20000);
        venueName = required(venueName, "venueName", 200);
        venueAddress = required(venueAddress, "venueAddress", 500);
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        coverImageUrl = coverImageUrl == null || coverImageUrl.isBlank() ? null : coverImageUrl.strip();
        if (coverImageUrl != null && coverImageUrl.length() > 2048) {
            throw new IllegalArgumentException("coverImageUrl is too long");
        }
    }

    private static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized;
    }
}
