package com.sealhackathon.api.criteria.dto.request;

import com.sealhackathon.api.criteria.value_object.CriteriaType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-04 POST /api/v1/rounds/{roundId}/criteria
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCriterionRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private CriteriaType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "weight phải > 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "weight phải <= 1.0")
    private Float weight;

    @NotNull
    @Min(1)
    private Integer maxScore;

    private String description;

    private String rubricUrl;

    @Min(0)
    private Integer displayOrder;
}
