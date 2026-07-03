package com.sealhackathon.api.chapters.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterRankingItemResponse {

    private Integer chapterId;
    private String chapterName;
    private Float bestTeamScore;
    private Float totalScore;
    private Integer rank;
    private Integer teamsParticipated;
    private Integer prizesWon;
}
