package com.se194093.be.tracks.mapper;

import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.value_object.TrackStatus;
import org.springframework.stereotype.Component;

@Component
public class TrackMapper {

    public Track toEntity(CreateTrackRequest req, Hackathon hackathon) {
        return Track.builder()
                .hackathon(hackathon)
                .name(req.getName())
                .description(req.getDescription())
                .maxTeams(req.getMaxTeams())
                .maxTeamsPerGroup(req.getMaxTeamsPerGroup())
                .minTeamSize(req.getMinTeamSize())
                .maxTeamSize(req.getMaxTeamSize())
                .status(TrackStatus.OPEN)
                .build();
    }

    public void applyUpdate(Track entity, UpdateTrackRequest req) {
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setMaxTeams(req.getMaxTeams());
        entity.setMaxTeamsPerGroup(req.getMaxTeamsPerGroup());
        entity.setMinTeamSize(req.getMinTeamSize());
        entity.setMaxTeamSize(req.getMaxTeamSize());
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
    }

    public TrackResponse toResponse(Track e) {
        if (e == null) {
            return null;
        }
        return TrackResponse.builder()
                .id(e.getId())
                .hackathonId(e.getHackathon() == null ? null : e.getHackathon().getId())
                .name(e.getName())
                .description(e.getDescription())
                .maxTeams(e.getMaxTeams())
                .maxTeamsPerGroup(e.getMaxTeamsPerGroup())
                .minTeamSize(e.getMinTeamSize())
                .maxTeamSize(e.getMaxTeamSize())
                .status(e.getStatus())
                .build();
    }

    public TrackSummaryResponse toSummary(Track e) {
        if (e == null) {
            return null;
        }
        return TrackSummaryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .status(e.getStatus())
                .maxTeams(e.getMaxTeams())
                .maxTeamsPerGroup(e.getMaxTeamsPerGroup())
                .build();
    }
}
