package com.sealhackathon.api.users.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * MF-02 §14 — Cross-track personnel.
 *
 * <p><b>users.role</b> (1 cột): vai trò tài khoản đăng nhập — MENTOR <b>hoặc</b> JUDGE, không lưu 2 role.
 * <p><b>Phân công thực tế</b> lưu ở {@code mentor_assignments} / {@code judge_assignments}
 * (cùng {@code user_id}, khác {@code track_id}). Mọi track sơ loại có {@code tracks.round_id} →
 * cùng round + khác track được phép; cấm chỉ khi trùng {@code track_id}.
 */
public final class PersonnelAssignmentRules {

    public static final Set<UserRole> ASSIGNABLE_PERSONNEL =
            EnumSet.of(UserRole.MENTOR, UserRole.JUDGE);

    public static boolean isPersonnelAccountRole(UserRole role) {
        return role != null && ASSIGNABLE_PERSONNEL.contains(role);
    }

    /** Dropdown phân công: gộp pool MENTOR + JUDGE trừ khi cần lọc đúng 1 role tài khoản. */
    public static boolean shouldExpandPersonnelPool(UserRole roleFilter, Boolean personnelOnly,
                                                    Boolean accountRoleExact) {
        if (Boolean.TRUE.equals(accountRoleExact)) {
            return false;
        }
        if (Boolean.TRUE.equals(personnelOnly)) {
            return true;
        }
        return isPersonnelAccountRole(roleFilter);
    }

    private PersonnelAssignmentRules() {
    }

    public static void requireApprovedPersonnel(User user, String assignmentKind) {
        if (!ASSIGNABLE_PERSONNEL.contains(user.getRole())) {
            throw new BusinessRuleException(ErrorCode.USER_INVALID_ROLE,
                    "User #%d không thể phân công %s — cần role MENTOR hoặc JUDGE (hiện %s)"
                            .formatted(user.getId(), assignmentKind, user.getRole()),
                    Map.of("userId", user.getId(), "role", user.getRole()));
        }
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.USER_NOT_APPROVED,
                    "User #%d chưa APPROVED".formatted(user.getId()),
                    Map.of("userId", user.getId(), "status", user.getStatus()));
        }
    }
}
