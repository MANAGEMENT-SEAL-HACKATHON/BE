package com.sealhackathon.api.rounds.dto.response;

import com.sealhackathon.api.common.response.Warning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** FR-27 — payload + warnings tách biệt (warnings chỉ ở envelope ApiResponse). */
@Getter
@Builder
@AllArgsConstructor
public class AssignFinalJudgesResult {

    private FinalJudgeAssignmentResponse assignment;
    private List<Warning> warnings;
}
