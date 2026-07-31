package com.sealhackathon.api.rounds.dto.response;

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
public class RoundRankingItemResponse {

    private Integer rank;
    private Integer teamId;
    private String teamName;
    private Integer trackId;
    private String assignedGroup;
    private Double totalScore;
    private Boolean tiebreakRequired;
    private String participationStatus;
    private LocalDateTime submittedAt;
    private String submissionStatus;
    private Double penaltyScore;
    private Double priorityCriterionScore;
    private String priorityCriterionName;
    private Integer submissionId;
    /** Nhãn ngắn khi hạng khác nhau dù điểm hiển thị bằng nhau (waterfall / micro-penalty). */
    private String tiebreakReasonLabel;
}
