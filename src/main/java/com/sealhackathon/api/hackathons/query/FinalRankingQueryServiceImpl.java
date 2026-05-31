package com.sealhackathon.api.hackathons.query;

import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinalRankingQueryServiceImpl implements FinalRankingQueryService {

    @Override
    public List<FinalTeamRankingItemResponse> teamRankingsForHackathon(Integer hackathonId) {
        // TODO: FR-31/33A — query v_round_leaderboard JOIN rounds WHERE is_final=TRUE
        //       AND teams.status=ACTIVE; gate hackathon.status >= PENDING_CONFIRM
        return Collections.emptyList();
    }
}
