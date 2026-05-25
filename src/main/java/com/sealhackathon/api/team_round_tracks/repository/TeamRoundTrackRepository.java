package com.sealhackathon.api.team_round_tracks.repository;

import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRoundTrackRepository extends JpaRepository<TeamRoundTrack, Integer> {

    List<TeamRoundTrack> findByTeam_Id(Integer teamId);

    Optional<TeamRoundTrack> findByTeam_IdAndTrack_Round_Id(Integer teamId, Integer roundId);

    boolean existsByTeam_Id(Integer teamId);
}
