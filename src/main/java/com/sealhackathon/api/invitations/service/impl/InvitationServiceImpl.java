package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.invitations.service.InvitationService;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * FR-05a Resend invitation: regenerate token, reset expiresAt = NOW+48h, gửi lại email stub.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvitationServiceImpl implements InvitationService {

    private static final int INVITATION_EXPIRY_HOURS = 48;
    private static final SecureRandom RNG = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Override
    public TempJudgeResponse.InvitationInfo resend(Integer invitationId) {
        Invitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));
        if (inv.getAcceptedAt() != null) {
            throw new ConflictException(ErrorCode.INVITATION_ALREADY_ACCEPTED,
                    "Invitation đã được accept tại " + inv.getAcceptedAt(),
                    Map.of("invitationId", invitationId, "acceptedAt", inv.getAcceptedAt()));
        }
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(ErrorCode.INVITATION_STILL_VALID,
                    "Token còn hiệu lực — chỉ resend sau khi hết hạn",
                    Map.of("invitationId", invitationId, "expiresAt", inv.getExpiresAt()));
        }

        inv.setToken(generateToken());
        inv.setExpiresAt(LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS));
        Invitation saved = invitationRepository.save(inv);

        String fullName = userRepository.findByEmail(saved.getEmail())
                .map(User::getFullName)
                .orElse(saved.getEmail());
        boolean tokenSent = true;
        try {
            emailService.resendInvitation(saved.getEmail(), fullName,
                    saved.getToken(), saved.getExpiresAt());
        } catch (RuntimeException ex) {
            log.warn("[Invitation] resend failed for {}: {}", saved.getEmail(), ex.getMessage());
            tokenSent = false;
        }

        auditService.log(AuditAction.INVITATION_RESEND, "invitations", saved.getId(), Map.of(
                "email",     saved.getEmail(),
                "expiresAt", saved.getExpiresAt().toString(),
                "tokenSent", tokenSent
        ));
        return TempJudgeResponse.InvitationInfo.builder()
                .id(saved.getId())
                .expiresAt(saved.getExpiresAt())
                .tokenSent(tokenSent)
                .acceptedAt(saved.getAcceptedAt())
                .build();
    }

    private String generateToken() {
        byte[] randomBytes = new byte[36];
        RNG.nextBytes(randomBytes);
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String token = (UUID.randomUUID().toString().replace("-", "") + base64).substring(0, 64);
        return new String(token.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
