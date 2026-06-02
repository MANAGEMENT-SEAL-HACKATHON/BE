package com.sealhackathon.api.me.service.impl;

import com.sealhackathon.api.me.dto.request.MarkNotificationsReadRequest;
import com.sealhackathon.api.me.dto.response.MeNotificationResponse;
import com.sealhackathon.api.me.service.MeNotificationService;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeNotificationServiceImpl implements MeNotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<MeNotificationResponse> listForCurrentUser(Boolean unreadOnly) {
        // TODO: FR — load notifications for auth user, filter unreadOnly
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void markRead(MarkNotificationsReadRequest request) {
        // TODO: FR — PATCH is_read for owned notifications
    }
}
