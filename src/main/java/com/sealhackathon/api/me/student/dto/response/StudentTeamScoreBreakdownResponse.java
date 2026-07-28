package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A2-1 — Bảng điểm riêng của đội: tiêu chí + TB, giám khảo ẩn danh (Giám khảo 1/2/3).
 * Không lộ judgeId / tên / email.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTeamScoreBreakdownResponse {

    private Integer roundId;
    private String roundName;
    private Integer teamId;
    private String teamName;
    private Integer submissionId;
    private List<CriterionColumn> criteria;
    /** Nhãn ổn định «Giám khảo 1»… sort theo judgeId nội bộ (không phơi ID). */
    private List<AnonymousJudge> judges;
    private List<Cell> cells;
    private List<CriterionAvg> criterionAverages;
    private Double teamAverage;

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
    public static class AnonymousJudge {
        /** 1-based index ổn định. */
        private int ordinal;
        private String label;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cell {
        private int judgeOrdinal;
        private Integer criterionId;
        private Float scoreValue;
        private String comment;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionAvg {
        private Integer criterionId;
        private Double average;
    }
}
