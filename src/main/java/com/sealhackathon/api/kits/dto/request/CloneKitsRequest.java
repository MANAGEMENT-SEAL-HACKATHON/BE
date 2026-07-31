package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloneKitsRequest {

    @NotNull
    private Integer sourceHackathonId;

    /** When false, stock rows are created with quantityTotal=0 (size/fit frame only). */
    @Builder.Default
    private Boolean includeStockQuantities = false;

    @Builder.Default
    private Boolean includeBundles = true;
}
