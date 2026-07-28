package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A1 — Kiểm tra chấm tổng thể.
 * <ul>
 *   <li>Không {@code trackId}: chỉ {@link #tracks} summary (payload nhỏ).</li>
 *   <li>Có {@code trackId}: ma trận chi tiết track đó ({@link #criteria}/{@link #judges}/{@link #teams}).</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundScoreAuditResponse {

    private Integer roundId;
    private Integer trackId;
    private String trackName;
    /** Summary mode — mỗi track + % tiến độ GK. */
    private List<TrackSummary> tracks;
    /** Detail mode. */
    private List<ScoreBreakdownResponse.CriterionColumn> criteria;
    private List<ScoreBreakdownResponse.JudgeRow> judges;
    private List<TeamMatrix> teams;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrackSummary {
        private Integer trackId;
        private String trackName;
        private int teamCount;
        private int submissionCount;
        private List<JudgeProgress> judgeProgress;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgeProgress {
        private Integer judgeId;
        private String judgeName;
        private int scoredCells;
        private int expectedCells;
        private double percent;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamMatrix {
        private Integer teamId;
        private String teamName;
        private Integer submissionId;
        private List<ScoreBreakdownResponse.Cell> cells;
        private List<ScoreBreakdownResponse.CriterionStats> criterionStats;
        private Double overallMean;
    }
}
