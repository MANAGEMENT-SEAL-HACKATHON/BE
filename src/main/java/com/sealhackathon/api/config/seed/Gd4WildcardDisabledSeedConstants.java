package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — wildcard tắt → {@code GET /wildcard-candidates} trả {@code candidates=[]}.
 */
public final class Gd4WildcardDisabledSeedConstants {

    private Gd4WildcardDisabledSeedConstants() {
    }

    public static final String SLUG_GD4_WILDCARD_DISABLED = "seal-gd4-wildcard-disabled";

    public static String studentEmail(int teamIndex) {
        return "student.gd4wd.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 WD Leader %02d".formatted(teamIndex);
    }
}
