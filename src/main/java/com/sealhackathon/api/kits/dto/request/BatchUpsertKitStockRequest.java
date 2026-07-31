package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchUpsertKitStockRequest {

    @NotEmpty
    @Valid
    private List<UpsertKitStockRequest> stocks;
}
