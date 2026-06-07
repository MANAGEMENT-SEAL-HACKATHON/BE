package com.sealhackathon.api.hackathons.value_object;

/**
 * Dry-run checklist cho {@code GET /hackathons/{id}/readiness?target=}.
 *
 * <p>{@link #ONGOING} — FR-07 gate G1–G5 (PATCH status DRAFT→ONGOING).
 * {@link #FINAL_ROUND} — checklist vận hành GĐ4/5 trước activate Chung kết.
 * {@link #AWARDS} / {@link #PENDING_CONFIRM} — shell GĐ6.
 */
public enum ReadinessTarget {
    ONGOING,
    FINAL_ROUND,
    AWARDS,
    PENDING_CONFIRM;

    public static ReadinessTarget fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return ONGOING;
        }
        String normalized = raw.trim().toUpperCase();
        for (ReadinessTarget t : values()) {
            if (t.name().equals(normalized)) {
                return t;
            }
        }
        try {
            HackathonStatus status = HackathonStatus.valueOf(normalized);
            return switch (status) {
                case ONGOING -> ONGOING;
                case PENDING_CONFIRM -> PENDING_CONFIRM;
                default -> ONGOING;
            };
        } catch (IllegalArgumentException ex) {
            return ONGOING;
        }
    }
}
