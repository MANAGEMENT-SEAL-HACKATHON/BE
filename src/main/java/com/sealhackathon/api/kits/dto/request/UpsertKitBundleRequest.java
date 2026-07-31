package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertKitBundleRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private Boolean isDefault;

    @NotEmpty
    @Valid
    private List<BundleItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BundleItemRequest {

        @NotNull
        private Integer kitItemId;

        @Min(1)
        @Builder.Default
        private Integer quantity = 1;
    }
}
