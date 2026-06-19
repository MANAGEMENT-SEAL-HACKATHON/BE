package com.sealhackathon.api.submissions.repository;

import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    List<Submission> findByTeam_Id(Integer teamId);

    List<Submission> findByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<Submission> findByTeam_IdAndTrack_Round_Id(Integer teamId, Integer roundId);

    List<Submission> findByRound_Id(Integer roundId);

    List<Submission> findByTrack_Round_Id(Integer roundId);

    List<Submission> findByTrack_Id(Integer trackId);

    Optional<Submission> findTopByTeam_IdAndTrack_IdOrderBySubmittedAtDesc(Integer teamId, Integer trackId);

    Optional<Submission> findTopByTeam_IdAndRound_IdOrderBySubmittedAtDesc(Integer teamId, Integer roundId);

    @Query("""
            SELECT COUNT(s)
              FROM Submission s
             WHERE (s.round IS NOT NULL AND s.round.id = :roundId)
                OR (s.track IS NOT NULL AND s.track.round.id = :roundId)
            """)
    long countByRoundId(@Param("roundId") Integer roundId);

    List<Submission> findByStatus(SubmissionStatus status);

    List<Submission> findByStatusAndRound_Id(SubmissionStatus status, Integer roundId);
}
