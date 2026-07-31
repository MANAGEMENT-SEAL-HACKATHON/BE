package com.sealhackathon.api.kits.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CloneKitsResponse {

    private final Integer sourceHackathonId;
    private final Integer targetHackathonId;
    private final int itemsCloned;
    private final int itemsSkipped;
    private final int bundlesCloned;
    private final List<Integer> newItemIds;
    private final List<Integer> newBundleIds;
}
