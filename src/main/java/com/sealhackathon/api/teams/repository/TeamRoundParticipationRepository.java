package com.sealhackathon.api.teams.repository;

import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRoundParticipationRepository extends JpaRepository<TeamRoundParticipation, Integer> {

    Optional<TeamRoundParticipation> findByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<TeamRoundParticipation> findByTeam_Id(Integer teamId);

    @Query("SELECT trp FROM TeamRoundParticipation trp JOIN FETCH trp.round WHERE trp.team.id = :teamId")
    List<TeamRoundParticipation> findByTeamIdWithRound(@Param("teamId") Integer teamId);

    List<TeamRoundParticipation> findByRound_Id(Integer roundId);

    long countByRound_Id(Integer roundId);
}
