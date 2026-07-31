package com.sealhackathon.api.kits.dto.response;

import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import com.sealhackathon.api.kits.value_object.KitItemType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitAllocationResponse {

    private Integer id;
    private Integer hackathonId;
    private Integer userId;
    private Integer kitItemId;
    private String kitItemName;
    private KitItemType kitItemType;
    private String size;
    private String fit;
    private KitAllocationStatus status;
    private LocalDateTime issuedAt;
    private Integer issuedById;
    private String note;
}
