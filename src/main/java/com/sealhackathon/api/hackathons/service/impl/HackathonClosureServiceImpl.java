package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.hackathons.dto.request.ConfirmHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.query.FinalRankingQueryService;
import com.sealhackathon.api.hackathons.service.HackathonClosureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HackathonClosureServiceImpl implements HackathonClosureService {

    private final FinalRankingQueryService finalRankingQueryService;

    @Override
    public HackathonResponse confirm(Integer hackathonId, ConfirmHackathonRequest req) {
        // TODO: FR-33 — gate PENDING_CONFIRM, final round scoring_locked, NO_PRIZES_RECORDED
        // TODO: FR-33 — SELECT FOR UPDATE hackathons; status → FINISHED; audit FR-36 (ip/ua)
        // TODO: FR-33 — publish HackathonFinishedEvent after commit
        return HackathonResponse.builder().id(hackathonId).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinalTeamRankingItemResponse> teamRankings(Integer hackathonId) {
        return finalRankingQueryService.teamRankingsForHackathon(hackathonId);
    }
}
