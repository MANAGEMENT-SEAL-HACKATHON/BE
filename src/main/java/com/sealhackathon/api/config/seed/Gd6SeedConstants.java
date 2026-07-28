package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ6 — slug {@link #SLUG_GD6_PENDING_CONFIRM}.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md}
 */
public final class Gd6SeedConstants {

    private Gd6SeedConstants() {
    }

    public static final String SLUG_GD6_PENDING_CONFIRM = "seal-gd6-pending-confirm";

    public static final String TEAM_01 = "GD6-01 ADVANCED CK";
    public static final String TEAM_02 = "GD6-02 ADVANCED CK";
    public static final String TEAM_03 = "GD6-03 ADVANCED CK";

    public static String studentEmail(int teamIndex) {
        return "student.gd6.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD6 Leader %02d".formatted(teamIndex);
    }
}
