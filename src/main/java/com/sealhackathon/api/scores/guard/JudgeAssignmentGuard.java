package com.sealhackathon.api.scores.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** FR-18 — judge phải được phân công track/round của submission. */
@Component
@RequiredArgsConstructor
public class JudgeAssignmentGuard {

    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public void requireJudgeForSubmission(Integer judgeId, Submission submission) {
        if (submission.getTrack() != null) {
            if (!judgeAssignmentRepository.existsByJudgeIdAndTrackId(judgeId, submission.getTrack().getId())) {
                throw new AuthException(ErrorCode.JUDGE_NOT_ASSIGNED_TO_TRACK,
                        "Judge chưa được phân công cho track này", HttpStatus.FORBIDDEN);
            }
            return;
        }
        Integer roundId = submission.getRound().getId();
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw new AuthException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Judge chưa được phân công cho round này", HttpStatus.FORBIDDEN);
        }
    }

    public boolean canAccessRound(Integer userId, Integer roundId, boolean isCoordinator) {
        if (isCoordinator) {
            return true;
        }
        return judgeAssignmentRepository.existsByJudgeIdAndRoundScope(userId, roundId);
    }

    public boolean canAccessTrack(Integer userId, Integer trackId, boolean isCoordinator) {
        if (isCoordinator) {
            return true;
        }
        return judgeAssignmentRepository.existsByJudgeIdAndTrackId(userId, trackId);
    }
}
