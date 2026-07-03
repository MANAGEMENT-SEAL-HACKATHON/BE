package com.sealhackathon.api.me.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.me.dto.request.MarkNotificationsReadRequest;
import com.sealhackathon.api.me.dto.response.MeNotificationResponse;
import com.sealhackathon.api.notifications.entity.Notification;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeNotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private MeNotificationServiceImpl service;

    @Test
    void listForCurrentUser_returnsMappedRowsForCurrentUser() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        LocalDateTime sent = LocalDateTime.of(2026, 7, 1, 10, 0);
        Notification n = Notification.builder()
                .id(1)
                .user(User.builder().id(7).build())
                .type("EVENT_REMINDER")
                .title("Sự kiện sắp diễn ra: Kickoff")
                .body("Thời gian: 2026-07-05T09:00")
                .isRead(false)
                .sentAt(sent)
                .build();
        when(notificationRepository.findByUserIdOrderBySentAtDesc(eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        List<MeNotificationResponse> result = service.listForCurrentUser(false);

        assertEquals(1, result.size());
        assertEquals("EVENT_REMINDER", result.get(0).getType());
        assertEquals("Sự kiện sắp diễn ra: Kickoff", result.get(0).getTitle());
        assertFalse(result.get(0).getIsRead());
        assertEquals(sent, result.get(0).getSentAt());
    }

    @Test
    void listForCurrentUser_unreadOnly_filtersUnread() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        Notification unread = Notification.builder()
                .id(2)
                .user(User.builder().id(7).build())
                .type("TEAM_WARNING")
                .title("Cảnh báo")
                .body("body")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findByUserIdAndIsReadFalse(7)).thenReturn(List.of(unread));

        List<MeNotificationResponse> result = service.listForCurrentUser(true);

        assertEquals(1, result.size());
        assertEquals("TEAM_WARNING", result.get(0).getType());
    }

    @Test
    void markRead_onlyUpdatesOwnedUnreadNotifications() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        User owner = User.builder().id(7).build();
        User other = User.builder().id(99).build();
        Notification ownedUnread = Notification.builder()
                .id(10)
                .user(owner)
                .type("JUDGE_ASSIGNED")
                .title("Judge")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        Notification foreign = Notification.builder()
                .id(11)
                .user(other)
                .type("JUDGE_ASSIGNED")
                .title("Foreign")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findAllById(List.of(10, 11))).thenReturn(List.of(ownedUnread, foreign));

        MarkNotificationsReadRequest req = new MarkNotificationsReadRequest();
        req.setNotificationIds(List.of(10, 11));
        service.markRead(req);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().get(0).getIsRead());
        assertTrue(captor.getValue().get(0).getReadAt() != null);
    }
}
