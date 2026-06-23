package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — bad path API validation: round inactive, thiếu judge, submission INCOMPLETE.
 *
 * <p>Slug: {@link #SLUG_GD3_EDGE_ERRORS}
 */
public final class Gd3EdgeErrorsSeedConstants {

    private Gd3EdgeErrorsSeedConstants() {
    }

    public static final String SLUG_GD3_EDGE_ERRORS = "seal-gd3-edge-errors";

    public static final String TEAM_COMPLETE = "GD3-E01 Complete";
    public static final String TEAM_INCOMPLETE = "GD3-E02 Incomplete-slide";
    public static final String TEAM_NO_SUBMIT = "GD3-E03 No-submit";
    public static final String TEAM_TRACK2 = "GD3-E04 Track2-ready";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.edge.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Edge Leader %02d".formatted(teamIndex);
    }
}
