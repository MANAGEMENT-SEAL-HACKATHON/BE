package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Track có thể chọn làm nguồn clone criteria (đếm theo {@code criteria.track_id}, kể cả bản clone).
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriteriaCloneSourceItemResponse {

    private final Integer trackId;
    private final String trackName;
    private final Integer hackathonId;
    private final String hackathonName;
    private final Integer roundId;
    private final long criteriaCount;
    /** (legacy) true nếu còn {@code sourceCriteriaId} trỏ criterion track khác — sau migrate thường false. */
    private final boolean clonedFromAnotherTrack;
}
