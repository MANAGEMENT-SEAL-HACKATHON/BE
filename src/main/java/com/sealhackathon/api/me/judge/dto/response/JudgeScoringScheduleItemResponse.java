package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeScoringScheduleItemResponse {

    private Integer roundId;
    private String roundName;
    private LocalDateTime scoringStartAt;
    private LocalDateTime scoringEndAt;
}
