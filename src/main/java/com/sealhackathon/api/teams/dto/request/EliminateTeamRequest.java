package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-21 — PATCH /teams/{id}/eliminate */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EliminateTeamRequest {

    @NotBlank
    private String reason;
}
