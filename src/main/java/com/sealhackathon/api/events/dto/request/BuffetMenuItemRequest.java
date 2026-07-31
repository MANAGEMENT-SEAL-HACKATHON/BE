package com.sealhackathon.api.events.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuffetMenuItemRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    @Min(1)
    private Integer quantity;

    @Size(max = 30)
    private String unit;

    @Size(max = 500)
    private String note;

    private Integer displayOrder;
}
