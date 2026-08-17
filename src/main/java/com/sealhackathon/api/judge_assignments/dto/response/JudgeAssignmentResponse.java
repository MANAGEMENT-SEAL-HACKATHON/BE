package com.sealhackathon.api.judge_assignments.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JudgeAssignmentResponse {

    private final Integer id;
    private final Integer judgeId;
    private final String judgeFullName;
    private final String judgeEmail;
    private final Boolean judgeIsTemp;
    private final Integer roundId;
    private final String roundName;
    private final Integer trackId;
    private final String trackName;
    private final JudgeAssignmentType assignmentType;
    private final LocalDateTime assignedAt;
    private final Integer assignedById;
}
