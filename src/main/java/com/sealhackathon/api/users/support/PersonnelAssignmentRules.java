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
 * Personnel assignment rules (FR-05).
 *
 * <p><b>users.role</b>: tài khoản MENTOR hoặc JUDGE — có thể được gán vai chéo
 * (MENTOR account làm Judge, JUDGE account làm Mentor).
 *
 * <p><b>Cùng vai trò — 1 bảng / vòng:</b>
 * <ul>
 *   <li>Mentor track A → không Mentor track B cùng vòng</li>
 *   <li>Judge track A → không Judge track B cùng vòng</li>
 * </ul>
 *
 * <p><b>Cross-track (vai khác) — được phép:</b>
 * <ul>
 *   <li>Mentor track A → được Judge track B (không phải A)</li>
 *   <li>Judge track A → được Mentor track B (không phải A)</li>
 * </ul>
 *
 * <p><b>Cùng track — cấm:</b> Mentor + Judge cùng {@code track_id} → {@code CONFLICT_SAME_TRACK}.
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
