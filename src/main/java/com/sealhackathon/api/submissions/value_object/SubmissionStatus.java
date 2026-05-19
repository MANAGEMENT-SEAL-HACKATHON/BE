package com.sealhackathon.api.submissions.value_object;

/**
 * [BC-06] Trạng thái bài nộp.
 *
 * <ul>
 *   <li>{@code SUBMITTED}     — nộp đúng deadline</li>
 *   <li>{@code LATE}          — nộp sau deadline (Sơ loại, tự động)</li>
 *   <li>{@code LATE_PENDING}  — nộp trễ, chờ Coordinator duyệt (Sơ loại only)</li>
 *   <li>{@code LATE_APPROVED} — nộp trễ, đã được duyệt cho điểm</li>
 *   <li>{@code REJECTED}      — bị từ chối (vd: Chung kết HARD_LOCK)</li>
 *   <li>{@code ACCEPTED}      — đã được chấp nhận chính thức</li>
 * </ul>
 */
public enum SubmissionStatus {
    SUBMITTED,
    LATE,
    LATE_PENDING,
    LATE_APPROVED,
    REJECTED,
    ACCEPTED
}
