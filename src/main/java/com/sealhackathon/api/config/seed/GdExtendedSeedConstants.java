package com.sealhackathon.api.config.seed;

/**
 * Slug / email bổ sung cho seed GĐ3, GĐ4 (tùy chọn), GĐ5, GĐ6.
 *
 * <p>Xem {@code docs/testing/seed-coverage-audit.md}.
 */
public final class GdExtendedSeedConstants {

    private GdExtendedSeedConstants() {
    }

    public static final String DEV_STUDENT_PASSWORD = Gd2SeedConstants.DEV_STUDENT_PASSWORD;

    /** GĐ3 — Sơ loại đang mở: active, đã phát đề, chưa lock/publish. */
    public static final String SLUG_GD3_PRELIM_OPEN = "seal-gd3-prelim-open";

    /** GĐ4 — Sơ loại đã lock, chưa publish: test ranking → publish → advance → activate CK. */
    public static final String SLUG_GD4_ADVANCE_READY = "seal-gd4-advance-ready";

    /** GĐ4 — Tiebreak 3-way + wildcard (chỉ khi {@code app.seed.gd4.enabled=true}). */
    public static final String SLUG_GD4_TIEBREAK = "seal-gd4-tiebreak-wildcard";

    /** GĐ5 — CK đang thi: active, chưa lock CK, hackathon {@code ONGOING}. */
    public static final String SLUG_GD5_FINAL_ACTIVE = "seal-gd5-final-active";

    /** GĐ6 — Sau lock CK: {@code PENDING_CONFIRM}, có giải mẫu. */
    public static final String SLUG_GD6_PENDING_CONFIRM = "seal-gd6-pending-confirm";

    public static final String TEAM_PREFIX_GD3 = "GD3-";
    public static final String TEAM_PREFIX_GD4 = "GD4-";
    public static final String TEAM_PREFIX_GD5 = "GD5-";
    public static final String TEAM_PREFIX_GD6 = "GD6-";

    public static final String GD3_TEAM_SUBMITTED = TEAM_PREFIX_GD3 + "01 SUBMITTED + scored";
    public static final String GD3_TEAM_LATE_PENDING = TEAM_PREFIX_GD3 + "02 LATE_PENDING";
    public static final String GD3_TEAM_LATE_APPROVED = TEAM_PREFIX_GD3 + "03 LATE_APPROVED";
    public static final String GD3_TEAM_NO_SUBMISSION = TEAM_PREFIX_GD3 + "04 chưa nộp bài";
    public static final String GD3_TEAM_T2_SCORED = TEAM_PREFIX_GD3 + "05 Track2 SUBMITTED+scored";
    public static final String GD3_TEAM_T2_PARTIAL = TEAM_PREFIX_GD3 + "06 Track2 chấm dở";

    public static final String GD3_STU_LEADER_01 = "student.gd3.leader01@fpt.edu.vn";
    public static final String GD3_STU_LEADER_02 = "student.gd3.leader02@fpt.edu.vn";
    public static final String GD3_STU_LEADER_03 = "student.gd3.leader03@fpt.edu.vn";
    public static final String GD3_STU_LEADER_04 = "student.gd3.leader04@fpt.edu.vn";
    public static final String GD3_STU_LEADER_05 = "student.gd3.leader05@fpt.edu.vn";
    public static final String GD3_STU_LEADER_06 = "student.gd3.leader06@fpt.edu.vn";

    public static final String GD4_TEAM_01 = TEAM_PREFIX_GD4 + "01 Tiebreak A";
    public static final String GD4_TEAM_02 = TEAM_PREFIX_GD4 + "02 Tiebreak A";
    public static final String GD4_TEAM_03 = TEAM_PREFIX_GD4 + "03 Tiebreak A";
    public static final String GD4_TEAM_04 = TEAM_PREFIX_GD4 + "04 Wildcard B";
    public static final String GD4_TEAM_05 = TEAM_PREFIX_GD4 + "05 Wildcard B";

