package com.sealhackathon.api.scores.repository;

import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.value_object.ScoreType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    List<Score> findBySubmission_Id(Integer submissionId);

    List<Score> findBySubmission_IdAndScoreType(Integer submissionId, ScoreType scoreType);

    List<Score> findBySubmission_Track_Round_Id(Integer roundId);

    List<Score> findBySubmission_Round_Id(Integer roundId);

    long countByCriterion_Id(Integer criterionId);

    default long countByCriteriaId(Integer criteriaId) {
        return countByCriterion_Id(criteriaId);
    }

    @Query("""
            SELECT COUNT(s)
              FROM Score s
             WHERE (s.submission.round IS NOT NULL AND s.submission.round.id = :roundId)
                OR (s.submission.track IS NOT NULL AND s.submission.track.round.id = :roundId)
            """)
    long countByRoundId(@Param("roundId") Integer roundId);
}
