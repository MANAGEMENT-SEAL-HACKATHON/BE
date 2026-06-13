package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** API dành riêng cho Coordinator tạo Đội ép buộc (Bỏ qua luồng gửi Mail). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateTeamRequest {

    @NotNull
    private Integer hackathonId;

    @NotBlank
    @Size(max = 200)
    private String teamName;

    @NotNull
    private Integer leaderId;

    @NotEmpty
    private List<Integer> memberIds;
}