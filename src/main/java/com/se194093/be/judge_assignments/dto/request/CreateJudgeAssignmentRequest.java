package com.se194093.be.judge_assignments.dto.request;

import com.se194093.be.judge_assignments.value_object.JudgeAssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-05c POST /api/v1/judge-assignments
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJudgeAssignmentRequest {

    @NotNull
    private Integer judgeId;

    @NotNull
    private Integer roundId;

    /**
     * Mặc định NORMAL nếu không truyền.
     */
    private JudgeAssignmentType assignmentType;
}
