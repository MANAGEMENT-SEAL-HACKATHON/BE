package com.sealhackathon.api.kits.value_object;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Allowed preferred shirt sizes on HackathonRegistration. */
public final class ShirtSize {

    public static final Set<String> ALLOWED = Set.of("XS", "S", "M", "L", "XL", "XXL");

    private ShirtSize() {}

    public static String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(v)) {
            throw new IllegalArgumentException("Invalid shirt size: " + raw);
        }
        return v;
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return ALLOWED.contains(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static String allowedList() {
        return Arrays.stream(ALLOWED.toArray(String[]::new)).sorted().collect(Collectors.joining(", "));
    }
}
