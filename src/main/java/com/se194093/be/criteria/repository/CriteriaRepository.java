package com.se194093.be.criteria.repository;

import com.se194093.be.criteria.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CriteriaRepository extends JpaRepository<Criteria, Integer> {

    List<Criteria> findByRoundIdOrderByDisplayOrderAsc(Integer roundId);

    long countByRoundId(Integer roundId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByRoundId(Integer roundId);

    /**
     * Tổng weight của Criteria type ≠ PENALTY trong một Round. Dùng cho:
     * <ul>
     *   <li>FR-04 weight-summary (UI realtime)</li>
     *   <li>FR-06 Gate cứng (chuyển ONGOING)</li>
     *   <li>FR-06B Safety net (activate Round)</li>
     * </ul>
     *
     * @return {@code Optional.empty()} nếu Round chưa có Criteria nào (vs 0.0 nếu có nhưng toàn PENALTY).
     */
    @Query("""
            SELECT SUM(c.weight)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    Optional<Double> sumWeightExcludingPenalty(@Param("roundId") Integer roundId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    long countNormalByRoundId(@Param("roundId") Integer roundId);
}
