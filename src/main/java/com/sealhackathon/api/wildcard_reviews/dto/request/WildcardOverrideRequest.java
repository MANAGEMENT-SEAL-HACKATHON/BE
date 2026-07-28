package com.sealhackathon.api.wildcard_reviews.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plan C — POST /wildcard-reviews/{id}/override */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardOverrideRequest {

    @NotNull
    private Boolean approved;

    /** PROPOSED_TEAM_VIOLATION | TRACK_QUOTA_ADJUST | SCORE_CORRECTED | OTHER */
    private String category;

    private String note;
}
