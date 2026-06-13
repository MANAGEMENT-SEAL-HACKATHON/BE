package com.sealhackathon.api.teams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** API dành riêng cho Coordinator ép thêm 1 sinh viên vào đội (God Mode). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAddMemberRequest {

    @NotNull(message = "ID của sinh viên không được để trống")
    private Integer userId;
}