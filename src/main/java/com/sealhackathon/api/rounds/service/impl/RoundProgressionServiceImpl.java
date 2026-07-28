package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.live_scoring.event.ScoringLockedEvent;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.request.UnlockScoringRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceRosterItemResponse;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.AssignFinalJudgesResult;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RankingPreviewResult;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreAuditResponse;
import com.sealhackathon.api.rounds.dto.response.ScoreBreakdownResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidateResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidatesResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.TiebreakRuleOrdering;
import com.sealhackathon.api.rounds.support.WildcardCandidateSelection;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardOverrideHistoryResponse;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import com.sealhackathon.api.wildcard_reviews.entity.WildcardOverrideHistory;
import com.sealhackathon.api.wildcard_reviews.entity.WildcardReview;
import com.sealhackathon.api.wildcard_reviews.repository.WildcardOverrideHistoryRepository;
import com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository;
import com.sealhackathon.api.wildcard_reviews.support.WildcardOverrideCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final com.sealhackathon.api.teams.repository.TeamRepository teamRepository; // Để lấy Entity Team
    private final WildcardReviewRepository wildcardReviewRepository;
    private final WildcardOverrideHistoryRepository wildcardOverrideHistoryRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundProblemStatementStorage problemStatementStorage;
    private final TeamMemberRepository teamMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final RoundPresentationReadiness roundPresentationReadiness;
    private final CriteriaRepository criteriaRepository;
    private final com.sealhackathon.api.announcements.service.AnnouncementService announcementService;
    private final com.sealhackathon.api.live_scoring.PresentationQueuePublisher presentationQueuePublisher;

    private static final int ADVANCE_ROSTER_DEFAULT_PAGE_SIZE = 50;

    @Override
    public RoundSummaryResponse releaseProblem(Integer roundId, MultipartFile file) {
        Round round = roundAccessGuard.requireActiveRound(roundId);
        if (round.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát — không thể thay đổi");
        }
        LocalDateTime now = LocalDateTime.now();
        if (round.getExamAt() == null || round.getExamAt().isAfter(now)) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM,
                    "Chưa tới giờ thi, chưa thể phát đề!");
        }
        // Phát đề: cần Active + đã tới examAt (+ PDF tracks cho prelim).
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId).stream()
                    .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                    .toList();
            if (tracks.isEmpty()) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Chưa có bảng đấu — không thể phát đề Sơ loại");
            }
            List<String> missing = tracks.stream()
                    .filter(t -> !TrackProblemStatementStorage.hasProblemFile(t))
                    .map(Track::getName)
                    .toList();
            if (!missing.isEmpty()) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Các bảng đấu chưa có file đề bài: " + String.join(", ", missing));
            }
            LocalDateTime releasedAt = now;
            round.setProblemReleasedAt(releasedAt);
            for (Track track : tracks) {
                if (track.getProblemReleasedAt() == null) {
                    track.setProblemReleasedAt(releasedAt);
                    trackRepository.save(track);
                }
            }
            Round saved = roundRepository.save(round);
            java.util.Map<String, Object> auditMeta = new java.util.HashMap<>();
            auditMeta.put("isFinal", false);
            auditMeta.put("trackCount", tracks.size());
            auditService.log(AuditAction.ROUND_RELEASE_PROBLEM, "rounds", roundId, auditMeta);
            notifyProblemReleased(saved);
            return roundMapper.toSummary(saved, 0, 0, 0f);
        }

        // Final: mở cửa đề CK; PDF resolve theo track sơ loại của từng đội (không upload PDF round).
        if (file != null && !file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Chung kết sử dụng lại đề sơ loại theo bảng đấu — không upload đề riêng trên vòng CK");
        }
        List<String> missingPrelimPdfs = listMissingPrelimPdfsForAdvancedTeams(round);
        if (!missingPrelimPdfs.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Các bảng đấu sơ loại thiếu đề PDF: " + String.join(", ", missingPrelimPdfs));
        }
        round.setProblemReleasedAt(now);
        Round saved = roundRepository.save(round);
        java.util.Map<String, Object> auditMeta = new java.util.HashMap<>();
        auditMeta.put("isFinal", true);
        auditMeta.put("reusedPrelimTrackPdf", true);
        auditService.log(AuditAction.ROUND_RELEASE_PROBLEM, "rounds", roundId, auditMeta);
        notifyProblemReleased(saved);
        return roundMapper.toSummary(saved, 0, 0, 0f);
    }

    @Override
    @Transactional
    public CloseSubmissionEarlyResponse closeSubmissionEarly(Integer roundId) {
        Round round = roundAccessGuard.requireActiveRoundForUpdate(roundId);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Round đã khóa chấm điểm — không thể kết thúc thời gian thi sớm");
        }
        if (round.getSubmissionClosedEarlyAt() != null) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_ALREADY_CLOSED,
                    "Thời gian thi đã được kết thúc sớm trước đó");
        }

        LocalDateTime now = LocalDateTime.now();

        // Gatekeeper — server clock only (no client now)
        if (round.getProblemReleasedAt() == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_UNRELEASED,
                    "Vòng thi chưa phát đề, không thể kết thúc sớm!");
        }
        if (round.getExamAt() == null || round.getExamAt().isAfter(now)) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM,
                    "Chưa đến giờ thi, không thể kết thúc sớm!");
        }

        boolean deadlineAdjusted = round.getSubmissionDeadline() == null
                || round.getSubmissionDeadline().isAfter(now);
        // examAt already started — do not move start time backwards into the past artificially
        boolean examAtAdjusted = false;

        round.setSubmissionClosedEarlyAt(now);
        if (deadlineAdjusted) {
            // Clamp to past so submit ngay sau close luôn afterDeadline (tránh race isAfter(now)==false)
            round.setSubmissionDeadline(now.minusSeconds(5));
            round.setDeadlineReminderSentAt(null);
        }

        normalizeSubmissionOpenAfterClose(round);

        Round saved = roundRepository.save(round);
        auditService.log(AuditAction.ROUND_CLOSE_SUBMISSION_EARLY, "rounds", roundId, Map.of(
                "examAtAdjusted", examAtAdjusted,
                "deadlineAdjusted", deadlineAdjusted,
                "submissionDeadline", String.valueOf(saved.getSubmissionDeadline()),
                "submissionOpen", String.valueOf(saved.getSubmissionOpen()),
                "examAt", String.valueOf(saved.getExamAt())));

        return CloseSubmissionEarlyResponse.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .examAtAdjusted(examAtAdjusted)
                .deadlineAdjusted(deadlineAdjusted)
                .closedAt(now)
                .build();
    }

    /** Đảm bảo submissionOpen không nằm sau submissionDeadline (TC-GATE-04). */
    private static void normalizeSubmissionOpenAfterClose(Round round) {
        LocalDateTime deadline = round.getSubmissionDeadline();
        if (deadline == null) {
            return;
        }
        LocalDateTime open = round.getSubmissionOpen();
        if (open == null || open.isAfter(deadline)) {
            LocalDateTime examAt = round.getExamAt();
            if (examAt != null && !examAt.isAfter(deadline)) {
                round.setSubmissionOpen(examAt);
            } else {
                round.setSubmissionOpen(deadline);
            }
        }
    }

    @Override
    @Transactional
    public LockScoringResult lockScoring(Integer roundId, LockScoringRequest req) {
        LockScoringRequest body = req != null ? req : LockScoringRequest.builder().build();
        Round round = roundAccessGuard.requireActiveRoundForUpdate(roundId);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Round đã khóa chấm điểm");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean closedEarly = round.getSubmissionClosedEarlyAt() != null;
        boolean pastDeadline = round.getSubmissionDeadline() != null
                && now.isAfter(round.getSubmissionDeadline());
        if (!closedEarly && !pastDeadline) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_NOT_CLOSED,
                    "Chưa đóng vòng thi (chưa hết giờ hoặc chưa kết thúc sớm), không thể khóa chấm!");
        }

        // Gate 2–3: force MUST NOT bypass
        roundPresentationReadiness.assertShuffled(round);
        roundPresentationReadiness.assertPresentationsComplete(round);

        List<Warning> warnings = new ArrayList<>();
        RoundScoringProgressResponse progress = scoringProgressQueryService.progressForRound(round);
        int pending = progress.getPendingSubmissions() != null ? progress.getPendingSubmissions() : 0;
        if (pending > 0) {
            if (!Boolean.TRUE.equals(body.getForce())) {
                throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_SCORING_INCOMPLETE,
                        "Còn bài chưa được chấm điểm, không thể khóa chấm!");
            }
            if (!StringUtils.hasText(body.getReason())) {
                throw new BusinessRuleException(ErrorCode.FORCE_LOCK_REASON_REQUIRED,
                        "Bắt buộc lý do khi force lock");
            }
            warnings.add(Warning.builder()
                    .code(WarningCode.PARTIAL_SCORING_BEFORE_LOCK)
                    .message("Còn bài chưa được chấm điểm")
                    .build());
        } else if (Boolean.TRUE.equals(body.getForce()) && !StringUtils.hasText(body.getReason())) {
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

        if (Boolean.TRUE.equals(saved.getIsFinal())) {
            transitionHackathonToPendingConfirm(saved);
        }

        autoApplyResolvableTiebreaks(roundId);

        return LockScoringResult.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Override
    @Transactional
    public RoundSummaryResponse unlockScoring(Integer roundId, UnlockScoringRequest req) {
        UnlockScoringRequest body = req != null ? req : UnlockScoringRequest.builder().build();
        if (!StringUtils.hasText(body.getReason())) {
            throw new BusinessRuleException(ErrorCode.UNLOCK_REASON_REQUIRED,
                    "Bắt buộc lý do khi mở khóa chấm");
        }
        Round round = roundAccessGuard.requireActiveRoundForUpdate(roundId);
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Round chưa khóa chấm");
        }
        round.setScoringLocked(false);
        round.setScoringLockedAt(null);
        round.setScoringLockedBy(null);
        round.setForceLocked(false);
        round.setForceLockReason(null);
        Round saved = roundRepository.save(round);
        auditService.log(AuditAction.ROUND_SCORING_UNLOCKED, "rounds", roundId,
                Map.of("reason", body.getReason()));
        presentationQueuePublisher.publishScoringUnlocked(roundId, null, body.getReason());
        return roundMapper.toSummary(saved, 0, 0, 0f);
    }

    /** FR-30A — lock round Chung kết ⇒ hackathon ONGOING → PENDING_CONFIRM. */
    private void transitionHackathonToPendingConfirm(Round finalRound) {
        Hackathon hackathon = finalRound.getHackathon();
        if (hackathon == null) {
            return;
        }
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return;
        }
        HackathonStatus from = hackathon.getStatus();
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);
        auditService.log(AuditAction.HACKATHON_STATUS_CHANGE, "hackathons", hackathon.getId(),
                Map.of(
                        "from", from.name(),
                        "to", HackathonStatus.PENDING_CONFIRM.name(),
                        "trigger", "FINAL_ROUND_LOCK",
                        "roundId", finalRound.getId()));
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

        try {
            announcementService.publishResults(
                    round.getHackathon().getId(),
                    roundId,
                    "Kết quả sơ loại đã công bố",
                    "Kết quả vòng «" + round.getName() + "» đã được công bố. Xem bảng xếp hạng.");
        } catch (Exception ignored) {
            // durable flag already set; announcement is best-effort
        }

        try {
            notifyResultsPublished(round);
        } catch (Exception ignored) {
            // in-app notify is best-effort after durable publish flag
        }

        return roundMapper.toSummary(saved, 0, 0, 0f);
    }

    /** Fan-out in-app notify to accepted team members of the published round. */
    private void notifyResultsPublished(Round round) {
        Set<User> recipients = new LinkedHashSet<>();
        for (TeamRoundTrack trt : teamRoundTrackRepository.findByTrack_Round_Id(round.getId())) {
            teamMemberRepository.findByTeam_Id(trt.getTeam().getId()).stream()
                    .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                    .map(TeamMember::getUser)
                    .forEach(recipients::add);
        }
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(recipients),
                "SCORE_RELEASED",
                "Kết quả đã công bố — %s".formatted(round.getName()),
                "Kết quả vòng «%s» đã được công bố. Xem bảng xếp hạng và điểm đội."
                        .formatted(round.getName()),
                "rounds",
                round.getId());
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
        return rankingPreviewResult(roundId).getItems();
    }

    @Override
    @Transactional(readOnly = true)
    public RankingPreviewResult rankingPreviewResult(Integer roundId) {
        roundAccessGuard.requireRound(roundId);
        List<RoundRankingItemResponse> items = roundRankingQueryService.rankingForRound(roundId, true);
        List<Warning> warnings = List.of();
        if (roundRankingQueryService.hasIncompleteScoring(roundId, true)) {
            warnings = List.of(Warning.of(
                    WarningCode.INCOMPLETE_SCORING_IN_RANKING,
                    "Một số tiêu chí chưa được chấm — điểm preview có thể thiếu (COALESCE=0)"));
        }
        return RankingPreviewResult.builder().items(items).warnings(warnings).build();
    }

    // =========================================================================
    // NHIỆM VỤ 1.1: TÌM KIẾM CÁC ĐỘI ĐỒNG ĐIỂM TẠI RANH GIỚI CUT-OFF
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<TiebreakItemResponse> tiebreak(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        return enrichTiebreakItems(round, detectRawTiebreakItems(round, roundId));
    }

    private List<TiebreakItemResponse> detectRawTiebreakItems(Round round, Integer roundId) {
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return detectTiebreakForFinalRound(round);
        }

        Integer topNAdvance = round.getTopNAdvance();
        if (topNAdvance == null || topNAdvance <= 0) {
            return List.of();
        }

        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(roundId, false);
        if (ranking.isEmpty()) {
            return List.of();
        }

        boolean displayNetsPenalty = rankingDisplayNetsPenalty(round);

        Map<String, List<RoundRankingItemResponse>> partitionedRanking = ranking.stream()
                .collect(Collectors.groupingBy(item -> {
                    String trackPart = item.getTrackId() != null ? item.getTrackId().toString() : "0";
                    String groupPart = item.getAssignedGroup() != null ? item.getAssignedGroup() : "DEFAULT";
                    return trackPart + "_" + groupPart;
                }));

        List<TiebreakItemResponse> tiebreakItems = new ArrayList<>();

        for (Map.Entry<String, List<RoundRankingItemResponse>> entry : partitionedRanking.entrySet()) {
            // Sort by effective score so cutoff index matches progression order after micro-penalty resolve.
            List<RoundRankingItemResponse> groupRanking = entry.getValue().stream()
                    .sorted(Comparator
                            .comparing((RoundRankingItemResponse r) -> effectiveScoreForTieDetection(r, displayNetsPenalty),
                                    Comparator.reverseOrder())
                            .thenComparing(r -> r.getPenaltyScore() != null ? r.getPenaltyScore() : 0.0)
                            .thenComparing(RoundRankingItemResponse::getTeamId))
                    .toList();
            if (groupRanking.size() <= topNAdvance) {
                continue;
            }

            double cutoffScore = effectiveScoreForTieDetection(groupRanking.get(topNAdvance - 1), displayNetsPenalty);
            long safeCount = groupRanking.stream()
                    .filter(r -> effectiveScoreForTieDetection(r, displayNetsPenalty) > cutoffScore + 1e-9)
                    .count();

            List<Integer> borderlineTeamIds = groupRanking.stream()
                    .filter(r -> sameEffectiveScore(
                            effectiveScoreForTieDetection(r, displayNetsPenalty), cutoffScore))
                    .map(RoundRankingItemResponse::getTeamId)
                    .toList();

            long remainingSlots = topNAdvance - safeCount;

            if (borderlineTeamIds.size() > remainingSlots) {
                tiebreakItems.add(TiebreakItemResponse.builder()
                        .partitionKey(entry.getKey())
                        .cutoffRank(topNAdvance)
                        .candidateTeamIds(borderlineTeamIds)
                        .build());
            }
        }
        return tiebreakItems;
    }

    private List<TiebreakItemResponse> detectTiebreakForFinalRound(Round round) {
        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(round.getId(), false);
        if (ranking.isEmpty()) {
            return List.of();
        }

        boolean displayNetsPenalty = rankingDisplayNetsPenalty(round);
        List<RoundRankingItemResponse> ordered = ranking.stream()
                .sorted(Comparator
                        .comparing((RoundRankingItemResponse r) -> effectiveScoreForTieDetection(r, displayNetsPenalty),
                                Comparator.reverseOrder())
                        .thenComparing(r -> r.getPenaltyScore() != null ? r.getPenaltyScore() : 0.0)
                        .thenComparing(RoundRankingItemResponse::getTeamId))
                .toList();

        List<TiebreakItemResponse> tiebreakItems = new ArrayList<>();
        int i = 0;
        while (i < ordered.size()) {
            double score = effectiveScoreForTieDetection(ordered.get(i), displayNetsPenalty);
            int j = i + 1;
            while (j < ordered.size()
                    && sameEffectiveScore(effectiveScoreForTieDetection(ordered.get(j), displayNetsPenalty), score)) {
                j++;
            }
            if (j - i > 1) {
                List<Integer> tiedTeamIds = ordered.subList(i, j).stream()
                        .map(RoundRankingItemResponse::getTeamId)
                        .toList();
                tiebreakItems.add(TiebreakItemResponse.builder()
                        .partitionKey("FINAL")
                        .cutoffRank(ordered.get(i).getRank())
                        .candidateTeamIds(tiedTeamIds)
                        .build());
            }
            i = j;
        }
        return tiebreakItems;
    }

    /**
     * Điểm hiệu lực để phát hiện đồng điểm: trừ micro-penalty khi {@code totalScore} chưa nhúng penalty
     * (ONGOING / PENDING_CONFIRM). Khi hackathon FINISHED, ranking đã trừ penalty vào totalScore —
     * không trừ lần hai.
     */
    static double effectiveScoreForTieDetection(RoundRankingItemResponse item, boolean displayNetsPenalty) {
        double total = item.getTotalScore() != null ? item.getTotalScore() : 0.0;
        if (displayNetsPenalty) {
            return total;
        }
        double penalty = item.getPenaltyScore() != null ? item.getPenaltyScore() : 0.0;
        return total - penalty;
    }

    private static boolean rankingDisplayNetsPenalty(Round round) {
        return round != null
                && round.getHackathon() != null
                && round.getHackathon().getStatus()
                == com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED;
    }

    private static boolean sameEffectiveScore(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    private List<TiebreakItemResponse> enrichTiebreakItems(Round round, List<TiebreakItemResponse> rawItems) {
        if (rawItems.isEmpty()) {
            return List.of();
        }
        TiebreakRule rule = round.getTiebreakRule() != null ? round.getTiebreakRule() : TiebreakRule.COORDINATOR_DECISION;
        Map<Integer, Submission> submissionByTeam = loadSubmissionsByTeam(round.getId());
        Map<Integer, Double> penaltyByTeam = tiebreakEvaluationRepository.findByRound_Id(round.getId()).stream()
                .collect(Collectors.groupingBy(
                        te -> te.getTeam().getId(),
                        Collectors.summingDouble(TiebreakEvaluation::getPenaltyScore)));
        Map<Integer, Double> totalScoreByTeam = roundRankingQueryService.rankingForRound(round.getId(), false).stream()
                .collect(Collectors.toMap(
                        RoundRankingItemResponse::getTeamId,
                        RoundRankingItemResponse::getTotalScore,
                        (a, b) -> a));

        List<TiebreakItemResponse> enriched = new ArrayList<>();
        for (TiebreakItemResponse item : rawItems) {
            List<TiebreakRuleOrdering.TiebreakCandidate> candidates = item.getCandidateTeamIds().stream()
                    .map(teamId -> toTiebreakCandidate(
                            teamId,
                            submissionByTeam.get(teamId),
                            penaltyByTeam.getOrDefault(teamId, 0.0),
                            totalScoreByTeam.get(teamId)))
                    .toList();

            Optional<List<Integer>> suggestedOrder = TiebreakRuleOrdering.orderByRule(rule, candidates);
            boolean requiresManual = rule == TiebreakRule.COORDINATOR_DECISION || suggestedOrder.isEmpty();
            String reason = null;
            if (rule == TiebreakRule.COORDINATOR_DECISION) {
                reason = "COORDINATOR_DECISION";
            } else if (suggestedOrder.isEmpty()) {
                reason = "DEEP_TIE";
            }

            enriched.add(TiebreakItemResponse.builder()
                    .partitionKey(item.getPartitionKey())
                    .cutoffRank(item.getCutoffRank())
                    .candidateTeamIds(item.getCandidateTeamIds())
                    .tiebreakRule(rule)
                    .reason(reason)
                    .requiresManualReorder(requiresManual)
                    .suggestedOrderedTeamIds(suggestedOrder.orElse(null))
                    .build());
        }
        return enriched;
    }

    private Map<Integer, Submission> loadSubmissionsByTeam(Integer roundId) {
        Map<Integer, Submission> byTeam = new HashMap<>();
        Map<Integer, Submission> byId = new HashMap<>();
        for (Submission submission : submissionRepository.findByRound_Id(roundId)) {
            byId.put(submission.getId(), submission);
        }
        for (Submission submission : submissionRepository.findByTrack_Round_Id(roundId)) {
            byId.putIfAbsent(submission.getId(), submission);
        }
        for (Submission submission : byId.values()) {
            byTeam.putIfAbsent(submission.getTeam().getId(), submission);
        }
        return byTeam;
    }

    private static TiebreakRuleOrdering.TiebreakCandidate toTiebreakCandidate(
            Integer teamId,
            Submission submission,
            double penaltyScore,
            Double totalScore) {
        return new TiebreakRuleOrdering.TiebreakCandidate(
                teamId,
                submission != null ? submission.getStatus() : null,
                submission != null ? submission.getSubmittedAt() : null,
                penaltyScore,
                totalScore);
    }

    private void autoApplyResolvableTiebreaks(Integer roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null || !Boolean.TRUE.equals(round.getScoringLocked())) {
            return;
        }

        User actor = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        if (actor == null) {
            return;
        }

        List<TiebreakItemResponse> items = enrichTiebreakItems(round, detectRawTiebreakItems(round, roundId));
        for (TiebreakItemResponse item : items) {
            if (Boolean.TRUE.equals(item.getRequiresManualReorder())) {
                continue;
            }
            List<Integer> orderedIds = item.getSuggestedOrderedTeamIds();
            if (orderedIds == null || orderedIds.isEmpty()) {
                continue;
            }
            applyTiebreakOrder(round, orderedIds, actor,
                    "Auto-resolved by " + item.getTiebreakRule().name());
        }
    }

    // =========================================================================
    // NHIỆM VỤ 1.2: CẬP NHẬT KẾT QUẢ PHÂN XỬ ĐỒNG ĐIỂM (COORDINATOR DECISION)
    // =========================================================================
    @Override
    public List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req) {
        roundAccessGuard.requireRound(roundId);
        Round round = roundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED, "Phải khóa chấm điểm trước khi giải quyết Tiebreak");
        }

        User coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        List<Integer> orderedIds = req.getOrderedTeamIds();

        java.util.Set<Integer> orderedSet = new java.util.HashSet<>(orderedIds);
        if (orderedSet.size() != orderedIds.size()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "orderedTeamIds không được trùng lặp",
                    java.util.Map.of("orderedTeamIds", orderedIds));
        }
        boolean matchesTiedGroup = tiebreak(roundId).stream()
                .anyMatch(item -> orderedSet.equals(new java.util.HashSet<>(item.getCandidateTeamIds())));
        if (!matchesTiedGroup) {
            // Có thể race: coordinator khác vừa resolve nhóm này
            boolean alreadyResolved = orderedIds.stream().anyMatch(teamId ->
                    tiebreakEvaluationRepository.findByRound_IdAndTeam_Id(roundId, teamId).stream()
                            .anyMatch(te -> Boolean.TRUE.equals(te.getIsCastingVote())));
            if (alreadyResolved) {
                throw new ConflictException(ErrorCode.TIEBREAK_ALREADY_RESOLVED,
                        "Nhóm đồng điểm này đã được resolve bởi coordinator khác",
                        java.util.Map.of("orderedTeamIds", orderedIds, "roundId", roundId));
            }
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "orderedTeamIds phải khớp nhóm đội đang hòa điểm cần tiebreak",
                    java.util.Map.of("orderedTeamIds", orderedIds));
        }

        // Race guard: nếu nhóm đã có casting-vote từ resolve trước → 409
        boolean alreadyHasCastingVote = orderedIds.stream().anyMatch(teamId ->
                tiebreakEvaluationRepository.findByRound_IdAndTeam_Id(roundId, teamId).stream()
                        .anyMatch(te -> Boolean.TRUE.equals(te.getIsCastingVote())));
        if (alreadyHasCastingVote) {
            throw new ConflictException(ErrorCode.TIEBREAK_ALREADY_RESOLVED,
                    "Nhóm đồng điểm này đã được resolve — vui lòng tải lại bảng xếp hạng",
                    java.util.Map.of("orderedTeamIds", orderedIds, "roundId", roundId));
        }

        applyTiebreakOrder(round, orderedIds, coordinator, req.getNote());

        return roundRankingQueryService.rankingForRound(roundId, false);
    }

    private void applyTiebreakOrder(Round round, List<Integer> orderedIds, User judge, String note) {
        Integer roundId = round.getId();
        float penaltyIncrement = 0.01f;
        float currentPenalty = 0.0f;

        List<TiebreakEvaluation> evaluationsToSave = new ArrayList<>();

        for (Integer teamId : orderedIds) {
            Team team = teamRepository.findById(teamId).orElseThrow();

            tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(roundId, teamId, judge.getId())
                    .ifPresent(tiebreakEvaluationRepository::delete);

            if (currentPenalty > 0) {
                evaluationsToSave.add(TiebreakEvaluation.builder()
                        .round(round)
                        .team(team)
                        .judge(judge)
                        .penaltyScore(currentPenalty)
                        .isCastingVote(true)
                        .tiebreakLevel(2)
                        .notes(note)
                        .evaluatedAt(LocalDateTime.now())
                        .build());
            }
            currentPenalty += penaltyIncrement;
        }

        if (!evaluationsToSave.isEmpty()) {
            tiebreakEvaluationRepository.saveAll(evaluationsToSave);
        }
        // Audit mọi resolve (kể cả khi chỉ 1 đội đứng đầu không cần penalty row)
        auditService.log(AuditAction.ROUND_TIEBREAK_RESOLVED, "tiebreak_evaluations", roundId,
                java.util.Map.of(
                        "orderedTeamIds", orderedIds,
                        "note", note != null ? note : "",
                        "actorId", judge.getId()));
    }

    // =========================================================================
    // NHIỆM VỤ 2.1: TỰ ĐỘNG QUÉT VÀ ĐỀ XUẤT VÉ VỚT (WILDCARD CANDIDATES) — Plan C
    // =========================================================================
    @Override
    public WildcardCandidatesResponse wildcardCandidates(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        Hackathon hackathon = round.getHackathon();
        boolean hackathonEnabled = Boolean.TRUE.equals(hackathon.getWildcardEnabled());
        // Product decision: Wildcard removed — always empty pool (Top-N only). WC-MIG.
        return emptyWildcardResponse(hackathonEnabled, false);
    }

    private WildcardCandidatesResponse lockedWildcardResponse(Round round, boolean hackathonEnabled) {
        List<WildcardReview> reviews = wildcardReviewRepository.findByRound_Id(round.getId());
        reviews.sort(Comparator
                .comparing(WildcardReview::getProposalRank, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WildcardReview::getAvgScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WildcardReview::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.getTeam().getId(), Comparator.nullsLast(Comparator.naturalOrder())));

        int slots = (int) reviews.stream().filter(r -> Boolean.TRUE.equals(r.getSystemProposed())).count();
        if (slots <= 0) {
            slots = (int) reviews.stream().filter(r -> Boolean.TRUE.equals(r.getCoordinatorApproved())).count();
        }

        List<WildcardCandidateResponse> responses = new ArrayList<>();
        for (WildcardReview review : reviews) {
            responses.add(toCandidateResponse(review));
        }

        int approvedCount = (int) reviews.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCoordinatorApproved()))
                .count();

        return WildcardCandidatesResponse.builder()
                .hackathonWildcardEnabled(hackathonEnabled)
                .roundWildcardEnabled(true)
                .availableSlots(slots)
                .autoAdvancedCount(0)
                .approvedCount(approvedCount)
                .decisionsFinalized(true)
                .proposalConfirmedAt(round.getWildcardProposalConfirmedAt())
                .candidates(responses)
                .build();
    }

    private static WildcardCandidatesResponse emptyWildcardResponse(
            boolean hackathonEnabled, boolean roundEnabled) {
        return WildcardCandidatesResponse.builder()
                .hackathonWildcardEnabled(hackathonEnabled)
                .roundWildcardEnabled(roundEnabled)
                .availableSlots(0)
                .autoAdvancedCount(0)
                .approvedCount(0)
                .decisionsFinalized(false)
                .proposalConfirmedAt(null)
                .candidates(List.of())
                .build();
    }

    private Optional<WildcardPoolSnapshot> resolveWildcardPool(Round round) {
        Integer minTeamsFinal = round.getMinTeamsFinal();
        Integer topNAdvance = round.getTopNAdvance();
        if (minTeamsFinal == null || topNAdvance == null || topNAdvance <= 0) {
            return Optional.empty();
        }

        // slots theory = minTeamsFinal − (topN × actual_track_count); ≤0 → no WC pool
        long trackCount = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .count();
        int theoreticalSlots = (int) (minTeamsFinal - (topNAdvance * trackCount));
        if (theoreticalSlots <= 0) {
            return Optional.empty();
        }

        List<RoundRankingItemResponse> ranking =
                roundRankingQueryService.rankingForRound(round.getId(), false);
        if (ranking.isEmpty()) {
            return Optional.empty();
        }

        List<RoundRankingItemResponse> topNTeams = new ArrayList<>();
        List<RoundRankingItemResponse> remainingTeams = new ArrayList<>();

        for (RoundRankingItemResponse item : ranking) {
            if (item.getParticipationStatus() != null
                    && ParticipationStatus.ELIMINATED.name().equals(item.getParticipationStatus())) {
                continue;
            }
            if (item.getRank() != null && item.getRank() <= topNAdvance) {
                topNTeams.add(item);
            } else {
                remainingTeams.add(item);
            }
        }

        int actualSlots = minTeamsFinal - topNTeams.size();
        int slots = Math.min(theoreticalSlots, actualSlots);
        if (slots <= 0 || remainingTeams.isEmpty()) {
            return Optional.empty();
        }

        // Plan C: propose exactly `slots` (avgScore DESC, submittedAt ASC, teamId ASC)
        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectExactSlots(remainingTeams, slots);
        return Optional.of(new WildcardPoolSnapshot(slots, topNTeams.size(), selected));
    }

    private List<WildcardCandidateResponse> buildWildcardCandidateResponses(
            Round round, WildcardPoolSnapshot snapshot) {
        List<WildcardCandidateResponse> responses = new ArrayList<>();
        int candidateRank = 1;
        int slots = snapshot.availableSlots();

        for (RoundRankingItemResponse candidate : snapshot.selectedCandidates()) {
            Team team = teamRepository.findById(candidate.getTeamId()).orElseThrow();
            Track track = candidate.getTrackId() != null
                    ? trackRepository.findById(candidate.getTrackId()).orElse(null)
                    : null;

            WildcardReview review = wildcardReviewRepository
                    .findByRound_IdAndTeam_Id(round.getId(), team.getId())
                    .orElseGet(() -> WildcardReview.builder()
                            .round(round)
                            .team(team)
                            .track(track)
                            .build());

            review.setTrack(track);
            review.setAvgScore(candidate.getTotalScore() != null
                    ? candidate.getTotalScore().floatValue()
                    : 0f);
            review.setSubmittedAt(candidate.getSubmittedAt());
            review.setProposalRank(candidateRank);
            review.setSystemProposed(true);

            if (review.getId() == null) {
                review = wildcardReviewRepository.save(review);
            } else if (round.getWildcardProposalConfirmedAt() == null
                    && review.getCoordinatorApproved() == null) {
                review = wildcardReviewRepository.save(review);
            }

            responses.add(WildcardCandidateResponse.builder()
                    .reviewId(review.getId())
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .assignedGroup(candidate.getAssignedGroup())
                    .candidateRank(candidateRank++)
                    .totalScore(candidate.getTotalScore())
                    .submittedAt(review.getSubmittedAt())
                    .systemProposed(true)
                    .reason("Hệ thống đề xuất: Top " + slots + " điểm cao nhất ngoài Top "
                            + round.getTopNAdvance() + " mỗi bảng (điểm ↓, nộp sớm ↑, teamId)")
                    .coordinatorApproved(review.getCoordinatorApproved())
                    .coordinatorNote(review.getCoordinatorNote())
                    .isOverride(Boolean.TRUE.equals(review.getIsOverride()))
                    .overrideReasonCategory(review.getOverrideReasonCategory())
                    .build());
        }
        return responses;
    }

    private WildcardCandidateResponse toCandidateResponse(WildcardReview review) {
        return WildcardCandidateResponse.builder()
                .reviewId(review.getId())
                .teamId(review.getTeam().getId())
                .teamName(review.getTeam().getTeamName())
                .assignedGroup(null)
                .candidateRank(review.getProposalRank())
                .totalScore(review.getAvgScore() != null ? review.getAvgScore().doubleValue() : null)
                .submittedAt(review.getSubmittedAt())
                .systemProposed(Boolean.TRUE.equals(review.getSystemProposed()))
                .reason(Boolean.TRUE.equals(review.getIsOverride())
                        ? "Đã Override (" + review.getOverrideReasonCategory() + ")"
                        : "Đề xuất đã xác nhận")
                .coordinatorApproved(review.getCoordinatorApproved())
                .coordinatorNote(review.getCoordinatorNote())
                .isOverride(Boolean.TRUE.equals(review.getIsOverride()))
                .overrideReasonCategory(review.getOverrideReasonCategory())
                .build();
    }

    // =========================================================================
    // Plan C: XÁC NHẬN ĐỀ XUẤT → LOCK
    // =========================================================================
    @Override
    public WildcardCandidatesResponse confirmWildcardProposal(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);

        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Phải khóa chấm điểm trước khi xác nhận đề xuất vé vớt");
        }

        if (round.getWildcardProposalConfirmedAt() != null) {
            throw new BusinessRuleException(ErrorCode.WILDCARD_PROPOSAL_ALREADY_CONFIRMED,
                    "Đề xuất vé vớt đã được xác nhận — không thể xác nhận lại. Sửa qua Override.");
        }

        if (!Boolean.TRUE.equals(round.getWildcardEnabled())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Vé vớt chưa được bật trên vòng này");
        }

        WildcardPoolSnapshot pool = resolveWildcardPool(round)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Không có pool vé vớt đang mở cho vòng này"));

        User coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        Set<Integer> proposedTeamIds = pool.selectedCandidates().stream()
                .map(RoundRankingItemResponse::getTeamId)
                .collect(Collectors.toSet());

        // Ensure review rows + mark proposed / approve top slots
        int rank = 1;
        for (RoundRankingItemResponse candidate : pool.selectedCandidates()) {
            Team team = teamRepository.findById(candidate.getTeamId()).orElseThrow();
            Track track = candidate.getTrackId() != null
                    ? trackRepository.findById(candidate.getTrackId()).orElse(null)
                    : null;

            WildcardReview review = wildcardReviewRepository
                    .findByRound_IdAndTeam_Id(round.getId(), team.getId())
                    .orElseGet(() -> WildcardReview.builder()
                            .round(round)
                            .team(team)
                            .build());

            review.setTrack(track);
            review.setAvgScore(candidate.getTotalScore() != null
                    ? candidate.getTotalScore().floatValue()
                    : 0f);
            review.setSubmittedAt(candidate.getSubmittedAt());
            review.setProposalRank(rank++);
            review.setSystemProposed(true);
            review.setCoordinatorApproved(true);
            review.setCoordinatorNote("Xác nhận đề xuất hệ thống");
            review.setReviewedBy(coordinator);
            review.setReviewedAt(now);
            review.setIsOverride(false);
            review.setOverrideReasonCategory(null);
            review.setOverrideNote(null);
            wildcardReviewRepository.save(review);
        }

        // Reject any other pending reviews for this round not in the proposal
        for (WildcardReview existing : wildcardReviewRepository.findByRound_Id(round.getId())) {
            if (!proposedTeamIds.contains(existing.getTeam().getId())) {
                existing.setSystemProposed(false);
                existing.setCoordinatorApproved(false);
                existing.setCoordinatorNote("Tự động từ chối — ngoài đề xuất Top " + pool.availableSlots());
                existing.setReviewedBy(coordinator);
                existing.setReviewedAt(now);
                wildcardReviewRepository.save(existing);
            }
        }

        round.setWildcardProposalConfirmedAt(now);
        roundRepository.save(round);

        auditService.log(AuditAction.WILDCARD_PROPOSAL_CONFIRMED, "rounds", roundId,
                java.util.Map.of(
                        "slots", pool.availableSlots(),
                        "proposedTeamIds", proposedTeamIds,
                        "confirmedAt", now.toString()));

        return lockedWildcardResponse(round, Boolean.TRUE.equals(round.getHackathon().getWildcardEnabled()));
    }

    // =========================================================================
    // Plan C: OVERRIDE sau khi LOCKED
    // =========================================================================
    @Override
    public WildcardReviewResponse overrideWildcardReview(Integer reviewId, WildcardOverrideRequest req) {
        WildcardReview review = wildcardReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("WildcardReview", reviewId));
        Round round = review.getRound();

        if (round.getWildcardProposalConfirmedAt() == null) {
            throw new BusinessRuleException(ErrorCode.WILDCARD_PROPOSAL_NOT_LOCKED,
                    "Chỉ Override sau khi đã xác nhận đề xuất vé vớt");
        }

        String category = WildcardOverrideCategory.normalize(req.getCategory());
        if (!WildcardOverrideCategory.isValid(category)) {
            throw new BusinessRuleException(ErrorCode.WILDCARD_OVERRIDE_CATEGORY_INVALID,
                    "Category override không hợp lệ. Dùng: PROPOSED_TEAM_VIOLATION, TRACK_QUOTA_ADJUST, SCORE_CORRECTED, OTHER");
        }

        String note = req.getNote() != null ? req.getNote().trim() : "";
        if (WildcardOverrideCategory.requiresNote(category) && !StringUtils.hasText(note)) {
            throw new BusinessRuleException(ErrorCode.WILDCARD_OVERRIDE_NOTE_REQUIRED,
                    "Category OTHER bắt buộc nhập ghi chú (WC-05)");
        }

        Boolean before = review.getCoordinatorApproved();
        Boolean after = req.getApproved();
        User coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        review.setCoordinatorApproved(after);
        review.setCoordinatorNote(StringUtils.hasText(note) ? note : review.getCoordinatorNote());
        review.setOverrideReasonCategory(category);
        review.setOverrideNote(StringUtils.hasText(note) ? note : null);
        review.setIsOverride(true);
        review.setReviewedBy(coordinator);
        review.setReviewedAt(now);
        WildcardReview saved = wildcardReviewRepository.save(review);

        WildcardOverrideHistory history = WildcardOverrideHistory.builder()
                .round(round)
                .review(saved)
                .team(saved.getTeam())
                .category(category)
                .note(StringUtils.hasText(note) ? note : null)
                .beforeApproved(before)
                .afterApproved(after)
                .byUser(coordinator)
                .overriddenAt(now)
                .build();
        wildcardOverrideHistoryRepository.save(history);

        auditService.log(AuditAction.WILDCARD_OVERRIDE, "wildcard_reviews", saved.getId(),
                java.util.Map.of(
                        "category", category,
                        "beforeApproved", before,
                        "afterApproved", after,
                        "teamId", saved.getTeam().getId()));

        return WildcardReviewResponse.builder()
                .id(saved.getId())
                .roundId(saved.getRound().getId())
                .teamId(saved.getTeam().getId())
                .avgScore(saved.getAvgScore())
                .coordinatorApproved(saved.getCoordinatorApproved())
                .coordinatorNote(saved.getCoordinatorNote())
                .reviewedAt(saved.getReviewedAt())
                .build();
    }

    @Override
    public List<WildcardOverrideHistoryResponse> listWildcardOverrides(Integer roundId) {
        roundAccessGuard.requireRound(roundId);
        return wildcardOverrideHistoryRepository.findByRound_IdOrderByOverriddenAtDesc(roundId).stream()
                .map(h -> WildcardOverrideHistoryResponse.builder()
                        .id(h.getId())
                        .roundId(h.getRound().getId())
                        .reviewId(h.getReview().getId())
                        .teamId(h.getTeam().getId())
                        .teamName(h.getTeam().getTeamName())
                        .category(h.getCategory())
                        .note(h.getNote())
                        .beforeApproved(h.getBeforeApproved())
                        .afterApproved(h.getAfterApproved())
                        .byUserId(h.getByUser() != null ? h.getByUser().getId() : null)
                        .byUserName(h.getByUser() != null ? h.getByUser().getFullName() : null)
                        .overriddenAt(h.getOverriddenAt())
                        .build())
                .toList();
    }

    // =========================================================================
    // NHIỆM VỤ 2.2: LƯU QUYẾT ĐỊNH CỦA BAN TỔ CHỨC (DUYỆT VÉ VỚT) — legacy soft-disabled after lock
    // =========================================================================
    @Override
    public WildcardReviewResponse decideWildcardReview(Integer reviewId, WildcardReviewDecisionRequest req) {
        WildcardReview review = wildcardReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("WildcardReview", reviewId));

        if (review.getRound().getWildcardProposalConfirmedAt() != null) {
            throw new BusinessRuleException(ErrorCode.WILDCARD_PROPOSAL_ALREADY_CONFIRMED,
                    "Đề xuất đã khóa — dùng Override để sửa quyết định");
        }

        if (!Boolean.TRUE.equals(review.getRound().getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Phải khóa chấm điểm trước khi xét duyệt vé vớt");
        }

        if (review.getCoordinatorApproved() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Quyết định vé vớt đã chốt — không thể thay đổi");
        }

        Round round = review.getRound();
        WildcardPoolSnapshot pool = resolveWildcardPool(round)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Không có pool vé vớt đang mở cho vòng này"));

        boolean teamInPool = pool.selectedCandidates().stream()
                .anyMatch(c -> Objects.equals(c.getTeamId(), review.getTeam().getId()));
        if (!teamInPool) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đội không thuộc danh sách vé vớt hiện tại");
        }

        if (Boolean.TRUE.equals(req.getApproved())) {
            long approvedCount = wildcardReviewRepository.countByRound_IdAndCoordinatorApproved(
                    round.getId(), true);
            if (approvedCount >= pool.availableSlots()) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Đã đủ " + pool.availableSlots() + " suất vé vớt được duyệt");
            }
        }

        User coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        review.setCoordinatorApproved(req.getApproved());
        review.setCoordinatorNote(req.getCoordinatorNote());
        review.setReviewedBy(coordinator);
        review.setReviewedAt(now);
        WildcardReview saved = wildcardReviewRepository.save(review);

        if (Boolean.TRUE.equals(req.getApproved())) {
            long approvedAfter = wildcardReviewRepository.countByRound_IdAndCoordinatorApproved(
                    round.getId(), true);
            if (approvedAfter >= pool.availableSlots()) {
                autoRejectRemainingWildcardPool(round, pool, coordinator, now);
            }
        }

        auditService.log(AuditAction.ROUND_UPDATE, "wildcard_reviews", saved.getId(),
                java.util.Map.of(
                        "coordinatorApproved", req.getApproved(),
                        "teamId", saved.getTeam().getId()));

        return WildcardReviewResponse.builder()
                .id(saved.getId())
                .roundId(saved.getRound().getId())
                .teamId(saved.getTeam().getId())
                .avgScore(saved.getAvgScore())
                .coordinatorApproved(saved.getCoordinatorApproved())
                .coordinatorNote(saved.getCoordinatorNote())
                .reviewedAt(saved.getReviewedAt())
                .build();
    }

    private void autoRejectRemainingWildcardPool(
            Round round, WildcardPoolSnapshot pool, User coordinator, LocalDateTime reviewedAt) {
        for (RoundRankingItemResponse candidate : pool.selectedCandidates()) {
            wildcardReviewRepository.findByRound_IdAndTeam_Id(round.getId(), candidate.getTeamId())
                    .filter(pending -> pending.getCoordinatorApproved() == null)
                    .ifPresent(pending -> {
                        pending.setCoordinatorApproved(false);
                        pending.setCoordinatorNote("Tự động từ chối — đủ suất vé vớt");
                        pending.setReviewedBy(coordinator);
                        pending.setReviewedAt(reviewedAt);
                        wildcardReviewRepository.save(pending);
                    });
        }
    }

    private record WildcardPoolSnapshot(
            int availableSlots, int autoAdvancedCount, List<RoundRankingItemResponse> selectedCandidates) {}

    // =========================================================================
    // NHIỆM VỤ 1.3: CÀI GATE BẢO VỆ CHO ADVANCE_TEAMS (Không cho thăng vòng nếu còn Tiebreak)
    // =========================================================================
    @Override
    public AdvanceTeamsResponse advanceTeams(Integer roundId, AdvanceTeamsRequest req) {
        Round round = requirePreliminaryRoundForProgression(roundId);
        requireScoringLockedAndPublished(round);

        // Wildcard removed (plan v3): advance is Top-N per track only — never block on WC.
        // Legacy WC rows/flags are ignored for progression gates (WC-MIG).

        autoApplyResolvableTiebreaks(roundId);

        List<TiebreakItemResponse> unresolvedTiebreaks = tiebreak(roundId);
        if (!unresolvedTiebreaks.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.TIEBREAK_REQUIRED,
                    "Vẫn còn đội đồng điểm tại ranh giới thăng vòng. Vui lòng giải quyết Tiebreak trước khi Advance.",
                    java.util.Map.of("unresolvedItems", unresolvedTiebreaks));
        }

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

    /**
     * @deprecated Wildcard removed — always no-op. Kept so legacy callers compile;
     * advance never waits on vé vớt (Top-N only).
     */
    @Deprecated
    private void requireWildcardReadyForAdvance(Round round) {
        // no-op
    }

    @Override
    public AssignFinalJudgesResult assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                    "Phân Judge Chung kết chỉ cho round FINAL",
                    java.util.Map.of("roundId", roundId));
        }

        List<Warning> warnings = new ArrayList<>();
        List<Integer> assigned = new ArrayList<>();
        JudgeAssignmentType assignmentType = req.getAssignmentType() != null
                ? req.getAssignmentType()
                : JudgeAssignmentType.FINAL_EXTERNAL;
        for (Integer judgeId : req.getJudgeIds()) {
            JudgeAssignmentService.CreateResult result =
                    judgeAssignmentService.assignFinalRoundG4(roundId, judgeId, assignmentType);
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

        return AssignFinalJudgesResult.builder()
                .assignment(FinalJudgeAssignmentResponse.builder()
                        .roundId(roundId)
                        .judgeIds(assigned)
                        .build())
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    // =========================================================================
    // NHIỆM VỤ 3: SCOREBOARD PUBLIC - BẢNG ĐIỂM CÔNG KHAI (FR-20)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public RoundScoreboardResponse scoreboard(Integer roundId) {
        // 1. Lấy thông tin Round (không yêu cầu đăng nhập)
        Round round = roundAccessGuard.requireRound(roundId);

        // 2. Gate Bảo Mật: Chặn tuyệt đối nếu BTC chưa nhấn "Công Bố"
        if (!Boolean.TRUE.equals(round.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Kết quả vòng thi này chưa được Ban Tổ Chức công bố.",
                    java.util.Map.of("roundId", roundId, "isPublished", false));
        }

        // 3. Tận dụng lại hàm tính Xếp hạng (Đã xử lý trừ điểm Penalty ở Nhiệm vụ 1)
        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(roundId, false);

        // 4. Bọc data trả về cho Frontend render Landing Page
        return RoundScoreboardResponse.builder()
                .roundId(round.getId())
                .roundName(round.getName())
                .ranking(ranking)
                .build();
    }

    // =========================================================================
    // Bug3 — Advance roster (CK & loại) + Bug4 — Score breakdown
    // G5-J: full scoring-audit endpoint deferred; use scoreBreakdown per submission.
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdvanceRosterItemResponse> advanceRoster(Integer roundId, Integer page, Integer size) {
        Round round = requirePreliminaryRoundForProgression(roundId);
        if (!Boolean.TRUE.equals(round.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Phải công bố kết quả Sơ loại trước khi xem danh sách CK / loại",
                    Map.of("roundId", roundId));
        }

        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? ADVANCE_ROSTER_DEFAULT_PAGE_SIZE : Math.min(size, 100);

        List<AdvanceRosterItemResponse> all = buildAdvanceRosterItems(round);
        long total = all.size();
        int from = Math.min(pageIndex * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);

        return PageResponse.<AdvanceRosterItemResponse>builder()
                .items(all.subList(from, to))
                .page(pageIndex)
                .size(pageSize)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    private List<AdvanceRosterItemResponse> buildAdvanceRosterItems(Round round) {
        Integer roundId = round.getId();
        List<TeamRoundTrack> assignments = teamRoundTrackRepository.findByTrack_Round_Id(roundId);
        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(roundId, false);
        Map<Integer, RoundRankingItemResponse> rankingByTeam = ranking.stream()
                .filter(r -> r.getTeamId() != null)
                .collect(Collectors.toMap(RoundRankingItemResponse::getTeamId, r -> r, (a, b) -> a));

        boolean rosterDecided = assignments.stream().anyMatch(trt ->
                trt.getParticipationStatus() == ParticipationStatus.ADVANCED
                        || trt.getParticipationStatus() == ParticipationStatus.ELIMINATED);

        Set<Integer> wildcardApproved = wildcardReviewRepository.findByRound_Id(roundId).stream()
                .filter(w -> Boolean.TRUE.equals(w.getCoordinatorApproved()))
                .map(w -> w.getTeam().getId())
                .collect(Collectors.toSet());

        Integer topN = round.getTopNAdvance();
        int topNVal = topN != null && topN > 0 ? topN : 0;

        // Preview: Top-N per assignedGroup (same rule as FE buildAdvancePayload)
        Set<Integer> previewTopN = new HashSet<>();
        if (!rosterDecided && topNVal > 0) {
            Map<String, List<RoundRankingItemResponse>> byGroup = new LinkedHashMap<>();
            for (RoundRankingItemResponse item : ranking) {
                String key = item.getAssignedGroup() != null ? item.getAssignedGroup()
                        : (item.getTrackId() != null ? "T" + item.getTrackId() : "default");
                byGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
            for (List<RoundRankingItemResponse> group : byGroup.values()) {
                group.stream()
                        .filter(item -> item.getParticipationStatus() == null
                                || !ParticipationStatus.ELIMINATED.name().equals(item.getParticipationStatus()))
                        .sorted(Comparator.comparing(RoundRankingItemResponse::getRank,
                                Comparator.nullsLast(Integer::compareTo)))
                        .limit(topNVal)
                        .map(RoundRankingItemResponse::getTeamId)
                        .filter(Objects::nonNull)
                        .forEach(previewTopN::add);
            }
        }

        List<AdvanceRosterItemResponse> items = new ArrayList<>();
        for (TeamRoundTrack trt : assignments) {
            Team team = trt.getTeam();
            Track track = trt.getTrack();
            RoundRankingItemResponse rankItem = rankingByTeam.get(team.getId());
            Integer rank = rankItem != null ? rankItem.getRank() : null;
            Double totalScore = rankItem != null ? rankItem.getTotalScore() : null;
            String group = trt.getAssignedGroup() != null ? trt.getAssignedGroup()
                    : (rankItem != null ? rankItem.getAssignedGroup() : null);

            boolean isDq = team.getStatus() == TeamStatus.ELIMINATED
                    && StringUtils.hasText(team.getEliminationReason());

            String status;
            String reasonCode;
            String reasonLabel;

            if (rosterDecided) {
                status = trt.getParticipationStatus() != null
                        ? trt.getParticipationStatus().name()
                        : ParticipationStatus.ELIMINATED.name();
                if (status.equals(ParticipationStatus.ADVANCED.name())) {
                    if (wildcardApproved.contains(team.getId())
                            && (rank == null || topNVal <= 0 || rank > topNVal)) {
                        reasonCode = "WILDCARD";
                        reasonLabel = "Vé vớt";
                    } else {
                        reasonCode = "TOP_N";
                        reasonLabel = topNVal > 0
                                ? "Top " + topNVal + (group != null ? " — " + group : "")
                                : "Top N";
                    }
                } else if (isDq) {
                    reasonCode = "DQ";
                    reasonLabel = "Loại kỷ luật / DQ";
                } else {
                    reasonCode = "OUT";
                    reasonLabel = "Không vào Top N / không vé vớt";
                }
            } else {
                // Preview after publish
                if (isDq) {
                    status = ParticipationStatus.ELIMINATED.name();
                    reasonCode = "DQ";
                    reasonLabel = "Loại kỷ luật / DQ";
                } else if (previewTopN.contains(team.getId())) {
                    status = ParticipationStatus.ADVANCED.name();
                    reasonCode = "TOP_N";
                    reasonLabel = "Top " + topNVal + (group != null ? " — " + group : "");
                } else if (wildcardApproved.contains(team.getId())) {
                    status = ParticipationStatus.ADVANCED.name();
                    reasonCode = "WILDCARD";
                    reasonLabel = "Vé vớt";
                } else {
                    status = ParticipationStatus.ELIMINATED.name();
                    reasonCode = "OUT";
                    reasonLabel = "Không vào Top N / không vé vớt";
                }
            }

            items.add(AdvanceRosterItemResponse.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .trackId(track != null ? track.getId() : null)
                    .trackName(track != null ? track.getName() : null)
                    .status(status)
                    .reasonCode(reasonCode)
                    .reasonLabel(reasonLabel)
                    .rank(rank)
                    .totalScore(totalScore)
                    .assignedGroup(group)
                    .build());
        }

        items.sort(Comparator
                .comparing((AdvanceRosterItemResponse i) ->
                        ParticipationStatus.ADVANCED.name().equals(i.getStatus()) ? 0 : 1)
                .thenComparing(AdvanceRosterItemResponse::getTrackName, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdvanceRosterItemResponse::getRank, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AdvanceRosterItemResponse::getTeamName, Comparator.nullsLast(String::compareTo)));

        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreBreakdownResponse scoreBreakdown(Integer roundId, Integer submissionId) {
        Round round = roundAccessGuard.requireRound(roundId);
        if (submissionId == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "submissionId bắt buộc");
        }
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        Integer subRoundId = submission.getRound() != null
                ? submission.getRound().getId()
                : (submission.getTrack() != null ? submission.getTrack().getRound().getId() : null);
        if (!Objects.equals(subRoundId, roundId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Submission không thuộc round này",
                    Map.of("submissionId", submissionId, "roundId", roundId));
        }

        List<Criteria> criteriaList;
        List<JudgeAssignment> assignments;
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            criteriaList = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId);
            assignments = judgeAssignmentRepository.findByRoundId(roundId);
        } else {
            Integer trackId = submission.getTrack() != null ? submission.getTrack().getId() : null;
            if (trackId == null) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Submission Sơ loại thiếu track");
            }
            criteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
            assignments = judgeAssignmentRepository.findByTrackId(trackId);
        }

        List<User> judges = assignments.stream()
                .map(JudgeAssignment::getJudge)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        List<Score> scores = scoreRepository.findBySubmission_IdAndScoreType(submissionId, ScoreType.NORMAL);
        Map<String, Score> scoreMap = new HashMap<>();
        Map<Integer, LocalDateTime> lastByJudge = new HashMap<>();
        for (Score s : scores) {
            if (s.getJudge() == null || s.getCriterion() == null) continue;
            String key = s.getJudge().getId() + ":" + s.getCriterion().getId();
            scoreMap.put(key, s);
            LocalDateTime at = s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getScoredAt();
            lastByJudge.merge(s.getJudge().getId(), at, (a, b) -> a.isAfter(b) ? a : b);
        }

        List<ScoreBreakdownResponse.CriterionColumn> criteriaCols = criteriaList.stream()
                .map(c -> ScoreBreakdownResponse.CriterionColumn.builder()
                        .criterionId(c.getId())
                        .name(c.getName())
                        .maxScore(c.getMaxScore() != null ? c.getMaxScore().floatValue() : null)
                        .build())
                .toList();

        List<ScoreBreakdownResponse.JudgeRow> judgeRows = judges.stream()
                .map(j -> ScoreBreakdownResponse.JudgeRow.builder()
                        .judgeId(j.getId())
                        .judgeName(j.getFullName())
                        .lastScoredAt(lastByJudge.get(j.getId()))
                        .build())
                .toList();

        List<ScoreBreakdownResponse.Cell> cells = new ArrayList<>();
        List<Double> allValues = new ArrayList<>();
        List<ScoreBreakdownResponse.CriterionStats> criterionStats = new ArrayList<>();

        for (Criteria criterion : criteriaList) {
            List<Double> values = new ArrayList<>();
            int missing = 0;
            for (User judge : judges) {
                Score s = scoreMap.get(judge.getId() + ":" + criterion.getId());
                Float value = s != null ? s.getScoreValue() : null;
                cells.add(ScoreBreakdownResponse.Cell.builder()
                        .judgeId(judge.getId())
                        .criterionId(criterion.getId())
                        .scoreValue(value)
                        .comment(s != null ? s.getComment() : null)
                        .build());
                if (value != null) {
                    values.add(value.doubleValue());
                    allValues.add(value.doubleValue());
                } else {
                    missing++;
                }
            }
            criterionStats.add(ScoreBreakdownResponse.CriterionStats.builder()
                    .criterionId(criterion.getId())
                    .mean(meanOf(values))
                    .variance(varianceOf(values))
                    .scoredCount(values.size())
                    .missingCount(missing)
                    .build());
        }

        Team team = submission.getTeam();
        return ScoreBreakdownResponse.builder()
                .roundId(roundId)
                .submissionId(submissionId)
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getTeamName() : null)
                .criteria(criteriaCols)
                .judges(judgeRows)
                .cells(cells)
                .criterionStats(criterionStats)
                .overallMean(meanOf(allValues))
                .overallVariance(varianceOf(allValues))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundScoreAuditResponse scoreBreakdownAll(Integer roundId, Integer trackId) {
        Round round = roundAccessGuard.requireRound(roundId);
        boolean isFinal = Boolean.TRUE.equals(round.getIsFinal());

        if (trackId == null) {
            return buildScoreAuditSummary(round, isFinal);
        }
        return buildScoreAuditDetail(round, trackId, isFinal);
    }

    private RoundScoreAuditResponse buildScoreAuditSummary(Round round, boolean isFinal) {
        Integer roundId = round.getId();
        List<RoundScoreAuditResponse.TrackSummary> trackSummaries = new ArrayList<>();

        if (isFinal) {
            List<Submission> subs = submissionRepository.findByRound_Id(roundId);
            List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(roundId);
            List<Criteria> criteriaList = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId);
            trackSummaries.add(buildTrackSummary(null, round.getName() != null ? round.getName() : "Chung kết",
                    subs, assignments, criteriaList));
        } else {
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId);
            for (Track track : tracks) {
                List<Submission> subs = submissionRepository.findByTrack_Id(track.getId());
                List<JudgeAssignment> assignments = judgeAssignmentRepository.findByTrackId(track.getId());
                List<Criteria> criteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId());
                trackSummaries.add(buildTrackSummary(track.getId(), track.getName(), subs, assignments, criteriaList));
            }
        }

        return RoundScoreAuditResponse.builder()
                .roundId(roundId)
                .tracks(trackSummaries)
                .build();
    }

    private RoundScoreAuditResponse.TrackSummary buildTrackSummary(
            Integer trackId,
            String trackName,
            List<Submission> subs,
            List<JudgeAssignment> assignments,
            List<Criteria> criteriaList) {
        List<User> judges = distinctJudges(assignments);
        int expectedPerJudge = Math.max(0, subs.size() * criteriaList.size());
        List<Integer> subIds = subs.stream().map(Submission::getId).toList();
        Map<Integer, Integer> scoredByJudge = new HashMap<>();
        if (!subIds.isEmpty() && !criteriaList.isEmpty()) {
            List<Score> scores = scoreRepository.findBySubmission_IdInAndScoreType(subIds, ScoreType.NORMAL);
            for (Score s : scores) {
                if (s.getJudge() == null || s.getScoreValue() == null) continue;
                scoredByJudge.merge(s.getJudge().getId(), 1, Integer::sum);
            }
        }
        List<RoundScoreAuditResponse.JudgeProgress> progress = judges.stream()
                .map(j -> {
                    int scored = scoredByJudge.getOrDefault(j.getId(), 0);
                    double pct = expectedPerJudge == 0 ? 0.0 : (100.0 * scored / expectedPerJudge);
                    return RoundScoreAuditResponse.JudgeProgress.builder()
                            .judgeId(j.getId())
                            .judgeName(j.getFullName())
                            .scoredCells(scored)
                            .expectedCells(expectedPerJudge)
                            .percent(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .toList();

        Set<Integer> teamIds = subs.stream()
                .map(s -> s.getTeam() != null ? s.getTeam().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return RoundScoreAuditResponse.TrackSummary.builder()
                .trackId(trackId)
                .trackName(trackName)
                .teamCount(teamIds.size())
                .submissionCount(subs.size())
                .judgeProgress(progress)
                .build();
    }

    private RoundScoreAuditResponse buildScoreAuditDetail(Round round, Integer trackId, boolean isFinal) {
        Integer roundId = round.getId();
        List<Submission> subs;
        List<JudgeAssignment> assignments;
        List<Criteria> criteriaList;
        String trackName;
        Integer resolvedTrackId = trackId;

        if (isFinal) {
            // CK: trackId optional / ignored — single matrix for whole final round
            subs = submissionRepository.findByRound_Id(roundId);
            assignments = judgeAssignmentRepository.findByRoundId(roundId);
            criteriaList = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId);
            trackName = round.getName() != null ? round.getName() : "Chung kết";
            resolvedTrackId = null;
        } else {
            final Integer lookupTrackId = trackId;
            Track track = trackRepository.findById(lookupTrackId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track", lookupTrackId));
            if (track.getRound() == null || !Objects.equals(track.getRound().getId(), roundId)) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Track không thuộc round này",
                        Map.of("trackId", lookupTrackId, "roundId", roundId));
            }
            subs = submissionRepository.findByTrack_Id(lookupTrackId);
            assignments = judgeAssignmentRepository.findByTrackId(lookupTrackId);
            criteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(lookupTrackId);
            trackName = track.getName();
            resolvedTrackId = lookupTrackId;
        }

        List<User> judges = distinctJudges(assignments);
        List<Integer> subIds = subs.stream().map(Submission::getId).toList();
        Map<String, Score> scoreMap = new HashMap<>();
        if (!subIds.isEmpty()) {
            for (Score s : scoreRepository.findBySubmission_IdInAndScoreType(subIds, ScoreType.NORMAL)) {
                if (s.getJudge() == null || s.getCriterion() == null || s.getSubmission() == null) continue;
                scoreMap.put(s.getSubmission().getId() + ":" + s.getJudge().getId() + ":" + s.getCriterion().getId(), s);
            }
        }

        List<ScoreBreakdownResponse.CriterionColumn> criteriaCols = criteriaList.stream()
                .map(c -> ScoreBreakdownResponse.CriterionColumn.builder()
                        .criterionId(c.getId())
                        .name(c.getName())
                        .maxScore(c.getMaxScore() != null ? c.getMaxScore().floatValue() : null)
                        .build())
                .toList();

        List<ScoreBreakdownResponse.JudgeRow> judgeRows = judges.stream()
                .map(j -> ScoreBreakdownResponse.JudgeRow.builder()
                        .judgeId(j.getId())
                        .judgeName(j.getFullName())
                        .build())
                .toList();

        List<RoundScoreAuditResponse.TeamMatrix> teams = new ArrayList<>();
        for (Submission sub : subs) {
            Team team = sub.getTeam();
            List<ScoreBreakdownResponse.Cell> cells = new ArrayList<>();
            List<Double> allValues = new ArrayList<>();
            List<ScoreBreakdownResponse.CriterionStats> criterionStats = new ArrayList<>();
            for (Criteria criterion : criteriaList) {
                List<Double> values = new ArrayList<>();
                int missing = 0;
                for (User judge : judges) {
                    Score s = scoreMap.get(sub.getId() + ":" + judge.getId() + ":" + criterion.getId());
                    Float value = s != null ? s.getScoreValue() : null;
                    cells.add(ScoreBreakdownResponse.Cell.builder()
                            .judgeId(judge.getId())
                            .criterionId(criterion.getId())
                            .scoreValue(value)
                            .comment(s != null ? s.getComment() : null)
                            .build());
                    if (value != null) {
                        values.add(value.doubleValue());
                        allValues.add(value.doubleValue());
                    } else {
                        missing++;
                    }
                }
                criterionStats.add(ScoreBreakdownResponse.CriterionStats.builder()
                        .criterionId(criterion.getId())
                        .mean(meanOf(values))
                        .variance(varianceOf(values))
                        .scoredCount(values.size())
                        .missingCount(missing)
                        .build());
            }
            teams.add(RoundScoreAuditResponse.TeamMatrix.builder()
                    .teamId(team != null ? team.getId() : null)
                    .teamName(team != null ? team.getTeamName() : null)
                    .submissionId(sub.getId())
                    .cells(cells)
                    .criterionStats(criterionStats)
                    .overallMean(meanOf(allValues))
                    .build());
        }

        teams.sort(Comparator.comparing(RoundScoreAuditResponse.TeamMatrix::getTeamName,
                Comparator.nullsLast(String::compareTo)));

        return RoundScoreAuditResponse.builder()
                .roundId(roundId)
                .trackId(resolvedTrackId)
                .trackName(trackName)
                .criteria(criteriaCols)
                .judges(judgeRows)
                .teams(teams)
                .build();
    }

    private static List<User> distinctJudges(List<JudgeAssignment> assignments) {
        return assignments.stream()
                .map(JudgeAssignment::getJudge)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private static Double meanOf(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        double sum = 0;
        for (Double v : values) sum += v;
        return sum / values.size();
    }

    private static Double varianceOf(List<Double> values) {
        if (values == null || values.size() < 2) return values == null || values.isEmpty() ? null : 0.0;
        double mean = meanOf(values);
        double acc = 0;
        for (Double v : values) {
            double d = v - mean;
            acc += d * d;
        }
        return acc / values.size();
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
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            notifyFinalProblemReleased(round);
        } else {
            notifyPrelimProblemReleased(round);
        }
    }

    private void notifyPrelimProblemReleased(Round round) {
        List<TeamRoundTrack> assignments = teamRoundTrackRepository.findByTrack_Round_Id(round.getId());
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();

        for (Track track : tracks) {
            Set<User> teamMembers = new LinkedHashSet<>();
            for (TeamRoundTrack trt : assignments) {
                if (!trt.getTrack().getId().equals(track.getId())) {
                    continue;
                }
                teamMemberRepository.findByTeam_Id(trt.getTeam().getId()).stream()
                        .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                        .map(TeamMember::getUser)
                        .forEach(teamMembers::add);
            }
            if (!teamMembers.isEmpty()) {
                notificationService.sendBatch(
                        new ArrayList<>(teamMembers),
                        "PROBLEM_RELEASED",
                        "Đề Sơ loại — %s".formatted(track.getName()),
                        "Đề bài cho bảng \"%s\" đã được phát. Vào trang đội để tải PDF — mỗi đội chỉ thấy đề của bảng mình."
                                .formatted(track.getName()),
                        "rounds",
                        round.getId());
            }
        }

        Set<User> staff = collectTrackStaff(round.getId());
        if (!staff.isEmpty()) {
            notificationService.sendBatch(
                    new ArrayList<>(staff),
                    "PROBLEM_RELEASED",
                    "Đề Sơ loại đã phát — %s".formatted(round.getName()),
                    "Coordinator đã phát đề cho tất cả bảng đấu trong vòng Sơ loại.",
                    "rounds",
                    round.getId());
        }
    }

    private void notifyFinalProblemReleased(Round round) {
        Integer hackathonId = round.getHackathon().getId();
        Set<User> recipients = new LinkedHashSet<>();

        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (prelim != null) {
            for (TeamRoundTrack trt : teamRoundTrackRepository.findByTrack_Round_Id(prelim.getId())) {
                if (trt.getParticipationStatus() != ParticipationStatus.ADVANCED) {
                    continue;
                }
                teamMemberRepository.findByTeam_Id(trt.getTeam().getId()).stream()
                        .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                        .map(TeamMember::getUser)
                        .forEach(recipients::add);
            }
        }

        judgeAssignmentRepository.findByRoundId(round.getId()).stream()
                .map(JudgeAssignment::getJudge)
                .forEach(recipients::add);

        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(recipients),
                "PROBLEM_RELEASED",
                "Đề Chung kết đã được phát",
                "Đề bài Vòng Chung kết đã sẵn sàng. Vào trang đội để tải PDF đề bài.",
                "rounds",
                round.getId());
    }

    private Set<User> collectTrackStaff(Integer roundId) {
        Set<User> recipients = new LinkedHashSet<>();
        for (Track t : trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId)) {
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
        return recipients;
    }

    /**
     * Final release readiness: mỗi track của đội ADVANCED phải còn file PDF sơ loại.
     * Nếu chưa có đội ADVANCED (chưa advance), kiểm tra mọi track prelim còn PDF.
     */
    private List<String> listMissingPrelimPdfsForAdvancedTeams(Round finalRound) {
        Integer hackathonId = finalRound.getHackathon() != null ? finalRound.getHackathon().getId() : null;
        if (hackathonId == null) {
            return List.of("Không xác định được hackathon");
        }
        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (prelim == null) {
            return List.of("Chưa có vòng Sơ loại");
        }
        List<TeamRoundTrack> advanced = teamRoundTrackRepository.findByTrack_Round_Id(prelim.getId()).stream()
                .filter(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED)
                .toList();
        java.util.LinkedHashSet<Track> tracksToCheck = new java.util.LinkedHashSet<>();
        if (!advanced.isEmpty()) {
            for (TeamRoundTrack trt : advanced) {
                if (trt.getTrack() != null && trt.getTrack().getStatus() != TrackStatus.CANCELLED) {
                    tracksToCheck.add(trt.getTrack());
                }
            }
        } else {
            trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                    .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                    .forEach(tracksToCheck::add);
        }
        return tracksToCheck.stream()
                .filter(t -> !TrackProblemStatementStorage.hasProblemFile(t))
                .map(Track::getName)
                .toList();
    }
}
