package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorTeamAssignmentResponse {

    private Integer assignmentId;
    private Integer teamId;
    private String teamName;
    private Integer hackathonId;
    private String responseStatus;
    private String declineReason;
}
