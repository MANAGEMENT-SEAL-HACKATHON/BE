package com.sealhackathon.api.config.seed;

/** GĐ2 — registration_end đã qua → REGISTRATION_CLOSED. */
public final class Gd2RegistrationClosedSeedConstants {

    private Gd2RegistrationClosedSeedConstants() {
    }

    public static final String SLUG_GD2_REGISTRATION_CLOSED = "seal-gd2-registration-closed";

    public static final String TEAM_UNLOCKED = "GD2-RC01 ACTIVE chưa khóa";

    public static String studentEmail() {
        return "student.gd2.rc.leader01@fpt.edu.vn";
    }
}
