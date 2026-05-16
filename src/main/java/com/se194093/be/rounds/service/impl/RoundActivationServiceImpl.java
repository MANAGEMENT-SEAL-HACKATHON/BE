package com.se194093.be.rounds.service.impl;

import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.service.RoundActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-06b-activate.md}.
 *
 * <p>Inject: RoundRepository, CriteriaRepository, RoundMapper, AuditService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoundActivationServiceImpl implements RoundActivationService {

    @Override
    public RoundResponse activate(Integer roundId, String note) {
        // TODO Dev:
        //   round = roundRepo.findById(roundId) or 404
        //   total = criteriaRepo.sumWeightExcludingPenalty(roundId)
        //   if total.isEmpty() → throw BusinessRuleException(ROUND_NO_CRITERIA, "Round chưa có Criteria")
        //   if Math.abs(total.get() - WEIGHT_TARGET) > WEIGHT_TOLERANCE:
        //       throw BusinessRuleException(ROUND_WEIGHT_NOT_ONE,
        //           "Tổng trọng số = %.4f, cần chỉnh về 1.0".formatted(total.get()),
        //           Map.of("currentTotal", total.get(), "missing", 1.0 - total.get()))
        //   roundRepo.deactivateOtherRoundsInTrack(round.getTrack().getId(), roundId)
        //   round.setIsActive(true)
        //   roundRepo.save(round)
        //   audit.log(AuditAction.ROUND_ACTIVATE, "rounds", roundId,
        //             Map.of("trackId", round.getTrack().getId(),
        //                    "note", note,
        //                    "weightTotal", total.get()))
        //   return mapper.toResponse(round)
        throw new UnsupportedOperationException("FR-06B PATCH /rounds/{id}/activate - to be implemented");
    }
}
