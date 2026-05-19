package com.sealhackathon.api.criteria.service.impl;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.criteria.dto.response.WeightSummaryResponse;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeightSummaryServiceImpl implements WeightSummaryService {

    private final CriteriaRepository criteriaRepository;

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Optional<Double> rawTotal(Integer roundId) {
        return rawTotalForFinalRound(roundId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> rawTotalForTrack(Integer trackId) {
        return criteriaRepository.sumWeightExcludingPenaltyByTrackId(trackId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> rawTotalForFinalRound(Integer finalRoundId) {
        return criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(finalRoundId);
    }

    @Override
    @Transactional(readOnly = true)
    public WeightSummaryResponse summaryForTrack(Integer trackId) {
        return buildSummary(trackId, null,
                criteriaRepository.sumWeightExcludingPenaltyByTrackId(trackId).orElse(0.0),
                criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId));
    }

    @Override
    @Transactional(readOnly = true)
    public WeightSummaryResponse summaryForFinalRound(Integer finalRoundId) {
        return buildSummary(null, finalRoundId,
                criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(finalRoundId).orElse(0.0),
                criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRoundId));
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public WeightSummaryResponse summary(Integer roundId) {
        return summaryForFinalRound(roundId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidForTrack(Integer trackId) {
        Optional<Double> total = rawTotalForTrack(trackId);
        return total.isPresent() && isClose(total.get())
                && criteriaRepository.countNormalByTrackId(trackId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidForFinalRound(Integer finalRoundId) {
        Optional<Double> total = rawTotalForFinalRound(finalRoundId);
        return total.isPresent() && isClose(total.get())
                && criteriaRepository.countNormalByFinalRoundId(finalRoundId) > 0;
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public boolean isValid(Integer roundId) {
        return isValidForFinalRound(roundId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warning> warningIfNotOneForTrack(Integer trackId) {
        return warningFromTotal(trackId, "trackId", rawTotalForTrack(trackId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warning> warningIfNotOneForFinalRound(Integer finalRoundId) {
        return warningFromTotal(finalRoundId, "roundId", rawTotalForFinalRound(finalRoundId));
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Optional<Warning> warningIfNotOne(Integer roundId) {
        return warningIfNotOneForFinalRound(roundId);
    }

    private WeightSummaryResponse buildSummary(Integer trackId, Integer roundId, double total,
                                               List<Criteria> criteria) {
        double missing = round4(TARGET - total);
        String status = isClose(total) ? "OK" : "WARN";
        List<WeightSummaryResponse.Item> items = criteria.stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .map(c -> WeightSummaryResponse.Item.builder()
                        .criterionId(c.getId())
                        .name(c.getName())
                        .weight(c.getWeight())
                        .build())
                .toList();
        return WeightSummaryResponse.builder()
                .trackId(trackId)
                .roundId(roundId)
                .total(round4(total))
                .missing(missing)
                .status(status)
                .items(items)
                .build();
    }

    private Optional<Warning> warningFromTotal(Integer id, String key, Optional<Double> totalOpt) {
        if (totalOpt.isEmpty()) {
            return Optional.empty();
        }
        double total = totalOpt.get();
        if (isClose(total)) {
            return Optional.empty();
        }
        return Optional.of(Warning.of("WEIGHT_NOT_ONE",
                "Tổng weight = %.4f, cần 1.0".formatted(total),
                Map.of(key, id, "total", total)));
    }

    private static boolean isClose(double total) {
        return Math.abs(total - TARGET) <= TOLERANCE;
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
