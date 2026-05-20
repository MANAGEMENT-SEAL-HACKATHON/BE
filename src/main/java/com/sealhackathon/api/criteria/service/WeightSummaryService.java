package com.sealhackathon.api.criteria.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.criteria.dto.response.WeightSummaryResponse;

import java.util.Optional;

/**
 * Tổng weight Criteria — theo Track (Sơ loại) hoặc Round FINAL (Chung kết).
 */
public interface WeightSummaryService {

    double TARGET    = 1.0;
    double TOLERANCE = 0.001;

    /** @deprecated dùng {@link #rawTotalForTrack} hoặc {@link #rawTotalForFinalRound} */
    @Deprecated
    Optional<Double> rawTotal(Integer roundId);

    Optional<Double> rawTotalForTrack(Integer trackId);

    Optional<Double> rawTotalForFinalRound(Integer finalRoundId);

    WeightSummaryResponse summaryForTrack(Integer trackId);

    WeightSummaryResponse summaryForFinalRound(Integer finalRoundId);

    /** @deprecated */
    @Deprecated
    WeightSummaryResponse summary(Integer roundId);

    boolean isValidForTrack(Integer trackId);

    boolean isValidForFinalRound(Integer finalRoundId);

    /** @deprecated */
    @Deprecated
    boolean isValid(Integer roundId);

    Optional<Warning> warningIfNotOneForTrack(Integer trackId);

    Optional<Warning> warningIfNotOneForFinalRound(Integer finalRoundId);

    /** @deprecated */
    @Deprecated
    Optional<Warning> warningIfNotOne(Integer roundId);
}
