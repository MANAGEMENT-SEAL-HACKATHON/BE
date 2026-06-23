package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ5 bad path — CK inactive, nộp bài → {@code ROUND_NOT_ACTIVE}.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile D
 */
public final class Gd5EdgeErrorsSeedConstants {

    private Gd5EdgeErrorsSeedConstants() {
    }

    public static final String SLUG_GD5_EDGE_ERRORS = "seal-gd5-edge-errors";

    public static final String[] TEAM_NAMES = {
            "GD5-E01 ADVANCED inactive CK",
            "GD5-E02 ADVANCED inactive CK",
            "GD5-E03 ADVANCED inactive CK",
            "GD5-E04 ADVANCED inactive CK"
    };

    public static String studentEmail(int teamIndex) {
        return "student.gd5e.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Edge Leader %02d".formatted(teamIndex);
    }
}
