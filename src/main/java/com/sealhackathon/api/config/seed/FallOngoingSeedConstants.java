package com.sealhackathon.api.config.seed;

/** GĐ2 Fall — leader tự chọn track (FR-U-15-F). */
public final class FallOngoingSeedConstants {

    private FallOngoingSeedConstants() {
    }

    public static final String SLUG_FALL_ONGOING = "seal-fall-ongoing-2026";

    public static String studentEmail(int idx) {
        return "student.fall.t%02d.leader@fpt.edu.vn".formatted(idx);
    }

    public static String teamName(int idx) {
        return "FALL-T%02d".formatted(idx);
    }
}
