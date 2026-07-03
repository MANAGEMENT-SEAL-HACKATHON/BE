package com.sealhackathon.api.config.seed;

/** GĐ2 — đội ACTIVE chưa lock → lottery fail TEAM_NOT_LOCKED. */
public final class Gd2LotteryNotLockedSeedConstants {

    private Gd2LotteryNotLockedSeedConstants() {
    }

    public static final String SLUG_GD2_LOTTERY_NOT_LOCKED = "seal-gd2-lottery-not-locked";

    public static String studentEmail(int idx) {
        return "student.gd2nl.t%02d.leader@fpt.edu.vn".formatted(idx);
    }

    public static String teamName(int idx) {
        return "GD2-NL-T%02d".formatted(idx);
    }
}
