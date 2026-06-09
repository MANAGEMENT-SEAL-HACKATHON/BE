package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationShuffleResponse {

    private Integer roundId;
    private List<TrackShuffleResult> tracks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrackShuffleResult {
        private Integer trackId;
        private int slotCount;
        private boolean shuffled;
    }
}
