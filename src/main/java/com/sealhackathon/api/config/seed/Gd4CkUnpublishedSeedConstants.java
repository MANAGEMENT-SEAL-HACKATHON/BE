package com.sealhackathon.api.config.seed;

/** GĐ4 — SL locked chưa publish, có guest judge CK → activate CK fail RESULT_NOT_PUBLISHED. */
public final class Gd4CkUnpublishedSeedConstants {

    private Gd4CkUnpublishedSeedConstants() {
    }

    public static final String SLUG_GD4_CK_UNPUBLISHED = "seal-gd4-ck-unpublished";

    public static String studentEmail(int idx) {
        return "student.gd4cu.t%02d.leader@fpt.edu.vn".formatted(idx);
    }

    public static String studentDisplayName(int idx) {
        return "GD4-CU T%02d Leader".formatted(idx);
    }

    public static final String[] TEAM_NAMES = {
            "GD4-CU-T01", "GD4-CU-T02", "GD4-CU-T03", "GD4-CU-T04",
    };

    public static final float[] TEAM_SCORES = {9.0f, 7.0f, 9.0f, 7.0f};

    public static final String[] GROUPS = {"BANG-A", "BANG-A", "BANG-B", "BANG-B"};

    /** Indices trong {@link #TEAM_NAMES} được advance sang CK (readiness READY, activate → RESULT_NOT_PUBLISHED). */
    public static final int[] ADVANCED_TEAM_INDICES = {0, 2};
}
