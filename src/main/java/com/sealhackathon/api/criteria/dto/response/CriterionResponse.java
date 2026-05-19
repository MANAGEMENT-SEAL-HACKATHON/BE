package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriterionResponse {

    private final Integer id;
    private final Integer trackId;
    private final Integer roundId;
    private final Integer sourceCriteriaId;
    private final String name;
    private final CriteriaType type;
    private final Float weight;
    private final Integer maxScore;
    private final String description;
    private final String rubricUrl;
    private final Integer displayOrder;
}
