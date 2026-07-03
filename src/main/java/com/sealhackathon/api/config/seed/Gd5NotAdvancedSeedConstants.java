package com.sealhackathon.api.config.seed;

/** GĐ5 — team không ADVANCED vào CK → TEAM_NOT_IN_ROUND. */
public final class Gd5NotAdvancedSeedConstants {

    private Gd5NotAdvancedSeedConstants() {
    }

    public static final String SLUG_GD5_NOT_ADVANCED = "seal-gd5-not-advanced";

    public static final String TEAM_ADVANCED = "GD5-NA01 ADVANCED CK";
    public static final String TEAM_NOT_ADVANCED = "GD5-NA02 NOT in CK round";

    public static String studentEmail(int idx) {
        return "student.gd5na.leader%02d@fpt.edu.vn".formatted(idx);
    }
}
