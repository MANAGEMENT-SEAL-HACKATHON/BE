package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ5 — slug {@link #SLUG_GD5_FINAL_ACTIVE}.
 *
 * <p>Doc: {@code docs/testing/gd4-gd5-e2e-seed-data.md}
 */
public final class Gd5SeedConstants {

    private Gd5SeedConstants() {
    }

    public static final String SLUG_GD5_FINAL_ACTIVE = "seal-gd5-final-active";

    public static final String TEAM_01 = "GD5-01 CK SUBMITTED + scored";
    public static final String TEAM_02 = "GD5-02 CK SUBMITTED chưa chấm";
    public static final String TEAM_03 = "GD5-03 ADVANCED chưa nộp CK";
    public static final String TEAM_04 = "GD5-04 ADVANCED chưa nộp CK (dự phòng)";

    public static String studentEmail(int teamIndex) {
        return "student.gd5.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Leader %02d".formatted(teamIndex);
    }
}
