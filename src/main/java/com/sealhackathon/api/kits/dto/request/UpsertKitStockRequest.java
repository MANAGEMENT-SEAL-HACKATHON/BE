package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertKitStockRequest {

    /** Null/blank for items without size. */
    @Size(max = 10)
    private String size;

    @NotNull
    @Min(0)
    private Integer quantityTotal;
}
