package com.sealhackathon.api.kits.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueKitBundleResponse {

    private List<KitAllocationResponse> issued;
    private List<KitAllocationResponse> skipped;
}
