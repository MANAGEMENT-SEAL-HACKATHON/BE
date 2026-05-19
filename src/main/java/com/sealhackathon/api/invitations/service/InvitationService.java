package com.sealhackathon.api.invitations.service;

import com.sealhackathon.api.users.dto.response.TempJudgeResponse;

/**
 * FR-05a — Resend invitation (POST /api/v1/invitations/{id}/resend).
 *
 * <p>Business rules:
 * <ul>
 *   <li>404 nếu invitation không tồn tại.</li>
 *   <li>409 {@code INVITATION_ALREADY_ACCEPTED} nếu {@code accepted_at IS NOT NULL}.</li>
 *   <li>Regenerate token + expires_at = NOW + 48h; gọi {@link EmailService#resendInvitation}.</li>
 * </ul>
 *
 * <p>Audit: {@code INVITATION_RESEND}.
 */
public interface InvitationService {

    TempJudgeResponse.InvitationInfo resend(Integer invitationId);
}
