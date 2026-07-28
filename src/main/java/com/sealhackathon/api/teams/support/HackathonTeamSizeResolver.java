package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
        List<Track> tracks = activeTracks(hackathonId);
        if (tracks.isEmpty()) {
            return new TeamSizeLimits(DEFAULT_MIN_TEAM_SIZE, DEFAULT_MAX_TEAM_SIZE);
        }
        return computeIntersection(tracks);
    }

    public void assertCompatibleWithExistingTracks(Integer hackathonId, int minTeamSize, int maxTeamSize,
                                                   Integer excludeTrackId) {
        List<Track> others = activeTracks(hackathonId).stream()
                .filter(t -> excludeTrackId == null || !excludeTrackId.equals(t.getId()))
                .toList();
        if (others.isEmpty()) {
            return;
        }
        Track candidate = Track.builder().minTeamSize(minTeamSize).maxTeamSize(maxTeamSize).build();
        List<Track> combined = new java.util.ArrayList<>(others);
        combined.add(candidate);
        try {
            computeIntersection(combined);
        } catch (BusinessRuleException ex) {
            throw new BusinessRuleException(ErrorCode.TRACK_INVALID_TEAM_SIZE,
                    "Quy mô đội các Track không giao nhau — không thể có đội hợp lệ cho mọi Track",
                    Map.of("minTeamSize", minTeamSize, "maxTeamSize", maxTeamSize, "hackathonId", hackathonId));
        }
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

    private List<Track> activeTracks(Integer hackathonId) {
        return trackRepository.findByHackathonIdOrderById(hackathonId).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
    }

    private static TeamSizeLimits computeIntersection(List<Track> tracks) {
        int min = tracks.stream().mapToInt(Track::getMinTeamSize).max().orElse(DEFAULT_MIN_TEAM_SIZE);
        int max = tracks.stream().mapToInt(Track::getMaxTeamSize).min().orElse(DEFAULT_MAX_TEAM_SIZE);
        if (max < min) {
            throw new BusinessRuleException(ErrorCode.TRACK_INVALID_TEAM_SIZE,
                    "Quy mô đội các Track không giao nhau (min=%d, max=%d)".formatted(min, max),
                    Map.of("effectiveMinTeamSize", min, "effectiveMaxTeamSize", max));
        }
        return new TeamSizeLimits(min, max);
    }
}
