package com.sealhackathon.api.teams.dto.request;

import com.sealhackathon.api.teams.value_object.TeamMemberAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-12 — Invitee phản hồi lời mời (ACCEPT / REJECT / LEFT). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchTeamMemberRequest {

    @NotNull
    private TeamMemberAction action;
}
