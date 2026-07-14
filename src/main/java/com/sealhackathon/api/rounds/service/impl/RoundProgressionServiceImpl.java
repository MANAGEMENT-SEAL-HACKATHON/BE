package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
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
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.AssignFinalJudgesResult;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidateResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidatesResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.WildcardCandidateSelection;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import com.sealhackathon.api.wildcard_reviews.entity.WildcardReview;
import com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository;
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
    private final HackathonRepository hackathonRepository;
    private final RoundProblemStatementStorage problemStatementStorage;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public RoundSummaryResponse releaseProblem(Integer roundId, MultipartFile file) {
        Round round = roundAccessGuard.requireActiveRound(roundId);
        if (round.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát — không thể thay đổi");
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            if (file != null && !file.isEmpty()) {
                problemStatementStorage.store(round, file);
            } else if (!RoundProblemStatementStorage.hasProblemFile(round)) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Vui lòng upload file PDF đề Chung kết trước khi phát");
            }
        } else {
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
        }
        round.setProblemReleasedAt(LocalDateTime.now());
        Round saved = roundRepository.save(round);
        java.util.Map<String, Object> auditMeta = new java.util.HashMap<>();
        auditMeta.put("isFinal", Boolean.TRUE.equals(saved.getIsFinal()));
        if (Boolean.TRUE.equals(saved.getIsFinal())) {
            String filename = RoundProblemStatementStorage.displayFilename(saved);
            if (filename != null) {
                auditMeta.put("filename", filename);
            }
            if (StringUtils.hasText(saved.getProblemStatementStorageKey())) {
                auditMeta.put("storageKey", saved.getProblemStatementStorageKey());
            }
        } else {
            auditMeta.put("trackCount", trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId).stream()
                    .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                    .count());
        }
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
        boolean deadlineAdjusted = round.getSubmissionDeadline() == null
                || round.getSubmissionDeadline().isAfter(now);
        boolean examAtAdjusted = round.getExamAt() == null || round.getExamAt().isAfter(now);

        round.setSubmissionClosedEarlyAt(now);
        if (deadlineAdjusted) {
            // Clamp to past so submit ngay sau close luôn afterDeadline (tránh race isAfter(now)==false)
            round.setSubmissionDeadline(now.minusSeconds(5));
        }
        if (examAtAdjusted) {
            round.setExamAt(now);
        }
        if (deadlineAdjusted) {
            round.setDeadlineReminderSentAt(null);
        }

        Round saved = roundRepository.save(round);
        auditService.log(AuditAction.ROUND_CLOSE_SUBMISSION_EARLY, "rounds", roundId, Map.of(
                "examAtAdjusted", examAtAdjusted,
                "deadlineAdjusted", deadlineAdjusted,
                "submissionDeadline", String.valueOf(saved.getSubmissionDeadline()),
                "examAt", String.valueOf(saved.getExamAt())));

        return CloseSubmissionEarlyResponse.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .examAtAdjusted(examAtAdjusted)
                .deadlineAdjusted(deadlineAdjusted)
                .closedAt(now)
                .build();
    }

    @Override
    @Transactional
    public LockScoringResult lockScoring(Integer roundId, LockScoringRequest req) {
        LockScoringRequest body = req != null ? req : LockScoringRequest.builder().build();
        Round round = roundAccessGuard.requireActiveRoundForUpdate(roundId);
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

        if (Boolean.TRUE.equals(saved.getIsFinal())) {
            transitionHackathonToPendingConfirm(saved);
        }

        return LockScoringResult.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
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

    // =========================================================================
    // NHIỆM VỤ 1.1: TÌM KIẾM CÁC ĐỘI ĐỒNG ĐIỂM TẠI RANH GIỚI CUT-OFF
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<TiebreakItemResponse> tiebreak(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return tiebreakForFinalRound(roundId);
        }

        Integer topNAdvance = round.getTopNAdvance();
        if (topNAdvance == null || topNAdvance <= 0) {
            return List.of();
        }

        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(roundId, false);
        if (ranking.isEmpty()) return List.of();

        // Gom nhóm Ranking theo Bảng đấu (Partition Key)
        Map<String, List<RoundRankingItemResponse>> partitionedRanking = ranking.stream()
                .collect(Collectors.groupingBy(item -> {
                    String trackPart = item.getTrackId() != null ? item.getTrackId().toString() : "0";
                    String groupPart = item.getAssignedGroup() != null ? item.getAssignedGroup() : "DEFAULT";
                    return trackPart + "_" + groupPart;
                }));

        List<TiebreakItemResponse> tiebreakItems = new ArrayList<>();

        // Thuật toán dò tìm Tiebreak cho từng Bảng
        for (Map.Entry<String, List<RoundRankingItemResponse>> entry : partitionedRanking.entrySet()) {
            List<RoundRankingItemResponse> groupRanking = entry.getValue();
            if (groupRanking.size() <= topNAdvance) {
                continue; // Bảng này có số lượng đội <= chỉ tiêu, không cần tiebreak
            }

            // Lấy điểm của đội đang đứng chính xác tại vị trí Cut-off (Chỉ tiêu)
            Double cutoffScore = groupRanking.get(topNAdvance - 1).getTotalScore();

            // Đếm số lượng đội nằm TRÊN mức điểm Cut-off (chắc chắn an toàn)
            long safeCount = groupRanking.stream().filter(r -> r.getTotalScore() > cutoffScore).count();

            // Lấy danh sách TẤT CẢ các đội có điểm BẰNG CHÍNH XÁC điểm Cut-off
            List<Integer> borderlineTeamIds = groupRanking.stream()
                    .filter(r -> r.getTotalScore().equals(cutoffScore))
                    .map(RoundRankingItemResponse::getTeamId)
                    .toList();

            // Tính số "Ghế" còn trống cho các đội bằng điểm
            long remainingSlots = topNAdvance - safeCount;

            // NẾU số đội bằng điểm LỚN HƠN số ghế còn trống => Phát sinh Tiebreak!
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

    private List<TiebreakItemResponse> tiebreakForFinalRound(Integer roundId) {
        List<RoundRankingItemResponse> ranking = roundRankingQueryService.rankingForRound(roundId, false);
        if (ranking.isEmpty()) {
            return List.of();
        }

        List<TiebreakItemResponse> tiebreakItems = new ArrayList<>();
        int i = 0;
        while (i < ranking.size()) {
            Double score = ranking.get(i).getTotalScore();
            int j = i + 1;
            while (j < ranking.size() && java.util.Objects.equals(ranking.get(j).getTotalScore(), score)) {
                j++;
            }
            if (j - i > 1) {
                List<Integer> tiedTeamIds = ranking.subList(i, j).stream()
                        .map(RoundRankingItemResponse::getTeamId)
                        .toList();
                tiebreakItems.add(TiebreakItemResponse.builder()
                        .partitionKey("FINAL")
                        .cutoffRank(ranking.get(i).getRank())
                        .candidateTeamIds(tiedTeamIds)
                        .build());
            }
            i = j;
        }
        return tiebreakItems;
    }

    // =========================================================================
    // NHIỆM VỤ 1.2: CẬP NHẬT KẾT QUẢ PHÂN XỬ ĐỒNG ĐIỂM (COORDINATOR DECISION)
    // =========================================================================
    @Override
    public List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req) {
        Round round = roundAccessGuard.requireRound(roundId);
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
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "orderedTeamIds phải khớp nhóm đội đang hòa điểm cần tiebreak",
                    java.util.Map.of("orderedTeamIds", orderedIds));
        }

        // Áp dụng điểm Penalty tăng dần để tách top (VD: Đội 1: 0đ, Đội 2: -0.01đ, Đội 3: -0.02đ)
        // Đội xếp đầu tiên trong Request sẽ giữ nguyên điểm, các đội sau sẽ bị trừ dần để tụt hạng.
        float penaltyIncrement = 0.01f;
        float currentPenalty = 0.0f;

        List<TiebreakEvaluation> evaluationsToSave = new ArrayList<>();

        for (Integer teamId : orderedIds) {
            Team team = teamRepository.findById(teamId).orElseThrow();

            // Xóa Tiebreak cũ của Coordinator cho đội này ở Vòng này (nếu đã từng làm) để ghi đè
            tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(roundId, teamId, coordinator.getId())
                    .ifPresent(tiebreakEvaluationRepository::delete);

            if (currentPenalty > 0) {
                evaluationsToSave.add(TiebreakEvaluation.builder()
                        .round(round)
                        .team(team)
                        .judge(coordinator) // Ở mức Coordinator Decision, Judge chính là Coordinator
                        .penaltyScore(currentPenalty)
                        .isCastingVote(true)
                        .tiebreakLevel(2) // Level 2: Quyết định của BTC
                        .notes(req.getNote())
                        .evaluatedAt(LocalDateTime.now())
                        .build());
            }
            currentPenalty += penaltyIncrement;
        }

        if (!evaluationsToSave.isEmpty()) {
            tiebreakEvaluationRepository.saveAll(evaluationsToSave);
            auditService.log(AuditAction.ROUND_TIEBREAK_RESOLVED, "tiebreak_evaluations", roundId,
                    java.util.Map.of("orderedTeamIds", orderedIds, "note", req.getNote()));
        }

        // Trả về Bảng Xếp Hạng mới ngay lập tức để FE render lại UI
        return roundRankingQueryService.rankingForRound(roundId, false);
    }

    // =========================================================================
    // NHIỆM VỤ 2.1: TỰ ĐỘNG QUÉT VÀ ĐỀ XUẤT VÉ VỚT (WILDCARD CANDIDATES)
    // =========================================================================
    @Override
    public WildcardCandidatesResponse wildcardCandidates(Integer roundId) {
        Round round = roundAccessGuard.requireRound(roundId);
        Hackathon hackathon = round.getHackathon();
        boolean hackathonEnabled = Boolean.TRUE.equals(hackathon.getWildcardEnabled());
        boolean roundEnabled = Boolean.TRUE.equals(round.getWildcardEnabled());

        if (!hackathonEnabled || !roundEnabled) {
            return emptyWildcardResponse(hackathonEnabled, roundEnabled);
        }

        Optional<WildcardPoolSnapshot> pool = resolveWildcardPool(round);
        if (pool.isEmpty()) {
            return WildcardCandidatesResponse.builder()
                    .hackathonWildcardEnabled(true)
                    .roundWildcardEnabled(true)
                    .availableSlots(0)
                    .autoAdvancedCount(0)
                    .approvedCount(0)
                    .decisionsFinalized(false)
                    .candidates(List.of())
                    .build();
        }

        WildcardPoolSnapshot snapshot = pool.get();
        List<WildcardCandidateResponse> responses = buildWildcardCandidateResponses(round, snapshot);

        int approvedCount = (int) responses.stream()
                .filter(c -> Boolean.TRUE.equals(c.getCoordinatorApproved()))
                .count();
        boolean decisionsFinalized = !responses.isEmpty()
                && responses.stream().allMatch(c -> c.getCoordinatorApproved() != null);

        return WildcardCandidatesResponse.builder()
                .hackathonWildcardEnabled(true)
                .roundWildcardEnabled(true)
                .availableSlots(snapshot.availableSlots())
                .autoAdvancedCount(snapshot.autoAdvancedCount())
                .approvedCount(approvedCount)
                .decisionsFinalized(decisionsFinalized)
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
                .candidates(List.of())
                .build();
    }

    private Optional<WildcardPoolSnapshot> resolveWildcardPool(Round round) {
        Integer minTeamsFinal = round.getMinTeamsFinal();
        Integer topNAdvance = round.getTopNAdvance();
        if (minTeamsFinal == null || topNAdvance == null || topNAdvance <= 0) {
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

        int slots = minTeamsFinal - topNTeams.size();
        if (slots <= 0 || remainingTeams.isEmpty()) {
            return Optional.empty();
        }

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectWithTiesAtCutoff(remainingTeams, slots);
        return Optional.of(new WildcardPoolSnapshot(slots, topNTeams.size(), selected));
    }

    private List<WildcardCandidateResponse> buildWildcardCandidateResponses(
            Round round, WildcardPoolSnapshot snapshot) {
        List<WildcardCandidateResponse> responses = new ArrayList<>();
        int candidateRank = 1;
        int slots = snapshot.availableSlots();
        int poolSize = snapshot.selectedCandidates().size();

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
                            .avgScore(candidate.getTotalScore() != null
                                    ? candidate.getTotalScore().floatValue()
                                    : 0f)
                            .build());

            if (review.getId() == null) {
                review = wildcardReviewRepository.save(review);
            }

            responses.add(WildcardCandidateResponse.builder()
                    .reviewId(review.getId())
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .assignedGroup(candidate.getAssignedGroup())
                    .candidateRank(candidateRank++)
                    .totalScore(candidate.getTotalScore())
                    .reason(slots < poolSize
                            ? "Đồng điểm tại ngưỡng vé vớt — cần Coordinator chọn "
                                    + slots + " / " + poolSize + " đội"
                            : "Hệ thống đề xuất: Top " + slots + " điểm cao nhất ngoài Top "
                                    + round.getTopNAdvance() + " mỗi bảng")
                    .coordinatorApproved(review.getCoordinatorApproved())
                    .coordinatorNote(review.getCoordinatorNote())
                    .build());
        }
        return responses;
    }

    // =========================================================================
    // NHIỆM VỤ 2.2: LƯU QUYẾT ĐỊNH CỦA BAN TỔ CHỨC (DUYỆT VÉ VỚT)
    // =========================================================================
    @Override
    public WildcardReviewResponse decideWildcardReview(Integer reviewId, WildcardReviewDecisionRequest req) {
        WildcardReview review = wildcardReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("WildcardReview", reviewId));

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

        // RÀO CHẮN (GATE): NẾU CÒN ĐỘI CHƯA TIEBREAK THÌ CHẶN LẠI
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
}
