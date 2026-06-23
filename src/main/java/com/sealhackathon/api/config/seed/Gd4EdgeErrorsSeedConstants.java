package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 bad path — activate CK thiếu judge ({@code JUDGE_NOT_ASSIGNED}).
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile D
 */
public final class Gd4EdgeErrorsSeedConstants {

    private Gd4EdgeErrorsSeedConstants() {
    }

    public static final String SLUG_GD4_EDGE_ERRORS = "seal-gd4-edge-errors";

    public static final String[] TEAM_NAMES = {
            "GD4-E01 Advanced A",
            "GD4-E02 Advanced A",
            "GD4-E03 Advanced B",
            "GD4-E04 Advanced B"
    };

    public static final String[] GROUPS = {"A", "A", "B", "B"};

    public static final float[] TEAM_SCORES = {9.0f, 8.0f, 9.0f, 8.0f};

    public static String studentEmail(int teamIndex) {
        return "student.gd4e.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 Edge Leader %02d".formatted(teamIndex);
    }
}
