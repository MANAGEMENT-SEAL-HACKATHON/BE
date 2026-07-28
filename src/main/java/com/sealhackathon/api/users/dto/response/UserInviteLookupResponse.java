package com.sealhackathon.api.users.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserInviteLookupResponse {
    String fullName;
    String email;
    String studentCode;
    String avatarUrl;
    String institution;
    String role;
}
