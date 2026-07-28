package com.sealhackathon.api.live_scoring;

import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresentationQueuePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Integer roundId, Integer trackId, PresentationQueueResponse payload) {
        try {
            if (trackId != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rounds/" + roundId + "/tracks/" + trackId + "/presentation-queue",
                        payload);
            }
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/presentation-queue",
                    payload);
        } catch (Exception ex) {
            log.warn("[PresentationQueue] broadcast round #{} track #{} failed: {}",
                    roundId, trackId, ex.getMessage());
        }
    }

    /**
     * Publish timer phase event so judges sync without waiting for REST poll.
     * Includes submissionId for multi-track FE filtering.
     */
    public void publishControllerChanged(
            Integer roundId,
            Integer trackId,
            Integer controllerJudgeId,
            Integer previousControllerJudgeId) {
        ControllerChangedEvent event = ControllerChangedEvent.builder()
                .type(ControllerChangedEvent.TYPE)
                .roundId(roundId)
                .trackId(trackId)
                .controllerJudgeId(controllerJudgeId)
                .previousControllerJudgeId(previousControllerJudgeId)
                .timestamp(Instant.now().toString())
                .build();
        try {
            if (trackId != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rounds/" + roundId + "/tracks/" + trackId + "/presentation-queue",
                        event);
            }
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/presentation-queue",
                    event);
        } catch (Exception ex) {
            log.warn("[PresentationQueue] CONTROLLER_CHANGED broadcast round #{} failed: {}",
                    roundId, ex.getMessage());
        }
    }

    public void publishScoringUnlocked(Integer roundId, Integer trackId, String reason) {
        ScoringUnlockedEvent event = ScoringUnlockedEvent.builder()
                .type(ScoringUnlockedEvent.TYPE)
                .roundId(roundId)
                .trackId(trackId)
                .reason(reason)
                .timestamp(Instant.now().toString())
                .build();
        try {
            if (trackId != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rounds/" + roundId + "/tracks/" + trackId + "/scoring",
                        event);
            }
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/scoring",
                    event);
            // Also on presentation-queue topic so existing FE socket receives unlock
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/presentation-queue",
                    event);
        } catch (Exception ex) {
            log.warn("[PresentationQueue] SCORING_UNLOCKED broadcast round #{} failed: {}",
                    roundId, ex.getMessage());
        }
    }

    public void publishTimerPhase(
            Integer roundId,
            Integer trackId,
            Integer submissionId,
            String phase,
            Integer remainingSeconds) {
        if (roundId == null || submissionId == null || phase == null) {
            return;
        }
        PresentationTimerPhaseEvent event = PresentationTimerPhaseEvent.builder()
                .type(PresentationTimerPhaseEvent.TYPE)
                .queueId(String.valueOf(roundId))
                .submissionId(submissionId)
                .phase(phase)
                .remainingSeconds(remainingSeconds != null ? remainingSeconds : 0)
                .timestamp(Instant.now().toString())
                .build();
        try {
            if (trackId != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rounds/" + roundId + "/tracks/" + trackId + "/presentation-queue",
                        event);
            }
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/presentation-queue",
                    event);
        } catch (Exception ex) {
            log.warn("[PresentationQueue] TIMER_PHASE broadcast round #{} track #{} failed: {}",
                    roundId, trackId, ex.getMessage());
        }
    }
}
