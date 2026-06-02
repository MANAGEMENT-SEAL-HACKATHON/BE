package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorTeamScoreResponse {

    private Integer roundId;
    private BigDecimal totalScore;
    private Boolean scoringLocked;
}
