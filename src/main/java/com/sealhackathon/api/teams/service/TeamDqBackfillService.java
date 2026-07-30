package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;

import java.util.List;

/**
 * GĐ4 — sau DQ đội đã ADVANCED: đôn đội kế cùng bảng trước khi CK activate.
 */
public interface TeamDqBackfillService {

    /**
     * @param eliminatedTeam đội vừa bị DQ (đã set ELIMINATED)
     * @param previouslyAdvancedPrelimSeats ghế ADVANCED prelim trước khi mutate
     * @param reason lý do DQ (non-blank)
     */
    void afterEliminate(Team eliminatedTeam,
                        List<AdvancedPrelimSeat> previouslyAdvancedPrelimSeats,
                        String reason);

    record AdvancedPrelimSeat(TeamRoundTrack trt) {}
}
