package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.sealhackathon.api.users.dto.request.PatchMeRequest;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.request.PatchUserStatusRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.mapper.UserResponseMapper;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.service.StudentCardStorageService;
import com.sealhackathon.api.users.service.UserAdminService;
import com.sealhackathon.api.users.support.PersonnelAssignmentRules;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserResponseMapper userResponseMapper;
    private final StudentCardStorageService studentCardStorageService;

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
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().trim().isEmpty() ? null : req.getPhone().trim());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().trim().isEmpty() ? null : req.getAvatarUrl().trim());
        }
        if (req.getUserType() != null) {
            if (req.getUserType() == UserType.UNSPECIFIED) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "userType không hợp lệ khi cập nhật hồ sơ");
            }
            user.setUserType(req.getUserType());
            if (req.getUserType() == UserType.INTERNAL) {
                user.setInstitution(null);
            } else if (req.getUserType() == UserType.EXTERNAL) {
                user.setChapter(null);
            }
        }
        if (req.getStudentCode() != null) {
            String studentCode = req.getStudentCode().trim();
            user.setStudentCode(studentCode.isEmpty() ? null : studentCode);
        }
        if (req.getChapterId() != null) {
            Chapter chapter = chapterRepository.findById(req.getChapterId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                            "chapterId không tồn tại", Map.of("chapterId", req.getChapterId())));
            if (chapter.getStatus() != ChapterStatus.ACTIVE) {
                throw new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                        "Chapter không ACTIVE", Map.of("chapterId", req.getChapterId()));
            }
            user.setChapter(chapter);
            if (user.getUserType() == UserType.INTERNAL) {
                user.setInstitution(null);
            }
        }
        if (req.getInstitution() != null) {
            String institution = req.getInstitution().trim();
            user.setInstitution(institution.isEmpty() ? null : institution);
            if (user.getUserType() == UserType.EXTERNAL) {
                user.setChapter(null);
            }
        }
        if (user.getStudentCode() != null && !user.getStudentCode().isBlank()) {
            if (userRepository.existsByStudentCodeAndIdNot(user.getStudentCode(), user.getId())) {
                throw new BusinessRuleException(ErrorCode.STUDENT_CODE_DUPLICATE,
                        "Mã sinh viên đã được sử dụng", Map.of("studentCode", user.getStudentCode()));
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userResponseMapper.toDetail(userRepository.save(user));
    }

    @Override
    public UserDetailResponse uploadMyStudentCard(MultipartFile file) {
        User user = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));
        String storageKey = studentCardStorageService.store(user.getId(), file, user.getStudentCardImagePath());
        user.setStudentCardImagePath(storageKey);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return userResponseMapper.toDetail(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getMyStudentCard() {
        User user = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));
        return studentCardStorageService.loadAsResource(user.getStudentCardImagePath());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listUsers(UserStatus status, UserRole role, Boolean personnelOnly,
                                                       Boolean accountRoleExact, UserType userType, String q,
                                                       Pageable pageable) {
        boolean expandPool = PersonnelAssignmentRules.shouldExpandPersonnelPool(
                role, personnelOnly, accountRoleExact);
        UserRole roleParam = expandPool ? null : role;
        Boolean personnelParam = expandPool ? Boolean.TRUE : personnelOnly;
        Page<User> page = userRepository.searchAdmin(status, roleParam, personnelParam, userType, q, pageable);
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

        List<JudgeAssignmentResponse> syncedAssignments = null;
        if (req.getIsDeptHead() != null) {
            log.warn("Deprecated PATCH isDeptHead for user #{} — ignored", userId);
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "is_dept_head đã ngừng sử dụng — chọn HEAD khi gán giám khảo Sơ loại hoặc Chung kết",
                    Map.of("userId", userId));
        }

        User saved = userRepository.save(user);
        return userResponseMapper.toResponse(saved, syncedAssignments);
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
            ensureStudentProfileCompleteForApproval(user);
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

    @Override
    @Transactional(readOnly = true)
    public Resource getUserStudentCard(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return studentCardStorageService.loadAsResource(user.getStudentCardImagePath());
    }

    private static void ensureStudentProfileCompleteForApproval(User user) {
        if (user.getRole() != UserRole.STUDENT) {
            return;
        }
        if (user.getUserType() == null || user.getUserType() == UserType.UNSPECIFIED) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Sinh viên phải khai báo loại tài khoản INTERNAL/EXTERNAL trước khi duyệt");
        }
        if (user.getStudentCode() == null || user.getStudentCode().isBlank()) {
            throw new BusinessRuleException(ErrorCode.STUDENT_CODE_REQUIRED,
                    "Sinh viên phải khai báo studentCode trước khi duyệt");
        }
        if (user.getStudentCardImagePath() == null || user.getStudentCardImagePath().isBlank()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Sinh viên phải upload ảnh thẻ sinh viên trước khi duyệt");
        }
        if (user.getUserType() == UserType.INTERNAL) {
            if (user.getChapter() == null) {
                throw new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                        "Sinh viên INTERNAL phải chọn chapter trước khi duyệt");
            }
        } else if (user.getUserType() == UserType.EXTERNAL) {
            if (user.getInstitution() == null || user.getInstitution().isBlank()) {
                throw new BusinessRuleException(ErrorCode.INSTITUTION_REQUIRED,
                        "Sinh viên EXTERNAL phải khai báo institution trước khi duyệt");
            }
        }
    }
}
