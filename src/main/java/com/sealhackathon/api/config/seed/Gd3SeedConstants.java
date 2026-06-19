package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ3 — slug {@link #SLUG_GD3_PRELIM_OPEN}.
 *
 * <p>Doc: {@code docs/testing/fe-gd3-api-mapping.md} §14
 */
public final class Gd3SeedConstants {

    private Gd3SeedConstants() {
    }

    public static final String SLUG_GD3_PRELIM_OPEN = "seal-gd3-prelim-open";

    public static final String TEAM_01 = "GD3-01";
    public static final String TEAM_02 = "GD3-02";
    public static final String TEAM_03 = "GD3-03";
    public static final String TEAM_04 = "GD3-04";
    public static final String TEAM_05 = "GD3-05";
    public static final String TEAM_06 = "GD3-06";

    /** Đội cuối — chưa nộp bài; demo nộp → shuffle queue → chấm live. */
    public static final int DEMO_TEAM_INDEX = 6;

    public static String studentEmail(int teamIndex) {
        return "student.gd3.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Leader %02d".formatted(teamIndex);
    }
}
