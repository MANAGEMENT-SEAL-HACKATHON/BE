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
    private Integer trackId;
    private String trackName;
    private Integer roundId;
    /** FR-J-07 — NORMAL | HEAD (stub until DB completion_status migration). */
    private String assignmentType;
    private String completionStatus;
}
