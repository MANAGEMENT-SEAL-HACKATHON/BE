package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ5 — queue CK PRESENTING, chấm đủ / một phần / chưa chấm.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile B
 */
public final class Gd5ScoringLiveSeedConstants {

    private Gd5ScoringLiveSeedConstants() {
    }

    public static final String SLUG_GD5_SCORING_LIVE = "seal-gd5-scoring-live";

    public static final String[] TEAM_NAMES = {
            "GD5-L01 CK scored full",
            "GD5-L02 CK scored partial",
            "GD5-L03 CK PRESENTING",
            "GD5-L04 CK WAITING"
    };

    public static String studentEmail(int teamIndex) {
        return "student.gd5l.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Scoring Leader %02d".formatted(teamIndex);
    }
}
