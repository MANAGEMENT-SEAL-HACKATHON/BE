package com.sealhackathon.api.users.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * FR-05a — response gộp User + Invitation sau khi tạo Judge tạm.
 *
 * <p>{@code tokenSent = true} nghĩa là email đã được gọi (queue async). Token KHÔNG trả về.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempJudgeResponse {

    private final UserSummaryResponse user;
    private final InvitationInfo invitation;

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InvitationInfo {
        private final Integer id;
        private final LocalDateTime expiresAt;
        private final Boolean tokenSent;
        private final LocalDateTime acceptedAt;
        private final LocalDateTime revokedAt;
    }
}
