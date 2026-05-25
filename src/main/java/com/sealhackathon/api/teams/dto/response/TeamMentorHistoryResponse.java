package com.sealhackathon.api.teams.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** FR-13C GET /api/v1/teams/{id}/mentors */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMentorHistoryResponse {

    private Integer teamId;
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Integer roundId;
        private String roundName;
        private Integer mentorId;
        private String mentorName;
        private LocalDateTime assignedAt;
    }
}
