package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLeaderboardItemResponse {

    private Integer rank;
    private Integer teamId;
    private String teamName;
    private BigDecimal totalScore;
}
