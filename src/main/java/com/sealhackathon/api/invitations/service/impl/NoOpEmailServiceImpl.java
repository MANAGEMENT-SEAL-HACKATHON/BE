package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.invitations.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Impl mặc định khi {@code app.mail.enabled=false} (hoặc không set) — chỉ log, không gửi mail.
 * Bật SMTP thật bằng {@code app.mail.enabled=true} → {@link SmtpEmailServiceImpl}.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmailServiceImpl implements EmailService {

    @Override
    public void sendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                         String loginUrl, LocalDateTime expiresAt) {
        log.info("[EmailService stub] sendGuestJudgeInvitation to {} ({}) loginUrl={} expires={}",
                email, fullName, loginUrl, expiresAt);
        log.debug("[EmailService stub] temp password (DO NOT log in production): {}", tempPassword);
    }

    @Override
    public void resendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                           String loginUrl, LocalDateTime expiresAt) {
        log.info("[EmailService stub] resendGuestJudgeInvitation to {} ({}) loginUrl={} expires={}",
                email, fullName, loginUrl, expiresAt);
        log.debug("[EmailService stub] temp password (DO NOT log in production): {}", tempPassword);
    }

    @Override
    public void sendRegistrationWelcome(String email, String fullName, String loginUrl) {
        log.info("[EmailService stub] sendRegistrationWelcome to {} ({}) loginUrl={}",
                email, fullName, loginUrl);
    }

    @Override
    public void sendEmailVerification(String email, String fullName, String verifyUrl,
                                      LocalDateTime expiresAt) {
        log.info("[EmailService stub] sendEmailVerification to {} ({}) expires={}", email, fullName, expiresAt);
        log.debug("[EmailService stub] verify URL (DO NOT log in production): {}", verifyUrl);
    }

    @Override
    public void sendPasswordReset(String email, String fullName, String resetUrl,
                                  LocalDateTime expiresAt) {
        log.info("[EmailService stub] sendPasswordReset to {} ({}) expires={}", email, fullName, expiresAt);
        log.debug("[EmailService stub] reset URL (DO NOT log in production): {}", resetUrl);
    }

    @Override
    public void sendMentorAssignment(String email, String fullName, String trackName,
                                     String hackathonName, String loginUrl) {
        log.info("[EmailService stub] sendMentorAssignment to {} ({}) track='{}' hackathon='{}'",
                email, fullName, trackName, hackathonName);
    }

    @Override
    public void sendJudgeAssignment(String email, String fullName, String assignmentLabel,
                                    String hackathonName, String loginUrl) {
        log.info("[EmailService stub] sendJudgeAssignment to {} ({}) assignment='{}' hackathon='{}'",
                email, fullName, assignmentLabel, hackathonName);
    }
}
