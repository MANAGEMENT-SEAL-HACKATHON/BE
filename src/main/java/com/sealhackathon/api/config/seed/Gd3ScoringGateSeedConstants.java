package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — judge chấm khi slot WAITING → {@code SCORING_NOT_OPEN}.
 *
 * <p>Slug: {@link #SLUG_GD3_SCORING_GATE}
 */
public final class Gd3ScoringGateSeedConstants {

    private Gd3ScoringGateSeedConstants() {
    }

    public static final String SLUG_GD3_SCORING_GATE = "seal-gd3-scoring-gate";

    public static final String TEAM_PRESENTING = "GD3-SG-T01 Presenting";
    public static final String TEAM_WAITING = "GD3-SG-T02 Waiting";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.scoringgate.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Scoring Gate Leader %02d".formatted(teamIndex);
    }
}
