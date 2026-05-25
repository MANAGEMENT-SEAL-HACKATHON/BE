package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-13C POST /api/v1/teams/{id}/rounds/{roundId}/mentor */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTeamMentorRequest {

    @NotNull
    private Integer mentorId;
}
