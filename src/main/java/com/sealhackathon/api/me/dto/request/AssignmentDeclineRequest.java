package com.sealhackathon.api.me.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDeclineRequest {

    @NotBlank(message = "Lý do từ chối bắt buộc")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}
