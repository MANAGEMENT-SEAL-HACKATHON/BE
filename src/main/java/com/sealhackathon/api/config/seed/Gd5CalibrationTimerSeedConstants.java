package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ5 — calibration OPEN trên CK + queue timer.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile C
 */
public final class Gd5CalibrationTimerSeedConstants {

    private Gd5CalibrationTimerSeedConstants() {
    }

    public static final String SLUG_GD5_CALIBRATION_TIMER = "seal-gd5-calibration-timer";

    public static final String[] TEAM_NAMES = {
            "GD5-C01 CK calibration",
            "GD5-C02 CK calibration",
            "GD5-C03 CK PRESENTING",
            "GD5-C04 CK WAITING"
    };

    public static String studentEmail(int teamIndex) {
        return "student.gd5c.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Calib Leader %02d".formatted(teamIndex);
    }
}
