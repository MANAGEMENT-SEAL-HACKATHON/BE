package com.sealhackathon.api.users.service;

import com.sealhackathon.api.users.dto.response.UserInviteLookupResponse;

import java.util.List;

public interface UserLookupService {

    List<UserInviteLookupResponse> lookupInviteCandidates(String q);

    List<UserInviteLookupResponse> lookupCoordinatorInviteCandidates(String q);
}
