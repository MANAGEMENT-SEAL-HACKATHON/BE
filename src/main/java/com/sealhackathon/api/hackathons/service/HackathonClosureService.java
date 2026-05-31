package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.ConfirmHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;

import java.util.List;

public interface HackathonClosureService {

    /** FR-33 — PENDING_CONFIRM → FINISHED. */
    HackathonResponse confirm(Integer hackathonId, ConfirmHackathonRequest req);

    /** FR-31 / FR-33A — XH Team CK (view, không persist). */
    List<FinalTeamRankingItemResponse> teamRankings(Integer hackathonId);
}
