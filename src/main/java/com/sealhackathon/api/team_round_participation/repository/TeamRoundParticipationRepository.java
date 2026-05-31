package com.sealhackathon.api.team_round_participation.repository;

import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRoundParticipationRepository extends JpaRepository<TeamRoundParticipation, Integer> {

    Optional<TeamRoundParticipation> findByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<TeamRoundParticipation> findByTeam_Id(Integer teamId);

    List<TeamRoundParticipation> findByRound_Id(Integer roundId);

    long countByRound_Id(Integer roundId);
}
