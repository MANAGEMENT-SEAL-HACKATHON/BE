package com.se194093.be.tracks.mapper;

import com.se194093.be.rounds.entity.Round;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.value_object.TrackStatus;
import org.springframework.stereotype.Component;

@Component
public class TrackMapper {

    public Track toEntity(CreateTrackRequest req, Round round) {
        return Track.builder()
                .round(round)
                .name(req.getName())
                .description(req.getDescription())
                .topic(req.getTopic())
                .maxTeams(req.getMaxTeams())
                .maxTeamsPerGroup(req.getMaxTeamsPerGroup())
                .minTeamSize(req.getMinTeamSize())
                .maxTeamSize(req.getMaxTeamSize())
                .status(TrackStatus.OPEN)
                .sequenceOrder(req.getSequenceOrder() != null ? req.getSequenceOrder() : 1)
                .build();
    }

    public void applyUpdate(Track entity, UpdateTrackRequest req) {
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        if (req.getTopic() != null) {
            entity.setTopic(req.getTopic());
        }
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
        Integer hackathonId = null;
        Integer roundId = null;
        if (e.getRound() != null) {
            roundId = e.getRound().getId();
            if (e.getRound().getHackathon() != null) {
                hackathonId = e.getRound().getHackathon().getId();
            }
        }
        return TrackResponse.builder()
                .id(e.getId())
                .hackathonId(hackathonId)
                .roundId(roundId)
                .name(e.getName())
                .description(e.getDescription())
                .topic(e.getTopic())
                .maxTeams(e.getMaxTeams())
                .maxTeamsPerGroup(e.getMaxTeamsPerGroup())
                .minTeamSize(e.getMinTeamSize())
                .maxTeamSize(e.getMaxTeamSize())
                .status(e.getStatus())
                .sequenceOrder(e.getSequenceOrder())
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
