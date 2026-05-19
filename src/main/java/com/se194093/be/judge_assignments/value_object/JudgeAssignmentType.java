package com.se194093.be.judge_assignments.value_object;

/**
 * [BC-07] Phân loại JudgeAssignment.
 *
 * <ul>
 *   <li>{@code NORMAL}         — Judge chấm thường ở Sơ loại/Bán kết (Track).</li>
 *   <li>{@code HEAD}           — Trưởng nhóm Judge của Track (quyền giải quyết tranh chấp).</li>
 *   <li>{@code CALIBRATION}    — Judge tham gia phiên hiệu chuẩn điểm.</li>
 *   <li>{@code FINAL_EXTERNAL} — Judge Chung kết, BẮT BUỘC EXTERNAL.
 *       Enforce ở DB trigger {@code trg_check_mentor_judge_conflict_ins} —
 *       tham chiếu {@code docs/db/schema-v3.0-mysql.md} §5.4.</li>
 * </ul>
 */
public enum JudgeAssignmentType {
    NORMAL,
    HEAD,
    CALIBRATION,
    FINAL_EXTERNAL
}
