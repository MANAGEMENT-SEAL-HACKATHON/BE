package com.se194093.be.notifications.service;

import com.se194093.be.users.entity.User;

import java.util.List;

/**
 * Service ghi {@code notifications} từ tầng nghiệp vụ.
 *
 * <p>Scope MF-01: SYNC insert record cùng transaction caller. KHÔNG dùng async/Spring event
 * (để dev validate flow bằng cách query trực tiếp bảng {@code notifications}).
 *
 * <p>{@code referenceType} = tên bảng (vd "hackathons", "tracks", "events"),
 * {@code referenceId} = id record bị tác động (nullable).
 */
public interface NotificationService {

    void send(User user, String type, String title, String body,
              String referenceType, Integer referenceId);

    void sendBatch(List<User> users, String type, String title, String body,
                   String referenceType, Integer referenceId);
}
