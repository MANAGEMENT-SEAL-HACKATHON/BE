package com.sealhackathon.api.events.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuffetMenuItemResponse {

    private final Integer id;
    private final Integer eventId;
    private final String name;
    private final Integer quantity;
    private final String unit;
    private final String note;
    private final Integer displayOrder;
}
