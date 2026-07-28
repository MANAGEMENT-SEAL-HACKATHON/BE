package com.sealhackathon.api.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationDurationSetupRequest {

    @NotNull
    private Integer roundId;

    /**
     * GĐ3 — override theo track. Bỏ trống khi cấu hình mức round (GĐ5 chung kết hoặc default sơ loại).
     */
    private Integer trackId;

    @NotNull
    @Min(1)
    private Integer presentationMinutes;

    @NotNull
    @Min(1)
    private Integer qaMinutes;
}
