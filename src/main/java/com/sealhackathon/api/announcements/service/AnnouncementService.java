package com.sealhackathon.api.announcements.service;

import com.sealhackathon.api.announcements.dto.response.AnnouncementResponse;
import com.sealhackathon.api.announcements.entity.AnnouncementView;
import com.sealhackathon.api.announcements.entity.HackathonAnnouncement;
import com.sealhackathon.api.announcements.repository.AnnouncementViewRepository;
import com.sealhackathon.api.announcements.repository.HackathonAnnouncementRepository;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.live_scoring.AnnouncementEvent;
import com.sealhackathon.api.live_scoring.AnnouncementPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final HackathonAnnouncementRepository announcementRepository;
    private final AnnouncementViewRepository viewRepository;
    private final AnnouncementPublisher announcementPublisher;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;

    public HackathonAnnouncement publishResults(Integer hackathonId, Integer roundId, String title, String message) {
        HackathonAnnouncement ann = announcementRepository.save(HackathonAnnouncement.builder()
                .hackathonId(hackathonId)
                .roundId(roundId)
                .kind(AnnouncementEvent.RESULTS_PUBLISHED)
                .title(title)
                .message(message)
                .softHidden(false)
                .createdAt(LocalDateTime.now())
                .build());
        announcementPublisher.publish(hackathonId, AnnouncementEvent.builder()
                .type(AnnouncementEvent.TYPE)
                .kind(AnnouncementEvent.RESULTS_PUBLISHED)
                .hackathonId(hackathonId)
                .roundId(roundId)
                .title(title)
                .message(message)
                .timestamp(Instant.now().toString())
                .build());
        auditService.log(AuditAction.ANNOUNCEMENT_PUBLISHED, "hackathon_announcements", ann.getId(),
                Map.of("hackathonId", hackathonId, "roundId", roundId, "kind", AnnouncementEvent.RESULTS_PUBLISHED));
        return ann;
    }

    @Transactional(readOnly = true)
    public List<HackathonAnnouncement> listVisible(Integer hackathonId) {
        return announcementRepository.findByHackathonIdAndSoftHiddenFalseOrderByCreatedAtDesc(hackathonId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> feedForCurrentUser(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        List<HackathonAnnouncement> items = listVisible(hackathonId);
        LocalDateTime lastViewed = viewRepository.findByUserIdAndHackathonId(userId, hackathonId)
                .map(AnnouncementView::getLastViewedAt)
                .orElse(null);
        long unread = items.stream()
                .filter(a -> lastViewed == null || a.getCreatedAt().isAfter(lastViewed))
                .count();
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("unreadCount", unread);
        out.put("lastViewedAt", lastViewed);
        return out;
    }

    public void markViewed(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        AnnouncementView view = viewRepository.findByUserIdAndHackathonId(userId, hackathonId)
                .orElse(AnnouncementView.builder().userId(userId).hackathonId(hackathonId).build());
        view.setLastViewedAt(LocalDateTime.now());
        viewRepository.save(view);
    }

    public AnnouncementResponse softHide(Integer announcementId, boolean hidden) {
        HackathonAnnouncement ann = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));
        ann.setSoftHidden(hidden);
        announcementRepository.save(ann);
        auditService.log(AuditAction.ANNOUNCEMENT_SOFT_HIDE, "hackathon_announcements", announcementId,
                Map.of("softHidden", hidden));
        return AnnouncementResponse.from(ann);
    }
}
