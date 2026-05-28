package com.sealhackathon.api.users.dto.response;

import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDetailResponse {

    private final Integer id;
    private final String fullName;
    private final String email;
    private final UserRole role;
    private final UserType userType;
    private final UserStatus status;
    private final String studentCode;
    private final Integer chapterId;
    /** Mã chapter (VD FPT-HCM) — Coordinator đối chiếu khi duyệt INTERNAL. */
    private final String chapterCode;
    private final String chapterName;
    private final String institution;
    private final String studentCardImagePath;
    private final Boolean isTempAccount;
    private final Boolean mustChangePassword;
    private final Boolean isDeptHead;
    private final String rejectionReason;
    private final LocalDateTime emailVerifiedAt;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
}
