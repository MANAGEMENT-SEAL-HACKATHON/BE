package com.sealhackathon.api.criteria.repository;

import com.sealhackathon.api.criteria.entity.Criteria;
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

    List<Criteria> findByTrackIdAndIsTiebreakerPriorityTrueOrderByDisplayOrderAsc(Integer trackId);

    Optional<Criteria> findByTrackIdAndIsTiebreakerPriorityTrue(Integer trackId);

    long countByTrackId(Integer trackId);

    /**
     * Gỡ {@code source_criteria_id} giữa track/round khác nhau (clone độc lập — dùng repair seed dev).
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE criteria c
            INNER JOIN criteria src ON c.source_criteria_id = src.id
               SET c.source_criteria_id = NULL
             WHERE (c.track_id IS NOT NULL AND src.track_id IS NOT NULL AND c.track_id <> src.track_id)
                OR (c.round_id IS NOT NULL AND src.round_id IS NOT NULL AND c.round_id <> src.round_id)
                OR (c.track_id IS NOT NULL AND src.track_id IS NULL)
                OR (c.track_id IS NULL AND src.track_id IS NOT NULL)
                OR (c.round_id IS NOT NULL AND src.round_id IS NULL)
                OR (c.round_id IS NULL AND src.round_id IS NOT NULL)
            """, nativeQuery = true)
    int unlinkCrossScopeSourceCriteria();

    /** Trước DELETE — gỡ tham chiếu con (dữ liệu legacy còn {@code source_criteria_id}). */
    @Modifying
    @Query("UPDATE Criteria c SET c.sourceCriteria = null WHERE c.sourceCriteria.id = :id")
    int clearSourceReferencing(@Param("id") Integer id);

    /** Legacy: còn {@code source_criteria_id} trỏ sang track/round khác (sau migrate thường = 0). */
    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
              JOIN c.sourceCriteria src
             WHERE c.track.id = :trackId
               AND (
                    (src.track IS NOT NULL AND src.track.id <> :trackId)
                 OR (src.round IS NOT NULL)
               )
            """)
    long countLegacyCrossScopeSourceByTrackId(@Param("trackId") Integer trackId);

    @Query("""
            SELECT SUM(c.weight)
              FROM Criteria c
             WHERE c.track.id = :trackId
               AND c.type <> com.sealhackathon.api.criteria.value_object.CriteriaType.PENALTY
            """)
    Optional<Double> sumWeightExcludingPenaltyByTrackId(@Param("trackId") Integer trackId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.track.id = :trackId
               AND c.type <> com.sealhackathon.api.criteria.value_object.CriteriaType.PENALTY
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
            SELECT c FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.isTiebreakerPriority = true
            """)
    Optional<Criteria> findByFinalRoundIdAndIsTiebreakerPriorityTrue(@Param("roundId") Integer roundId);

    @Query("""
            SELECT c FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.isTiebreakerPriority = true
             ORDER BY c.displayOrder ASC
            """)
    List<Criteria> findByFinalRoundIdAndIsTiebreakerPriorityTrueOrderByDisplayOrderAsc(@Param("roundId") Integer roundId);

    @Query("""
            SELECT SUM(c.weight)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.type <> com.sealhackathon.api.criteria.value_object.CriteriaType.PENALTY
            """)
    Optional<Double> sumWeightExcludingPenaltyByFinalRoundId(@Param("roundId") Integer roundId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.round.id = :roundId
               AND c.track IS NULL
               AND c.type <> com.sealhackathon.api.criteria.value_object.CriteriaType.PENALTY
            """)
    long countNormalByFinalRoundId(@Param("roundId") Integer roundId);

    @Query("""
            SELECT COUNT(c)
              FROM Criteria c
             WHERE c.round.id = :roundId
                OR c.track.round.id = :roundId
            """)
    long countByRoundIdOrTracksInRound(@Param("roundId") Integer roundId);

    /** Native count — khớp FK DB (criteria.round_id hoặc track thuộc round). */
    @Query(value = """
            SELECT COUNT(*)
              FROM criteria c
              LEFT JOIN tracks t ON c.track_id = t.id
             WHERE c.round_id = :roundId
                OR t.round_id = :roundId
            """, nativeQuery = true)
    long countAllLinkedToRoundNative(@Param("roundId") Integer roundId);

    /**
     * Gỡ {@code source_criteria_id} trỏ vào criterion thuộc round (trước bulk DELETE).
     * DB đã có ON DELETE SET NULL — bước này tránh edge-case Hibernate/native delete lệch.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE criteria c
            INNER JOIN criteria src ON c.source_criteria_id = src.id
            LEFT JOIN tracks t ON src.track_id = t.id
               SET c.source_criteria_id = NULL
             WHERE src.round_id = :roundId
                OR t.round_id = :roundId
            """, nativeQuery = true)
    int unlinkSourceReferencingRound(@Param("roundId") Integer roundId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            DELETE FROM Criteria c
             WHERE c.round.id = :roundId
                OR c.track.round.id = :roundId
            """)
    void deleteByRoundId(@Param("roundId") Integer roundId);

    /** Native delete — khớp FK DB; JPQL deleteByRoundId có thể không xóa khi round_id set trực tiếp. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            DELETE c FROM criteria c
              LEFT JOIN tracks t ON c.track_id = t.id
             WHERE c.round_id = :roundId
                OR t.round_id = :roundId
            """, nativeQuery = true)
    int deleteAllLinkedToRoundNative(@Param("roundId") Integer roundId);
}
