package com.sealhackathon.api.me.service;

import com.sealhackathon.api.me.dto.request.MarkNotificationsReadRequest;
import com.sealhackathon.api.me.dto.response.MeNotificationResponse;

import java.util.List;

public interface MeNotificationService {

    List<MeNotificationResponse> listForCurrentUser(Boolean unreadOnly);

    void markRead(MarkNotificationsReadRequest request);
}
