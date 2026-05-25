package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-13B-R PATCH /api/v1/teams/{id}/rounds/{roundId}/track */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignTeamTrackRequest {

    @NotNull
    private Integer trackId;

    private String assignedGroup;
}
