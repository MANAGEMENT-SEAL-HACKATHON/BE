package com.sealhackathon.api.me.support;

import org.springframework.stereotype.Component;

/**
 * Kiểm tra phân công Judge + HEAD tiebreak (FR-J-05..26).
 *
 * <p><b>Status:</b> placeholder — no call sites yet. Judge portal enforces scope via
 * {@code JudgePortalServiceImpl} and assignment repositories. Implement here when
 * consolidating judge access checks (Phase 5).
 */
@Component
public class JudgeAccessGuard {

    public void assertAssignedToTrack(Integer trackId) {
        // Deferred — see class Javadoc
    }

    public void assertHeadJudgeForRound(Integer roundId) {
        // Deferred — tiebreak vote allowed for any assigned judge in round scope
    }
}
