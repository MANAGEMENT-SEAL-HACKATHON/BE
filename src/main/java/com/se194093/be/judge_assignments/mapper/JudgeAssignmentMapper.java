package com.se194093.be.judge_assignments.mapper;

import com.se194093.be.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.tracks.entity.Track;
import org.springframework.stereotype.Component;

/**
 * [BC-07] JudgeAssignment dùng XOR FK (track_id hoặc round_id).
 * Mapper resolve {@code roundId} qua {@code round} hoặc fallback
 * {@code track.round}, và {@code trackId} qua {@code track} trực tiếp.
 */
@Component
public class JudgeAssignmentMapper {

    public JudgeAssignmentResponse toResponse(JudgeAssignment e) {
        if (e == null) {
            return null;
        }
        Track track = e.getTrack();
        Round round = e.getRound();
        Integer roundId = null;
        String roundName = null;
        if (round != null) {
            roundId = round.getId();
            roundName = round.getName();
        } else if (track != null && track.getRound() != null) {
            roundId = track.getRound().getId();
            roundName = track.getRound().getName();
        }
        return JudgeAssignmentResponse.builder()
                .id(e.getId())
                .judgeId(e.getJudge() == null ? null : e.getJudge().getId())
                .judgeFullName(e.getJudge() == null ? null : e.getJudge().getFullName())
                .judgeEmail(e.getJudge() == null ? null : e.getJudge().getEmail())
                .judgeIsTemp(e.getJudge() == null ? null : e.getJudge().getIsTempAccount())
                .roundId(roundId)
                .roundName(roundName)
                .trackId(track == null ? null : track.getId())
                .trackName(track == null ? null : track.getName())
                .assignmentType(e.getAssignmentType())
                .assignedAt(e.getAssignedAt())
                .assignedById(e.getAssignedBy() == null ? null : e.getAssignedBy().getId())
                .build();
    }
}
