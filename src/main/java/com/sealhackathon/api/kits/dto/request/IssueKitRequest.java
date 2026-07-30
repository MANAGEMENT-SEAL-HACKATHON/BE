package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueKitRequest {

    @NotNull
    private Integer userId;

    @NotNull
    private Integer kitItemId;

    /** Optional override when preferred size is missing or wrong at desk. */
    @Size(max = 10)
    private String size;

    @Size(max = 1000)
    private String note;
}
