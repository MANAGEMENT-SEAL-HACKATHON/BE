package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — duyệt nộp trễ / bad path submission.
 *
 * <p>Slug: {@link #SLUG_GD3_LATE_REVIEW}
 */
public final class Gd3LateReviewSeedConstants {

    private Gd3LateReviewSeedConstants() {
    }

    public static final String SLUG_GD3_LATE_REVIEW = "seal-gd3-late-review";

    public static final String TEAM_ON_TIME = "GD3-L01 On-time";
    public static final String TEAM_LATE_PENDING = "GD3-L02 Late-pending";
    public static final String TEAM_LATE_APPROVED = "GD3-L03 Late-approved";
    public static final String TEAM_NO_SUBMIT = "GD3-L04 No-submit";
    public static final String TEAM_LATE_REJECTED = "GD3-L05 Late-rejected";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.late.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Late Leader %02d".formatted(teamIndex);
    }
}
