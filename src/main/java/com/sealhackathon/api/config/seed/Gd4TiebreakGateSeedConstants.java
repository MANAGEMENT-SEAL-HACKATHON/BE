package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — đồng điểm ranh giới topN chặn advance ({@code TIEBREAK_REQUIRED}).
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile B
 */
public final class Gd4TiebreakGateSeedConstants {

    private Gd4TiebreakGateSeedConstants() {
    }

    public static final String SLUG_GD4_TIEBREAK_GATE = "seal-gd4-tiebreak-gate";

    /** 4 đội cùng bảng A — 2 đội hòa 9.0 tại ranh giới topN=1. */
    public static final String[] TEAM_NAMES = {
            "GD4-TB01 Tie 9.0",
            "GD4-TB02 Tie 9.0",
            "GD4-TB03 Rank3 7.0",
            "GD4-TB04 Rank4 6.0"
    };

    public static final float[] TEAM_SCORES = {9.0f, 9.0f, 7.0f, 6.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4tb.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Tiebreak Leader %02d".formatted(teamIndex);
    }
}
