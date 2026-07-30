package com.sealhackathon.api.kits.dto.response;

import com.sealhackathon.api.kits.value_object.KitItemType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitItemResponse {

    private Integer id;
    private Integer hackathonId;
    private String name;
    private KitItemType type;
    private Boolean hasSize;
    private List<KitStockResponse> stocks;
}
