package com.sealhackathon.api.teams.repository;

import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRoundTrackRepository extends JpaRepository<TeamRoundTrack, Integer> {

    List<TeamRoundTrack> findByTeam_Id(Integer teamId);

    @Query("SELECT trt FROM TeamRoundTrack trt JOIN FETCH trt.track t JOIN FETCH t.round r JOIN FETCH r.hackathon WHERE trt.team.id = :teamId")
    List<TeamRoundTrack> findByTeamIdWithTrackAndRound(@Param("teamId") Integer teamId);

    Optional<TeamRoundTrack> findByTeam_IdAndTrack_Round_Id(Integer teamId, Integer roundId);

    Optional<TeamRoundTrack> findByTeam_IdAndTrack_Id(Integer teamId, Integer trackId);

    List<TeamRoundTrack> findByTrack_Round_Id(Integer roundId);

    long countByTrack_Id(Integer trackId);

    boolean existsByTeam_Id(Integer teamId);
}
