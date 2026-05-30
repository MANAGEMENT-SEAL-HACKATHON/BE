package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * FR-13B PATCH /api/v1/hackathons/{id}/lottery — bốc thăm Track.
 * Hỗ trợ 2 chế độ:
 * 1. Batch Save: FE gửi danh sách assignments (chọn tay).
 * 2. Auto Lottery: FE để trống assignments, BE tự động chia bảng ngẫu nhiên.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonLotteryRequest {

    @NotNull(message = "roundId không được để trống")
    private Integer roundId;

    @Valid
    private List<Assignment> assignments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Assignment {
        @NotNull
        private Integer teamId;

        @NotNull
        private Integer trackId;

        private String assignedGroup;
    }
}