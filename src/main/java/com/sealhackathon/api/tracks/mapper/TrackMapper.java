package com.sealhackathon.api.tracks.mapper;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.springframework.stereotype.Component;

@Component
public class TrackMapper {

    public Track toEntity(CreateTrackRequest req, Round round, int sequenceOrder) {
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
                .sequenceOrder(sequenceOrder)
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
        Integer roundId = e.getRound() != null ? e.getRound().getId() : null;
        return TrackSummaryResponse.builder()
                .id(e.getId())
                .roundId(roundId)
                .name(e.getName())
                .status(e.getStatus())
                .sequenceOrder(e.getSequenceOrder())
                .maxTeams(e.getMaxTeams())
                .maxTeamsPerGroup(e.getMaxTeamsPerGroup())
                .build();
    }
}
