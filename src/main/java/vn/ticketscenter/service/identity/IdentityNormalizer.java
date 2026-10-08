package vn.ticketscenter.service.identity;

import java.text.Normalizer;
import java.util.Locale;

public final class IdentityNormalizer {
    private IdentityNormalizer() {}

    public static String userName(String value) {
        String result = normalized(value);
        if (!result.matches("[a-z0-9_]{3,32}"))
            throw new IllegalArgumentException("Invalid userName");
        return result;
    }

    public static String email(String value) {
        String result = normalized(value);
        if (result.length() > 254 || !result.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Invalid email");
        return result;
    }

    private static String normalized(String value) {
        if (value == null) throw new IllegalArgumentException("Identity value required");
        return value.strip().toLowerCase(Locale.ROOT);
    }

    public static String fullName(String value) {
        if (value == null) throw new IllegalArgumentException("Full name required");
        String result = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (result.isEmpty()
                || result.length() > 120
                || result.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid full name");
        return result;
    }

    public static String phone(String value) {
        if (value == null || value.isBlank()) return null;
        String result = value.strip();
        if (result.length() > 20 || !result.matches("\\+?[0-9][0-9 ()-]*"))
            throw new IllegalArgumentException("Invalid phone");
        return result;
    }
}
