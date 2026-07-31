package com.sealhackathon.api.kits.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class KitCloneSourcesResponse {

    private final Integer targetHackathonId;
    private final List<Source> sources;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Source {
        private final Integer hackathonId;
        private final String hackathonName;
        private final Integer itemCount;
        private final Integer bundleCount;
    }
}
