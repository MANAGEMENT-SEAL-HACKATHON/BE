package com.se194093.be.criteria.service;

import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.dto.response.WeightSummaryResponse;

import java.util.Optional;

/**
 * Tính & format tổng weight Criteria của 1 Round. Dùng chung cho cả 3 tầng validate:
 * <ol>
 *   <li>FR-04: GET {@code /weight-summary} (UI realtime)</li>
 *   <li>FR-04: gắn {@code warnings} vào response của mọi mutation Criteria</li>
 *   <li>FR-06 / FR-06B: dùng method {@link #isValid(Integer)} để chốt block</li>
 * </ol>
 */
public interface WeightSummaryService {

    double TARGET    = 1.0;
    double TOLERANCE = 0.001;

    /**
     * Tổng raw — {@code Optional.empty()} nếu Round chưa có Criteria type ≠ PENALTY nào.
     */
    Optional<Double> rawTotal(Integer roundId);

    /**
     * Trả về summary đầy đủ (kèm danh sách item) cho UI realtime.
     */
    WeightSummaryResponse summary(Integer roundId);

    /**
     * @return TRUE nếu {@code |total - 1.0| &lt;= 0.001} VÀ Round có ít nhất 1 Criteria.
     */
    boolean isValid(Integer roundId);

    /**
     * @return {@code Optional.of(Warning)} nếu tổng lệch — gắn vào response 2xx của mutation Criteria;
     *         {@code Optional.empty()} nếu đã đúng 1.0.
     */
    Optional<Warning> warningIfNotOne(Integer roundId);
}
