package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Kiểm tra trước {@code queue/next} — không chặn khi đã có điểm và judge đã chấm đủ theo nghiệp vụ thực tế.
 *
 * <p>Quy tắc (không xiết từng criterion):
 * <ul>
 *   <li>Chưa có điểm NORMAL nào → chặn (tránh next nhầm khi chưa chấm).</li>
 *   <li>Có điểm nhưng chưa đủ judge trên track chấm ít nhất 1 lần → cần
 *       {@code acknowledgeIncompleteScoring=true} (FE confirm).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PresentationNextScoringGuard {

    private final ScoreRepository scoreRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public PresentationQueueNextResponse.ScoringSnapshot snapshot(
            Submission submission, Integer trackId, Round round) {
        if (submission == null || submission.getId() == null) {
            return null;
        }
        int judgesAssigned = countAssignedJudges(trackId, round);
        long scoreCount = scoreRepository.countBySubmission_IdAndScoreType(
                submission.getId(), ScoreType.NORMAL);
        int judgesScored = (int) scoreRepository.findBySubmission_IdAndScoreType(
                        submission.getId(), ScoreType.NORMAL).stream()
                .map(s -> s.getJudge().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
        boolean incomplete = scoreCount == 0
                || (judgesAssigned > 1 && judgesScored < judgesAssigned);
        return PresentationQueueNextResponse.ScoringSnapshot.builder()
                .submissionId(submission.getId())
                .judgesAssigned(judgesAssigned)
                .judgesScored(judgesScored)
                .scoreCount(scoreCount)
                .incomplete(incomplete)
                .build();
    }

    public void validateBeforeNext(
            Submission submission, Integer trackId, Round round, boolean acknowledgeIncompleteScoring) {
        PresentationQueueNextResponse.ScoringSnapshot snap = snapshot(submission, trackId, round);
        if (snap == null) {
            return;
        }
        if (snap.getScoreCount() == 0) {
            throw new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT,
                    "Chưa có điểm nào cho bài đang thuyết trình — không thể chuyển đội",
                    Map.of(
                            "submissionId", snap.getSubmissionId(),
                            "reason", "NO_SCORES",
                            "judgesAssigned", snap.getJudgesAssigned()));
        }
        if (snap.isIncomplete() && !acknowledgeIncompleteScoring) {
            throw new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT,
                    "Chưa đủ judge chấm cho bài này — gửi acknowledgeIncompleteScoring=true sau khi xác nhận",
                    Map.of(
                            "submissionId", snap.getSubmissionId(),
                            "reason", "MISSING_JUDGE_SCORES",
                            "judgesAssigned", snap.getJudgesAssigned(),
                            "judgesScored", snap.getJudgesScored(),
                            "scoreCount", snap.getScoreCount()));
        }
    }

    private int countAssignedJudges(Integer trackId, Round round) {
        if (trackId != null) {
            return judgeAssignmentRepository.findByTrackId(trackId).size();
        }
        if (round != null && round.getId() != null) {
            return judgeAssignmentRepository.findByRoundId(round.getId()).size();
        }
        return 0;
    }
}
