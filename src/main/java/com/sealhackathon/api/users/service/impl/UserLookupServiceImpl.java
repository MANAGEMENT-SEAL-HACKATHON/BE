package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.dto.response.UserInviteLookupResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.service.UserLookupService;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

    private static final int MAX_RESULTS = 10;
    private static final int MIN_QUERY_LENGTH = 2;

    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional(readOnly = true)
    public List<UserInviteLookupResponse> lookupInviteCandidates(String q) {
        CurrentUserStub actor = currentUserAccessor.currentUser();
        if (actor == null || actor.getRole() != UserRole.STUDENT) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                    "Chỉ sinh viên đã duyệt mới được tìm tài khoản để mời vào đội");
        }
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        Integer excludeId = actor.getUserId();
        return userRepository.searchApprovedStudentsForInvite(trimmed, excludeId,
                        PageRequest.of(0, MAX_RESULTS))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserInviteLookupResponse> lookupCoordinatorInviteCandidates(String q) {
        CurrentUserStub actor = currentUserAccessor.currentUser();
        if (actor == null || actor.getRole() != UserRole.COORDINATOR) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                    "Chỉ điều phối viên mới được tìm tài khoản để mời giám khảo/mentor");
        }
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        return userRepository.searchApprovedPersonnelForCoordinatorInvite(trimmed,
                        PageRequest.of(0, MAX_RESULTS))
                .stream()
                .map(this::toPersonnelResponse)
                .toList();
    }

    private UserInviteLookupResponse toResponse(User user) {
        return UserInviteLookupResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentCode(user.getStudentCode())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private UserInviteLookupResponse toPersonnelResponse(User user) {
        return UserInviteLookupResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentCode(user.getStudentCode())
                .avatarUrl(user.getAvatarUrl())
                .institution(user.getInstitution())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
