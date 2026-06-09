package com.sealhackathon.api.live_scoring;

import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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
}
