package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** FR-13 — Duyệt hàng loạt (tùy chọn, spec v3.5). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkApproveTeamsRequest {

    @NotNull
    private Integer hackathonId;

    @NotEmpty
    private List<Integer> teamIds;
}
