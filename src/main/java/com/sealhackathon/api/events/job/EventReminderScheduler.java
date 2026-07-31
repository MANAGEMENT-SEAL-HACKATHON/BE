package com.sealhackathon.api.events.job;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gửi {@code EVENT_UPCOMING} cho user APPROVED khi sự kiện public sắp diễn ra (trong cửa sổ lead-hours).
 * Khác {@code EVENT_REMINDER} (fan-out ngay lúc tạo/sửa lịch trong {@link com.sealhackathon.api.events.service.impl.EventServiceImpl}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event-reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class EventReminderScheduler {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.event-reminder.lead-hours:24}")
    private int leadHours;

    @Scheduled(cron = "${app.event-reminder.scheduler.cron:0 0 * * * *}")
    @Transactional
    public void runUpcomingEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(leadHours);
        List<Event> due = eventRepository.findPublicUpcomingWithoutReminder(now, deadline);
        if (due.isEmpty()) {
            return;
        }

        List<User> recipients = userRepository.findAllByStatus(UserStatus.APPROVED);
        if (recipients.isEmpty()) {
            return;
        }

        for (Event event : due) {
            notificationService.sendBatch(
                    recipients,
                    "EVENT_UPCOMING",
                    "Sự kiện sắp diễn ra: %s".formatted(event.getTitle()),
                    buildBody(event),
                    "events",
                    event.getId());
            event.setReminderSentAt(now);
            eventRepository.save(event);
            log.info("Sent EVENT_UPCOMING for event #{} ({}) to {} users", event.getId(), event.getTitle(),
                    recipients.size());
        }
    }

    static String buildBody(Event event) {
        return "Thời gian: %s%s".formatted(
                event.getStartsAt(),
                event.getLocation() == null || event.getLocation().isBlank()
                        ? "" : " — " + event.getLocation());
    }
}
