package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — activate prelim fail: {@code ROUND_NO_CRITERIA}, {@code ROUND_WEIGHT_NOT_ONE}.
 */
public final class Gd3RoundConfigEdgeSeedConstants {

    private Gd3RoundConfigEdgeSeedConstants() {
    }

    public static final String SLUG_GD3_ROUND_CONFIG_EDGE = "seal-gd3-round-config-edge";

    public static final String TEAM_TRACK1 = "GD3-RC01 Track1";
    public static final String TEAM_TRACK2 = "GD3-RC02 Track2";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.rc.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 RC Leader %02d".formatted(teamIndex);
    }
}
