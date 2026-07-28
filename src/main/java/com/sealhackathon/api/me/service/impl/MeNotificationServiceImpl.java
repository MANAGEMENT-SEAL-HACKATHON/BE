package com.sealhackathon.api.me.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.me.dto.request.MarkNotificationsReadRequest;
import com.sealhackathon.api.me.dto.response.MeNotificationResponse;
import com.sealhackathon.api.me.service.MeNotificationService;
import com.sealhackathon.api.notifications.entity.Notification;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeNotificationServiceImpl implements MeNotificationService {

    private static final int MAX_LIST_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public List<MeNotificationResponse> listForCurrentUser(Boolean unreadOnly) {
        Integer userId = currentUserAccessor.currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Notification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByUserIdAndIsReadFalse(userId).stream()
                        .sorted(Comparator.comparing(Notification::getSentAt).reversed())
                        .toList()
                : notificationRepository
                        .findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, MAX_LIST_SIZE))
                        .getContent();

        return notifications.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void markRead(MarkNotificationsReadRequest request) {
        Integer userId = currentUserAccessor.currentUserId();
        if (userId == null || request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) {
            return;
        }

        List<Notification> owned = notificationRepository.findAllById(request.getNotificationIds()).stream()
                .filter(n -> n.getUser() != null && userId.equals(n.getUser().getId()))
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        owned.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(owned);
    }

    private MeNotificationResponse toResponse(Notification n) {
        return MeNotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.getIsRead())
                .sentAt(n.getSentAt())
                .build();
    }
}
