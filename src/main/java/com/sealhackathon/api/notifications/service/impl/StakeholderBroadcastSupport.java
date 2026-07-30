package com.sealhackathon.api.notifications.service.impl;

import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Separate bean so {@code @Async} / {@code REQUIRES_NEW} apply via Spring proxy
 * (avoids self-invocation on {@link StakeholderBroadcastServiceImpl}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StakeholderBroadcastSupport {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final HackathonRepository hackathonRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendInAppRequiresNew(List<User> recipients, String type, String title, String body,
                                     String referenceType, Integer referenceId) {
        notificationService.sendBatch(recipients, type, title, body, referenceType, referenceId);
    }

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmailsAsync(Integer hackathonId, List<User> recipients, String subject,
                                String headline, List<String> lines, String ctaUrl) {
        for (User user : recipients) {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            try {
                emailService.sendHackathonBroadcast(
                        user.getEmail(),
                        user.getFullName(),
                        subject,
                        headline,
                        lines,
                        ctaUrl);
            } catch (Exception ex) {
                log.warn("[StakeholderBroadcast] email failed userId={} email={}: {}",
                        user.getId(), user.getEmail(), ex.getMessage());
            }
        }
        hackathonRepository.findById(hackathonId).ifPresent(h -> {
            h.setLastBroadcastAt(LocalDateTime.now());
            hackathonRepository.save(h);
        });
    }
}
