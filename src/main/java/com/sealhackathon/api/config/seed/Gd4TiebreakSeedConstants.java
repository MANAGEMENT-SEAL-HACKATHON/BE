package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ4 Phase E — tiebreak.
 *
 * <p>Doc: {@code docs/testing/dev-seed-slugs-guide.md}
 */
public final class Gd4TiebreakSeedConstants {

    private Gd4TiebreakSeedConstants() {
    }

    public static final String SLUG_GD4_TIEBREAK_SUBMISSION_TIME = "seal-gd4-tiebreak-submission-time";
    public static final String SLUG_GD4_TIEBREAK_MANUAL = "seal-gd4-tiebreak-manual";

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
}
