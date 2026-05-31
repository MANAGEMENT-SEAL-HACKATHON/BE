package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalTeamRankingItemResponse {

    private Integer rank;
    private Integer teamId;
    private String teamName;
    private Integer chapterId;
    private String chapterName;
    private Double weightedAvgScore;
    private Integer judgeCount;
}
