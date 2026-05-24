package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.users.dto.request.PatchMeRequest;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.request.PatchUserStatusRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.mapper.UserResponseMapper;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.service.UserAdminService;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserResponseMapper userResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getMe() {
        User user = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));
        return userResponseMapper.toDetail(user);
    }

    @Override
    public UserDetailResponse patchMe(PatchMeRequest req) {
        User user = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().trim().isEmpty() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().trim().isEmpty() ? null : req.getAvatarUrl().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userResponseMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listUsers(UserStatus status, UserRole role, UserType userType,
                                                       String q, Pageable pageable) {
        Page<User> page = userRepository.searchAdmin(status, role, userType, q, pageable);
        return PageResponse.from(page, page.getContent().stream()
                .map(userResponseMapper::toSummary)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userResponseMapper.toDetail(user);
    }

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
        return userResponseMapper.toResponse(saved);
    }

    @Override
    public UserDetailResponse patchStatus(Integer userId, PatchUserStatusRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserStatus from = user.getStatus();
        UserStatus to = req.getStatus();

        if (from == UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể đổi trạng thái từ APPROVED",
                    Map.of("from", from.name(), "to", to.name()));
        }

        if (from == to) {
            return userResponseMapper.toDetail(user);
        }

        if (from == UserStatus.REJECTED && to == UserStatus.PENDING) {
            if (req.getOverrideReason() == null || req.getOverrideReason().isBlank()) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "REJECTED → PENDING cần overrideReason");
            }
            user.setStatus(UserStatus.PENDING);
            user.setRejectionReason(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            auditService.log(AuditAction.ACCOUNT_STATUS_OVERRIDE, "users", userId, Map.of(
                    "from", from.name(),
                    "to", to.name(),
                    "overrideReason", req.getOverrideReason().trim(),
                    "by", currentUserAccessor.currentUserId()));
            return userResponseMapper.toDetail(user);
        }

        if (to == UserStatus.REJECTED) {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new BusinessRuleException(ErrorCode.REJECTION_REASON_REQUIRED,
                        "REJECTED bắt buộc rejectionReason");
            }
            user.setStatus(UserStatus.REJECTED);
            user.setRejectionReason(req.getRejectionReason().trim());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            auditService.log(AuditAction.ACCOUNT_REJECT, "users", userId, Map.of(
                    "from", from.name(),
                    "reason", req.getRejectionReason().trim(),
                    "by", currentUserAccessor.currentUserId()));
            return userResponseMapper.toDetail(user);
        }

        if (to == UserStatus.APPROVED) {
            if (from != UserStatus.PENDING) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "Chỉ PENDING mới duyệt APPROVED",
                        Map.of("from", from.name()));
            }
            if (user.getEmailVerifiedAt() == null) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Phải xác thực email trước khi duyệt",
                        Map.of("userId", userId));
            }
            user.setStatus(UserStatus.APPROVED);
            user.setRejectionReason(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            auditService.log(AuditAction.ACCOUNT_APPROVE, "users", userId, Map.of(
                    "from", from.name(),
                    "by", currentUserAccessor.currentUserId()));
            return userResponseMapper.toDetail(user);
        }

        throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Chuyển trạng thái không hợp lệ",
                Map.of("from", from.name(), "to", to.name()));
    }
}
