package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ4 — Tiebreak rules + Wildcard gap (Phase E).
 */
public final class Gd4TiebreakWildcardSeedConstants {

    private Gd4TiebreakWildcardSeedConstants() {
    }

    public static final String SLUG_TIEBREAK_SUBMISSION_TIME = "seal-gd4-tiebreak-submission-time";
    public static final String SLUG_TIEBREAK_MANUAL = "seal-gd4-tiebreak-manual";
    public static final String SLUG_WILDCARD_GAP = "seal-gd4-wildcard-gap";

    public static String studentEmail(String prefix, int teamIndex) {
        return "student.%s.leader%02d@fpt.edu.vn".formatted(prefix, teamIndex);
    }

    public static String studentDisplayName(String prefix, int teamIndex) {
        return "%s Leader %02d".formatted(prefix.toUpperCase(), teamIndex);
    }
}
