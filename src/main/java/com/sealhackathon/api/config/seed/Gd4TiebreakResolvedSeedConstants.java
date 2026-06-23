package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — coordinator đã resolve tiebreak, sẵn sàng advance.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile F
 */
public final class Gd4TiebreakResolvedSeedConstants {

    private Gd4TiebreakResolvedSeedConstants() {
    }

    public static final String SLUG_GD4_TIEBREAK_RESOLVED = "seal-gd4-tiebreak-resolved";

    public static final String[] TEAM_NAMES = {
            "GD4-TR01 Tie winner",
            "GD4-TR02 Tie penalized",
            "GD4-TR03 Rank3 7.0",
            "GD4-TR04 Rank4 6.0"
    };

    public static final float[] TEAM_SCORES = {9.0f, 9.0f, 7.0f, 6.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4tr.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Tiebreak Resolved %02d".formatted(teamIndex);
    }
}
