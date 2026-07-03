package com.sealhackathon.api.config.seed;

/** GĐ6 — đã có FIRST prize → PRIZE_DUPLICATE khi POST thêm. */
public final class Gd6PrizeDuplicateSeedConstants {

    private Gd6PrizeDuplicateSeedConstants() {
    }

    public static final String SLUG_GD6_PRIZE_DUPLICATE = "seal-gd6-prize-duplicate";

    public static final String TEAM_FIRST = "GD6-PD01 FIRST prize holder";

    public static String studentEmail() {
        return "student.gd6pd.leader01@fpt.edu.vn";
    }
}
