package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ5 — CK active nhưng deadline đã qua (HARD_LOCK → {@code REJECTED}).
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile E
 */
public final class Gd5LateHardlockSeedConstants {

    private Gd5LateHardlockSeedConstants() {
    }

    public static final String SLUG_GD5_LATE_HARDLOCK = "seal-gd5-late-hardlock";

    public static final String[] TEAM_NAMES = {
            "GD5-LH01 On-time missed",
            "GD5-LH02 Late submit",
            "GD5-LH03 Late submit",
            "GD5-LH04 Late submit"
    };

    public static String studentEmail(int teamIndex) {
        return "student.gd5lh.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD5 Late Hardlock %02d".formatted(teamIndex);
    }
}
