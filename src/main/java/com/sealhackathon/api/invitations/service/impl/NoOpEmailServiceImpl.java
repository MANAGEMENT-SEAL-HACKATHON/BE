package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.invitations.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Impl no-op cho dev trước khi module Email Worker sẵn sàng. Chỉ log để Coordinator/QA xác nhận
 * service đã được gọi.
 *
 * <p>Khi module Email Worker ra mắt: tạo impl mới ({@code SmtpEmailServiceImpl} chẳng hạn) +
 * đánh dấu {@code @Primary} hoặc xóa class này.
 */
@Service
@Slf4j
public class NoOpEmailServiceImpl implements EmailService {

    @Override
    public void sendInvitation(String email, String fullName, String token, LocalDateTime expiresAt) {
        log.info("[EmailService stub] sendInvitation to {} ({}) token=... expires={}",
                email, fullName, expiresAt);
        log.debug("[EmailService stub] Token (DO NOT log in production): {}", token);
    }

    @Override
    public void resendInvitation(String email, String fullName, String token, LocalDateTime expiresAt) {
        log.info("[EmailService stub] resendInvitation to {} ({}) token=... expires={}",
                email, fullName, expiresAt);
        log.debug("[EmailService stub] Token (DO NOT log in production): {}", token);
    }
}
