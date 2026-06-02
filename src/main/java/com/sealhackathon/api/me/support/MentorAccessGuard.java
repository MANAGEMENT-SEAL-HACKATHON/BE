package com.sealhackathon.api.me.support;

import org.springframework.stereotype.Component;

/**
 * Kiểm tra phạm vi Mentor (track/team assignment).
 * TODO: FR-M-05..19 — mentor_assignments, scoring_locked for scores view.
 */
@Component
public class MentorAccessGuard {

    public void assertAssignedToTeam(Integer teamId) {
        // TODO: FR-M-06/10 — mentor_team_assignments
    }
}
