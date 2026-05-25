package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-11 POST /api/v1/teams — Leader tạo đội (không chọn Track).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamRequest {

    @NotNull
    private Integer hackathonId;

    @NotBlank
    @Size(max = 200)
    private String teamName;
}
