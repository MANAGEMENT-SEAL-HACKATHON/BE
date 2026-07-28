package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignFinalJudgesRequest {

    @NotEmpty
    private List<Integer> judgeIds;

    /** HEAD hoặc FINAL_EXTERNAL — mặc định FINAL_EXTERNAL. */
    private JudgeAssignmentType assignmentType;
}
