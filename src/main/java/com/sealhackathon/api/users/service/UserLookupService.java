package com.sealhackathon.api.users.service;

import com.sealhackathon.api.users.dto.response.UserInviteLookupResponse;

import java.util.List;

public interface UserLookupService {

    /**
     * Tìm SV đã duyệt đã đăng ký {@code hackathonId} để mời vào đội.
     * {@code hackathonId} bắt buộc.
     */
    List<UserInviteLookupResponse> lookupInviteCandidates(String q, Integer hackathonId);

    List<UserInviteLookupResponse> lookupCoordinatorInviteCandidates(String q);
}
