package com.sealhackathon.api.users.mapper;

import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User u) {
        return toSummary(u, null, null);
    }

    public UserSummaryResponse toSummary(User u, Invitation inv) {
        return toSummary(u, inv, inv == null ? null : inv.getLastTokenSent());
    }

    public UserSummaryResponse toSummary(User u, Invitation inv, Boolean tokenSent) {
        if (u == null) {
            return null;
        }
        TempJudgeResponse.InvitationInfo invitationInfo = null;
        if (inv != null) {
            invitationInfo = TempJudgeResponse.InvitationInfo.builder()
                    .id(inv.getId())
                    .expiresAt(inv.getExpiresAt())
                    .acceptedAt(inv.getAcceptedAt())
                    .tokenSent(tokenSent)
                    .revokedAt(inv.getRevokedAt())
                    .build();
        }
        return UserSummaryResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .userType(u.getUserType())
                .isTempAccount(u.getIsTempAccount())
                .isDeptHead(u.getIsDeptHead())
                .mustChangePassword(u.getMustChangePassword())
                .institution(u.getInstitution())
                .avatarUrl(u.getAvatarUrl())
                .invitation(invitationInfo)
                .build();
    }

    public TempJudgeResponse toTempJudgeResponse(User u, Invitation inv, boolean tokenSent) {
        return TempJudgeResponse.builder()
                .user(toSummary(u, inv, tokenSent))
                .invitation(TempJudgeResponse.InvitationInfo.builder()
                        .id(inv == null ? null : inv.getId())
                        .expiresAt(inv == null ? null : inv.getExpiresAt())
                        .acceptedAt(inv == null ? null : inv.getAcceptedAt())
                        .tokenSent(tokenSent)
                        .revokedAt(inv == null ? null : inv.getRevokedAt())
                        .build())
                .build();
    }
}
