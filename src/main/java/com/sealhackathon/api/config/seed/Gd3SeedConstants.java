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

    public static final String TEAM_01 = "GD3-01 SUBMITTED + scored";
    public static final String TEAM_02 = "GD3-02 LATE_PENDING";
    public static final String TEAM_03 = "GD3-03 LATE_APPROVED";
    public static final String TEAM_04 = "GD3-04 chưa nộp";
    public static final String TEAM_05 = "GD3-05 Track2 scored";
    public static final String TEAM_06 = "GD3-06 Track2 chấm dở";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Leader %02d".formatted(teamIndex);
    }
}
