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
public class CloseRegistrationEarlyRequest {

    /**
     * Giờ thi Sơ loại coordinator chọn khi đóng ĐK sớm.
     * Hệ thống cascade WS / KO / CK / Awards theo mốc này (1 lần).
     */
    @NotNull
    private LocalDateTime newPrelimExamAt;

    /** Tuỳ chọn — chỉnh WS/KO/CK/Awards trong ràng buộc GĐ1; null = dùng mặc định cascade. */
    private CompetitionScheduleOverrides overrides;
}
