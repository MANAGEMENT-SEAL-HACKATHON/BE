package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeFinalAssignmentResponse {

    private Integer assignmentId;
    private Integer hackathonId;
    private String hackathonName;
    private Integer roundId;
    private String roundName;
    private String role;
    /** PENDING | ACCEPTED | DECLINED — default ACCEPTED. */
    private String responseStatus;
    private String declineReason;
}
