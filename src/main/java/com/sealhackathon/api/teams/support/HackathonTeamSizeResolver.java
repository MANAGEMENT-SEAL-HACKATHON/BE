package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Quy mô đội theo cấu hình Track của hackathon (min_team_size / max_team_size).
 * Trước khi bốc thăm: giao của các track (max min, min max) để đội hợp lệ với mọi track.
 */
@Component
@RequiredArgsConstructor
public class HackathonTeamSizeResolver {

    public static final int DEFAULT_MIN_TEAM_SIZE = 3;
    public static final int DEFAULT_MAX_TEAM_SIZE = 5;

    private final TrackRepository trackRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;

    public record TeamSizeLimits(int minTeamSize, int maxTeamSize) {}

    public TeamSizeLimits forHackathon(Integer hackathonId) {
        List<Track> tracks = trackRepository.findByHackathonIdOrderById(hackathonId).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
        if (tracks.isEmpty()) {
            return new TeamSizeLimits(DEFAULT_MIN_TEAM_SIZE, DEFAULT_MAX_TEAM_SIZE);
        }
        int min = tracks.stream().mapToInt(Track::getMinTeamSize).max().orElse(DEFAULT_MIN_TEAM_SIZE);
        int max = tracks.stream().mapToInt(Track::getMaxTeamSize).min().orElse(DEFAULT_MAX_TEAM_SIZE);
        if (max < min) {
            return new TeamSizeLimits(DEFAULT_MIN_TEAM_SIZE, DEFAULT_MAX_TEAM_SIZE);
        }
        return new TeamSizeLimits(min, max);
    }

    public TeamSizeLimits forTeam(Team team) {
        List<TeamRoundTrack> assignments = teamRoundTrackRepository.findByTeam_Id(team.getId());
        if (!assignments.isEmpty()) {
            Track track = assignments.stream()
                    .max(Comparator.comparing(TeamRoundTrack::getAssignedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(assignments.get(assignments.size() - 1))
                    .getTrack();
            return new TeamSizeLimits(track.getMinTeamSize(), track.getMaxTeamSize());
        }
        return forHackathon(team.getHackathon().getId());
    }
}
