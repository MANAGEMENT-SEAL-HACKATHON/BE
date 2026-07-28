package com.sealhackathon.api.live_scoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Integer hackathonId, AnnouncementEvent event) {
        if (hackathonId == null || event == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/hackathons/" + hackathonId + "/announcements", event);
        } catch (Exception ex) {
            log.warn("[Announcement] broadcast hackathon #{} failed: {}", hackathonId, ex.getMessage());
        }
    }
}
