package com.se194093.be.rounds.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.mapper.RoundMapper;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.rounds.service.RoundActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * FR-06B safety-net activate Round: gate weight=1.0 (excluding PENALTY), deactivate sibling rounds
 * trong cùng track, audit ROUND_ACTIVATE.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RoundActivationServiceImpl implements RoundActivationService {

    private final RoundRepository roundRepository;
    private final CriteriaRepository criteriaRepository;
    private final RoundMapper roundMapper;
    private final AuditService auditService;

    @Override
    public RoundResponse activate(Integer roundId, String note) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        Optional<Double> totalOpt = criteriaRepository.sumWeightExcludingPenalty(roundId);
        if (totalOpt.isEmpty() || criteriaRepository.countNormalByRoundId(roundId) == 0) {
            throw new BusinessRuleException(ErrorCode.ROUND_NO_CRITERIA,
                    "Round chưa có Criteria (type ≠ PENALTY) để activate",
                    Map.of("roundId", roundId));
        }
        double total = totalOpt.get();
        double missing = WEIGHT_TARGET - total;
        if (Math.abs(missing) > WEIGHT_TOLERANCE) {
            throw new BusinessRuleException(ErrorCode.ROUND_WEIGHT_NOT_ONE,
                    "Tổng weight = %.4f, cần điều chỉnh về 1.0 (lệch %.4f)".formatted(total, missing),
                    Map.of("roundId", roundId, "currentTotal", total, "missing", missing));
        }

        Integer trackId = round.getTrack() == null ? null : round.getTrack().getId();
        int deactivated = 0;
        if (trackId != null) {
            deactivated = roundRepository.deactivateOtherRoundsInTrack(trackId, roundId);
            if (deactivated > 0) {
                auditService.log(AuditAction.ROUND_DEACTIVATE, "rounds", roundId,
                        Map.of("trackId", trackId, "deactivatedCount", deactivated,
                               "reason", "Activated round " + roundId));
            }
        }

        round.setIsActive(true);
        Round saved = roundRepository.save(round);

        auditService.log(AuditAction.ROUND_ACTIVATE, "rounds", roundId, Map.of(
                "trackId",      trackId,
                "note",         note,
                "weightTotal",  total,
                "siblingDeactivated", deactivated
        ));
        return roundMapper.toResponse(saved);
    }
}
