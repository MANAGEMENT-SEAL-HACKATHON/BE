package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ5 — CK active, 4 đội ADVANCED, chưa có submission CK.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile A
 */
public final class Gd5SubmitOpenSeedConstants {

    private Gd5SubmitOpenSeedConstants() {
    }

    public static final String SLUG_GD5_SUBMIT_OPEN = "seal-gd5-submit-open";

    public static final String[] TEAM_NAMES = {
            "GD5-S01 CK chưa nộp",
            "GD5-S02 CK chưa nộp",
            "GD5-S03 CK chưa nộp",
            "GD5-S04 CK chưa nộp"
    };

    public static String studentEmail(int teamIndex) {
        return "student.gd5s.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Submit Leader %02d".formatted(teamIndex);
    }
}
