package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalJudgeAssignmentResponse {

    private Integer roundId;
    private List<Integer> judgeIds;
}
