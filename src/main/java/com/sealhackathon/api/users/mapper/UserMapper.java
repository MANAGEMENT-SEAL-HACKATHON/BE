package com.sealhackathon.api.users.mapper;

import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User u) {
        if (u == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .userType(u.getUserType())
                .isTempAccount(u.getIsTempAccount())
                .institution(u.getInstitution())
                .build();
    }

    public TempJudgeResponse toTempJudgeResponse(User u, Invitation inv, boolean tokenSent) {
        return TempJudgeResponse.builder()
                .user(toSummary(u))
                .invitation(TempJudgeResponse.InvitationInfo.builder()
                        .id(inv == null ? null : inv.getId())
                        .expiresAt(inv == null ? null : inv.getExpiresAt())
                        .acceptedAt(inv == null ? null : inv.getAcceptedAt())
                        .tokenSent(tokenSent)
                        .build())
                .build();
    }
}
