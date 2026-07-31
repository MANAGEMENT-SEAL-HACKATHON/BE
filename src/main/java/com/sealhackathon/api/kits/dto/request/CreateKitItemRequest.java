package com.sealhackathon.api.kits.dto.request;

import com.sealhackathon.api.kits.value_object.KitItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateKitItemRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private KitItemType type;

    private Boolean hasSize;

    /** Optional initial stock lines (fit/size/qty). Omitted = create item only. */
    @Valid
    private List<UpsertKitStockRequest> stocks;
}
