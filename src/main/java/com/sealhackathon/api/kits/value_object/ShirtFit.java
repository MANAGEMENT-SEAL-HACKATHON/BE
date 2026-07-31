package com.sealhackathon.api.kits.value_object;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Allowed preferred shirt fits on HackathonRegistration / KitStock. */
public final class ShirtFit {

    public static final String DEFAULT = "UNISEX";

    public static final Set<String> ALLOWED = Set.of("UNISEX", "MALE", "FEMALE");

    private ShirtFit() {}

    public static String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(v)) {
            throw new IllegalArgumentException("Invalid shirt fit: " + raw);
        }
        return v;
    }

    /** Blank → {@link #DEFAULT}; invalid → throws. */
    public static String normalizeOrDefault(String raw) {
        String v = normalizeOrNull(raw);
        return v == null ? DEFAULT : v;
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
