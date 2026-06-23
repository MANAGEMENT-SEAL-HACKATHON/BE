package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — calibration OPEN + queue timer PAUSED / QA.
 *
 * <p>Slug: {@link #SLUG_GD3_CALIBRATION_TIMER}
 */
public final class Gd3CalibrationTimerSeedConstants {

    private Gd3CalibrationTimerSeedConstants() {
    }

    public static final String SLUG_GD3_CALIBRATION_TIMER = "seal-gd3-calibration-timer";

    public static final String[] TEAM_NAMES = {
            "GD3-CT01 Calib-sample",
            "GD3-CT02 Queue-done",
            "GD3-CT03 Timer-paused",
            "GD3-CT04 Timer-qa",
            "GD3-CT05 Waiting"
    };

    public static final String[] GROUPS = {"BANG-A", "BANG-A", "BANG-A", "BANG-B", "BANG-B"};

    public static String studentEmail(int teamIndex) {
        return "student.gd3.calib.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 Calib Leader %02d".formatted(teamIndex);
    }
}
