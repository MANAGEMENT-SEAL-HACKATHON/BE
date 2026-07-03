package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ4 — POST assign judge SL lên CK → warnings {@code JUDGE_PARTICIPATED_IN_PRELIM},
 * {@code MIN_FINAL_JUDGES_NOT_MET}.
 */
public final class Gd4JudgeAssignWarningsSeedConstants {

    private Gd4JudgeAssignWarningsSeedConstants() {
    }

    public static final String SLUG_GD4_JUDGE_ASSIGN_WARNINGS = "seal-gd4-judge-assign-warnings";

    public static String studentEmail(int teamIndex) {
        return "student.gd4jw.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD4 JW Leader %02d".formatted(teamIndex);
    }
}
