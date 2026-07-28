package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ4 Phase E — tiebreak + wildcard gap.
 *
 * <p>Doc: {@code docs/testing/dev-seed-slugs-guide.md}
 */
public final class Gd4TiebreakSeedConstants {

    private Gd4TiebreakSeedConstants() {
    }

    public static final String SLUG_GD4_TIEBREAK_SUBMISSION_TIME = "seal-gd4-tiebreak-submission-time";
    public static final String SLUG_GD4_TIEBREAK_MANUAL = "seal-gd4-tiebreak-manual";
    public static final String SLUG_GD4_WILDCARD_GAP = "seal-gd4-wildcard-gap";

    /** 3 đội cùng bảng A — Team1 rõ Top-2; Team2/3 hòa tại ngưỡng topN=2. */
    public static final String[] TEAM_NAMES_SUBMISSION_TIME = {
            "GD4-ST01 Clear Rank1",
            "GD4-ST02 Tie Earlier",
            "GD4-ST03 Tie Later"
    };

    public static final float[] TEAM_SCORES_SUBMISSION_TIME = {9.0f, 7.0f, 7.0f};

    /** 4 đội cùng bảng A — TM01/TM02 hòa tại topN=1. */
    public static final String[] TEAM_NAMES_MANUAL = {
            "GD4-TM01 Tie A",
            "GD4-TM02 Tie B",
            "GD4-TM03 Rank3",
            "GD4-TM04 Rank4"
    };

    public static final float[] TEAM_SCORES_MANUAL = {9.0f, 9.0f, 7.0f, 6.0f};

    /** 8 đội × 2 track — mỗi track 9/7/7/6 để WC pool ≥2 ứng viên đồng điểm. */
    public static final String[] TEAM_NAMES_WILDCARD_GAP = {
            "GD4-WC01 Rank1 Track A",
            "GD4-WC02 WC Pool A",
            "GD4-WC03 WC Pool A2",
            "GD4-WC04 Rank4 Track A",
            "GD4-WC05 Rank1 Track B",
            "GD4-WC06 WC Pool B",
            "GD4-WC07 WC Pool B2",
            "GD4-WC08 Rank4 Track B"
    };

    public static final String[] GROUPS_WILDCARD_GAP = {"A", "B", "C", "D", "A", "B", "C", "D"};

    public static final float[] TEAM_SCORES_WILDCARD_GAP = {
            9.0f, 7.0f, 7.0f, 6.0f, 9.0f, 7.0f, 7.0f, 6.0f
    };

    public static String studentEmailSubmissionTime(int teamIndex) {
        return "student.gd4st.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayNameSubmissionTime(int teamIndex) {
        return "GD4 ST Leader %02d".formatted(teamIndex);
    }

    public static String studentEmailManual(int teamIndex) {
        return "student.gd4tm.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayNameManual(int teamIndex) {
        return "GD4 TM Leader %02d".formatted(teamIndex);
    }

    public static String studentEmailWildcardGap(int teamIndex) {
        return "student.gd4wc.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayNameWildcardGap(int teamIndex) {
        return "GD4 WC Leader %02d".formatted(teamIndex);
    }
}
