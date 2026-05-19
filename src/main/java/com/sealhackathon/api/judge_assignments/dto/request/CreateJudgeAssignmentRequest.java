package com.sealhackathon.api.judge_assignments.dto.request;

import com.sealhackathon.api.common.validation.XorTrackOrRoundId;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-05c POST /api/v1/judge-assignments — XOR trackId (Sơ loại) hoặc roundId (Chung kết).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XorTrackOrRoundId
public class CreateJudgeAssignmentRequest {

    @NotNull
    private Integer judgeId;

    private Integer trackId;

    private Integer roundId;

    private JudgeAssignmentType assignmentType;
}
