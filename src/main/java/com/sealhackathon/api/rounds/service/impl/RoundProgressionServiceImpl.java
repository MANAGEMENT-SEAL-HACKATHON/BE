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
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService;
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
import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.entity.Team;
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
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final JudgeAssignmentService judgeAssignmentService;

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
        Round round = roundAccessGuard.requireRound(roundId);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Publish chỉ áp dụng round Sơ loại",
                    java.util.Map.of("roundId", roundId));
        }
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Phải khóa chấm điểm trước khi công bố kết quả");
        }
        if (Boolean.TRUE.equals(round.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Kết quả round đã được công bố");
        }

        User publisher = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        round.setIsPublished(true);
        round.setPublishedAt(LocalDateTime.now());
        round.setPublishedBy(publisher);
        Round saved = roundRepository.save(round);

        auditService.log(AuditAction.ROUND_PUBLISH, "rounds", roundId,
                java.util.Map.of("hackathonId", round.getHackathon().getId()));

        return roundMapper.toSummary(saved, 0, 0, 0f);
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
        Round round = requirePreliminaryRoundForProgression(roundId);
        requireScoringLockedAndPublished(round);

        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(round.getHackathon().getId())
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                        "Hackathon chưa có round Chung kết",
                        java.util.Map.of("hackathonId", round.getHackathon().getId())));

        List<Integer> advanced = req.getAdvancedTeamIds();
        List<Integer> eliminated = req.getEliminatedTeamIds() != null ? req.getEliminatedTeamIds() : List.of();
        Set<Integer> overlap = new HashSet<>(advanced);
        overlap.retainAll(eliminated);
        if (!overlap.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đội không thể vừa advance vừa eliminate",
                    java.util.Map.of("teamIds", overlap));
        }

        Hackathon hackathon = round.getHackathon();
        for (Integer teamId : advanced) {
            TeamRoundTrack trt = requireTeamInPreliminaryRound(teamId, roundId);
            trt.setParticipationStatus(ParticipationStatus.ADVANCED);
            teamRoundTrackRepository.save(trt);
            upsertFinalRoundParticipation(trt.getTeam(), finalRound, hackathon);
        }
        for (Integer teamId : eliminated) {
            TeamRoundTrack trt = requireTeamInPreliminaryRound(teamId, roundId);
            trt.setParticipationStatus(ParticipationStatus.ELIMINATED);
            teamRoundTrackRepository.save(trt);
        }

        auditService.log(AuditAction.ROUND_ADVANCE_TEAMS, "rounds", roundId,
                java.util.Map.of(
                        "advancedCount", advanced.size(),
                        "eliminatedCount", eliminated.size(),
                        "finalRoundId", finalRound.getId()));

        return AdvanceTeamsResponse.builder()
                .roundId(roundId)
                .advancedTeamIds(advanced)
                .eliminatedTeamIds(eliminated)
                .build();
    }

    @Override
    public FinalJudgeAssignmentResponse assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                    "Phân Judge Chung kết chỉ cho round FINAL",
                    java.util.Map.of("roundId", roundId));
        }

        List<Warning> warnings = new ArrayList<>();
        List<Integer> assigned = new ArrayList<>();
        for (Integer judgeId : req.getJudgeIds()) {
            JudgeAssignmentService.CreateResult result =
                    judgeAssignmentService.assignFinalRoundG4(roundId, judgeId);
            assigned.add(judgeId);
            if (result.warnings() != null) {
                warnings.addAll(result.warnings());
            }
        }

        long judgeCount = judgeAssignmentRepository.findByRoundId(roundId).size();
        if (judgeCount == 0) {
            warnings.add(Warning.builder()
                    .code(WarningCode.MIN_FINAL_JUDGES_NOT_MET)
                    .message("Round Chung kết chưa có Judge — cần phân công trước activate")
                    .build());
        } else if (judgeCount < 3) {
            warnings.add(Warning.builder()
                    .code(WarningCode.MIN_FINAL_JUDGES_NOT_MET)
                    .message("Panel Chung kết có %d judge — khuyến nghị tối thiểu 3 trước activate"
                            .formatted(judgeCount))
                    .build());
        }

        return FinalJudgeAssignmentResponse.builder()
                .roundId(roundId)
                .judgeIds(assigned)
                .warnings(warnings.isEmpty() ? null : warnings)
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

    private Round requirePreliminaryRoundForProgression(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Thao tác progression chỉ áp dụng round Sơ loại",
                    java.util.Map.of("roundId", roundId));
        }
        return round;
    }

    private void requireScoringLockedAndPublished(Round round) {
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Phải khóa chấm điểm trước khi chốt danh sách thăng vòng");
        }
        if (!Boolean.TRUE.equals(round.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Phải công bố kết quả Sơ loại trước khi advance");
        }
    }

    private TeamRoundTrack requireTeamInPreliminaryRound(Integer teamId, Integer roundId) {
        return teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.TEAM_NOT_IN_ROUND,
                        "Đội #%d không tham gia round Sơ loại #%d".formatted(teamId, roundId),
                        java.util.Map.of("teamId", teamId, "roundId", roundId)));
    }

    private void upsertFinalRoundParticipation(Team team, Round finalRound, Hackathon hackathon) {
        teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .orElseGet(() -> teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                        .team(team)
                        .round(finalRound)
                        .hackathon(hackathon)
                        .build()));
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
