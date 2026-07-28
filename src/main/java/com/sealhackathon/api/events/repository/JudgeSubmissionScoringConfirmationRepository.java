package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.JudgeSubmissionScoringConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface JudgeSubmissionScoringConfirmationRepository
        extends JpaRepository<JudgeSubmissionScoringConfirmation, Integer> {

    Optional<JudgeSubmissionScoringConfirmation> findBySubmission_IdAndJudge_Id(
            Integer submissionId, Integer judgeId);

    @Query("""
            SELECT COUNT(DISTINCT c.judge.id)
              FROM JudgeSubmissionScoringConfirmation c
             WHERE c.submission.id = :submissionId
            """)
    int countDistinctJudgesBySubmissionId(@Param("submissionId") Integer submissionId);

    boolean existsBySubmission_IdAndJudge_Id(Integer submissionId, Integer judgeId);

    @Transactional
    void deleteBySubmission_Id(Integer submissionId);

    @Transactional
    void deleteBySubmission_IdAndJudge_Id(Integer submissionId, Integer judgeId);

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM JudgeSubmissionScoringConfirmation c
             WHERE c.submission.track IS NOT NULL
               AND c.submission.track.round.id = :roundId
               AND c.submission.track.id = :trackId
            """)
    void deleteByTrackScope(@Param("roundId") Integer roundId, @Param("trackId") Integer trackId);

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM JudgeSubmissionScoringConfirmation c
             WHERE c.submission.round IS NOT NULL
               AND c.submission.round.id = :roundId
            """)
    void deleteByFinalRoundScope(@Param("roundId") Integer roundId);
}
