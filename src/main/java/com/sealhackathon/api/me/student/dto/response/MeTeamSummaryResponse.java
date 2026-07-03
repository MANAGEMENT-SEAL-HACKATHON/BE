package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeTeamSummaryResponse {

    private Integer teamId;
    private String teamName;
    private Integer hackathonId;
    private Integer leaderId;
    private String status;
    private Integer trackId;
    private String trackName;
    private String lotteryStatus;
}
