package com.sealhackathon.api.hackathons.job;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Nhắc Coordinator khi còn Hackathon ở trạng thái DRAFT (setup chưa hoàn tất) mà ngày mở đăng ký đã cận kề
 * hoặc đã trôi qua. Gửi một lần cho mỗi hackathon (idempotent qua {@code draftReminderSentAt}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.draft-reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class HackathonDraftReminderScheduler {

    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.draft-reminder.lead-days:3}")
    private int leadDays;

    @Scheduled(cron = "${app.draft-reminder.scheduler.cron:0 0 8 * * *}")
    @Transactional
    public void runDraftReminders() {
        LocalDate threshold = LocalDate.now().plusDays(leadDays);
        List<Hackathon> drafts = hackathonRepository
                .findByStatusAndDraftReminderSentAtIsNull(HackathonStatus.DRAFT).stream()
                .filter(h -> h.getRegistrationStart() == null
                        || !h.getRegistrationStart().isAfter(threshold))
                .toList();
        if (drafts.isEmpty()) {
            return;
        }

        List<User> coordinators = userRepository
                .findAllByRoleAndStatus(UserRole.COORDINATOR, UserStatus.APPROVED);

        LocalDateTime now = LocalDateTime.now();
        for (Hackathon h : drafts) {
            Set<User> recipients = new LinkedHashSet<>(coordinators);
            if (h.getCreatedBy() != null) {
                recipients.add(h.getCreatedBy());
            }
            if (recipients.isEmpty()) {
                continue;
            }
            notificationService.sendBatch(
                    new ArrayList<>(recipients),
                    "HACKATHON_DRAFT_REMINDER",
                    "Hackathon \"%s\" vẫn đang ở trạng thái nháp".formatted(h.getName()),
                    "Cuộc thi chưa được kích hoạt (DRAFT)%s. Hãy vào hoàn tất thiết lập và chuyển sang ONGOING trước khi mở đăng ký.".formatted(
                            h.getRegistrationStart() == null ? ""
                                    : " — ngày mở đăng ký dự kiến %s".formatted(h.getRegistrationStart())),
                    "hackathons",
                    h.getId());
            h.setDraftReminderSentAt(now);
            hackathonRepository.save(h);
            log.info("Sent HACKATHON_DRAFT_REMINDER for hackathon #{} ({}) to {} users",
                    h.getId(), h.getName(), recipients.size());
        }
    }
}
