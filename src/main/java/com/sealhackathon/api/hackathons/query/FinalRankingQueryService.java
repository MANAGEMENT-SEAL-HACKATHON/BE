package com.sealhackathon.api.hackathons.query;

import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;

import java.util.List;

/** FR-31 / FR-33A — XH Team round FINAL (view, không persist). */
public interface FinalRankingQueryService {

    List<FinalTeamRankingItemResponse> teamRankingsForHackathon(Integer hackathonId);
}
