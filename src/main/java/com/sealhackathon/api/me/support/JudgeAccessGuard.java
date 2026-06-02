package com.sealhackathon.api.me.support;

import org.springframework.stereotype.Component;

/**
 * Kiểm tra phân công Judge + HEAD tiebreak.
 * TODO: FR-J-05..26 — judge_assignments scope, HEAD role.
 */
@Component
public class JudgeAccessGuard {

    public void assertAssignedToTrack(Integer trackId) {
        // TODO: FR-J-05 — judge_track_assignments
    }

    public void assertHeadJudgeForRound(Integer roundId) {
        // TODO: FR-J-22/23 — HEAD tiebreak vote
    }
}
