package com.se194093.be.rounds.service.impl;

import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;
import com.se194093.be.rounds.service.RoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-03-rounds.md}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    @Override
    public RoundResponse create(Integer trackId, CreateRoundRequest req) {
        // TODO Dev:
        //  1. findTrack(trackId) or 404
        //  2. validate submissionDeadline > submissionOpen (nếu submissionOpen != null) AND > NOW()
        //     → 422 ROUND_DEADLINE_INVALID
        //  3. save Round (is_active=false, scoring_locked=false, force_locked=false)
        //  4. audit ROUND_CREATE
        //  5. KHÔNG validate weight Criteria!
        throw new UnsupportedOperationException("FR-03 POST /rounds - to be implemented");
    }

    @Override
    public List<RoundSummaryResponse> listByTrack(Integer trackId) {
        // TODO Dev:
        //  - rounds = repo.findByTrackIdOrderBySequenceOrderAsc(trackId)
        //  - For each: criteriaCount = criteriaRepo.countByRoundIdAndTypeNot(roundId, PENALTY)
        //              currentWeightTotal = criteriaRepo.sumWeightByRoundId(roundId)
        //  - map sang RoundSummaryResponse
        throw new UnsupportedOperationException("FR-03 GET /rounds - to be implemented");
    }

    @Override
    public RoundResponse getById(Integer id) {
        throw new UnsupportedOperationException("FR-03 GET /rounds/{id} - to be implemented");
    }

    @Override
    public RoundResponse update(Integer id, UpdateRoundRequest req) {
        // TODO Dev:
        //  - findById → 404
        //  - validate deadline; validate forceLocked + reason
        //  - detect transition scoringLocked false→true → audit ROUND_LOCK
        //  - detect transition forceLocked false→true → audit ROUND_FORCE_LOCK
        //  - applyUpdate; save; audit ROUND_UPDATE { before, after }
        throw new UnsupportedOperationException("FR-03 PUT /rounds/{id} - to be implemented");
    }

    @Override
    public Integer delete(Integer id) {
        // TODO Dev:
        //  - findById → 404
        //  - guard !exists submissions(round_id=id) → 409 ROUND_HAS_SUBMISSIONS
        //  - guard !is_active → 409 ROUND_ANOTHER_ACTIVE (yêu cầu deactivate trước)
        //  - delete (cascade criteria + judge_assignments)
        //  - notify judges bị hủy
        //  - audit ROUND_DELETE
        throw new UnsupportedOperationException("FR-03 DELETE /rounds/{id} - to be implemented");
    }
}
