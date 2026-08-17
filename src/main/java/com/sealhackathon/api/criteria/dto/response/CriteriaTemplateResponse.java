package com.sealhackathon.api.criteria.dto.response;

import com.sealhackathon.api.criteria.value_object.CriteriaType;

import java.time.LocalDateTime;
import java.util.List;

public record CriteriaTemplateResponse(
        Integer id,
        String name,
        String description,
        Boolean isDefault,
        Integer createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Item> items
) {
    public record Item(
            Integer id,
            String name,
            CriteriaType type,
            Float weight,
            Integer maxScore,
            String description,
            Integer displayOrder,
            Boolean isTiebreakerPriority
    ) {}
}
