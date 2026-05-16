package com.se194093.be.criteria.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * FR-04 POST /api/v1/rounds/{roundId}/criteria/batch — tạo bulk trong 1 transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchCreateCriteriaRequest {

    @NotEmpty
    @Size(max = 20, message = "Tối đa 20 criterion mỗi batch")
    @Valid
    private List<CreateCriterionRequest> items;
}
