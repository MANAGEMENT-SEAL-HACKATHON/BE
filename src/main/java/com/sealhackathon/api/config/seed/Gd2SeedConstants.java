package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed MF-02 Giai đoạn 2 — Teams & Lottery (dev / Postman).
 *
 * <p>Sau {@code spring.profiles.active=dev}, xem log {@code [Gd2DataSeeder]}.
 * Hackathon mục tiêu: {@link Gd1SeedConstants#SLUG_ONGOING} (SEAL Spring 2026, ONGOING).
 */
public final class Gd2SeedConstants {

    private Gd2SeedConstants() {
    }

    public static final String DEV_STUDENT_PASSWORD = "Student@dev1";

    /** Marker — mọi đội seed GĐ2 đều bắt đầu bằng prefix này. */
    public static final String TEAM_NAME_PREFIX = "GD2-";

    public static final String TEAM_01 = TEAM_NAME_PREFIX + "01 Chờ duyệt (1 người)";
    public static final String TEAM_02 = TEAM_NAME_PREFIX + "02 Chờ duyệt (2 ACCEPTED + 1 PENDING)";
    public static final String TEAM_03 = TEAM_NAME_PREFIX + "03 Sẵn duyệt ACTIVE (4 người)";
    public static final String TEAM_04 = TEAM_NAME_PREFIX + "04 ACTIVE + bốc thăm Track 1";
    public static final String TEAM_05 = TEAM_NAME_PREFIX + "05 ACTIVE đã khóa + bốc thăm";
    public static final String TEAM_06 = TEAM_NAME_PREFIX + "06 REJECTED";
    public static final String TEAM_07 = TEAM_NAME_PREFIX + "07 ACTIVE chưa mentor (bốc thăm)";
    public static final String TEAM_08 = TEAM_NAME_PREFIX + "08 ELIMINATED";
    public static final String TEAM_09 = TEAM_NAME_PREFIX + "09 ACTIVE bốc thăm Track 2";

    // --- Sinh viên APPROVED (password chung DEV_STUDENT_PASSWORD) ---

    public static final String STU_HCM_LEADER_01 = "student.gd2.hcm.leader01@fpt.edu.vn";
    public static final String STU_HN_LEADER_02 = "student.gd2.hn.leader02@fpt.edu.vn";
    public static final String STU_HCM_03 = "student.gd2.hcm.member03@fpt.edu.vn";
    public static final String STU_HCM_04 = "student.gd2.hcm.member04@fpt.edu.vn";
    public static final String STU_EXT_PENDING = "student.gd2.ext.pending@gmail.com";
    public static final String STU_HCM_LEADER_03 = "student.gd2.hcm.leader03@fpt.edu.vn";
    public static final String STU_HCM_06 = "student.gd2.hcm.member06@fpt.edu.vn";
    public static final String STU_HN_07 = "student.gd2.hn.member07@fpt.edu.vn";
    public static final String STU_EXT_08 = "student.gd2.ext.member08@gmail.com";
    public static final String STU_EXT_LEADER_04 = "student.gd2.ext.leader04@gmail.com";
    public static final String STU_HCM_10 = "student.gd2.hcm.member10@fpt.edu.vn";
    public static final String STU_HN_11 = "student.gd2.hn.member11@fpt.edu.vn";
    public static final String STU_HCM_LEADER_05 = "student.gd2.hcm.leader05@fpt.edu.vn";
    public static final String STU_HCM_12 = "student.gd2.hcm.member12@fpt.edu.vn";
    public static final String STU_EXT_13 = "student.gd2.ext.member13@gmail.com";
    public static final String STU_HN_14 = "student.gd2.hn.member14@fpt.edu.vn";
    public static final String STU_HCM_LEADER_06 = "student.gd2.hcm.leader06@fpt.edu.vn";
    public static final String STU_HCM_15 = "student.gd2.hcm.member15@fpt.edu.vn";
    public static final String STU_EXT_16 = "student.gd2.ext.member16@gmail.com";
    public static final String STU_HCM_LEADER_07 = "student.gd2.hcm.leader07@fpt.edu.vn";
    public static final String STU_HN_17 = "student.gd2.hn.member17@fpt.edu.vn";
    public static final String STU_EXT_18 = "student.gd2.ext.member18@gmail.com";
    public static final String STU_HCM_LEADER_08 = "student.gd2.hcm.leader08@fpt.edu.vn";
    public static final String STU_HCM_19 = "student.gd2.hcm.member19@fpt.edu.vn";
    public static final String STU_HN_20 = "student.gd2.hn.member20@fpt.edu.vn";
    public static final String STU_EXT_LEADER_09 = "student.gd2.ext.leader09@gmail.com";
    public static final String STU_HCM_21 = "student.gd2.hcm.member21@fpt.edu.vn";
    public static final String STU_EXT_22 = "student.gd2.ext.member22@gmail.com";
    public static final String STU_EXT_23 = "student.gd2.ext.member23@gmail.com";

    /** Chưa thuộc đội — dùng test mời / USER_IN_ANOTHER_TEAM. */
    public static final String STU_POOL_FREE = "student.gd2.pool.free@gmail.com";
    public static final String STU_POOL_BUSY = "student.gd2.pool.busy@gmail.com";

    public static final String[] ALL_STUDENT_EMAILS = {
            STU_HCM_LEADER_01, STU_HN_LEADER_02, STU_HCM_03, STU_HCM_04, STU_EXT_PENDING,
            STU_HCM_LEADER_03, STU_HCM_06, STU_HN_07, STU_EXT_08,
            STU_EXT_LEADER_04, STU_HCM_10, STU_HN_11,
            STU_HCM_LEADER_05, STU_HCM_12, STU_EXT_13, STU_HN_14,
            STU_HCM_LEADER_06, STU_HCM_15, STU_EXT_16,
            STU_HCM_LEADER_07, STU_HN_17, STU_EXT_18,
            STU_HCM_LEADER_08, STU_HCM_19, STU_HN_20,
            STU_EXT_LEADER_09, STU_HCM_21, STU_EXT_22, STU_EXT_23,
            STU_POOL_FREE, STU_POOL_BUSY
    };
}
