package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.invitations.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
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
}
