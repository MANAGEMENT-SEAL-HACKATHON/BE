package com.sealhackathon.api.kits.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShirtSizeResponse {

    private Integer hackathonId;
    private String preferredShirtSize;
    private String preferredShirtFit;
}
