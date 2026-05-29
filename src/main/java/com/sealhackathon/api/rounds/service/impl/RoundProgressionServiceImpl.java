package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.live_scoring.event.ScoringLockedEvent;
import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ReleaseProblemRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.request.WildcardDecisionRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidateResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundProgressionServiceImpl implements RoundProgressionService {

    private final RoundRepository roundRepository;
    private final RoundMapper roundMapper;
    private final RoundAccessGuard roundAccessGuard;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TrackRepository trackRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final ScoreRepository scoreRepository;
    private final ScoringProgressQueryService scoringProgressQueryService;
    private final RoundRankingQueryService roundRankingQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public RoundSummaryResponse releaseProblem(Integer roundId, ReleaseProblemRequest req) {
        Round round = roundAccessGuard.requireActiveRound(roundId);
        if (round.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát — không thể sửa URL");
        }
        round.setProblemStatementUrl(req.getProblemStatementUrl());
        round.setProblemReleasedAt(LocalDateTime.now());
        Round saved = roundRepository.save(round);
        auditService.log(AuditAction.ROUND_RELEASE_PROBLEM, "rounds", roundId,
                java.util.Map.of("url", req.getProblemStatementUrl()));
        notifyProblemReleased(saved);
        return roundMapper.toSummary(saved, 0, 0, 0f);
    }

    @Override
    public LockScoringResult lockScoring(Integer roundId, LockScoringRequest req) {
        LockScoringRequest body = req != null ? req : LockScoringRequest.builder().build();
        Round round = roundAccessGuard.requireActiveRound(roundId);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Round đã khóa chấm điểm");
        }

        List<Warning> warnings = new ArrayList<>();
        RoundScoringProgressResponse progress = scoringProgressQueryService.progressForRound(round);
        if (progress.getPendingSubmissions() != null && progress.getPendingSubmissions() > 0) {
            warnings.add(Warning.builder()
                    .code(WarningCode.PARTIAL_SCORING_BEFORE_LOCK)
                    .message("Còn bài chưa được chấm điểm")
                    .build());
        }

        if (Boolean.TRUE.equals(body.getForce()) && !StringUtils.hasText(body.getReason())) {
            throw new BusinessRuleException(ErrorCode.FORCE_LOCK_REASON_REQUIRED,
                    "Bắt buộc lý do khi force lock");
        }

        User locker = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        round.setScoringLocked(true);
        round.setScoringLockedAt(LocalDateTime.now());
        round.setScoringLockedBy(locker);
        if (Boolean.TRUE.equals(body.getForce())) {
            round.setForceLocked(true);
            round.setForceLockReason(body.getReason());
        }
        Round saved = roundRepository.save(round);
        finalizeScoresForRound(roundId);

        String auditAction = Boolean.TRUE.equals(body.getForce())
                ? AuditAction.ROUND_FORCE_LOCK
                : AuditAction.ROUND_LOCK;
        auditService.log(auditAction, "rounds", roundId,
                java.util.Map.of("force", Boolean.TRUE.equals(body.getForce())));

        eventPublisher.publishEvent(new ScoringLockedEvent(this, roundId));

        return LockScoringResult.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Override
    public RoundSummaryResponse publish(Integer roundId) {
        // TODO: FR-24 GĐ4
        return RoundSummaryResponse.builder().id(roundId).isPublished(true).build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundScoringProgressResponse scoringProgress(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        return scoringProgressQueryService.progressForRound(round);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundRankingItemResponse> ranking(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Chưa khóa chấm — dùng ranking/preview cho live scoring");
        }
        return roundRankingQueryService.rankingForRound(roundId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundRankingItemResponse> rankingPreview(Integer roundId) {
        roundAccessGuard.requireRound(roundId);
        return roundRankingQueryService.rankingForRound(roundId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiebreakItemResponse> tiebreak(Integer roundId) {
        return List.of();
    }

    @Override
    public List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WildcardCandidateResponse> wildcardCandidates(Integer roundId) {
        return List.of();
    }

    @Override
    public List<WildcardCandidateResponse> wildcardApprove(Integer roundId, WildcardDecisionRequest req) {
        return List.of();
    }

    @Override
    public List<WildcardCandidateResponse> wildcardReject(Integer roundId, WildcardDecisionRequest req) {
        return List.of();
    }

    @Override
    public WildcardReviewResponse decideWildcardReview(Integer reviewId, WildcardReviewDecisionRequest req) {
        return WildcardReviewResponse.builder().id(reviewId).coordinatorApproved(req.getApproved()).build();
    }

    @Override
    public AdvanceTeamsResponse advanceTeams(Integer roundId, AdvanceTeamsRequest req) {
        return AdvanceTeamsResponse.builder()
                .roundId(roundId)
                .advancedTeamIds(req.getAdvancedTeamIds())
                .eliminatedTeamIds(req.getEliminatedTeamIds())
                .build();
    }

    @Override
    public FinalJudgeAssignmentResponse assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req) {
        return FinalJudgeAssignmentResponse.builder()
                .roundId(roundId)
                .judgeIds(req.getJudgeIds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundScoreboardResponse scoreboard(Integer roundId) {
        return RoundScoreboardResponse.builder()
                .roundId(roundId)
                .ranking(List.of())
                .build();
    }

    private void finalizeScoresForRound(Integer roundId) {
        Set<Integer> seen = new HashSet<>();
        List<Score> toUpdate = new ArrayList<>();
        for (Score s : scoreRepository.findBySubmission_Round_Id(roundId)) {
            if (seen.add(s.getId())) {
                s.setIsFinal(true);
                toUpdate.add(s);
            }
        }
        for (Score s : scoreRepository.findBySubmission_Track_Round_Id(roundId)) {
            if (seen.add(s.getId())) {
                s.setIsFinal(true);
                toUpdate.add(s);
            }
        }
        scoreRepository.saveAll(toUpdate);
    }

    private void notifyProblemReleased(Round round) {
        Set<User> recipients = new LinkedHashSet<>();
        for (Track t : trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId())) {
            if (t.getStatus() == TrackStatus.CANCELLED) {
                continue;
            }
            mentorAssignmentRepository.findByTrackId(t.getId()).stream()
                    .map(MentorAssignment::getMentor)
                    .forEach(recipients::add);
            judgeAssignmentRepository.findByTrackId(t.getId()).stream()
                    .map(JudgeAssignment::getJudge)
                    .forEach(recipients::add);
        }
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(recipients),
                "PROBLEM_RELEASED",
                "Đề bài vòng '%s' đã được phát".formatted(round.getName()),
                round.getProblemStatementUrl(),
                "rounds",
                round.getId());
    }
}
