package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-11C PATCH /api/v1/teams/{id}/transfer-leader */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferLeaderRequest {

    @NotNull
    private Integer newLeaderId;
}
