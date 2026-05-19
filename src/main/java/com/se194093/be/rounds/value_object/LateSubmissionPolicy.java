package com.se194093.be.rounds.value_object;

/**
 * [BC-01] Chính sách nộp bài trễ cho mỗi Round.
 *
 * <ul>
 *   <li>{@code ALLOW_LATE_PENDING} — Sơ loại: bài nộp sau deadline đi vào
 *       trạng thái {@code LATE_PENDING}, chờ Coordinator xét duyệt.</li>
 *   <li>{@code HARD_LOCK} — Chung kết: deadline cứng tuyệt đối.
 *       Bài nộp trễ tự động {@code REJECTED} (enforce bởi
 *       {@code trg_check_submission_round_is_final}).</li>
 * </ul>
 */
public enum LateSubmissionPolicy {
    ALLOW_LATE_PENDING,
    HARD_LOCK
}
