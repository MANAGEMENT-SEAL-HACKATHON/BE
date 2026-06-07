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
public class MentorAssignedTeamsResponse {

    private String roundName;
    private String roundStatus;
    private List<TeamItem> teams;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamItem {
        private Integer teamId;
        private String teamName;
        private Integer groupNumber;
        private String status;
        private String presentationSchedule;
        private String location;
    }
}
