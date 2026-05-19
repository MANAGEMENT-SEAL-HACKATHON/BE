package com.sealhackathon.api.team_members.value_object;

/**
 * Trạng thái xác nhận tham gia đội của thành viên.
 *
 * <ul>
 *   <li>{@code PENDING}  — chờ thành viên xác nhận lời mời</li>
 *   <li>{@code ACCEPTED} — đã chấp nhận, đang trong đội</li>
 *   <li>{@code REJECTED} — từ chối lời mời</li>
 *   <li>{@code LEFT}     — đã rời đội</li>
 * </ul>
 */
public enum TeamMemberStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    LEFT
}
