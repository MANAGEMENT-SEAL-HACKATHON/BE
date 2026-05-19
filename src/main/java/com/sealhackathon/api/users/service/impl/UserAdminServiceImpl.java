package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.service.UserAdminService;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private static final Set<UserRole> DEPT_HEAD_ELIGIBLE =
            EnumSet.of(UserRole.JUDGE, UserRole.MENTOR);

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public UserResponse patchUser(Integer userId, PatchUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (req.getIsDeptHead() != null) {
            if (!DEPT_HEAD_ELIGIBLE.contains(user.getRole())) {
                throw new BusinessRuleException(ErrorCode.USER_INVALID_ROLE,
                        "is_dept_head chỉ áp dụng cho JUDGE hoặc MENTOR",
                        Map.of("userId", userId, "role", user.getRole()));
            }
            Boolean previous = user.getIsDeptHead();
            user.setIsDeptHead(req.getIsDeptHead());
            user.setUpdatedAt(LocalDateTime.now());
            if (Boolean.TRUE.equals(req.getIsDeptHead()) && !Boolean.TRUE.equals(previous)) {
                auditService.log(AuditAction.USER_DEPT_HEAD_SET, "users", userId, Map.of(
                        "setBy", currentUserAccessor.currentUserId(),
                        "isDeptHead", true));
            }
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private static UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .userType(u.getUserType())
                .isTempAccount(u.getIsTempAccount())
                .isDeptHead(u.getIsDeptHead())
                .status(u.getStatus())
                .institution(u.getInstitution())
                .build();
    }
}
