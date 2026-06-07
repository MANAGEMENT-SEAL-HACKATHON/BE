package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.mentor_team_assignments.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FR-11/12 — Chỉ người liên quan đội mới xem được chi tiết + danh sách thành viên.
 */
@Component
@RequiredArgsConstructor
public class TeamAccessGuard {

    private static final List<TeamMemberStatus> VIEWABLE_MEMBER_STATUSES = List.of(
            TeamMemberStatus.PENDING,
            TeamMemberStatus.ACCEPTED);

    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;

    public void assertCanViewTeamDetails(Integer teamId) {
        var user = currentUserAccessor.currentUser();
        if (user.getRole() == UserRole.COORDINATOR) {
            return;
        }
        Integer userId = user.getUserId();
        if (user.getRole() == UserRole.MENTOR
                && mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(userId, teamId)) {
            return;
        }
        if (teamMemberRepository.existsByUser_IdAndTeam_IdAndMemberStatusIn(
                userId, teamId, VIEWABLE_MEMBER_STATUSES)) {
            return;
        }
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Bạn không có quyền xem đội này",
                HttpStatus.FORBIDDEN);
    }
}
