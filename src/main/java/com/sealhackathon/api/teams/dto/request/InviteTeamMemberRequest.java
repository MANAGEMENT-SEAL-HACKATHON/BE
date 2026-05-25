package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-12 POST /api/v1/teams/{id}/members/invite (BACKLOG). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteTeamMemberRequest {

    @NotBlank
    @Email
    private String email;
}
