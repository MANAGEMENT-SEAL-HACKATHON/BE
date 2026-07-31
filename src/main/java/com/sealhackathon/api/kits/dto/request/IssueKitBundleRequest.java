package com.sealhackathon.api.kits.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueKitBundleRequest {

    @NotNull
    private Integer userId;

    @NotNull
    private Integer bundleId;

    /** Applied only to SHIRT items in the bundle. */
    @Size(max = 10)
    private String size;

    /** Applied only to SHIRT items in the bundle. */
    @Size(max = 20)
    private String fit;

    @Size(max = 1000)
    private String note;
}
