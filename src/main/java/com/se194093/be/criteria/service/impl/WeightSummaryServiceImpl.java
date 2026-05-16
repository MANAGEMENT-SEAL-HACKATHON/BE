package com.se194093.be.criteria.service.impl;

import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.dto.response.WeightSummaryResponse;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.criteria.value_object.CriteriaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Impl chuẩn — KHÔNG chỉ là skeleton vì logic đủ ngắn để hoàn thiện. Đây là dependency
 * cốt lõi cho cả Tầng 1 (FR-04), Tầng 2 (FR-06), Tầng 3 (FR-06B).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeightSummaryServiceImpl implements WeightSummaryService {

    private final CriteriaRepository criteriaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> rawTotal(Integer roundId) {
        return criteriaRepository.sumWeightExcludingPenalty(roundId);
    }

    @Override
    @Transactional(readOnly = true)
    public WeightSummaryResponse summary(Integer roundId) {
        double total = criteriaRepository.sumWeightExcludingPenalty(roundId).orElse(0.0);
        double missing = round4(TARGET - total);
        String status = isClose(total) ? "OK" : "WARN";
        List<WeightSummaryResponse.Item> items = criteriaRepository
                .findByRoundIdOrderByDisplayOrderAsc(roundId).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .map(c -> WeightSummaryResponse.Item.builder()
                        .criterionId(c.getId())
                        .name(c.getName())
                        .weight(c.getWeight())
                        .build())
                .toList();
        return WeightSummaryResponse.builder()
                .roundId(roundId)
                .total(round4(total))
                .missing(missing)
                .status(status)
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(Integer roundId) {
        Optional<Double> total = criteriaRepository.sumWeightExcludingPenalty(roundId);
        return total.isPresent() && isClose(total.get())
                && criteriaRepository.countNormalByRoundId(roundId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warning> warningIfNotOne(Integer roundId) {
        Optional<Double> total = criteriaRepository.sumWeightExcludingPenalty(roundId);
        double t = total.orElse(0.0);
        if (isClose(t)) {
            return Optional.empty();
        }
        double missing = round4(TARGET - t);
        return Optional.of(Warning.of(
                "WEIGHT_NOT_ONE",
                "Tổng weight hiện tại %.4f, cần %s %.4f để đủ 1.0"
                        .formatted(t, missing > 0 ? "thêm" : "giảm", Math.abs(missing)),
                Map.of("currentTotal", round4(t), "missing", missing)
        ));
    }

    private static boolean isClose(double total) {
        return Math.abs(total - TARGET) <= TOLERANCE;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
