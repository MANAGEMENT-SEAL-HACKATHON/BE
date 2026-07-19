package com.sealhackathon.api.live_scoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Bắn tín hiệu invalidate nhẹ khi có bài nộp mới/cập nhật để Coord panel
 * «Tình trạng nộp bài» refresh ngay (không chờ poll). Tái dùng topic
 * leaderboard-preview vốn FE chỉ dùng làm trigger reload (bỏ qua payload) —
 * không dựng WS stack mới.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionRosterPublisher {

    /** Payload dạng record (không dùng Map để tránh convertAndSend ambiguous). */
    public record RosterInvalidate(String type, String at) {}

    private final SimpMessagingTemplate messagingTemplate;

    public void publishInvalidate(Integer roundId) {
        if (roundId == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/leaderboard-preview",
                    new RosterInvalidate("SUBMISSION_ROSTER_INVALIDATE", Instant.now().toString()));
        } catch (Exception ex) {
            log.warn("[SubmissionRoster] invalidate round #{} failed: {}", roundId, ex.getMessage());
        }
    }
}
