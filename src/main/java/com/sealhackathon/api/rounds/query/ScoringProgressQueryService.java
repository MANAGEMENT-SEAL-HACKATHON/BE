package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FR-20A — tiến độ chấm điểm theo round. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringProgressQueryService {

    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public RoundScoringProgressResponse progressForRound(Round round) {
        Integer roundId = round.getId();
        List<Submission> submissions = mergeRoundSubmissions(roundId);
        int gradable = 0;
        int scored = 0;
        for (Submission s : submissions) {
            if (!SubmissionGradablePolicy.isGradable(s)) {
                continue;
            }
            gradable++;
            if (isFullyScored(s, round)) {
                scored++;
            }
        }
        return RoundScoringProgressResponse.builder()
                .roundId(roundId)
                .totalSubmissions(gradable)
                .scoredSubmissions(scored)
                .pendingSubmissions(Math.max(0, gradable - scored))
                .scoringLocked(round.getScoringLocked())
                .build();
    }

    private boolean isFullyScored(Submission submission, Round round) {
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return isFullyScoredForFinalRound(submission, round.getScoringLocked());
        }
        return isFullyScoredForPrelimTrack(submission, round.getScoringLocked());
    }

    /** GD5 — tiêu chí gắn round (track null), submission có thể không có track. */
    private boolean isFullyScoredForFinalRound(Submission submission, Boolean scoringLocked) {
        Integer roundId = submission.getRound() != null
                ? submission.getRound().getId()
                : null;
        if (roundId == null) {
            return false;
        }
        boolean useFinal = Boolean.TRUE.equals(scoringLocked);
        long requiredJudges = Math.max(1, judgeAssignmentRepository.findByRoundId(roundId).size());
        List<Criteria> criteria = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
        if (criteria.isEmpty()) {
            return false;
        }
        for (Criteria criterion : criteria) {
            long count = scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                    submission.getId(), criterion.getId(), ScoreType.NORMAL, useFinal);
            if (count < requiredJudges) {
                return false;
            }
        }
        return true;
    }

    /** GD3 — tiêu chí theo track (giữ nguyên). */
    private boolean isFullyScoredForPrelimTrack(Submission submission, Boolean scoringLocked) {
        if (submission.getTrack() == null) {
            return false;
        }
        boolean useFinal = Boolean.TRUE.equals(scoringLocked);
        long requiredJudges = Math.max(1,
                judgeAssignmentRepository.findByTrackId(submission.getTrack().getId()).size());
        List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(
                        submission.getTrack().getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
        if (criteria.isEmpty()) {
            return false;
        }
        for (Criteria criterion : criteria) {
            long count = scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                    submission.getId(), criterion.getId(), ScoreType.NORMAL, useFinal);
            if (count < requiredJudges) {
                return false;
            }
        }
        return true;
    }

    private List<Submission> mergeRoundSubmissions(Integer roundId) {
        Map<Integer, Submission> byId = new HashMap<>();
        for (Submission s : submissionRepository.findByRound_Id(roundId)) {
            byId.put(s.getId(), s);
        }
        for (Submission s : submissionRepository.findByTrack_Round_Id(roundId)) {
            byId.putIfAbsent(s.getId(), s);
        }
        return new ArrayList<>(byId.values());
    }
}
