package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — sơ loại đã khóa chấm + đồng điểm ranh giới Top-N (hybrid → GĐ4 tiebreak).
 *
 * <p>Slug: {@link #SLUG_GD3_TIEBREAK_HYBRID}
 */
public final class Gd3TiebreakHybridSeedConstants {

    private Gd3TiebreakHybridSeedConstants() {
    }

    public static final String SLUG_GD3_TIEBREAK_HYBRID = "seal-gd3-tiebreak-hybrid";

    /** Bảng A: 3 đội — 2 đội hòa điểm tại ranh giới topN=2. */
    public static final String TEAM_TIE_A1 = "GD3-T01 Tie-A rank1";
    public static final String TEAM_TIE_A2 = "GD3-T02 Tie-A rank2";
    public static final String TEAM_TIE_A3 = "GD3-T03 Tie-A rank3";
    /** Bảng B: 2 đội rõ ràng. */
    public static final String TEAM_CLEAR_B1 = "GD3-T04 Clear-B rank1";
    public static final String TEAM_CLEAR_B2 = "GD3-T05 Clear-B rank2";
    /** Track2 — 1 đội. */
    public static final String TEAM_TRACK2 = "GD3-T06 Track2 solo";

    public static final float[] TEAM_SCORES = {8.0f, 8.0f, 6.0f, 9.0f, 5.0f, 7.5f};

    public static String studentEmail(int teamIndex) {
        return "student.gd3.tie.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Tie Leader %02d".formatted(teamIndex);
    }
}
