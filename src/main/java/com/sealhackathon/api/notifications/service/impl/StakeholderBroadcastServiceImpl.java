package com.sealhackathon.api.notifications.service.impl;

import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.NotificationsProperties;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.support.HackathonStakeholderRecipients;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StakeholderBroadcastServiceImpl implements StakeholderBroadcastService {

    private final HackathonStakeholderRecipients stakeholderRecipients;
    private final NotificationService notificationService;
    private final HackathonRepository hackathonRepository;
    private final AppProperties appProperties;
    private final NotificationsProperties notificationsProperties;
    private final StakeholderBroadcastSupport broadcastSupport;

    @Override
    public void broadcast(Integer hackathonId, String type, String title, List<String> detailLines,
                          String referenceType, Integer referenceId, boolean sendEmail) {
        String body = detailLines == null || detailLines.isEmpty()
                ? ""
                : String.join("\n", detailLines);
        broadcast(hackathonId, type, title, body, referenceType, referenceId, sendEmail);
    }

    @Override
    public void broadcast(Integer hackathonId, String type, String title, String body,
                          String referenceType, Integer referenceId, boolean sendEmail) {
        if (hackathonId == null) {
            return;
        }
        List<User> recipients = stakeholderRecipients.collect(hackathonId);
        if (recipients.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            notificationService.sendBatch(recipients, type, title, body, referenceType, referenceId);
        } else {
            broadcastSupport.sendInAppRequiresNew(recipients, type, title, body, referenceType, referenceId);
        }

        if (!sendEmail) {
            return;
        }
        if (isWithinDedupeWindow(hackathonId)) {
            log.debug("[StakeholderBroadcast] skip email (dedupe) hackathonId={}", hackathonId);
            return;
        }

        List<User> emailRecipients = new ArrayList<>(recipients);
        String subject = title;
        String headline = title;
        List<String> lines = body == null || body.isBlank()
                ? List.of()
                : List.of(body.split("\\R"));
        String ctaUrl = defaultCtaUrl();

        Runnable emailTask = () -> broadcastSupport.sendEmailsAsync(
                hackathonId, emailRecipients, subject, headline, lines, ctaUrl);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailTask.run();
                }
            });
        } else {
            emailTask.run();
        }
    }

    private boolean isWithinDedupeWindow(Integer hackathonId) {
        Hackathon h = hackathonRepository.findById(hackathonId).orElse(null);
        if (h == null || h.getLastBroadcastAt() == null) {
            return false;
        }
        long seconds = ChronoUnit.SECONDS.between(h.getLastBroadcastAt(), LocalDateTime.now());
        return seconds >= 0 && seconds < notificationsProperties.getBroadcastDedupeSeconds();
    }

    private String defaultCtaUrl() {
        String base = appProperties.getFrontendUrl();
        if (base == null || base.isBlank()) {
            return "/dashboard";
        }
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + "/dashboard";
    }
}
