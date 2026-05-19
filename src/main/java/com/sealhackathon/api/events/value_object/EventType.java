package com.sealhackathon.api.events.value_object;

/**
 * [BC-09] Loại sự kiện Hackathon — đã LOẠI BỎ {@code TEAM_MEETING}.
 *
 * <p>Họp đội nội bộ được quản lý ở phạm vi đội (team_meetings entity riêng nếu có
 * nhu cầu sau này), không thuộc lịch tổng của Hackathon.
 */
public enum EventType {
    KICKOFF,
    WORKSHOP,
    PRESENTATION,
    AWARDS,
    OTHER
}
