package com.sealhackathon.api.notifications.service.impl;

import com.sealhackathon.api.notifications.entity.Notification;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sync impl của {@link NotificationService}. Insert record vào bảng {@code notifications} ngay
 * trong cùng transaction caller (không async, không Spring event). Mọi exception khi save sẽ
 * propagate để rollback cùng mutation chính.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void send(User user, String type, String title, String body,
                     String referenceType, Integer referenceId) {
        if (user == null) {
            log.warn("[NotificationService] skip send: user is null (type={})", type);
            return;
        }
        Notification n = Notification.builder()
                .user(user).type(type).title(title).body(body)
                .referenceType(referenceType).referenceId(referenceId)
                .isRead(false).sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(n);
    }

    @Override
    public void sendBatch(List<User> users, String type, String title, String body,
                          String referenceType, Integer referenceId) {
        if (users == null || users.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Notification> batch = users.stream()
                .filter(u -> u != null)
                .map(u -> Notification.builder()
                        .user(u).type(type).title(title).body(body)
                        .referenceType(referenceType).referenceId(referenceId)
                        .isRead(false).sentAt(now)
                        .build())
                .toList();
        notificationRepository.saveAll(batch);
    }
}
