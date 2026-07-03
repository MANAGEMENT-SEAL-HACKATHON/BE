package com.sealhackathon.api.invitations.service;

import java.time.LocalDateTime;

/**
 * Gửi email hệ thống. Mặc định {@link com.sealhackathon.api.invitations.service.impl.NoOpEmailServiceImpl}
 * (chỉ log). Bật SMTP thật qua {@code app.mail.enabled=true} →
 * {@link com.sealhackathon.api.invitations.service.impl.SmtpEmailServiceImpl}.
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

    /**
     * Mail chào mừng / xác nhận đã đăng ký tài khoản (không chặn đăng nhập).
     */
    void sendRegistrationWelcome(String email, String fullName, String loginUrl);

    /**
     * Mail xác thực email sau đăng ký — chứa link verify kèm token và thời hạn.
     */
    void sendEmailVerification(String email, String fullName, String verifyUrl, LocalDateTime expiresAt);

    /**
     * Mail đặt lại mật khẩu — chứa link reset kèm token và thời hạn.
     */
    void sendPasswordReset(String email, String fullName, String resetUrl, LocalDateTime expiresAt);

    /**
     * Thông báo phân công Mentor cho một Track.
     */
    void sendMentorAssignment(String email, String fullName, String trackName,
                              String hackathonName, String loginUrl);

    /**
     * Thông báo phân công Judge (Track sơ loại hoặc Round chung kết).
     */
    void sendJudgeAssignment(String email, String fullName, String assignmentLabel,
                             String hackathonName, String loginUrl);
}
