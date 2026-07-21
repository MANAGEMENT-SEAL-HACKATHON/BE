package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationTimerActionResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationTimerBlock;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.guard.PresentationForceAdvanceAckGuard;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.service.PresentationTimerService;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationNextScoringGuard;
import com.sealhackathon.api.presentation.support.PresentationQaTimeoutMaterializer;
import com.sealhackathon.api.presentation.support.PresentationTimerCalculator;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationTimerServiceImpl implements PresentationTimerService {

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationControllerGuard controllerGuard;
    private final PresentationForceAdvanceAckGuard forceAdvanceAckGuard;
    private final PresentationNextScoringGuard nextScoringGuard;
    private final PresentationDurationResolver durationResolver;
    private final PresentationQueueService presentationQueueService;
    private final PresentationQueuePublisher queuePublisher;
    private final RoundPhaseResolver roundPhaseResolver;

    @Override
    public PresentationTimerActionResponse start(Integer roundId, Integer trackId) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);
        if (slot.getTimerPhase() != PresentationTimerPhase.IDLE
                && slot.getTimerPhase() != PresentationTimerPhase.SETUP
                && slot.getTimerPhase() != PresentationTimerPhase.ENDED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Timer đã chạy");
        }
        slot.setTimerPhase(PresentationTimerPhase.PRESENTING);
        slot.setPresentationStartedAt(LocalDateTime.now());
        slot.setQaStartedAt(null);
        slot.setPausedAt(null);
        slot.setPausedAccumulatedSeconds(0);
        slot.setTimerPhaseBeforePause(null);
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    @Override
    public PresentationTimerActionResponse pause(Integer roundId, Integer trackId) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);
        if (slot.getTimerPhase() != PresentationTimerPhase.PRESENTING
                && slot.getTimerPhase() != PresentationTimerPhase.QA) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Chỉ pause khi đang PRESENTING hoặc QA");
        }
        slot.setTimerPhaseBeforePause(slot.getTimerPhase());
        slot.setTimerPhase(PresentationTimerPhase.PAUSED);
        slot.setPausedAt(LocalDateTime.now());
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    @Override
    public PresentationTimerActionResponse resume(Integer roundId, Integer trackId) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);
        if (slot.getTimerPhase() != PresentationTimerPhase.PAUSED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Timer không ở trạng thái PAUSED");
        }
        if (slot.getPausedAt() != null) {
            int extra = (int) Duration.between(slot.getPausedAt(), LocalDateTime.now()).getSeconds();
            slot.setPausedAccumulatedSeconds(
                    (slot.getPausedAccumulatedSeconds() != null ? slot.getPausedAccumulatedSeconds() : 0) + extra);
        }
        PresentationTimerPhase restore = slot.getTimerPhaseBeforePause() != null
                ? slot.getTimerPhaseBeforePause()
                : PresentationTimerPhase.PRESENTING;
        slot.setTimerPhase(restore);
        slot.setPausedAt(null);
        slot.setTimerPhaseBeforePause(null);
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    @Override
    public PresentationTimerActionResponse qa(Integer roundId, Integer trackId) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);
        if (slot.getTimerPhase() != PresentationTimerPhase.PRESENTING) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Chỉ chuyển QA từ PRESENTING");
        }
        slot.setTimerPhase(PresentationTimerPhase.QA);
        slot.setQaStartedAt(LocalDateTime.now());
        slot.setPausedAt(null);
        slot.setPausedAccumulatedSeconds(0);
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    @Override
    public PresentationTimerActionResponse end(Integer roundId, Integer trackId, boolean acknowledgeIncompleteScoring) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);

        boolean inQa = slot.getTimerPhase() == PresentationTimerPhase.QA
                || (slot.getTimerPhase() == PresentationTimerPhase.PAUSED
                && slot.getTimerPhaseBeforePause() == PresentationTimerPhase.QA
                && slot.getQaStartedAt() != null);
        if (!inQa) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ kết thúc sớm Q&A khi đang ở pha QA (hoặc tạm dừng sau QA)");
        }

        int remaining = PresentationTimerCalculator.remainingSeconds(
                slot, ctx.track(), ctx.round(), durationResolver);

        // Hết giờ tự nhiên → ENDED, ghi nhận điểm tới đâu (không bắt đủ chốt)
        if (remaining <= 0 && slot.getTimerPhase() == PresentationTimerPhase.QA) {
            slot.setTimerPhase(PresentationTimerPhase.ENDED);
            slot.setQaEndedEarly(false);
            presentationSlotRepository.save(slot);
            return publishAndRespond(ctx, slot);
        }

        // Kết thúc sớm: mọi GK phải Chốt điểm (trừ Coord/controller force-ack)
        if (slot.getSubmission() != null) {
            boolean ack = forceAdvanceAckGuard.resolveAcknowledge(
                    acknowledgeIncompleteScoring, ctx.trackId(), ctx.round());
            nextScoringGuard.validateBeforeNext(
                    slot.getSubmission(), ctx.trackId(), ctx.round(), ack, true);
        }

        slot.setTimerPhase(PresentationTimerPhase.ENDED);
        slot.setQaEndedEarly(true);
        slot.setPausedAt(null);
        slot.setTimerPhaseBeforePause(null);
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    @Override
    public PresentationTimerActionResponse reset(Integer roundId, Integer trackId) {
        TimerContext ctx = resolveContext(roundId, trackId);
        PresentationSlot slot = requirePresentingSlot(ctx);
        slot.setTimerPhase(PresentationTimerPhase.IDLE);
        slot.setTimerPhaseBeforePause(null);
        slot.setPresentationStartedAt(null);
        slot.setQaStartedAt(null);
        slot.setPausedAt(null);
        slot.setPausedAccumulatedSeconds(0);
        slot.setQaEndedEarly(null);
        presentationSlotRepository.save(slot);
        return publishAndRespond(ctx, slot);
    }

    private PresentationTimerActionResponse publishAndRespond(TimerContext ctx, PresentationSlot slot) {
        PresentationQueueResponse payload = presentationQueueService.getQueue(ctx.round().getId(), ctx.trackId());
        queuePublisher.publish(ctx.round().getId(), ctx.trackId(), payload);
        Integer submissionId = slot.getSubmission() != null ? slot.getSubmission().getId() : null;
        PresentationTimerBlock timer = buildTimerBlock(slot, ctx.track(), ctx.round());
        if (submissionId != null && timer.getPhase() != null) {
            queuePublisher.publishTimerPhase(
                    ctx.round().getId(),
                    ctx.trackId(),
                    submissionId,
                    timer.getPhase(),
                    timer.getRemainingSeconds());
        }
        return PresentationTimerActionResponse.builder()
                .roundId(ctx.round().getId())
                .trackId(ctx.trackId())
                .submissionId(submissionId)
                .timer(timer)
                .build();
    }

    private PresentationTimerBlock buildTimerBlock(PresentationSlot slot, Track track, Round round) {
        PresentationQaTimeoutMaterializer.materializeIfExpired(
                slot, track, round, durationResolver, presentationSlotRepository);
        PresentationTimerPhase phase = slot.getTimerPhase() != null ? slot.getTimerPhase() : PresentationTimerPhase.IDLE;
        return PresentationTimerBlock.builder()
                .phase(phase.name())
                .presentationMinutes(durationResolver.presentationMinutes(track, round))
                .qaMinutes(durationResolver.qaMinutes(track, round))
                .presentationStartedAt(slot.getPresentationStartedAt())
                .qaStartedAt(slot.getQaStartedAt())
                .pausedAt(slot.getPausedAt())
                .pausedAccumulatedSeconds(slot.getPausedAccumulatedSeconds())
                .remainingSeconds(PresentationTimerCalculator.remainingSeconds(slot, track, round, durationResolver))
                .qaEndedEarly(slot.getQaEndedEarly())
                .build();
    }

    private PresentationSlot requirePresentingSlot(TimerContext ctx) {
        PresentationSlot found = findPresentingSlot(ctx)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Không có đội đang PRESENTING trong queue"));
        // Atomic race guard: re-load with PESSIMISTIC_WRITE trước khi mutate phase
        return presentationSlotRepository.findByIdForUpdate(found.getId())
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Không có đội đang PRESENTING trong queue"));
    }

    private java.util.Optional<PresentationSlot> findPresentingSlot(TimerContext ctx) {
        if (ctx.trackId() == null) {
            return presentationSlotRepository.findFirstByRound_IdAndTrackIsNullAndQueueStatus(
                    ctx.round().getId(), PresentationQueueStatus.PRESENTING);
        }
        return presentationSlotRepository.findFirstByRound_IdAndTrack_IdAndQueueStatus(
                ctx.round().getId(), ctx.trackId(), PresentationQueueStatus.PRESENTING);
    }

    private TimerContext resolveContext(Integer roundId, Integer trackId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (roundPhaseResolver.resolve(round) != RoundPhase.JUDGING) {
            throw new BusinessRuleException(ErrorCode.SCORING_NOT_OPEN,
                    "Chưa hết giờ nộp / chưa kết thúc sớm — không điều khiển timer thuyết trình");
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            controllerGuard.requireControllerForRound(roundId, round);
            return new TimerContext(round, null, null);
        }
        if (trackId == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "trackId bắt buộc");
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (!track.getRound().getId().equals(roundId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Track không thuộc round");
        }
        controllerGuard.requireControllerForTrack(trackId, track, round);
        return new TimerContext(round, trackId, track);
    }

    private record TimerContext(Round round, Integer trackId, Track track) {}
}
