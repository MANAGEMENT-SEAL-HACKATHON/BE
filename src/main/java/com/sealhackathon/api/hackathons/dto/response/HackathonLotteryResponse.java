package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonLotteryResponse {

    private Integer hackathonId;
    private Integer roundId;
    private int assignedCount;
    private List<Integer> teamIds;
}
