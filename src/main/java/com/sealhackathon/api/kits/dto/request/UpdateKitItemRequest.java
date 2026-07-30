package com.sealhackathon.api.kits.dto.request;

import com.sealhackathon.api.kits.value_object.KitItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateKitItemRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private KitItemType type;

    private Boolean hasSize;
}
