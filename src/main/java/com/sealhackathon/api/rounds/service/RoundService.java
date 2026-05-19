package com.sealhackathon.api.rounds.service;

import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;

import java.util.List;

/**
 * FR-02 — CRUD Round dưới Hackathon. Activate xem {@link RoundActivationService} (FR-07B).
 */
public interface RoundService {

    RoundResponse createByHackathon(Integer hackathonId, CreateRoundRequest req);

    List<RoundSummaryResponse> listByHackathon(Integer hackathonId);

    RoundResponse getById(Integer id);

    RoundResponse update(Integer id, UpdateRoundRequest req);

    Integer delete(Integer id);
}
