package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeTrackAssignmentResponse {

    private Integer assignmentId;
    private Integer hackathonId;
    private String hackathonName;
    private Integer trackId;
    private String trackName;
    private Integer roundId;
    private String roundName;
    /** FR-J-07 — NORMAL | HEAD (stub until DB completion_status migration). */
    private String assignmentType;
    private String completionStatus;
    /** Số đội cần chấm trong track (bài nộp gradable). */
    private Integer totalTeams;
    /** Số đội judge này đã chấm đủ tiêu chí. */
    private Integer scoredTeams;
    /** PENDING | ACCEPTED | DECLINED — default ACCEPTED. */
    private String responseStatus;
    private String declineReason;
}
