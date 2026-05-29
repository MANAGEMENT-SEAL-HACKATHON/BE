package com.sealhackathon.api.scores.guard;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** FR-18 — chặn user vừa mentor vừa judge cùng track. */
@Component
@RequiredArgsConstructor
public class MentorJudgeConflictGuard {

    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public void requireNoConflict(Integer judgeId, Submission submission) {
        if (submission.getTrack() == null) {
            return;
        }
        Integer trackId = submission.getTrack().getId();
        boolean isMentor = mentorAssignmentRepository.findByTrackId(trackId).stream()
                .anyMatch(ma -> ma.getMentor().getId().equals(judgeId));
        if (isMentor && judgeAssignmentRepository.existsByJudgeIdAndTrackId(judgeId, trackId)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK,
                    "Không thể chấm: user vừa Mentor vừa Judge cùng track",
                    Map.of("trackId", trackId, "userId", judgeId));
        }
    }
}
