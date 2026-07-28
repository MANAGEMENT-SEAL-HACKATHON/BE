package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kiểm tra trước {@code queue/next} — và trước kết thúc sớm Q&A.
 * <ul>
 *   <li>Luôn chặn khi chưa có điểm nào ({@code NO_SCORES}), trừ force-ack.</li>
 *   <li>Thiếu chốt điểm: chặn khi Q&A kết thúc sớm ({@code requireCompleteScoring=true}),
 *       trừ force-ack Coord/controller. Hết giờ tự nhiên → ghi nhận tới đâu, thiếu cũng được.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PresentationNextScoringGuard {

    private final ScoreRepository scoreRepository;
    private final PresentationScoringCompletionHelper scoringCompletionHelper;

    public PresentationQueueNextResponse.ScoringSnapshot snapshot(
            Submission submission, Integer trackId, Round round) {
        if (submission == null || submission.getId() == null) {
            return null;
        }
        int judgesAssigned = Math.max(1, scoringCompletionHelper.countAssignedJudges(trackId, round));
        long scoreCount = scoreRepository.countBySubmission_IdAndScoreType(
                submission.getId(), ScoreType.NORMAL);
        int judgesScored = scoringCompletionHelper.countDistinctJudgesWithAnyScore(submission.getId());
        int judgesFullyScored = scoringCompletionHelper.countJudgesFullyScored(submission);
        boolean incomplete = scoringCompletionHelper.isScoringIncomplete(submission, trackId, round);
        return PresentationQueueNextResponse.ScoringSnapshot.builder()
                .submissionId(submission.getId())
                .judgesAssigned(judgesAssigned)
                .judgesScored(judgesScored)
                .judgesFullyScored(judgesFullyScored)
                .scoreCount(scoreCount)
                .incomplete(incomplete)
                .build();
    }

    public void validateBeforeNext(
            Submission submission, Integer trackId, Round round, boolean acknowledgeIncompleteScoring) {
        validateBeforeNext(submission, trackId, round, acknowledgeIncompleteScoring, true);
    }

    /**
     * @param requireCompleteScoring true = kết thúc sớm Q&A / slot cũ (null qaEndedEarly);
     *                               false = hết giờ Q&A tự nhiên — cho phép thiếu điểm
     */
    public void validateBeforeNext(
            Submission submission,
            Integer trackId,
            Round round,
            boolean acknowledgeIncompleteScoring,
            boolean requireCompleteScoring) {
        PresentationQueueNextResponse.ScoringSnapshot snap = snapshot(submission, trackId, round);
        if (snap == null) {
            return;
        }
        if (snap.getScoreCount() == 0 && !acknowledgeIncompleteScoring) {
            throw new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT,
                    "Chưa có điểm nào cho bài đang thuyết trình — không thể chuyển đội",
                    Map.of(
                            "submissionId", snap.getSubmissionId(),
                            "reason", "NO_SCORES",
                            "judgesAssigned", snap.getJudgesAssigned()));
        }
        if (requireCompleteScoring && snap.isIncomplete() && !acknowledgeIncompleteScoring) {
            throw new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT,
                    "Chưa đủ judge Chốt điểm — mỗi judge cần chấm đủ tiêu chí rồi bấm Chốt điểm",
                    Map.of(
                            "submissionId", snap.getSubmissionId(),
                            "reason", "MISSING_JUDGE_SCORES",
                            "judgesAssigned", snap.getJudgesAssigned(),
                            "judgesScored", snap.getJudgesScored(),
                            "judgesFullyScored", snap.getJudgesFullyScored(),
                            "scoreCount", snap.getScoreCount()));
        }
    }
}
