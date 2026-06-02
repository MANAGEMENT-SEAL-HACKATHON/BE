package com.sealhackathon.api.me.support;

import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền Student trên team/round (ownership).
 * TODO: FR-U-15..21 — team membership, hackathon registration, round window.
 */
@Component
public class StudentAccessGuard {

    public void assertTeamMember(Integer teamId) {
        // TODO: FR-U-15 — current user in team_members
    }

    public void assertRegisteredForHackathon(Integer hackathonId) {
        // TODO: FR-U-06 — hackathon_registrations
    }
}
