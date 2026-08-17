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
        /** Prelim: ADVANCED|ELIMINATED|PARTICIPATING from TRT. Final: always PARTICIPATING. */
        private String participationStatus;
        /**
         * Timeline display for final rounds: COMPLETED | PARTICIPATING | UPCOMING.
         * Null for preliminary (FE falls back to participationStatus).
         */
        private String displayStatus;
        private Boolean isFinalRound;
        private Boolean scoringLocked;
        private Boolean resultPublished;
        private String hackathonStatus;
    }
}
