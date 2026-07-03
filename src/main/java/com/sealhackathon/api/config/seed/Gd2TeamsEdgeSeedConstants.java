package com.sealhackathon.api.config.seed;

/**
 * GĐ2 negative matrix — 9 đội đa trạng thái (mf02/05).
 */
public final class Gd2TeamsEdgeSeedConstants {

    private Gd2TeamsEdgeSeedConstants() {
    }

    public static final String SLUG_GD2_TEAMS_EDGE = "seal-gd2-teams-edge";

    public static final String TEAM_01 = "GD2-T01 Chờ duyệt (1 người)";
    public static final String TEAM_02 = "GD2-T02 Chờ duyệt (2 ACCEPTED + 1 PENDING)";
    public static final String TEAM_03 = "GD2-T03 Sẵn duyệt ACTIVE (4 người)";
    public static final String TEAM_04 = "GD2-T04 ACTIVE + bốc thăm Track 1";
    public static final String TEAM_05 = "GD2-T05 ACTIVE đã khóa + bốc thăm";
    public static final String TEAM_06 = "GD2-T06 REJECTED";
    public static final String TEAM_07 = "GD2-T07 ACTIVE chưa mentor (bốc thăm)";
    public static final String TEAM_08 = "GD2-T08 ELIMINATED";
    public static final String TEAM_09 = "GD2-T09 ACTIVE bốc thăm Track 2";

    public static String studentEmail(String local) {
        return "student.gd2." + local + "@fpt.edu.vn";
    }

    public static String extEmail(String local) {
        return "student.gd2." + local + "@gmail.com";
    }
}
