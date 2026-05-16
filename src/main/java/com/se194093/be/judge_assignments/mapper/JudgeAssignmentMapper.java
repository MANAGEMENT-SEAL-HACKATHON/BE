package com.se194093.be.judge_assignments.mapper;

import com.se194093.be.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import org.springframework.stereotype.Component;

@Component
public class JudgeAssignmentMapper {

    public JudgeAssignmentResponse toResponse(JudgeAssignment e) {
        if (e == null) {
            return null;
        }
        return JudgeAssignmentResponse.builder()
                .id(e.getId())
                .judgeId(e.getJudge() == null ? null : e.getJudge().getId())
                .judgeFullName(e.getJudge() == null ? null : e.getJudge().getFullName())
                .judgeEmail(e.getJudge() == null ? null : e.getJudge().getEmail())
                .judgeIsTemp(e.getJudge() == null ? null : e.getJudge().getIsTempAccount())
                .roundId(e.getRound() == null ? null : e.getRound().getId())
                .roundName(e.getRound() == null ? null : e.getRound().getName())
                .trackId(e.getRound() == null || e.getRound().getTrack() == null
                        ? null : e.getRound().getTrack().getId())
                .trackName(e.getRound() == null || e.getRound().getTrack() == null
                        ? null : e.getRound().getTrack().getName())
                .assignmentType(e.getAssignmentType())
                .assignedAt(e.getAssignedAt())
                .assignedById(e.getAssignedBy() == null ? null : e.getAssignedBy().getId())
                .build();
    }
}
