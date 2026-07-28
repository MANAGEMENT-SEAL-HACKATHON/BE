package com.sealhackathon.api.scores.guard;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** FR-18 / FR-13C — chặn user vừa mentor vừa judge cùng track hoặc mentor đội mình chấm. */
@Component
@RequiredArgsConstructor
public class MentorJudgeConflictGuard {

    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public void requireNoConflict(Integer judgeId, Submission submission) {
        Integer teamId = submission.getTeam().getId();
        Integer roundId = resolveRoundId(submission);

        if (roundId != null
                && mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(judgeId, teamId)
                && isJudgeForSubmission(judgeId, submission, roundId)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK,
                    "Không thể chấm: user là Mentor của đội này trong vòng thi",
                    Map.of("teamId", teamId, "roundId", roundId, "userId", judgeId));
        }

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

    private static Integer resolveRoundId(Submission submission) {
        if (submission.getRound() != null) {
            return submission.getRound().getId();
        }
        if (submission.getTrack() != null && submission.getTrack().getRound() != null) {
            return submission.getTrack().getRound().getId();
        }
        return null;
    }

    private boolean isJudgeForSubmission(Integer judgeId, Submission submission, Integer roundId) {
        if (submission.getTrack() != null) {
            return judgeAssignmentRepository.existsByJudgeIdAndTrackId(judgeId, submission.getTrack().getId());
        }
        return judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId);
    }
}
