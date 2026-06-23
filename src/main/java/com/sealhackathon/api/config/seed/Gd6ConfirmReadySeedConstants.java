package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ6 — đủ 3 giải, sẵn sàng {@code PATCH /confirm} một lần.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile B
 */
public final class Gd6ConfirmReadySeedConstants {

    private Gd6ConfirmReadySeedConstants() {
    }

    public static final String SLUG_GD6_CONFIRM_READY = "seal-gd6-confirm-ready";

    public static final String[] TEAM_NAMES = {
            "GD6-R01 FIRST prize",
            "GD6-R02 SECOND prize",
            "GD6-R03 THIRD prize"
    };

    public static final float[] TEAM_SCORES = {9.4f, 8.8f, 8.2f};

    public static String studentEmail(int teamIndex) {
        return "student.gd6r.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD6 Confirm-ready Leader %02d".formatted(teamIndex);
    }
}
