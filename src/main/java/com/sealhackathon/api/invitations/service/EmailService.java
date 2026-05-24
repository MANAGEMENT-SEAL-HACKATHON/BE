package com.sealhackathon.api.invitations.service;

import java.time.LocalDateTime;

/**
 * Gửi email judge khách mời (FR-05a). Impl SMTP/SES thay {@code NoOpEmailServiceImpl} sau.
 */
public interface EmailService {

    /**
     * Email: địa chỉ đăng nhập, mật khẩu tạm, link {@code /login}, thời hạn invitation.
     */
    void sendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                  String loginUrl, LocalDateTime expiresAt);

    /**
     * Gửi lại sau khi invitation hết hạn (MK tạm mới).
     */
    void resendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                    String loginUrl, LocalDateTime expiresAt);
}
