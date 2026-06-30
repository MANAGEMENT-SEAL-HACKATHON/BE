package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Điều kiện chuyển {@code queue/next}: mỗi judge được phân công track/round
 * phải chấm đủ tiêu chí (Chốt điểm) — không cần bước "Xác nhận" riêng.
 */
@Component
@RequiredArgsConstructor
public class PresentationScoringCompletionHelper {

    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;

    public int countAssignedJudges(Integer trackId, Integer roundId) {
        if (trackId != null) {
            return judgeAssignmentRepository.findByTrackId(trackId).size();
        }
        if (roundId != null) {
            return judgeAssignmentRepository.findByRoundId(roundId).size();
        }
        return 0;
    }

    public int countAssignedJudges(Integer trackId, Round round) {
        return countAssignedJudges(trackId, round != null ? round.getId() : null);
    }

    public int countDistinctJudgesWithAnyScore(Integer submissionId) {
        return (int) scoreRepository.findBySubmission_IdAndScoreType(submissionId, ScoreType.NORMAL).stream()
                .map(s -> s.getJudge().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    public int countJudgesFullyScored(Submission submission) {
        if (submission == null || submission.getId() == null) {
            return 0;
        }
        long criteriaCount = criteriaCountFor(submission);
        if (criteriaCount == 0) {
            return 0;
        }
        return (int) scoreRepository.findBySubmission_IdAndScoreType(submission.getId(), ScoreType.NORMAL).stream()
                .filter(s -> s.getJudge() != null && s.getJudge().getId() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getJudge().getId(),
                        Collectors.mapping(
                                s -> s.getCriterion().getId(),
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))))
                .values().stream()
                .filter(distinctCriteria -> distinctCriteria >= criteriaCount)
                .count();
    }

    public boolean hasJudgeFullyScored(Integer judgeId, Submission submission) {
        if (judgeId == null || submission == null || submission.getId() == null) {
            return false;
        }
        long criteriaCount = criteriaCountFor(submission);
        if (criteriaCount == 0) {
            return false;
        }
        long myCriteriaScored = scoreRepository
                .findBySubmission_IdAndScoreType(submission.getId(), ScoreType.NORMAL).stream()
                .filter(s -> judgeId.equals(s.getJudge().getId()))
                .map(s -> s.getCriterion().getId())
                .distinct()
                .count();
        return myCriteriaScored >= criteriaCount;
    }

    public int countJudgesConfirmed(Integer submissionId) {
        if (submissionId == null) {
            return 0;
        }
        return scoringConfirmationRepository.countDistinctJudgesBySubmissionId(submissionId);
    }

    /**
     * Đủ điều kiện chuyển đội tiếp.
     * Sơ loại (GD3): 2+ judge → confirm; 1 judge → đủ điểm tiêu chí.
     * Chung kết (GD5): luôn bắt buộc confirm (Chốt điểm).
     */
    public boolean canAdvanceQueue(Submission submission, Integer trackId, Integer roundId) {
        if (submission == null || submission.getId() == null) {
            return false;
        }
        int judgesAssigned = Math.max(1, countAssignedJudges(trackId, roundId));
        if (isFinalSubmission(submission) || judgesAssigned >= 2) {
            return countJudgesConfirmed(submission.getId()) >= judgesAssigned;
        }
        return countJudgesFullyScored(submission) >= judgesAssigned;
    }

    public boolean isScoringIncomplete(Submission submission, Integer trackId, Round round) {
        if (submission == null || submission.getId() == null) {
            return true;
        }
        int judgesAssigned = Math.max(1, countAssignedJudges(trackId, round));
        if ((round != null && Boolean.TRUE.equals(round.getIsFinal())) || judgesAssigned >= 2) {
            return countJudgesConfirmed(submission.getId()) < judgesAssigned;
        }
        long scoreCount = scoreRepository.countBySubmission_IdAndScoreType(submission.getId(), ScoreType.NORMAL);
        if (scoreCount == 0) {
            return true;
        }
        return countJudgesFullyScored(submission) < judgesAssigned;
    }

    private boolean isFinalSubmission(Submission submission) {
        if (submission.getRound() != null && Boolean.TRUE.equals(submission.getRound().getIsFinal())) {
            return true;
        }
        if (submission.getTrack() != null
                && submission.getTrack().getRound() != null
                && Boolean.TRUE.equals(submission.getTrack().getRound().getIsFinal())) {
            return true;
        }
        return false;
    }

    private long criteriaCountFor(Submission submission) {
        if (submission == null || submission.getRound() == null) {
            return 0;
        }
        Round round = submission.getRound();
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return criteriaRepository.countNormalByFinalRoundId(round.getId());
        }
        if (submission.getTrack() != null) {
            return criteriaRepository.countNormalByTrackId(submission.getTrack().getId());
        }
        return 0;
    }
}
