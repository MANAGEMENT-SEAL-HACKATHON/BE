package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorRankingResponse {

    private Integer hackathonId;
    private List<MentorRankingItem> teamRankings;
    private List<MentorRankingItem> chapterRankings;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MentorRankingItem {
        private Integer rank;
        private Integer teamId;
        private String teamName;
    }
}
