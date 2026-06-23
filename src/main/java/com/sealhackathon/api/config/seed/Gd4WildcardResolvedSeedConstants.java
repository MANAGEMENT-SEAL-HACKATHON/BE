package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — wildcard đã duyệt/từ chối sẵn, sẵn sàng advance.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile E
 */
public final class Gd4WildcardResolvedSeedConstants {

    private Gd4WildcardResolvedSeedConstants() {
    }

    public static final String SLUG_GD4_WILDCARD_RESOLVED = "seal-gd4-wildcard-resolved";

    public static final String[] TEAM_NAMES = {
            "GD4-W01 Rank1 Bảng A",
            "GD4-W02 Rank2 Bảng A",
            "GD4-W03 Rank1 Bảng B",
            "GD4-W04 Rank2 Bảng B",
            "GD4-W05 Rank1 Bảng C",
            "GD4-W06 Rank2 Bảng C",
            "GD4-W07 Rank1 Bảng D",
            "GD4-W08 Rank2 Bảng D"
    };

    public static final String[] GROUPS = {"A", "A", "B", "B", "C", "C", "D", "D"};

    public static final float[] TEAM_SCORES = {9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4w.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Wildcard Leader %02d".formatted(teamIndex);
    }
}
