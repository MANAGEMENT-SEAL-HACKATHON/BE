package com.sealhackathon.api.config.seed;

/** GĐ3 — mentor portal với đội đã gán mentor. */
public final class Gd3MentorPortalSeedConstants {

    private Gd3MentorPortalSeedConstants() {
    }

    public static final String SLUG_GD3_MENTOR_PORTAL = "seal-gd3-mentor-portal";

    public static String studentEmail(int idx) {
        return "student.gd3mp.t%02d.leader@fpt.edu.vn".formatted(idx);
    }

    public static String teamName(int idx) {
        return "GD3-MP-T%02d".formatted(idx);
    }
}
