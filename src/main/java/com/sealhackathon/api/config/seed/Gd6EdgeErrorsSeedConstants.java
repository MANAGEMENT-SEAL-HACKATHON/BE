package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ6 bad path — confirm khi CK chưa lock ({@code ROUND_NOT_SCORING_LOCKED}).
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile D
 */
public final class Gd6EdgeErrorsSeedConstants {

    private Gd6EdgeErrorsSeedConstants() {
    }

    public static final String SLUG_GD6_EDGE_ERRORS = "seal-gd6-edge-errors";

    public static final String[] TEAM_NAMES = {
            "GD6-E01 CK unlocked gate",
            "GD6-E02 CK unlocked gate",
            "GD6-E03 CK unlocked gate"
    };

    public static final float[] TEAM_SCORES = {9.0f, 8.5f, 8.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd6e.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD6 Edge Leader %02d".formatted(teamIndex);
    }
}
