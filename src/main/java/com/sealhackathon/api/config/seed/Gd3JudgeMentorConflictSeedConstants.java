package com.sealhackathon.api.config.seed;

/**
 * Seed GĐ3 — user vừa Mentor vừa Judge cùng track → {@code CONFLICT_MENTOR_JUDGE_SAME_TRACK}.
 */
public final class Gd3JudgeMentorConflictSeedConstants {

    private Gd3JudgeMentorConflictSeedConstants() {
    }

    public static final String SLUG_GD3_JUDGE_MENTOR_CONFLICT = "seal-gd3-judge-mentor-conflict";

    public static final String TEAM_PRESENTING = "GD3-MC01 Presenting";
    public static final String TEAM_WAITING = "GD3-MC02 Waiting";

    public static String studentEmail(int teamIndex) {
        return "student.gd3.mc.leader%02d@fpt.edu.vn".formatted(teamIndex);
    }

    public static String studentDisplayName(int teamIndex) {
        return "GD3 MC Leader %02d".formatted(teamIndex);
    }
}
