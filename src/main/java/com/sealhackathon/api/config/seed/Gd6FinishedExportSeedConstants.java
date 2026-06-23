package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ6 — {@code FINISHED}, rankings + export CSV.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile C
 */
public final class Gd6FinishedExportSeedConstants {

    private Gd6FinishedExportSeedConstants() {
    }

    public static final String SLUG_GD6_FINISHED_EXPORT = "seal-gd6-finished-export";

    public static final String[] TEAM_NAMES = {
            "GD6-F01 FINISHED FIRST",
            "GD6-F02 FINISHED SECOND",
            "GD6-F03 FINISHED THIRD"
    };

    public static final float[] TEAM_SCORES = {9.5f, 8.9f, 8.3f};

    public static String studentEmail(int teamIndex) {
        return "student.gd6f.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD6 Finished Leader %02d".formatted(teamIndex);
    }
}
