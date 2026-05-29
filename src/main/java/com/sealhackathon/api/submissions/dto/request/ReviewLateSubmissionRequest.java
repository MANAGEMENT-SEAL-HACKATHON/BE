package com.sealhackathon.api.submissions.dto.request;

import com.sealhackathon.api.submissions.value_object.LateReviewDecision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-16A — PATCH /submissions/{id}/review-late */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLateSubmissionRequest {

    @NotNull
    private LateReviewDecision decision;

    private String note;
}
