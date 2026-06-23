package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — hàng đợi thuyết trình + chấm live (happy / hybrid path).
 *
 * <p>Slug: {@link #SLUG_GD3_SCORING_LIVE}
 */
public final class Gd3ScoringLiveSeedConstants {

    private Gd3ScoringLiveSeedConstants() {
    }

    public static final String SLUG_GD3_SCORING_LIVE = "seal-gd3-scoring-live";

    public static final String[] TEAM_NAMES = {
            "GD3-S01 Scored-full",
            "GD3-S02 Scored-partial",
            "GD3-S03 Queue-presenting",
            "GD3-S04 Track2-done",
            "GD3-S05 Track2-presenting",
            "GD3-S06 No-score"
    };

    /** Track1: teams 1–3; Track2: teams 4–6. */
    public static final String[] GROUPS = {"BANG-A", "BANG-A", "BANG-A", "BANG-B", "BANG-B", "BANG-B"};

    public static String studentEmail(int teamIndex) {
        return "student.gd3.live.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Live Leader %02d".formatted(teamIndex);
    }
}
