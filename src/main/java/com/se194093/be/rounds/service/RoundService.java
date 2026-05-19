package com.se194093.be.rounds.service;

import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;

import java.util.List;

/**
 * FR-02 — CRUD Round dưới Hackathon. Activate xem {@link RoundActivationService} (FR-07B).
 */
public interface RoundService {

    RoundResponse createByHackathon(Integer hackathonId, CreateRoundRequest req);

    /** @deprecated delegate — dùng {@link #createByHackathon} */
    @Deprecated
    RoundResponse create(Integer trackId, CreateRoundRequest req);

    List<RoundSummaryResponse> listByHackathon(Integer hackathonId);

    /** @deprecated legacy */
    @Deprecated
    List<RoundSummaryResponse> listByTrack(Integer trackId);

    RoundResponse getById(Integer id);

    RoundResponse update(Integer id, UpdateRoundRequest req);

    Integer delete(Integer id);
}
