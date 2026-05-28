package com.sealhackathon.api.teams.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamJourneyResponse {

    private Integer teamId;
    private String teamName;
    private List<JourneyStep> steps;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JourneyStep {
        private Integer roundId;
        private String roundName;
        private Integer trackId;
        private String trackName;
        private String participationStatus;
    }
}
