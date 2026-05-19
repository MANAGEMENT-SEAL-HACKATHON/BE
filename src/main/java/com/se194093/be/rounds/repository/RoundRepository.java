package com.se194093.be.rounds.repository;

import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.value_object.RoundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findByHackathon_IdOrderBySequenceOrderAsc(Integer hackathonId);

    @Query("""
            SELECT COALESCE(MAX(r.sequenceOrder), 0)
              FROM Round r
             WHERE r.hackathon.id = :hackathonId
               AND r.isFinal = FALSE
            """)
    int maxSequenceOrderNonFinal(@Param("hackathonId") Integer hackathonId);

    long countByHackathon_IdAndIsFinalTrue(Integer hackathonId);

    List<Round> findByHackathon_IdAndRoundType(Integer hackathonId, RoundType roundType);

    @Query("""
            SELECT r FROM Round r
             WHERE r.hackathon.id = :hackathonId
               AND r.isFinal = FALSE
               AND r.roundType IN (
                   com.se194093.be.rounds.value_object.RoundType.PRELIMINARY,
                   com.se194093.be.rounds.value_object.RoundType.SEMIFINAL
               )
            """)
    List<Round> findPreliminaryLikeByHackathonId(@Param("hackathonId") Integer hackathonId);

    Optional<Round> findByHackathon_IdAndIsFinalTrue(Integer hackathonId);

    /** @deprecated legacy — Track chỉ có 1 Round cha */
    @Deprecated
    @Query("""
            SELECT r FROM Round r, Track t
             WHERE t.id = :trackId AND t.round = r
             ORDER BY r.sequenceOrder ASC
            """)
    List<Round> findByTrackIdOrderBySequenceOrderAsc(@Param("trackId") Integer trackId);

    @Modifying
    @Query("""
            UPDATE Round r
               SET r.isActive = FALSE
             WHERE r.hackathon.id = :hackathonId
               AND r.id <> :keepRoundId
               AND r.isActive = TRUE
            """)
    int deactivateOtherActiveRoundsInHackathon(@Param("hackathonId") Integer hackathonId,
                                               @Param("keepRoundId") Integer keepRoundId);

    /** @deprecated use {@link #deactivateOtherActiveRoundsInHackathon} */
    @Deprecated
    default int deactivateOtherRoundsInTrack(Integer trackId, Integer keepRoundId) {
        return 0;
    }
}
