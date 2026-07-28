package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.teams.entity.Team;

/**
 * Giải phóng thành viên khi đội bị REJECTED / giải tán / merge-source.
 * Không dùng cho {@code ELIMINATED} — giữ lịch sử thi đấu.
 */
public interface TeamMembershipReleaseService {

    /**
     * @param withdrawRegistration rút đăng ký hackathon cho member đã ACCEPTED (policy close-reg)
     */
    void releaseMembers(Team team, String reason, boolean withdrawRegistration);
}
