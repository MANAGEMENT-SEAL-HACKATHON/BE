package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationQueueNextResponse {

    private Integer nextSubmissionId;
    private Integer nextTeamId;
    private Integer trackId;
    /** Tóm tắt chấm của bài vừa kết thúc (trước khi chuyển). */
    private ScoringSnapshot completedSubmissionScoring;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoringSnapshot {
        private Integer submissionId;
        private int judgesAssigned;
        private int judgesScored;
        private int judgesFullyScored;
        private long scoreCount;
        private boolean incomplete;
    }
}
