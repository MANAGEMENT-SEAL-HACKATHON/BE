package com.sealhackathon.api.scores.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.live_scoring.event.LiveScoreSavedEvent;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.scores.guard.MentorJudgeConflictGuard;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.dto.request.SubmitCalibrationScoreRequest;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.service.ScoreService;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final CriteriaRepository criteriaRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final JudgeAssignmentGuard judgeAssignmentGuard;
    private final MentorJudgeConflictGuard mentorJudgeConflictGuard;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ScoreResponse submitScore(SubmitScoreRequest req) {
        Integer judgeId = currentUserAccessor.currentUserId();
        Submission submission = submissionRepository.findById(req.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission", req.getSubmissionId()));
        Criteria criterion = criteriaRepository.findById(req.getCriterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Criteria", req.getCriterionId()));

        validateCriterionForSubmission(submission, criterion);
        if (!SubmissionGradablePolicy.isGradable(submission)) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_NOT_GRADABLE,
                    "Bài nộp không ở trạng thái cho phép chấm điểm");
        }
        if (submission.getTrack() != null) {
            teamRoundTrackRepository.findByTeam_IdAndTrack_Id(
                            submission.getTeam().getId(), submission.getTrack().getId())
                    .filter(trt -> trt.getParticipationStatus() == ParticipationStatus.ELIMINATED)
                    .ifPresent(trt -> {
                        throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                                "Đội đã bị loại khỏi vòng — không thể chấm điểm");
                    });
        }
        if (req.getScoreValue() > criterion.getMaxScore()) {
            throw new BusinessRuleException(ErrorCode.SCORE_EXCEEDS_MAX,
                    "Điểm vượt max_score của tiêu chí",
                    Map.of("maxScore", criterion.getMaxScore(), "given", req.getScoreValue()));
        }

        judgeAssignmentGuard.requireJudgeForSubmission(judgeId, submission);
        mentorJudgeConflictGuard.requireNoConflict(judgeId, submission);

        Integer roundId = submission.getRound().getId();
        Round round = roundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new com.sealhackathon.api.common.exception.ScoringLockedException(
                    "Round đã khóa chấm điểm");
        }

        ScoreType scoreType = req.getScoreType() != null ? req.getScoreType() : ScoreType.NORMAL;
        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", judgeId));

        Score score = scoreRepository
                .findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                        submission.getId(), judgeId, criterion.getId(), scoreType)
                .orElseGet(() -> Score.builder()
                        .submission(submission)
                        .judge(judge)
                        .criterion(criterion)
                        .scoreType(scoreType)
                        .build());

        score.setScoreValue(req.getScoreValue());
        score.setComment(req.getComment());
        score.setIsFinal(false);
        score.setScoredAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());

        Score saved = scoreRepository.save(score);
        auditService.log(AuditAction.SCORE_UPSERT, "scores", saved.getId(),
                Map.of("submissionId", submission.getId(), "criterionId", criterion.getId()));

        ScoreResponse response = toResponse(saved);
        Integer trackId = submission.getTrack() != null ? submission.getTrack().getId() : null;
        eventPublisher.publishEvent(new LiveScoreSavedEvent(this, roundId, trackId, response));
        return response;
    }

    @Override
    public ScoreResponse submitCalibrationScore(SubmitCalibrationScoreRequest req) {
        // TODO: FR-29 calibration — GĐ5
        return ScoreResponse.builder().build();
    }

    private void validateCriterionForSubmission(Submission submission, Criteria criterion) {
        if (submission.getTrack() != null) {
            if (criterion.getTrack() == null
                    || !criterion.getTrack().getId().equals(submission.getTrack().getId())) {
                throw new BusinessRuleException(ErrorCode.CRITERION_WRONG_ROUND,
                        "Tiêu chí không thuộc track của bài nộp");
            }
            return;
        }
        if (criterion.getRound() == null
                || !criterion.getRound().getId().equals(submission.getRound().getId())) {
            throw new BusinessRuleException(ErrorCode.CRITERION_WRONG_ROUND,
                    "Tiêu chí không thuộc round của bài nộp");
        }
    }

    private static ScoreResponse toResponse(Score score) {
        return ScoreResponse.builder()
                .id(score.getId())
                .submissionId(score.getSubmission().getId())
                .judgeId(score.getJudge().getId())
                .criterionId(score.getCriterion().getId())
                .scoreValue(score.getScoreValue())
                .comment(score.getComment())
                .scoreType(score.getScoreType())
                .isFinal(score.getIsFinal())
                .calibrationSessionId(score.getCalibrationSession() != null
                        ? score.getCalibrationSession().getId() : null)
                .scoredAt(score.getScoredAt())
                .updatedAt(score.getUpdatedAt())
                .build();
    }
}
