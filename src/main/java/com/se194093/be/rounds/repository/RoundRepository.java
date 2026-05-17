package com.se194093.be.rounds.repository;

import com.se194093.be.rounds.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findByTrackIdOrderBySequenceOrderAsc(Integer trackId);

    Optional<Round> findByTrackIdAndIsActiveTrue(Integer trackId);

    boolean existsByTrackIdAndIsActiveTrue(Integer trackId);

    long countByTrackId(Integer trackId);

    @Query("SELECT MAX(r.sequenceOrder) FROM Round r WHERE r.track.id = :trackId")
    Integer findMaxSequenceByTrackId(@Param("trackId") Integer trackId);

    /**
     * Khi activate Round mới — tắt cờ {@code is_active} của tất cả Round khác trong cùng Track
     * (theo FR-06B step 6).
     */
    @Modifying
    @Query("""
            UPDATE Round r
               SET r.isActive = FALSE
             WHERE r.track.id = :trackId
               AND r.id <> :keepRoundId
               AND r.isActive = TRUE
            """)
    int deactivateOtherRoundsInTrack(@Param("trackId") Integer trackId,
                                     @Param("keepRoundId") Integer keepRoundId);
}
