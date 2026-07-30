package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokeKitRequest {

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
