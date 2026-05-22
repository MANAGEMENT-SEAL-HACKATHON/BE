package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriteriaCloneSourcesResponse {

    private final Integer targetTrackId;
    private final Integer roundId;
    private final List<CriteriaCloneSourceItemResponse> sources;
}
