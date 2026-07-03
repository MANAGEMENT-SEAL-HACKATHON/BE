package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — CK thiếu criteria → activate {@code ROUND_NO_CRITERIA}.
 */
public final class Gd4CkNoCriteriaSeedConstants {

    private Gd4CkNoCriteriaSeedConstants() {
    }

    public static final String SLUG_GD4_CK_NO_CRITERIA = "seal-gd4-ck-no-criteria";

    public static String studentEmail(int teamIndex) {
        return "student.gd4nc.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 NC Leader %02d".formatted(teamIndex);
    }
}