    public static final String GD4_STU_01 = "student.gd4.leader01@fpt.edu.vn";
    public static final String GD4_STU_02 = "student.gd4.leader02@fpt.edu.vn";
    public static final String GD4_STU_03 = "student.gd4.leader03@fpt.edu.vn";
    public static final String GD4_STU_04 = "student.gd4.leader04@fpt.edu.vn";
    public static final String GD4_STU_05 = "student.gd4.leader05@fpt.edu.vn";

    /** GĐ4 advance-ready — 8 đội (6 advance + 2 eliminate trong payload mẫu). */
    public static final String GD4A_TEAM_01 = TEAM_PREFIX_GD4 + "A01 Rank1 Bảng A";
    public static final String GD4A_TEAM_02 = TEAM_PREFIX_GD4 + "A02 Rank2 Bảng A";
    public static final String GD4A_TEAM_03 = TEAM_PREFIX_GD4 + "A03 Rank1 Bảng B";
    public static final String GD4A_TEAM_04 = TEAM_PREFIX_GD4 + "A04 Rank2 Bảng B";
    public static final String GD4A_TEAM_05 = TEAM_PREFIX_GD4 + "A05 Rank1 Bảng C";
    public static final String GD4A_TEAM_06 = TEAM_PREFIX_GD4 + "A06 Rank2 Bảng C";
    public static final String GD4A_TEAM_07 = TEAM_PREFIX_GD4 + "A07 Rank1 Bảng D";
    public static final String GD4A_TEAM_08 = TEAM_PREFIX_GD4 + "A08 Rank2 Bảng D";

    public static final String GD4A_STU_01 = "student.gd4a.leader01@fpt.edu.vn";
    public static final String GD4A_STU_02 = "student.gd4a.leader02@fpt.edu.vn";
    public static final String GD4A_STU_03 = "student.gd4a.leader03@fpt.edu.vn";
    public static final String GD4A_STU_04 = "student.gd4a.leader04@fpt.edu.vn";
    public static final String GD4A_STU_05 = "student.gd4a.leader05@fpt.edu.vn";
    public static final String GD4A_STU_06 = "student.gd4a.leader06@fpt.edu.vn";
    public static final String GD4A_STU_07 = "student.gd4a.leader07@fpt.edu.vn";
    public static final String GD4A_STU_08 = "student.gd4a.leader08@fpt.edu.vn";

    public static final String GD5_TEAM_FINAL_SCORED = TEAM_PREFIX_GD5 + "01 CK SUBMITTED + scored";
    public static final String GD5_TEAM_FINAL_SUBMITTED = TEAM_PREFIX_GD5 + "02 CK SUBMITTED chưa chấm";
    public static final String GD5_TEAM_NO_FINAL_SUB = TEAM_PREFIX_GD5 + "03 ADVANCED chưa nộp CK";
    public static final String GD5_TEAM_ADV_ONLY = TEAM_PREFIX_GD5 + "04 ADVANCED chưa nộp CK (dự phòng)";

    public static final String GD5_STU_01 = "student.gd5.leader01@fpt.edu.vn";
    public static final String GD5_STU_02 = "student.gd5.leader02@fpt.edu.vn";
    public static final String GD5_STU_03 = "student.gd5.leader03@fpt.edu.vn";
    public static final String GD5_STU_04 = "student.gd5.leader04@fpt.edu.vn";

    public static final String GD6_TEAM_ADV_01 = TEAM_PREFIX_GD6 + "01 ADVANCED CK";
    public static final String GD6_TEAM_ADV_02 = TEAM_PREFIX_GD6 + "02 ADVANCED CK";
    public static final String GD6_TEAM_ADV_03 = TEAM_PREFIX_GD6 + "03 ADVANCED CK";

    public static final String GD6_STU_01 = "student.gd6.leader01@fpt.edu.vn";
    public static final String GD6_STU_02 = "student.gd6.leader02@fpt.edu.vn";
    public static final String GD6_STU_03 = "student.gd6.leader03@fpt.edu.vn";
}
