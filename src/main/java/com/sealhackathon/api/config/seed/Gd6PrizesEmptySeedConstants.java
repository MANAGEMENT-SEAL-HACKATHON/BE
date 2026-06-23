package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ6 — PENDING_CONFIRM, chưa có prize (test {@code NO_PRIZES_RECORDED}).
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile A
 */
public final class Gd6PrizesEmptySeedConstants {

    private Gd6PrizesEmptySeedConstants() {
    }

    public static final String SLUG_GD6_PRIZES_EMPTY = "seal-gd6-prizes-empty";

    public static final String[] TEAM_NAMES = {
            "GD6-P01 CK scored",
            "GD6-P02 CK scored",
            "GD6-P03 CK scored"
    };

    public static final float[] TEAM_SCORES = {9.2f, 8.6f, 8.1f};

    public static String studentEmail(int teamIndex) {
        return "student.gd6p.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD6 Prizes-empty Leader %02d".formatted(teamIndex);
    }
}
