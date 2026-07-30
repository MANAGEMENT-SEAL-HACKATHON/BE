package com.sealhackathon.api.kits.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitReconciliationLineResponse {

    private Integer kitItemId;
    private String kitItemName;
    private String size;
    private Integer quantityTotal;
    private Integer quantityIssued;
    private Integer remaining;
    private Integer eligibleCount;
    /** issued - eligible (negative = under-issued). */
    private Integer variance;
}
