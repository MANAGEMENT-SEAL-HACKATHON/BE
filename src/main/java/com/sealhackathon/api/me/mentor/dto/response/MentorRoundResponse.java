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
public class MentorRoundResponse {

    private Integer roundId;
    private String roundName;
    private String status;
    private String description;
    private int teamCount;
    private List<TeamInfo> teams;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamInfo {
        private Integer teamId;
        private String teamName;
    }
}
