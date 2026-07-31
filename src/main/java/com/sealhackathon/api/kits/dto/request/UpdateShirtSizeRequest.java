package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShirtSizeRequest {

    @NotBlank
    @Size(max = 10)
    private String preferredShirtSize;

    /** Optional; defaults to UNISEX when blank. */
    @Size(max = 20)
    private String preferredShirtFit;
}
