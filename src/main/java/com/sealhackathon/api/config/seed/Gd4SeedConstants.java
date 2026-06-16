package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ4 — slug {@link #SLUG_GD4_ADVANCE_READY}.
 *
 * <p>Doc: {@code docs/testing/gd4-gd5-e2e-seed-data.md} § GĐ4
 */
public final class Gd4SeedConstants {

    private Gd4SeedConstants() {
    }

    public static final String SLUG_GD4_ADVANCE_READY = "seal-gd4-advance-ready";

    public static final String[] TEAM_NAMES = {
            "GD4-A01 Rank1 Bảng A",
            "GD4-A02 Rank2 Bảng A",
            "GD4-A03 Rank1 Bảng B",
            "GD4-A04 Rank2 Bảng B",
            "GD4-A05 Rank1 Bảng C",
            "GD4-A06 Rank2 Bảng C",
            "GD4-A07 Rank1 Bảng D",
            "GD4-A08 Rank2 Bảng D"
    };

    /** Bảng A–D theo thứ tự team 1–8. */
    public static final String[] GROUPS = {"A", "A", "B", "B", "C", "C", "D", "D"};

    /** Điểm rank1 / rank2 trong mỗi bảng. */
    public static final float[] TEAM_SCORES = {9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4a.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Leader %02d".formatted(teamIndex);
    }
}
