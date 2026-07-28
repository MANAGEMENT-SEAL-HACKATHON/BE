package com.sealhackathon.api.users.mapper;

import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserResponseMapper {

    public UserResponse toResponse(User u) {
        return toResponse(u, null);
    }

    public UserResponse toResponse(User u, List<JudgeAssignmentResponse> assignments) {
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
                .studentCardImagePath(u.getStudentCardImagePath())
                .assignments(assignments)
                .build();
    }

    public UserSummaryResponse toSummary(User u) {
        return UserSummaryResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .userType(u.getUserType())
                .isTempAccount(u.getIsTempAccount())
                .isDeptHead(u.getIsDeptHead())
                .institution(u.getInstitution())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }

    public UserDetailResponse toDetail(User u) {
        return UserDetailResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .userType(u.getUserType())
                .status(u.getStatus())
                .studentCode(u.getStudentCode())
                .chapterId(u.getChapter() != null ? u.getChapter().getId() : null)
                .chapterCode(u.getChapter() != null ? u.getChapter().getCode() : null)
                .chapterName(u.getChapter() != null ? u.getChapter().getName() : null)
                .institution(u.getInstitution())
                .studentCardImagePath(u.getStudentCardImagePath())
                .isTempAccount(u.getIsTempAccount())
                .mustChangePassword(u.getMustChangePassword())
                .isDeptHead(u.getIsDeptHead())
                .rejectionReason(u.getRejectionReason())
                .emailVerifiedAt(u.getEmailVerifiedAt())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
