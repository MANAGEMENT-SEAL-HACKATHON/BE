package com.sealhackathon.api.config.seed;

/** GĐ5 — CK active, không có judge assign → JUDGE_NOT_ASSIGNED. */
public final class Gd5JudgeEdgeSeedConstants {

    private Gd5JudgeEdgeSeedConstants() {
    }

    public static final String SLUG_GD5_JUDGE_EDGE = "seal-gd5-judge-edge";

    public static final String TEAM_NAME = "GD5-J01 SUBMITTED no judge";

    public static String studentEmail() {
        return "student.gd5j.leader01@fpt.edu.vn";
    }
}
