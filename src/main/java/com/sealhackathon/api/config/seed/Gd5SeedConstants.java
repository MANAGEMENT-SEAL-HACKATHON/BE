package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed GĐ5 — slug {@link #SLUG_GD5_FINAL_ACTIVE}.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md}
 */
public final class Gd5SeedConstants {

    private Gd5SeedConstants() {
    }

    public static final String SLUG_GD5_FINAL_ACTIVE = "seal-gd5-final-active";

    public static final String TEAM_01 = "GD5-01 đã nộp CK";
    public static final String TEAM_02 = "GD5-02 đã nộp CK";
    /** Đội ADVANCED còn trống — dùng leader03 để test nộp CK. */
    public static final String TEAM_03 = "GD5-03 chưa nộp CK — test nộp";
    /** Không vào CK — vẫn ACTIVE ở hackathon để test lọc roster. */
    public static final String TEAM_04 = "GD5-04 bị loại sơ loại (không vào CK)";

    /** Leader đội còn trống để test nộp: {@link #studentEmail}(3). */
    public static final int TEAM_INDEX_PENDING_SUBMIT = 3;

    public static String studentEmail(int teamIndex) {
        return "student.gd5.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Leader %02d".formatted(teamIndex);
    }
}
