package com.sealhackathon.api.rounds.dto.response;

import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiebreakItemResponse {

    private String partitionKey;
    private Integer cutoffRank;
    private List<Integer> candidateTeamIds;
    private TiebreakRule tiebreakRule;
    private String reason;
    private Boolean requiresManualReorder;
    private List<Integer> suggestedOrderedTeamIds;
}
