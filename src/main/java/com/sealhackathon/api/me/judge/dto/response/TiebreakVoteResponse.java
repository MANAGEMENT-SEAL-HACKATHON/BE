package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiebreakVoteResponse {

    private Integer roundId;
    private List<Integer> orderedTeamIds;
    private String status;
}
