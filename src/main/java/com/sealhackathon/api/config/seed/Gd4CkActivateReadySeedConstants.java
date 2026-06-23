package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — đã publish + advance 6 đội, guest judge CK, CK chưa active.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile C
 */
public final class Gd4CkActivateReadySeedConstants {

    private Gd4CkActivateReadySeedConstants() {
    }

    public static final String SLUG_GD4_CK_ACTIVATE_READY = "seal-gd4-ck-activate-ready";

    public static final String[] TEAM_NAMES = {
            "GD4-K01 Rank1 Bảng A",
            "GD4-K02 Wildcard Bảng A",
            "GD4-K03 Rank1 Bảng B",
            "GD4-K04 Eliminate B",
            "GD4-K05 Rank1 Bảng C",
            "GD4-K06 Wildcard Bảng C",
            "GD4-K07 Rank1 Bảng D",
            "GD4-K08 Eliminate D"
    };

    public static final String[] GROUPS = {"A", "A", "B", "B", "C", "C", "D", "D"};

    public static final float[] TEAM_SCORES = {9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f};

    /** Chỉ số team (0-based) được mark ADVANCED khi seed. */
    public static final int[] ADVANCED_TEAM_INDICES = {0, 1, 2, 4, 5, 6};

    public static String studentEmail(int teamIndex) {
        return "student.gd4k.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 CK Leader %02d".formatted(teamIndex);
    }
}
