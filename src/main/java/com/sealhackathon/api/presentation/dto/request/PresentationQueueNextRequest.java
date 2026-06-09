package com.sealhackathon.api.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationQueueNextRequest {

    private Integer trackId;
    private Integer currentSubmissionId;
    /** @deprecated use currentSubmissionId */
    @Deprecated
    private Integer currentTeamId;
    /**
     * FE set true sau dialog xác nhận khi BE trả {@code SCORING_INCOMPLETE_BEFORE_NEXT}
     * (còn judge trên track chưa chấm lần nào).
     */
    private Boolean acknowledgeIncompleteScoring;
}
