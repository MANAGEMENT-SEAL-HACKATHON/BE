package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — sơ loại đã publish, chưa advance.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile A
 */
public final class Gd4PublishedSeedConstants {

    private Gd4PublishedSeedConstants() {
    }

    public static final String SLUG_GD4_PUBLISHED = "seal-gd4-published";

    public static final String[] TEAM_NAMES = {
            "GD4-P01 Rank1 Bảng A",
            "GD4-P02 Rank2 Bảng A",
            "GD4-P03 Rank1 Bảng B",
            "GD4-P04 Rank2 Bảng B",
            "GD4-P05 Rank1 Bảng C",
            "GD4-P06 Rank2 Bảng C",
            "GD4-P07 Rank1 Bảng D",
            "GD4-P08 Rank2 Bảng D"
    };

    public static final String[] GROUPS = {"A", "A", "B", "B", "C", "C", "D", "D"};

    public static final float[] TEAM_SCORES = {9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f, 9.0f, 7.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4p.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Published Leader %02d".formatted(teamIndex);
    }
}
