package com.sealhackathon.api.teams.dto.request;

import com.sealhackathon.api.teams.value_object.TeamStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-13 — Coordinator duyệt / từ chối đội. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchTeamStatusRequest {

    @NotNull
    private TeamStatus status;

    @Size(max = 2000)
    private String rejectionReason;
}
