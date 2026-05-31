package com.sealhackathon.api.wildcard_reviews.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-22A — PATCH /wildcard-reviews/{id} */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardReviewDecisionRequest {

    @NotNull
    private Boolean approved;

    private String coordinatorNote;
}
