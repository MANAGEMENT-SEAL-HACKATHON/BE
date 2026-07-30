package com.sealhackathon.api.kits.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitStockResponse {

    private Integer id;
    private String size;
    private Integer quantityTotal;
    private Integer quantityIssued;
    private Integer remaining;
    private Long version;
}
