package com.se194093.be.tracks.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.se194093.be.tracks.value_object.TrackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackSummaryResponse {

    private final Integer id;
    private final String name;
    private final TrackStatus status;
    private final Integer maxTeams;
    private final Integer maxTeamsPerGroup;
}
