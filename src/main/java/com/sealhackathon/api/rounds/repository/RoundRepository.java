package com.sealhackathon.api.rounds.repository;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.value_object.RoundType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Round r WHERE r.id = :id")
    Optional<Round> findByIdForUpdate(@Param("id") Integer id);

    List<Round> findByHackathon_IdOrderByExamAtAsc(Integer hackathonId);

    boolean existsByHackathon_Id(Integer hackathonId);

    @Query("""
            SELECT MAX(r.examAt)
              FROM Round r
             WHERE r.hackathon.id = :hackathonId
               AND r.isFinal = FALSE
            """)
    Optional<LocalDateTime> maxExamAtNonFinal(@Param("hackathonId") Integer hackathonId);

    long countByHackathon_IdAndIsFinalTrue(Integer hackathonId);

    List<Round> findByHackathon_IdAndRoundType(Integer hackathonId, RoundType roundType);

    @Query("""
            SELECT r FROM Round r
             WHERE r.hackathon.id = :hackathonId
               AND r.isFinal = FALSE
               AND r.roundType IN (
                   com.sealhackathon.api.rounds.value_object.RoundType.PRELIMINARY,
                   com.sealhackathon.api.rounds.value_object.RoundType.SEMIFINAL
               )
            """)
    List<Round> findPreliminaryLikeByHackathonId(@Param("hackathonId") Integer hackathonId);

    Optional<Round> findByHackathon_IdAndIsFinalTrue(Integer hackathonId);

    /** @deprecated legacy — Track chỉ có 1 Round cha */
    @Deprecated
    @Query("""
            SELECT r FROM Round r, Track t
             WHERE t.id = :trackId AND t.round = r
             ORDER BY r.examAt ASC
            """)
    List<Round> findByTrackIdOrderByExamAtAsc(@Param("trackId") Integer trackId);

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

    /** Active, unlocked rounds whose submission deadline falls within the lead window and have no reminder yet. */
    @Query("""
            SELECT r FROM Round r
             WHERE r.isActive = TRUE
               AND r.scoringLocked = FALSE
               AND r.deadlineReminderSentAt IS NULL
               AND r.submissionDeadline IS NOT NULL
               AND r.submissionDeadline > :now
               AND r.submissionDeadline <= :deadline
            """)
    List<Round> findActiveWithUpcomingDeadlineWithoutReminder(@Param("now") LocalDateTime now,
                                                              @Param("deadline") LocalDateTime deadline);

    /** @deprecated use {@link #deactivateOtherActiveRoundsInHackathon} */
    @Deprecated
    default int deactivateOtherRoundsInTrack(Integer trackId, Integer keepRoundId) {
        return 0;
    }
}
