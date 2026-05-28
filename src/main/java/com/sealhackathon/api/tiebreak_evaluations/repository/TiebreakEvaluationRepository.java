package com.sealhackathon.api.tiebreak_evaluations.repository;

import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TiebreakEvaluationRepository extends JpaRepository<TiebreakEvaluation, Integer> {

    List<TiebreakEvaluation> findByRound_Id(Integer roundId);

    List<TiebreakEvaluation> findByRound_IdAndTeam_Id(Integer roundId, Integer teamId);

    Optional<TiebreakEvaluation> findByRound_IdAndTeam_IdAndJudge_Id(
            Integer roundId, Integer teamId, Integer judgeId);
}
