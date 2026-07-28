package com.sealhackathon.api.criteria.dto.request;

import com.sealhackathon.api.criteria.value_object.CriteriaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CriteriaTemplateRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        Boolean isDefault,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank @Size(max = 200) String name,
            @NotNull CriteriaType type,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") Float weight,
            @NotNull @Min(1) Integer maxScore,
            String description,
            @NotNull @Min(0) Integer displayOrder
    ) {}
}
