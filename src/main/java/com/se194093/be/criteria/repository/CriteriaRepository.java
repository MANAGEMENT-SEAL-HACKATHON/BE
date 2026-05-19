package com.se194093.be.criteria.repository;

import com.se194093.be.criteria.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CriteriaRepository extends JpaRepository<Criteria, Integer> {

    List<Criteria> findByTrackIdOrderByDisplayOrderAsc(Integer trackId);

    long countByTrackId(Integer trackId);

    @Query("""
            SELECT SUM(c.weight)
              FROM Criteria c
             WHERE c.track.id = :trackId
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    Optional<Double> sumWeightExcludingPenaltyByTrackId(@Param("trackId") Integer trackId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.track.id = :trackId
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    long countNormalByTrackId(@Param("trackId") Integer trackId);

    @Query("""
            SELECT c FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
             ORDER BY c.displayOrder ASC
            """)
    List<Criteria> findByFinalRoundIdOrderByDisplayOrderAsc(@Param("roundId") Integer roundId);

    @Query("""
            SELECT SUM(c.weight)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    Optional<Double> sumWeightExcludingPenaltyByFinalRoundId(@Param("roundId") Integer roundId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.type <> com.se194093.be.criteria.value_object.CriteriaType.PENALTY
            """)
    long countNormalByFinalRoundId(@Param("roundId") Integer roundId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.round.id = :roundId
                OR c.track.round.id = :roundId
            """)
    long countByRoundIdOrTracksInRound(@Param("roundId") Integer roundId);

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM Criteria c
             WHERE c.round.id = :roundId
                OR c.track.round.id = :roundId
            """)
    void deleteByRoundId(@Param("roundId") Integer roundId);
}
