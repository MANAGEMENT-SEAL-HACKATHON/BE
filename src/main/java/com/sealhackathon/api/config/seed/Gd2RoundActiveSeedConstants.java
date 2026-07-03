package com.sealhackathon.api.config.seed;

/** GĐ2 — prelim active + lottery xong → PATCH lottery → ROUND_ALREADY_ACTIVE (B-N2). */
public final class Gd2RoundActiveSeedConstants {

    private Gd2RoundActiveSeedConstants() {
    }

    public static final String SLUG_GD2_ROUND_ACTIVE = "seal-gd2-round-active";

    public static String studentEmail(int index) {
        return "student.gd2.ra.leader%02d@fpt.edu.vn".formatted(index);
    }

    public static String teamName(int index) {
        return "GD2-RA-T%02d".formatted(index);
    }
}
