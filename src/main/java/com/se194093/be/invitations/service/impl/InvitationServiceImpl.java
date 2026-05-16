package com.se194093.be.invitations.service.impl;

import com.se194093.be.invitations.service.InvitationService;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-05-personnel.md} §3.
 *
 * <p>Inject: InvitationRepository, EmailService, UserRepository, AuditService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    @Override
    public TempJudgeResponse.InvitationInfo resend(Integer invitationId) {
        // TODO Dev:
        //  - inv = invitationRepo.findById(invitationId) or 404
        //  - if inv.acceptedAt != null → throw 409 INVITATION_ALREADY_ACCEPTED
        //  - inv.token = generateUuidTokenBase64(64)
        //  - inv.expiresAt = now().plusHours(48)
        //  - invitationRepo.save(inv)
        //  - emailService.resendInvitation(inv.email, user.fullName, inv.token, inv.expiresAt)
        //  - audit.log(INVITATION_RESEND, "invitations", inv.id, Map.of("email", inv.email))
        //  - return InvitationInfo.builder().id(inv.id).expiresAt(inv.expiresAt).tokenSent(true).build()
        throw new UnsupportedOperationException("FR-05a POST /invitations/{id}/resend - to be implemented");
    }
}
