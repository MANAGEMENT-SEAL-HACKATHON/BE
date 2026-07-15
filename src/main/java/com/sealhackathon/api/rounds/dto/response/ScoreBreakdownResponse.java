package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ma trận điểm judges × criteria cho 1 submission (Bug4 — score-breakdown).
 * Ô thiếu = {@code scoreValue == null}.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBreakdownResponse {

    private Integer roundId;
    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    private List<CriterionColumn> criteria;
    private List<JudgeRow> judges;
    private List<Cell> cells;
    private List<CriterionStats> criterionStats;
    private Double overallMean;
    private Double overallVariance;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionColumn {
        private Integer criterionId;
        private String name;
        private Float maxScore;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgeRow {
        private Integer judgeId;
        private String judgeName;
        private LocalDateTime lastScoredAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cell {
        private Integer judgeId;
        private Integer criterionId;
        private Float scoreValue;
        private String comment;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionStats {
        private Integer criterionId;
        private Double mean;
        private Double variance;
        private int scoredCount;
        private int missingCount;
    }
}
