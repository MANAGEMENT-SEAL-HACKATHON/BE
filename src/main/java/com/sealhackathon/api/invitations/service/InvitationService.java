package com.sealhackathon.api.invitations.service;

import com.sealhackathon.api.users.dto.response.TempJudgeResponse;

/**
 * FR-05a — Resend invitation judge khách (POST /api/v1/invitations/{id}/resend).
 */
public interface InvitationService {

    TempJudgeResponse.InvitationInfo resend(Integer invitationId);
}
