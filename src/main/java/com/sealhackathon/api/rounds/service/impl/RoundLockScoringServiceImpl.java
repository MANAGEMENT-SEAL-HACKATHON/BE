package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.live_scoring.event.ScoringLockedEvent;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.UnlockScoringRequest;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundLockScoringService;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundLockScoringServiceImpl implements RoundLockScoringService {

    private final RoundRepository roundRepository;
    private final RoundMapper roundMapper;
    private final RoundAccessGuard roundAccessGuard;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ScoringProgressQueryService scoringProgressQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserAccessor currentUserAccessor;
    private final ScoreRepository scoreRepository;
    private final RoundPresentationReadiness roundPresentationReadiness;
    private final HackathonRepository hackathonRepository;
    private final PresentationQueuePublisher presentationQueuePublisher;

    @Override
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
                Map.of("force", Boolean.TRUE.equals(body.getForce())));

        eventPublisher.publishEvent(new ScoringLockedEvent(this, roundId));

        if (Boolean.TRUE.equals(saved.getIsFinal())) {
            transitionHackathonToPendingConfirm(saved);
        }

        return LockScoringResult.builder()
                .round(roundMapper.toSummary(saved, 0, 0, 0f))
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Override
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

    private void transitionHackathonToPendingConfirm(Round finalRound) {
        Hackathon hackathon = finalRound.getHackathon();
        if (hackathon == null || hackathon.getStatus() != HackathonStatus.ONGOING) {
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
        if (!toUpdate.isEmpty()) {
            scoreRepository.saveAll(toUpdate);
        }
    }
}
