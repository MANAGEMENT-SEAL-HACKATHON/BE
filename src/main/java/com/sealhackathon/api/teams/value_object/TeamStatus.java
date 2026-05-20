package com.sealhackathon.api.teams.value_object;

/**
 * Trạng thái đội thi trong Hackathon.
 *
 * <ul>
 *   <li>{@code PENDING}    — chờ phê duyệt</li>
 *   <li>{@code ACTIVE}     — đang tham gia</li>
 *   <li>{@code ELIMINATED} — bị loại (vd: vi phạm, không nộp bài)</li>
 *   <li>{@code REJECTED}   — bị từ chối phê duyệt</li>
 * </ul>
 */
public enum TeamStatus {
    PENDING,
    ACTIVE,
    ELIMINATED,
    REJECTED
}
