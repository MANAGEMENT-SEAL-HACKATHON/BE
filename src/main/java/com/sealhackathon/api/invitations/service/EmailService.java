package com.sealhackathon.api.invitations.service;

import java.time.LocalDateTime;

/**
 * Interface stub gửi email mời (FR-05a). Triển khai cụ thể (SMTP / SES / SendGrid)
 * thực hiện ở module Email Worker — out-of-scope MF-01.
 *
 * <p>Quy ước:
 * <ul>
 *   <li>Body chứa link one-time-use: {@code ${frontend.url}/invitations/accept?token={token}}</li>
 *   <li>{@code expiresAt} là deadline để user set mật khẩu lần đầu (mặc định 48h)</li>
 *   <li>KHÔNG gửi mật khẩu plaintext</li>
 *   <li>Thực hiện async — không block flow tạo Judge tạm</li>
 * </ul>
 */
public interface EmailService {

    /**
     * Gửi email mời Judge khách mời.
     */
    void sendInvitation(String email, String fullName, String token, LocalDateTime expiresAt);

    /**
     * Gửi lại email mời với token mới (FR-05a POST /invitations/{id}/resend).
     */
    void resendInvitation(String email, String fullName, String token, LocalDateTime expiresAt);
}
