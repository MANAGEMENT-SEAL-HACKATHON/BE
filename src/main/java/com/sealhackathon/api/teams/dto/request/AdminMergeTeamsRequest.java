package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** API dành riêng cho Coordinator gộp 2 đội thiếu người thành 1 đội. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMergeTeamsRequest {

    @NotNull(message = "ID của Đội bị gộp (Source Team) không được để trống")
    private Integer sourceTeamId;
}