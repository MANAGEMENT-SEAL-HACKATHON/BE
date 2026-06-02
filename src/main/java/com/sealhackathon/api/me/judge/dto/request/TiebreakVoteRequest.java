package com.sealhackathon.api.me.judge.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TiebreakVoteRequest {

    @NotNull
    private Integer roundId;

    @NotEmpty
    private List<Integer> orderedTeamIds;
}
