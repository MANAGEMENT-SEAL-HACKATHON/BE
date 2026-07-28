package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionScheduleAdjustRequest {

    /** Giờ thi Sơ loại mới — bắt buộc; ngày ≥ registrationEnd + 3 để còn WS+KO. */
    @NotNull
    private LocalDateTime newPrelimExamAt;

    /** Tuỳ chọn — chỉnh WS/KO/CK/Awards trong ràng buộc GĐ1. */
    private CompetitionScheduleOverrides overrides;
}
