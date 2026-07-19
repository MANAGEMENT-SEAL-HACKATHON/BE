package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Per-team scoring row for Coord progress panel. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringProgressItemResponse {

    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    private Integer trackId;
    private String trackName;
    private boolean scored;
}
